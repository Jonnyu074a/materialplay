package com.materialplay.launcher.metadata

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Downloads artwork into app-private storage so the library keeps working offline
 * and without re-fetching remote images.
 */
class ImageStore(
    private val context: Context,
    private val client: OkHttpClient
) {
    private val root: File get() = File(context.filesDir, "art").apply { mkdirs() }

    private fun dirFor(packageName: String): File =
        File(root, packageName.replace(Regex("[^A-Za-z0-9._-]"), "_")).apply { mkdirs() }

    suspend fun download(url: String, packageName: String, kind: String): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "MaterialPlay/1.0 (Android)")
                    .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
                    val body = response.body ?: throw IOException("Empty image body")
                    if (body.contentLength() > MAX_IMAGE_BYTES) throw IOException("Image too large")
                    val target = File(dirFor(packageName), "$kind.jpg")
                    val temp = File(dirFor(packageName), "$kind.part")
                    body.byteStream().use { input ->
                        temp.outputStream().use { output ->
                            input.copyTo(output, bufferSize = 64 * 1024)
                        }
                    }
                    if (temp.length() == 0L) {
                        temp.delete()
                        throw IOException("Empty image")
                    }
                    if (target.exists()) target.delete()
                    if (!temp.renameTo(target)) {
                        temp.copyTo(target, overwrite = true)
                        temp.delete()
                    }
                    target.absolutePath
                }
            }.getOrNull()
        }

    suspend fun import(packageName: String, kind: String, uri: Uri): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                val target = File(dirFor(packageName), "$kind.jpg")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    target.outputStream().use { output -> input.copyTo(output, 64 * 1024) }
                } ?: return@runCatching null
                target.absolutePath
            }.getOrNull()
        }

    fun deleteKinds(packageName: String, kinds: Set<String>) {
        val dir = dirFor(packageName)
        kinds.forEach { kind ->
            File(dir, "$kind.jpg").delete()
            File(dir, "$kind.part").delete()
        }
    }

    fun deleteByPrefix(packageName: String, prefix: String) {
        dirFor(packageName).listFiles()?.forEach { file ->
            if (file.name.startsWith(prefix)) file.delete()
        }
    }

    fun deleteAll(packageName: String) {
        dirFor(packageName).deleteRecursively()
    }

    companion object {
        private const val MAX_IMAGE_BYTES = 15L * 1024 * 1024
    }
}
