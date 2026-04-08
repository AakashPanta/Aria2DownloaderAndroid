package com.aria2.downloader.ui.screens.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aria2.downloader.domain.model.DownloadStatus
import com.aria2.downloader.ui.components.DownloadProgressBar
import com.aria2.downloader.ui.components.FileInfo
import com.aria2.downloader.ui.components.SpeedAndETA

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    viewModel: DetailViewModel,
    downloadId: String,
    onNavigateBack: () -> Unit
) {
    val downloadInfo by viewModel.downloadInfo.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Download Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (downloadInfo == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                downloadInfo?.let { info ->
                    // File Info
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            FileInfo(
                                fileName = info.fileName,
                                downloadedSize = com.aria2.downloader.domain.model.DownloadInfo.formatBytes(info.downloadedBytes),
                                totalSize = info.formattedFileSize()
                            )

                            Divider()

                            DetailRow("URL", info.url)
                            DetailRow("Status", info.status.name)
                            DetailRow(
                                "Resume Support",
                                if (info.supportsResume) "Supported" else "Not Supported"
                            )
                        }
                    }

                    // Progress Section
                    if (info.isActive || info.status == DownloadStatus.PAUSED) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                DownloadProgressBar(
                                    progress = info.progressPercent / 100f,
                                    label = "${info.progressPercent}%"
                                )

                                SpeedAndETA(
                                    speedText = info.progress.formattedSpeed(),
                                    etaText = if (info.status == DownloadStatus.DOWNLOADING) {
                                        info.progress.formattedTimeRemaining()
                                    } else {
                                        "---"
                                    }
                                )
                            }
                        }
                    }

                    // Error Message
                    if (info.isFailed && info.errorMessage != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "Error",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = info.errorMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Action Buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        when (info.status) {
                            DownloadStatus.DOWNLOADING -> {
                                Button(
                                    onClick = { viewModel.pauseDownload() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    colors = ButtonDefaults.outlinedButtonColors()
                                ) {
                                    Text("Pause")
                                }

                                Button(
                                    onClick = { viewModel.cancelDownload() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    colors = ButtonDefaults.outlinedButtonColors()
                                ) {
                                    Text("Cancel")
                                }
                            }
                            DownloadStatus.PAUSED -> {
                                Button(
                                    onClick = { viewModel.resumeDownload() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                ) {
                                    Text("Resume")
                                }

                                Button(
                                    onClick = { viewModel.cancelDownload() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    colors = ButtonDefaults.outlinedButtonColors()
                                ) {
                                    Text("Cancel")
                                }
                            }
                            DownloadStatus.FAILED -> {
                                Button(
                                    onClick = { viewModel.retryDownload() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                ) {
                                    Text("Retry Download")
                                }
                            }
                            else -> {}
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
