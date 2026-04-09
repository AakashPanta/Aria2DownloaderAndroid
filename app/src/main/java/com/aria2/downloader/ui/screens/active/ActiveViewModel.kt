package com.aria2.downloader.ui.screens.active

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aria2.downloader.data.repository.DownloadRepository
import com.aria2.downloader.domain.engine.DownloadEngine
import com.aria2.downloader.domain.model.DownloadInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ActiveViewModel @Inject constructor(
    private val downloadRepository: DownloadRepository,
    private val downloadEngine: DownloadEngine
) : ViewModel() {

    val downloads = downloadRepository.observeRunning()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun pause(download: DownloadInfo) = viewModelScope.launch { downloadEngine.pause(download.id) }
    fun resume(download: DownloadInfo) = viewModelScope.launch { downloadEngine.resume(download.id) }
    fun cancel(download: DownloadInfo) = viewModelScope.launch { downloadEngine.cancel(download.id) }
    fun retry(download: DownloadInfo) = viewModelScope.launch { downloadEngine.retry(download.id) }
}
