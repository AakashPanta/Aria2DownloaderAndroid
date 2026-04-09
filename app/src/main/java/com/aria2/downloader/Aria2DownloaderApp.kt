package com.aria2.downloader

import android.app.Application
import com.aria2.downloader.data.preferences.SettingsRepository
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@HiltAndroidApp
class Aria2DownloaderApp : Application() {

    @Inject lateinit var settingsRepository: SettingsRepository

    override fun onCreate() {
        super.onCreate()
        CoroutineScope(Dispatchers.Default).launch {
            settingsRepository.applyLauncherIcon(settingsRepository.currentSettings().appIcon)
        }
    }
}
