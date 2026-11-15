package com.materialplay.launcher.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.materialplay.launcher.core.Text
import com.materialplay.launcher.data.db.GameEntity
import com.materialplay.launcher.metadata.parseScreenshots
import com.materialplay.launcher.ui.components.DividerLine
import com.materialplay.launcher.ui.components.GameArt
import com.materialplay.launcher.ui.components.GlassCard
import com.materialplay.launcher.ui.components.ScreenshotImage
import com.materialplay.launcher.ui.components.Tag
import kotlinx.coroutines.launch

@Composable
fun DetailsScreen(vm: LauncherViewModel, game: GameEntity, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val games by vm.games.collectAsStateWithLifecycle()
    val hiddenGames by vm.hiddenGames.collectAsStateWithLifecycle()
    val totals by vm.totals.collectAsStateWithLifecycle()
    val refreshing by vm.refreshing.collectAsStateWithLifecycle()
    val live = games.firstOrNull { it.packageName == game.packageName }
        ?: hiddenGames.firstOrNull { it.packageName == game.packageName }
        ?: game
    val total = totals.firstOrNull { it.packageName == live.packageName }?.total ?: 0L
    val screenshots = remember(live.screenshots) { parseScreenshots(live.screenshots) }

    var showMatch by rememberSaveable { mutableStateOf(false) }
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    var confirmHide by rememberSaveable { mutableStateOf(false) }

    val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch { vm.setCustomArt(live, "cover", uri) }
    }
    val heroPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch { vm.setCustomArt(live, "hero", uri) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 40.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.5f)
            ) {
                GameArt(
                    game = live,
                    modifier = Modifier.fillMaxSize(),
                    hero = true,
                    corner = 0.dp
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.25f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.88f)
                                )
                            )
                        )
                )
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                ) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(20.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (live.isFavorite) Tag("FAVORITE", color = MaterialTheme.colorScheme.primary)
                        if (live.isExcluded) Tag("HIDDEN", color = Color.White)
                        if (!live.installed) Tag("NOT INSTALLED", color = MaterialTheme.colorScheme.error)
                        val status = when (live.metadataStatus) {
                            "matched" -> "METADATA MATCHED"
                            "manual" -> "MANUAL MATCH"
                            "notfound" -> "NO MATCH"
                            else -> null
                        }
                        status?.let { Tag(it, color = Color.White) }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = live.label,
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    val subtitle = listOfNotNull(
                        live.developer,
                        Text.extractYear(live.releaseDate),
                        live.displayGenre,
                        if (total > 0) "${Text.playtime(total)} tracked" else null
                    ).joinToString("  •  ")
                    if (subtitle.isNotBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.78f)
                        )
                    }
                }
            }
        }

        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { vm.launch(live) },
                        enabled = live.installed
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Play")
                    }
                    OutlinedButton(onClick = { vm.toggleFavorite(live) }) {
                        Icon(
                            Icons.Filled.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = if (live.isFavorite) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(if (live.isFavorite) "Favorited" else "Favorite")
                    }
                    IconButton(
                        onClick = { vm.refreshMetadata(live) },
                        enabled = refreshing == null
                    ) {
                        if (refreshing == live.packageName) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Filled.Refresh, contentDescription = "Refresh metadata")
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                OutlinedButton(
                    onClick = { showMatch = true },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Search and correct metadata") }
            }
        }

        item {
            if (live.description.isNullOrBlank()) {
                GlassCard(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Column(Modifier.padding(18.dp)) {
                        Text("No description yet", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Add a metadata provider key in Settings, then refresh or match the game manually.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Text("About", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = live.description.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
                Text("Details", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(10.dp))
                DetailRow("Developer", live.developer)
                DetailRow("Released", live.releaseDate)
                DetailRow("Genres", live.genres?.replace(", ", " · "))
                DetailRow("Source", live.source?.uppercase())
                DetailRow("Package", live.packageName)
                DetailRow("Last played", if (live.lastPlayedAt > 0) Text.relative(live.lastPlayedAt) else null)
                DetailRow("Play count", if (live.playCount > 0) live.playCount.toString() else null)
            }
        }

        if (screenshots.isNotEmpty()) {
            item {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Screenshots",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "${screenshots.size} saved",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(screenshots, key = { it }) { path ->
                            ScreenshotImage(
                                path = path,
                                modifier = Modifier
                                    .width(240.dp)
                                    .aspectRatio(16f / 9f)
                            )
                        }
                    }
                }
            }
        }

        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp)) {
                Text("Artwork", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Use your own images when the provider does not have the right art.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { coverPicker.launch("image/*") }, modifier = Modifier.weight(1f)) {
                        Text("Set portrait cover")
                    }
                    OutlinedButton(onClick = { heroPicker.launch("image/*") }, modifier = Modifier.weight(1f)) {
                        Text("Set hero image")
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TextButton(onClick = { confirmClear = true }) { Text("Clear metadata") }
                    if (live.isExcluded) {
                        TextButton(onClick = {
                            vm.setExcluded(live, false)
                            onBack()
                        }) { Text("Restore to library") }
                    } else {
                        TextButton(onClick = { confirmHide = true }) { Text("Hide from library") }
                    }
                    TextButton(onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_DELETE,
                                    Uri.parse("package:${live.packageName}")
                                )
                            )
                        }
                    }) {
                        Text("Uninstall", color = MaterialTheme.colorScheme.error)
                    }
                }
                Spacer(Modifier.height(10.dp))
                DividerLine()
                Spacer(Modifier.height(10.dp))
                Text(
                    "Uninstalling opens the system confirmation. Hidden games keep their play history and can be restored from Library → Hidden.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showMatch) {
        MetadataMatchDialog(vm = vm, game = live, onDismiss = { showMatch = false })
    }
    if (confirmClear) {
        ConfirmDialog(
            title = "Clear metadata?",
            body = "The description, genres and downloaded artwork for ${live.label} will be removed. Play history is kept.",
            confirmLabel = "Clear",
            destructive = true,
            onConfirm = {
                scope.launch {
                    vm.clearMetadata(live)
                }
            },
            onDismiss = { confirmClear = false }
        )
    }
    if (confirmHide) {
        ConfirmDialog(
            title = "Hide ${live.label}?",
            body = "The game disappears from the library and Gaming Mode. Stats and history stay in the database.",
            confirmLabel = "Hide",
            destructive = true,
            onConfirm = {
                vm.setExcluded(live, true)
                onBack()
            },
            onDismiss = { confirmHide = false }
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(104.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Suppress("unused")
private fun rounded(width: Int) = RoundedCornerShape(width.dp)
