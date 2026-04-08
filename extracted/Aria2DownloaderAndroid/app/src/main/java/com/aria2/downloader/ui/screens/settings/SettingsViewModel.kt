package com.aria2.downloader.ui.screens.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val preferences: SharedPreferences =
        context.getSharedPreferences("aria2_settings", Context.MODE_PRIVATE)

    private val _maxConnections = MutableStateFlow(
        preferences.getInt("max_connections", 4)
    )
    val maxConnections = _maxConnections.asStateFlow()

    private val _darkModeEnabled = MutableStateFlow(
        preferences.getBoolean("dark_mode", false)
    )
    val darkModeEnabled = _darkModeEnabled.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(
        preferences.getBoolean("notifications_enabled", true)
    )
    val notificationsEnabled = _notificationsEnabled.asStateFlow()

    fun updateMaxConnections(value: Int) {
        _maxConnections.value = value
        preferences.edit().putInt("max_connections", value).apply()
    }

    fun updateDarkMode(enabled: Boolean) {
        _darkModeEnabled.value = enabled
        preferences.edit().putBoolean("dark_mode", enabled).apply()
    }

    fun updateNotifications(enabled: Boolean) {
        _notificationsEnabled.value = enabled
        preferences.edit().putBoolean("notifications_enabled", enabled).apply()
    }
}
