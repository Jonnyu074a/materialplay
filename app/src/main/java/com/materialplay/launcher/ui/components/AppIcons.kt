package com.materialplay.launcher.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

private fun icon(name: String, pathData: String): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = addPathNodes(pathData),
        fill = SolidColor(Color.White)
    ).build()

val GamepadIcon: ImageVector by lazy {
    icon(
        "Gamepad",
        "M7.2,6h9.6c2.0,0 3.7,1.5 4.0,3.5l0.9,6.5c0.2,1.6 -1.0,3.0 -2.6,3.0c-0.7,0 -1.3,-0.2 -1.8,-0.7L14.8,17H9.2l-2.5,2.3c-0.5,0.5 -1.1,0.7 -1.8,0.7c-1.6,0 -2.8,-1.4 -2.6,-3.0l0.9,-6.5C3.5,7.5 5.2,6 7.2,6zM10,9v2H8v2h2v2h2v-2h2v-2h-2V9H10zM16.5,9.2a1.3,1.3 0,1 0,0 2.6a1.3,1.3 0,1 0,0 -2.6zM18.8,11.8a1.1,1.1 0,1 0,0 2.2a1.1,1.1 0,1 0,0 -2.2z"
    )
}

val GridIcon: ImageVector by lazy {
    icon(
        "Grid",
        "M3,3h8v8H3zM13,3h8v8h-8zM3,13h8v8H3zM13,13h8v8h-8z"
    )
}

val ChartIcon: ImageVector by lazy {
    icon(
        "Chart",
        "M4,20h16v2H4zM6,10h3v8H6zM11,5h3v13h-3zM16,8h3v10h-3z"
    )
}

val SparkIcon: ImageVector by lazy {
    icon(
        "Spark",
        "M12,2l1.9,6.1L20,10l-6.1,1.9L12,18l-1.9,-6.1L4,10l6.1,-1.9zM19,16l0.8,2.2L22,19l-2.2,0.8L19,22l-0.8,-2.2L16,19l2.2,-0.8z"
    )
}
