package com.aria2.downloader.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(download: DownloadEntity)

    @Update
    suspend fun updateDownload(download: DownloadEntity)

    @Delete
    suspend fun deleteDownload(download: DownloadEntity)

    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun getDownloadById(id: String): DownloadEntity?

    @Query("SELECT * FROM downloads")
    fun getAllDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status = :status")
    fun getDownloadsByStatus(status: String): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status IN ('DOWNLOADING', 'PENDING')")
    fun getActiveDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status = 'COMPLETED' ORDER BY completedAt DESC")
    fun getCompletedDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status = 'FAILED'")
    fun getFailedDownloads(): Flow<List<DownloadEntity>>

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteDownloadById(id: String)

    @Query("DELETE FROM downloads WHERE status = 'COMPLETED'")
    suspend fun deleteAllCompleted()

    @Query("DELETE FROM downloads")
    suspend fun clearAllDownloads()
}
