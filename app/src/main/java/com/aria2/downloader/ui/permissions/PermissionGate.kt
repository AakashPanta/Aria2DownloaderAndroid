package com.aria2.downloader.ui.permissions

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

@Composable
fun PermissionAwareContent(
    activity: ComponentActivity,
    content: @Composable () -> Unit
) {
    val runtimePermissions = remember { buildRuntimePermissions(activity) }
    var continueWithoutAll by remember { mutableStateOf(false) }
    var autoOpenedAllFiles by remember { mutableStateOf(false) }
    var requestNonce by remember { mutableStateOf(0) }
    var permissionState by remember(requestNonce) {
        mutableStateOf(evaluatePermissionState(activity, runtimePermissions))
    }

    val launcher = rememberLauncherForActivityResult(RequestMultiplePermissions()) {
        permissionState = evaluatePermissionState(activity, runtimePermissions)
    }

    DisposableEffect(activity, runtimePermissions) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionState = evaluatePermissionState(activity, runtimePermissions)
            }
        }
        activity.lifecycle.addObserver(observer)
        onDispose { activity.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(runtimePermissions, requestNonce, continueWithoutAll) {
        if (continueWithoutAll) return@LaunchedEffect

        if (permissionState.missingRuntimePermissions.isNotEmpty()) {
            launcher.launch(permissionState.missingRuntimePermissions.toTypedArray())
        } else if (permissionState.requiresAllFiles && !permissionState.allFilesGranted && !autoOpenedAllFiles) {
            autoOpenedAllFiles = true
            activity.startActivity(createAllFilesAccessIntent(activity))
        }
    }

    if (permissionState.allGranted || continueWithoutAll) {
        content()
    } else {
        PermissionGate(
            state = permissionState,
            onGrantRuntime = {
                requestNonce++
                launcher.launch(permissionState.missingRuntimePermissions.toTypedArray())
            },
            onGrantAllFiles = {
                activity.startActivity(createAllFilesAccessIntent(activity))
            },
            onOpenAppSettings = {
                val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", activity.packageName, null)
                )
                activity.startActivity(intent)
            },
            onContinueLimited = { continueWithoutAll = true }
        )
    }
}

@Composable
private fun PermissionGate(
    state: PermissionState,
    onGrantRuntime: () -> Unit,
    onGrantAllFiles: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onContinueLimited: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(Icons.Default.Security, contentDescription = null)
                Text("Permission setup", style = MaterialTheme.typography.headlineSmall)

                Text(
                    "This build uses the public Download folder by default: /storage/emulated/0/Download",
                    style = MaterialTheme.typography.bodyMedium
                )

                if (state.missingRuntimePermissions.isNotEmpty()) {
                    Text(
                        "Missing runtime permissions: ${state.permissionLabels.joinToString()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(onClick = onGrantRuntime, modifier = Modifier.fillMaxWidth()) {
                        Text("Grant media and notification permissions")
                    }
                }

                if (state.requiresAllFiles && !state.allFilesGranted) {
                    Text(
                        "Storage access is still missing. Grant All files access so aria2 can save directly into /storage/emulated/0/Download.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(onClick = onGrantAllFiles, modifier = Modifier.fillMaxWidth()) {
                        Text("Grant storage access")
                    }
                }

                if (state.showRationale) {
                    Text(
                        "If you denied a permission, use the buttons below to retry or open app settings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedButton(onClick = onOpenAppSettings, modifier = Modifier.fillMaxWidth()) {
                    Text("Open app settings")
                }

                OutlinedButton(onClick = onContinueLimited, modifier = Modifier.fillMaxWidth()) {
                    Text("Continue for now")
                }
            }
        }
    }
}

private data class PermissionState(
    val missingRuntimePermissions: List<String>,
    val permissionLabels: List<String>,
    val allFilesGranted: Boolean,
    val requiresAllFiles: Boolean,
    val showRationale: Boolean
) {
    val allGranted: Boolean
        get() = missingRuntimePermissions.isEmpty() && (!requiresAllFiles || allFilesGranted)
}

private fun buildRuntimePermissions(activity: ComponentActivity): List<String> {
    val permissions = mutableListOf<String>()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        permissions += Manifest.permission.POST_NOTIFICATIONS
        permissions += Manifest.permission.READ_MEDIA_IMAGES
        permissions += Manifest.permission.READ_MEDIA_VIDEO
        permissions += Manifest.permission.READ_MEDIA_AUDIO
    } else {
        permissions += Manifest.permission.READ_EXTERNAL_STORAGE
    }

    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
        permissions += Manifest.permission.WRITE_EXTERNAL_STORAGE
    }

    return permissions.filter {
        ContextCompat.checkSelfPermission(activity, it) != android.content.pm.PackageManager.PERMISSION_GRANTED
    }
}

private fun evaluatePermissionState(
    activity: ComponentActivity,
    runtimePermissions: List<String>
): PermissionState {
    val missing = runtimePermissions.filter {
        ContextCompat.checkSelfPermission(activity, it) != android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    val labels = missing.mapNotNull { permissionLabel(it) }.distinct()

    val requiresAllFiles = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
    val allFilesGranted = if (requiresAllFiles) Environment.isExternalStorageManager() else true

    return PermissionState(
        missingRuntimePermissions = missing,
        permissionLabels = labels,
        allFilesGranted = allFilesGranted,
        requiresAllFiles = requiresAllFiles,
        showRationale = missing.any(activity::shouldShowRequestPermissionRationale)
    )
}

private fun permissionLabel(permission: String): String? = when (permission) {
    Manifest.permission.POST_NOTIFICATIONS -> "Notifications"
    Manifest.permission.READ_MEDIA_IMAGES,
    Manifest.permission.READ_MEDIA_VIDEO -> "Photos and videos"
    Manifest.permission.READ_MEDIA_AUDIO -> "Music and audio"
    Manifest.permission.READ_EXTERNAL_STORAGE,
    Manifest.permission.WRITE_EXTERNAL_STORAGE -> "Storage"
    else -> null
}

private fun createAllFilesAccessIntent(activity: ComponentActivity): Intent {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Intent(
            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
            Uri.parse("package:${activity.packageName}")
        ).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
        }
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", activity.packageName, null)
        }
    }
}
