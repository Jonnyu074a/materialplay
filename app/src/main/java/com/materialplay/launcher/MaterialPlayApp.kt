package com.materialplay.launcher

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.materialplay.launcher.di.AppContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MaterialPlayApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        createNotificationChannels()

        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                container.isForeground.value = true
            }

            override fun onStop(owner: LifecycleOwner) {
                container.isForeground.value = false
            }
        })

        // Reconcile the library on cold start without blocking the first frame.
        container.appScope.launch(Dispatchers.IO) {
            runCatching { container.syncLibrary(enrich = true) }
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_MONITOR,
                getString(R.string.monitor_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.monitor_channel_desc)
                setShowBadge(false)
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_GAMING,
                getString(R.string.gaming_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.gaming_channel_desc)
                enableVibration(true)
            }
        )
    }

    companion object {
        const val CHANNEL_MONITOR = "monitor"
        const val CHANNEL_GAMING = "gaming"
    }
}
