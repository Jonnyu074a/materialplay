package com.materialplay.launcher.games

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.materialplay.launcher.MaterialPlayApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
/**
 * Keeps the library in sync when packages are installed, removed or updated.
 * Registered in the manifest so it also runs when the launcher process is not open.
 */
class PackageChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as? MaterialPlayApp ?: return
        val pending = goAsync()
        app.container.appScope.launch(Dispatchers.IO) {
            try {
                app.container.gameRepository.syncLibrary()
            } catch (_: Throwable) {
                // A later manual sync will reconcile the library.
            } finally {
                pending.finish()
            }
        }
    }
}
