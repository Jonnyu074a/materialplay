package com.materialplay.launcher.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.materialplay.launcher.MaterialPlayApp
import com.materialplay.launcher.data.db.ControllerEntity
import com.materialplay.launcher.data.db.GameEntity
import com.materialplay.launcher.data.db.PlayTotals
import com.materialplay.launcher.data.db.SessionEntity
import com.materialplay.launcher.data.settings.AppSettings
import com.materialplay.launcher.gamepad.ConnectedController
import com.materialplay.launcher.gamepad.RawPadState
import com.materialplay.launcher.games.LaunchResult
import com.materialplay.launcher.games.ScannedApp
import com.materialplay.launcher.metadata.MetadataCandidate
import com.materialplay.launcher.metadata.RefreshOutcome
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Shared presentation logic for Normal Mode. Gaming Mode uses the same repositories
 * through [com.materialplay.launcher.gaming.GamingViewModel], so both surfaces stay
 * consistent without duplicating data rules.
 */
open class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as MaterialPlayApp).container

    val settings: StateFlow<AppSettings> = container.settings.settings
    val games: StateFlow<List<GameEntity>> = container.gameRepository.visibleGames
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val allGames: StateFlow<List<GameEntity>> = container.gameRepository.allGames
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val hiddenGames: StateFlow<List<GameEntity>> = container.gameRepository.hiddenGames
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val favorites: StateFlow<List<GameEntity>> = container.gameRepository.favorites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val sessions: StateFlow<List<SessionEntity>> = container.sessionRepository.recentSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val allSessions: StateFlow<List<SessionEntity>> = container.sessionRepository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val totals: StateFlow<List<PlayTotals>> = container.sessionRepository.totals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val controllers: StateFlow<List<ConnectedController>> = container.gamepadManager.controllers
    val savedControllers: StateFlow<List<ControllerEntity>> = container.controllerRepository.savedControllers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val rawInput: StateFlow<RawPadState> = container.rawInput.state

    /** Semantic controller actions. Gaming Mode is the only active consumer. */
    val padActions = container.gamepadRouter.actions

    /** Fired when the configured overlay chord is held. */
    val overlayRequests = container.gamepadRouter.overlayRequests

    private val _syncing = MutableStateFlow(false)
    val syncing: StateFlow<Boolean> = _syncing.asStateFlow()

    private val _refreshing = MutableStateFlow<String?>(null)
    val refreshing: StateFlow<String?> = _refreshing.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages = _messages.asSharedFlow()

    init {
        viewModelScope.launch {
            container.gamepadManager.controllers.collect { connected ->
                connected.forEach { container.controllerRepository.remember(it) }
            }
        }
        syncLibrary(showMessage = false)
    }

    fun syncLibrary(showMessage: Boolean = true) {
        if (_syncing.value) return
        viewModelScope.launch {
            _syncing.value = true
            try {
                val summary = container.syncLibrary()
                if (showMessage) {
                    _messages.emit(
                        "Library scanned: ${summary.games} games • ${summary.added} new"
                    )
                }
            } catch (error: Throwable) {
                _messages.emit("Scan failed: ${error.message ?: "unknown error"}")
            } finally {
                _syncing.value = false
            }
        }
    }

    fun launch(game: GameEntity) {
        viewModelScope.launch {
            when (val result = container.gameRepository.launch(game)) {
                is LaunchResult.Success -> Unit
                is LaunchResult.Missing -> {
                    _messages.emit("${game.label} is not installed anymore")
                    syncLibrary(showMessage = false)
                }
                is LaunchResult.Error -> _messages.emit(result.message)
            }
        }
    }

    fun toggleFavorite(game: GameEntity) {
        viewModelScope.launch {
            container.gameRepository.setFavorite(game.packageName, !game.isFavorite)
        }
    }

    fun setExcluded(game: GameEntity, excluded: Boolean) {
        viewModelScope.launch {
            container.gameRepository.setExcluded(game.packageName, excluded)
            _messages.emit(if (excluded) "${game.label} hidden from the library" else "${game.label} restored")
        }
    }

    fun refreshMetadata(game: GameEntity, query: String? = null) {
        if (_refreshing.value != null) return
        viewModelScope.launch {
            _refreshing.value = game.packageName
            val outcome = container.metadataRepository.refresh(game, query)
            _refreshing.value = null
            _messages.emit(messageOf(outcome))
        }
    }

    suspend fun findCandidates(game: GameEntity, query: String): List<MetadataCandidate> =
        runCatching { container.metadataRepository.searchCandidates(game.label, query) }
            .getOrElse { error ->
                _messages.emit("Search failed: ${error.message ?: "unknown error"}")
                emptyList()
            }

    suspend fun applyCandidate(game: GameEntity, candidate: MetadataCandidate): RefreshOutcome {
        val outcome = container.metadataRepository.applyCandidate(game, candidate)
        _messages.emit(messageOf(outcome))
        return outcome
    }

    suspend fun setCustomArt(game: GameEntity, kind: String, uri: Uri): RefreshOutcome {
        val outcome = container.metadataRepository.setCustomArt(game, kind, uri)
        _messages.emit(messageOf(outcome))
        return outcome
    }

    suspend fun clearMetadata(game: GameEntity) {
        container.metadataRepository.clearMetadata(game)
        _messages.emit("Metadata cleared for ${game.label}")
    }

    suspend fun launchableApps(): List<ScannedApp> =
        runCatching { container.scanner.scan().apps }.getOrDefault(emptyList())
            .sortedBy { it.label.lowercase() }

    fun addManually(packageName: String) {
        viewModelScope.launch {
            if (container.gameRepository.addManually(packageName)) {
                _messages.emit("Added to library")
            } else {
                _messages.emit("Could not add that app")
            }
        }
    }

    suspend fun appInfo(game: GameEntity) = runCatching {
        val pm = getApplication<Application>().packageManager
        pm.getApplicationInfo(game.packageName, 0)
    }.getOrNull()

    fun updateSettings(block: (AppSettings) -> AppSettings) {
        viewModelScope.launch { container.settings.update(block) }
    }

    fun setControllerProfile(descriptor: String, profile: String) {
        viewModelScope.launch {
            container.controllerRepository.setGlyphProfile(descriptor, profile)
        }
    }

    fun setControllerFaceSwap(descriptor: String, swap: Boolean?) {
        viewModelScope.launch {
            container.controllerRepository.setFaceSwap(descriptor, swap)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            container.sessionRepository.clearHistory()
            _messages.emit("Play history cleared")
        }
    }

    fun closeOpenSession() {
        viewModelScope.launch { container.sessionRepository.closeOpenSession() }
    }

    fun remember(controller: ConnectedController) {
        viewModelScope.launch { container.controllerRepository.remember(controller) }
    }

    private fun messageOf(outcome: RefreshOutcome): String = when (outcome) {
        is RefreshOutcome.Success -> outcome.message
        is RefreshOutcome.NotFound -> outcome.message
        is RefreshOutcome.Error -> outcome.message
    }

    companion object {
        fun factory(app: MaterialPlayApp): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return LauncherViewModel(app) as T
                }
            }
    }
}
