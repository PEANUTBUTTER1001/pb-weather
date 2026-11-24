package com.pbweather.weather.ui.permission

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.pbweather.weather.R

@Composable
fun LocationPermissionAction(onResult: (Boolean) -> Unit, content: @Composable (() -> Unit) -> Unit) {
    var explain by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission(), onResult)
    content { explain = true }
    if (explain) {
        AlertDialog(
            onDismissRequest = {
                explain = false
                onResult(false)
            },
            title = { Text(stringResource(R.string.location_permission_title)) },
            text = { Text(stringResource(R.string.location_permission_body)) },
            confirmButton = {
                TextButton(onClick = {
                    explain = false
                    launcher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                }) { Text(stringResource(R.string.location_permission_request)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    explain = false
                    onResult(false)
                }) {
                    Text(stringResource(R.string.location_search_instead))
                }
            }
        )
    }
}
