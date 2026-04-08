package com.aria2.downloader.domain.engine

import android.util.Patterns
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class URLValidator {
    
    fun isValidUrl(url: String): Boolean {
        return try {
            URL(url)
            Patterns.WEB_URL.matcher(url).matches()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun validateAndGetInfo(url: String): URLInfo? = withContext(Dispatchers.IO) {
        return@withContext try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "HEAD"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.instanceFollowRedirects = true

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                return@withContext null
            }

            val contentLength = connection.getHeaderField("Content-Length")?.toLongOrNull() ?: -1L
            val acceptRanges = connection.getHeaderField("Accept-Ranges") ?: "none"
            val contentType = connection.getHeaderField("Content-Type") ?: "application/octet-stream"
            val contentDisposition = connection.getHeaderField("Content-Disposition") ?: ""

            val fileName = extractFileName(url, contentDisposition)

            connection.disconnect()

            URLInfo(
                url = url,
                fileName = fileName,
                fileSize = contentLength,
                supportsResume = acceptRanges.equals("bytes", ignoreCase = true),
                contentType = contentType,
                exists = true
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun extractFileName(url: String, contentDisposition: String): String {
        // Try to extract from Content-Disposition header
        val regex = """filename=(?:"([^"]+)"|([^;\s]+))""".toRegex()
        val matchResult = regex.find(contentDisposition)
        if (matchResult != null) {
            val fileName = matchResult.groupValues[1].ifEmpty { matchResult.groupValues[2] }
            if (fileName.isNotEmpty()) {
                return fileName
            }
        }

        // Extract from URL
        val path = URL(url).path
        return path.substringAfterLast('/').takeIf { it.isNotEmpty() } ?: "download"
    }

    data class URLInfo(
        val url: String,
        val fileName: String,
        val fileSize: Long,
        val supportsResume: Boolean,
        val contentType: String,
        val exists: Boolean
    )
}
