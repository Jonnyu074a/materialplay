package com.materialplay.launcher.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM games WHERE installed = 1 AND isExcluded = 0 ORDER BY label COLLATE NOCASE ASC")
    fun observeVisible(): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE installed = 1 AND isExcluded = 0 ORDER BY label COLLATE NOCASE ASC")
    suspend fun visibleNow(): List<GameEntity>

    @Query("SELECT * FROM games WHERE isExcluded = 0 ORDER BY label COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE installed = 1 AND isExcluded = 1 ORDER BY label COLLATE NOCASE ASC")
    fun observeHidden(): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE isFavorite = 1 AND isExcluded = 0 ORDER BY label COLLATE NOCASE ASC")
    fun observeFavorites(): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE packageName = :packageName LIMIT 1")
    fun observe(packageName: String): Flow<GameEntity?>

    @Query("SELECT * FROM games WHERE packageName = :packageName LIMIT 1")
    suspend fun get(packageName: String): GameEntity?

    @Query("SELECT * FROM games")
    suspend fun allNow(): List<GameEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(game: GameEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(games: List<GameEntity>)

    @Update
    suspend fun update(game: GameEntity)

    @Query("UPDATE games SET installed = 0 WHERE packageName = :packageName")
    suspend fun markUninstalled(packageName: String)

    @Query("UPDATE games SET installed = 1 WHERE packageName = :packageName")
    suspend fun markInstalled(packageName: String)

    @Query("UPDATE games SET isFavorite = :favorite WHERE packageName = :packageName")
    suspend fun setFavorite(packageName: String, favorite: Boolean)

    @Query("UPDATE games SET isExcluded = :excluded WHERE packageName = :packageName")
    suspend fun setExcluded(packageName: String, excluded: Boolean)

    @Query("UPDATE games SET lastPlayedAt = :time, playCount = playCount + 1 WHERE packageName = :packageName")
    suspend fun markPlayed(packageName: String, time: Long)

    @Query("DELETE FROM games WHERE packageName = :packageName")
    suspend fun delete(packageName: String)

    @Query("DELETE FROM games WHERE installed = 0 AND isManual = 1")
    suspend fun purgeMissingManual()
}
