package com.materialplay.launcher.data.settings

enum class ThemeMode { SYSTEM, DARK, LIGHT }

enum class BackgroundStyle { ARTWORK, GRADIENT, SOLID }

enum class AppLanguage(val label: String) {
    SYSTEM("System"),
    SPANISH("Español"),
    ENGLISH("English")
}

enum class UiScale(val label: String, val factor: Float) {
    COMPACT("Compact", 0.86f),
    DEFAULT("Default", 1f),
    LARGE("Large", 1.14f)
}

enum class LibraryLayout(val label: String) {
    CAROUSEL("Carousel"),
    GRID("Grid"),
    LIST("List")
}

enum class GamingDensity(val label: String, val factor: Float) {
    COMPACT("Compact", 0.9f),
    DEFAULT("Default", 1.0f),
    LARGE("Large", 1.15f),
    EXTRA("Extra", 1.25f),
    CUSTOM("Custom", 1.0f)
}

enum class SortMode(val label: String) {
    TITLE("Title"),
    LAST_PLAYED("Last played"),
    PLAYTIME("Play time"),
    RECENTLY_ADDED("Recently added")
}

enum class OverlayCombo(val label: String, val actions: Set<String>) {
    L1_R1("L1 + R1", setOf("L1", "R1")),
    START_SELECT("Start + Select", setOf("START", "SELECT")),
    L3_R3("Stick clicks (L3 + R3)", setOf("L3", "R3")),
    BACK_START("Back + Start", setOf("BACK", "START"));

    companion object {
        fun fromId(id: String): OverlayCombo =
            entries.firstOrNull { it.name == id } ?: L1_R1
    }
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val accentId: String = "playstation",
    val useDynamicColor: Boolean = false,
    val coverScale: Float = 1f,
    val animations: Boolean = true,
    val blur: Float = 0.55f,
    val backgroundStyle: BackgroundStyle = BackgroundStyle.ARTWORK,
    val uiScale: UiScale = UiScale.COMPACT,
    val language: AppLanguage = AppLanguage.SYSTEM,

    val libraryLayout: LibraryLayout = LibraryLayout.GRID,

    val gamingDensity: GamingDensity = GamingDensity.LARGE,
    val gamingDensityCustom: Float = 1.15f,

    val autoGamingOnConnect: Boolean = true,
    val autoNormalOnDisconnect: Boolean = false,
    val showDisconnectPrompt: Boolean = true,
    val monitorInBackground: Boolean = false,
    val openFromOtherApps: Boolean = false,
    val detectUsb: Boolean = true,
    val detectBluetooth: Boolean = true,
    val glyphProfile: String = "auto",
    val overlayCombo: OverlayCombo = OverlayCombo.L1_R1,

    val autoMetadata: Boolean = true,
    val rawgKey: String = "",
    val igdbClientId: String = "",
    val igdbSecret: String = "",
    val sgdbKey: String = "",

    val sortMode: SortMode = SortMode.TITLE,
    val sortAscending: Boolean = true,
    val showOnlyGames: Boolean = true,
    val immersiveHints: Boolean = true
) {
    val hasAnyMetadataKey: Boolean
        get() = rawgKey.isNotBlank() || (igdbClientId.isNotBlank() && igdbSecret.isNotBlank())
}
