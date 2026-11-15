package com.materialplay.launcher.di

import android.content.Context
import com.materialplay.launcher.data.ControllerRepository
import com.materialplay.launcher.data.SessionRepository
import com.materialplay.launcher.data.db.AppDatabase
import com.materialplay.launcher.data.settings.SettingsRepository
import com.materialplay.launcher.gamepad.GamepadManager
import com.materialplay.launcher.gamepad.GamepadRawState
import com.materialplay.launcher.gamepad.GamepadRouter
import com.materialplay.launcher.games.GameLauncher
import com.materialplay.launcher.games.GameRepository
import com.materialplay.launcher.games.InstalledAppScanner
import com.materialplay.launcher.games.SyncSummary
import com.materialplay.launcher.metadata.IgdbProvider
import com.materialplay.launcher.metadata.ImageStore
import com.materialplay.launcher.metadata.MetadataRepository
import com.materialplay.launcher.metadata.RawgProvider
import com.materialplay.launcher.metadata.SteamGridDbProvider
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Cache
import okhttp3.OkHttpClient

/**
 * Manual dependency container. Keeping construction explicit makes the ownership of
 * every long-lived process (DB, HTTP cache, input listener) obvious and testable.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val isForeground = MutableStateFlow(false)

    val database: AppDatabase by lazy { AppDatabase.create(appContext) }
    val settings by lazy { SettingsRepository(appContext, appScope) }

    val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .callTimeout(45, TimeUnit.SECONDS)
            .cache(Cache(File(appContext.cacheDir, "http"), 48L * 1024 * 1024))
            .build()
    }

    val images: ImageStore by lazy { ImageStore(appContext, httpClient) }
    val rawInput = GamepadRawState()
    val gamepadRouter: GamepadRouter by lazy { GamepadRouter(appScope, rawInput) }
    val gamepadManager: GamepadManager by lazy {
        GamepadManager(appContext, appScope).also { it.start() }
    }

    val scanner: InstalledAppScanner by lazy { InstalledAppScanner(appContext) }
    val sessionRepository: SessionRepository by lazy {
        SessionRepository(database.sessionDao(), database.gameDao())
    }
    val gameLauncher: GameLauncher by lazy {
        GameLauncher(appContext, sessionRepository, database.gameDao())
    }
    val gameRepository: GameRepository by lazy {
        GameRepository(appContext, database.gameDao(), scanner, gameLauncher)
    }
    val controllerRepository by lazy { ControllerRepository(database.controllerDao()) }

    val metadataRepository: MetadataRepository by lazy {
        MetadataRepository(
            gameDao = database.gameDao(),
            settingsRepository = settings,
            images = images,
            providers = listOf(RawgProvider(httpClient), IgdbProvider(httpClient)),
            artProviders = listOf(SteamGridDbProvider(httpClient))
        )
    }

    private val syncMutex = Mutex()

    /** Scan + optionally enrich new games. Serialised so two triggers never overlap. */
    suspend fun syncLibrary(enrich: Boolean = true): SyncSummary = syncMutex.withLock {
        val summary = gameRepository.syncLibrary(onlyGames = settings.settings.value.showOnlyGames)
        if (enrich) {
            appScope.launch {
                runCatching { metadataRepository.refreshMissing(3) }
            }
        }
        summary
    }
}
