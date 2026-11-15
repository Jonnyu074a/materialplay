package com.materialplay.launcher.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.materialplay.launcher.R

/** Localized "today / yesterday / 7 days ago" style label. */
@Composable
fun relativeTimeLabel(epochMillis: Long): String {
    val days = ((System.currentTimeMillis() - epochMillis) / 86_400_000L).toInt().coerceAtLeast(0)
    return when {
        days == 0 -> stringResource(R.string.gm_rel_today)
        days == 1 -> stringResource(R.string.gm_rel_yesterday)
        days < 14 -> stringResource(R.string.gm_rel_days, days)
        days < 60 -> stringResource(R.string.gm_rel_weeks, days / 7)
        else -> stringResource(R.string.gm_rel_months, days / 30)
    }
}
