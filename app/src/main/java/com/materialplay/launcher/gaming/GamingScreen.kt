package com.materialplay.launcher.gaming

import android.app.Activity
import android.app.usage.StorageStatsManager
import android.os.Process
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.materialplay.launcher.R
import com.materialplay.launcher.ui.components.AppIcon
import com.materialplay.launcher.ui.components.gradientFor
import com.materialplay.launcher.ui.components.relativeTimeLabel
import java.io.File
import android.content.Context
import android.media.AudioManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.materialplay.launcher.controllerui.GlyphHintBar
import com.materialplay.launcher.controllerui.GlyphProfile
import com.materialplay.launcher.controllerui.PadGlyph
import com.materialplay.launcher.controllerui.ProfileOptions
import com.materialplay.launcher.controllerui.ProfileSelector
import com.materialplay.launcher.data.db.GameEntity
import com.materialplay.launcher.data.db.SessionEntity
import com.materialplay.launcher.data.settings.AppLanguage
import com.materialplay.launcher.data.settings.GamingDensity
import com.materialplay.launcher.data.settings.BackgroundStyle
import com.materialplay.launcher.data.settings.LibraryLayout
import com.materialplay.launcher.data.settings.OverlayCombo
import com.materialplay.launcher.data.settings.UiScale
import com.materialplay.launcher.data.settings.SortMode
import com.materialplay.launcher.data.settings.ThemeMode
import com.materialplay.launcher.gamepad.ConnectedController
import com.materialplay.launcher.gamepad.PadAction
import com.materialplay.launcher.ui.components.ChartIcon
import com.materialplay.launcher.ui.components.GameArt
import com.materialplay.launcher.ui.components.GamepadIcon
import com.materialplay.launcher.ui.components.GridIcon
import com.materialplay.launcher.ui.theme.AccentOptions
import com.materialplay.launcher.ui.theme.accentFor
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private enum class GamingTab(val labelRes: Int, val icon: ImageVector) {
    HOME(com.materialplay.launcher.R.string.tab_home, Icons.Filled.Home),
    LIBRARY(com.materialplay.launcher.R.string.tab_library, GridIcon),
    PADS(com.materialplay.launcher.R.string.tab_pads, GamepadIcon),
    STATS(com.materialplay.launcher.R.string.tab_profile, ChartIcon),
    SETTINGS(com.materialplay.launcher.R.string.tab_settings, Icons.Filled.Settings)
}

private enum class GamingZone { NAV, CONTENT, DETAILS }

private enum class GamingPanel { NONE, QUICK, PAUSE, INTRO, CONFIRM }

private data class GamingSettingRow(
    val label: String,
    val value: String,
    val adjust: (Int) -> Unit,
    val activate: () -> Unit
)

private data class GamingConfirm(
    val title: String,
    val body: String,
    val confirmLabel: String,
    val destructive: Boolean,
    val onConfirm: () -> Unit
)

@Composable
fun GamingScreen(
    vm: GamingViewModel,
    autoTransition: Boolean,
    introControllerName: String?,
    onExit: () -> Unit
) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val games by vm.games.collectAsStateWithLifecycle()
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    val sessions by vm.sessions.collectAsStateWithLifecycle()
    val totals by vm.totals.collectAsStateWithLifecycle()
    val controllers by vm.controllers.collectAsStateWithLifecycle()
    val savedControllers by vm.savedControllers.collectAsStateWithLifecycle()
    val syncing by vm.syncing.collectAsStateWithLifecycle()
    val refreshing by vm.refreshing.collectAsStateWithLifecycle()
    val rawInput by vm.rawInput.collectAsStateWithLifecycle()

    val configuration = LocalConfiguration.current
    val context = LocalContext.current
    val activity = com.materialplay.launcher.ui.theme.LocalActivity.current
    val audio = remember(context) {
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    val battery = rememberBatterySnapshot()

    var tabName by remember { mutableStateOf(GamingTab.HOME.name) }
    val tab = GamingTab.entries.firstOrNull { it.name == tabName } ?: GamingTab.HOME

    var zone by remember { mutableStateOf(GamingZone.NAV) }
    var navIndex by remember { mutableIntStateOf(0) }
    // Home: row 0 is the cover carousel (homeCol = selected cover), row 1 the Play / Details buttons.
    var homeRow by remember { mutableIntStateOf(0) }
    var homeCol by remember { mutableIntStateOf(0) }
    var homeAction by remember { mutableIntStateOf(0) }
    val overlayStamp = remember { LongArray(1) }
    var libraryIndex by remember { mutableIntStateOf(0) }
    var controllerIndex by remember { mutableIntStateOf(0) }
    var statsIndex by remember { mutableIntStateOf(0) }
    var settingsIndex by remember { mutableIntStateOf(0) }

    var panel by remember {
        mutableStateOf(if (autoTransition) GamingPanel.INTRO else GamingPanel.NONE)
    }
    var quickIndex by remember { mutableIntStateOf(0) }
    var pauseIndex by remember { mutableIntStateOf(0) }
    var pauseNotice by remember { mutableStateOf<String?>(null) }
    var confirmIndex by remember { mutableIntStateOf(0) }
    var confirmRequest by remember { mutableStateOf<GamingConfirm?>(null) }
    var detailPackage by remember { mutableStateOf<String?>(null) }
    var detailIndex by remember { mutableIntStateOf(0) }

    val brightnessState = remember {
        mutableFloatStateOf(
            activity?.window?.attributes?.screenBrightness?.takeIf { it >= 0f } ?: 0.68f
        )
    }
    var brightness by brightnessState
    val volumeState = remember {
        mutableIntStateOf(
            audio?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
        )
    }
    var volume by volumeState
    val maxVolume = remember(audio) {
        audio?.getStreamMaxVolume(AudioManager.STREAM_MUSIC)?.coerceAtLeast(1) ?: 1
    }

    fun applyBrightness(value: Float) {
        val clamped = value.coerceIn(0.05f, 1f)
        brightness = clamped
        activity?.window?.let { window ->
            val attributes = window.attributes
            attributes.screenBrightness = clamped
            window.attributes = attributes
        }
    }

    fun applyVolume(value: Int) {
        val clamped = value.coerceIn(0, maxVolume)
        volume = clamped
        audio?.setStreamVolume(AudioManager.STREAM_MUSIC, clamped, 0)
    }

    LaunchedEffect(panel) {
        if (panel != GamingPanel.QUICK) return@LaunchedEffect
        volume = audio?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: volume
    }

    LaunchedEffect(panel) {
        if (panel != GamingPanel.INTRO) return@LaunchedEffect
        kotlinx.coroutines.delay(2_800)
        if (panel == GamingPanel.INTRO) panel = GamingPanel.NONE
    }

    LaunchedEffect(vm) {
        vm.messages.collect { message ->
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(message)
        }
    }

    val activeController = controllers.firstOrNull()
    val activeStored = activeController?.let { controller ->
        savedControllers.firstOrNull { it.descriptor == controller.identity }
    }
    val profile = GlyphProfile.resolve(
        settings.glyphProfile,
        activeStored?.glyphProfile,
        activeController
    )

    val onText = stringResource(R.string.gm_on)
    val offText = stringResource(R.string.gm_off)
    val rescanLabel = stringResource(R.string.gm_rescan_library)
    val disconnectedNotice = stringResource(R.string.gm_disconnected_notice)
    val leaveConfirm = GamingConfirm(
        title = stringResource(R.string.gm_leave_title),
        body = stringResource(R.string.gm_leave_body),
        confirmLabel = stringResource(R.string.gm_leave),
        destructive = false,
        onConfirm = onExit
    )
    val clearConfirm = GamingConfirm(
        title = stringResource(R.string.gm_clear_title),
        body = stringResource(R.string.gm_clear_body),
        confirmLabel = stringResource(R.string.gm_clear_confirm),
        destructive = true,
        onConfirm = { vm.clearHistory() }
    )

    val totalsMap = remember(totals) { totals.associate { it.packageName to it.total } }
    val recent = remember(games) {
        games.filter { it.lastPlayedAt > 0 }.sortedByDescending { it.lastPlayedAt }
    }
    val visibleLibrary = remember(games, settings.sortMode, settings.sortAscending, totalsMap) {
        val comparator: Comparator<GameEntity> = when (settings.sortMode) {
            SortMode.TITLE -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.label }
            SortMode.LAST_PLAYED -> compareByDescending { it.lastPlayedAt }
            SortMode.PLAYTIME -> compareByDescending { totalsMap[it.packageName] ?: 0L }
            SortMode.RECENTLY_ADDED -> compareByDescending { it.addedAt }
        }
        val sorted = games.sortedWith(comparator)
        if (settings.sortAscending) sorted else sorted.reversed()
    }

    // Real width in the (scaled) dp of Gaming Mode, not the system dp.
    val gamingDensityValue = LocalDensity.current.density
    val widthDp = context.resources.displayMetrics.widthPixels / gamingDensityValue
    val tileDp = 92f * settings.coverScale
    val libraryColumns = ((widthDp - 48f) / (tileDp + 12f)).toInt().coerceIn(3, 10)

    val homeGames = remember(games, recent) {
        (recent + games.filter { it !in recent }).distinctBy { it.packageName }.take(40)
    }
    val featured = if (tab == GamingTab.HOME) {
        homeGames.getOrNull(homeCol) ?: recent.firstOrNull() ?: games.firstOrNull()
    } else {
        recent.firstOrNull() ?: games.firstOrNull()
    }
    val detailGame = detailPackage?.let { packageName ->
        games.firstOrNull { it.packageName == packageName }
    }

    fun selectTab(next: GamingTab) {
        tabName = next.name
        navIndex = GamingTab.entries.indexOf(next)
        zone = GamingZone.CONTENT
    }

    val libraryState = rememberLazyGridState()
    val controllersState = rememberLazyListState()
    val statsState = rememberLazyListState()
    val settingsState = rememberLazyListState()

    LaunchedEffect(homeGames.size) {
        val last = homeGames.lastIndex.coerceAtLeast(0)
        if (homeCol > last) homeCol = last
    }

    LaunchedEffect(visibleLibrary.size) {
        if (libraryIndex > visibleLibrary.lastIndex.coerceAtLeast(0)) {
            libraryIndex = visibleLibrary.lastIndex.coerceAtLeast(0)
        }
    }

    LaunchedEffect(controllers.size) {
        val last = (controllers.size - 1).coerceAtLeast(0)
        if (controllerIndex > last) controllerIndex = last
    }

    LaunchedEffect(sessions.size) {
        if (statsIndex > sessions.lastIndex.coerceAtLeast(0)) {
            statsIndex = sessions.lastIndex.coerceAtLeast(0)
        }
    }

    LaunchedEffect(detailPackage, detailGame) {
        if (detailPackage != null && detailGame == null) {
            detailPackage = null
            zone = GamingZone.CONTENT
        }
    }

    var knownControllerIds by remember { mutableStateOf<Set<String>?>(null) }
    LaunchedEffect(controllers) {
        val ids = controllers.map { it.identity }.toSet()
        val previous = knownControllerIds
        knownControllerIds = ids
        if (previous == null || previous.isEmpty() || ids.isNotEmpty()) return@LaunchedEffect
        val latest = vm.settings.value
        if (latest.autoNormalOnDisconnect) {
            onExit()
        } else if (latest.showDisconnectPrompt && panel == GamingPanel.NONE) {
            pauseNotice = disconnectedNotice
            pauseIndex = 0
            panel = GamingPanel.PAUSE
        }
    }

    val gamingSettingRows = buildList {
        add(
            GamingSettingRow(
                label = stringResource(R.string.theme),
                value = settings.themeMode.name.lowercase().replaceFirstChar { it.uppercase() },
                adjust = { step ->
                    val next = shift(ThemeMode.entries, settings.themeMode, step)
                    vm.updateSettings { it.copy(themeMode = next) }
                },
                activate = {
                    val next = shift(ThemeMode.entries, settings.themeMode, 1)
                    vm.updateSettings { it.copy(themeMode = next) }
                }
            )
        )
        add(
            GamingSettingRow(
                label = stringResource(R.string.accent),
                value = AccentOptions.firstOrNull { it.id == settings.accentId }?.label ?: "Mint",
                adjust = { step ->
                    val ids = AccentOptions.map { it.id }
                    val index = ids.indexOf(settings.accentId).coerceAtLeast(0)
                    val next = ids[(index + step + ids.size) % ids.size]
                    vm.updateSettings { it.copy(accentId = next) }
                },
                activate = {
                    val ids = AccentOptions.map { it.id }
                    val index = ids.indexOf(settings.accentId).coerceAtLeast(0)
                    vm.updateSettings { it.copy(accentId = ids[(index + 1) % ids.size]) }
                }
            )
        )
        add(
            GamingSettingRow(
                label = stringResource(R.string.background_style),
                value = settings.backgroundStyle.name.lowercase().replaceFirstChar { it.uppercase() },
                adjust = { step ->
                    val next = shift(BackgroundStyle.entries, settings.backgroundStyle, step)
                    vm.updateSettings { it.copy(backgroundStyle = next) }
                },
                activate = {
                    val next = shift(BackgroundStyle.entries, settings.backgroundStyle, 1)
                    vm.updateSettings { it.copy(backgroundStyle = next) }
                }
            )
        )
        add(
            GamingSettingRow(
                label = stringResource(R.string.cover_size),
                value = "${(settings.coverScale * 100).roundToInt()}%",
                adjust = { step ->
                    val next = (settings.coverScale + step * 0.1f).coerceIn(0.7f, 1.5f)
                    vm.updateSettings { it.copy(coverScale = next) }
                },
                activate = {
                    val next = if (settings.coverScale >= 1.5f) 0.7f else settings.coverScale + 0.1f
                    vm.updateSettings { it.copy(coverScale = next.coerceAtMost(1.5f)) }
                }
            )
        )
        add(toggleRow(stringResource(R.string.animations), settings.animations, onText, offText) { checked ->
            vm.updateSettings { it.copy(animations = checked) }
        })
        add(toggleRow(stringResource(R.string.button_hints), settings.immersiveHints, onText, offText) { checked ->
            vm.updateSettings { it.copy(immersiveHints = checked) }
        })
        add(toggleRow(stringResource(R.string.open_gaming_on_connect), settings.autoGamingOnConnect, onText, offText) { checked ->
            vm.updateSettings { it.copy(autoGamingOnConnect = checked) }
        })
        add(toggleRow(stringResource(R.string.leave_gaming_on_disconnect), settings.autoNormalOnDisconnect, onText, offText) { checked ->
            vm.updateSettings { it.copy(autoNormalOnDisconnect = checked) }
        })
        add(toggleRow(stringResource(R.string.ask_before_leaving), settings.showDisconnectPrompt, onText, offText) { checked ->
            vm.updateSettings { it.copy(showDisconnectPrompt = checked) }
        })
        add(
            GamingSettingRow(
                label = stringResource(R.string.quick_overlay_shortcut),
                value = settings.overlayCombo.label,
                adjust = { step ->
                    val next = shift(OverlayCombo.entries, settings.overlayCombo, step)
                    vm.updateSettings { it.copy(overlayCombo = next) }
                },
                activate = {
                    val next = shift(OverlayCombo.entries, settings.overlayCombo, 1)
                    vm.updateSettings { it.copy(overlayCombo = next) }
                }
            )
        )
        add(
            GamingSettingRow(
                label = stringResource(R.string.glyph_appearance),
                value = ProfileOptions.firstOrNull {
                    it.id.equals(settings.glyphProfile, true)
                }?.label ?: "Automatic",
                adjust = { step ->
                    val index = ProfileOptions.indexOfFirst {
                        it.id.equals(settings.glyphProfile, true)
                    }.coerceAtLeast(0)
                    val next = ProfileOptions[(index + step + ProfileOptions.size) % ProfileOptions.size]
                    vm.updateSettings { it.copy(glyphProfile = next.id) }
                },
                activate = {
                    val index = ProfileOptions.indexOfFirst {
                        it.id.equals(settings.glyphProfile, true)
                    }.coerceAtLeast(0)
                    val next = ProfileOptions[(index + 1) % ProfileOptions.size]
                    vm.updateSettings { it.copy(glyphProfile = next.id) }
                }
            )
        )
        add(
            GamingSettingRow(
                label = stringResource(R.string.gm_density),
                value = settings.gamingDensity.label,
                adjust = { step ->
                    val next = shift(GamingDensity.entries, settings.gamingDensity, step)
                    vm.updateSettings { it.copy(gamingDensity = next) }
                },
                activate = {
                    val next = shift(GamingDensity.entries, settings.gamingDensity, 1)
                    vm.updateSettings { it.copy(gamingDensity = next) }
                }
            )
        )
        add(
            GamingSettingRow(
                label = stringResource(R.string.library_layout),
                value = settings.libraryLayout.label,
                adjust = { step ->
                    val next = shift(LibraryLayout.entries, settings.libraryLayout, step)
                    vm.updateSettings { it.copy(libraryLayout = next) }
                },
                activate = {
                    val next = shift(LibraryLayout.entries, settings.libraryLayout, 1)
                    vm.updateSettings { it.copy(libraryLayout = next) }
                }
            )
        )
        add(
            GamingSettingRow(
                label = stringResource(R.string.language),
                value = settings.language.label,
                adjust = { step ->
                    val next = shift(AppLanguage.entries, settings.language, step)
                    vm.updateSettings { it.copy(language = next) }
                },
                activate = {
                    val next = shift(AppLanguage.entries, settings.language, 1)
                    vm.updateSettings { it.copy(language = next) }
                }
            )
        )
        add(
            GamingSettingRow(
                label = rescanLabel,
                value = if (syncing) stringResource(R.string.gm_scanning) else stringResource(R.string.gm_run_now),
                adjust = { vm.syncLibrary() },
                activate = { vm.syncLibrary() }
            )
        )
        add(
            GamingSettingRow(
                label = stringResource(R.string.gm_quick_menu),
                value = stringResource(R.string.gm_open_overlay),
                adjust = { panel = GamingPanel.QUICK },
                activate = { panel = GamingPanel.QUICK }
            )
        )
        add(
            GamingSettingRow(
                label = stringResource(R.string.gm_clear_history),
                value = stringResource(R.string.gm_delete_sessions),
                adjust = {
                    confirmIndex = 0
                    confirmRequest = clearConfirm
                    panel = GamingPanel.CONFIRM
                },
                activate = {
                    confirmIndex = 0
                    confirmRequest = clearConfirm
                    panel = GamingPanel.CONFIRM
                }
            )
        )
        add(
            GamingSettingRow(
                label = stringResource(R.string.gm_return_normal),
                value = stringResource(R.string.gm_touch_launcher),
                adjust = { onExit() },
                activate = { onExit() }
            )
        )
    }

    fun closeDetail() {
        detailPackage = null
        zone = GamingZone.CONTENT
    }

    fun openPause() {
        pauseIndex = 0
        pauseNotice = null
        panel = GamingPanel.PAUSE
    }

    fun closePanel() {
        if (panel == GamingPanel.CONFIRM) {
            confirmRequest = null
            confirmIndex = 0
        }
        panel = GamingPanel.NONE
    }

    fun openDetails(game: GameEntity) {
        detailPackage = game.packageName
        detailIndex = 0
        zone = GamingZone.DETAILS
    }

    fun launchGame(game: GameEntity) {
        detailPackage = null
        zone = GamingZone.CONTENT
        vm.launch(game)
    }

    fun cycleProfile(step: Int) {
        val controller = controllers.getOrNull(controllerIndex) ?: return
        val current = savedControllers.firstOrNull { it.descriptor == controller.identity }
            ?.glyphProfile ?: "auto"
        val index = ProfileOptions.indexOfFirst { it.id.equals(current, true) }.coerceAtLeast(0)
        val next = ProfileOptions[(index + step + ProfileOptions.size) % ProfileOptions.size]
        vm.setControllerProfile(controller.identity, next.id)
    }

    fun handleConfirm(action: PadAction) {
        when (action) {
            PadAction.UP, PadAction.LEFT -> confirmIndex = 0
            PadAction.DOWN, PadAction.RIGHT -> confirmIndex = 1
            PadAction.CONFIRM -> {
                val request = confirmRequest
                val confirmed = confirmIndex == 1
                closePanel()
                if (confirmed) request?.onConfirm?.invoke()
            }
            PadAction.BACK, PadAction.START, PadAction.SELECT -> closePanel()
            else -> Unit
        }
    }

    fun handleQuick(action: PadAction) {
        when (action) {
            PadAction.UP -> quickIndex = (quickIndex + 5) % 6
            PadAction.DOWN -> quickIndex = (quickIndex + 1) % 6
            PadAction.LEFT -> when (quickIndex) {
                1 -> applyBrightness(brightness - 0.05f)
                2 -> applyVolume(volume - 1)
                else -> Unit
            }
            PadAction.RIGHT -> when (quickIndex) {
                1 -> applyBrightness(brightness + 0.05f)
                2 -> applyVolume(volume + 1)
                else -> Unit
            }
            PadAction.CONFIRM -> when (quickIndex) {
                1 -> applyBrightness(brightness + 0.05f)
                2 -> applyVolume(volume + 1)
                5 -> closePanel()
                else -> Unit
            }
            PadAction.BACK, PadAction.START, PadAction.SELECT -> closePanel()
            else -> Unit
        }
    }

    fun handlePause(action: PadAction) {
        when (action) {
            PadAction.UP -> pauseIndex = (pauseIndex + 3) % 4
            PadAction.DOWN -> pauseIndex = (pauseIndex + 1) % 4
            PadAction.CONFIRM -> when (pauseIndex) {
                0 -> {
                    pauseNotice = null
                    closePanel()
                }
                1 -> {
                    pauseNotice = null
                    closePanel()
                    libraryIndex = 0
                    selectTab(GamingTab.LIBRARY)
                }
                2 -> {
                    pauseNotice = null
                    closePanel()
                    settingsIndex = 0
                    selectTab(GamingTab.SETTINGS)
                }
                else -> {
                    pauseNotice = null
                    confirmIndex = 0
                    confirmRequest = leaveConfirm
                    panel = GamingPanel.CONFIRM
                }
            }
            PadAction.BACK, PadAction.START -> {
                pauseNotice = null
                closePanel()
            }
            else -> Unit
        }
    }

    fun handleDetail(action: PadAction) {
        val game = detailGame ?: return
        when (action) {
            PadAction.UP, PadAction.LEFT -> detailIndex = (detailIndex + 3) % 4
            PadAction.DOWN, PadAction.RIGHT -> detailIndex = (detailIndex + 1) % 4
            PadAction.CONFIRM -> when (detailIndex) {
                0 -> launchGame(game)
                1 -> vm.toggleFavorite(game)
                2 -> vm.refreshMetadata(game)
                else -> closeDetail()
            }
            PadAction.BACK -> closeDetail()
            PadAction.START -> openPause()
            else -> Unit
        }
    }

    fun handleNav(action: PadAction) {
        val count = GamingTab.entries.size
        when (action) {
            PadAction.LEFT, PadAction.RIGHT -> {
                val step = if (action == PadAction.LEFT) -1 else 1
                navIndex = (navIndex + step + count) % count
                // Tabs switch live while the focus moves across the top bar.
                tabName = GamingTab.entries[navIndex].name
            }
            PadAction.DOWN, PadAction.CONFIRM -> {
                tabName = GamingTab.entries[navIndex].name
                zone = GamingZone.CONTENT
            }
            PadAction.BACK -> openPause()
            PadAction.SELECT, PadAction.QUATERNARY -> panel = GamingPanel.QUICK
            PadAction.START -> openPause()
            else -> Unit
        }
    }

    fun handleHome(action: PadAction) {
        val count = homeGames.size
        if (count == 0) {
            if (action == PadAction.UP || action == PadAction.CONFIRM) zone = GamingZone.NAV
            return
        }
        when (action) {
            PadAction.UP -> if (homeRow == 1) homeRow = 0 else zone = GamingZone.NAV
            PadAction.DOWN -> if (homeRow == 0) homeRow = 1
            PadAction.LEFT -> {
                if (homeRow == 0) homeCol = (homeCol + count - 1) % count
                else homeAction = (homeAction - 1).coerceAtLeast(0)
            }
            PadAction.RIGHT -> {
                if (homeRow == 0) homeCol = (homeCol + 1) % count
                else homeAction = (homeAction + 1).coerceAtMost(1)
            }
            PadAction.CONFIRM -> homeGames.getOrNull(homeCol)?.let { game ->
                if (homeRow == 1 && homeAction == 1) openDetails(game) else launchGame(game)
            }
            PadAction.TERTIARY -> homeGames.getOrNull(homeCol)?.let { openDetails(it) }
            else -> Unit
        }
    }

    fun handleLibrary(action: PadAction) {
        if (visibleLibrary.isEmpty()) return
        val last = visibleLibrary.lastIndex
        when (action) {
            PadAction.LEFT -> libraryIndex = (libraryIndex - 1).coerceIn(0, last)
            PadAction.RIGHT -> libraryIndex = (libraryIndex + 1).coerceIn(0, last)
            PadAction.UP -> if (libraryIndex < libraryColumns) zone = GamingZone.NAV
            else libraryIndex = (libraryIndex - libraryColumns).coerceIn(0, last)
            PadAction.DOWN -> libraryIndex = (libraryIndex + libraryColumns).coerceIn(0, last)
            PadAction.CONFIRM -> visibleLibrary.getOrNull(libraryIndex)?.let { openDetails(it) }
            PadAction.TERTIARY -> visibleLibrary.getOrNull(libraryIndex)?.let { vm.toggleFavorite(it) }
            else -> Unit
        }
    }

    fun handleControllers(action: PadAction) {
        if (controllers.isEmpty()) return
        val last = controllers.lastIndex
        when (action) {
            PadAction.UP -> controllerIndex = (controllerIndex - 1).coerceIn(0, last)
            PadAction.DOWN -> controllerIndex = (controllerIndex + 1).coerceIn(0, last)
            PadAction.LEFT -> cycleProfile(-1)
            PadAction.RIGHT -> cycleProfile(1)
            PadAction.CONFIRM -> cycleProfile(1)
            else -> Unit
        }
    }

    fun handleStats(action: PadAction) {
        if (sessions.isEmpty()) return
        val last = sessions.lastIndex
        when (action) {
            PadAction.UP, PadAction.LEFT -> statsIndex = (statsIndex - 1).coerceIn(0, last)
            PadAction.DOWN, PadAction.RIGHT -> statsIndex = (statsIndex + 1).coerceIn(0, last)
            PadAction.CONFIRM -> {
                val session = sessions.getOrNull(statsIndex)
                val game = session?.let { entry -> games.firstOrNull { it.packageName == entry.packageName } }
                if (game != null) launchGame(game)
            }
            PadAction.TERTIARY -> {
                val session = sessions.getOrNull(statsIndex)
                val game = session?.let { entry -> games.firstOrNull { it.packageName == entry.packageName } }
                if (game != null) openDetails(game)
            }
            else -> Unit
        }
    }

    fun handleSettings(action: PadAction) {
        when (action) {
            PadAction.UP -> if (settingsIndex == 0) zone = GamingZone.NAV
            else settingsIndex = (settingsIndex - 1).coerceIn(0, gamingSettingRows.lastIndex)
            PadAction.DOWN -> settingsIndex = (settingsIndex + 1).coerceIn(0, gamingSettingRows.lastIndex)
            PadAction.LEFT -> gamingSettingRows.getOrNull(settingsIndex)?.adjust?.invoke(-1)
            PadAction.RIGHT -> gamingSettingRows.getOrNull(settingsIndex)?.adjust?.invoke(1)
            PadAction.CONFIRM -> gamingSettingRows.getOrNull(settingsIndex)?.activate?.invoke()
            PadAction.TERTIARY -> {
                val row = gamingSettingRows.getOrNull(settingsIndex)
                if (row?.label == rescanLabel) row.activate()
            }
            else -> Unit
        }
    }

    fun handleContent(action: PadAction) {
        when (action) {
            PadAction.BACK -> zone = GamingZone.NAV
            PadAction.START -> openPause()
            PadAction.SELECT -> panel = GamingPanel.QUICK
            else -> when (tab) {
                GamingTab.HOME -> handleHome(action)
                GamingTab.LIBRARY -> handleLibrary(action)
                GamingTab.PADS -> handleControllers(action)
                GamingTab.STATS -> handleStats(action)
                GamingTab.SETTINGS -> handleSettings(action)
            }
        }
    }

    fun handleAction(action: PadAction) {
        if (panel == GamingPanel.INTRO) {
            panel = GamingPanel.NONE
            return
        }
        if ((action == PadAction.L1 || action == PadAction.R1) &&
            panel == GamingPanel.NONE && detailPackage == null
        ) {
            val step = if (action == PadAction.L1) -1 else 1
            val stamp = System.currentTimeMillis()
            scope.launch {
                // Give an L1+R1 chord a moment to claim the buttons before switching tabs.
                kotlinx.coroutines.delay(160)
                if (overlayStamp[0] >= stamp - 250) return@launch
                val count = GamingTab.entries.size
                val current = GamingTab.entries.indexOfFirst { it.name == tabName }.coerceAtLeast(0)
                val next = (current + step + count) % count
                tabName = GamingTab.entries[next].name
                navIndex = next
            }
            return
        }
        if (action == PadAction.QUATERNARY && panel == GamingPanel.NONE && detailPackage == null) {
            panel = GamingPanel.QUICK
            quickIndex = 0
            return
        }
        when (panel) {
            GamingPanel.QUICK -> {
                handleQuick(action)
                return
            }
            GamingPanel.PAUSE -> {
                handlePause(action)
                return
            }
            GamingPanel.CONFIRM -> {
                handleConfirm(action)
                return
            }
            GamingPanel.INTRO, GamingPanel.NONE -> Unit
        }
        if (detailPackage != null) {
            handleDetail(action)
            return
        }
        when (zone) {
            GamingZone.NAV -> handleNav(action)
            GamingZone.CONTENT -> handleContent(action)
            GamingZone.DETAILS -> handleDetail(action)
        }
    }

    val actionHandler = rememberUpdatedState<(PadAction) -> Unit> { action -> handleAction(action) }
    LaunchedEffect(vm) {
        vm.padActions.collect { action -> actionHandler.value(action) }
    }
    val overlayHandler = rememberUpdatedState<() -> Unit> {
        overlayStamp[0] = System.currentTimeMillis()
        when {
            panel == GamingPanel.QUICK -> panel = GamingPanel.NONE
            panel == GamingPanel.INTRO -> panel = GamingPanel.NONE
            // Do not silently discard a destructive confirmation or a disconnect notice.
            panel == GamingPanel.CONFIRM -> Unit
            panel == GamingPanel.PAUSE && pauseNotice != null -> Unit
            else -> {
                if (detailPackage != null) closeDetail()
                quickIndex = 0
                panel = GamingPanel.QUICK
            }
        }
    }
    LaunchedEffect(vm) {
        vm.overlayRequests.collect { overlayHandler.value() }
    }

    BackHandler {
        when {
            panel != GamingPanel.NONE -> closePanel()
            detailPackage != null -> closeDetail()
            zone == GamingZone.CONTENT -> zone = GamingZone.NAV
            tab != GamingTab.HOME -> {
                tabName = GamingTab.HOME.name
                navIndex = 0
            }
            else -> openPause()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GamingBackdrop(
            settings = settings,
            game = featured,
            cinematic = tab == GamingTab.HOME && detailPackage == null
        )

        Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
            GamingTopBar(
                tab = tab,
                navIndex = navIndex,
                navFocused = zone == GamingZone.NAV && panel == GamingPanel.NONE,
                battery = battery,
                controller = activeController,
                syncing = syncing,
                profile = profile,
                onSelect = { next -> selectTab(next) }
            )
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (tab) {
                        GamingTab.HOME -> HomeContent(
                            games = homeGames,
                            selectedIndex = homeCol,
                            focusedRow = homeRow,
                            focusedAction = homeAction,
                            focused = zone == GamingZone.CONTENT && panel == GamingPanel.NONE,
                            totalsMap = totalsMap,
                            sessions = sessions,
                            onSelect = { index ->
                                homeCol = index
                                homeRow = 0
                                zone = GamingZone.CONTENT
                            },
                            onPlay = { launchGame(it) },
                            onDetails = { openDetails(it) }
                        )
                        GamingTab.LIBRARY -> LibraryContent(
                            games = visibleLibrary,
                            totals = totalsMap,
                            columns = libraryColumns,
                            focusIndex = libraryIndex,
                            focused = zone == GamingZone.CONTENT && panel == GamingPanel.NONE,
                            gridState = libraryState,
                            onDetails = { openDetails(it) }
                        )
                        GamingTab.PADS -> ControllersContent(
                            controllers = controllers,
                            saved = savedControllers,
                            focusIndex = controllerIndex,
                            focused = zone == GamingZone.CONTENT && panel == GamingPanel.NONE,
                            listState = controllersState,
                            rawActions = rawInput.actions,
                            rawUpdatedAt = rawInput.updatedAt,
                            globalProfile = settings.glyphProfile,
                            profile = profile,
                            onProfile = { identity, id -> vm.setControllerProfile(identity, id) },
                            onCycle = { step -> cycleProfile(step) },
                            onScan = { vm.syncLibrary() },
                            syncing = syncing
                        )
                        GamingTab.STATS -> StatsContent(
                            totals = totals,
                            sessions = sessions,
                            labels = games.associate { it.packageName to it.label },
                            focusIndex = statsIndex,
                            focused = zone == GamingZone.CONTENT && panel == GamingPanel.NONE,
                            listState = statsState,
                            onDetails = { game -> openDetails(game) }
                        )
                        GamingTab.SETTINGS -> SettingsContent(
                            rows = gamingSettingRows,
                            focusIndex = settingsIndex,
                            focused = zone == GamingZone.CONTENT && panel == GamingPanel.NONE,
                            listState = settingsState,
                            onActivate = { index ->
                                settingsIndex = index
                                gamingSettingRows.getOrNull(index)?.activate?.invoke()
                            }
                        )
                    }
                }
                if (settings.immersiveHints) {
                    GamingHintBar(
                        tab = tab,
                        zone = zone,
                        detailOpen = detailPackage != null,
                        profile = profile,
                        overlayCombo = settings.overlayCombo
                    )
                }
            }

        if (panel == GamingPanel.INTRO) {
            IntroOverlay(controllerName = introControllerName, profile = profile)
        }
        if (panel == GamingPanel.QUICK) {
            val clock = rememberClock()
            QuickOverlay(
                selected = quickIndex,
                controllerName = activeController?.displayName,
                battery = battery,
                clock = clock,
                brightness = brightness,
                onBrightness = { applyBrightness(it) },
                volume = volume,
                maxVolume = maxVolume,
                onVolume = { applyVolume(it) },
                onDismiss = { closePanel() },
                profile = profile
            )
        }
        if (panel == GamingPanel.PAUSE) {
            GamingMenuOverlay(
                title = if (pauseNotice == null) stringResource(R.string.gm_paused)
                else stringResource(R.string.gm_controller_disconnected),
                body = pauseNotice,
                items = listOf(
                    stringResource(R.string.gm_resume),
                    stringResource(R.string.tab_library),
                    stringResource(R.string.tab_settings),
                    stringResource(R.string.gm_exit_gaming)
                ),
                selected = pauseIndex,
                profile = profile,
                onSelect = { index ->
                    pauseIndex = index
                    handlePause(PadAction.CONFIRM)
                },
                onDismiss = { pauseNotice = null; closePanel() }
            )
        }
        if (panel == GamingPanel.CONFIRM) {
            val request = confirmRequest
            GamingMenuOverlay(
                title = request?.title ?: stringResource(R.string.gm_confirm),
                body = request?.body,
                items = listOf(
                    stringResource(R.string.gm_cancel),
                    request?.confirmLabel ?: stringResource(R.string.gm_confirm)
                ),
                selected = confirmIndex,
                profile = profile,
                onSelect = { index ->
                    confirmIndex = index
                    handleConfirm(PadAction.CONFIRM)
                },
                onDismiss = { closePanel() }
            )
        }
        if (detailGame != null && panel == GamingPanel.NONE) {
            GamingDetailsOverlay(
                game = detailGame,
                totalPlaytime = totalsMap[detailGame.packageName] ?: 0L,
                selected = detailIndex,
                refreshing = refreshing == detailGame.packageName,
                profile = profile,
                onPlay = { launchGame(detailGame) },
                onFavorite = { vm.toggleFavorite(detailGame) },
                onRefresh = { vm.refreshMetadata(detailGame) },
                onDismiss = { closeDetail() }
            )
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = if (settings.immersiveHints) 58.dp else 16.dp)
        )
    }
}

@Composable
private fun GamingBackdrop(
    settings: com.materialplay.launcher.data.settings.AppSettings,
    game: GameEntity?,
    cinematic: Boolean
) {
    val background = MaterialTheme.colorScheme.background
    val accent = accentFor(settings.accentId)
    val context = LocalContext.current
    Box(modifier = Modifier.fillMaxSize().background(background)) {
        if (settings.backgroundStyle == BackgroundStyle.ARTWORK && game != null) {
            Crossfade(
                targetState = game,
                animationSpec = tween(if (settings.animations) 320 else 0),
                label = "gaming-backdrop"
            ) { target ->
                val art = (target.heroPath ?: target.coverPath)
                    ?.let { File(it) }
                    ?.takeIf { it.exists() }
                if (art != null) {
                    val soften = if (!cinematic && settings.blur > 0.02f) {
                        Modifier.blur((settings.blur * 28).dp)
                    } else {
                        Modifier
                    }
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(art).build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().then(soften),
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.CenterEnd
                    )
                } else {
                    // No official artwork: ambient glow built from the app icon.
                    Box(Modifier.fillMaxSize().background(gradientFor(target.packageName))) {
                        AppIcon(
                            packageName = target.packageName,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .size(420.dp)
                                .blur(48.dp)
                                .alpha(0.55f)
                        )
                    }
                }
            }
        } else if (settings.backgroundStyle == BackgroundStyle.GRADIENT) {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        listOf(
                            background,
                            background,
                            accent.copy(alpha = 0.18f).compositeOver(background)
                        )
                    )
                )
            )
        }
        val strong = !cinematic || settings.backgroundStyle != BackgroundStyle.ARTWORK
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(
                        Color.Black.copy(alpha = if (strong) 0.72f else 0.45f),
                        Color.Black.copy(alpha = if (strong) 0.78f else 0.12f),
                        Color.Black.copy(alpha = if (strong) 0.90f else 0.88f)
                    )
                )
            )
        )
        if (cinematic) {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.horizontalGradient(
                        listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent)
                    )
                )
            )
        }
    }
}

@Composable
private fun GamingTopBar(
    tab: GamingTab,
    navIndex: Int,
    navFocused: Boolean,
    battery: BatterySnapshot,
    controller: ConnectedController?,
    syncing: Boolean,
    profile: GlyphProfile,
    onSelect: (GamingTab) -> Unit
) {
    val clock = rememberClock()
    val accent = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PadGlyph(PadAction.L1, profile, size = 18.dp)
        Spacer(Modifier.width(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GamingTab.entries.forEachIndexed { index, item ->
                val selected = item == tab
                val isFocused = navFocused && navIndex == index
                val shape = RoundedCornerShape(8.dp)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(shape)
                        .focusRing(isFocused, shape)
                        .clickable { onSelect(item) }
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                ) {
                    Text(
                        stringResource(item.labelRes),
                        fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) Color.White else Color.White.copy(alpha = 0.55f),
                        maxLines = 1
                    )
                    Box(
                        Modifier
                            .padding(top = 3.dp)
                            .width(if (selected) 20.dp else 0.dp)
                            .height(2.dp)
                            .background(accent)
                    )
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        PadGlyph(PadAction.R1, profile, size = 18.dp)
        Spacer(Modifier.weight(1f))
        if (syncing) {
            CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                strokeWidth = 2.dp,
                color = Color.White
            )
            Spacer(Modifier.width(10.dp))
        }
        Icon(
            GamepadIcon,
            contentDescription = null,
            tint = if (controller != null) accent else Color.White.copy(alpha = 0.5f),
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            controller?.displayName ?: stringResource(R.string.gm_touch_input),
            fontSize = 12.sp,
            color = if (controller != null) accent else Color.White.copy(alpha = 0.6f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 100.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            if (battery.charging) "${battery.level}% ⚡" else "${battery.level}%",
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.75f)
        )
        Spacer(Modifier.width(12.dp))
        Text(clock, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
    }
}

/** Square cover. Without official art it shows an ambient tile built from the app icon. */
@Composable
private fun GameTile(
    game: GameEntity,
    modifier: Modifier = Modifier,
    corner: Dp = 10.dp
) {
    val shape = RoundedCornerShape(corner)
    val context = LocalContext.current
    val cover = (game.coverPath ?: game.heroPath)?.let { File(it) }?.takeIf { it.exists() }
    Box(modifier = modifier.clip(shape).background(gradientFor(game.packageName))) {
        if (cover != null) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(cover).crossfade(true).build(),
                contentDescription = game.label,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            AppIcon(
                packageName = game.packageName,
                modifier = Modifier.fillMaxSize().blur(18.dp).alpha(0.5f)
            )
            AppIcon(
                packageName = game.packageName,
                modifier = Modifier.align(Alignment.Center).fillMaxSize(0.78f)
            )
        }
    }
}

@Composable
private fun HomeContent(
    games: List<GameEntity>,
    selectedIndex: Int,
    focusedRow: Int,
    focusedAction: Int,
    focused: Boolean,
    totalsMap: Map<String, Long>,
    sessions: List<SessionEntity>,
    onSelect: (Int) -> Unit,
    onPlay: (GameEntity) -> Unit,
    onDetails: (GameEntity) -> Unit
) {
    val game = games.getOrNull(selectedIndex)
    if (game == null) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                stringResource(R.string.gm_welcome),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.gm_welcome_body),
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.75f)
            )
        }
        return
    }

    val context = LocalContext.current
    val accent = MaterialTheme.colorScheme.primary
    val tracked = totalsMap[game.packageName] ?: 0L
    val sessionCount = remember(sessions, game.packageName) {
        sessions.count { it.packageName == game.packageName }
    }
    val recentMinutes = remember(sessions, game.packageName) {
        val cutoff = System.currentTimeMillis() - 14L * 24 * 60 * 60 * 1000
        sessions
            .filter { it.packageName == game.packageName && it.startedAt >= cutoff }
            .sumOf { it.durationMs } / 60_000
    }
    val installedBytes = remember(game.packageName) {
        runCatching {
            val manager = context.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager
            manager.queryStatsForPackage(
                android.os.storage.StorageManager.UUID_DEFAULT,
                game.packageName,
                Process.myUserHandle()
            ).appBytes
        }.getOrNull()
    }
    val playedLine = listOfNotNull(
        game.lastPlayedAt.takeIf { it > 0 }
            ?.let { stringResource(R.string.gm_played, relativeTimeLabel(it)) },
        tracked.takeIf { it > 0 }?.let { formatPlaytime(it) }
    ).joinToString("  ·  ")

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val viewportWidth = maxWidth - 40.dp
        val showCards = maxHeight >= 275.dp
        val showPlayed = maxHeight >= 200.dp
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            HomeCarousel(
                games = games,
                selectedIndex = selectedIndex,
                focused = focused && focusedRow == 0,
                viewportWidth = viewportWidth,
                onSelect = onSelect
            )

            Spacer(Modifier.weight(1f))

            val logo = game.logoPath?.let { File(it) }?.takeIf { it.exists() }
            if (logo != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(logo).build(),
                    contentDescription = game.label,
                    modifier = Modifier.fillMaxWidth(0.35f).height(40.dp),
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.CenterStart
                )
            } else {
                Text(
                    game.label,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(0.6f)
                )
            }
            if (showPlayed && playedLine.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(playedLine, fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f), maxLines = 1)
            }

            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val playFocused = focused && focusedRow == 1 && focusedAction == 0
                val moreFocused = focused && focusedRow == 1 && focusedAction == 1
                val playShape = RoundedCornerShape(6.dp)
                Row(
                    modifier = Modifier
                        .width(132.dp)
                        .height(30.dp)
                        .clip(playShape)
                        .background(if (playFocused) accent else Color.White.copy(alpha = 0.18f))
                        .then(
                            if (playFocused) Modifier.border(2.dp, Color.White, playShape) else Modifier
                        )
                        .clickable { onPlay(game) },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        stringResource(R.string.gm_play),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(if (moreFocused) accent else Color.White.copy(alpha = 0.18f))
                        .then(
                            if (moreFocused) Modifier.border(2.dp, Color.White, CircleShape) else Modifier
                        )
                        .clickable { onDetails(game) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.MoreVert,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (showCards) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    InfoCard(
                        title = stringResource(R.string.gm_card_time),
                        value = if (tracked > 0) formatPlaytime(tracked) else "—",
                        detail = listOfNotNull(
                            game.lastPlayedAt.takeIf { it > 0 }?.let { relativeTimeLabel(it) }
                                ?: stringResource(R.string.gm_never_played),
                            recentMinutes.takeIf { it > 0 }
                                ?.let { stringResource(R.string.gm_two_weeks, it) }
                        ).joinToString(" · ")
                    )
                    if (sessionCount > 0) {
                        InfoCard(
                            title = stringResource(R.string.gm_card_sessions),
                            value = sessionCount.toString(),
                            detail = stringResource(R.string.gm_card_sessions_sub)
                        )
                    }
                    if (installedBytes != null) {
                        InfoCard(
                            title = stringResource(R.string.gm_card_storage),
                            value = formatBytes(installedBytes),
                            detail = stringResource(R.string.gm_card_storage_sub)
                        )
                    }
                    val genre = game.displayGenre
                    if (genre != null) {
                        InfoCard(
                            title = stringResource(R.string.gm_card_info),
                            value = genre,
                            detail = game.developer ?: ""
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun HomeCarousel(
    games: List<GameEntity>,
    selectedIndex: Int,
    focused: Boolean,
    viewportWidth: Dp,
    onSelect: (Int) -> Unit
) {
    val listState = rememberLazyListState()
    LaunchedEffect(selectedIndex) {
        listState.animateScrollToItem(selectedIndex.coerceAtLeast(0))
    }
    val accent = MaterialTheme.colorScheme.primary
    LazyRow(
        state = listState,
        contentPadding = PaddingValues(end = (viewportWidth - 90.dp).coerceAtLeast(0.dp)),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().height(106.dp)
    ) {
        itemsIndexed(games, key = { _, game -> game.packageName }) { index, game ->
            val selected = index == selectedIndex
            val tileSize by animateDpAsState(
                targetValue = if (selected) 80.dp else 52.dp,
                animationSpec = tween(150),
                label = "home-tile"
            )
            val shape = RoundedCornerShape(10.dp)
            val ring = when {
                selected && focused -> Modifier.border(3.dp, accent, shape)
                selected -> Modifier.border(2.dp, Color.White.copy(alpha = 0.6f), shape)
                else -> Modifier
            }
            Column(modifier = Modifier.clickable { onSelect(index) }) {
                GameTile(
                    game = game,
                    modifier = Modifier.size(tileSize).then(ring),
                    corner = 10.dp
                )
                if (selected) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        game.label,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.width(tileSize)
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoCard(title: String, value: String, detail: String) {
    val shape = RoundedCornerShape(8.dp)
    Column(
        modifier = Modifier
            .width(150.dp)
            .height(58.dp)
            .clip(shape)
            .background(Color.Black.copy(alpha = 0.5f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), shape)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            title.uppercase(),
            fontSize = 9.sp,
            letterSpacing = 0.6.sp,
            color = Color.White.copy(alpha = 0.55f),
            maxLines = 1
        )
        Text(
            value,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            detail,
            fontSize = 10.sp,
            color = Color.White.copy(alpha = 0.6f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun formatBytes(bytes: Long): String {
    val mb = bytes / (1024f * 1024f)
    return if (mb >= 1024f) "%.1f GB".format(mb / 1024f) else "%.0f MB".format(mb)
}

@Composable
private fun LibraryContent(
    games: List<GameEntity>,
    totals: Map<String, Long>,
    columns: Int,
    focusIndex: Int,
    focused: Boolean,
    gridState: LazyGridState,
    onDetails: (GameEntity) -> Unit
) {
    LaunchedEffect(focused, focusIndex) {
        if (focused) gridState.animateScrollToItem((focusIndex - columns).coerceAtLeast(0))
    }

    if (games.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                stringResource(R.string.library_empty),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.gm_library_empty_body),
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.75f)
            )
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        state = gridState,
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(games, key = { _, game -> game.packageName }) { index, game ->
            val shape = RoundedCornerShape(10.dp)
            val selected = focused && focusIndex == index
            Column(modifier = Modifier.clip(shape).clickable { onDetails(game) }) {
                GameTile(
                    game = game,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .focusRing(selected, shape),
                    corner = 10.dp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    game.label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    totals[game.packageName]?.let { formatPlaytime(it) }
                        ?: game.displayGenre
                        ?: stringResource(R.string.gm_installed),
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ControllersContent(
    controllers: List<ConnectedController>,
    saved: List<com.materialplay.launcher.data.db.ControllerEntity>,
    focusIndex: Int,
    focused: Boolean,
    listState: LazyListState,
    rawActions: Set<PadAction>,
    rawUpdatedAt: Long,
    globalProfile: String,
    profile: GlyphProfile,
    onProfile: (String, String) -> Unit,
    onCycle: (Int) -> Unit,
    onScan: () -> Unit,
    syncing: Boolean
) {
    LaunchedEffect(focused, focusIndex) {
        // The section header is the first list item, so controller N is item N + 1.
        if (focused) listState.animateScrollToItem((focusIndex + 1).coerceAtLeast(0))
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(start = 26.dp, end = 26.dp, top = 4.dp, bottom = 26.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Controllers", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Move up or down to choose a pad. Left or right cycles its glyph appearance.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    if (syncing) "Scanning…" else "Live",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        if (controllers.isEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .clickable(enabled = !syncing, onClick = onScan)
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("No controller connected", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Pair a pad or plug in the GameSir over USB-C. Touch input still works.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.Filled.Refresh, contentDescription = null)
                }
            }
        } else {
            itemsIndexed(controllers, key = { _, controller -> controller.identity }) { index, controller ->
                val stored = saved.firstOrNull { it.descriptor == controller.identity }
                val cardProfile = GlyphProfile.resolve(
                    globalProfile,
                    stored?.glyphProfile,
                    controller
                )
                val selected = focused && focusIndex == index
                ControllerCard(
                    controller = controller,
                    profile = cardProfile,
                    storedProfile = stored?.glyphProfile ?: "auto",
                    selected = selected,
                    liveActions = if (index == 0) rawActions else emptySet(),
                    liveUpdatedAt = if (index == 0) rawUpdatedAt else 0L,
                    onProfile = { id -> onProfile(controller.identity, id) },
                    onCycle = onCycle
                )
            }
        }
        if (saved.isNotEmpty()) {
            item {
                Text(
                    "Remembered pads",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items(saved, key = { "saved-${it.descriptor}" }) { controller ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            controller.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            controller.descriptor,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        controller.glyphProfile,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun ControllerCard(
    controller: ConnectedController,
    profile: GlyphProfile,
    storedProfile: String,
    selected: Boolean,
    liveActions: Set<PadAction>,
    liveUpdatedAt: Long,
    onProfile: (String) -> Unit,
    onCycle: (Int) -> Unit
) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.86f))
            .focusRing(selected, shape)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable { onCycle(1) },
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
            Text("LIVE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
        Text(
            "${controller.vendorIdHex} • ${controller.productIdHex} • ${controller.descriptor}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            "Glyph appearance: ${ProfileOptions.firstOrNull { it.id.equals(storedProfile, true) }?.label ?: "Automatic"}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        ProfileSelector(
            selectedId = storedProfile,
            controller = controller,
            onSelect = onProfile
        )
        if (liveUpdatedAt > 0L) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PadAction.entries.take(8).forEach { action ->
                    val pressed = action in liveActions
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (pressed) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
                            )
                            .padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        PadGlyph(action, profile, size = 22.dp, muted = !pressed)
                        Text(
                            action.label,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            color = if (pressed) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            Text(
                "Press a button to verify the mapping.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatsContent(
    totals: List<com.materialplay.launcher.data.db.PlayTotals>,
    sessions: List<SessionEntity>,
    labels: Map<String, String>,
    focusIndex: Int,
    focused: Boolean,
    listState: LazyListState,
    onDetails: (GameEntity) -> Unit
) {
    LaunchedEffect(focused, focusIndex, sessions.size, totals.size) {
        if (focused && sessions.isNotEmpty()) {
            listState.animateScrollToItem((4 + totals.size + focusIndex).coerceAtLeast(0))
        }
    }

    val totalTime = totals.sumOf { it.total }
    val maxTotal = totals.maxOfOrNull { it.total } ?: 1L

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(start = 26.dp, end = 26.dp, top = 4.dp, bottom = 26.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(34.dp)) {
                Stat(label = "TRACKED", value = formatPlaytime(totalTime))
                Stat(label = "SESSIONS", value = sessions.size.toString())
                Stat(label = "GAMES", value = totals.size.toString())
            }
        }
        item {
            Text(
                "Approximate time from launch until this launcher returns.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            Text("By game", style = MaterialTheme.typography.titleLarge)
        }
        items(totals, key = { "total-${it.packageName}" }) { total ->
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        labels[total.packageName] ?: total.packageName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(formatPlaytime(total.total), style = MaterialTheme.typography.labelMedium)
                }
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((total.total.toFloat() / maxTotal).coerceIn(0.02f, 1f))
                            .height(6.dp)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
        item {
            Text("Recent sessions", style = MaterialTheme.typography.titleLarge)
        }
        if (sessions.isEmpty()) {
            item {
                Text(
                    "No sessions yet. Launch a game and it will appear here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            itemsIndexed(sessions, key = { _, session -> session.id }) { index, session ->
                val shape = RoundedCornerShape(12.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .background(
                            if (focused && focusIndex == index) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                        .focusRing(focused && focusIndex == index, shape)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            labels[session.packageName] ?: session.packageName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            com.materialplay.launcher.core.Text.dateTime(session.startedAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (session.endedAt == null) {
                        Text(
                            "in progress",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Text(
                            formatPlaytime(session.durationMs),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column {
        Text(value, style = MaterialTheme.typography.headlineMedium)
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingsContent(
    rows: List<GamingSettingRow>,
    focusIndex: Int,
    focused: Boolean,
    listState: LazyListState,
    onActivate: (Int) -> Unit
) {
    LaunchedEffect(focused, focusIndex) {
        if (focused) listState.animateScrollToItem((focusIndex - 2).coerceAtLeast(0))
    }
    val accent = MaterialTheme.colorScheme.primary

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        itemsIndexed(rows, key = { index, row -> "$index-${row.label}" }) { index, row ->
            val shape = RoundedCornerShape(10.dp)
            val selected = focused && focusIndex == index
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(
                        if (selected) accent.copy(alpha = 0.28f) else Color.Black.copy(alpha = 0.45f)
                    )
                    .focusRing(selected, shape)
                    .clickable { onActivate(index) }
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    row.label,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    row.value,
                    fontSize = 13.sp,
                    color = if (selected) Color.White else Color.White.copy(alpha = 0.65f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun GamingHintBar(
    tab: GamingTab,
    zone: GamingZone,
    detailOpen: Boolean,
    profile: GlyphProfile,
    overlayCombo: OverlayCombo
) {
    val move = stringResource(R.string.gm_hint_move)
    val open = stringResource(R.string.gm_hint_open)
    val play = stringResource(R.string.gm_play)
    val details = stringResource(R.string.gm_hint_details)
    val favorite = stringResource(R.string.gm_hint_favorite)
    val select = stringResource(R.string.gm_hint_select)
    val close = stringResource(R.string.gm_hint_close)
    val tabs = stringResource(R.string.gm_hint_tabs)
    val pause = stringResource(R.string.gm_hint_pause)
    val change = stringResource(R.string.gm_hint_change)
    val cycle = stringResource(R.string.gm_hint_cycle_glyphs)
    val prevTab = stringResource(R.string.gm_hint_prev_tab)
    val nextTab = stringResource(R.string.gm_hint_next_tab)

    val tabKeys = listOf(PadAction.L1 to prevTab, PadAction.R1 to nextTab)
    val start = PadAction.START to pause
    val hints = when {
        detailOpen -> listOf(PadAction.CONFIRM to select, PadAction.BACK to close)
        zone == GamingZone.NAV -> listOf(PadAction.LEFT to move, PadAction.CONFIRM to open) + tabKeys + start
        tab == GamingTab.HOME -> listOf(
            PadAction.CONFIRM to play,
            PadAction.TERTIARY to details,
            PadAction.BACK to tabs
        ) + tabKeys + start
        tab == GamingTab.LIBRARY -> listOf(
            PadAction.CONFIRM to details,
            PadAction.TERTIARY to favorite,
            PadAction.BACK to tabs
        ) + tabKeys + start
        tab == GamingTab.PADS -> listOf(
            PadAction.CONFIRM to cycle,
            PadAction.BACK to tabs
        ) + tabKeys + start
        tab == GamingTab.STATS -> listOf(
            PadAction.CONFIRM to play,
            PadAction.TERTIARY to details,
            PadAction.BACK to tabs
        ) + tabKeys + start
        else -> listOf(
            PadAction.CONFIRM to change,
            PadAction.UP to move,
            PadAction.BACK to tabs
        ) + tabKeys + start
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            hints.forEach { (action, label) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PadGlyph(action, profile, size = 18.dp)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        label,
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 1
                    )
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Text(
            overlayCombo.label,
            fontSize = 10.sp,
            color = Color.White.copy(alpha = 0.6f),
            maxLines = 1
        )
        Spacer(Modifier.width(6.dp))
        Text(
            stringResource(R.string.gm_quick_menu),
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1
        )
    }
}

private fun <T> shift(values: List<T>, current: T, step: Int): T {
    val index = values.indexOf(current)
    if (index < 0) return values.first()
    return values[(index + step % values.size + values.size) % values.size]
}

private fun toggleRow(
    label: String,
    checked: Boolean,
    on: String,
    off: String,
    onChange: (Boolean) -> Unit
): GamingSettingRow {
    fun flip() = onChange(!checked)
    return GamingSettingRow(
        label = label,
        value = if (checked) on else off,
        adjust = { flip() },
        activate = { flip() }
    )
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

private fun Color.compositeOver(background: Color): Color {
    val alpha = this.alpha
    val r = red * alpha + background.red * (1 - alpha)
    val g = green * alpha + background.green * (1 - alpha)
    val b = blue * alpha + background.blue * (1 - alpha)
    return Color(r, g, b, 1f)
}
