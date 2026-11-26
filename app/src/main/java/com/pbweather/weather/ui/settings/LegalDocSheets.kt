package com.pbweather.weather.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pbweather.weather.R
import com.pbweather.weather.domain.ForecastState
import com.pbweather.weather.ui.components.Rule
import com.pbweather.weather.ui.components.temperature
import com.pbweather.weather.ui.components.timeLabel
import com.pbweather.weather.ui.theme.Dims
import com.pbweather.weather.ui.theme.WeatherTheme
import com.pbweather.weather.ui.theme.modalScrim
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalDocSheet(target: String, onClose: () -> Unit) {
    val title =
        stringResource(
            when (target) {
                "privacy" -> R.string.privacy_policy
                "licenses" -> R.string.open_source_licenses
                "usage" -> R.string.usage_guide
                else -> R.string.help
            }
        )
    ModalBottomSheet(onDismissRequest = onClose, containerColor = WeatherTheme.colors.surface, scrimColor = modalScrim(WeatherTheme.mode)) {
        Column(
            Modifier.fillMaxWidth().heightIn(
                max = LocalConfiguration.current.screenHeightDp.dp * .88f
            ).verticalScroll(rememberScrollState()).padding(Dims.sheet),
            verticalArrangement = Arrangement.spacedBy(Dims.medium)
        ) {
            Text(title, style = WeatherTheme.type.sheetTitle)
            SelectionContainer {
                Column(verticalArrangement = Arrangement.spacedBy(Dims.medium)) {
                    when (target) {
                        "privacy" -> {
                            listOf(
                                R.string.privacy_overview_title to R.string.privacy_overview,
                                R.string.privacy_storage_title to R.string.privacy_storage,
                                R.string.privacy_connections_title to R.string.privacy_connections,
                                R.string.privacy_permissions_title to R.string.privacy_permissions,
                                R.string.privacy_deletion_title to R.string.privacy_deletion
                            ).forEach { (heading, body) ->
                                Text(stringResource(heading), style = MaterialTheme.typography.titleMedium)
                                Text(stringResource(body))
                            }
                        }
                        "licenses" -> LicenseList()
                        "usage" -> {
                            Text(stringResource(R.string.data_sources))
                            Text(stringResource(R.string.usage_text))
                        }
                        else -> {
                            Text(stringResource(R.string.help_text))
                            Text(stringResource(R.string.widget_refresh_explanation))
                            Text(stringResource(R.string.help_extra))
                        }
                    }
                }
            }
            Text(
                stringResource(R.string.document_effective_date),
                style = MaterialTheme.typography.bodySmall,
                color = WeatherTheme.colors.secondary
            )
            Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.document_confirm)) }
        }
    }
}

@Composable private fun LicenseList() {
    val context = LocalContext.current
    val apache by produceState("") {
        value = withContext(Dispatchers.IO) { context.assets.open("legal/apache-2.0.txt").bufferedReader().use { it.readText() } }
    }
    listOf(
        R.string.license_androidx,
        R.string.license_kotlin,
        R.string.license_dagger,
        R.string.license_square,
        R.string.license_google_services
    ).forEach { entry ->
        var expanded by remember { mutableStateOf(false) }
        Rule()
        Column(Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(vertical = Dims.medium)) {
            Text(stringResource(entry) + if (expanded) " ⌄" else " ›", style = MaterialTheme.typography.titleMedium)
            if (expanded) {
                Text(
                    stringResource(
                        when (entry) {
                            R.string.license_androidx -> R.string.license_androidx_body
                            R.string.license_kotlin -> R.string.license_kotlin_body
                            R.string.license_dagger -> R.string.license_dagger_body
                            R.string.license_square -> R.string.license_square_body
                            else -> R.string.license_google_body
                        }
                    )
                )
                if (entry != R.string.license_google_services) Text(apache, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetPreviewSheet(name: String?, forecast: ForecastState, onLocations: () -> Unit, onClose: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onClose, containerColor = WeatherTheme.colors.surface, scrimColor = modalScrim(WeatherTheme.mode)) {
        Column(Modifier.fillMaxWidth().padding(Dims.sheet), verticalArrangement = Arrangement.spacedBy(Dims.medium)) {
            Text(stringResource(R.string.widget_preview), style = WeatherTheme.type.sheetTitle)
            Text(name ?: stringResource(R.string.primary_not_set), style = MaterialTheme.typography.titleMedium)
            if (name != null) {
                Text(temperature(forecast.now.data?.temperature), style = MaterialTheme.typography.headlineMedium)
                Text(stringResource(R.string.preview_last_success, timeLabel(forecast.now.meta?.receivedAt) ?: "—"))
                Text(stringResource(R.string.widget_refresh_explanation), style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = onLocations) { Text(stringResource(R.string.change_primary)) }
            Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.document_confirm)) }
        }
    }
}
