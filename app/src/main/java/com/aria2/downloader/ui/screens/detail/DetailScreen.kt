package com.aria2.downloader.ui.screens.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aria2.downloader.domain.model.DownloadStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    viewModel: DetailViewModel,
    onNavigateBack: () -> Unit
) {
    val download by viewModel.download.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(download?.fileName ?: "Download details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { inner ->
        val item = download
        if (item == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inner)
                    .padding(24.dp)
            ) {
                Text("Loading…")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inner)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(item.fileName, style = MaterialTheme.typography.titleLarge)
                        Text(
                            item.source,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LinearProgressIndicator(
                            progress = { item.progress.fraction },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("${item.progressPercent}% • ${item.formattedCompletedSize()} / ${item.formattedTotalSize()}")
                    }
                }

                DetailPair("Status", item.formattedStatus())
                DetailPair("Connections", item.formattedConnections())
                DetailPair("Speed", item.progress.formattedSpeed())
                DetailPair("ETA", item.progress.formattedEta())
                DetailPair("Source type", item.sourceType.name)
                DetailPair("Destination", item.destinationDir)
                item.infoHash?.let { DetailPair("Info hash", it) }
                item.errorMessage?.let { DetailPair("Error", it) }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (item.status) {
                        DownloadStatus.DOWNLOADING,
                        DownloadStatus.METADATA,
                        DownloadStatus.QUEUED,
                        DownloadStatus.VALIDATING -> {
                            OutlinedButton(
                                onClick = { viewModel.pause(item) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Pause")
                            }
                            OutlinedButton(
                                onClick = { viewModel.cancel(item) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancel")
                            }
                        }

                        DownloadStatus.PAUSED -> {
                            Button(
                                onClick = { viewModel.resume(item) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Resume")
                            }
                            OutlinedButton(
                                onClick = { viewModel.cancel(item) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancel")
                            }
                        }

                        DownloadStatus.FAILED -> {
                            Button(
                                onClick = { viewModel.retry(item) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Retry")
                            }
                            OutlinedButton(
                                onClick = { viewModel.cancel(item) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Dismiss")
                            }
                        }

                        else -> Unit
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailPair(label: String, value: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(value, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
