package com.materialplay.launcher.games

import android.content.Context
import com.materialplay.launcher.data.db.GameDao
import com.materialplay.launcher.data.db.GameEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

data class SyncSummary(
    val scannedApps: Int,
    val games: Int,
    val added: Int,
    val removed: Int
)

class GameRepository(
    private val context: Context,
    private val gameDao: GameDao,
    private val scanner: InstalledAppScanner,
    private val launcher: GameLauncher
) {
    val visibleGames: Flow<List<GameEntity>> = gameDao.observeVisible()
    val allGames: Flow<List<GameEntity>> = gameDao.observeAll()
    val hiddenGames: Flow<List<GameEntity>> = gameDao.observeHidden()
    val favorites: Flow<List<GameEntity>> = gameDao.observeFavorites()

    fun game(packageName: String): Flow<GameEntity?> = gameDao.observe(packageName)

    suspend fun syncLibrary(onlyGames: Boolean = true): SyncSummary = withContext(Dispatchers.IO) {
        val scan = scanner.scan()
        val existing = gameDao.allNow().associateBy { it.packageName }
        val launcherPackages = scan.apps.associateBy { it.packageName }
        val changed = mutableListOf<GameEntity>()
        var added = 0

        for (app in scan.apps) {
            val previous = existing[app.packageName]
            val trackedManually = previous?.isManual == true
            if (!app.isGame && !trackedManually && onlyGames) continue
            if (previous == null) {
                added++
                changed += GameEntity(
                    packageName = app.packageName,
                    label = app.label,
                    installed = true,
                    addedAt = System.currentTimeMillis()
                )
            } else if (previous.label != app.label || !previous.installed) {
                changed += previous.copy(label = app.label, installed = true)
            }
        }

        var removed = 0
        for (previous in existing.values) {
            if (launcherPackages.containsKey(previous.packageName)) continue
            if (!scanner.isInstalled(previous.packageName) && previous.installed) {
                changed += previous.copy(installed = false)
                removed++
            }
        }

        if (changed.isNotEmpty()) gameDao.upsertAll(changed)
        gameDao.purgeMissingManual()

        SyncSummary(
            scannedApps = scan.apps.size,
            games = scan.gameCount,
            added = added,
            removed = removed
        )
    }

    /** Adds any launchable app to the library, even if it does not declare a game category. */
    suspend fun addManually(packageName: String): Boolean = withContext(Dispatchers.IO) {
        val app = scanner.scan().apps.firstOrNull { it.packageName == packageName } ?: return@withContext false
        val existing = gameDao.get(packageName)
        val updated = existing?.copy(isManual = true, isExcluded = false, installed = true)
            ?: GameEntity(
                packageName = packageName,
                label = app.label,
                isManual = true,
                installed = true
            )
        gameDao.upsert(updated)
        true
    }

    suspend fun setFavorite(packageName: String, favorite: Boolean) =
        gameDao.setFavorite(packageName, favorite)

    /** Excluded apps stay in the database (so stats survive) but disappear from the library. */
    suspend fun setExcluded(packageName: String, excluded: Boolean) =
        gameDao.setExcluded(packageName, excluded)

    suspend fun remove(packageName: String) = gameDao.delete(packageName)

    suspend fun launch(game: GameEntity): LaunchResult = launcher.launch(game)

    fun isInstalled(packageName: String): Boolean = launcher.isInstalled(packageName)
}
