package com.materialplay.launcher.ui

import android.content.Context
import android.os.Process
import android.app.usage.StorageStatsManager
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.res.stringResource
import com.materialplay.launcher.R
import com.materialplay.launcher.ui.components.relativeTimeLabel
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.materialplay.launcher.core.Text
import com.materialplay.launcher.data.db.GameEntity
import com.materialplay.launcher.ui.components.GameArt
import com.materialplay.launcher.ui.components.GamepadIcon
import com.materialplay.launcher.ui.components.GlassCard
import com.materialplay.launcher.ui.components.Tag
import com.materialplay.launcher.ui.theme.accentFor
import java.io.File

@Composable
fun HomeScreen(
    vm: LauncherViewModel,
    onOpenGame: (GameEntity) -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenControllers: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenSettings: () -> Unit,
    onStartGaming: () -> Unit
) {
    val games by vm.games.collectAsStateWithLifecycle()
    val sessions by vm.sessions.collectAsStateWithLifecycle()
    val totals by vm.totals.collectAsStateWithLifecycle()
    val controllers by vm.controllers.collectAsStateWithLifecycle()
    val syncing by vm.syncing.collectAsStateWithLifecycle()

    val recent = remember(games) {
        games.filter { it.lastPlayedAt > 0 }.sortedByDescending { it.lastPlayedAt }
    }
    val carousel = remember(games, recent) {
        (recent + games.filter { it !in recent }).distinctBy { it.packageName }.take(24)
    }

    var selectedPackage by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = remember(carousel, selectedPackage) {
        carousel.firstOrNull { it.packageName == selectedPackage } ?: carousel.firstOrNull()
    }
    val tracked = totals.firstOrNull { it.packageName == selected?.packageName }?.total ?: 0L
    val lastSession = sessions.firstOrNull { it.packageName == selected?.packageName }
    val recentMinutes = remember(sessions, selected) {
        val cutoff = System.currentTimeMillis() - 14L * 24 * 60 * 60 * 1000
        sessions.filter { it.packageName == selected?.packageName && it.startedAt >= cutoff }
            .sumOf { it.durationMs } / 60_000
    }

    val context = LocalContext.current
    val installedBytes = remember(selected) {
        selected?.let { game ->
            runCatching {
                val sm = context.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager
                sm.queryStatsForPackage(android.os.storage.StorageManager.UUID_DEFAULT, game.packageName, Process.myUserHandle())
                    .appBytes
            }.getOrNull()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Backdrop artwork with crossfade
        Crossfade(targetState = selected?.packageName, animationSpec = androidx.compose.animation.core.tween(300)) { _ ->
            val path = selected?.heroPath ?: selected?.coverPath
            if (path != null && File(path).exists()) {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(File(path)).crossfade(true).build(),
                    contentDescription = selected?.label,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.CenterEnd
                )
            } else {
                Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF1A2236), Color(0xFF05070C)))))
            }
        }
        // Vertical gradient: transparent top -> 85% black bottom
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.25f),
                            Color.Black.copy(alpha = 0.85f)
                        )
                    )
                )
        )
        // Horizontal left scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.6f),
                            Color.Transparent
                        )
                    )
                )
        )

        if (games.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                verticalArrangement = Arrangement.Center
            ) {
                GlassCard {
                    Column(Modifier.padding(24.dp)) {
                        Text(stringResource(R.string.no_games_yet), style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.library_empty_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { vm.syncLibrary() }, enabled = !syncing) {
                            if (syncing) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            else Text(stringResource(R.string.scan_library))
                        }
                    }
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top bar placeholder space (MainShell already draws top bar; this spacer keeps content below)
                Spacer(Modifier.height(40.dp))

                // Carousel
                LazyRow(
                    contentPadding = PaddingValues(start = 24.dp, end = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(108.dp)
                ) {
                    itemsIndexed(carousel, key = { _, it -> it.packageName }) { index, game ->
                        val isFocused = selected?.packageName == game.packageName
                        val size by animateDpAsState(if (isFocused) 92.dp else 60.dp, tween(150), label = "cover")
                        val requester = remember { FocusRequester() }
                        if (index == 0) {
                            LaunchedEffect(Unit) { runCatching { requester.requestFocus() } }
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .focusRequester(requester)
                                .onFocusChanged { if (it.isFocused) selectedPackage = game.packageName }
                                .clickable { if (isFocused) { vm.launch(game) } else { selectedPackage = game.packageName } }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(size)
                                    .clip(RoundedCornerShape(10.dp))
                                    .then(
                                        if (isFocused) Modifier.border(2.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(10.dp))
                                        else Modifier
                                    )
                            ) {
                                GameArt(
                                    game = game,
                                    modifier = Modifier.fillMaxSize(),
                                    corner = 10.dp
                                )
                                if (isFocused) {
                                    Box(
                                        Modifier
                                            .matchParentSize()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color.Transparent)
                                            .then(Modifier)
                                    )
                                }
                            }
                            if (isFocused) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    game.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.width(92.dp)
                                )
                            }
                        }
                    }
                }

                // Free zone: artwork visible
                Spacer(Modifier.weight(1f))

                // Game logo
                if (selected != null) {
                    val logoPath = selected.logoPath
                    if (logoPath != null && File(logoPath).exists()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(File(logoPath)).build(),
                            contentDescription = selected.label,
                            modifier = Modifier
                                .fillMaxWidth(0.35f)
                                .height(44.dp)
                                .padding(start = 24.dp),
                            contentScale = ContentScale.Fit,
                            alignment = Alignment.CenterStart
                        )
                    } else {
                        Text(
                            selected.label,
                            style = MaterialTheme.typography.headlineLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 24.dp)
                        )
                    }

                    // Played line
                    val playedLine = buildString {
                        if (selected.lastPlayedAt > 0) append(stringResource(R.string.gm_played, relativeTimeLabel(selected.lastPlayedAt)))
                        if (tracked > 0) {
                            if (selected.lastPlayedAt > 0) append("  ·  ")
                            append(Text.playtime(tracked))
                        }
                    }
                    if (playedLine.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            playedLine,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.padding(start = 24.dp)
                        )
                    }

                    // Play + more buttons
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.padding(start = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { vm.launch(selected) },
                            enabled = selected.installed,
                            modifier = Modifier.width(140.dp).height(32.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.gm_play), style = MaterialTheme.typography.labelLarge)
                        }
                        IconButton(
                            onClick = { onOpenGame(selected) },
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f))
                        ) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                        if (selected.isFavorite) {
                            Icon(Icons.Filled.Favorite, contentDescription = "Favorite", tint = accentFor(vm.settings.value.accentId), modifier = Modifier.size(18.dp))
                        }
                    }

                    // Data cards row
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Card: Your time
                        DataCard(
                            title = stringResource(R.string.gm_card_time).uppercase(),
                            value = if (tracked > 0) Text.playtime(tracked) else "—",
                            subtitle = if (selected.lastPlayedAt > 0) relativeTimeLabel(selected.lastPlayedAt) else stringResource(R.string.gm_never_played),
                            extra = if (recentMinutes > 0) stringResource(R.string.gm_two_weeks, recentMinutes) else null,
                            progress = null
                        )
                        // Card: Storage (if available)
                        if (installedBytes != null) {
                            DataCard(
                                title = stringResource(R.string.gm_card_storage).uppercase(),
                                value = formatBytes(installedBytes),
                                subtitle = stringResource(R.string.gm_card_storage_sub),
                                extra = null,
                                progress = null
                            )
                        }
                        // Card: sessions count
                        val sessionCount = sessions.count { it.packageName == selected.packageName }
                        if (sessionCount > 0) {
                            DataCard(
                                title = stringResource(R.string.gm_card_sessions).uppercase(),
                                value = sessionCount.toString(),
                                subtitle = stringResource(R.string.gm_card_sessions_sub),
                                extra = null,
                                progress = null
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
            }
        }

    }
}

@Composable
private fun DataCard(
    title: String,
    value: String,
    subtitle: String,
    extra: String?,
    progress: Float?
) {
    GlassCard(
        containerColor = Color.Black.copy(alpha = 0.55f),
        modifier = Modifier
            .width(150.dp)
            .heightIn(min = 56.dp)
    ) {
        Column(Modifier.padding(8.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (extra != null) {
                Text(extra, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    val mb = bytes / (1024f * 1024f)
    return if (mb >= 1024) "%.1f GB".format(mb / 1024f) else "%.0f MB".format(mb)
}