package com.aria2.downloader.domain.engine

import android.content.Context
import android.os.Environment
import android.util.Log
import com.aria2.downloader.domain.model.DownloadInfo
import com.aria2.downloader.domain.model.DownloadProgress
import com.aria2.downloader.domain.model.DownloadStatus
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import java.io.File
import java.util.*

class DownloadEngine(
    private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val maxConnections: Int = 4
) {
    companion object {
        private const val TAG = "DownloadEngine"
    }

    private val segmentedDownloader = SegmentedDownloader(okHttpClient, maxConnections)
    private val bandwidthTracker = BandwidthTracker()
    private val activeDownloads = mutableMapOf<String, Job>()
    private val downloadJobs = mutableMapOf<String, Job>()

    suspend fun startDownload(
        downloadInfo: DownloadInfo,
        onProgressUpdate: suspend (DownloadInfo) -> Unit
    ): Result<Unit> {
        return try {
            val downloadDir = getDownloadDirectory()
            val outputFile = File(downloadDir, downloadInfo.fileName)

            var currentInfo = downloadInfo.copy(status = DownloadStatus.DOWNLOADING)
            activeDownloads[downloadInfo.id] = coroutineScope {
                segmentedDownloader.download(
                    url = downloadInfo.url,
                    outputFile = outputFile,
                    fileSize = downloadInfo.fileSize,
                    supportsResume = downloadInfo.supportsResume,
                    onProgress = { downloadedBytes, totalBytes ->
                        bandwidthTracker.updateProgress(downloadedBytes)
                        val speed = bandwidthTracker.getCurrentSpeed()
                        val remaining = bandwidthTracker.estimateTimeRemaining(totalBytes - downloadedBytes)

                        currentInfo = currentInfo.copy(
                            downloadedBytes = downloadedBytes,
                            status = DownloadStatus.DOWNLOADING,
                            progress = DownloadProgress(
                                downloadedBytes = downloadedBytes,
                                totalBytes = totalBytes,
                                progress = if (totalBytes > 0) downloadedBytes.toFloat() / totalBytes else 0f,
                                speedBytesPerSecond = speed,
                                estimatedTimeRemainingMs = remaining
                            )
                        )
                    },
                    onCancel = {
                        activeDownloads[downloadInfo.id]?.isCancelled ?: false
                    }
                )
            }

            val result = activeDownloads[downloadInfo.id]?.let {
                try {
                    withTimeoutOrNull(Long.MAX_VALUE) {
                        it.await()
                        Result.success(Unit)
                    } ?: Result.failure(Exception("Download timeout"))
                } catch (e: CancellationException) {
                    Result.failure(e)
                }
            } ?: Result.failure(Exception("Download job not found"))

            if (result.isSuccess) {
                currentInfo = currentInfo.copy(
                    status = DownloadStatus.COMPLETED,
                    completedAt = System.currentTimeMillis(),
                    downloadedBytes = downloadInfo.fileSize
                )
            } else {
                currentInfo = currentInfo.copy(
                    status = DownloadStatus.FAILED,
                    errorMessage = result.exceptionOrNull()?.message ?: "Unknown error"
                )
            }

            onProgressUpdate(currentInfo)
            activeDownloads.remove(downloadInfo.id)
            result
        } catch (e: Exception) {
            Log.e(TAG, "Download error", e)
            Result.failure(e)
        }
    }

    suspend fun pauseDownload(downloadId: String) {
        activeDownloads[downloadId]?.cancel()
        activeDownloads.remove(downloadId)
    }

    suspend fun cancelDownload(downloadId: String) {
        activeDownloads[downloadId]?.cancel()
        activeDownloads.remove(downloadId)
    }

    fun getDownloadDirectory(): File {
        return File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
            "Aria2Downloads"
        ).apply { mkdirs() }
    }

    fun isDownloadActive(downloadId: String): Boolean {
        return activeDownloads.containsKey(downloadId)
    }

    suspend fun validateURL(url: String): URLValidator.URLInfo? {
        return URLValidator().validateAndGetInfo(url)
    }

    fun cleanup() {
        activeDownloads.values.forEach { it.cancel() }
        activeDownloads.clear()
        bandwidthTracker.reset()
    }
}
