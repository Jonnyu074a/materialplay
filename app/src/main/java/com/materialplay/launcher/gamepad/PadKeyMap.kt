package com.materialplay.launcher.gamepad

import android.view.KeyEvent

/**
 * Translates Android key codes to semantic actions. Face buttons are swapped for
 * Nintendo-style physical layouts, and can be manually overridden per controller.
 */
object PadKeyMap {

    fun actionFor(keyCode: Int, swapFaceButtons: Boolean = false): PadAction? {
        return when (keyCode) {
            KeyEvent.KEYCODE_BUTTON_A -> if (swapFaceButtons) PadAction.BACK else PadAction.CONFIRM
            KeyEvent.KEYCODE_BUTTON_B -> if (swapFaceButtons) PadAction.CONFIRM else PadAction.BACK
            KeyEvent.KEYCODE_BUTTON_X -> if (swapFaceButtons) PadAction.QUATERNARY else PadAction.TERTIARY
            KeyEvent.KEYCODE_BUTTON_Y -> if (swapFaceButtons) PadAction.TERTIARY else PadAction.QUATERNARY
            KeyEvent.KEYCODE_BUTTON_L1 -> PadAction.L1
            KeyEvent.KEYCODE_BUTTON_R1 -> PadAction.R1
            KeyEvent.KEYCODE_BUTTON_L2 -> PadAction.L2
            KeyEvent.KEYCODE_BUTTON_R2 -> PadAction.R2
            KeyEvent.KEYCODE_BUTTON_THUMBL -> PadAction.L3
            KeyEvent.KEYCODE_BUTTON_THUMBR -> PadAction.R3
            KeyEvent.KEYCODE_BUTTON_START -> PadAction.START
            KeyEvent.KEYCODE_BUTTON_SELECT -> PadAction.SELECT
            KeyEvent.KEYCODE_BUTTON_MODE -> PadAction.START
            KeyEvent.KEYCODE_DPAD_UP -> PadAction.UP
            KeyEvent.KEYCODE_DPAD_DOWN -> PadAction.DOWN
            KeyEvent.KEYCODE_DPAD_LEFT -> PadAction.LEFT
            KeyEvent.KEYCODE_DPAD_RIGHT -> PadAction.RIGHT
            // Keyboard fallbacks make the console UI testable without a controller.
            KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_SPACE, KeyEvent.KEYCODE_NUMPAD_ENTER -> PadAction.CONFIRM
            KeyEvent.KEYCODE_ESCAPE -> PadAction.BACK
            KeyEvent.KEYCODE_TAB -> PadAction.SELECT
            else -> null
        }
    }

    fun keyLabel(keyCode: Int): String = when (keyCode) {
        KeyEvent.KEYCODE_BUTTON_A -> "A"
        KeyEvent.KEYCODE_BUTTON_B -> "B"
        KeyEvent.KEYCODE_BUTTON_X -> "X"
        KeyEvent.KEYCODE_BUTTON_Y -> "Y"
        KeyEvent.KEYCODE_BUTTON_L1 -> "L1"
        KeyEvent.KEYCODE_BUTTON_R1 -> "R1"
        KeyEvent.KEYCODE_BUTTON_L2 -> "L2"
        KeyEvent.KEYCODE_BUTTON_R2 -> "R2"
        KeyEvent.KEYCODE_BUTTON_THUMBL -> "L3"
        KeyEvent.KEYCODE_BUTTON_THUMBR -> "R3"
        KeyEvent.KEYCODE_BUTTON_START -> "Start"
        KeyEvent.KEYCODE_BUTTON_SELECT -> "Select"
        KeyEvent.KEYCODE_BUTTON_MODE -> "Mode"
        KeyEvent.KEYCODE_DPAD_UP -> "D-Pad Up"
        KeyEvent.KEYCODE_DPAD_DOWN -> "D-Pad Down"
        KeyEvent.KEYCODE_DPAD_LEFT -> "D-Pad Left"
        KeyEvent.KEYCODE_DPAD_RIGHT -> "D-Pad Right"
        else -> KeyEvent.keyCodeToString(keyCode).removePrefix("KEYCODE_")
    }

    fun layoutFor(name: String, descriptor: String, vendorId: Int, overrideSwap: Boolean? = null): PadLayout {
        if (overrideSwap != null) return if (overrideSwap) PadLayout.NINTENDO else PadLayout.STANDARD
        val haystack = "$name $descriptor".lowercase()
        val nintendo = vendorId == 0x057E ||
            haystack.contains("nintendo") ||
            haystack.contains("switch pro") ||
            haystack.contains("joy-con")
        return if (nintendo) PadLayout.NINTENDO else PadLayout.STANDARD
    }
}
