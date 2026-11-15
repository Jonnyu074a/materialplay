package com.materialplay.launcher.metadata

import com.materialplay.launcher.data.settings.AppSettings
import java.net.URLEncoder
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/**
 * IGDB provider. Authentication uses the Twitch client-credentials flow and the
 * resulting token is cached in memory until shortly before it expires.
 */
class IgdbProvider(private val client: OkHttpClient) : MetadataProvider {
    override val id = "igdb"
    override val displayName = "IGDB"

    private data class Token(val value: String, val expiresAt: Long, val clientId: String)
    private val cachedToken = AtomicReference<Token?>(null)

    override fun isConfigured(settings: AppSettings) =
        settings.igdbClientId.isNotBlank() && settings.igdbSecret.isNotBlank()

    override suspend fun search(title: String, settings: AppSettings): List<MetadataCandidate> =
        withContext(Dispatchers.IO) {
            if (!isConfigured(settings)) return@withContext emptyList()
            val token = accessToken(settings)
            val body = """
                search "${escape(title)}";
                fields name,summary,first_release_date,genres.name,developers.name,cover.url,artworks.url,screenshots.url;
                limit 10;
            """.trimIndent()

            val request = Request.Builder()
                .url("https://api.igdb.com/v4/games")
                .header("Client-ID", settings.igdbClientId.trim())
                .header("Authorization", "Bearer ${token.value}")
                .post(body.toRequestBody("text/plain".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw ProviderException("IGDB HTTP ${response.code}")
                }
                val array = runCatching { JSONArray(raw) }.getOrElse {
                    throw ProviderException("IGDB returned an unexpected payload", it)
                }
                val candidates = (0 until array.length()).mapNotNull { index ->
                    val item = array.optJSONObject(index) ?: return@mapNotNull null
                    val name = item.optString("name")
                    val score = TitleMatcher.score(title, name)
                    if (score < 0.5) return@mapNotNull null

                    val cover = image(item.optJSONObject("cover")?.optString("url"), "t_cover_big")
                    val hero = image(firstImage(item.optJSONArray("artworks")), "t_1080p")
                    val screenshots = images(item.optJSONArray("screenshots"), "t_screenshot_big").take(8)
                    val release = item.optLong("first_release_date", 0L).takeIf { it > 0 }
                        ?.let { com.materialplay.launcher.core.ReleaseDates.igdbTimestamp(it) }

                    MetadataCandidate(
                        provider = id,
                        sourceId = item.optInt("id").toString(),
                        title = name,
                        description = item.optString("summary").takeIf { it.isNotBlank() },
                        genres = names(item.optJSONArray("genres")),
                        developers = names(item.optJSONArray("developers")),
                        releaseDate = release,
                        coverUrl = cover,
                        heroUrl = hero,
                        screenshots = screenshots,
                        score = score
                    )
                }
                TitleMatcher.rank(title, candidates)
            }
        }

    private fun accessToken(settings: AppSettings): Token {
        val clientId = settings.igdbClientId.trim()
        val current = cachedToken.get()
        if (current != null && current.clientId == clientId && current.expiresAt > System.currentTimeMillis()) {
            return current
        }
        val url = "https://id.twitch.tv/oauth2/token?client_id=${URLEncoder.encode(clientId, "UTF-8")}" +
            "&client_secret=${URLEncoder.encode(settings.igdbSecret.trim(), "UTF-8")}&grant_type=client_credentials"
        val request = Request.Builder().url(url).post(FormBody.Builder().build()).build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw ProviderException("Twitch auth HTTP ${response.code}")
            val json = JSONObject(body)
            val token = json.optString("access_token")
            if (token.isBlank()) throw ProviderException("Twitch auth failed")
            val ttl = json.optLong("expires_in", 3600L)
            val value = Token(token, System.currentTimeMillis() + (ttl - 120) * 1000, clientId)
            cachedToken.set(value)
            return value
        }
    }

    private fun names(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        return (0 until array.length()).mapNotNull { index ->
            array.optJSONObject(index)?.optString("name")?.takeIf { it.isNotBlank() }
        }
    }

    private fun firstImage(array: JSONArray?): String? =
        if (array == null || array.length() == 0) null else array.optJSONObject(0)?.optString("url")

    private fun images(array: JSONArray?, size: String): List<String> {
        if (array == null) return emptyList()
        return (0 until array.length()).mapNotNull { index ->
            image(array.optJSONObject(index)?.optString("url"), size)
        }
    }

    private fun image(url: String?, size: String): String? {
        val value = url?.takeIf { it.isNotBlank() } ?: return null
        val normalized = if (value.startsWith("//")) "https:$value" else value
        return normalized.replace(Regex("/t_[a-z0-9_]+/"), "/$size/")
    }

    private fun escape(value: String): String =
        value.replace("\\", "\\\\").replace("\"", "\\\"")
}
