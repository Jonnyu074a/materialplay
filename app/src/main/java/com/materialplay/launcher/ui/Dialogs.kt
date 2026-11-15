package com.materialplay.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.materialplay.launcher.data.db.GameEntity
import com.materialplay.launcher.games.ScannedApp
import com.materialplay.launcher.metadata.MetadataCandidate
import com.materialplay.launcher.ui.components.AppIcon
import com.materialplay.launcher.ui.components.Tag
import kotlinx.coroutines.launch

@Composable
fun AddAppDialog(
    vm: LauncherViewModel,
    existing: Set<String>,
    onDismiss: () -> Unit
) {
    var apps by remember { mutableStateOf<List<ScannedApp>?>(null) }
    var query by remember { mutableStateOf("") }
    var loadError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val result = runCatching { vm.launchableApps() }
        apps = result.getOrElse {
            loadError = it.message ?: "Could not list installed apps"
            emptyList()
        }.filter { it.packageName !in existing }
    }

    val filtered = remember(apps, query) {
        apps.orEmpty().filter {
            query.isBlank() ||
                it.label.contains(query, ignoreCase = true) ||
                it.packageName.contains(query, ignoreCase = true)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Add an app", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Emulators, ports or anything launchable that is not tagged as a game.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }

                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Filter installed apps") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    shape = MaterialTheme.shapes.large
                )
                Spacer(Modifier.height(12.dp))

                when {
                    apps == null -> Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }

                    loadError != null -> Text(
                        text = loadError.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    filtered.isEmpty() -> Text(
                        text = if (apps.orEmpty().isEmpty()) "Every launchable app is already in the library."
                        else "No installed app matches \"$query\".",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    else -> LazyColumn(
                        modifier = Modifier.heightIn(max = 400.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(filtered, key = { it.packageName }) { app ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        vm.addManually(app.packageName)
                                        apps = apps.orEmpty().filterNot { it.packageName == app.packageName }
                                    }
                                    .padding(vertical = 8.dp, horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AppIcon(packageName = app.packageName, modifier = Modifier.size(40.dp))
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = app.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = app.packageName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (app.isGame) Tag("GAME")
                                Spacer(Modifier.width(8.dp))
                                TextButton(onClick = {
                                    vm.addManually(app.packageName)
                                    apps = apps.orEmpty().filterNot { it.packageName == app.packageName }
                                }) { Text("Add") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetadataMatchDialog(
    vm: LauncherViewModel,
    game: GameEntity,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf(game.matchQuery ?: game.label) }
    var candidates by remember { mutableStateOf<List<MetadataCandidate>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var searched by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun search() {
        if (loading) return
        scope.launch {
            loading = true
            error = null
            searched = false
            try {
                candidates = vm.findCandidates(game, query.trim().ifBlank { game.label })
                searched = true
            } catch (failure: Throwable) {
                error = failure.message ?: "Metadata search failed"
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(game.packageName) { search() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 620.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Match metadata", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Pick the correct game. Covers and details download locally.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }

                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text("Search title") },
                        shape = MaterialTheme.shapes.large
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { search() }, enabled = !loading) { Text("Search") }
                }

                Spacer(Modifier.height(12.dp))

                when {
                    loading -> Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(10.dp))
                            Text("Searching providers…", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    error != null -> Text(
                        text = error.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    searched && candidates.isEmpty() -> Text(
                        text = "No candidates. Add a provider key in Settings, or try the game's English title.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    else -> LazyColumn(
                        modifier = Modifier.heightIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(candidates, key = { "${it.provider}:${it.sourceId}" }) { candidate ->
                            CandidateRow(
                                candidate = candidate,
                                onApply = {
                                    scope.launch {
                                        vm.applyCandidate(game, candidate)
                                        onDismiss()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CandidateRow(candidate: MetadataCandidate, onApply: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onApply)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = 52.dp, height = 70.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
        ) {
            AsyncImage(
                model = candidate.coverUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = candidate.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            val meta = listOfNotNull(
                candidate.sourceLabel,
                candidate.releaseDate?.take(4),
                candidate.developers.firstOrNull(),
                "${(candidate.score * 100).toInt()}% match"
            ).joinToString(" • ")
            Text(
                text = meta,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            candidate.description?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Text("Use", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    body: String,
    confirmLabel: String = "Confirm",
    destructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            TextButton(onClick = {
                onConfirm()
                onDismiss()
            }) {
                Text(
                    confirmLabel,
                    color = if (destructive) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
