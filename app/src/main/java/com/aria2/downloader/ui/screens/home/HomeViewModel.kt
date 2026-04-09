package com.aria2.downloader.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aria2.downloader.data.repository.DownloadRepository
import com.aria2.downloader.domain.model.DownloadInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class DashboardState(
    val all: List<DownloadInfo> = emptyList(),
    val active: List<DownloadInfo> = emptyList(),
    val queued: List<DownloadInfo> = emptyList(),
    val completed: List<DownloadInfo> = emptyList(),
    val totalSpeedBytes: Long = 0L
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    downloadRepository: DownloadRepository
) : ViewModel() {

    val dashboardState = combine(
        downloadRepository.observeAll(),
        downloadRepository.observeRunning(),
        downloadRepository.observeQueued(),
        downloadRepository.observeCompleted()
    ) { all, active, queued, completed ->
        DashboardState(
            all = all,
            active = active,
            queued = queued,
            completed = completed,
            totalSpeedBytes = active.sumOf { it.downloadSpeedBytes }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardState())
}
