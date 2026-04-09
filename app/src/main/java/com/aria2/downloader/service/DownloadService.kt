package com.aria2.downloader.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.aria2.downloader.MainActivity
import com.aria2.downloader.R
import com.aria2.downloader.data.preferences.SettingsRepository
import com.aria2.downloader.domain.engine.Aria2ProcessManager
import com.aria2.downloader.domain.engine.Aria2Runtime
import com.aria2.downloader.domain.engine.DownloadEngine
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DownloadService : LifecycleService() {

    @Inject lateinit var downloadEngine: DownloadEngine
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var processManager: Aria2ProcessManager

    private var monitorJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(
            NOTIFICATION_ID,
            buildNotification("Starting aria2 engine…", "Preparing background sync")
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (monitorJob?.isActive != true) {
            monitorJob = lifecycleScope.launch {
                val startup = runCatching { processManager.ensureRunning() }
                if (startup.isFailure) {
                    startForeground(
                        NOTIFICATION_ID,
                        buildNotification(
                            title = "aria2 startup failed",
                            text = startup.exceptionOrNull()?.message ?: "Unknown startup error"
                        )
                    )
                    delay(5000)
                    stopSelf()
                    return@launch
                }

                while (isActive) {
                    val settings = settingsRepository.currentSettings()
                    val snapshot = runCatching { downloadEngine.syncNow() }.getOrNull()

                    if (snapshot != null && settings.notificationsEnabled) {
                        val notification = if (snapshot.active.isNotEmpty()) {
                            val top = snapshot.active.first()
                            buildNotification(
                                title = "Downloading ${snapshot.active.size} item(s)",
                                text = "${top.fileName} • ${top.progressPercent}% • ${top.progress.formattedSpeed()}"
                            )
                        } else {
                            buildNotification(
                                title = "Download engine idle",
                                text = "aria2 RPC on port ${Aria2Runtime.rpcPort} is ready for new jobs."
                            )
                        }
                        startForeground(NOTIFICATION_ID, notification)
                    }

                    if (snapshot != null && snapshot.active.isEmpty()) {
                        delay(5000)
                        val fresh = runCatching { downloadEngine.syncNow() }.getOrNull()
                        if (fresh != null && fresh.active.isEmpty()) {
                            stopForeground(STOP_FOREGROUND_REMOVE)
                            stopSelf()
                            break
                        }
                    }
                    delay(POLL_INTERVAL_MS)
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        monitorJob?.cancel()
        super.onDestroy()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.download_service_channel),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.download_service_description)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(title: String, text: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pending = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pending)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "aria2_service"
        private const val NOTIFICATION_ID = 4201
        private const val POLL_INTERVAL_MS = 1200L

        fun createIntent(context: Context): Intent = Intent(context, DownloadService::class.java)
    }
}
