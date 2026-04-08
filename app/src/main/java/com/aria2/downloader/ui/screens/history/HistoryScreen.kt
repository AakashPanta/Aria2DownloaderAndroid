package com.aria2.downloader.ui.screens.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aria2.downloader.ui.components.DownloadCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onNavigateBack: () -> Unit,
    onDownloadClicked: (String) -> Unit
) {
    val completedDownloads by viewModel.completedDownloads.collectAsState()
    val failedDownloads by viewModel.failedDownloads.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Download History") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (completedDownloads.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearAllCompleted() }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Clear completed")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (completedDownloads.isEmpty() && failedDownloads.isEmpty()) {
            EmptyHistoryView(
                modifier = Modifier.padding(paddingValues)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Completed Downloads
                if (completedDownloads.isNotEmpty()) {
                    item {
                        Text(
                            text = "Completed (${completedDownloads.size})",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    items(completedDownloads) { download ->
                        DownloadCard(
                            downloadInfo = download,
                            onClick = { onDownloadClicked(download.id) },
                            onPause = {},
                            onResume = {},
                            onCancel = { viewModel.deleteDownload(download.id) },
                            onRetry = {}
                        )
                    }
                }

                // Failed Downloads
                if (failedDownloads.isNotEmpty()) {
                    item {
                        Text(
                            text = "Failed (${failedDownloads.size})",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    items(failedDownloads) { download ->
                        DownloadCard(
                            downloadInfo = download,
                            onClick = { onDownloadClicked(download.id) },
                            onPause = {},
                            onResume = {},
                            onCancel = { viewModel.deleteDownload(download.id) },
                            onRetry = { viewModel.deleteDownload(download.id) }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun EmptyHistoryView(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Filled.Delete,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No download history",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Your download history will appear here",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
