package com.aria2.downloader.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.aria2.downloader.domain.model.DownloadInfo
import com.aria2.downloader.domain.model.DownloadProgress
import com.aria2.downloader.domain.model.DownloadStatus

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey
    val id: String,
    val url: String,
    val fileName: String,
    val fileSize: Long,
    val downloadedBytes: Long,
    val status: String, // Store as string for Room compatibility
    val createdAt: Long,
    val completedAt: Long?,
    val errorMessage: String?,
    val supportsResume: Boolean
) {
    fun toDomainModel(): DownloadInfo {
        return DownloadInfo(
            id = id,
            url = url,
            fileName = fileName,
            fileSize = fileSize,
            downloadedBytes = downloadedBytes,
            status = DownloadStatus.valueOf(status),
            progress = DownloadProgress(
                downloadedBytes = downloadedBytes,
                totalBytes = fileSize,
                progress = if (fileSize > 0) downloadedBytes.toFloat() / fileSize else 0f
            ),
            createdAt = createdAt,
            completedAt = completedAt,
            errorMessage = errorMessage,
            supportsResume = supportsResume
        )
    }

    companion object {
        fun fromDomainModel(downloadInfo: DownloadInfo): DownloadEntity {
            return DownloadEntity(
                id = downloadInfo.id,
                url = downloadInfo.url,
                fileName = downloadInfo.fileName,
                fileSize = downloadInfo.fileSize,
                downloadedBytes = downloadInfo.downloadedBytes,
                status = downloadInfo.status.name,
                createdAt = downloadInfo.createdAt,
                completedAt = downloadInfo.completedAt,
                errorMessage = downloadInfo.errorMessage,
                supportsResume = downloadInfo.supportsResume
            )
        }
    }
}
