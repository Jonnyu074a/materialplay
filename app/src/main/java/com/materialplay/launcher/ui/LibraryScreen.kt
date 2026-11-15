package com.materialplay.launcher.ui

import androidx.compose.ui.res.stringResource
import com.materialplay.launcher.R
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.materialplay.launcher.core.Text
import com.materialplay.launcher.data.db.GameEntity
import com.materialplay.launcher.data.settings.SortMode
import com.materialplay.launcher.ui.components.EmptyState
import com.materialplay.launcher.ui.components.GameArt
import com.materialplay.launcher.ui.components.Tag

@Composable
fun LibraryScreen(
    vm: LauncherViewModel,
    onOpenGame: (GameEntity) -> Unit,
    installedOnly: Boolean = false,
    showCategories: Boolean = true
) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val games by vm.games.collectAsStateWithLifecycle()
    val hiddenGames by vm.hiddenGames.collectAsStateWithLifecycle()
    val totals by vm.totals.collectAsStateWithLifecycle()
    val syncing by vm.syncing.collectAsStateWithLifecycle()

    var query by rememberSaveable { mutableStateOf("") }
    var showAdd by rememberSaveable { mutableStateOf(false) }
    var showHidden by rememberSaveable { mutableStateOf(false) }
    var category by rememberSaveable { mutableStateOf("all") }
    var viewMode by rememberSaveable { mutableStateOf(settings.libraryLayout.name) }

    val totalsMap = remember(totals) { totals.associate { it.packageName to it.total } }
    val baseSource = if (showHidden) hiddenGames else games
    val filteredByInstalled = if (installedOnly) baseSource.filter { it.installed } else baseSource
    val source = filteredByInstalled
    val visible = remember(source, settings.sortMode, settings.sortAscending, query, totalsMap, category) {
        val categoryFiltered = when (category) {
            "installed" -> source.filter { it.installed }
            "recent" -> source.filter { it.lastPlayedAt > 0 }
            "favorites" -> source.filter { it.isFavorite }
            else -> source
        }
        val filtered = categoryFiltered.filter {
            query.isBlank() ||
                it.label.contains(query, ignoreCase = true) ||
                it.packageName.contains(query, ignoreCase = true) ||
                it.developer?.contains(query, ignoreCase = true) == true
        }
        val comparator: Comparator<GameEntity> = when (settings.sortMode) {
            SortMode.TITLE -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.label }
            SortMode.LAST_PLAYED -> compareByDescending { it.lastPlayedAt }
            SortMode.PLAYTIME -> compareByDescending { totalsMap[it.packageName] ?: 0L }
            SortMode.RECENTLY_ADDED -> compareByDescending { it.addedAt }
        }
        val sorted = filtered.sortedWith(comparator)
        if (settings.sortAscending) sorted else sorted.reversed()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 12.dp, top = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "LIBRARY",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (showHidden) "${hiddenGames.size} hidden" else "${games.size} games",
                    style = MaterialTheme.typography.headlineMedium
                )
            }
            if (syncing) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
            }
            IconButton(onClick = { vm.syncLibrary() }, enabled = !syncing) {
                Icon(Icons.Filled.Refresh, contentDescription = "Rescan library")
            }
            IconButton(onClick = { showAdd = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add app manually")
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            singleLine = true,
            placeholder = { Text(stringResource(R.string.search_library)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            shape = MaterialTheme.shapes.large
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(SortMode.entries) { mode ->
                FilterChip(
                    selected = settings.sortMode == mode,
                    onClick = {
                        vm.updateSettings { it.copy(sortMode = mode) }
                    },
                    label = { Text(mode.label) }
                )
            }
            item {
                FilterChip(
                    selected = !settings.sortAscending,
                    onClick = {
                        vm.updateSettings { it.copy(sortAscending = !it.sortAscending) }
                    },
                    label = { Text(if (settings.sortAscending) "A → Z" else "Z → A") }
                )
            }
            item {
                FilterChip(
                    selected = showHidden,
                    enabled = hiddenGames.isNotEmpty() || showHidden,
                    onClick = { showHidden = !showHidden },
                    label = { Text(stringResource(R.string.hidden_count, hiddenGames.size)) }
                )
            }
        }

        if (showCategories && !showHidden) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val cats = listOf("all" to "All", "installed" to "Installed", "recent" to "Recent", "favorites" to "Favorites")
                items(cats) { (id, label) ->
                    FilterChip(
                        selected = category == id,
                        onClick = { category = id },
                        label = { Text(label) }
                    )
                }
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(listOf("GRID", "LIST", "CAROUSEL")) { mode ->
                FilterChip(
                    selected = viewMode == mode,
                    onClick = { viewMode = mode },
                    label = { Text(mode.lowercase().replaceFirstChar { it.uppercase() }) }
                )
            }
        }

        if (visible.isEmpty()) {
            EmptyState(
                title = when {
                    showHidden -> "No hidden games"
                    games.isEmpty() -> "Library is empty"
                    else -> "No matches"
                },
                body = when {
                    showHidden -> "Games you hide from their details appear here. Open one to restore it."
                    games.isEmpty() -> "Scanning finds games Android reports as installed. You can also add any launchable app manually."
                    else -> "Nothing matches \"$query\". Try a shorter search or a different sort."
                },
                action = if (!showHidden && games.isEmpty()) {
                    {
                        androidx.compose.material3.Button(onClick = { vm.syncLibrary() }) {
                            Text(stringResource(R.string.scan_library))
                        }
                    }
                } else {
                    null
                }
            )
        } else {
            when (viewMode) {
                "LIST" -> androidx.compose.foundation.lazy.LazyColumn(
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(visible, key = { it.packageName }) { game ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenGame(game) }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(52.dp)) {
                                com.materialplay.launcher.ui.components.GameArt(
                                    game = game,
                                    modifier = Modifier.fillMaxSize(),
                                    corner = 10.dp
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(game.label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text(
                                    game.displayGenre ?: if (game.lastPlayedAt > 0) com.materialplay.launcher.core.Text.relative(game.lastPlayedAt) else "Installed",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (totalsMap[game.packageName] ?: 0L > 0) {
                                Text(
                                    com.materialplay.launcher.core.Text.playtime(totalsMap[game.packageName] ?: 0L),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
                "CAROUSEL" -> androidx.compose.foundation.lazy.LazyRow(
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(visible, key = { it.packageName }) { game ->
                        Column(
                            modifier = Modifier
                                .width(150.dp)
                                .clickable { onOpenGame(game) }
                        ) {
                            com.materialplay.launcher.ui.components.GameArt(
                                game = game,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(0.72f),
                                corner = 16.dp
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(game.label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = (108 * settings.coverScale).dp),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    items(visible, key = { it.packageName }) { game ->
                        LibraryCard(
                            game = game,
                            total = totalsMap[game.packageName] ?: 0L,
                            onOpen = { onOpenGame(game) },
                            onFavorite = { vm.toggleFavorite(game) }
                        )
                    }
                }
            }
        }
    }

    if (showAdd) {
        AddAppDialog(
            vm = vm,
            existing = games.map { it.packageName }.toSet(),
            onDismiss = { showAdd = false }
        )
    }
}

@Composable
private fun LibraryCard(
    game: GameEntity,
    total: Long,
    onOpen: () -> Unit,
    onFavorite: () -> Unit
) {
    Column(modifier = Modifier.clickable(onClick = onOpen)) {
        Box {
            GameArt(
                game = game,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.72f),
                corner = 18.dp
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(onClick = onFavorite),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Favorite,
                    contentDescription = if (game.isFavorite) "Remove favorite" else "Add favorite",
                    modifier = Modifier.size(16.dp),
                    tint = if (game.isFavorite) MaterialTheme.colorScheme.primary
                    else Color.White.copy(alpha = 0.75f)
                )
            }
            if (total > 0) {
                Tag(
                    text = Text.playtime(total),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp),
                    color = Color.White
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = game.label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = game.displayGenre
                ?: game.developer
                ?: if (game.lastPlayedAt > 0) Text.relative(game.lastPlayedAt) else "Installed",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
