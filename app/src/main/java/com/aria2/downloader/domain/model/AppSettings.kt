package com.aria2.downloader.domain.model

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val maxConcurrentDownloads: Int = 3,
    val split: Int = 8,
    val maxConnectionPerServer: Int = 8,
    val minSplitSizeMb: Int = 4,
    val enableDht: Boolean = true,
    val peerExchange: Boolean = true,
    val localPeerDiscovery: Boolean = true,
    val requireEncryption: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val appIcon: AppIcon = AppIcon.DEFAULT,
    val rpcToken: String = "aakash-aria2-rpc",
    val downloadLocationPath: String = "/storage/emulated/0/Download",
    val downloadLocationUri: String? = null,
    val downloadLocationLabel: String? = null
)
