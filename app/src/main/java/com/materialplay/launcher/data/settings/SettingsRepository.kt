package com.materialplay.launcher.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Single source of truth for user preferences. Values are persisted in DataStore and
 * exposed as an immutable [AppSettings] snapshot.
 */
class SettingsRepository(
    private val context: Context,
    scope: CoroutineScope
) {
    private object Keys {
        val theme = stringPreferencesKey("theme")
        val accent = stringPreferencesKey("accent")
        val dynamic = booleanPreferencesKey("dynamic_color")
        val coverScale = floatPreferencesKey("cover_scale")
        val animations = booleanPreferencesKey("animations")
        val blur = floatPreferencesKey("blur")
        val background = stringPreferencesKey("background_style")
        val uiScale = stringPreferencesKey("ui_scale")
        val language = stringPreferencesKey("language")
        val libraryLayout = stringPreferencesKey("library_layout")
        val gamingDensity = stringPreferencesKey("gaming_density")
        val gamingDensityCustom = floatPreferencesKey("gaming_density_custom")

        val autoGaming = booleanPreferencesKey("auto_gaming_on_connect")
        val autoNormal = booleanPreferencesKey("auto_normal_on_disconnect")
        val disconnectPrompt = booleanPreferencesKey("disconnect_prompt")
        val monitor = booleanPreferencesKey("monitor_in_background")
        val openFromApps = booleanPreferencesKey("open_from_other_apps")
        val detectUsb = booleanPreferencesKey("detect_usb")
        val detectBluetooth = booleanPreferencesKey("detect_bluetooth")
        val glyph = stringPreferencesKey("glyph_profile")
        val overlayCombo = stringPreferencesKey("overlay_combo")

        val autoMetadata = booleanPreferencesKey("auto_metadata")
        val rawg = stringPreferencesKey("rawg_key")
        val igdbId = stringPreferencesKey("igdb_client_id")
        val igdbSecret = stringPreferencesKey("igdb_secret")
        val sgdb = stringPreferencesKey("sgdb_key")

        val sortMode = stringPreferencesKey("sort_mode")
        val sortAsc = booleanPreferencesKey("sort_ascending")
        val onlyGames = booleanPreferencesKey("show_only_games")
        val hints = booleanPreferencesKey("immersive_hints")
    }

    val settings: StateFlow<AppSettings> = context.settingsStore.data
        .catch { error ->
            if (error is IOException) emit(androidx.datastore.preferences.core.emptyPreferences())
            else throw error
        }
        .map { prefs -> prefs.toSettings() }
        .stateIn(scope, SharingStarted.Eagerly, AppSettings())

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        val updated = transform(settings.value)
        context.settingsStore.edit { prefs ->
            prefs[Keys.theme] = updated.themeMode.name
            prefs[Keys.accent] = updated.accentId
            prefs[Keys.dynamic] = updated.useDynamicColor
            prefs[Keys.coverScale] = updated.coverScale
            prefs[Keys.animations] = updated.animations
            prefs[Keys.blur] = updated.blur
            prefs[Keys.background] = updated.backgroundStyle.name
            prefs[Keys.uiScale] = updated.uiScale.name
            prefs[Keys.language] = updated.language.name
            prefs[Keys.libraryLayout] = updated.libraryLayout.name
            prefs[Keys.gamingDensity] = updated.gamingDensity.name
            prefs[Keys.gamingDensityCustom] = updated.gamingDensityCustom

            prefs[Keys.autoGaming] = updated.autoGamingOnConnect
            prefs[Keys.autoNormal] = updated.autoNormalOnDisconnect
            prefs[Keys.disconnectPrompt] = updated.showDisconnectPrompt
            prefs[Keys.monitor] = updated.monitorInBackground
            prefs[Keys.openFromApps] = updated.openFromOtherApps
            prefs[Keys.detectUsb] = updated.detectUsb
            prefs[Keys.detectBluetooth] = updated.detectBluetooth
            prefs[Keys.glyph] = updated.glyphProfile
            prefs[Keys.overlayCombo] = updated.overlayCombo.name

            prefs[Keys.autoMetadata] = updated.autoMetadata
            prefs[Keys.rawg] = updated.rawgKey
            prefs[Keys.igdbId] = updated.igdbClientId
            prefs[Keys.igdbSecret] = updated.igdbSecret
            prefs[Keys.sgdb] = updated.sgdbKey

            prefs[Keys.sortMode] = updated.sortMode.name
            prefs[Keys.sortAsc] = updated.sortAscending
            prefs[Keys.onlyGames] = updated.showOnlyGames
            prefs[Keys.hints] = updated.immersiveHints
        }
    }

    private fun Preferences.toSettings(): AppSettings {
        fun enumOrNull(name: String?): ThemeMode? =
            ThemeMode.entries.firstOrNull { it.name == name }

        return AppSettings(
            themeMode = enumOrNull(this[Keys.theme]) ?: ThemeMode.DARK,
            accentId = this[Keys.accent] ?: "playstation",
            useDynamicColor = this[Keys.dynamic] ?: false,
            coverScale = (this[Keys.coverScale] ?: 1f).coerceIn(0.7f, 1.5f),
            animations = this[Keys.animations] ?: true,
            blur = (this[Keys.blur] ?: 0.55f).coerceIn(0f, 1f),
            backgroundStyle = BackgroundStyle.entries.firstOrNull { it.name == this[Keys.background] }
                ?: BackgroundStyle.ARTWORK,
            uiScale = UiScale.entries.firstOrNull { it.name == this[Keys.uiScale] } ?: UiScale.COMPACT,
            language = AppLanguage.entries.firstOrNull { it.name == this[Keys.language] } ?: AppLanguage.SYSTEM,
            libraryLayout = LibraryLayout.entries.firstOrNull { it.name == this[Keys.libraryLayout] }
                ?: LibraryLayout.GRID,
            gamingDensity = GamingDensity.entries.firstOrNull { it.name == this[Keys.gamingDensity] }
                ?: GamingDensity.LARGE,
            gamingDensityCustom = (this[Keys.gamingDensityCustom] ?: 1.15f).coerceIn(0.8f, 1.5f),

            autoGamingOnConnect = this[Keys.autoGaming] ?: true,
            autoNormalOnDisconnect = this[Keys.autoNormal] ?: false,
            showDisconnectPrompt = this[Keys.disconnectPrompt] ?: true,
            monitorInBackground = this[Keys.monitor] ?: false,
            openFromOtherApps = this[Keys.openFromApps] ?: false,
            detectUsb = this[Keys.detectUsb] ?: true,
            detectBluetooth = this[Keys.detectBluetooth] ?: true,
            glyphProfile = this[Keys.glyph] ?: "auto",
            overlayCombo = OverlayCombo.fromId(this[Keys.overlayCombo] ?: OverlayCombo.L1_R1.name),

            autoMetadata = this[Keys.autoMetadata] ?: true,
            rawgKey = this[Keys.rawg] ?: "",
            igdbClientId = this[Keys.igdbId] ?: "",
            igdbSecret = this[Keys.igdbSecret] ?: "",
            sgdbKey = this[Keys.sgdb] ?: "",

            sortMode = SortMode.entries.firstOrNull { it.name == this[Keys.sortMode] } ?: SortMode.TITLE,
            sortAscending = this[Keys.sortAsc] ?: true,
            showOnlyGames = this[Keys.onlyGames] ?: true,
            immersiveHints = this[Keys.hints] ?: true
        )
    }
}
