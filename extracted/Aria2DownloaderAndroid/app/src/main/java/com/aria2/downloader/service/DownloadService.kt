package com.aria2.downloader.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.aria2.downloader.MainActivity
import com.aria2.downloader.R
import com.aria2.downloader.data.repository.DownloadRepository
import com.aria2.downloader.domain.engine.DownloadEngine
import com.aria2.downloader.domain.model.DownloadStatus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DownloadService : LifecycleService() {
    companion object {
        private const val CHANNEL_ID = "download_service"
        private const val NOTIFICATION_ID = 1
        const val ACTION_START_DOWNLOAD = "com.aria2.downloader.START_DOWNLOAD"
        const val ACTION_PAUSE_DOWNLOAD = "com.aria2.downloader.PAUSE_DOWNLOAD"
        const val ACTION_CANCEL_DOWNLOAD = "com.aria2.downloader.CANCEL_DOWNLOAD"
        const val EXTRA_DOWNLOAD_ID = "download_id"
    }

    @Inject
    lateinit var downloadEngine: DownloadEngine

    @Inject
    lateinit var downloadRepository: DownloadRepository

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        when (intent?.action) {
            ACTION_START_DOWNLOAD -> {
                val downloadId = intent.getStringExtra(EXTRA_DOWNLOAD_ID) ?: return START_STICKY
                lifecycleScope.launch {
                    handleStartDownload(downloadId)
                }
            }
            ACTION_PAUSE_DOWNLOAD -> {
                val downloadId = intent.getStringExtra(EXTRA_DOWNLOAD_ID) ?: return START_STICKY
                lifecycleScope.launch {
                    downloadEngine.pauseDownload(downloadId)
                }
            }
            ACTION_CANCEL_DOWNLOAD -> {
                val downloadId = intent.getStringExtra(EXTRA_DOWNLOAD_ID) ?: return START_STICKY
                lifecycleScope.launch {
                    downloadEngine.cancelDownload(downloadId)
                }
            }
        }

        return START_STICKY
    }

    private suspend fun handleStartDownload(downloadId: String) {
        val downloadInfo = downloadRepository.getDownloadById(downloadId) ?: return

        startForeground(NOTIFICATION_ID, buildNotification("Starting download..."))

        downloadEngine.startDownload(downloadInfo) { updatedInfo ->
            downloadRepository.updateDownload(updatedInfo)
            updateNotification(updatedInfo)

            if (updatedInfo.status == DownloadStatus.COMPLETED ||
                updatedInfo.status == DownloadStatus.FAILED ||
                updatedInfo.status == DownloadStatus.CANCELLED
            ) {
                if (!downloadEngine.isDownloadActive(downloadId)) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        }
    }

    private fun updateNotification(downloadInfo: com.aria2.downloader.domain.model.DownloadInfo) {
        val notification = when (downloadInfo.status) {
            DownloadStatus.DOWNLOADING -> buildNotification(
                "Downloading: ${downloadInfo.fileName}",
                downloadInfo.progressPercent,
                downloadInfo.progress.formattedSpeed()
            )
            DownloadStatus.COMPLETED -> buildNotification(
                "Download completed: ${downloadInfo.fileName}"
            )
            DownloadStatus.FAILED -> buildNotification(
                "Download failed: ${downloadInfo.errorMessage ?: "Unknown error"}"
            )
            else -> buildNotification("Downloading: ${downloadInfo.fileName}")
        }
        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(
        title: String,
        progress: Int = 0,
        speedText: String = ""
    ): NotificationCompat.Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(speedText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(progress > 0)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        if (progress > 0) {
            builder.setProgress(100, progress, false)
        }

        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Download Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications for Aria2 Downloader"
                setShowBadge(true)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
