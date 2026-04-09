package com.aria2.downloader.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aria2.downloader.data.preferences.SettingsRepository
import com.aria2.downloader.domain.model.AppIcon
import com.aria2.downloader.domain.model.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    paddingTop: androidx.compose.ui.unit.Dp,
    viewModel: SettingsViewModel
) {
    val settings by viewModel.settings.collectAsState()
    val message by viewModel.message.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val currentLocation = if (settings.downloadLocationUri != null) {
        settings.downloadLocationLabel ?: "Custom folder"
    } else {
        settings.downloadLocationPath
    }

    val folderPicker = rememberLauncherForActivityResult(OpenDocumentTree()) { uri ->
        if (uri != null) {
            viewModel.updateDownloadLocation(uri)
        }
    }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(top = paddingTop)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .padding(bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SettingsCard("Theme") {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    ThemeMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = settings.themeMode == mode,
                            onClick = { viewModel.updateTheme(mode) },
                            shape = androidx.compose.material3.SegmentedButtonDefaults.itemShape(
                                index,
                                ThemeMode.entries.size
                            )
                        ) {
                            Text(mode.name.lowercase().replaceFirstChar { it.titlecase() })
                        }
                    }
                }
            }

            SettingsCard("Download location") {
                Text(
                    "Current location",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    currentLocation,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    if (settings.downloadLocationUri == null) {
                        "Default direct download path is /storage/emulated/0/Download"
                    } else {
                        "Custom folders use SAF. aria2 downloads first, then completed files are copied into the selected folder."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { folderPicker.launch(null) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Pick folder")
                    }
                    OutlinedButton(
                        onClick = viewModel::useDefaultDownloadLocation,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Use Download")
                    }
                }
            }

            SettingsCard("Launcher icon") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppIcon.entries.forEach { icon ->
                        AssistChip(
                            onClick = { viewModel.updateIcon(icon) },
                            label = { Text(icon.name.lowercase().replaceFirstChar { it.titlecase() }) }
                        )
                    }
                }
            }

            SettingsCard("Concurrent downloads") {
                SliderRow("Queue size", settings.maxConcurrentDownloads.toFloat(), 1f..10f) {
                    viewModel.updateConcurrent(it.toInt())
                }
                SliderRow("Split per item", settings.split.toFloat(), 1f..16f) {
                    viewModel.updateSplit(it.toInt())
                }
                SliderRow("Connections per server", settings.maxConnectionPerServer.toFloat(), 1f..16f) {
                    viewModel.updateConnections(it.toInt())
                }
                SliderRow("Min split size (MB)", settings.minSplitSizeMb.toFloat(), 1f..32f) {
                    viewModel.updateMinSplit(it.toInt())
                }
            }

            SettingsCard("Torrent engine") {
                ToggleRow("DHT", settings.enableDht, viewModel::updateDht)
                ToggleRow("PEX", settings.peerExchange, viewModel::updatePex)
                ToggleRow("Local peer discovery", settings.localPeerDiscovery, viewModel::updateLpd)
                ToggleRow("Require encryption", settings.requireEncryption, viewModel::updateEncryption)
            }

            SettingsCard("Notifications") {
                ToggleRow(
                    "Foreground service notifications",
                    settings.notificationsEnabled,
                    viewModel::updateNotifications
                )
            }

            SettingsCard("About this build") {
                Text("Version 1.1.0", style = MaterialTheme.typography.bodyMedium)
                Text("Developer: Aakash Panta", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "This build ships a bundled arm64 aria2 binary, starts it as a local daemon, and talks to it over JSON-RPC.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Default path: ${SettingsRepository.DEFAULT_DOWNLOAD_PATH}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun SliderRow(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("$title: ${value.toInt()}", style = MaterialTheme.typography.bodyMedium)
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range
        )
    }
}

@Composable
private fun ToggleRow(
    title: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onToggle)
    }
}
