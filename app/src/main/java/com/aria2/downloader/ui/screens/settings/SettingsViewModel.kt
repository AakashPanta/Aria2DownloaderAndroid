package com.aria2.downloader.ui.screens.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aria2.downloader.data.preferences.SettingsRepository
import com.aria2.downloader.domain.engine.DownloadEngine
import com.aria2.downloader.domain.model.AppIcon
import com.aria2.downloader.domain.model.AppSettings
import com.aria2.downloader.domain.model.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val downloadEngine: DownloadEngine
) : ViewModel() {

    val settings = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun updateTheme(mode: ThemeMode) = viewModelScope.launch { settingsRepository.updateThemeMode(mode) }

    fun updateConcurrent(value: Int) = viewModelScope.launch {
        settingsRepository.updateMaxConcurrentDownloads(value)
        runCatching { downloadEngine.applyGlobalSettings() }
    }

    fun updateSplit(value: Int) = viewModelScope.launch {
        settingsRepository.updateSplit(value)
        runCatching { downloadEngine.applyGlobalSettings() }
    }

    fun updateConnections(value: Int) = viewModelScope.launch {
        settingsRepository.updateMaxConnectionPerServer(value)
        runCatching { downloadEngine.applyGlobalSettings() }
    }

    fun updateMinSplit(value: Int) = viewModelScope.launch {
        settingsRepository.updateMinSplitSize(value)
        runCatching { downloadEngine.applyGlobalSettings() }
    }

    fun updateDht(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.updateEnableDht(enabled)
        runCatching { downloadEngine.applyGlobalSettings() }
    }

    fun updatePex(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.updatePeerExchange(enabled)
        runCatching { downloadEngine.applyGlobalSettings() }
    }

    fun updateLpd(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.updateLocalPeerDiscovery(enabled)
        runCatching { downloadEngine.applyGlobalSettings() }
    }

    fun updateEncryption(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.updateRequireEncryption(enabled)
        runCatching { downloadEngine.applyGlobalSettings() }
    }

    fun updateNotifications(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.updateNotificationsEnabled(enabled)
    }

    fun updateIcon(icon: AppIcon) = viewModelScope.launch {
        settingsRepository.updateAppIcon(icon)
    }

    fun updateDownloadLocation(uri: Uri) = viewModelScope.launch {
        _message.value = settingsRepository.updateDownloadLocation(uri)
            .fold(
                onSuccess = { "Download location set to $it" },
                onFailure = { it.message ?: "Could not use the selected folder." }
            )
    }

    fun useDefaultDownloadLocation() = viewModelScope.launch {
        settingsRepository.clearDownloadLocation()
        _message.value = "Download location set to /storage/emulated/0/Download"
    }

    fun consumeMessage() {
        _message.value = null
    }
}
