package com.aria2.downloader.domain.engine

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.RandomAccessFile

data class DownloadSegment(
    val index: Int,
    val startByte: Long,
    val endByte: Long,
    var downloadedBytes: Long = 0
)

class SegmentedDownloader(
    private val okHttpClient: OkHttpClient,
    private val maxConnections: Int = 4
) {
    companion object {
        private const val TAG = "SegmentedDownloader"
    }

    suspend fun download(
        url: String,
        outputFile: File,
        fileSize: Long,
        supportsResume: Boolean,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit,
        onCancel: suspend () -> Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            // Create parent directories if needed
            outputFile.parentFile?.mkdirs()

            // If server doesn't support resume or file size is unknown, do single-connection download
            if (!supportsResume || fileSize <= 0) {
                downloadSingleConnection(url, outputFile, onProgress, onCancel)
            } else {
                downloadSegmented(url, outputFile, fileSize, onProgress, onCancel)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Download failed", e)
            Result.failure(e)
        }
    }

    private suspend fun downloadSingleConnection(
        url: String,
        outputFile: File,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit,
        onCancel: suspend () -> Boolean
    ): Result<Unit> {
        return try {
            val request = Request.Builder()
                .url(url)
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
            }

            val body = response.body ?: return Result.failure(Exception("Empty response body"))
            val totalBytes = body.contentLength()

            RandomAccessFile(outputFile, "rw").use { file ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(8192)
                    var downloadedBytes = 0L
                    var bytesRead: Int

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        if (onCancel()) {
                            return Result.failure(Exception("Download cancelled"))
                        }
                        file.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        onProgress(downloadedBytes, totalBytes)
                    }
                }
            }

            response.close()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Single connection download failed", e)
            Result.failure(e)
        }
    }

    private suspend fun downloadSegmented(
        url: String,
        outputFile: File,
        fileSize: Long,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit,
        onCancel: suspend () -> Boolean
    ): Result<Unit> {
        return try {
            val segmentSize = (fileSize + maxConnections - 1) / maxConnections
            val segments = mutableListOf<DownloadSegment>()

            var currentStart = 0L
            for (i in 0 until maxConnections) {
                val start = currentStart
                val end = minOf(currentStart + segmentSize - 1, fileSize - 1)
                if (start < fileSize) {
                    segments.add(DownloadSegment(i, start, end))
                    currentStart = end + 1
                }
            }

            // Create empty file
            RandomAccessFile(outputFile, "rw").use { it.setLength(fileSize) }

            val downloadMutex = Mutex()
            val segmentJobs = segments.map { segment ->
                async {
                    downloadSegment(url, outputFile, segment, downloadMutex, onProgress, onCancel)
                }
            }

            // Wait for all segments to complete
            val results = segmentJobs.awaitAll()
            val hasError = results.any { !it.isSuccess }

            if (hasError) {
                val error = results.firstOrNull { !it.isSuccess }?.exceptionOrNull()
                Result.failure(error ?: Exception("Segment download failed"))
            } else {
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Segmented download failed", e)
            Result.failure(e)
        }
    }

    private suspend fun downloadSegment(
        url: String,
        outputFile: File,
        segment: DownloadSegment,
        downloadMutex: Mutex,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit,
        onCancel: suspend () -> Boolean
    ): Result<Unit> {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("Range", "bytes=${segment.startByte}-${segment.endByte}")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (response.code !in 200..299) {
                return Result.failure(Exception("HTTP ${response.code}: Segment download failed"))
            }

            val body = response.body ?: return Result.failure(Exception("Empty response"))

            RandomAccessFile(outputFile, "rw").use { file ->
                body.byteStream().use { input ->
                    file.seek(segment.startByte)
                    val buffer = ByteArray(8192)
                    var bytesRead: Int

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        if (onCancel()) {
                            response.close()
                            return Result.failure(Exception("Download cancelled"))
                        }
                        file.write(buffer, 0, bytesRead)
                        segment.downloadedBytes += bytesRead

                        downloadMutex.withLock {
                            val totalDownloaded = calculateTotalDownloaded()
                            onProgress(totalDownloaded, (file.length()))
                        }
                    }
                }
            }

            response.close()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Segment ${segment.index} download failed", e)
            Result.failure(e)
        }
    }

    private fun calculateTotalDownloaded(): Long {
        // This is a simplified version; in production, you'd track all segments
        return 0L
    }
}
