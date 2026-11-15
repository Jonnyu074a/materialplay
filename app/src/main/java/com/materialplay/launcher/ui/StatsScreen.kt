package com.materialplay.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.materialplay.launcher.core.Text
import com.materialplay.launcher.data.db.GameEntity
import com.materialplay.launcher.data.db.PlayTotals
import com.materialplay.launcher.data.db.SessionEntity
import com.materialplay.launcher.ui.components.GlassCard
import com.materialplay.launcher.ui.components.SectionHeader
import com.materialplay.launcher.ui.components.StatBlock

@Composable
fun StatsScreen(vm: LauncherViewModel) {
    val totals by vm.totals.collectAsStateWithLifecycle()
    val allSessions by vm.allSessions.collectAsStateWithLifecycle()
    val recent by vm.sessions.collectAsStateWithLifecycle()
    val allGames by vm.allGames.collectAsStateWithLifecycle()
    var confirmClear by rememberSaveable { mutableStateOf(false) }

    val labels = remember(allGames) {
        allGames.associate { it.packageName to it.label }
    }
    val totalTime = totals.sumOf { it.total }
    val maxTotal = totals.maxOfOrNull { it.total } ?: 1L
    val running = recent.firstOrNull { it.endedAt == null }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Column {
                Text(
                    "PLAY ACTIVITY",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text("Session stats", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Material Play measures from launch until the launcher is back in front. Treat the numbers as approximate play time.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            GlassCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatBlock(Text.playtime(totalTime), "TOTAL")
                    StatBlock(allSessions.size.toString(), "SESSIONS")
                    StatBlock(totals.size.toString(), "GAMES")
                    StatBlock(recent.size.toString(), "RECENT")
                }
            }
        }

        if (running != null) {
            item {
                GlassCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Session in progress",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                labels[running.packageName] ?: running.packageName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            "since ${Text.clock(running.startedAt)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (totals.isNotEmpty()) {
            item {
                SectionHeader(title = "By game", subtitle = "Approximate tracked time")
            }
            items(totals.size, key = { totals[it].packageName }) { index ->
                TotalRow(
                    total = totals[index],
                    label = labels[totals[index].packageName] ?: totals[index].packageName,
                    max = maxTotal
                )
            }
        }

        item {
            SectionHeader(
                title = "Recent sessions",
                subtitle = "Newest first",
                actionLabel = if (allSessions.isEmpty()) null else "Clear",
                onAction = if (allSessions.isEmpty()) null else ({ confirmClear = true })
            )
        }

        if (recent.isEmpty()) {
            item {
                Text(
                    "No sessions yet. Launch a game from Material Play and its session will appear here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(recent.size, key = { recent[it].id }) { index ->
                SessionRow(
                    session = recent[index],
                    label = labels[recent[index].packageName] ?: recent[index].packageName
                )
            }
        }

        item {
            GlassCard {
                Column(Modifier.padding(18.dp)) {
                    Text("Why are times approximate?", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Android does not let one app read another app's usage without privileged access. The session starts at launch and ends when this launcher returns to the foreground, so time spent in the game's own menus before it becomes visible may be slightly off.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { confirmClear = true }) {
                        Text("Clear play history", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    if (confirmClear) {
        ConfirmDialog(
            title = "Clear play history?",
            body = "Every recorded session and total will be deleted. Games, favorites and controller profiles are not affected.",
            confirmLabel = "Clear history",
            destructive = true,
            onConfirm = { vm.clearHistory() },
            onDismiss = { confirmClear = false }
        )
    }
}

@Composable
private fun TotalRow(total: PlayTotals, label: String, max: Long) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = Text.playtime(total.total),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(5.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
        ) {
            val fraction = (total.total.toFloat() / max.toFloat()).coerceIn(0.02f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(6.dp)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
        Text(
            "${total.count} session${if (total.count == 1) "" else "s"}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SessionRow(session: SessionEntity, label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(
                Text.dateTime(session.startedAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (session.endedAt == null) {
            Text(
                "in progress",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    Text.playtime(session.durationMs),
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    "approx.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
