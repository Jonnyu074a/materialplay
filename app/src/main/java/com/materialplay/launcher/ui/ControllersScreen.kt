package com.materialplay.launcher.ui

import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.materialplay.launcher.controllerui.GlyphLegend
import com.materialplay.launcher.controllerui.GlyphProfile
import com.materialplay.launcher.controllerui.PadGlyph
import com.materialplay.launcher.controllerui.ProfileSelector
import com.materialplay.launcher.core.Text
import com.materialplay.launcher.data.db.ControllerEntity
import com.materialplay.launcher.gamepad.ConnectedController
import com.materialplay.launcher.gamepad.PadAction
import com.materialplay.launcher.gamepad.PadLayout
import com.materialplay.launcher.gamepad.RawPadState
import com.materialplay.launcher.ui.components.EmptyState
import com.materialplay.launcher.ui.components.GlassCard
import com.materialplay.launcher.ui.components.SectionHeader
import com.materialplay.launcher.ui.components.Tag
import kotlin.math.abs

@Composable
fun ControllersScreen(vm: LauncherViewModel) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val controllers by vm.controllers.collectAsStateWithLifecycle()
    val saved by vm.savedControllers.collectAsStateWithLifecycle()
    val raw by vm.rawInput.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Column {
                Text(
                    "CONTROLLERS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text("Pads and live input", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Android reports controllers as input devices. Connect the GameSir X5 Lite over USB-C or Bluetooth, then press a button to verify the mapping.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (controllers.isEmpty()) {
            item {
                GlassCard {
                    EmptyState(
                        title = "No controller connected",
                        body = "Pair a Bluetooth pad or plug in a USB-C controller. This screen updates as soon as Android reports the device."
                    )
                }
            }
        } else {
            items(controllers.size, key = { controllers[it].identity }) { index ->
                val controller = controllers[index]
                val stored = saved.firstOrNull { it.descriptor == controller.identity }
                ControllerCard(
                    vm = vm,
                    controller = controller,
                    stored = stored,
                    raw = if (controllers.size == 1) raw else RawPadState(),
                    globalProfile = settings.glyphProfile
                )
            }
        }

        if (saved.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Remembered pads",
                    subtitle = "Appearance and layout preferences are stored per device"
                )
            }
            items(saved.size, key = { saved[it].descriptor }) { index ->
                SavedControllerRow(saved[index])
            }
        }

        item {
            GlassCard {
                Column(Modifier.padding(18.dp)) {
                    Text("Live input test", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "The test reacts to the controller that sends events. Face-button glyphs follow the selected visual profile, which never changes the physical mapping.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    val profile = GlyphProfile.resolve(
                        settings.glyphProfile,
                        saved.firstOrNull()?.glyphProfile?.takeIf { it != "auto" },
                        controllers.firstOrNull()
                    )
                    GlyphLegend(profile = profile, columns = 3)
                }
            }
        }

        item {
            GlassCard {
                Column(Modifier.padding(18.dp)) {
                    Text("Connection notes", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "• USB-C: connect the pad, accept any Android prompt and press a face button.\n" +
                            "• Bluetooth: pair in system settings first; some pads expose two modes, choose the gamepad one.\n" +
                            "• If buttons look swapped, set the face-button layout below or use the Nintendo override.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ControllerCard(
    vm: LauncherViewModel,
    controller: ConnectedController,
    stored: ControllerEntity?,
    raw: RawPadState,
    globalProfile: String
) {
    val profile = GlyphProfile.resolve(globalProfile, stored?.glyphProfile, controller)
    val layout = stored?.faceSwap?.let { if (it) PadLayout.NINTENDO else PadLayout.STANDARD }
        ?: controller.layout
    val swap = layout == PadLayout.NINTENDO

    GlassCard {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    PadGlyph(PadAction.CONFIRM, profile, size = 30.dp)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        controller.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1
                    )
                    Text(
                        "${controller.connection.label} • ${controller.vendorLabel ?: "Unknown vendor"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Tag("LIVE")
            }

            Spacer(Modifier.height(12.dp))
            val identity = listOfNotNull(
                "VID ${controller.vendorIdHex}",
                "PID ${controller.productIdHex}",
                if (controller.hasGamepadSource) "Gamepad" else "Joystick",
                if (swap) "Nintendo layout" else "Standard layout"
            ).joinToString("  •  ")
            Text(identity, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                controller.descriptor,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )

            Spacer(Modifier.height(16.dp))
            Text("Glyph appearance", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            ProfileSelector(
                selectedId = stored?.glyphProfile ?: "auto",
                controller = controller,
                onSelect = { vm.setControllerProfile(controller.identity, it) }
            )

            Spacer(Modifier.height(14.dp))
            Text("Physical face-button layout", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = stored?.faceSwap == null,
                    onClick = { vm.setControllerFaceSwap(controller.identity, null) },
                    label = { Text("Auto") }
                )
                FilterChip(
                    selected = stored?.faceSwap == false,
                    onClick = { vm.setControllerFaceSwap(controller.identity, false) },
                    label = { Text("Standard") }
                )
                FilterChip(
                    selected = stored?.faceSwap == true,
                    onClick = { vm.setControllerFaceSwap(controller.identity, true) },
                    label = { Text("Nintendo") }
                )
            }
            Text(
                if (swap) "A/B confirm and back are swapped to match the physical labels."
                else "A confirms, B goes back.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(16.dp))
            Text("Live buttons", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            if (raw.updatedAt == 0L) {
                Text(
                    "No input seen yet — press any button or move a stick.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val actions = PadAction.entries.chunked(4)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    actions.forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { action ->
                                val pressed = action in raw.actions
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (pressed) MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.surfaceContainerHigh
                                        )
                                        .padding(vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    PadGlyph(action, profile, size = 26.dp, muted = !pressed)
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        action.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (pressed) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (row.size < 4) repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Axes", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(6.dp))
            if (raw.axes.isEmpty()) {
                Text(
                    "Sticks are centered.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    raw.axes.entries.sortedBy { it.key }.forEach { (axis, value) ->
                        AxisRow(label = axisLabel(axis), value = value)
                    }
                }
            }
        }
    }
}

@Composable
private fun AxisRow(label: String, value: Float) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.width(88.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(abs(value).coerceIn(0f, 1f))
                    .height(8.dp)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = "%+.2f".format(value),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(48.dp)
        )
    }
}

@Composable
private fun SavedControllerRow(controller: ControllerEntity) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(controller.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(
                "${controller.connectionType.lowercase()} • last seen ${Text.relative(controller.lastSeen)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                if (controller.vendorId == 0 && controller.productId == 0) controller.descriptor
                else "VID 0x%04X • PID 0x%04X".format(controller.vendorId, controller.productId),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
        Tag(controller.glyphProfile)
    }
}

private fun axisLabel(axis: Int): String = when (axis) {
    MotionEvent.AXIS_X -> "Left X"
    MotionEvent.AXIS_Y -> "Left Y"
    MotionEvent.AXIS_Z -> "Right X"
    MotionEvent.AXIS_RZ -> "Right Y"
    MotionEvent.AXIS_RX -> "RX"
    MotionEvent.AXIS_RY -> "RY"
    MotionEvent.AXIS_HAT_X -> "D-Pad X"
    MotionEvent.AXIS_HAT_Y -> "D-Pad Y"
    MotionEvent.AXIS_LTRIGGER -> "L2"
    MotionEvent.AXIS_RTRIGGER -> "R2"
    MotionEvent.AXIS_BRAKE -> "Brake"
    MotionEvent.AXIS_GAS -> "Gas"
    else -> "Axis $axis"
}
