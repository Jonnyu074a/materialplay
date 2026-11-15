package com.materialplay.launcher.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "games")
data class GameEntity(
    @PrimaryKey val packageName: String,
    val label: String,
    val isManual: Boolean = false,
    val isExcluded: Boolean = false,
    val isFavorite: Boolean = false,
    val installed: Boolean = true,
    val addedAt: Long = System.currentTimeMillis(),
    val lastPlayedAt: Long = 0L,
    val playCount: Int = 0,
    /** none | matched | manual | notfound */
    val metadataStatus: String = "none",
    val matchQuery: String? = null,
    val source: String? = null,
    val sourceId: String? = null,
    val description: String? = null,
    /** Comma separated for compact storage. */
    val genres: String? = null,
    val developer: String? = null,
    val releaseDate: String? = null,
    val coverPath: String? = null,
    val heroPath: String? = null,
    val logoPath: String? = null,
    /** JSON array of local screenshot paths. */
    val screenshots: String? = null
) {
    val hasCustomArt: Boolean get() = source == "manual"
    val displayGenre: String? get() = genres?.split(",")?.firstOrNull()?.trim()?.ifBlank { null }
}
