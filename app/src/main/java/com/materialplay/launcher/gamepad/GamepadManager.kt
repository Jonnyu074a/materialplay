package com.materialplay.launcher.gamepad

import android.content.Context
import android.hardware.input.InputManager
import android.os.Handler
import android.os.Looper
import android.view.InputDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Watches physical controllers through InputManager. Detection is based on the
 * device's declared sources, so keyboards and touchscreens are ignored.
 */
class GamepadManager(
    private val context: Context,
    private val scope: CoroutineScope
) : InputManager.InputDeviceListener {

    private val inputManager = context.getSystemService(Context.INPUT_SERVICE) as InputManager
    private val _controllers = MutableStateFlow<List<ConnectedController>>(emptyList())
    val controllers: StateFlow<List<ConnectedController>> = _controllers.asStateFlow()

    private val _active = MutableStateFlow<ConnectedController?>(null)
    val active: StateFlow<ConnectedController?> = _active.asStateFlow()

    private var registered = false

    fun start() {
        if (registered) return
        registered = true
        runCatching { inputManager.registerInputDeviceListener(this, Handler(Looper.getMainLooper())) }
        refresh()
    }

    fun stop() {
        if (!registered) return
        registered = false
        runCatching { inputManager.unregisterInputDeviceListener(this) }
    }

    fun refresh() {
        val deviceIds = inputManager.inputDeviceIds
        val found = (0 until deviceIds.size).mapNotNull { index ->
            runCatching { inputManager.getInputDevice(deviceIds[index]) }.getOrNull()
        }.mapNotNull { device -> device.toController() }
            .filter { it.hasGamepadSource || it.hasJoystickSource }
            .sortedBy { it.deviceId }
        _controllers.value = found
        _active.value = found.firstOrNull()
    }

    override fun onInputDeviceAdded(deviceId: Int) = refresh()

    override fun onInputDeviceRemoved(deviceId: Int) = refresh()

    override fun onInputDeviceChanged(deviceId: Int) = refresh()

    private fun InputDevice.toController(): ConnectedController? {
        if (id < 0) return null
        val sources = sources
        val gamepad = (sources and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD
        val joystick = (sources and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
        // Require at least one gamepad key capability; some devices advertise JOYSTICK
        // without any face buttons (e.g. pedals).
        val hasFaceButtons = runCatching {
            hasKeys(android.view.KeyEvent.KEYCODE_BUTTON_A).any { it }
        }.getOrDefault(false)
        if (!gamepad && !joystick) return null
        if (joystick && !gamepad && !hasFaceButtons) return null

        val deviceName = name.orEmpty()
        val deviceDescriptor = descriptor.orEmpty()
        val vendorName = vendorLabel(vendorId, deviceName)
        val layout = PadKeyMap.layoutFor(deviceName, deviceDescriptor, vendorId)
        val bluetooth = deviceDescriptor.contains("bluetooth", ignoreCase = true) ||
            deviceName.contains("bluetooth", ignoreCase = true) ||
            deviceName.contains("wireless", ignoreCase = true) ||
            deviceName.contains("dual sense", ignoreCase = true) ||
            deviceName.contains("dualshock", ignoreCase = true) ||
            deviceName.contains("xbox wireless", ignoreCase = true) ||
            deviceName.contains("switch pro", ignoreCase = true)
        val connection = when {
            bluetooth -> ConnectionKind.BLUETOOTH
            vendorId != 0 || productId != 0 -> ConnectionKind.USB
            else -> ConnectionKind.UNKNOWN
        }
        return ConnectedController(
            deviceId = id,
            name = deviceName,
            descriptor = deviceDescriptor.ifBlank { "input:$id" },
            vendorId = vendorId,
            productId = productId,
            connection = connection,
            vendorLabel = vendorName,
            layout = layout,
            hasGamepadSource = gamepad,
            hasJoystickSource = joystick
        )
    }

    private fun vendorLabel(vendorId: Int, name: String): String? {
        if (name.contains("gamesir", ignoreCase = true)) return "GameSir"
        return when (vendorId) {
            0x054C -> "Sony"
            0x045E -> "Microsoft"
            0x057E -> "Nintendo"
            0x2DC8 -> "8BitDo"
            0x28DE -> "Valve"
            0x0F0D -> "HORI"
            0x1532 -> "Razer"
            0x046D -> "Logitech"
            0x1038 -> "SteelSeries"
            0x1BAD -> "Xbox / third-party"
            else -> null
        }
    }
}
