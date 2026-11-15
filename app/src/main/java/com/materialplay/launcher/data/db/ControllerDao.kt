package com.materialplay.launcher.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ControllerDao {
    @Query("SELECT * FROM controllers ORDER BY lastSeen DESC")
    fun observeAll(): Flow<List<ControllerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ControllerEntity)

    @Query("SELECT * FROM controllers WHERE descriptor = :descriptor LIMIT 1")
    suspend fun get(descriptor: String): ControllerEntity?

    @Query("UPDATE controllers SET glyphProfile = :profile WHERE descriptor = :descriptor")
    suspend fun setGlyphProfile(descriptor: String, profile: String)

    @Query("UPDATE controllers SET faceSwap = :swap WHERE descriptor = :descriptor")
    suspend fun setFaceSwap(descriptor: String, swap: Boolean?)
}
