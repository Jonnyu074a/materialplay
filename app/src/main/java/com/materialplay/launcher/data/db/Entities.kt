package com.materialplay.launcher.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val startedAt: Long,
    val endedAt: Long? = null,
    val durationMs: Long = 0,
    /** Always true today; reserved for imports from other sources. */
    val fromLauncher: Boolean = true
)

@Entity(tableName = "controllers")
data class ControllerEntity(
    @PrimaryKey val descriptor: String,
    val name: String,
    val vendorId: Int = 0,
    val productId: Int = 0,
    val connectionType: String = "unknown",
    /** "auto" follows the global profile, otherwise a glyph profile name. */
    val glyphProfile: String = "auto",
    /** Overrides automatic face-button layout detection for this controller. */
    val faceSwap: Boolean? = null,
    val lastSeen: Long = System.currentTimeMillis()
)

data class PlayTotals(
    val packageName: String,
    val total: Long,
    val count: Int
)
