package com.iptvhero.pro.data.download

import android.content.Context
import android.net.Uri
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

data class OfflineDownloadState(
    val isDownloading: Boolean = false,
    val progress: Float = 0f,
    val isCompleted: Boolean = false,
    val localUri: String? = null,
    val error: String? = null
)

class OfflineDownloadManager(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val _states = MutableStateFlow<Map<String, OfflineDownloadState>>(emptyMap())
    val states: StateFlow<Map<String, OfflineDownloadState>> = _states.asStateFlow()

    private fun getOfflineDir(): File {
        val base = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir
        return File(base, "IPTVHeroOffline").apply { if (!exists()) mkdirs() }
    }

    fun getTargetFile(key: String, title: String, extension: String = "mp4"): File {
        val safeTitle = title.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(50)
        val safeKey = key.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(24)
        val cleanExt = extension.substringBefore('?').ifBlank { "mp4" }
        return File(getOfflineDir(), "${safeKey}_${safeTitle}.$cleanExt")
    }

    fun checkExistingFile(key: String, title: String, extension: String = "mp4"): OfflineDownloadState {
        val file = getTargetFile(key, title, extension)
        return if (file.exists() && file.length() > 1024L) {
            OfflineDownloadState(
                isDownloading = false,
                progress = 1f,
                isCompleted = true,
                localUri = Uri.fromFile(file).toString()
            )
        } else {
            OfflineDownloadState()
        }
    }

    suspend fun downloadMedia(
        key: String,
        title: String,
        url: String,
        extension: String = "mp4"
    ) = withContext(Dispatchers.IO) {
        val targetFile = getTargetFile(key, title, extension)
        val tempFile = File(targetFile.parentFile, "${targetFile.name}.part")

        _states.update {
            it + (key to OfflineDownloadState(isDownloading = true, progress = 0.01f))
        }

        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 13) IPTVHeroProPlus/6.8")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("HTTP ${response.code}")
                }
                val body = response.body ?: throw IllegalStateException("Resposta vazia")
                val totalBytes = body.contentLength()

                body.byteStream().use { input ->
                    FileOutputStream(tempFile).use { output ->
                        val buffer = ByteArray(16 * 1024)
                        var downloaded = 0L
                        var read: Int
                        var lastEmit = 0L
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            downloaded += read
                            val now = System.currentTimeMillis()
                            if (now - lastEmit > 250L) {
                                lastEmit = now
                                val pct = if (totalBytes > 0L) {
                                    (downloaded.toFloat() / totalBytes.toFloat()).coerceIn(0.01f, 0.99f)
                                } else 0.5f
                                _states.update {
                                    it + (key to OfflineDownloadState(isDownloading = true, progress = pct))
                                }
                            }
                        }
                    }
                }
            }

            if (targetFile.exists()) targetFile.delete()
            tempFile.renameTo(targetFile)

            _states.update {
                it + (key to OfflineDownloadState(
                    isDownloading = false,
                    progress = 1f,
                    isCompleted = true,
                    localUri = Uri.fromFile(targetFile).toString()
                ))
            }
        } catch (e: Exception) {
            tempFile.delete()
            _states.update {
                it + (key to OfflineDownloadState(
                    isDownloading = false,
                    progress = 0f,
                    isCompleted = false,
                    error = "Falha no download: ${e.message ?: "Erro"}"
                ))
            }
        }
    }

    fun deleteOfflineFile(key: String, title: String, extension: String = "mp4") {
        getTargetFile(key, title, extension).delete()
        _states.update { it - key }
    }
}
