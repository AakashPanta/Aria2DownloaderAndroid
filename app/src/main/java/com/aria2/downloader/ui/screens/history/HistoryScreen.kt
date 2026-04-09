package com.aria2.downloader.ui.screens.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
fun HistoryScreen(
    paddingTop: androidx.compose.ui.unit.Dp,
    viewModel: HistoryViewModel,
    onOpenDetail: (String) -> Unit
) {
    val downloads by viewModel.downloads.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Completed & failed") }) }
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
            item {
                Spacer(modifier = Modifier.height(4.dp))
                if (downloads.any { it.status.name == "COMPLETED" }) {
                    Button(onClick = { viewModel.clearCompleted() }) {
                        Text("Clear completed")
                    }
                }
            }

            if (downloads.isEmpty()) {
                item {
                    Text(
                        "Completed jobs and failures will show up here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(downloads) { item ->
                    DownloadCard(
                        download = item,
                        onClick = { onOpenDetail(item.id) },
                        onCancel = { viewModel.delete(item) }
                    )
                }
            }
        }
    }
}
