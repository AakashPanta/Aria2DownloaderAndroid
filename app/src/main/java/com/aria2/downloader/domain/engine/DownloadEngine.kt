package com.aria2.downloader.domain.engine

import android.content.Context
import android.os.Environment
import android.util.Log
import com.aria2.downloader.domain.model.DownloadInfo
import com.aria2.downloader.domain.model.DownloadProgress
import com.aria2.downloader.domain.model.DownloadStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.io.File

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
    private val activeDownloads = mutableMapOf<String, Deferred<Result<Unit>>>()

    suspend fun startDownload(
        downloadInfo: DownloadInfo,
        onProgressUpdate: suspend (DownloadInfo) -> Unit
    ): Result<Unit> = coroutineScope {
        try {
            val downloadDir = getDownloadDirectory()
            val outputFile = File(downloadDir, downloadInfo.fileName)

            var currentInfo = downloadInfo.copy(status = DownloadStatus.DOWNLOADING)
            val progressScope: CoroutineScope = this

            val deferred = async(Dispatchers.IO) {
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

                        progressScope.launch {
                            onProgressUpdate(currentInfo)
                        }
                    },
                    onCancel = {
                        activeDownloads[downloadInfo.id]?.isCancelled == true
                    }
                )
            }

            activeDownloads[downloadInfo.id] = deferred

            val result = try {
                deferred.await()
            } catch (e: CancellationException) {
                Result.failure(e)
            }

            currentInfo = when {
                result.isSuccess -> currentInfo.copy(
                    status = DownloadStatus.COMPLETED,
                    completedAt = System.currentTimeMillis(),
                    downloadedBytes = if (downloadInfo.fileSize > 0) downloadInfo.fileSize else currentInfo.downloadedBytes,
                    progress = currentInfo.progress.copy(
                        downloadedBytes = if (downloadInfo.fileSize > 0) downloadInfo.fileSize else currentInfo.downloadedBytes,
                        totalBytes = if (downloadInfo.fileSize > 0) downloadInfo.fileSize else currentInfo.progress.totalBytes,
                        progress = 1f,
                        estimatedTimeRemainingMs = 0L
                    )
                )
                result.exceptionOrNull() is CancellationException -> currentInfo.copy(
                    status = DownloadStatus.CANCELLED,
                    errorMessage = null
                )
                else -> currentInfo.copy(
                    status = DownloadStatus.FAILED,
                    errorMessage = result.exceptionOrNull()?.message ?: "Unknown error"
                )
            }

            onProgressUpdate(currentInfo)
            activeDownloads.remove(downloadInfo.id)
            result
        } catch (e: Exception) {
            activeDownloads.remove(downloadInfo.id)
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
