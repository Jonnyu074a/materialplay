package com.materialplay.launcher.gaming

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.materialplay.launcher.controllerui.GlyphHintBar
import com.materialplay.launcher.controllerui.GlyphProfile
import com.materialplay.launcher.controllerui.PadGlyph
import com.materialplay.launcher.data.db.GameEntity
import com.materialplay.launcher.gamepad.PadAction
import com.materialplay.launcher.ui.components.GameArt
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

data class BatterySnapshot(val level: Int, val charging: Boolean)

@Composable
fun rememberBatterySnapshot(): BatterySnapshot {
    val context = LocalContext.current
    var snapshot by remember { mutableStateOf(readBattery(context)) }
    LaunchedEffect(context) {
        while (true) {
            snapshot = readBattery(context)
            delay(15_000)
        }
    }
    return snapshot
}

private fun readBattery(context: Context): BatterySnapshot {
    val intent = runCatching {
        context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }.getOrNull()
    val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
    val percent = if (level >= 0 && scale > 0) (level * 100f / scale).roundToInt() else 0
    val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
        status == BatteryManager.BATTERY_STATUS_FULL
    return BatterySnapshot(percent.coerceIn(0, 100), charging)
}

@Composable
fun rememberClock(): String {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000)
        }
    }
    return remember(now) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(now))
    }
}

/** Visible controller focus ring shared by every Gaming Mode surface. */
@Composable
fun Modifier.focusRing(
    focused: Boolean,
    shape: Shape,
    color: Color = MaterialTheme.colorScheme.primary
): Modifier = if (focused) {
    this.border(3.dp, color, shape)
} else {
    this
}

/** Full-screen modal backdrop with a card that remains readable at TV distance. */
@Composable
private fun OverlayScaffold(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.66f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.62f)
                .clickable { }
                .clip(RoundedCornerShape(30.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.98f))
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f),
                    RoundedCornerShape(30.dp)
                )
                .padding(26.dp)
        ) {
            content()
        }
    }
}

@Composable
fun QuickOverlay(
    selected: Int,
    controllerName: String?,
    battery: BatterySnapshot,
    clock: String,
    brightness: Float,
    onBrightness: (Float) -> Unit,
    volume: Int,
    maxVolume: Int,
    onVolume: (Int) -> Unit,
    onDismiss: () -> Unit,
    profile: GlyphProfile
) {
    OverlayScaffold(onDismiss = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "QUICK MENU",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text("Session controls", style = MaterialTheme.typography.headlineMedium)
                }
                Text(clock, style = MaterialTheme.typography.headlineLarge)
            }
            Text(
                controllerName ?: "Touch input",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(2.dp))

            QuickRow(selected = selected == 0, title = "Battery") {
                Text(
                    if (battery.charging) "${battery.level}% • charging" else "${battery.level}%",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            QuickRow(selected = selected == 1, title = "Brightness") {
                Column(modifier = Modifier.weight(1f)) {
                    Slider(
                        value = brightness.coerceIn(0.05f, 1f),
                        onValueChange = onBrightness,
                        valueRange = 0.05f..1f
                    )
                }
                Text("${(brightness * 100).roundToInt()}%", style = MaterialTheme.typography.labelLarge)
            }
            QuickRow(selected = selected == 2, title = "Volume") {
                Column(modifier = Modifier.weight(1f)) {
                    Slider(
                        value = volume.toFloat(),
                        onValueChange = { onVolume(it.roundToInt()) },
                        valueRange = 0f..maxVolume.coerceAtLeast(1).toFloat()
                    )
                }
                Text("$volume", style = MaterialTheme.typography.labelLarge)
            }
            QuickRow(selected = selected == 3, title = "Time") {
                Text(clock, style = MaterialTheme.typography.titleMedium)
            }
            QuickRow(selected = selected == 4, title = "Connection") {
                Text(
                    controllerName ?: "No controller connected",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            QuickRow(
                selected = selected == 5,
                title = "Close overlay",
                onClick = onDismiss
            ) {
                Text("Resume", style = MaterialTheme.typography.titleMedium)
            }

            GlyphHintBar(
                hints = listOf(
                    PadAction.CONFIRM to "Select",
                    PadAction.BACK to "Close"
                ),
                profile = profile,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun QuickRow(
    selected: Boolean,
    title: String,
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainerHigh
            )
            .focusRing(selected, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(112.dp)
        )
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            content()
        }
    }
}

@Composable
fun GamingMenuOverlay(
    title: String,
    body: String?,
    items: List<String>,
    selected: Int,
    profile: GlyphProfile,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    OverlayScaffold(onDismiss = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "GAMING MODE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(title, style = MaterialTheme.typography.headlineMedium)
            if (!body.isNullOrBlank()) {
                Text(
                    body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(6.dp))
            items.forEachIndexed { index, label ->
                val shape = RoundedCornerShape(16.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .background(
                            if (selected == index) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                        .focusRing(selected == index, shape)
                        .clickable { onSelect(index) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (selected == index) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            GlyphHintBar(
                hints = listOf(
                    PadAction.CONFIRM to "Select",
                    PadAction.BACK to "Back"
                ),
                profile = profile,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun IntroOverlay(controllerName: String?, profile: GlyphProfile) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.Black.copy(alpha = 0.9f),
                        Color.Black.copy(alpha = 0.72f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                PadGlyph(PadAction.CONFIRM, profile, size = 60.dp)
            }
            Text("GAMING MODE", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(
                controllerName ?: "Controller ready",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White
            )
            Text(
                "Use the D-pad or left stick to move, A to select, B to go back.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.72f)
            )
        }
    }
}

@Composable
fun GamingDetailsOverlay(
    game: GameEntity,
    totalPlaytime: Long,
    selected: Int,
    refreshing: Boolean,
    profile: GlyphProfile,
    onPlay: () -> Unit,
    onFavorite: () -> Unit,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit
) {
    val shape = RoundedCornerShape(30.dp)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.9f)
                .clickable { }
                .clip(shape)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
        ) {
            GameArt(
                game = game,
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.38f),
                hero = true,
                corner = 0.dp
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    if (game.isFavorite) "FAVORITE • IN YOUR LIBRARY" else "IN YOUR LIBRARY",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    game.label,
                    style = MaterialTheme.typography.headlineLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                val meta = listOfNotNull(
                    game.developer,
                    game.releaseDate?.take(4),
                    game.displayGenre,
                    if (totalPlaytime > 0) "${formatPlaytime(totalPlaytime)} tracked" else null
                ).joinToString("  •  ")
                if (meta.isNotBlank()) {
                    Text(
                        meta,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!game.description.isNullOrBlank()) {
                    Text(
                        game.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 8,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(6.dp))
                DetailAction("Play", selected == 0, enabled = game.installed, onClick = onPlay) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(24.dp))
                }
                DetailAction(
                    if (game.isFavorite) "Remove favorite" else "Add favorite",
                    selected == 1,
                    onClick = onFavorite
                )
                DetailAction(
                    if (refreshing) "Refreshing metadata…" else "Refresh metadata",
                    selected == 2,
                    enabled = !refreshing,
                    onClick = onRefresh
                )
                DetailAction("Back", selected == 3, onClick = onDismiss)
                Spacer(Modifier.height(6.dp))
                GlyphHintBar(
                    hints = listOf(
                        PadAction.CONFIRM to "Select",
                        PadAction.BACK to "Close"
                    ),
                    profile = profile,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DetailAction(
    label: String,
    focused: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
    leading: (@Composable () -> Unit)? = null
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                if (focused) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainerHigh
            )
            .focusRing(focused, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        leading?.invoke()
        Text(
            label,
            style = MaterialTheme.typography.titleMedium,
            color = when {
                !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                focused -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

private fun formatPlaytime(millis: Long): String {
    val minutes = millis / 60_000
    val hours = minutes / 60
    return when {
        hours > 0 -> "${hours}h ${minutes % 60}m"
        minutes > 0 -> "${minutes}m"
        else -> "<1m"
    }
}
