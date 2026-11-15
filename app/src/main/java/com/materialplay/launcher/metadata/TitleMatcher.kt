package com.materialplay.launcher.metadata

import com.materialplay.launcher.core.Text

/** Fuzzy title matcher used to turn a package label into a metadata candidate. */
object TitleMatcher {

    fun score(query: String, candidate: String): Double {
        val q = Text.normalize(query)
        val c = Text.normalize(candidate)
        if (q.isBlank() || c.isBlank()) return 0.0
        if (q == c) return 1.0

        var base = when {
            c.startsWith("$q ") || q.startsWith("$c ") -> 0.93
            c.contains(q) || q.contains(c) -> 0.84
            else -> tokenScore(q, c)
        }
        // Penalise sequels when the query has no number, e.g. "Doom" vs "Doom 3".
        if (base > 0.3) {
            val qTokens = q.split(' ')
            val cTokens = c.split(' ')
            val extraNumber = cTokens.any { it.all(Char::isDigit) } &&
                qTokens.none { it.all(Char::isDigit) }
            if (extraNumber) base -= 0.12
        }
        return base.coerceIn(0.0, 1.0)
    }

    private fun tokenScore(q: String, c: String): Double {
        val qTokens = q.split(' ').filter { it.isNotBlank() }.toSet()
        val cTokens = c.split(' ').filter { it.isNotBlank() }.toSet()
        if (qTokens.isEmpty() || cTokens.isEmpty()) return 0.0
        val intersection = qTokens.intersect(cTokens).size.toDouble()
        val union = qTokens.union(cTokens).size.toDouble()
        val jaccard = intersection / union
        val coverage = intersection / qTokens.size
        return (jaccard * 0.45 + coverage * 0.55).coerceAtMost(0.8)
    }

    fun best(query: String, candidates: List<MetadataCandidate>): MetadataCandidate? =
        candidates
            .maxByOrNull { score(query, it.title) }
            ?.takeIf { score(query, it.title) >= 0.55 }

    fun rank(query: String, candidates: List<MetadataCandidate>): List<MetadataCandidate> =
        candidates
            .map { it.copy(score = score(query, it.title)) }
            .sortedByDescending { it.score }
}
