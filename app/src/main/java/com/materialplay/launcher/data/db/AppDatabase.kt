package com.materialplay.launcher.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [GameEntity::class, SessionEntity::class, ControllerEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao
    abstract fun sessionDao(): SessionDao
    abstract fun controllerDao(): ControllerDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "materialplay.db")
                .fallbackToDestructiveMigration()
                .build()
    }
}
