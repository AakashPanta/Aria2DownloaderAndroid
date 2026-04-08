package com.aria2.downloader.ui.screens.newdownload

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aria2.downloader.data.repository.DownloadRepository
import com.aria2.downloader.domain.engine.DownloadEngine
import com.aria2.downloader.domain.engine.URLValidator
import com.aria2.downloader.domain.model.DownloadInfo
import com.aria2.downloader.domain.model.DownloadStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class NewDownloadViewModel @Inject constructor(
    private val downloadRepository: DownloadRepository,
    private val downloadEngine: DownloadEngine
) : ViewModel() {

    private val _urlState = MutableStateFlow("")
    val urlState = _urlState.asStateFlow()

    private val _validationState = MutableStateFlow<ValidationState>(ValidationState.Idle)
    val validationState = _validationState.asStateFlow()

    private val _urlInfo = MutableStateFlow<URLValidator.URLInfo?>(null)
    val urlInfo = _urlInfo.asStateFlow()

    private val _isValidating = MutableStateFlow(false)
    val isValidating = _isValidating.asStateFlow()

    fun updateUrl(url: String) {
        _urlState.value = url
        _validationState.value = ValidationState.Idle
        _urlInfo.value = null
    }

    fun validateAndStartDownload() {
        val url = _urlState.value.trim()
        if (url.isEmpty()) {
            _validationState.value = ValidationState.Error("Please enter a URL")
            return
        }

        viewModelScope.launch {
            _isValidating.value = true
            _validationState.value = ValidationState.Validating

            val urlValidator = URLValidator()
            if (!urlValidator.isValidUrl(url)) {
                _validationState.value = ValidationState.Error("Invalid URL format")
                _isValidating.value = false
                return@launch
            }

            val info = urlValidator.validateAndGetInfo(url)
            if (info == null) {
                _validationState.value = ValidationState.Error("Unable to reach the server. Check the URL and try again.")
                _isValidating.value = false
                return@launch
            }

            _urlInfo.value = info
            startDownload(info)
            _isValidating.value = false
            _validationState.value = ValidationState.Success
        }
    }

    private suspend fun startDownload(urlInfo: URLValidator.URLInfo) {
        val downloadId = UUID.randomUUID().toString()
        val downloadInfo = DownloadInfo(
            id = downloadId,
            url = urlInfo.url,
            fileName = urlInfo.fileName,
            fileSize = urlInfo.fileSize,
            status = DownloadStatus.PENDING,
            supportsResume = urlInfo.supportsResume
        )

        downloadRepository.insertDownload(downloadInfo)
        downloadEngine.startDownload(downloadInfo) { updated ->
            viewModelScope.launch {
                downloadRepository.updateDownload(updated)
            }
        }
    }

    sealed class ValidationState {
        object Idle : ValidationState()
        object Validating : ValidationState()
        object Success : ValidationState()
        data class Error(val message: String) : ValidationState()
    }
}
