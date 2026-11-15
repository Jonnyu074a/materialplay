package com.materialplay.launcher.controllerui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.materialplay.launcher.gamepad.ConnectedController
import com.materialplay.launcher.gamepad.PadAction

data class ProfileOption(val id: String, val label: String, val profile: GlyphProfile)

val ProfileOptions = listOf(
    ProfileOption("auto", "Automatic", GlyphProfile.XBOX),
    ProfileOption("XBOX", "Xbox", GlyphProfile.XBOX),
    ProfileOption("PLAYSTATION", "PlayStation", GlyphProfile.PLAYSTATION),
    ProfileOption("NINTENDO", "Nintendo", GlyphProfile.NINTENDO),
    ProfileOption("GENERIC", "Generic", GlyphProfile.GENERIC)
)

/**
 * Visual profile picker. "Automatic" follows the controller name/vendor; a manual
 * choice only changes glyphs, never which physical button triggers an action.
 */
@Composable
fun ProfileSelector(
    selectedId: String,
    controller: ConnectedController?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    includeAuto: Boolean = true
) {
    val options = if (includeAuto) ProfileOptions else ProfileOptions.drop(1)
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(options, key = { it.id }) { option ->
            val selected = selectedId.equals(option.id, ignoreCase = true)
            val preview = if (option.id == "auto") GlyphProfile.autoFor(controller) else option.profile
            Column(
                modifier = Modifier
                    .width(148.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (selected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                    .border(
                        width = if (selected) 2.dp else 1.dp,
                        color = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .clickable { onSelect(option.id) }
                    .padding(14.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PadGlyph(PadAction.CONFIRM, preview, size = 26.dp)
                    PadGlyph(PadAction.BACK, preview, size = 26.dp)
                    PadGlyph(PadAction.TERTIARY, preview, size = 26.dp)
                    PadGlyph(PadAction.QUATERNARY, preview, size = 26.dp)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = option.label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface
                )
                if (option.id == "auto") {
                    Text(
                        text = "Detected: ${preview.label}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/** Full legend used in Controller Appearance. */
@Composable
fun GlyphLegend(profile: GlyphProfile, modifier: Modifier = Modifier, columns: Int = 3) {
    val rows = listOf(
        PadAction.CONFIRM to "Confirm",
        PadAction.BACK to "Back",
        PadAction.TERTIARY to "Alternate",
        PadAction.QUATERNARY to "Secondary",
        PadAction.L1 to "Left bumper",
        PadAction.R1 to "Right bumper",
        PadAction.L2 to "Left trigger",
        PadAction.R2 to "Right trigger",
        PadAction.START to "Menu",
        PadAction.SELECT to "View",
        PadAction.L3 to "Left stick click",
        PadAction.R3 to "Right stick click",
        PadAction.UP to "D-Pad up",
        PadAction.LEFT to "D-Pad left",
        PadAction.DOWN to "D-Pad down",
        PadAction.RIGHT to "D-Pad right"
    )
    val chunked = rows.chunked(columns)
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        chunked.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { (action, label) ->
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PadGlyph(action, profile, size = 28.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (row.size < columns) {
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

/** Horizontal footer with "A Play", "B Back" style hints. */
@Composable
fun GlyphHintBar(
    hints: List<Pair<PadAction, String>>,
    profile: GlyphProfile,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        hints.forEach { (action, label) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                PadGlyph(action, profile, size = 24.dp)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = tint.copy(alpha = 0.85f)
                )
            }
        }
    }
}

/** Compact single hint used inside rows and empty states. */
@Composable
fun InlineHint(action: PadAction, label: String, profile: GlyphProfile, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(20.dp),
            contentAlignment = Alignment.Center
        ) { PadGlyph(action, profile, size = 20.dp) }
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
