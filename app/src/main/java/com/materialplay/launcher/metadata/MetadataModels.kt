package com.materialplay.launcher.metadata

import com.materialplay.launcher.data.settings.AppSettings

data class MetadataCandidate(
    val provider: String,
    val sourceId: String,
    val title: String,
    val description: String? = null,
    val genres: List<String> = emptyList(),
    val developers: List<String> = emptyList(),
    val releaseDate: String? = null,
    val coverUrl: String? = null,
    val heroUrl: String? = null,
    val logoUrl: String? = null,
    val screenshots: List<String> = emptyList(),
    val score: Double = 0.0
) {
    val sourceLabel: String
        get() = when (provider) {
            "rawg" -> "RAWG"
            "igdb" -> "IGDB"
            "steamgriddb" -> "SteamGridDB"
            else -> provider
        }
}

data class Artwork(
    val coverUrl: String? = null,
    val heroUrl: String? = null,
    val logoUrl: String? = null,
    val screenshots: List<String> = emptyList()
)

interface MetadataProvider {
    val id: String
    val displayName: String
    fun isConfigured(settings: AppSettings): Boolean
    suspend fun search(title: String, settings: AppSettings): List<MetadataCandidate>
}

interface ArtProvider {
    val id: String
    val displayName: String
    fun isConfigured(settings: AppSettings): Boolean
    suspend fun findArt(title: String, settings: AppSettings): Artwork?
}

class ProviderException(message: String, cause: Throwable? = null) : Exception(message, cause)
