package com.materialplay.launcher.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.materialplay.launcher.gamepad.GamepadMonitorService
import com.materialplay.launcher.ui.components.ChartIcon
import com.materialplay.launcher.ui.components.GamepadIcon
import com.materialplay.launcher.ui.components.GridIcon
import com.materialplay.launcher.data.settings.AppSettings
import com.materialplay.launcher.data.settings.ThemeMode
import com.materialplay.launcher.ui.theme.accentFor
import com.materialplay.launcher.gaming.rememberBatterySnapshot
import com.materialplay.launcher.gaming.rememberClock

enum class ShellTab(val labelRes: Int, val icon: ImageVector) {
    HOME(com.materialplay.launcher.R.string.tab_games, Icons.Filled.PlayArrow),
    LIBRARY(com.materialplay.launcher.R.string.tab_library, GridIcon),
    MEDIA(com.materialplay.launcher.R.string.tab_media, Icons.Filled.PlayArrow),
    PADS(com.materialplay.launcher.R.string.tab_pads, GamepadIcon),
    PROFILE(com.materialplay.launcher.R.string.tab_profile, Icons.Filled.Person),
    SETTINGS(com.materialplay.launcher.R.string.tab_settings, Icons.Filled.Settings)
}

@Composable
fun MainShell(vm: LauncherViewModel, onStartGaming: () -> Unit) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val games by vm.games.collectAsStateWithLifecycle()
    val hiddenGames by vm.hiddenGames.collectAsStateWithLifecycle()
    val controllers by vm.controllers.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var tabName by rememberSaveable { mutableStateOf(ShellTab.HOME.name) }
    var detailPackage by rememberSaveable { mutableStateOf<String?>(null) }
    val detailGame = detailPackage?.let { pkg ->
        games.firstOrNull { it.packageName == pkg }
            ?: hiddenGames.firstOrNull { it.packageName == pkg }
    }
    val snackbar = remember { androidx.compose.material3.SnackbarHostState() }

    LaunchedEffect(vm) {
        vm.messages.collect { message ->
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(message)
        }
    }

    LaunchedEffect(settings.monitorInBackground, settings.openFromOtherApps) {
        if (settings.monitorInBackground || settings.openFromOtherApps) {
            GamepadMonitorService.start(context)
        } else {
            GamepadMonitorService.stop(context)
        }
    }

    LaunchedEffect(detailPackage, detailGame) {
        if (detailPackage != null && detailGame == null) detailPackage = null
    }

    BackHandler(enabled = detailPackage != null) { detailPackage = null }

    val tab = ShellTab.entries.firstOrNull { it.name == tabName } ?: ShellTab.HOME
    val battery = rememberBatterySnapshot()
    val clock = rememberClock()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backdrop(settings))
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            // Screen content (fills all, including behind top bar)
            if (detailGame != null) {
                DetailsScreen(vm = vm, game = detailGame, onBack = { detailPackage = null })
            } else {
                when (tab) {
                    ShellTab.HOME -> HomeScreen(
                        vm = vm,
                        onOpenGame = { detailPackage = it.packageName },
                        onOpenLibrary = { tabName = ShellTab.LIBRARY.name },
                        onOpenControllers = { tabName = ShellTab.PADS.name },
                        onOpenStats = { tabName = ShellTab.PROFILE.name },
                        onOpenSettings = { tabName = ShellTab.SETTINGS.name },
                        onStartGaming = onStartGaming
                    )
                    ShellTab.LIBRARY -> LibraryScreen(
                        vm = vm,
                        onOpenGame = { detailPackage = it.packageName },
                        installedOnly = false,
                        showCategories = true
                    )
                    ShellTab.MEDIA -> MediaScreen(vm = vm)
                    ShellTab.PADS -> ControllersScreen(vm = vm)
                    ShellTab.PROFILE -> StatsScreen(vm = vm)
                    ShellTab.SETTINGS -> SettingsScreen(vm = vm, onStartGaming = onStartGaming)
                }
            }
            // Compact top navigation bar overlay (transparent)
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent))
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                // L1 glyph hint
                Text(
                    "L1",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 6.dp)
                )
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ShellTab.entries.filter { it != ShellTab.SETTINGS }.forEach { item ->
                        val selected = tab == item
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            androidx.compose.material3.TextButton(onClick = { tabName = item.name }) {
                                Text(
                                    stringResource(item.labelRes),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                            if (selected) {
                                Box(
                                    Modifier
                                        .width(24.dp)
                                        .height(2.dp)
                                        .background(accentFor(settings.accentId))
                                )
                            } else {
                                Spacer(Modifier.height(2.dp))
                            }
                        }
                    }
                }
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = onStartGaming) {
                        Icon(GamepadIcon, contentDescription = stringResource(com.materialplay.launcher.R.string.gaming_mode), tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = { tabName = ShellTab.LIBRARY.name }) {
                        Icon(Icons.Filled.Search, contentDescription = "Search", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = { tabName = ShellTab.SETTINGS.name }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = { tabName = ShellTab.PROFILE.name }) {
                        Icon(Icons.Filled.Person, contentDescription = "Profile", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    // Status cluster
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            controllers.firstOrNull()?.displayName ?: stringResource(com.materialplay.launcher.R.string.gm_touch_input),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (controllers.isNotEmpty()) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                        Text(
                            if (battery.charging) "${battery.level}%" else "${battery.level}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(clock, style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
            androidx.compose.material3.SnackbarHost(hostState = snackbar, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}

private fun backdrop(settings: AppSettings): Brush {
    val accent = accentFor(settings.accentId)
    return if (settings.themeMode == ThemeMode.LIGHT) {
        Brush.verticalGradient(
            listOf(
                Color(0xFFF7FAFC),
                Color(0xFFEEF3F7),
                accent.copy(alpha = 0.08f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color(0xFF0C1015),
                Color(0xFF090B0F),
                accent.copy(alpha = 0.12f).compositeOver(Color(0xFF090B0F))
            )
        )
    }
}

private fun Color.compositeOver(background: Color): Color {
    val alpha = this.alpha
    val r = red * alpha + background.red * (1 - alpha)
    val g = green * alpha + background.green * (1 - alpha)
    val b = blue * alpha + background.blue * (1 - alpha)
    return Color(r, g, b, 1f)
}