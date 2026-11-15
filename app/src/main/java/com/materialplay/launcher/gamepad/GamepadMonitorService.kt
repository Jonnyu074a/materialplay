package com.materialplay.launcher.gamepad

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.input.InputManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.materialplay.launcher.MaterialPlayApp
import com.materialplay.launcher.R
import com.materialplay.launcher.gaming.GamingActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Foreground service that keeps controller detection alive while the user is in
 * another app. When allowed it opens Gaming Mode directly (draw-over-apps
 * exemption); otherwise it raises a full-screen-intent notification, which is the
 * mechanism Android provides for background activity starts.
 */
class GamepadMonitorService : android.app.Service() {

    private lateinit var app: MaterialPlayApp
    private var observer: Job? = null
    private var lastLaunchAt = 0L
    private var previousIds: Set<String> = emptySet()
    private var started = false

    override fun onCreate() {
        super.onCreate()
        app = application as MaterialPlayApp
        startForegroundNotification()
        observeControllers()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        val settings = app.container.settings.settings.value
        if (!settings.monitorInBackground && !settings.openFromOtherApps) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?) = null

    override fun onDestroy() {
        observer?.cancel()
        super.onDestroy()
    }

    private fun startForegroundNotification() {
        if (started) return
        started = true
        val openIntent = PendingIntent.getActivity(
            this,
            10,
            Intent(this, GamingActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this,
            11,
            Intent(this, GamepadMonitorService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification: Notification = NotificationCompat.Builder(this, MaterialPlayApp.CHANNEL_MONITOR)
            .setSmallIcon(R.drawable.ic_tile)
            .setContentTitle(getString(R.string.monitor_channel_name))
            .setContentText("Watching for controllers to open Gaming Mode")
            .setContentIntent(openIntent)
            .addAction(0, getString(R.string.gaming_mode), openIntent)
            .addAction(0, "Stop", stopIntent)
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

        val type = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ->
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ->
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            else -> 0
        }
        runCatching { ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type) }
    }

    private fun observeControllers() {
        observer?.cancel()
        observer = app.container.appScope.launch {
            app.container.gamepadManager.controllers.collectLatest { controllers ->
                val ids = controllers.map { it.identity }.toSet()
                val added = ids - previousIds
                previousIds = ids
                if (added.isEmpty()) return@collectLatest
                val settings = app.container.settings.settings.value
                // Background opening needs both the auto-open preference and the
                // cross-app permission; either toggle can turn it off.
                if (!settings.autoGamingOnConnect || !settings.openFromOtherApps) return@collectLatest
                if (app.container.isForeground.value) return@collectLatest
                if (System.currentTimeMillis() - lastLaunchAt < 8000) return@collectLatest

                val controller = controllers.firstOrNull { it.identity in added } ?: return@collectLatest
                lastLaunchAt = System.currentTimeMillis()
                launchGamingMode(controller)
            }
        }
    }

    private fun launchGamingMode(controller: ConnectedController) {
        // Draw-over-other-apps is one of the documented exemptions from background
        // activity start restrictions, so it is the direct path when granted.
        if (Settings.canDrawOverlays(this)) {
            val intent = Intent(this, GamingActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(GamingActivity.EXTRA_AUTO_TRANSITION, true)
                .putExtra(GamingActivity.EXTRA_CONTROLLER_NAME, controller.displayName)
            val launched = runCatching { startActivity(intent) }.isSuccess
            if (launched) return
        }
        notifyControllerConnected(controller)
    }

    private fun notifyControllerConnected(controller: ConnectedController) {
        val notificationManager = getSystemService(NotificationManager::class.java) ?: return
        val fullScreenIntent = PendingIntent.getActivity(
            this,
            20,
            Intent(this, GamingActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(GamingActivity.EXTRA_AUTO_TRANSITION, true)
                .putExtra(GamingActivity.EXTRA_CONTROLLER_NAME, controller.displayName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val canFullScreen = if (Build.VERSION.SDK_INT >= 34) {
            runCatching { notificationManager.canUseFullScreenIntent() }.getOrDefault(false)
        } else true

        val builder = NotificationCompat.Builder(this, MaterialPlayApp.CHANNEL_GAMING)
            .setSmallIcon(R.drawable.ic_tile)
            .setContentTitle("${controller.displayName} connected")
            .setContentText("Open Gaming Mode to play with the controller")
            .setContentIntent(fullScreenIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .addAction(0, "Open Gaming Mode", fullScreenIntent)

        if (canFullScreen) {
            builder.setFullScreenIntent(fullScreenIntent, true)
        }
        notificationManager.notify(NOTIFICATION_CONNECTED, builder.build())
    }

    companion object {
        const val ACTION_STOP = "com.materialplay.launcher.STOP_MONITOR"
        private const val NOTIFICATION_ID = 4101
        private const val NOTIFICATION_CONNECTED = 4102

        fun start(context: Context) {
            val intent = Intent(context, GamepadMonitorService::class.java)
            runCatching {
                androidx.core.content.ContextCompat.startForegroundService(context, intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, GamepadMonitorService::class.java))
        }
    }
}
