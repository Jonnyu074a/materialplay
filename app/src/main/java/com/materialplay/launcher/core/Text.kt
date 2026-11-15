package com.materialplay.launcher.core

import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/** Text + number helpers shared by the UI and metadata matcher. */
object Text {

    private val editionSuffixes = listOf(
        "remastered", "remaster", "definitive edition", "deluxe edition", "ultimate edition",
        "complete edition", "game of the year edition", "goty edition", "enhanced edition",
        "mobile", "android", "hd", "lite", "demo", "beta", "early access", "for android"
    )

    private val romanNumerals = mapOf(
        "ii" to "2", "iii" to "3", "iv" to "4", "v" to "5", "vi" to "6",
        "vii" to "7", "viii" to "8", "ix" to "9", "x" to "10"
    )

    fun normalize(value: String): String {
        val base = Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            .lowercase(Locale.ROOT)
            .replace('&', ' ')
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()
        var result = base
        // Strip platform / edition noise, but only at the end of the name.
        for (suffix in editionSuffixes) {
            if (result.endsWith(" $suffix")) {
                result = result.removeSuffix(" $suffix").trim()
            }
        }
        val tokens = result.split(' ').map { token -> romanNumerals[token] ?: token }
        return tokens.joinToString(" ").trim()
    }

    fun initials(value: String): String = value
        .split(' ')
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }

    fun playtime(millis: Long): String {
        if (millis <= 0) return "0m"
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        return when {
            hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
            hours > 0 -> "${hours}h"
            minutes > 0 -> "${minutes}m"
            else -> "<1m"
        }
    }

    fun clock(epochMillis: Long): String =
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(epochMillis))

    fun date(epochMillis: Long): String =
        SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(epochMillis))

    fun dateTime(epochMillis: Long): String =
        SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()).format(Date(epochMillis))

    fun relative(epochMillis: Long): String {
        if (epochMillis <= 0) return "Never"
        val delta = System.currentTimeMillis() - epochMillis
        val minutes = TimeUnit.MILLISECONDS.toMinutes(delta)
        return when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            minutes < 60 * 24 -> "${minutes / 60}h ago"
            minutes < 60 * 24 * 7 -> "${minutes / (60 * 24)}d ago"
            else -> date(epochMillis)
        }
    }

    fun extractYear(value: String?): String? {
        val match = Regex("(19|20)\\d{2}").find(value ?: return null)
        return match?.value
    }
}
