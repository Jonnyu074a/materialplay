package com.materialplay.launcher.data

import com.materialplay.launcher.data.db.GameDao
import com.materialplay.launcher.data.db.SessionDao
import com.materialplay.launcher.data.db.SessionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Tracks launches performed from Material Play. The measured time is the interval
 * between launch and the launcher returning to the foreground, so it is explicitly
 * an approximation of real play time.
 */
class SessionRepository(
    private val sessionDao: SessionDao,
    private val gameDao: GameDao
) {
    val recentSessions: Flow<List<SessionEntity>> = sessionDao.observeRecent(40)
    val allSessions: Flow<List<SessionEntity>> = sessionDao.observeAll()
    val totals: Flow<List<com.materialplay.launcher.data.db.PlayTotals>> = sessionDao.observeTotals()

    suspend fun start(packageName: String) = withContext(Dispatchers.IO) {
        closeOpenSession()
        val now = System.currentTimeMillis()
        sessionDao.insert(
            SessionEntity(
                packageName = packageName,
                startedAt = now,
                endedAt = null,
                durationMs = 0
            )
        )
        gameDao.markPlayed(packageName, now)
    }

    suspend fun closeOpenSession() = withContext(Dispatchers.IO) {
        val open = sessionDao.openSession() ?: return@withContext
        sessionDao.close(open.id, System.currentTimeMillis())
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        sessionDao.clear()
    }
}
