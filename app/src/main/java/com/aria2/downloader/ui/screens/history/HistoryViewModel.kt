package com.aria2.downloader.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aria2.downloader.data.repository.DownloadRepository
import com.aria2.downloader.domain.model.DownloadInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: DownloadRepository
) : ViewModel() {
    val downloads = repository.observeFinished()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun clearCompleted() = viewModelScope.launch { repository.clearCompleted() }
    fun delete(download: DownloadInfo) = viewModelScope.launch { repository.deleteById(download.id) }
}
