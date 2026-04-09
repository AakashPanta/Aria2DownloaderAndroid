package com.aria2.downloader.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.aria2.downloader.domain.model.DownloadInfo
import com.aria2.downloader.domain.model.DownloadSourceType
import com.aria2.downloader.domain.model.DownloadStatus

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val id: String,
    val aria2Gid: String?,
    val source: String,
    val fileName: String,
    val sourceType: String,
    val destinationDir: String,
    val totalBytes: Long,
    val completedBytes: Long,
    val downloadSpeedBytes: Long,
    val uploadSpeedBytes: Long,
    val connections: Int,
    val status: String,
    val mimeType: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val completedAt: Long?,
    val errorCode: String?,
    val errorMessage: String?,
    val selectedFiles: String?,
    val infoHash: String?
) {
    fun toDomain(): DownloadInfo = DownloadInfo(
        id = id,
        aria2Gid = aria2Gid,
        source = source,
        fileName = fileName,
        sourceType = runCatching { DownloadSourceType.valueOf(sourceType) }.getOrDefault(DownloadSourceType.DIRECT),
        destinationDir = destinationDir,
        totalBytes = totalBytes,
        completedBytes = completedBytes,
        downloadSpeedBytes = downloadSpeedBytes,
        uploadSpeedBytes = uploadSpeedBytes,
        connections = connections,
        status = runCatching { DownloadStatus.valueOf(status) }.getOrDefault(DownloadStatus.QUEUED),
        mimeType = mimeType,
        createdAt = createdAt,
        updatedAt = updatedAt,
        completedAt = completedAt,
        errorCode = errorCode,
        errorMessage = errorMessage,
        selectedFiles = selectedFiles,
        infoHash = infoHash
    )

    companion object {
        fun fromDomain(download: DownloadInfo): DownloadEntity = DownloadEntity(
            id = download.id,
            aria2Gid = download.aria2Gid,
            source = download.source,
            fileName = download.fileName,
            sourceType = download.sourceType.name,
            destinationDir = download.destinationDir,
            totalBytes = download.totalBytes,
            completedBytes = download.completedBytes,
            downloadSpeedBytes = download.downloadSpeedBytes,
            uploadSpeedBytes = download.uploadSpeedBytes,
            connections = download.connections,
            status = download.status.name,
            mimeType = download.mimeType,
            createdAt = download.createdAt,
            updatedAt = download.updatedAt,
            completedAt = download.completedAt,
            errorCode = download.errorCode,
            errorMessage = download.errorMessage,
            selectedFiles = download.selectedFiles,
            infoHash = download.infoHash
        )
    }
}
