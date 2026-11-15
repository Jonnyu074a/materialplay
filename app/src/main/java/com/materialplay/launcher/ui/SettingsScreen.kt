package com.materialplay.launcher.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.materialplay.launcher.BuildConfig
import com.materialplay.launcher.controllerui.ProfileSelector
import com.materialplay.launcher.data.settings.AppLanguage
import com.materialplay.launcher.data.settings.BackgroundStyle
import com.materialplay.launcher.data.settings.GamingDensity
import com.materialplay.launcher.data.settings.LibraryLayout
import com.materialplay.launcher.data.settings.OverlayCombo
import com.materialplay.launcher.data.settings.UiScale
import com.materialplay.launcher.data.settings.SortMode
import com.materialplay.launcher.data.settings.ThemeMode
import com.materialplay.launcher.ui.components.GlassCard
import com.materialplay.launcher.ui.theme.AccentOptions

@Composable
fun SettingsScreen(vm: LauncherViewModel, onStartGaming: () -> Unit) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var overlayGranted by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var confirmClear by rememberSaveable { mutableStateOf(false) }

    LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
        overlayGranted = Settings.canDrawOverlays(context)
    }

    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    var rawg by remember(settings.rawgKey) { mutableStateOf(settings.rawgKey) }
    var igdbId by remember(settings.igdbClientId) { mutableStateOf(settings.igdbClientId) }
    var igdbSecret by remember(settings.igdbSecret) { mutableStateOf(settings.igdbSecret) }
    var sgdb by remember(settings.sgdbKey) { mutableStateOf(settings.sgdbKey) }
    var keysSaved by remember { mutableStateOf(false) }

    fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            runCatching { notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                "SETTINGS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text("Make it yours", style = MaterialTheme.typography.headlineMedium)
        }

        SettingsCard(title = "Appearance") {
            FieldLabel("Theme")
            ChipRow(
                options = ThemeMode.entries,
                selected = settings.themeMode,
                label = { mode -> mode.name.lowercase().replaceFirstChar { it.uppercase() } },
                onSelect = { mode -> vm.updateSettings { it.copy(themeMode = mode) } }
            )

            Spacer(Modifier.height(14.dp))
            FieldLabel("Language")
            ChipRow(
                options = AppLanguage.entries,
                selected = settings.language,
                label = { it.label },
                onSelect = { lang ->
                    vm.updateSettings { it.copy(language = lang) }
                    (context as? android.app.Activity)?.recreate()
                }
            )

            Spacer(Modifier.height(14.dp))
            FieldLabel("UI scale")
            ChipRow(
                options = UiScale.entries,
                selected = settings.uiScale,
                label = { it.label },
                onSelect = { scale -> vm.updateSettings { it.copy(uiScale = scale) } }
            )

            Spacer(Modifier.height(14.dp))
            FieldLabel("Accent")
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                AccentOptions.forEach { option ->
                    val selected = option.id == settings.accentId
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            vm.updateSettings { it.copy(accentId = option.id) }
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(option.color)
                                .border(
                                    width = if (selected) 3.dp else 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.onBackground
                                    else MaterialTheme.colorScheme.outlineVariant,
                                    shape = CircleShape
                                )
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            option.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            ToggleRow(
                title = "Dynamic colour",
                subtitle = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    "Follow the Android wallpaper palette (Android 12+)"
                } else {
                    "Requires Android 12 or newer"
                },
                checked = settings.useDynamicColor,
                enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                onChange = { checked -> vm.updateSettings { it.copy(useDynamicColor = checked) } }
            )

            Spacer(Modifier.height(6.dp))
            FieldLabel("Background style")
            ChipRow(
                options = BackgroundStyle.entries,
                selected = settings.backgroundStyle,
                label = { style -> style.name.lowercase().replaceFirstChar { it.uppercase() } },
                onSelect = { style -> vm.updateSettings { it.copy(backgroundStyle = style) } }
            )

            Spacer(Modifier.height(14.dp))
            SliderRow(
                title = "Cover size",
                valueLabel = "${(settings.coverScale * 100).toInt()}%",
                value = settings.coverScale,
                range = 0.7f..1.5f,
                onChange = { value -> vm.updateSettings { it.copy(coverScale = value) } }
            )
            SliderRow(
                title = "Backdrop blur",
                valueLabel = "${(settings.blur * 100).toInt()}%",
                value = settings.blur,
                range = 0f..1f,
                onChange = { value -> vm.updateSettings { it.copy(blur = value) } }
            )
            ToggleRow(
                title = "Animations",
                subtitle = "Transitions and cover motion",
                checked = settings.animations,
                onChange = { checked -> vm.updateSettings { it.copy(animations = checked) } }
            )
        }

        SettingsCard(title = "Display") {
            FieldLabel("Gaming density")
            ChipRow(
                options = GamingDensity.entries.toList(),
                selected = settings.gamingDensity,
                label = { it.label },
                onSelect = { d -> vm.updateSettings { it.copy(gamingDensity = d) } }
            )
            if (settings.gamingDensity == GamingDensity.CUSTOM) {
                Spacer(Modifier.height(8.dp))
                SliderRow(
                    title = "Custom scale",
                    valueLabel = "${(settings.gamingDensityCustom * 100).toInt()}%",
                    value = settings.gamingDensityCustom,
                    range = 0.8f..1.5f,
                    onChange = { v -> vm.updateSettings { it.copy(gamingDensityCustom = v) } }
                )
            }
        }

        SettingsCard(title = "Controller behaviour") {
            ToggleRow(
                title = "Open Gaming Mode on connect",
                subtitle = "Switch to the console UI when a pad connects in the foreground",
                checked = settings.autoGamingOnConnect,
                onChange = { checked -> vm.updateSettings { it.copy(autoGamingOnConnect = checked) } }
            )
            ToggleRow(
                title = "Leave Gaming Mode on disconnect",
                subtitle = "Return to Normal Mode when the last controller disconnects",
                checked = settings.autoNormalOnDisconnect,
                onChange = { checked -> vm.updateSettings { it.copy(autoNormalOnDisconnect = checked) } }
            )
            ToggleRow(
                title = "Ask before leaving Gaming Mode",
                subtitle = "Show a pause dialog when the controller disconnects",
                checked = settings.showDisconnectPrompt,
                onChange = { checked -> vm.updateSettings { it.copy(showDisconnectPrompt = checked) } }
            )

            Spacer(Modifier.height(12.dp))
            ToggleRow(
                title = "Background controller monitor",
                subtitle = "Foreground service watches for pads while this app is not visible",
                checked = settings.monitorInBackground,
                onChange = { checked ->
                    if (checked) requestNotificationPermission()
                    vm.updateSettings { it.copy(monitorInBackground = checked) }
                }
            )

            Spacer(Modifier.height(12.dp))
            ToggleRow(
                title = "Open Gaming Mode from other apps",
                subtitle = "Best effort: Android blocks background launches unless an exemption is granted",
                checked = settings.openFromOtherApps,
                onChange = { checked ->
                    if (checked) requestNotificationPermission()
                    vm.updateSettings { it.copy(openFromOtherApps = checked) }
                }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                            )
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (overlayGranted) "Draw-over granted" else "Allow draw-over")
                }
                if (Build.VERSION.SDK_INT >= 34) {
                    OutlinedButton(
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                )
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Full-screen alerts")
                    }
                }
            }
            Text(
                "With draw-over-apps granted the monitor can start Gaming Mode directly. Without it Android only allows a high-priority or full-screen notification, which the user must accept.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(12.dp))
            ToggleRow(
                title = "Detect USB controllers",
                subtitle = "React to USB-C gamepads such as GameSir X5 Lite",
                checked = settings.detectUsb,
                onChange = { checked -> vm.updateSettings { it.copy(detectUsb = checked) } }
            )
            ToggleRow(
                title = "Detect Bluetooth controllers",
                subtitle = "React to Bluetooth HID pads",
                checked = settings.detectBluetooth,
                onChange = { checked -> vm.updateSettings { it.copy(detectBluetooth = checked) } }
            )

            Spacer(Modifier.height(14.dp))
            FieldLabel("Quick overlay shortcut")
            ChipRow(
                options = OverlayCombo.entries.toList(),
                selected = settings.overlayCombo,
                label = { it.label },
                onSelect = { combo -> vm.updateSettings { it.copy(overlayCombo = combo) } }
            )

            Spacer(Modifier.height(14.dp))
            FieldLabel("Glyph appearance")
            ProfileSelector(
                selectedId = settings.glyphProfile,
                controller = null,
                onSelect = { id -> vm.updateSettings { it.copy(glyphProfile = id) } }
            )
            Text(
                "Visual profiles change the drawn glyphs only. Physical button mapping stays the same.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(12.dp))
            ToggleRow(
                title = "Button hints",
                subtitle = "Show controller hints in Gaming Mode",
                checked = settings.immersiveHints,
                onChange = { checked -> vm.updateSettings { it.copy(immersiveHints = checked) } }
            )

            Spacer(Modifier.height(12.dp))
            Button(onClick = onStartGaming, modifier = Modifier.fillMaxWidth()) {
                Text("Open Gaming Mode")
            }
        }

        SettingsCard(title = "Metadata providers") {
            ToggleRow(
                title = "Enrich new games automatically",
                subtitle = "Fetch a few covers after a library scan",
                checked = settings.autoMetadata,
                onChange = { checked -> vm.updateSettings { it.copy(autoMetadata = checked) } }
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Keys are stored only on this device. RAWG supplies descriptions and screenshots, IGDB supplies covers and release data, SteamGridDB adds portrait art. Only configure the ones you use.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            KeyField(
                label = "RAWG API key",
                value = rawg,
                secret = true,
                onChange = { rawg = it; keysSaved = false }
            )
            KeyField(
                label = "IGDB client ID",
                value = igdbId,
                secret = false,
                onChange = { igdbId = it; keysSaved = false }
            )
            KeyField(
                label = "IGDB client secret",
                value = igdbSecret,
                secret = true,
                onChange = { igdbSecret = it; keysSaved = false }
            )
            KeyField(
                label = "SteamGridDB API key",
                value = sgdb,
                secret = true,
                onChange = { sgdb = it; keysSaved = false }
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = {
                        vm.updateSettings {
                            it.copy(
                                rawgKey = rawg.trim(),
                                igdbClientId = igdbId.trim(),
                                igdbSecret = igdbSecret.trim(),
                                sgdbKey = sgdb.trim()
                            )
                        }
                        keysSaved = true
                    }
                ) { Text("Save keys") }
                if (keysSaved) {
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Saved",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        SettingsCard(title = "Library") {
            FieldLabel("Library layout")
            ChipRow(
                options = LibraryLayout.entries.toList(),
                selected = settings.libraryLayout,
                label = { it.label },
                onSelect = { layout -> vm.updateSettings { it.copy(libraryLayout = layout) } }
            )
            Spacer(Modifier.height(10.dp))
            FieldLabel("Default sort")
            ChipRow(
                options = SortMode.entries.toList(),
                selected = settings.sortMode,
                label = { it.label },
                onSelect = { mode -> vm.updateSettings { it.copy(sortMode = mode) } }
            )
            ToggleRow(
                title = "Ascending order",
                subtitle = "Oldest or A→Z first",
                checked = settings.sortAscending,
                onChange = { checked -> vm.updateSettings { it.copy(sortAscending = checked) } }
            )
            ToggleRow(
                title = "Only add games automatically",
                subtitle = "When off, every launchable app joins the library on scan",
                checked = settings.showOnlyGames,
                onChange = { checked ->
                    vm.updateSettings { it.copy(showOnlyGames = checked) }
                    vm.syncLibrary()
                }
            )
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = { vm.syncLibrary() },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Rescan installed apps") }
        }

        SettingsCard(title = "Data and privacy") {
            Text(
                "The library, play sessions, controller profiles and settings live in a local database and DataStore. Artwork is cached in the app's private files. There is no account and no telemetry. API calls only happen when metadata is requested.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            TextButton(onClick = { confirmClear = true }) {
                Text("Clear play history", color = MaterialTheme.colorScheme.error)
            }
        }

        SettingsCard(title = "About") {
            Text("Material Play ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(6.dp))
            Text(
                "A local-first game launcher with a controller-first Gaming Mode. Game detection uses Android's package manager; no game list is bundled. Launch times are approximate because Android does not expose exact play time to unprivileged apps. Automatic transitions from other apps depend on Android's background-activity-start rules and may fall back to a notification.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = {
                runCatching {
                    context.startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    )
                }
            }) { Text("Notification settings") }
        }

        Spacer(Modifier.height(20.dp))
    }

    if (confirmClear) {
        ConfirmDialog(
            title = "Clear play history?",
            body = "Every recorded session and total will be deleted. Games, favorites and controller profiles are kept.",
            confirmLabel = "Clear history",
            destructive = true,
            onConfirm = { vm.clearHistory() },
            onDismiss = { confirmClear = false }
        )
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun <T> ChipRow(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(label(option)) }
            )
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = if (enabled) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}

@Composable
private fun SliderRow(
    title: String,
    valueLabel: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(
                valueLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range
        )
    }
}

@Composable
private fun KeyField(
    label: String,
    value: String,
    secret: Boolean,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (secret) PasswordVisualTransformation() else VisualTransformation.None,
        shape = MaterialTheme.shapes.large
    )
    Spacer(Modifier.height(8.dp))
}
