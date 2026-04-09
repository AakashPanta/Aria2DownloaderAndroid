package com.aria2.downloader.ui.screens.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AboutScreen(paddingTop: androidx.compose.ui.unit.Dp) {
    Scaffold(topBar = { TopAppBar(title = { Text("About") }) }) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(top = paddingTop)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Aria2 Downloader", style = MaterialTheme.typography.headlineSmall)
                    Text("Version 1.1.0", style = MaterialTheme.typography.titleMedium)
                    Text("Developer: Aakash Panta", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Production-style Android download manager with aria2 JSON-RPC, multi-connection direct downloads, BitTorrent, magnet and Metalink support, foreground syncing and a Material 3 UI.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
