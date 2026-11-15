package com.materialplay.launcher

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.materialplay.launcher.gamepad.ConnectedController
import com.materialplay.launcher.gamepad.GamepadMonitorService
import com.materialplay.launcher.gaming.GamingActivity
import com.materialplay.launcher.ui.LauncherViewModel
import com.materialplay.launcher.ui.MainShell
import android.content.Context
import android.content.res.Configuration
import com.materialplay.launcher.data.settings.AppLanguage
import com.materialplay.launcher.ui.theme.LocalActivity
import com.materialplay.launcher.ui.theme.MaterialPlayTheme
import java.util.Locale
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Normal Mode host. It owns the shared [LauncherViewModel], forwards raw controller
 * input while the user is testing pads, and reacts to controller connections.
 */
class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        val language = runBlocking {
            (newBase.applicationContext as? MaterialPlayApp)?.container?.settings?.settings?.value?.language
                ?: AppLanguage.SYSTEM
        }
        val locale = when (language) {
            AppLanguage.SPANISH -> Locale("es")
            AppLanguage.ENGLISH -> Locale.ENGLISH
            AppLanguage.SYSTEM -> null
        }
        val ctx = if (locale == null) newBase else {
            val config = Configuration(newBase.resources.configuration)
            config.setLocale(locale)
            newBase.createConfigurationContext(config)
        }
        super.attachBaseContext(ctx)
    }

    private val app: MaterialPlayApp get() = application as MaterialPlayApp
    private lateinit var vm: LauncherViewModel

    /** Identities already seen, so only a new connection triggers Gaming Mode. */
    private var knownControllers: Set<String> = emptySet()
    private var gamingOpen = false

    private val gamingLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        gamingOpen = false
        if (::vm.isInitialized) vm.closeOpenSession()
    }

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Draw edge to edge; MainShell applies the system/cutout insets itself.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        vm = ViewModelProvider(this, LauncherViewModel.factory(app))[LauncherViewModel::class.java]

        setContent {
            val settings by vm.settings.collectAsStateWithLifecycle()
            CompositionLocalProvider(LocalActivity provides this@MainActivity) {
                MaterialPlayTheme(settings = settings) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        MainShell(vm = vm, onStartGaming = { openGaming(null) })
                    }
                }
            }
        }

        // Opening/closing the background monitor must also handle its notification
        // permission, otherwise Android silently hides the required notification.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.settings.map { it.monitorInBackground }.distinctUntilChanged()
                    .collect { enabled ->
                        if (enabled) {
                            GamepadMonitorService.start(this@MainActivity)
                            requestNotificationPermissionIfNeeded()
                        }
                    }
            }
        }

        // Auto-transition only when a controller is newly connected while the
        // launcher is in the foreground. The first snapshot is the baseline.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                app.container.gamepadManager.controllers.collect { controllers ->
                    val ids = controllers.map { it.identity }.toSet()
                    val added = ids - knownControllers
                    knownControllers = ids
                    if (added.isEmpty()) return@collect
                    val settings = vm.settings.value
                    val addedController = controllers.firstOrNull { it.identity in added }
                    val allowed = when (addedController?.connection) {
                        com.materialplay.launcher.gamepad.ConnectionKind.USB -> settings.detectUsb
                        com.materialplay.launcher.gamepad.ConnectionKind.BLUETOOTH -> settings.detectBluetooth
                        else -> true
                    }
                    if (settings.autoGamingOnConnect && allowed && !gamingOpen) {
                        openGaming(addedController)
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Returning from a launched game ends the approximate play session.
        vm.closeOpenSession()
    }

    private fun openGaming(controller: ConnectedController?) {
        if (gamingOpen) return
        gamingOpen = true
        val intent = Intent(this, GamingActivity::class.java).apply {
            putExtra(GamingActivity.EXTRA_AUTO_TRANSITION, controller != null)
            putExtra(GamingActivity.EXTRA_CONTROLLER_NAME, controller?.displayName)
        }
        runCatching { gamingLauncher.launch(intent) }
            .onFailure { gamingOpen = false }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            runCatching { notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS) }
        }
    }

    /**
     * Raw hardware input feeds the controller test even in Normal Mode. UI handling
     * wins first so typing in text fields keeps working.
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val handled = super.dispatchKeyEvent(event)
        if (!handled && app.container.gamepadManager.controllers.value.isNotEmpty()) {
            app.container.gamepadRouter.onKeyEvent(event)
        }
        return handled
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        val handled = super.onGenericMotionEvent(event)
        if (!handled) app.container.gamepadRouter.onGenericMotionEvent(event)
        return handled
    }
}
