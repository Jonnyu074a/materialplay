package com.materialplay.launcher.gaming

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import android.content.Context
import android.content.res.Configuration
import com.materialplay.launcher.MaterialPlayApp
import com.materialplay.launcher.data.settings.AppLanguage
import com.materialplay.launcher.ui.theme.LocalActivity
import com.materialplay.launcher.ui.theme.MaterialPlayTheme
import java.util.Locale
import com.materialplay.launcher.gamepad.PadAction
import com.materialplay.launcher.gamepad.PadLayout
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Console-first host. The activity owns the immersive window and hardware input;
 * [GamingScreen] owns the focus model and every visible action.
 */
class GamingActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        val language = (newBase.applicationContext as? MaterialPlayApp)?.container?.settings?.settings?.value?.language
            ?: AppLanguage.SYSTEM
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
    private lateinit var vm: GamingViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        hideSystemBars()

        vm = ViewModelProvider(this, GamingViewModel.factory(app))[GamingViewModel::class.java]

        val autoTransition = intent.getBooleanExtra(EXTRA_AUTO_TRANSITION, false)
        val controllerName = intent.getStringExtra(EXTRA_CONTROLLER_NAME)

        setContent {
            val settings by vm.settings.collectAsStateWithLifecycle()
            androidx.compose.runtime.CompositionLocalProvider(com.materialplay.launcher.ui.theme.LocalActivity provides this@GamingActivity) {
                MaterialPlayTheme(settings = settings, gaming = true) {
                    val baseDensity = androidx.compose.ui.platform.LocalDensity.current
                    val gamingScale = if (settings.gamingDensity == com.materialplay.launcher.data.settings.GamingDensity.CUSTOM)
                        settings.gamingDensityCustom else settings.gamingDensity.factor
                    androidx.compose.runtime.CompositionLocalProvider(
                        androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(
                            baseDensity.density * gamingScale,
                            baseDensity.fontScale
                        )
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            GamingScreen(
                                vm = vm,
                                autoTransition = autoTransition,
                                introControllerName = controllerName,
                                onExit = { finish() }
                            )
                        }
                    }
                }
            }
        }

        // Keep the shared router aligned with the selected combo and the physical
        // face-button layout of the active pad.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    vm.settings,
                    vm.controllers,
                    vm.savedControllers
                ) { settings, controllers, saved -> Triple(settings, controllers, saved) }
                    .collect { (settings, controllers, saved) ->
                        val combo = settings.overlayCombo.actions.mapNotNull { name ->
                            PadAction.entries.firstOrNull { it.name == name }
                        }.toSet()
                        app.container.gamepadRouter.overlayCombo = combo

                        val active = controllers.firstOrNull()
                        val stored = active?.let { controller ->
                            saved.firstOrNull { it.descriptor == controller.identity }
                        }
                        val swap = stored?.faceSwap ?: (active?.layout == PadLayout.NINTENDO)
                        app.container.gamepadRouter.swapFaceButtons = swap
                    }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // A launch started a session; when Gaming Mode is visible again the game
        // is no longer in front, so the measured session can close.
        vm.closeOpenSession()
        // Do not inherit keys held while another activity was focused.
        app.container.gamepadRouter.resetInputState()
        hideSystemBars()
    }

    override fun onStop() {
        super.onStop()
        app.container.gamepadRouter.resetInputState()
    }

    override fun onDestroy() {
        app.container.gamepadRouter.stop()
        super.onDestroy()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    /**
     * Gaming Mode consumes mapped keys before the Compose tree, so a focused
     * component never steals D-pad or face-button input. Unmapped keys (volume,
     * keyboard) continue to Android.
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (app.container.gamepadRouter.onKeyEvent(event)) return true
        return super.dispatchKeyEvent(event)
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (app.container.gamepadRouter.onGenericMotionEvent(event)) return true
        return super.onGenericMotionEvent(event)
    }

    private fun hideSystemBars() {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    companion object {
        const val EXTRA_AUTO_TRANSITION = "extra_auto_transition"
        const val EXTRA_CONTROLLER_NAME = "extra_controller_name"
    }
}
