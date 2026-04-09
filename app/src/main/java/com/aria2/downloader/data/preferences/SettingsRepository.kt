package com.aria2.downloader.data.preferences

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.documentfile.provider.DocumentFile
import com.aria2.downloader.domain.model.AppIcon
import com.aria2.downloader.domain.model.AppSettings
import com.aria2.downloader.domain.model.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "aria2_settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val MAX_CONCURRENT_DOWNLOADS = intPreferencesKey("max_concurrent_downloads")
        val SPLIT = intPreferencesKey("split")
        val MAX_CONNECTION_PER_SERVER = intPreferencesKey("max_connection_per_server")
        val MIN_SPLIT_SIZE_MB = intPreferencesKey("min_split_size_mb")
        val ENABLE_DHT = booleanPreferencesKey("enable_dht")
        val PEER_EXCHANGE = booleanPreferencesKey("peer_exchange")
        val LOCAL_PEER_DISCOVERY = booleanPreferencesKey("local_peer_discovery")
        val REQUIRE_ENCRYPTION = booleanPreferencesKey("require_encryption")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val APP_ICON = stringPreferencesKey("app_icon")
        val RPC_TOKEN = stringPreferencesKey("rpc_token")
        val DOWNLOAD_LOCATION_URI = stringPreferencesKey("download_location_uri")
        val DOWNLOAD_LOCATION_LABEL = stringPreferencesKey("download_location_label")
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map(::mapSettings)

    suspend fun currentSettings(): AppSettings = settings.first()

    suspend fun updateThemeMode(mode: ThemeMode) = updateString(Keys.THEME_MODE, mode.name)
    suspend fun updateMaxConcurrentDownloads(value: Int) = updateInt(Keys.MAX_CONCURRENT_DOWNLOADS, value.coerceIn(1, 10))
    suspend fun updateSplit(value: Int) = updateInt(Keys.SPLIT, value.coerceIn(1, 16))
    suspend fun updateMaxConnectionPerServer(value: Int) = updateInt(Keys.MAX_CONNECTION_PER_SERVER, value.coerceIn(1, 16))
    suspend fun updateMinSplitSize(value: Int) = updateInt(Keys.MIN_SPLIT_SIZE_MB, value.coerceIn(1, 32))
    suspend fun updateEnableDht(enabled: Boolean) = updateBoolean(Keys.ENABLE_DHT, enabled)
    suspend fun updatePeerExchange(enabled: Boolean) = updateBoolean(Keys.PEER_EXCHANGE, enabled)
    suspend fun updateLocalPeerDiscovery(enabled: Boolean) = updateBoolean(Keys.LOCAL_PEER_DISCOVERY, enabled)
    suspend fun updateRequireEncryption(enabled: Boolean) = updateBoolean(Keys.REQUIRE_ENCRYPTION, enabled)
    suspend fun updateNotificationsEnabled(enabled: Boolean) = updateBoolean(Keys.NOTIFICATIONS_ENABLED, enabled)
    suspend fun updateRpcToken(token: String) = updateString(Keys.RPC_TOKEN, token.ifBlank { "aakash-aria2-rpc" })

    suspend fun updateAppIcon(icon: AppIcon) {
        updateString(Keys.APP_ICON, icon.name)
        applyLauncherIcon(icon)
    }

    suspend fun updateDownloadLocation(treeUri: Uri): Result<String> = runCatching {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        context.contentResolver.takePersistableUriPermission(treeUri, flags)

        val tree = DocumentFile.fromTreeUri(context, treeUri)
            ?: error("The selected folder could not be opened.")
        require(tree.canWrite()) { "The selected folder is not writable." }

        val probeName = ".aria2_probe_${System.currentTimeMillis()}"
        val probe = tree.createFile("application/octet-stream", probeName)
            ?: error("Could not create a test file in the selected folder.")
        probe.delete()

        val label = tree.name?.takeIf { it.isNotBlank() } ?: "Custom folder"
        context.settingsDataStore.edit {
            it[Keys.DOWNLOAD_LOCATION_URI] = treeUri.toString()
            it[Keys.DOWNLOAD_LOCATION_LABEL] = label
        }
        label
    }

    suspend fun clearDownloadLocation() {
        context.settingsDataStore.edit {
            it.remove(Keys.DOWNLOAD_LOCATION_URI)
            it.remove(Keys.DOWNLOAD_LOCATION_LABEL)
        }
    }

    fun applyLauncherIcon(icon: AppIcon) {
        val packageManager = context.packageManager
        val packageName = context.packageName
        val defaultComponent = ComponentName(packageName, "com.aria2.downloader.MainActivity")
        val accentComponent = ComponentName(packageName, "com.aria2.downloader.launcher.AccentLauncher")
        val darkComponent = ComponentName(packageName, "com.aria2.downloader.launcher.DarkLauncher")

        val enabled = when (icon) {
            AppIcon.DEFAULT -> defaultComponent
            AppIcon.ACCENT -> accentComponent
            AppIcon.DARK -> darkComponent
        }

        listOf(defaultComponent, accentComponent, darkComponent).forEach { component ->
            packageManager.setComponentEnabledSetting(
                component,
                if (component == enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        }
    }

    private fun mapSettings(preferences: Preferences): AppSettings = AppSettings(
        themeMode = enumValueOfOrNull(preferences[Keys.THEME_MODE], ThemeMode.SYSTEM),
        maxConcurrentDownloads = preferences[Keys.MAX_CONCURRENT_DOWNLOADS] ?: 3,
        split = preferences[Keys.SPLIT] ?: 8,
        maxConnectionPerServer = preferences[Keys.MAX_CONNECTION_PER_SERVER] ?: 8,
        minSplitSizeMb = preferences[Keys.MIN_SPLIT_SIZE_MB] ?: 4,
        enableDht = preferences[Keys.ENABLE_DHT] ?: true,
        peerExchange = preferences[Keys.PEER_EXCHANGE] ?: true,
        localPeerDiscovery = preferences[Keys.LOCAL_PEER_DISCOVERY] ?: true,
        requireEncryption = preferences[Keys.REQUIRE_ENCRYPTION] ?: false,
        notificationsEnabled = preferences[Keys.NOTIFICATIONS_ENABLED] ?: true,
        appIcon = enumValueOfOrNull(preferences[Keys.APP_ICON], AppIcon.DEFAULT),
        rpcToken = preferences[Keys.RPC_TOKEN] ?: "aakash-aria2-rpc",
        downloadLocationUri = preferences[Keys.DOWNLOAD_LOCATION_URI],
        downloadLocationLabel = preferences[Keys.DOWNLOAD_LOCATION_LABEL]
    )

    private suspend fun updateString(key: Preferences.Key<String>, value: String) {
        context.settingsDataStore.edit { it[key] = value }
    }

    private suspend fun updateInt(key: Preferences.Key<Int>, value: Int) {
        context.settingsDataStore.edit { it[key] = value }
    }

    private suspend fun updateBoolean(key: Preferences.Key<Boolean>, value: Boolean) {
        context.settingsDataStore.edit { it[key] = value }
    }

    private fun <T : Enum<T>> enumValueOfOrNull(value: String?, default: T): T =
        runCatching { java.lang.Enum.valueOf(default.declaringJavaClass, value ?: default.name) }
            .getOrDefault(default)
}
