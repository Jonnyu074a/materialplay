package com.materialplay.launcher.metadata

import com.materialplay.launcher.data.settings.AppSettings
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

/**
 * SteamGridDB is used for presentation art (portrait grid, hero, transparent logo).
 * It never overrides metadata text from RAWG/IGDB, only artwork.
 */
class SteamGridDbProvider(private val client: OkHttpClient) : ArtProvider {
    override val id = "steamgriddb"
    override val displayName = "SteamGridDB"

    override fun isConfigured(settings: AppSettings) = settings.sgdbKey.isNotBlank()

    override suspend fun findArt(title: String, settings: AppSettings): Artwork? =
        withContext(Dispatchers.IO) {
            val key = settings.sgdbKey.trim()
            if (key.isBlank()) return@withContext null

            val searchTerm = URLEncoder.encode(title, "UTF-8")
            val search = runCatching {
                getJson("https://www.steamgriddb.com/api/v2/search/autocomplete/$searchTerm", key)
            }.getOrNull() ?: return@withContext null

            val data = search.optJSONArray("data") ?: return@withContext null
            val match = (0 until data.length())
                .mapNotNull { data.optJSONObject(it) }
                .map { it.optInt("id") to it.optString("name") }
                .filter { it.first != 0 }
                .maxByOrNull { TitleMatcher.score(title, it.second) }
                ?: return@withContext null
            if (TitleMatcher.score(title, match.second) < 0.6) return@withContext null

            val gameId = match.first
            val cover = pickUrl(getJson("https://www.steamgriddb.com/api/v2/grids/game/$gameId?dimensions=600x900", key))
            val hero = pickUrl(getJson("https://www.steamgriddb.com/api/v2/heroes/game/$gameId?dimensions=1920x620", key))
                ?: pickUrl(getJson("https://www.steamgriddb.com/api/v2/heroes/game/$gameId", key))
            val logo = pickUrl(getJson("https://www.steamgriddb.com/api/v2/logos/game/$gameId", key))

            if (cover == null && hero == null && logo == null) return@withContext null
            Artwork(coverUrl = cover, heroUrl = hero, logoUrl = logo)
        }

    private fun pickUrl(json: JSONObject): String? {
        val data = json.optJSONArray("data") ?: return null
        for (index in 0 until data.length()) {
            val url = data.optJSONObject(index)?.optString("url")
            if (!url.isNullOrBlank()) return url
        }
        return null
    }

    private fun getJson(url: String, key: String): JSONObject {
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $key")
            .header("User-Agent", "MaterialPlay/1.0")
            .build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw ProviderException("SteamGridDB HTTP ${response.code}")
            return JSONObject(body)
        }
    }
}
