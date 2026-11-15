package com.materialplay.launcher.metadata

import android.net.Uri
import com.materialplay.launcher.data.settings.AppSettings
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

/** RAWG.io provider: metadata, screenshots and landscape artwork. */
class RawgProvider(private val client: OkHttpClient) : MetadataProvider {
    override val id = "rawg"
    override val displayName = "RAWG"

    override fun isConfigured(settings: AppSettings) = settings.rawgKey.isNotBlank()

    override suspend fun search(title: String, settings: AppSettings): List<MetadataCandidate> =
        withContext(Dispatchers.IO) {
            val key = settings.rawgKey.trim()
            if (key.isBlank()) return@withContext emptyList()
            val encoded = URLEncoder.encode(title, "UTF-8")
            val listUrl = "https://api.rawg.io/api/games?key=$key&search=$encoded&page_size=10"
            val results = runCatching { getJson(listUrl).optJSONArray("results") ?: JSONArray() }
                .getOrElse { throw ProviderException("RAWG: ${it.message ?: "network error"}", it) }

            val candidates = mutableListOf<MetadataCandidate>()
            for (i in 0 until results.length()) {
                val item = results.optJSONObject(i) ?: continue
                val id = item.optInt("id")
                if (id == 0) continue
                val name = item.optString("name")
                val score = TitleMatcher.score(title, name)
                if (score < 0.4) continue

                // Enrich only plausible matches to conserve API quota.
                var detail: JSONObject? = null
                if (score >= 0.6 && candidates.size < 2) {
                    detail = runCatching {
                        getJson("https://api.rawg.io/api/games/$id?key=$key")
                    }.getOrNull()
                }
                val screenshots = if (detail != null) {
                    runCatching {
                        val arr = getJson("https://api.rawg.io/api/games/$id/screenshots?key=$key&page_size=8")
                            .optJSONArray("results") ?: JSONArray()
                        (0 until arr.length()).mapNotNull { s ->
                            arr.optJSONObject(s)?.optString("image")?.takeIf { it.isNotBlank() }
                        }
                    }.getOrDefault(emptyList())
                } else emptyList()

                val hero = detail?.optString("background_image_additional")?.takeIf { it.isNotBlank() }
                    ?: item.optString("background_image").takeIf { it.isNotBlank() }
                val cover = item.optString("background_image").takeIf { it.isNotBlank() }
                val genres = parseNames(detail?.optJSONArray("genres") ?: item.optJSONArray("genres"))
                val developers = parseNames(detail?.optJSONArray("developers"))
                val description = detail?.optString("description_raw")?.takeIf { it.isNotBlank() }

                candidates += MetadataCandidate(
                    provider = "rawg",
                    sourceId = id.toString(),
                    title = name,
                    description = description,
                    genres = genres,
                    developers = developers,
                    releaseDate = (detail?.optString("released") ?: item.optString("released"))
                        .takeIf { it.isNotBlank() && it != "null" },
                    coverUrl = cover,
                    heroUrl = hero,
                    screenshots = screenshots,
                    score = score
                )
            }
            TitleMatcher.rank(title, candidates)
        }

    private fun parseNames(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        return (0 until array.length()).mapNotNull { index ->
            array.optJSONObject(index)?.optString("name")?.takeIf { it.isNotBlank() }
        }
    }

    private fun getJson(url: String): JSONObject {
        val request = Request.Builder().url(url).header("User-Agent", "MaterialPlay/1.0").build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw ProviderException("RAWG HTTP ${response.code}")
            }
            return JSONObject(body)
        }
    }
}
