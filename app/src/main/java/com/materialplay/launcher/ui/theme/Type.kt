package com.materialplay.launcher.ui.theme

import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val PlayShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(22.dp)
)

private val base = Typography()

val PlayTypography = Typography(
    displayLarge = base.displayLarge.copy(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-1.2).sp
    ),
    displayMedium = base.displayMedium.copy(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.8).sp
    ),
    displaySmall = base.displaySmall.copy(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.5).sp
    ),
    headlineLarge = base.headlineLarge.copy(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.4).sp
    ),
    headlineMedium = base.headlineMedium.copy(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold
    ),
    titleLarge = base.titleLarge.copy(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.2).sp
    ),
    bodyLarge = base.bodyLarge.copy(
        fontFamily = FontFamily.SansSerif,
        lineHeight = 24.sp,
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None
        )
    ),
    labelLarge = base.labelLarge.copy(
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.2.sp
    )
)

/** Typography tuned for a 10-foot console interface. */
val GamingTypography = PlayTypography.copy(
    displayLarge = PlayTypography.displayLarge.copy(fontSize = 64.sp, lineHeight = 68.sp),
    headlineLarge = PlayTypography.headlineLarge.copy(fontSize = 36.sp, lineHeight = 42.sp),
    headlineMedium = PlayTypography.headlineMedium.copy(fontSize = 28.sp),
    titleLarge = PlayTypography.titleLarge.copy(fontSize = 24.sp),
    bodyLarge = PlayTypography.bodyLarge.copy(fontSize = 18.sp, lineHeight = 27.sp),
    labelLarge = PlayTypography.labelLarge.copy(fontSize = 15.sp)
)

val GamingTitleStyle = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.SemiBold,
    fontSize = 44.sp,
    lineHeight = 48.sp,
    letterSpacing = (-0.8).sp
)
