package com.aria2.downloader.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aria2.downloader.domain.model.DownloadProgress
import com.aria2.downloader.ui.components.DownloadCard
import com.aria2.downloader.ui.components.MetricCard

@Composable
fun HomeScreen(
    paddingTop: androidx.compose.ui.unit.Dp,
    viewModel: HomeViewModel,
    onNewDownload: () -> Unit,
    onOpenActive: () -> Unit,
    onOpenCompleted: () -> Unit,
    onOpenDetail: (String) -> Unit
) {
    val state by viewModel.dashboardState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Aria2 Downloader") })
        },
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Fast direct links, torrents and metalinks powered by aria2 RPC.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        title = "Active",
                        value = state.active.size.toString(),
                        subtitle = "Live queue",
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Completed",
                        value = state.completed.size.toString(),
                        subtitle = "Finished jobs",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            item {
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        title = "Speed",
                        value = DownloadProgress.formatBytes(state.totalSpeedBytes) + "/s",
                        subtitle = "Combined throughput",
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Library",
                        value = state.all.size.toString(),
                        subtitle = "Tracked downloads",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            item {
                androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onOpenActive, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Text(" Active")
                    }
                    Button(onClick = onOpenCompleted, modifier = Modifier.weight(1f)) {
                        Text("Completed")
                    }
                }
            }
            if (state.active.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("No active downloads right now.", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "The old “validating URL” dead-end is gone. Paste a direct link, magnet, torrent or metalink and the request goes straight into aria2.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(onClick = onNewDownload) {
                            Text("Add a download")
                        }
                    }
                }
            } else {
                item {
                    Text("Live queue", style = MaterialTheme.typography.titleLarge)
                }
                items(state.active.take(3)) { item ->
                    DownloadCard(
                        download = item,
                        onClick = { onOpenDetail(item.id) }
                    )
                }
            }
        }
    }
}
