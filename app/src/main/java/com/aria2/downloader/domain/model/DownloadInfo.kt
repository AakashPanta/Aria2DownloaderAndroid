package com.aria2.downloader.domain.model

import kotlin.math.roundToInt

data class DownloadInfo(
    val id: String,
    val url: String,
    val fileName: String,
    val fileSize: Long,
    val downloadedBytes: Long = 0,
    val status: DownloadStatus = DownloadStatus.PENDING,
    val progress: DownloadProgress = DownloadProgress(),
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val errorMessage: String? = null,
    val supportsResume: Boolean = true
) {
    val isCompleted: Boolean
        get() = status == DownloadStatus.COMPLETED

    val isFailed: Boolean
        get() = status == DownloadStatus.FAILED

    val isActive: Boolean
        get() = status == DownloadStatus.DOWNLOADING || status == DownloadStatus.PENDING

    val progressPercent: Int
        get() = if (fileSize > 0) {
            ((downloadedBytes.toDouble() / fileSize.toDouble()) * 100.0).roundToInt().coerceIn(0, 100)
        } else {
            0
        }

    fun formattedFileSize(): String {
        return formatBytes(fileSize)
    }

    fun formattedDownloadedSize(): String {
        return "${formatBytes(downloadedBytes)} / ${formatBytes(fileSize)}"
    }

    companion object {
        fun formatBytes(bytes: Long): String {
            return when {
                bytes < 1024 -> "$bytes B"
                bytes < 1024 * 1024 -> "${bytes / 1024} KB"
                bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
                else -> "${bytes / (1024 * 1024 * 1024)} GB"
            }
        }
    }
}
