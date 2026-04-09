package com.aria2.downloader.ui.screens.active

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aria2.downloader.domain.model.DownloadStatus
import com.aria2.downloader.ui.components.DownloadCard

@Composable
fun ActiveScreen(
    paddingTop: androidx.compose.ui.unit.Dp,
    viewModel: ActiveViewModel,
    onNewDownload: () -> Unit,
    onOpenDetail: (String) -> Unit
) {
    val downloads by viewModel.downloads.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Active downloads") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewDownload) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { inner ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(top = paddingTop)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }
            if (downloads.isEmpty()) {
                item {
                    Text(
                        "Nothing is running. Add a new job to start aria2 in the foreground service.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(downloads) { item ->
                    DownloadCard(
                        download = item,
                        onClick = { onOpenDetail(item.id) },
                        onPause = if (item.status != DownloadStatus.PAUSED) ({ viewModel.pause(item) }) else null,
                        onResume = if (item.status == DownloadStatus.PAUSED) ({ viewModel.resume(item) }) else null,
                        onCancel = { viewModel.cancel(item) },
                        onRetry = { viewModel.retry(item) }
                    )
                }
            }
        }
    }
}
