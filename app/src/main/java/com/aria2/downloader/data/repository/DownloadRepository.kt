package com.aria2.downloader.data.repository

import com.aria2.downloader.data.local.DownloadDao
import com.aria2.downloader.data.local.DownloadEntity
import com.aria2.downloader.domain.model.DownloadInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadRepository @Inject constructor(
    private val downloadDao: DownloadDao
) {
    fun observeAll(): Flow<List<DownloadInfo>> = downloadDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeActive(): Flow<List<DownloadInfo>> = downloadDao.observeActive().map { list -> list.map { it.toDomain() } }

    fun observeFinished(): Flow<List<DownloadInfo>> = downloadDao.observeFinished().map { list -> list.map { it.toDomain() } }

    fun observeCompleted(): Flow<List<DownloadInfo>> = downloadDao.observeCompleted().map { list -> list.map { it.toDomain() } }

    suspend fun getById(id: String): DownloadInfo? = downloadDao.getById(id)?.toDomain()

    suspend fun getByGid(gid: String): DownloadInfo? = downloadDao.getByGid(gid)?.toDomain()

    suspend fun upsert(download: DownloadInfo) = downloadDao.upsert(DownloadEntity.fromDomain(download))

    suspend fun upsertAll(downloads: List<DownloadInfo>) =
        downloadDao.upsertAll(downloads.map { DownloadEntity.fromDomain(it) })

    suspend fun deleteById(id: String) = downloadDao.deleteById(id)

    suspend fun clearCompleted() = downloadDao.clearCompleted()

    suspend fun clearAll() = downloadDao.clearAll()
}
