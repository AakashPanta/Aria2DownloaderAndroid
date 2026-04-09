package com.aria2.downloader.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(download: DownloadEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(downloads: List<DownloadEntity>)

    @Query("SELECT * FROM downloads ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status IN ('VALIDATING','METADATA','DOWNLOADING','PAUSED') ORDER BY updatedAt DESC")
    fun observeRunning(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status = 'QUEUED' ORDER BY updatedAt ASC")
    fun observeQueued(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status IN ('QUEUED','VALIDATING','METADATA','DOWNLOADING','PAUSED') ORDER BY updatedAt DESC")
    fun observeActive(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status IN ('COMPLETED','FAILED','CANCELLED') ORDER BY updatedAt DESC")
    fun observeFinished(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status = 'COMPLETED' ORDER BY completedAt DESC, updatedAt DESC")
    fun observeCompleted(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): DownloadEntity?

    @Query("SELECT * FROM downloads WHERE aria2Gid = :gid LIMIT 1")
    suspend fun getByGid(gid: String): DownloadEntity?

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM downloads WHERE status = 'COMPLETED'")
    suspend fun clearCompleted()

    @Query("DELETE FROM downloads")
    suspend fun clearAll()
}
