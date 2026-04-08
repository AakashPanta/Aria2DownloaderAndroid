package com.aria2.downloader.ui.screens.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aria2.downloader.data.repository.DownloadRepository
import com.aria2.downloader.domain.engine.DownloadEngine
import com.aria2.downloader.domain.model.DownloadInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val downloadRepository: DownloadRepository,
    private val downloadEngine: DownloadEngine,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val downloadId: String = savedStateHandle.get<String>("downloadId") ?: ""

    private val _downloadInfo = MutableStateFlow<DownloadInfo?>(null)
    val downloadInfo = _downloadInfo.asStateFlow()

    init {
        loadDownloadInfo()
    }

    private fun loadDownloadInfo() {
        viewModelScope.launch {
            val info = downloadRepository.getDownloadById(downloadId)
            _downloadInfo.value = info
        }
    }

    fun pauseDownload() {
        viewModelScope.launch {
            downloadEngine.pauseDownload(downloadId)
        }
    }

    fun resumeDownload() {
        viewModelScope.launch {
            _downloadInfo.value?.let {
                downloadEngine.startDownload(it) { updated ->
                    viewModelScope.launch {
                        downloadRepository.updateDownload(updated)
                        _downloadInfo.value = updated
                    }
                }
            }
        }
    }

    fun cancelDownload() {
        viewModelScope.launch {
            downloadEngine.cancelDownload(downloadId)
            loadDownloadInfo()
        }
    }

    fun retryDownload() {
        viewModelScope.launch {
            _downloadInfo.value?.let {
                downloadEngine.startDownload(it) { updated ->
                    viewModelScope.launch {
                        downloadRepository.updateDownload(updated)
                        _downloadInfo.value = updated
                    }
                }
            }
        }
    }
}
