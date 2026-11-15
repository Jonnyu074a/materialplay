package com.materialplay.launcher.metadata

import android.net.Uri
import android.util.Log
import com.materialplay.launcher.data.db.GameDao
import com.materialplay.launcher.data.db.GameEntity
import com.materialplay.launcher.data.settings.SettingsRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray

sealed interface RefreshOutcome {
    data class Success(val message: String, val provider: String) : RefreshOutcome
    data class NotFound(val message: String) : RefreshOutcome
    data class Error(val message: String) : RefreshOutcome
}

/**
 * Coordinates providers, image downloads and persistence of a game's presentation data.
 */
class MetadataRepository(
    private val gameDao: GameDao,
    private val settingsRepository: SettingsRepository,
    private val images: ImageStore,
    private val providers: List<MetadataProvider>,
    private val artProviders: List<ArtProvider>
) {
    private val autoMutex = Mutex()

    private val configuredProviders: List<MetadataProvider>
        get() {
            val snapshot = settingsRepository.settings.value
            return providers.filter { it.isConfigured(snapshot) }
        }

    suspend fun searchCandidates(title: String, query: String = title): List<MetadataCandidate> {
        val snapshot = settingsRepository.settings.value
        val active = providers.filter { it.isConfigured(snapshot) }
        if (active.isEmpty()) return emptyList()
        val results = mutableListOf<MetadataCandidate>()
        var lastError: Throwable? = null
        for (provider in active) {
            try {
                results += provider.search(query, snapshot)
            } catch (error: Throwable) {
                lastError = error
                Log.w("Metadata", "${provider.id} search failed", error)
            }
        }
        if (results.isEmpty() && lastError != null) {
            throw ProviderException(lastError.message ?: "Metadata provider failed", lastError)
        }
        return TitleMatcher.rank(title, results).take(20)
    }

    /** Fetches the best match automatically using the installed app label. */
    suspend fun refresh(game: GameEntity, queryOverride: String? = null): RefreshOutcome {
        val snapshot = settingsRepository.settings.value
        if (providers.none { it.isConfigured(snapshot) } &&
            artProviders.none { it.isConfigured(snapshot) }
        ) {
            return RefreshOutcome.Error(NoProvidersMessage)
        }
        val query = queryOverride?.takeIf { it.isNotBlank() } ?: game.label
        val candidates = try {
            searchCandidates(game.label, query)
        } catch (error: Throwable) {
            return RefreshOutcome.Error(error.message ?: "Metadata search failed")
        }

        val best = TitleMatcher.best(game.label, candidates)
            ?: TitleMatcher.best(query, candidates)
        val art = fetchArt(query)
        if (best == null && art == null) {
            val configured = configuredProviders.isNotEmpty()
            val outcome = if (configured) {
                RefreshOutcome.NotFound("No confident match for \u201C${game.label}\u201D")
            } else {
                RefreshOutcome.Error(NoProvidersMessage)
            }
            if (configured) {
                gameDao.update(
                    game.copy(
                        metadataStatus = "notfound",
                        matchQuery = query
                    )
                )
            }
            return outcome
        }
        return persist(
            game = game,
            candidate = best,
            artwork = art ?: best?.toArtwork(),
            status = if (best != null) "matched" else "matched",
            query = query
        )
    }

    /** Applies a specific candidate chosen by the user. */
    suspend fun applyCandidate(game: GameEntity, candidate: MetadataCandidate): RefreshOutcome {
        val art = runCatching { fetchArt(candidate.title) }.getOrNull() ?: candidate.toArtwork()
        return persist(game, candidate, art, "manual", candidate.title)
    }

    /** Replaces just the artwork with a local image. */
    suspend fun setCustomArt(game: GameEntity, kind: String, uri: Uri): RefreshOutcome {
        val path = images.import(game.packageName, kind, uri)
            ?: return RefreshOutcome.Error("Could not read the selected image")
        val updated = when (kind) {
            "cover" -> game.copy(coverPath = path)
            "hero" -> game.copy(heroPath = path)
            else -> game.copy(coverPath = path, heroPath = path)
        }
        gameDao.update(updated)
        return RefreshOutcome.Success("Artwork updated", "local")
    }

    suspend fun clearMetadata(game: GameEntity) {
        images.deleteAll(game.packageName)
        gameDao.update(
            game.copy(
                metadataStatus = "none",
                matchQuery = null,
                source = null,
                sourceId = null,
                description = null,
                genres = null,
                developer = null,
                releaseDate = null,
                coverPath = null,
                heroPath = null,
                logoPath = null,
                screenshots = null
            )
        )
    }

    /**
     * Refreshes a small batch of games that have no metadata yet. Called after a
     * library scan, guarded so repeated scans never hammer the APIs.
     */
    suspend fun refreshMissing(limit: Int = 4): Int = autoMutex.withLock {
        val snapshot = settingsRepository.settings.value
        if (!snapshot.autoMetadata || !snapshot.hasAnyMetadataKey) return@withLock 0
        val pending = gameDao.visibleNow()
            .filter { it.metadataStatus == "none" && it.coverPath == null }
            .take(limit)
        var done = 0
        for (game in pending) {
            val outcome = refresh(game)
            if (outcome is RefreshOutcome.Success) done++
            delay(700)
        }
        done
    }

    private suspend fun fetchArt(title: String): Artwork? {
        val snapshot = settingsRepository.settings.value
        for (provider in artProviders.filter { it.isConfigured(snapshot) }) {
            val art = runCatching { provider.findArt(title, snapshot) }.getOrNull()
            if (art != null) return art
        }
        return null
    }

    private suspend fun persist(
        game: GameEntity,
        candidate: MetadataCandidate?,
        artwork: Artwork?,
        status: String,
        query: String
    ): RefreshOutcome {
        val pkg = game.packageName
        val cover = artwork?.coverUrl?.let { images.download(it, pkg, "cover") }
            ?: candidate?.coverUrl?.let { images.download(it, pkg, "cover") }
        val heroUrl = artwork?.heroUrl ?: candidate?.heroUrl
        val hero = heroUrl?.let { images.download(it, pkg, "hero") }
            ?: candidate?.heroUrl?.let { images.download(it, pkg, "hero") }
        val logo = artwork?.logoUrl?.let { images.download(it, pkg, "logo") }

        val screenshots = mutableListOf<String>()
        val remoteShots = (artwork?.screenshots.orEmpty() + candidate?.screenshots.orEmpty())
            .distinct()
            .take(6)
        if (remoteShots.isNotEmpty()) {
            images.deleteByPrefix(pkg, "shot-")
            remoteShots.forEachIndexed { index, url ->
                images.download(url, pkg, "shot-$index")?.let { screenshots += it }
            }
        }

        val genres = candidate?.genres.orEmpty().distinct().take(6)
        val updated = game.copy(
            metadataStatus = status,
            matchQuery = query,
            source = artwork?.let { artProviders.firstOrNull { p -> p.isConfigured(settingsRepository.settings.value) }?.id }
                ?: candidate?.provider,
            sourceId = candidate?.sourceId,
            description = candidate?.description ?: game.description,
            genres = genres.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: game.genres,
            developer = candidate?.developers?.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: game.developer,
            releaseDate = candidate?.releaseDate ?: game.releaseDate,
            coverPath = cover ?: game.coverPath,
            heroPath = hero ?: game.heroPath,
            logoPath = logo ?: game.logoPath,
            screenshots = if (screenshots.isNotEmpty()) {
                JSONArray(screenshots).toString()
            } else game.screenshots
        )
        gameDao.update(updated)

        val providerLabel = candidate?.sourceLabel
            ?: artProviders.firstOrNull { it.isConfigured(settingsRepository.settings.value) }?.displayName
            ?: "provider"
        val title = candidate?.title ?: game.label
        return RefreshOutcome.Success("Matched to \u201C$title\u201D via $providerLabel", providerLabel)
    }

    companion object {
        const val NoProvidersMessage =
            "Add a RAWG, IGDB or SteamGridDB key in Settings to download covers and metadata."
    }
}

fun GameEntity.toArtwork(): Artwork = Artwork(
    coverUrl = coverPath?.let { "file://$it" },
    heroUrl = heroPath?.let { "file://$it" },
    logoUrl = logoPath?.let { "file://$it" },
    screenshots = parseScreenshots(screenshots)
)

fun MetadataCandidate.toArtwork(): Artwork = Artwork(
    coverUrl = coverUrl,
    heroUrl = heroUrl,
    logoUrl = logoUrl,
    screenshots = screenshots
)

fun parseScreenshots(raw: String?): List<String> {
    if (raw.isNullOrBlank()) return emptyList()
    return runCatching {
        val array = JSONArray(raw)
        (0 until array.length()).mapNotNull { array.optString(it).takeIf { path -> path.isNotBlank() } }
    }.getOrDefault(emptyList())
}
