package com.aria2.downloader.domain.model

enum class DownloadStatus {
    QUEUED,
    VALIDATING,
    METADATA,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}
