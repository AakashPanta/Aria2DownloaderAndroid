package com.aria2.downloader.ui.screens.queued

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
class QueuedViewModel @Inject constructor(
    private val repository: DownloadRepository,
    private val engine: DownloadEngine
) : ViewModel() {
    val downloads = repository.observeQueued()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun moveUp(download: DownloadInfo) = viewModelScope.launch { engine.moveQueue(download.id, -1) }
    fun moveDown(download: DownloadInfo) = viewModelScope.launch { engine.moveQueue(download.id, 1) }
    fun moveTop(download: DownloadInfo) = viewModelScope.launch { engine.moveQueueToTop(download.id) }
    fun cancel(download: DownloadInfo) = viewModelScope.launch { engine.cancel(download.id) }
}
