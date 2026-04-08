package com.aria2.downloader.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aria2.downloader.data.repository.DownloadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val downloadRepository: DownloadRepository
) : ViewModel() {

    val completedDownloads = downloadRepository.getCompletedDownloads()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    val failedDownloads = downloadRepository.getFailedDownloads()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    fun deleteDownload(downloadId: String) {
        viewModelScope.launch {
            downloadRepository.deleteDownload(downloadId)
        }
    }

    fun clearAllCompleted() {
        viewModelScope.launch {
            downloadRepository.clearCompleted()
        }
    }
}
