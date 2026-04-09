package com.aria2.downloader.domain.engine

import android.net.Uri
import android.webkit.MimeTypeMap
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLDecoder
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.aria2.downloader.domain.model.DownloadSourceType

@Singleton
class URLValidator @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    data class ValidationResult(
        val normalizedInput: String,
        val sourceType: DownloadSourceType,
        val fileName: String,
        val totalBytes: Long,
        val mimeType: String?,
        val supportsResume: Boolean,
        val warningMessage: String? = null
    )

    suspend fun inspect(input: String): ValidationResult = withContext(Dispatchers.IO) {
        val trimmed = input.trim()
        require(trimmed.isNotBlank()) { "Please enter a link." }

        if (trimmed.startsWith("magnet:?", ignoreCase = true)) {
            return@withContext ValidationResult(
                normalizedInput = trimmed,
                sourceType = DownloadSourceType.MAGNET,
                fileName = extractMagnetName(trimmed) ?: "Torrent metadata",
                totalBytes = 0L,
                mimeType = "application/x-bittorrent",
                supportsResume = true
            )
        }

        val uri = Uri.parse(trimmed)
        val scheme = uri.scheme?.lowercase()
        require(scheme in listOf("http", "https", "ftp", "sftp")) {
            "Only http, https, ftp, sftp and magnet links are supported here."
        }

        val guessedType = detectSourceType(trimmed, uri)
        val defaultName = extractFileName(trimmed) ?: when (guessedType) {
            DownloadSourceType.TORRENT -> "download.torrent"
            DownloadSourceType.METALINK -> "download.meta4"
            else -> "download"
        }

        val head = inspectWithHead(trimmed)
        if (head != null) {
            return@withContext ValidationResult(
                normalizedInput = head.finalUrl ?: trimmed,
                sourceType = detectSourceType(head.finalUrl ?: trimmed, Uri.parse(head.finalUrl ?: trimmed), head.contentType),
                fileName = head.fileName ?: defaultName,
                totalBytes = head.contentLength,
                mimeType = head.contentType,
                supportsResume = head.acceptRanges
            )
        }

        val rangeGet = inspectWithRangeRequest(trimmed)
        if (rangeGet != null) {
            return@withContext ValidationResult(
                normalizedInput = rangeGet.finalUrl ?: trimmed,
                sourceType = detectSourceType(rangeGet.finalUrl ?: trimmed, Uri.parse(rangeGet.finalUrl ?: trimmed), rangeGet.contentType),
                fileName = rangeGet.fileName ?: defaultName,
                totalBytes = rangeGet.contentLength,
                mimeType = rangeGet.contentType,
                supportsResume = rangeGet.acceptRanges,
                warningMessage = "The server rejected HEAD, so aria2 will start using a ranged GET request instead."
            )
        }

        ValidationResult(
            normalizedInput = trimmed,
            sourceType = guessedType,
            fileName = defaultName,
            totalBytes = 0L,
            mimeType = null,
            supportsResume = true,
            warningMessage = "Could not fully preflight the server. The download will still be handed to aria2."
        )
    }

    private fun detectSourceType(
        raw: String,
        uri: Uri,
        contentType: String? = null
    ): DownloadSourceType {
        val lower = raw.lowercase()
        val type = contentType?.lowercase().orEmpty()
        return when {
            lower.endsWith(".torrent") || type.contains("bittorrent") -> DownloadSourceType.TORRENT
            lower.endsWith(".meta4") || lower.endsWith(".metalink") || type.contains("metalink") || type.contains("xml") -> DownloadSourceType.METALINK
            else -> DownloadSourceType.DIRECT
        }
    }

    private fun extractFileName(url: String): String? {
        val uri = Uri.parse(url)
        val encoded = uri.lastPathSegment ?: return null
        val decoded = runCatching { URLDecoder.decode(encoded, "UTF-8") }.getOrNull() ?: encoded
        return decoded.substringAfterLast('/').takeIf { it.isNotBlank() }
    }

    private fun extractMagnetName(input: String): String? {
        return Uri.parse(input).getQueryParameter("dn")
    }

    private suspend fun inspectWithHead(url: String): RemoteMetadata? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .head()
            .header("User-Agent", "Aria2Downloader/1.1")
            .build()

        runCatching {
            okHttpClient.newCall(request).execute().use { response ->
                val code = response.code
                if (code !in 200..399) {
                    return@use null
                }
                val finalUrl = response.request.url.toString()
                RemoteMetadata(
                    finalUrl = finalUrl,
                    contentLength = response.header("Content-Length")?.toLongOrNull() ?: 0L,
                    contentType = response.header("Content-Type"),
                    acceptRanges = response.header("Accept-Ranges")?.equals("bytes", ignoreCase = true) == true,
                    fileName = response.header("Content-Disposition")?.let(::filenameFromDisposition)
                        ?: extractFileName(finalUrl)
                )
            }
        }.getOrNull()
    }

    private suspend fun inspectWithRangeRequest(url: String): RemoteMetadata? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .get()
            .header("User-Agent", "Aria2Downloader/1.1")
            .header("Range", "bytes=0-0")
            .build()

        runCatching {
            okHttpClient.newCall(request).execute().use { response ->
                val code = response.code
                if (code !in listOf(200, 206)) {
                    return@use null
                }
                val finalUrl = response.request.url.toString()
                val contentRangeLength = response.header("Content-Range")
                    ?.substringAfterLast('/')
                    ?.toLongOrNull()
                RemoteMetadata(
                    finalUrl = finalUrl,
                    contentLength = contentRangeLength ?: response.header("Content-Length")?.toLongOrNull() ?: 0L,
                    contentType = response.header("Content-Type"),
                    acceptRanges = code == 206 || response.header("Accept-Ranges")?.equals("bytes", ignoreCase = true) == true,
                    fileName = response.header("Content-Disposition")?.let(::filenameFromDisposition)
                        ?: extractFileName(finalUrl)
                )
            }
        }.getOrNull()
    }

    private fun filenameFromDisposition(header: String): String? {
        val match = Regex("""filename\*?=(?:UTF-8''|")?([^";]+)""", RegexOption.IGNORE_CASE).find(header)
        return match?.groupValues?.getOrNull(1)?.trim()?.trim('"')
    }

    private data class RemoteMetadata(
        val finalUrl: String?,
        val contentLength: Long,
        val contentType: String?,
        val acceptRanges: Boolean,
        val fileName: String?
    )
}
