package com.aria2.downloader.domain.model

import java.io.File

data class DownloadInfo(
    val id: String,
    val aria2Gid: String? = null,
    val source: String,
    val fileName: String,
    val sourceType: DownloadSourceType = DownloadSourceType.DIRECT,
    val destinationDir: String,
    val totalBytes: Long = 0L,
    val completedBytes: Long = 0L,
    val downloadSpeedBytes: Long = 0L,
    val uploadSpeedBytes: Long = 0L,
    val connections: Int = 0,
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val mimeType: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val errorCode: String? = null,
    val errorMessage: String? = null,
    val selectedFiles: String? = null,
    val infoHash: String? = null
) {
    val progress: DownloadProgress
        get() = DownloadProgress(
            completedBytes = completedBytes,
            totalBytes = totalBytes,
            downloadSpeedBytes = downloadSpeedBytes,
            uploadSpeedBytes = uploadSpeedBytes,
            connections = connections
        )

    val isActive: Boolean
        get() = status in setOf(
            DownloadStatus.QUEUED,
            DownloadStatus.VALIDATING,
            DownloadStatus.METADATA,
            DownloadStatus.DOWNLOADING,
            DownloadStatus.PAUSED
        )

    val progressPercent: Int
        get() = progress.percent

    val filePath: String?
        get() = if (fileName.isBlank()) null else File(destinationDir, fileName).absolutePath

    fun formattedTotalSize(): String = DownloadProgress.formatBytes(totalBytes)
    fun formattedCompletedSize(): String = DownloadProgress.formatBytes(completedBytes)
    fun formattedStatus(): String = status.name.replace('_', ' ').lowercase()
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

    fun formattedConnections(): String = if (connections > 0) connections.toString() else "—"
}
