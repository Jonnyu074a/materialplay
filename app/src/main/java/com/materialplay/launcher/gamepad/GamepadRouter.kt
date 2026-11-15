package com.materialplay.launcher.gamepad

import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Shared snapshot of raw hardware input, used by the controller test screen. */
class GamepadRawState {
    private val _state = MutableStateFlow(RawPadState())
    val state: StateFlow<RawPadState> = _state.asStateFlow()

    fun key(keyCode: Int, action: PadAction, down: Boolean) {
        val current = _state.value
        val keys = if (down) current.pressedKeys + keyCode else current.pressedKeys - keyCode
        val actions = if (down) current.actions + action else current.actions - action
        _state.value = current.copy(pressedKeys = keys, actions = actions, updatedAt = System.currentTimeMillis())
    }

    fun axes(values: Map<Int, Float>) {
        val current = _state.value
        _state.value = current.copy(axes = values, updatedAt = System.currentTimeMillis())
    }

    fun reset() {
        _state.value = RawPadState()
    }
}

/**
 * Central gamepad router for Gaming Mode. Converts hardware events into semantic
 * actions, adds joystick auto-repeat, and detects the configurable overlay chord.
 */
class GamepadRouter(
    private val scope: CoroutineScope,
    private val raw: GamepadRawState
) {
    private val _actions = MutableSharedFlow<PadAction>(extraBufferCapacity = 96)
    val actions: SharedFlow<PadAction> = _actions.asSharedFlow()

    private val _overlayRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 2)
    val overlayRequests: SharedFlow<Unit> = _overlayRequests.asSharedFlow()

    var overlayCombo: Set<PadAction> = setOf(PadAction.L1, PadAction.R1)
    var swapFaceButtons: Boolean = false
    var deadzone: Float = 0.55f

    private val pressed = mutableSetOf<PadAction>()
    private var overlayFired = false
    private var stickDirection: PadAction? = null
    private var stickJob: Job? = null

    fun onKeyEvent(event: KeyEvent): Boolean {
        val action = PadKeyMap.actionFor(event.keyCode, swapFaceButtons) ?: return false
        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                val isNew = pressed.add(action)
                raw.key(event.keyCode, action, true)
                if (isNew) {
                    checkOverlayChord()
                    _actions.tryEmit(action)
                } else if (action.isDirection) {
                    // Hardware key repeat is useful for held directions.
                    _actions.tryEmit(action)
                }
            }
            KeyEvent.ACTION_UP -> {
                pressed.remove(action)
                raw.key(event.keyCode, action, false)
                if (overlayCombo.none { it in pressed }) overlayFired = false
            }
        }
        return true
    }

    fun onGenericMotionEvent(event: MotionEvent): Boolean {
        val isJoystick = (event.source and InputDevice.SOURCE_JOYSTICK) != 0 ||
            (event.source and InputDevice.SOURCE_GAMEPAD) != 0
        if (!isJoystick) return false

        val values = buildMap {
            for (axis in TRACKED_AXES) {
                val value = event.getAxisValue(axis)
                if (abs(value) > 0.05f) put(axis, value)
            }
        }
        raw.axes(values)

        val x = event.getAxisValue(MotionEvent.AXIS_X)
        val y = event.getAxisValue(MotionEvent.AXIS_Y)
        val hatX = event.getAxisValue(MotionEvent.AXIS_HAT_X)
        val hatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y)
        val horizontal = if (abs(hatX) > abs(x)) hatX else x
        val vertical = if (abs(hatY) > abs(y)) hatY else y
        handleStick(horizontal, vertical)
        return true
    }

    fun resetInputState() {
        pressed.clear()
        overlayFired = false
        stickJob?.cancel()
        stickJob = null
        stickDirection = null
        raw.reset()
    }

    fun stop() {
        stickJob?.cancel()
        stickJob = null
    }

    private fun checkOverlayChord() {
        if (overlayCombo.isEmpty()) return
        if (!overlayFired && pressed.containsAll(overlayCombo)) {
            overlayFired = true
            _overlayRequests.tryEmit(Unit)
        }
    }

    private fun handleStick(x: Float, y: Float) {
        val direction = when {
            abs(x) < deadzone && abs(y) < deadzone -> null
            abs(x) > abs(y) -> if (x > 0f) PadAction.RIGHT else PadAction.LEFT
            else -> if (y > 0f) PadAction.DOWN else PadAction.UP
        }
        if (direction == stickDirection) return
        stickDirection = direction
        stickJob?.cancel()
        stickJob = null
        if (direction == null) return
        _actions.tryEmit(direction)
        stickJob = scope.launch {
            delay(330)
            while (isActive && stickDirection == direction) {
                _actions.tryEmit(direction)
                delay(140)
            }
        }
    }

    companion object {
        private val TRACKED_AXES = intArrayOf(
            MotionEvent.AXIS_X,
            MotionEvent.AXIS_Y,
            MotionEvent.AXIS_Z,
            MotionEvent.AXIS_RZ,
            MotionEvent.AXIS_RX,
            MotionEvent.AXIS_RY,
            MotionEvent.AXIS_HAT_X,
            MotionEvent.AXIS_HAT_Y,
            MotionEvent.AXIS_LTRIGGER,
            MotionEvent.AXIS_RTRIGGER,
            MotionEvent.AXIS_BRAKE,
            MotionEvent.AXIS_GAS
        )
    }
}
