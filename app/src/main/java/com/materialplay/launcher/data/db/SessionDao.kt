package com.materialplay.launcher.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Insert
    suspend fun insert(session: SessionEntity): Long

    @Query("SELECT * FROM sessions ORDER BY startedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 30): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    suspend fun openSession(): SessionEntity?

    @Query("UPDATE sessions SET endedAt = :end, durationMs = MAX(0, :end - startedAt) WHERE id = :id")
    suspend fun close(id: Long, end: Long)

    @Query("SELECT packageName, SUM(durationMs) AS total, COUNT(*) AS count FROM sessions GROUP BY packageName ORDER BY total DESC")
    fun observeTotals(): Flow<List<PlayTotals>>

    @Query("DELETE FROM sessions")
    suspend fun clear()
}
