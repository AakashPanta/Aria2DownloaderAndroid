package com.aria2.downloader.ui.screens.newdownload

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aria2.downloader.domain.model.DownloadSourceType

@Composable
fun NewDownloadScreen(
    viewModel: NewDownloadViewModel,
    onNavigateBack: () -> Unit
) {
    val input by viewModel.input.collectAsState()
    val selectedFiles by viewModel.selectedFiles.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedMode by rememberSaveable { mutableStateOf(0) }
    var pendingTorrentUri by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingMetalinkUri by rememberSaveable { mutableStateOf<String?>(null) }

    val torrentPicker = rememberLauncherForActivityResult(OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            pendingTorrentUri = uri.toString()
            viewModel.submitImported(uri, DownloadSourceType.TORRENT)
        }
    }

    val metalinkPicker = rememberLauncherForActivityResult(OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            pendingMetalinkUri = uri.toString()
            viewModel.submitImported(uri, DownloadSourceType.METALINK)
        }
    }

    LaunchedEffect(uiState.errorMessage, uiState.successMessage) {
        uiState.errorMessage?.let { snackbarHostState.showSnackbar(it) }
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("New download") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                "The request now goes through the same aria2 pipeline that performs the actual download, so valid links no longer get trapped in a separate validation dead-end.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                listOf("Link / magnet", "Torrent file", "Metalink").forEachIndexed { index, label ->
                    SegmentedButton(
                        selected = selectedMode == index,
                        onClick = { selectedMode = index },
                        shape = androidx.compose.material3.SegmentedButtonDefaults.itemShape(index, 3)
                    ) {
                        Text(label)
                    }
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    when (selectedMode) {
                        0 -> {
                            OutlinedTextField(
                                value = input,
                                onValueChange = viewModel::updateInput,
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 4,
                                label = { Text("Direct link, magnet, torrent URL or metalink URL") }
                            )
                            OutlinedTextField(
                                value = selectedFiles,
                                onValueChange = viewModel::updateSelectedFiles,
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Selective files (optional, e.g. 1-3,5)") }
                            )
                            Button(
                                onClick = viewModel::submitLink,
                                enabled = !uiState.isBusy,
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                if (uiState.isBusy) {
                                    CircularProgressIndicator(modifier = Modifier.height(20.dp))
                                } else {
                                    Text("Queue in aria2")
                                }
                            }
                        }
                        1 -> {
                            Text("Choose a .torrent file. If you fill the selective-files box, aria2 will use it as select-file.")
                            OutlinedTextField(
                                value = selectedFiles,
                                onValueChange = viewModel::updateSelectedFiles,
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Selective files (optional)") }
                            )
                            Button(
                                onClick = { torrentPicker.launch(arrayOf("application/x-bittorrent", "*/*")) },
                                enabled = !uiState.isBusy,
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Text("Pick .torrent file")
                            }
                            pendingTorrentUri?.let {
                                Text("Last selected: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        else -> {
                            Text("Choose a .meta4 or .metalink file to enqueue all contained resources through aria2.")
                            OutlinedTextField(
                                value = selectedFiles,
                                onValueChange = viewModel::updateSelectedFiles,
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Selective files (optional)") }
                            )
                            Button(
                                onClick = { metalinkPicker.launch(arrayOf("application/xml", "text/xml", "*/*")) },
                                enabled = !uiState.isBusy,
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Text("Pick metalink file")
                            }
                            pendingMetalinkUri?.let {
                                Text("Last selected: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}
