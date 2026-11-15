package com.materialplay.launcher.ui

import android.content.pm.ApplicationInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.materialplay.launcher.ui.components.AppIcon
import com.materialplay.launcher.ui.components.GlassCard

@Composable
fun MediaScreen(vm: LauncherViewModel) {
    val context = LocalContext.current
    val apps = remember(context) {
        val pm = context.packageManager
        pm.getInstalledApplications(0)
            .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 || it.category == ApplicationInfo.CATEGORY_VIDEO || it.category == ApplicationInfo.CATEGORY_AUDIO }
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
            .filter { it.category == ApplicationInfo.CATEGORY_VIDEO || it.category == ApplicationInfo.CATEGORY_AUDIO }
            .sortedBy { pm.getApplicationLabel(it).toString() }
    }
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "MEDIA",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 20.dp, top = 14.dp)
        )
        Text(
            "Installed media apps",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(start = 20.dp, bottom = 10.dp)
        )
        if (apps.isEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                Column(Modifier.padding(20.dp)) {
                    Text("No media apps detected", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Apps categorized as video or audio by Android appear here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 100.dp),
                contentPadding = PaddingValues(20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(apps, key = { it.packageName }) { app ->
                    val label = context.packageManager.getApplicationLabel(app).toString()
                    GlassCard(onClick = {
                        val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                        if (intent != null) context.startActivity(intent)
                    }) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(modifier = Modifier.size(56.dp)) {
                                AppIcon(packageName = app.packageName, modifier = Modifier.fillMaxSize())
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                label,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}