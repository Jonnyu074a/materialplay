package com.materialplay.launcher.games

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.materialplay.launcher.data.SessionRepository
import com.materialplay.launcher.data.db.GameDao
import com.materialplay.launcher.data.db.GameEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface LaunchResult {
    data class Success(val packageName: String) : LaunchResult
    data class Missing(val packageName: String) : LaunchResult
    data class Error(val message: String) : LaunchResult
}

class GameLauncher(
    private val context: Context,
    private val sessionRepository: SessionRepository,
    private val gameDao: GameDao
) {
    fun launchIntent(packageName: String): Intent? {
        val pm = context.packageManager
        val intent = pm.getLaunchIntentForPackage(packageName) ?: return null
        intent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        )
        return intent
    }

    suspend fun launch(game: GameEntity): LaunchResult = withContext(Dispatchers.Main) {
        val intent = launchIntent(game.packageName)
            ?: return@withContext LaunchResult.Missing(game.packageName)
        return@withContext try {
            sessionRepository.start(game.packageName)
            context.startActivity(intent)
            LaunchResult.Success(game.packageName)
        } catch (error: ActivityNotFoundException) {
            sessionRepository.closeOpenSession()
            LaunchResult.Missing(game.packageName)
        } catch (error: SecurityException) {
            sessionRepository.closeOpenSession()
            LaunchResult.Error("Android blocked the launch of ${game.label}")
        } catch (error: Exception) {
            sessionRepository.closeOpenSession()
            LaunchResult.Error(error.message ?: "Could not launch ${game.label}")
        }
    }

    fun isInstalled(packageName: String): Boolean = runCatching {
        context.packageManager.getApplicationInfo(packageName, 0)
        true
    }.getOrDefault(false)
}
