package com.pbweather.weather.ui.permission

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.pbweather.weather.R
import com.pbweather.weather.ui.components.PrototypeConfirmDialog
import com.pbweather.weather.ui.theme.Dims
import com.pbweather.weather.ui.theme.WeatherTheme

@Composable
fun LocationPermissionAction(onResult: (Boolean) -> Unit, content: @Composable (() -> Unit) -> Unit) {
    var explain by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission(), onResult)
    content { explain = true }
    if (explain) {
        PrototypeConfirmDialog(
            stringResource(R.string.location_permission_title),
            stringResource(R.string.location_permission_body),
            stringResource(R.string.location_permission_request),
            stringResource(R.string.location_search_instead),
            onConfirm = {
                explain = false
                launcher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            },
            onDismiss = {
                explain = false
                onResult(false)
            }
        )
    }
}
