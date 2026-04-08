package com.aria2.downloader.data.repository

import com.aria2.downloader.data.local.AppDatabase
import com.aria2.downloader.data.local.DownloadEntity
import com.aria2.downloader.domain.model.DownloadInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DownloadRepository(private val database: AppDatabase) {
    private val dao = database.downloadDao()

    suspend fun insertDownload(downloadInfo: DownloadInfo) {
        dao.insertDownload(DownloadEntity.fromDomainModel(downloadInfo))
    }

    suspend fun updateDownload(downloadInfo: DownloadInfo) {
        dao.updateDownload(DownloadEntity.fromDomainModel(downloadInfo))
    }

    suspend fun deleteDownload(downloadId: String) {
        dao.deleteDownloadById(downloadId)
    }

    suspend fun getDownloadById(id: String): DownloadInfo? {
        return dao.getDownloadById(id)?.toDomainModel()
    }

    fun getAllDownloads(): Flow<List<DownloadInfo>> {
        return dao.getAllDownloads().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    fun getActiveDownloads(): Flow<List<DownloadInfo>> {
        return dao.getActiveDownloads().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    fun getCompletedDownloads(): Flow<List<DownloadInfo>> {
        return dao.getCompletedDownloads().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    fun getFailedDownloads(): Flow<List<DownloadInfo>> {
        return dao.getFailedDownloads().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    suspend fun clearCompleted() {
        dao.deleteAllCompleted()
    }

    suspend fun clearAll() {
        dao.clearAllDownloads()
    }
}
