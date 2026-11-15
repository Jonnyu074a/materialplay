package com.materialplay.launcher.gamepad

/** Semantic actions independent from any physical button layout. */
enum class PadAction(val label: String) {
    CONFIRM("Confirm"),
    BACK("Back"),
    TERTIARY("Alternate"),
    QUATERNARY("Secondary"),
    L1("L1"),
    R1("R1"),
    L2("L2"),
    R2("R2"),
    START("Start"),
    SELECT("Select"),
    L3("L3"),
    R3("R3"),
    UP("Up"),
    DOWN("Down"),
    LEFT("Left"),
    RIGHT("Right");

    val isDirection: Boolean get() = this == UP || this == DOWN || this == LEFT || this == RIGHT
}

enum class ConnectionKind(val label: String) {
    USB("USB / HID"),
    BLUETOOTH("Bluetooth"),
    BUILT_IN("Built-in"),
    UNKNOWN("Unknown")
}

/** Physical face-button family. Affects which key code means Confirm/Back. */
enum class PadLayout { STANDARD, NINTENDO }

data class ConnectedController(
    val deviceId: Int,
    val name: String,
    val descriptor: String,
    val vendorId: Int,
    val productId: Int,
    val connection: ConnectionKind,
    val vendorLabel: String?,
    val layout: PadLayout,
    val hasGamepadSource: Boolean,
    val hasJoystickSource: Boolean
) {
    val identity: String get() = descriptor.ifBlank { "input:$deviceId" }
    val displayName: String get() = name.ifBlank { vendorLabel ?: "Controller" }
    val vendorIdHex: String get() = if (vendorId == 0) "n/a" else "0x%04X".format(vendorId)
    val productIdHex: String get() = if (productId == 0) "n/a" else "0x%04X".format(productId)
    val isGameSir: Boolean get() = name.contains("gamesir", ignoreCase = true)
}

data class RawPadState(
    val pressedKeys: Set<Int> = emptySet(),
    val actions: Set<PadAction> = emptySet(),
    val axes: Map<Int, Float> = emptyMap(),
    val updatedAt: Long = 0L
)
