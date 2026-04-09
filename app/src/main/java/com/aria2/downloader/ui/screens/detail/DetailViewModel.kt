package com.aria2.downloader.ui.screens.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aria2.downloader.data.repository.DownloadRepository
import com.aria2.downloader.domain.engine.DownloadEngine
import com.aria2.downloader.domain.model.DownloadInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: DownloadRepository,
    private val engine: DownloadEngine
) : ViewModel() {

    private val downloadId = savedStateHandle.getStateFlow<String?>("downloadId", null)

    val download = downloadId
        .filterNotNull()
        .flatMapLatest { id ->
            repository.observeAll().map { list -> list.firstOrNull { it.id == id } }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun pause(download: DownloadInfo) = viewModelScope.launch { engine.pause(download.id) }
    fun resume(download: DownloadInfo) = viewModelScope.launch { engine.resume(download.id) }
    fun cancel(download: DownloadInfo) = viewModelScope.launch { engine.cancel(download.id) }
    fun retry(download: DownloadInfo) = viewModelScope.launch { engine.retry(download.id) }
    fun delete(download: DownloadInfo) = viewModelScope.launch { engine.delete(download.id) }
}
