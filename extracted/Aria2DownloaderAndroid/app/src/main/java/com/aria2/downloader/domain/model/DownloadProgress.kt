package com.aria2.downloader.domain.model

data class DownloadProgress(
    val downloadedBytes: Long = 0,
    val totalBytes: Long = 0,
    val progress: Float = 0f,
    val speedBytesPerSecond: Long = 0,
    val estimatedTimeRemainingMs: Long = 0
) {
    val progressPercent: Int
        get() = (progress * 100).toInt().coerceIn(0, 100)

    val etaSeconds: Long
        get() = estimatedTimeRemainingMs / 1000

    fun formattedSpeed(): String {
        return when {
            speedBytesPerSecond < 1024 -> "$speedBytesPerSecond B/s"
            speedBytesPerSecond < 1024 * 1024 -> "${speedBytesPerSecond / 1024} KB/s"
            speedBytesPerSecond < 1024 * 1024 * 1024 -> "${speedBytesPerSecond / (1024 * 1024)} MB/s"
            else -> "${speedBytesPerSecond / (1024 * 1024 * 1024)} GB/s"
        }
    }

    fun formattedTimeRemaining(): String {
        val seconds = (estimatedTimeRemainingMs / 1000).toInt()
        return when {
            seconds < 60 -> "${seconds}s"
            seconds < 3600 -> "${seconds / 60}m ${seconds % 60}s"
            else -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
        }
    }
}
