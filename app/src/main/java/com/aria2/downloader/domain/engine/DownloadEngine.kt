package com.aria2.downloader.domain.engine

import android.content.Context
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import com.aria2.downloader.data.preferences.SettingsRepository
import com.aria2.downloader.data.repository.DownloadRepository
import com.aria2.downloader.domain.model.DownloadInfo
import com.aria2.downloader.domain.model.DownloadSourceType
import com.aria2.downloader.domain.model.DownloadStatus
import com.aria2.downloader.service.DownloadService
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

@Singleton
class DownloadEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: DownloadRepository,
    private val validator: URLValidator,
    private val processManager: Aria2ProcessManager,
    private val rpcClient: Aria2RpcClient,
    private val settingsRepository: SettingsRepository
) {
    suspend fun enqueueLink(input: String, selectedFiles: String? = null): Result<DownloadInfo> = runCatching {
        val validation = validator.inspect(input)
        processManager.ensureRunning()
        val settings = settingsRepository.currentSettings()
        applyGlobalSettings(settings)

        val initial = DownloadInfo(
            id = UUID.randomUUID().toString(),
            source = validation.normalizedInput,
            fileName = validation.fileName,
            sourceType = validation.sourceType,
            destinationDir = processManager.downloadDirectory.absolutePath,
            totalBytes = validation.totalBytes,
            status = DownloadStatus.VALIDATING,
            mimeType = validation.mimeType,
            selectedFiles = selectedFiles
        )
        repository.upsert(initial)

        val gid = rpcClient.addUri(
            validation.normalizedInput,
            buildDownloadOptions(
                settings = settings,
                fileName = validation.fileName,
                selectedFiles = selectedFiles,
                includeOut = validation.sourceType == DownloadSourceType.DIRECT
            )
        )

        val queued = initial.copy(
            aria2Gid = gid,
            status = if (validation.sourceType == DownloadSourceType.MAGNET) DownloadStatus.METADATA else DownloadStatus.QUEUED,
            updatedAt = System.currentTimeMillis()
        )
        repository.upsert(queued)
        ensureForegroundService()
        syncNow().all.firstOrNull { it.id == queued.id } ?: queued
    }

    suspend fun enqueueImportedDocument(
        uri: Uri,
        type: DownloadSourceType,
        selectedFiles: String? = null
    ): Result<DownloadInfo> = runCatching {
        require(type == DownloadSourceType.TORRENT || type == DownloadSourceType.METALINK) {
            "Only .torrent and metalink files can be imported."
        }
        val localCopy = copyImportToInternalStorage(uri)
        val displayName = DocumentFile.fromSingleUri(context, uri)?.name ?: localCopy.name
        enqueueImportedFile(localCopy, displayName, type, selectedFiles)
    }

    suspend fun pause(downloadId: String) {
        val record = repository.getById(downloadId) ?: return
        val gid = record.aria2Gid ?: return
        processManager.ensureRunning()
        rpcClient.pause(gid)
        repository.upsert(record.copy(status = DownloadStatus.PAUSED, updatedAt = System.currentTimeMillis()))
    }

    suspend fun resume(downloadId: String) {
        val record = repository.getById(downloadId) ?: return
        val gid = record.aria2Gid ?: return
        processManager.ensureRunning()
        rpcClient.unpause(gid)
        ensureForegroundService()
        repository.upsert(record.copy(status = DownloadStatus.QUEUED, updatedAt = System.currentTimeMillis(), errorMessage = null))
    }

    suspend fun cancel(downloadId: String) {
        val record = repository.getById(downloadId) ?: return
        record.aria2Gid?.let { gid ->
            processManager.ensureRunning()
            rpcClient.forceRemove(gid)
        }
        repository.upsert(
            record.copy(
                status = DownloadStatus.CANCELLED,
                updatedAt = System.currentTimeMillis(),
                completedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun retry(downloadId: String): Result<DownloadInfo> {
        val record = repository.getById(downloadId)
            ?: return Result.failure(IllegalArgumentException("Download not found"))
        return when (record.sourceType) {
            DownloadSourceType.TORRENT ->
                runCatching { enqueueImportedFile(File(record.source), record.fileName, DownloadSourceType.TORRENT, record.selectedFiles) }
            DownloadSourceType.METALINK ->
                runCatching { enqueueImportedFile(File(record.source), record.fileName, DownloadSourceType.METALINK, record.selectedFiles) }
            else ->
                enqueueLink(record.source, record.selectedFiles)
        }
    }

    suspend fun syncNow(): SyncSnapshot {
        processManager.ensureRunning()
        applyGlobalSettings(settingsRepository.currentSettings())
        val active = rpcClient.tellActive()
        val waiting = rpcClient.tellWaiting()
        val stopped = rpcClient.tellStopped(num = 200)

        val mapped = (active + waiting + stopped).map { task ->
            val existing = repository.getByGid(task.gid)
            val destinationDir = File(task.savePath).parent ?: existing?.destinationDir ?: processManager.downloadDirectory.absolutePath
            val completedAt = if (task.status == DownloadStatus.COMPLETED) System.currentTimeMillis() else existing?.completedAt
            DownloadInfo(
                id = existing?.id ?: UUID.randomUUID().toString(),
                aria2Gid = task.gid,
                source = existing?.source ?: task.source,
                fileName = if (task.fileName.isBlank()) existing?.fileName ?: "download-${task.gid}" else task.fileName,
                sourceType = existing?.sourceType ?: task.sourceType,
                destinationDir = destinationDir,
                totalBytes = if (task.totalLength > 0) task.totalLength else existing?.totalBytes ?: 0L,
                completedBytes = task.completedLength,
                downloadSpeedBytes = task.downloadSpeed,
                uploadSpeedBytes = task.uploadSpeed,
                connections = task.connections,
                status = task.status,
                mimeType = existing?.mimeType,
                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                completedAt = completedAt,
                errorCode = task.errorCode,
                errorMessage = task.errorMessage,
                selectedFiles = existing?.selectedFiles ?: task.selectedFiles,
                infoHash = task.infoHash ?: existing?.infoHash
            )
        }

        if (mapped.isNotEmpty()) {
            repository.upsertAll(mapped)
        }

        val all = repository.observeAll().first()
        val activeVisible = all.filter { it.status in setOf(
            DownloadStatus.QUEUED,
            DownloadStatus.VALIDATING,
            DownloadStatus.METADATA,
            DownloadStatus.DOWNLOADING,
            DownloadStatus.PAUSED
        ) }
        return SyncSnapshot(
            all = all,
            active = activeVisible,
            totalDownloadSpeed = activeVisible.sumOf { it.downloadSpeedBytes }
        )
    }

    suspend fun applyGlobalSettings() {
        applyGlobalSettings(settingsRepository.currentSettings())
    }

    private suspend fun applyGlobalSettings(settings: com.aria2.downloader.domain.model.AppSettings) {
        rpcClient.changeGlobalOption(
            mapOf(
                "max-concurrent-downloads" to settings.maxConcurrentDownloads.toString(),
                "split" to settings.split.toString(),
                "max-connection-per-server" to settings.maxConnectionPerServer.toString(),
                "min-split-size" to "${settings.minSplitSizeMb}M",
                "enable-dht" to settings.enableDht.toString(),
                "enable-peer-exchange" to settings.peerExchange.toString(),
                "bt-enable-lpd" to settings.localPeerDiscovery.toString(),
                "bt-require-crypto" to settings.requireEncryption.toString()
            )
        )
    }

    data class SyncSnapshot(
        val all: List<DownloadInfo>,
        val active: List<DownloadInfo>,
        val totalDownloadSpeed: Long
    )

    private suspend fun enqueueImportedFile(
        localFile: File,
        displayName: String,
        type: DownloadSourceType,
        selectedFiles: String?
    ): DownloadInfo {
        processManager.ensureRunning()
        val settings = settingsRepository.currentSettings()
        applyGlobalSettings(settings)

        val initial = DownloadInfo(
            id = UUID.randomUUID().toString(),
            source = localFile.absolutePath,
            fileName = displayName,
            sourceType = type,
            destinationDir = processManager.downloadDirectory.absolutePath,
            status = DownloadStatus.VALIDATING,
            selectedFiles = selectedFiles
        )
        repository.upsert(initial)

        val bytes = localFile.readBytes()
        val gid = when (type) {
            DownloadSourceType.TORRENT ->
                rpcClient.addTorrent(bytes, buildDownloadOptions(settings, displayName, selectedFiles, includeOut = false))
            DownloadSourceType.METALINK ->
                rpcClient.addMetalink(bytes, buildDownloadOptions(settings, displayName, selectedFiles, includeOut = false)).first()
            else -> error("Unsupported import type")
        }

        val queued = initial.copy(
            aria2Gid = gid,
            status = if (type == DownloadSourceType.TORRENT) DownloadStatus.METADATA else DownloadStatus.QUEUED,
            updatedAt = System.currentTimeMillis()
        )
        repository.upsert(queued)
        ensureForegroundService()
        return syncNow().all.firstOrNull { it.id == queued.id } ?: queued
    }

    private fun buildDownloadOptions(
        settings: com.aria2.downloader.domain.model.AppSettings,
        fileName: String,
        selectedFiles: String?,
        includeOut: Boolean
    ): Map<String, String> {
        val mutable = linkedMapOf(
            "dir" to processManager.downloadDirectory.absolutePath,
            "continue" to "true",
            "split" to settings.split.toString(),
            "max-connection-per-server" to settings.maxConnectionPerServer.toString(),
            "min-split-size" to "${settings.minSplitSizeMb}M"
        )
        if (includeOut) {
            mutable["out"] = fileName
        }
        if (!selectedFiles.isNullOrBlank()) {
            mutable["select-file"] = selectedFiles
        }
        return mutable
    }

    private fun ensureForegroundService() {
        val intent = DownloadService.createIntent(context)
        ContextCompat.startForegroundService(context, intent)
    }

    private suspend fun copyImportToInternalStorage(uri: Uri): File = withContext(Dispatchers.IO) {
        val name = DocumentFile.fromSingleUri(context, uri)?.name ?: "import-${System.currentTimeMillis()}"
        val extension = name.substringAfterLast('.', "")
        val target = File(context.filesDir, "aria2/imports/${UUID.randomUUID()}${if (extension.isNotBlank()) ".$extension" else ""}")
        target.parentFile?.mkdirs()
        context.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        } ?: error("Unable to read the selected file.")
        target
    }
}
