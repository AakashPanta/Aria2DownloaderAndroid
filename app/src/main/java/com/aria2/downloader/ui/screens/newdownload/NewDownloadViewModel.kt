package com.aria2.downloader.ui.screens.newdownload

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aria2.downloader.domain.engine.DownloadEngine
import com.aria2.downloader.domain.model.DownloadSourceType
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class NewDownloadViewModel @Inject constructor(
    private val downloadEngine: DownloadEngine
) : ViewModel() {

    private val _input = MutableStateFlow("")
    val input = _input.asStateFlow()

    private val _selectedFiles = MutableStateFlow("")
    val selectedFiles = _selectedFiles.asStateFlow()

    private val _uiState = MutableStateFlow(NewDownloadUiState())
    val uiState = _uiState.asStateFlow()

    fun updateInput(value: String) {
        _input.value = value
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }

    fun updateSelectedFiles(value: String) {
        _selectedFiles.value = value
    }

    fun submitLink() {
        val raw = input.value.trim()
        if (raw.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Paste a direct link, magnet, torrent URL or metalink URL.")
            return
        }
        enqueue { downloadEngine.enqueueLink(raw, selectedFiles.value.ifBlank { null }) }
    }

    fun submitImported(uri: Uri, sourceType: DownloadSourceType) {
        enqueue { downloadEngine.enqueueImportedDocument(uri, sourceType, selectedFiles.value.ifBlank { null }) }
    }

    private fun enqueue(block: suspend () -> Result<*>) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBusy = true, errorMessage = null, successMessage = null)
            val result = runCatching { block().getOrThrow() }
            _uiState.value = if (result.isSuccess) {
                NewDownloadUiState(
                    isBusy = false,
                    successMessage = "Queued successfully"
                )
            } else {
                NewDownloadUiState(
                    isBusy = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to enqueue the download."
                )
            }
        }
    }
}

data class NewDownloadUiState(
    val isBusy: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)
