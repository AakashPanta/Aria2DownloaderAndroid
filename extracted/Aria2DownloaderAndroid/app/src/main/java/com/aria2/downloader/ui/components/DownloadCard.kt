package com.aria2.downloader.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aria2.downloader.domain.model.DownloadInfo
import com.aria2.downloader.domain.model.DownloadStatus

@Composable
fun DownloadCard(
    downloadInfo: DownloadInfo,
    onPause: () -> Unit = {},
    onResume: () -> Unit = {},
    onCancel: () -> Unit = {},
    onRetry: () -> Unit = {},
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with fileName and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = downloadInfo.fileName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                StatusBadge(status = downloadInfo.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress bar
            DownloadProgressBar(
                progress = downloadInfo.progressPercent.toFloat() / 100f,
                label = "${downloadInfo.progressPercent}%",
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Size info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = downloadInfo.formattedDownloadedSize(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (downloadInfo.status == DownloadStatus.DOWNLOADING) {
                    Text(
                        text = downloadInfo.progress.formattedSpeed(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Action buttons
            if (downloadInfo.isActive || downloadInfo.status == DownloadStatus.FAILED) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when (downloadInfo.status) {
                        DownloadStatus.DOWNLOADING -> {
                            Button(
                                onClick = onPause,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp),
                                colors = ButtonDefaults.outlinedButtonColors()
                            ) {
                                Icon(
                                    Icons.Filled.Pause,
                                    contentDescription = "Pause",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Pause", style = MaterialTheme.typography.labelSmall)
                            }

                            Button(
                                onClick = onCancel,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp),
                                colors = ButtonDefaults.outlinedButtonColors()
                            ) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "Cancel",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Cancel", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        DownloadStatus.PAUSED -> {
                            Button(
                                onClick = onResume,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                            ) {
                                Icon(
                                    Icons.Filled.PlayArrow,
                                    contentDescription = "Resume",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Resume", style = MaterialTheme.typography.labelSmall)
                            }

                            Button(
                                onClick = onCancel,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp),
                                colors = ButtonDefaults.outlinedButtonColors()
                            ) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "Cancel",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        DownloadStatus.FAILED -> {
                            Button(
                                onClick = onRetry,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Refresh,
                                    contentDescription = "Retry",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Retry", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: DownloadStatus) {
    val backgroundColor = when (status) {
        DownloadStatus.DOWNLOADING -> MaterialTheme.colorScheme.primaryContainer
        DownloadStatus.COMPLETED -> MaterialTheme.colorScheme.primary
        DownloadStatus.FAILED -> MaterialTheme.colorScheme.error
        DownloadStatus.PAUSED -> MaterialTheme.colorScheme.secondaryContainer
        DownloadStatus.CANCELLED -> MaterialTheme.colorScheme.outline
        DownloadStatus.PENDING -> MaterialTheme.colorScheme.tertiaryContainer
    }

    val textColor = when (status) {
        DownloadStatus.DOWNLOADING -> MaterialTheme.colorScheme.onPrimaryContainer
        DownloadStatus.COMPLETED -> MaterialTheme.colorScheme.onPrimary
        DownloadStatus.FAILED -> MaterialTheme.colorScheme.onError
        DownloadStatus.PAUSED -> MaterialTheme.colorScheme.onSecondaryContainer
        DownloadStatus.CANCELLED -> MaterialTheme.colorScheme.onSurface
        DownloadStatus.PENDING -> MaterialTheme.colorScheme.onTertiaryContainer
    }

    Surface(
        color = backgroundColor,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = status.name,
            color = textColor,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
