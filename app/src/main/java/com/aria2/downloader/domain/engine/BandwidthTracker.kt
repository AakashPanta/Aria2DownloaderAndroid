package com.aria2.downloader.domain.engine

class BandwidthTracker {
    private var lastUpdateTime = System.currentTimeMillis()
    private var lastDownloadedBytes = 0L
    private var currentSpeedBytesPerSecond = 0L
    private val speedWindowMs = 2000L // 2-second window for speed calculation

    fun updateProgress(downloadedBytes: Long) {
        val currentTime = System.currentTimeMillis()
        val elapsedMs = currentTime - lastUpdateTime

        if (elapsedMs >= speedWindowMs) {
            val bytesDelta = downloadedBytes - lastDownloadedBytes
            currentSpeedBytesPerSecond = if (elapsedMs > 0) {
                (bytesDelta * 1000) / elapsedMs
            } else {
                0L
            }
            lastDownloadedBytes = downloadedBytes
            lastUpdateTime = currentTime
        }
    }

    fun getCurrentSpeed(): Long = currentSpeedBytesPerSecond

    fun estimateTimeRemaining(remainingBytes: Long): Long {
        return if (currentSpeedBytesPerSecond > 0) {
            (remainingBytes * 1000) / currentSpeedBytesPerSecond
        } else {
            0L
        }
    }

    fun reset() {
        lastUpdateTime = System.currentTimeMillis()
        lastDownloadedBytes = 0L
        currentSpeedBytesPerSecond = 0L
    }
}
