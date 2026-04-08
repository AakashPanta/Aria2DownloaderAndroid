package com.aria2.downloader.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class DownloadStatus {
    PENDING,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}
