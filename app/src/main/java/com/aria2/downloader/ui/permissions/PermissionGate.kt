package com.aria2.downloader.ui.permissions

import android.Manifest
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

@Composable
fun PermissionAwareContent(
    activity: ComponentActivity,
    content: @Composable () -> Unit
) {
    val requiredPermissions = remember { buildRuntimePermissions(activity) }
    var continueWithoutAll by remember { mutableStateOf(false) }
    var requestNonce by remember { mutableStateOf(0) }
    var permissionState by remember(requestNonce) { mutableStateOf(evaluatePermissionState(activity, requiredPermissions)) }

    val launcher = rememberLauncherForActivityResult(RequestMultiplePermissions()) {
        permissionState = evaluatePermissionState(activity, requiredPermissions)
    }

    LaunchedEffect(requiredPermissions, requestNonce) {
        if (requiredPermissions.isNotEmpty() && !permissionState.allGranted && !continueWithoutAll) {
            launcher.launch(requiredPermissions.toTypedArray())
        }
    }

    if (requiredPermissions.isEmpty() || permissionState.allGranted || continueWithoutAll) {
        content()
    } else {
        PermissionGate(
            showRationale = permissionState.showRationale,
            onGrant = {
                requestNonce++
                launcher.launch(requiredPermissions.toTypedArray())
            },
            onContinueLimited = { continueWithoutAll = true }
        )
    }
}

@Composable
private fun PermissionGate(
    showRationale: Boolean,
    onGrant: () -> Unit,
    onContinueLimited: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
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
                    if (showRationale) {
                        "Allow notifications and legacy storage access where applicable so the downloader can show foreground progress, import files smoothly and work reliably in the background."
                    } else {
                        "The app requests the permissions it actually needs on this Android version. You can retry the request now."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(onClick = onGrant, modifier = Modifier.fillMaxWidth()) {
                    Text("Grant permissions")
                }
                OutlinedButton(onClick = onContinueLimited, modifier = Modifier.fillMaxWidth()) {
                    Text("Continue for now")
                }
            }
        }
    }
}

private data class PermissionState(
    val allGranted: Boolean,
    val showRationale: Boolean
)

private fun buildRuntimePermissions(activity: ComponentActivity): List<String> {
    val permissions = mutableListOf<String>()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        permissions += Manifest.permission.POST_NOTIFICATIONS
    }
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
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
    permissions: List<String>
): PermissionState {
    if (permissions.isEmpty()) return PermissionState(allGranted = true, showRationale = false)
    val missing = permissions.filter {
        ContextCompat.checkSelfPermission(activity, it) != android.content.pm.PackageManager.PERMISSION_GRANTED
    }
    return PermissionState(
        allGranted = missing.isEmpty(),
        showRationale = missing.any(activity::shouldShowRequestPermissionRationale)
    )
}
