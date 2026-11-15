package com.materialplay.launcher.gaming

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.materialplay.launcher.MaterialPlayApp
import com.materialplay.launcher.ui.LauncherViewModel

/**
 * Gaming Mode reuses the Normal Mode state and actions; the console surface only
 * changes presentation and input handling.
 */
class GamingViewModel(application: Application) : LauncherViewModel(application) {
    companion object {
        fun factory(app: MaterialPlayApp): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return GamingViewModel(app) as T
                }
            }
    }
}
