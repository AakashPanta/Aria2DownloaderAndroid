package com.aria2.downloader.domain.engine

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.RandomAccessFile

data class DownloadSegment(
    val index: Int,
    val startByte: Long,
    val endByte: Long,
    var downloadedBytes: Long = 0L
)

class SegmentedDownloader(
    private val okHttpClient: OkHttpClient,
    private val maxConnections: Int = 4
) {
    companion object {
        private const val TAG = "SegmentedDownloader"
        private const val BUFFER_SIZE = 8192
    }

    suspend fun download(
        url: String,
        outputFile: File,
        fileSize: Long,
        supportsResume: Boolean,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit,
        onCancel: suspend () -> Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            outputFile.parentFile?.mkdirs()

            if (!supportsResume || fileSize <= 0L || maxConnections <= 1) {
                downloadSingleConnection(
                    url = url,
                    outputFile = outputFile,
                    onProgress = onProgress,
                    onCancel = onCancel
                )
            } else {
                downloadSegmented(
                    url = url,
                    outputFile = outputFile,
                    fileSize = fileSize,
                    onProgress = onProgress,
                    onCancel = onCancel
                )
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

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
                }

                val body = response.body ?: return Result.failure(Exception("Empty response body"))
                val totalBytes = body.contentLength()

                RandomAccessFile(outputFile, "rw").use { file ->
                    body.byteStream().use { input ->
                        val buffer = ByteArray(BUFFER_SIZE)
                        var downloadedBytes = 0L

                        while (true) {
                            val bytesRead = input.read(buffer)
                            if (bytesRead == -1) break

                            if (onCancel()) {
                                return Result.failure(Exception("Download cancelled"))
                            }

                            file.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead
                            onProgress(downloadedBytes, totalBytes)
                        }
                    }
                }
            }

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
    ): Result<Unit> = coroutineScope {
        try {
            val actualConnections = maxConnections.coerceAtLeast(1)
            val segmentSize = (fileSize + actualConnections - 1) / actualConnections
            val segments = mutableListOf<DownloadSegment>()

            var currentStart = 0L
            for (i in 0 until actualConnections) {
                if (currentStart >= fileSize) break

                val end = minOf(currentStart + segmentSize - 1, fileSize - 1)
                segments.add(
                    DownloadSegment(
                        index = i,
                        startByte = currentStart,
                        endByte = end
                    )
                )
                currentStart = end + 1
            }

            RandomAccessFile(outputFile, "rw").use { raf ->
                raf.setLength(fileSize)
            }

            val progressMutex = Mutex()

            val results = segments.map { segment ->
                async {
                    downloadSegment(
                        url = url,
                        outputFile = outputFile,
                        segment = segment,
                        allSegments = segments,
                        totalBytes = fileSize,
                        progressMutex = progressMutex,
                        onProgress = onProgress,
                        onCancel = onCancel
                    )
                }
            }.awaitAll()

            val firstError = results.firstOrNull { it.isFailure }?.exceptionOrNull()
            if (firstError != null) {
                Result.failure(firstError)
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
        allSegments: List<DownloadSegment>,
        totalBytes: Long,
        progressMutex: Mutex,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit,
        onCancel: suspend () -> Boolean
    ): Result<Unit> {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("Range", "bytes=${segment.startByte}-${segment.endByte}")
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return Result.failure(
                        Exception("HTTP ${response.code}: Segment ${segment.index} download failed")
                    )
                }

                val body = response.body ?: return Result.failure(Exception("Empty response body"))

                RandomAccessFile(outputFile, "rw").use { file ->
                    file.seek(segment.startByte)

                    body.byteStream().use { input ->
                        val buffer = ByteArray(BUFFER_SIZE)

                        while (true) {
                            val bytesRead = input.read(buffer)
                            if (bytesRead == -1) break

                            if (onCancel()) {
                                return Result.failure(Exception("Download cancelled"))
                            }

                            file.write(buffer, 0, bytesRead)
                            segment.downloadedBytes += bytesRead.toLong()

                            progressMutex.withLock {
                                val totalDownloaded = calculateTotalDownloaded(allSegments)
                                onProgress(totalDownloaded, totalBytes)
                            }
                        }
                    }
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Segment ${segment.index} download failed", e)
            Result.failure(e)
        }
    }

    private fun calculateTotalDownloaded(segments: List<DownloadSegment>): Long {
        return segments.sumOf { segment -> segment.downloadedBytes }
    }
}
