package com.aria2.downloader.domain.model

import kotlin.math.roundToInt

data class DownloadProgress(
    val completedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val downloadSpeedBytes: Long = 0L,
    val uploadSpeedBytes: Long = 0L,
    val connections: Int = 0
) {
    val fraction: Float
        get() = if (totalBytes > 0) {
            (completedBytes.toDouble() / totalBytes.toDouble()).toFloat().coerceIn(0f, 1f)
        } else {
            0f
        }

    val percent: Int
        get() = (fraction * 100f).roundToInt().coerceIn(0, 100)

    val etaSeconds: Long?
        get() {
            val remaining = totalBytes - completedBytes
            return if (downloadSpeedBytes > 0 && remaining > 0) remaining / downloadSpeedBytes else null
        }

    fun formattedEta(): String {
        val seconds = etaSeconds ?: return "—"
        return when {
            seconds < 60 -> "${seconds}s"
            seconds < 3600 -> "${seconds / 60}m ${seconds % 60}s"
            else -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
        }
    }

    fun formattedSpeed(): String = formatBytes(downloadSpeedBytes) + "/s"

    companion object {
        fun formatBytes(bytes: Long): String {
            val positive = bytes.coerceAtLeast(0)
            return when {
                positive < 1024L -> "$positive B"
                positive < 1024L * 1024L -> String.format("%.1f KB", positive / 1024.0)
                positive < 1024L * 1024L * 1024L -> String.format("%.1f MB", positive / 1024.0 / 1024.0)
                else -> String.format("%.1f GB", positive / 1024.0 / 1024.0 / 1024.0)
            }
        }
    }
}
