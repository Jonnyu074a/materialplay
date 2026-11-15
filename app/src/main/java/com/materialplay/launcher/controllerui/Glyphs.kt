package com.materialplay.launcher.controllerui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.materialplay.launcher.gamepad.ConnectedController
import com.materialplay.launcher.gamepad.PadAction

/** Visual families are independent from the physical controller mapping. */
enum class GlyphProfile(val label: String) {
    XBOX("Xbox"),
    PLAYSTATION("PlayStation"),
    NINTENDO("Nintendo"),
    GENERIC("Generic");

    companion object {
        fun fromId(id: String?): GlyphProfile? = entries.firstOrNull { it.name.equals(id, true) }

        fun autoFor(controller: ConnectedController?): GlyphProfile {
            if (controller == null) return XBOX
            val name = controller.name.lowercase()
            val vendor = controller.vendorId
            return when {
                vendor == 0x054C || name.contains("dualsense") || name.contains("dualshock") ||
                    name.contains("playstation") || name.contains("sony") -> PLAYSTATION
                vendor == 0x057E || name.contains("nintendo") || name.contains("switch") ||
                    name.contains("joy-con") -> NINTENDO
                vendor == 0x045E || name.contains("xbox") || name.contains("microsoft") -> XBOX
                else -> GENERIC
            }
        }

        /**
         * Resolution order: per-controller override, then the global setting, then
         * automatic detection from the active controller.
         */
        fun resolve(
            globalPreference: String,
            controllerPreference: String?,
            controller: ConnectedController?
        ): GlyphProfile {
            fromId(controllerPreference)?.let { return it }
            if (!globalPreference.equals("auto", true)) {
                fromId(globalPreference)?.let { return it }
            }
            return autoFor(controller)
        }
    }
}

private enum class GlyphKind {
    BUTTON, PS_SYMBOL, PILL, TRIGGER, DPAD, MENU, VIEW, STICK, PLUS, MINUS, CHEVRON, CHECK, PLAY
}

private data class GlyphStyle(
    val kind: GlyphKind,
    val background: Color = Color.Transparent,
    val border: Color? = null,
    val content: Color,
    val letters: String = "",
    val rotation: Float = 0f
)

private val XboxGreen = Color(0xFF6DBE45)
private val XboxRed = Color(0xFFE5484D)
private val XboxBlue = Color(0xFF3E8BFF)
private val XboxYellow = Color(0xFFF2C33C)
private val PsCross = Color(0xFF8FC2FF)
private val PsCircle = Color(0xFFFF7F8A)
private val PsSquare = Color(0xFFFF9DD0)
private val PsTriangle = Color(0xFF7BE4BC)

private fun styleFor(profile: GlyphProfile, action: PadAction, accent: Color, onSurface: Color): GlyphStyle {
    val darkButton = Color(0xFF1A1F27)
    val lightContent = Color(0xFFF4F7FB)
    val shoulderBg = Color(0xFF232932)
    val shoulderBorder = Color(0xFF47515F)
    val genericBg = Color(0xFF262C35)

    fun chevron(rotation: Float) = GlyphStyle(
        GlyphKind.CHEVRON,
        border = Color(0xFF556172),
        content = onSurface,
        rotation = rotation
    )

    return when (profile) {
        GlyphProfile.XBOX -> when (action) {
            PadAction.CONFIRM -> GlyphStyle(GlyphKind.BUTTON, XboxGreen, null, Color.White, "A")
            PadAction.BACK -> GlyphStyle(GlyphKind.BUTTON, XboxRed, null, Color.White, "B")
            PadAction.TERTIARY -> GlyphStyle(GlyphKind.BUTTON, XboxBlue, null, Color.White, "X")
            PadAction.QUATERNARY -> GlyphStyle(GlyphKind.BUTTON, XboxYellow, null, Color(0xFF241C05), "Y")
            PadAction.L1 -> GlyphStyle(GlyphKind.PILL, shoulderBg, shoulderBorder, lightContent, "LB")
            PadAction.R1 -> GlyphStyle(GlyphKind.PILL, shoulderBg, shoulderBorder, lightContent, "RB")
            PadAction.L2 -> GlyphStyle(GlyphKind.TRIGGER, shoulderBg, shoulderBorder, lightContent, "LT")
            PadAction.R2 -> GlyphStyle(GlyphKind.TRIGGER, shoulderBg, shoulderBorder, lightContent, "RT")
            PadAction.START -> GlyphStyle(GlyphKind.MENU, content = onSurface)
            PadAction.SELECT -> GlyphStyle(GlyphKind.VIEW, content = onSurface)
            PadAction.L3, PadAction.R3 -> GlyphStyle(GlyphKind.STICK, border = shoulderBorder, content = onSurface)
            PadAction.UP -> chevron(-90f)
            PadAction.DOWN -> chevron(90f)
            PadAction.LEFT -> chevron(180f)
            PadAction.RIGHT -> chevron(0f)
        }

        GlyphProfile.PLAYSTATION -> when (action) {
            PadAction.CONFIRM -> GlyphStyle(GlyphKind.PS_SYMBOL, darkButton, PsCross.copy(alpha = 0.6f), PsCross, "X")
            PadAction.BACK -> GlyphStyle(GlyphKind.PS_SYMBOL, darkButton, PsCircle.copy(alpha = 0.6f), PsCircle, "O")
            PadAction.TERTIARY -> GlyphStyle(GlyphKind.PS_SYMBOL, darkButton, PsSquare.copy(alpha = 0.6f), PsSquare, "S")
            PadAction.QUATERNARY -> GlyphStyle(GlyphKind.PS_SYMBOL, darkButton, PsTriangle.copy(alpha = 0.6f), PsTriangle, "T")
            PadAction.L1 -> GlyphStyle(GlyphKind.PILL, shoulderBg, shoulderBorder, lightContent, "L1")
            PadAction.R1 -> GlyphStyle(GlyphKind.PILL, shoulderBg, shoulderBorder, lightContent, "R1")
            PadAction.L2 -> GlyphStyle(GlyphKind.TRIGGER, shoulderBg, shoulderBorder, lightContent, "L2")
            PadAction.R2 -> GlyphStyle(GlyphKind.TRIGGER, shoulderBg, shoulderBorder, lightContent, "R2")
            PadAction.START -> GlyphStyle(GlyphKind.MENU, content = onSurface)
            PadAction.SELECT -> GlyphStyle(GlyphKind.VIEW, content = onSurface)
            PadAction.L3, PadAction.R3 -> GlyphStyle(GlyphKind.STICK, border = shoulderBorder, content = onSurface)
            PadAction.UP -> chevron(-90f)
            PadAction.DOWN -> chevron(90f)
            PadAction.LEFT -> chevron(180f)
            PadAction.RIGHT -> chevron(0f)
        }

        GlyphProfile.NINTENDO -> when (action) {
            PadAction.CONFIRM -> GlyphStyle(GlyphKind.BUTTON, darkButton, Color(0xFF59636F), lightContent, "A")
            PadAction.BACK -> GlyphStyle(GlyphKind.BUTTON, darkButton, Color(0xFF59636F), lightContent, "B")
            PadAction.TERTIARY -> GlyphStyle(GlyphKind.BUTTON, darkButton, Color(0xFF59636F), lightContent, "X")
            PadAction.QUATERNARY -> GlyphStyle(GlyphKind.BUTTON, darkButton, Color(0xFF59636F), lightContent, "Y")
            PadAction.L1 -> GlyphStyle(GlyphKind.PILL, shoulderBg, shoulderBorder, lightContent, "L")
            PadAction.R1 -> GlyphStyle(GlyphKind.PILL, shoulderBg, shoulderBorder, lightContent, "R")
            PadAction.L2 -> GlyphStyle(GlyphKind.TRIGGER, shoulderBg, shoulderBorder, lightContent, "ZL")
            PadAction.R2 -> GlyphStyle(GlyphKind.TRIGGER, shoulderBg, shoulderBorder, lightContent, "ZR")
            PadAction.START -> GlyphStyle(GlyphKind.PLUS, content = onSurface)
            PadAction.SELECT -> GlyphStyle(GlyphKind.MINUS, content = onSurface)
            PadAction.L3, PadAction.R3 -> GlyphStyle(GlyphKind.STICK, border = shoulderBorder, content = onSurface)
            PadAction.UP -> chevron(-90f)
            PadAction.DOWN -> chevron(90f)
            PadAction.LEFT -> chevron(180f)
            PadAction.RIGHT -> chevron(0f)
        }

        GlyphProfile.GENERIC -> when (action) {
            PadAction.CONFIRM -> GlyphStyle(GlyphKind.CHECK, genericBg, shoulderBorder, accent)
            PadAction.BACK -> GlyphStyle(GlyphKind.CHEVRON, genericBg, shoulderBorder, onSurface, rotation = 180f)
            PadAction.TERTIARY -> GlyphStyle(GlyphKind.PS_SYMBOL, genericBg, shoulderBorder, onSurface, "S")
            PadAction.QUATERNARY -> GlyphStyle(GlyphKind.PS_SYMBOL, genericBg, shoulderBorder, onSurface, "T")
            PadAction.L1 -> GlyphStyle(GlyphKind.PILL, genericBg, shoulderBorder, lightContent, "L")
            PadAction.R1 -> GlyphStyle(GlyphKind.PILL, genericBg, shoulderBorder, lightContent, "R")
            PadAction.L2 -> GlyphStyle(GlyphKind.TRIGGER, genericBg, shoulderBorder, lightContent, "L2")
            PadAction.R2 -> GlyphStyle(GlyphKind.TRIGGER, genericBg, shoulderBorder, lightContent, "R2")
            PadAction.START -> GlyphStyle(GlyphKind.MENU, content = onSurface)
            PadAction.SELECT -> GlyphStyle(GlyphKind.VIEW, content = onSurface)
            PadAction.L3, PadAction.R3 -> GlyphStyle(GlyphKind.STICK, border = shoulderBorder, content = onSurface)
            PadAction.UP -> chevron(-90f)
            PadAction.DOWN -> chevron(90f)
            PadAction.LEFT -> chevron(180f)
            PadAction.RIGHT -> chevron(0f)
        }
    }
}

@Composable
fun PadGlyph(
    action: PadAction,
    profile: GlyphProfile,
    modifier: Modifier = Modifier,
    size: Dp = 30.dp,
    muted: Boolean = false
) {
    val accent = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val style = styleFor(profile, action, accent, onSurface)
    Canvas(modifier = modifier.size(size)) {
        drawGlyph(style, muted)
    }
}

@Composable
fun PadGlyphStyle(
    profile: GlyphProfile,
    action: PadAction,
    size: Dp = 30.dp,
    muted: Boolean = false
) {
    PadGlyph(action = action, profile = profile, size = size, muted = muted)
}

private fun DrawScope.drawGlyph(style: GlyphStyle, muted: Boolean) {
    val s = this.size.minDimension
    val alpha = if (muted) 0.42f else 1f
    val box = Rect(s * 0.04f, s * 0.04f, s * 0.96f, s * 0.96f)
    val w = box.width
    val stroke = w * 0.085f

    when (style.kind) {
        GlyphKind.BUTTON, GlyphKind.PILL, GlyphKind.TRIGGER -> {
            if (style.kind == GlyphKind.BUTTON) {
                drawCircle(style.background.copy(alpha = alpha), radius = w / 2f, center = box.center)
                style.border?.let {
                    drawCircle(it.copy(alpha = alpha), radius = w / 2f, center = box.center, style = Stroke(w * 0.055f))
                }
            }
            if (style.kind == GlyphKind.PILL) {
                val height = w * 0.66f
                val top = box.top + (w - height) / 2f
                drawRoundRect(
                    style.background.copy(alpha = alpha),
                    topLeft = Offset(box.left, top),
                    size = Size(w, height),
                    cornerRadius = CornerRadius(height / 2f)
                )
                style.border?.let {
                    drawRoundRect(
                        it.copy(alpha = alpha),
                        topLeft = Offset(box.left, top),
                        size = Size(w, height),
                        cornerRadius = CornerRadius(height / 2f),
                        style = Stroke(w * 0.045f)
                    )
                }
            }
            if (style.kind == GlyphKind.TRIGGER) {
                val path = Path().apply {
                    moveTo(box.left + w * 0.12f, box.top + w * 0.18f)
                    quadraticTo(box.left + w * 0.1f, box.top + w * 0.04f, box.left + w * 0.26f, box.top + w * 0.04f)
                    lineTo(box.left + w * 0.74f, box.top + w * 0.04f)
                    quadraticTo(box.left + w * 0.9f, box.top + w * 0.04f, box.left + w * 0.88f, box.top + w * 0.18f)
                    lineTo(box.left + w * 0.76f, box.top + w * 0.9f)
                    quadraticTo(box.left + w * 0.74f, box.top + w * 0.97f, box.left + w * 0.64f, box.top + w * 0.97f)
                    lineTo(box.left + w * 0.36f, box.top + w * 0.97f)
                    quadraticTo(box.left + w * 0.26f, box.top + w * 0.97f, box.left + w * 0.24f, box.top + w * 0.9f)
                    close()
                }
                drawPath(path, style.background.copy(alpha = alpha))
                style.border?.let {
                    drawPath(path, it.copy(alpha = alpha), style = Stroke(w * 0.04f))
                }
            }
            drawLetters(style.letters, box, style.content.copy(alpha = alpha), w)
        }

        GlyphKind.PS_SYMBOL -> {
            drawCircle(style.background.copy(alpha = alpha), radius = w / 2f, center = box.center)
            style.border?.let {
                drawCircle(it.copy(alpha = alpha), radius = w / 2f - w * 0.035f, center = box.center, style = Stroke(w * 0.035f))
            }
            val symbolBox = box.deflate(w * 0.27f)
            when (style.letters) {
                "X" -> {
                    drawLine(style.content.copy(alpha = alpha), symbolBox.topLeft, symbolBox.bottomRight, stroke, StrokeCap.Round)
                    drawLine(style.content.copy(alpha = alpha), symbolBox.topRight, symbolBox.bottomLeft, stroke, StrokeCap.Round)
                }
                "O" -> drawCircle(
                    style.content.copy(alpha = alpha),
                    radius = symbolBox.width / 2f,
                    center = symbolBox.center,
                    style = Stroke(stroke)
                )
                "S" -> drawRoundRect(
                    style.content.copy(alpha = alpha),
                    topLeft = symbolBox.topLeft,
                    size = Size(symbolBox.width, symbolBox.height),
                    cornerRadius = CornerRadius(w * 0.035f),
                    style = Stroke(stroke)
                )
                "T" -> {
                    val path = Path().apply {
                        moveTo(symbolBox.center.x, symbolBox.top)
                        lineTo(symbolBox.right, symbolBox.bottom)
                        lineTo(symbolBox.left, symbolBox.bottom)
                        close()
                    }
                    drawPath(path, style.content.copy(alpha = alpha), style = Stroke(stroke, join = StrokeJoin.Round))
                }
                else -> drawLetters(style.letters, symbolBox, style.content.copy(alpha = alpha), w)
            }
        }

        GlyphKind.DPAD -> {
            val arm = w * 0.2f
            val path = Path().apply {
                moveTo(box.left + w * 0.34f, box.top + w * 0.08f)
                lineTo(box.left + w * 0.66f, box.top + w * 0.08f)
                lineTo(box.left + w * 0.66f, box.top + w * 0.34f)
                lineTo(box.left + w * 0.92f, box.top + w * 0.34f)
                lineTo(box.left + w * 0.92f, box.top + w * 0.66f)
                lineTo(box.left + w * 0.66f, box.top + w * 0.66f)
                lineTo(box.left + w * 0.66f, box.top + w * 0.92f)
                lineTo(box.left + w * 0.34f, box.top + w * 0.92f)
                lineTo(box.left + w * 0.34f, box.top + w * 0.66f)
                lineTo(box.left + w * 0.08f, box.top + w * 0.66f)
                lineTo(box.left + w * 0.08f, box.top + w * 0.34f)
                lineTo(box.left + w * 0.34f, box.top + w * 0.34f)
                close()
            }
            drawPath(path, style.content.copy(alpha = alpha))
            drawCircle(
                style.border?.copy(alpha = alpha) ?: Color.Transparent,
                radius = arm * 0.28f,
                center = box.center
            )
        }

        GlyphKind.MENU -> {
            val barWidth = w * 0.62f
            val barHeight = w * 0.1f
            val gap = w * 0.16f
            val left = box.left + (w - barWidth) / 2f
            repeat(3) { index ->
                drawRoundRect(
                    style.content.copy(alpha = alpha),
                    topLeft = Offset(left, box.center.y - gap - barHeight + index * gap),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(barHeight / 2f)
                )
            }
        }

        GlyphKind.VIEW -> {
            val rectWidth = w * 0.3f
            val rectHeight = w * 0.36f
            val strokeWidth = w * 0.07f
            drawRoundRect(
                style.content.copy(alpha = alpha),
                topLeft = Offset(box.center.x - rectWidth * 0.95f, box.center.y - rectHeight * 0.45f),
                size = Size(rectWidth, rectHeight),
                cornerRadius = CornerRadius(w * 0.045f),
                style = Stroke(strokeWidth)
            )
            drawRoundRect(
                style.content.copy(alpha = alpha),
                topLeft = Offset(box.center.x - rectWidth * 0.05f, box.center.y - rectHeight * 0.55f),
                size = Size(rectWidth, rectHeight),
                cornerRadius = CornerRadius(w * 0.045f),
                style = Stroke(strokeWidth)
            )
        }

        GlyphKind.STICK -> {
            drawCircle(
                style.content.copy(alpha = alpha),
                radius = w * 0.27f,
                center = box.center,
                style = Stroke(w * 0.065f)
            )
            drawCircle(style.content.copy(alpha = alpha * 0.7f), radius = w * 0.09f, center = box.center)
            val tick = w * 0.09f
            drawLine(style.content.copy(alpha = alpha * 0.7f), Offset(box.center.x, box.top + w * 0.06f), Offset(box.center.x, box.top + w * 0.06f + tick), w * 0.05f, StrokeCap.Round)
            drawLine(style.content.copy(alpha = alpha * 0.7f), Offset(box.center.x, box.bottom - w * 0.06f), Offset(box.center.x, box.bottom - w * 0.06f - tick), w * 0.05f, StrokeCap.Round)
            drawLine(style.content.copy(alpha = alpha * 0.7f), Offset(box.left + w * 0.06f, box.center.y), Offset(box.left + w * 0.06f + tick, box.center.y), w * 0.05f, StrokeCap.Round)
            drawLine(style.content.copy(alpha = alpha * 0.7f), Offset(box.right - w * 0.06f, box.center.y), Offset(box.right - w * 0.06f - tick, box.center.y), w * 0.05f, StrokeCap.Round)
        }

        GlyphKind.PLUS -> {
            drawLine(style.content.copy(alpha = alpha), Offset(box.center.x, box.top + w * 0.22f), Offset(box.center.x, box.bottom - w * 0.22f), w * 0.1f, StrokeCap.Round)
            drawLine(style.content.copy(alpha = alpha), Offset(box.left + w * 0.22f, box.center.y), Offset(box.right - w * 0.22f, box.center.y), w * 0.1f, StrokeCap.Round)
        }

        GlyphKind.MINUS -> drawLine(
            style.content.copy(alpha = alpha),
            Offset(box.left + w * 0.22f, box.center.y),
            Offset(box.right - w * 0.22f, box.center.y),
            w * 0.1f,
            StrokeCap.Round
        )

        GlyphKind.CHEVRON -> {
            rotate(style.rotation, box.center) {
                val path = Path().apply {
                    moveTo(box.left + w * 0.34f, box.top + w * 0.24f)
                    lineTo(box.left + w * 0.62f, box.center.y)
                    lineTo(box.left + w * 0.34f, box.bottom - w * 0.24f)
                }
                drawPath(path, style.content.copy(alpha = alpha), style = Stroke(w * 0.11f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }

        GlyphKind.CHECK -> {
            if (style.background != Color.Transparent) {
                drawCircle(style.background.copy(alpha = alpha), radius = w / 2f, center = box.center)
                style.border?.let {
                    drawCircle(it.copy(alpha = alpha), radius = w / 2f, center = box.center, style = Stroke(w * 0.045f))
                }
            }
            val path = Path().apply {
                moveTo(box.left + w * 0.24f, box.center.y)
                lineTo(box.left + w * 0.44f, box.center.y + w * 0.2f)
                lineTo(box.left + w * 0.78f, box.top + w * 0.3f)
            }
            drawPath(path, style.content.copy(alpha = alpha), style = Stroke(w * 0.1f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }

        GlyphKind.PLAY -> {
            val path = Path().apply {
                moveTo(box.left + w * 0.34f, box.top + w * 0.2f)
                lineTo(box.left + w * 0.34f, box.bottom - w * 0.2f)
                lineTo(box.right - w * 0.2f, box.center.y)
                close()
            }
            drawPath(path, style.content.copy(alpha = alpha))
        }
    }
}

private fun Rect.deflate(amount: Float): Rect = Rect(left + amount, top + amount, right - amount, bottom - amount)

private fun DrawScope.drawLetters(letters: String, box: Rect, color: Color, s: Float) {
    if (letters.isBlank()) return
    val stroke = s * 0.085f
    if (letters.length == 1) {
        val width = s * 0.46f
        val height = s * 0.56f
        drawLetter(
            letters[0],
            box.center.x - width / 2f,
            box.center.y - height / 2f,
            width,
            height,
            color,
            stroke
        )
    } else {
        val width = s * 0.27f
        val height = s * 0.46f
        val gap = s * 0.08f
        val total = width * 2 + gap
        val start = box.center.x - total / 2f
        letters.take(2).forEachIndexed { index, char ->
            drawLetter(
                char,
                start + index * (width + gap),
                box.center.y - height / 2f,
                width,
                height,
                color,
                stroke
            )
        }
    }
}

/** Letters are drawn as vector paths, never as font glyphs. */
private fun DrawScope.drawLetter(
    letter: Char,
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    color: Color,
    strokeWidth: Float
) {
    fun x(v: Float) = left + v * width
    fun y(v: Float) = top + v * height
    val path = Path()
    when (letter.uppercaseChar()) {
        'A' -> {
            path.moveTo(x(0.06f), y(0.92f)); path.lineTo(x(0.5f), y(0.08f)); path.lineTo(x(0.94f), y(0.92f))
            path.moveTo(x(0.24f), y(0.62f)); path.lineTo(x(0.76f), y(0.62f))
        }
        'B' -> {
            path.moveTo(x(0.18f), y(0.08f)); path.lineTo(x(0.18f), y(0.92f))
            path.moveTo(x(0.18f), y(0.1f)); path.lineTo(x(0.5f), y(0.1f))
            path.cubicTo(x(0.82f), y(0.1f), x(0.82f), y(0.46f), x(0.5f), y(0.48f))
            path.lineTo(x(0.18f), y(0.48f))
            path.moveTo(x(0.18f), y(0.48f)); path.lineTo(x(0.56f), y(0.48f))
            path.cubicTo(x(0.9f), y(0.5f), x(0.9f), y(0.9f), x(0.56f), y(0.9f))
            path.lineTo(x(0.18f), y(0.9f))
        }
        'X' -> {
            path.moveTo(x(0.12f), y(0.1f)); path.lineTo(x(0.88f), y(0.9f))
            path.moveTo(x(0.88f), y(0.1f)); path.lineTo(x(0.12f), y(0.9f))
        }
        'Y' -> {
            path.moveTo(x(0.08f), y(0.1f)); path.lineTo(x(0.5f), y(0.52f)); path.lineTo(x(0.92f), y(0.1f))
            path.moveTo(x(0.5f), y(0.52f)); path.lineTo(x(0.5f), y(0.92f))
        }
        'L' -> {
            path.moveTo(x(0.28f), y(0.08f)); path.lineTo(x(0.28f), y(0.92f)); path.lineTo(x(0.86f), y(0.92f))
        }
        'R' -> {
            path.moveTo(x(0.2f), y(0.08f)); path.lineTo(x(0.2f), y(0.92f))
            path.moveTo(x(0.2f), y(0.1f)); path.lineTo(x(0.56f), y(0.1f))
            path.cubicTo(x(0.88f), y(0.1f), x(0.88f), y(0.5f), x(0.56f), y(0.5f))
            path.lineTo(x(0.2f), y(0.5f))
            path.moveTo(x(0.52f), y(0.5f)); path.lineTo(x(0.88f), y(0.92f))
        }
        'T' -> {
            path.moveTo(x(0.08f), y(0.1f)); path.lineTo(x(0.92f), y(0.1f))
            path.moveTo(x(0.5f), y(0.1f)); path.lineTo(x(0.5f), y(0.92f))
        }
        '1' -> {
            path.moveTo(x(0.24f), y(0.26f)); path.lineTo(x(0.52f), y(0.08f)); path.lineTo(x(0.52f), y(0.92f))
            path.moveTo(x(0.26f), y(0.92f)); path.lineTo(x(0.8f), y(0.92f))
        }
        '2' -> {
            path.moveTo(x(0.14f), y(0.28f))
            path.cubicTo(x(0.18f), y(0.02f), x(0.86f), y(0.02f), x(0.84f), y(0.36f))
            path.cubicTo(x(0.82f), y(0.58f), x(0.42f), y(0.7f), x(0.16f), y(0.92f))
            path.lineTo(x(0.88f), y(0.92f))
        }
        else -> {
            path.moveTo(x(0.2f), y(0.5f)); path.lineTo(x(0.8f), y(0.5f))
        }
    }
    drawPath(
        path,
        color,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}
