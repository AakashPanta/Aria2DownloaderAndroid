package com.aria2.downloader.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aria2.downloader.data.repository.DownloadRepository
import com.aria2.downloader.domain.model.DownloadInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val downloadRepository: DownloadRepository
) : ViewModel() {

    val activeDownloads = downloadRepository.getActiveDownloads()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    fun removeDownload(downloadId: String) {
        viewModelScope.launch {
            downloadRepository.deleteDownload(downloadId)
        }
    }

    fun clearCompleted() {
        viewModelScope.launch {
            downloadRepository.clearCompleted()
        }
    }
}
