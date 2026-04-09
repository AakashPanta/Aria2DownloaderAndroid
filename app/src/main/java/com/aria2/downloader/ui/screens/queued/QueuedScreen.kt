package com.aria2.downloader.ui.screens.queued

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.VerticalAlignTop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aria2.downloader.ui.components.DownloadCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueuedScreen(
    paddingTop: androidx.compose.ui.unit.Dp,
    viewModel: QueuedViewModel,
    onOpenDetail: (String) -> Unit
) {
    val downloads by viewModel.downloads.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("Queued downloads") }) }) { inner ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(top = paddingTop)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (downloads.isEmpty()) {
                item {
                    Text(
                        "Nothing is waiting in the queue. Lower the simultaneous-download limit in settings if you want more jobs to stay queued.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(downloads) { item ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        androidx.compose.foundation.layout.Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            DownloadCard(
                                download = item,
                                onClick = { onOpenDetail(item.id) },
                                onCancel = { viewModel.cancel(item) }
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                IconButton(onClick = { viewModel.moveTop(item) }) {
                                    Icon(Icons.Default.VerticalAlignTop, contentDescription = "Move to top")
                                }
                                IconButton(onClick = { viewModel.moveUp(item) }) {
                                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move up")
                                }
                                IconButton(onClick = { viewModel.moveDown(item) }) {
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move down")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
