package com.materialplay.launcher.ui.theme

import androidx.compose.ui.graphics.Color

val Ink = Color(0xFF05070C)
val InkElevated = Color(0xFF0B0F17)
val InkCard = Color(0xFF111823)
val InkOutline = Color(0xFF243040)
val Fog = Color(0xFF98A6BA)
val Snow = Color(0xFFF4F7FC)

val PlayStationBlue = Color(0xFF2F7BF6)
val Mint = Color(0xFF7DE2D1)
val Azure = Color(0xFF7FB7FF)
val Violet = Color(0xFFB49CFF)
val Amber = Color(0xFFFFC96B)
val Rose = Color(0xFFFF8FA8)
val Lime = Color(0xFFC8F56E)

data class AccentOption(val id: String, val label: String, val color: Color)

val AccentOptions = listOf(
    AccentOption("playstation", "PlayStation", PlayStationBlue),
    AccentOption("mint", "Mint", Mint),
    AccentOption("azure", "Azure", Azure),
    AccentOption("violet", "Violet", Violet),
    AccentOption("amber", "Amber", Amber),
    AccentOption("rose", "Rose", Rose),
    AccentOption("lime", "Lime", Lime)
)

fun accentFor(id: String): Color =
    AccentOptions.firstOrNull { it.id == id }?.color ?: PlayStationBlue