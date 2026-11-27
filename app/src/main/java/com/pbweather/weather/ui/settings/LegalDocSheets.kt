package com.pbweather.weather.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pbweather.weather.R
import com.pbweather.weather.domain.ForecastState
import com.pbweather.weather.domain.WeatherCondition
import com.pbweather.weather.ui.components.AppIcon
import com.pbweather.weather.ui.components.PrimaryButton
import com.pbweather.weather.ui.components.Rule
import com.pbweather.weather.ui.components.SecondaryButton
import com.pbweather.weather.ui.components.SectionCard
import com.pbweather.weather.ui.components.SheetHeader
import com.pbweather.weather.ui.components.UiIcon
import com.pbweather.weather.ui.components.WeatherSymbol
import com.pbweather.weather.ui.components.prototypeSheetBorder
import com.pbweather.weather.ui.components.prototypeSheetModifier
import com.pbweather.weather.ui.components.skyLabel
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
    ModalBottomSheet(
        contentWindowInsets = { androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0) },
        onDismissRequest = onClose,
        containerColor = WeatherTheme.colors.surface,
        scrimColor = modalScrim(WeatherTheme.mode),
        shape = RoundedCornerShape(Dims.sheetRadius),
        dragHandle = null,
        modifier = prototypeSheetModifier(),
        sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            prototypeSheetBorder().fillMaxWidth().heightIn(
                max = LocalConfiguration.current.screenHeightDp.dp * .88f
            ).verticalScroll(rememberScrollState()).padding(Dims.sheet),
            verticalArrangement = Arrangement.spacedBy(Dims.medium)
        ) {
            SheetHeader(title, onClose)
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
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                PrimaryButton(onClick = onClose) { Text(stringResource(R.string.document_confirm)) }
            }
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
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(entry), Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                AppIcon(UiIcon.NEXT, null, Modifier.size(Dims.smallIcon).rotate(if (expanded) 90f else 0f))
            }
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
    ModalBottomSheet(
        contentWindowInsets = { androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0) },
        onDismissRequest = onClose,
        containerColor = WeatherTheme.colors.surface,
        scrimColor = modalScrim(WeatherTheme.mode),
        shape = RoundedCornerShape(Dims.sheetRadius),
        dragHandle = null,
        modifier = prototypeSheetModifier(),
        sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            prototypeSheetBorder().fillMaxWidth().heightIn(
                max = LocalConfiguration.current.screenHeightDp.dp * .88f
            ).verticalScroll(rememberScrollState()).padding(Dims.sheet),
            verticalArrangement = Arrangement.spacedBy(Dims.medium)
        ) {
            SheetHeader(stringResource(R.string.widget_preview), onClose)
            Box(
                Modifier.fillMaxWidth().background(
                    Brush.linearGradient(WeatherTheme.colors.widgetWallpaper),
                    RoundedCornerShape(Dims.radius)
                ).padding(horizontal = Dims.section, vertical = Dims.sheet)
            ) {
                SectionCard {
                    Text(name ?: stringResource(R.string.widget_no_primary), style = MaterialTheme.typography.titleSmall)
                    if (name != null) {
                        val hour = forecast.hourly.data?.hours?.minByOrNull {
                            kotlin.math.abs(it.at - (forecast.now.meta?.observedAt ?: it.at))
                        }
                        Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Text(
                                temperature(forecast.now.data?.temperature),
                                Modifier.weight(1f),
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Box(Modifier.size(Dims.touch)) {
                                WeatherSymbol(
                                    WeatherCondition.from(hour?.sky, forecast.now.data?.precipitationType),
                                    Modifier.size(Dims.touch)
                                )
                            }
                        }
                        val day = forecast.weekly.data?.firstOrNull()
                        Text(
                            stringResource(
                                R.string.design_widget_summary,
                                skyLabel(WeatherCondition.from(hour?.sky, forecast.now.data?.precipitationType)),
                                com.pbweather.weather.ui.components.number(day?.maximum) ?: "—",
                                com.pbweather.weather.ui.components.number(day?.minimum) ?: "—"
                            ),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Rule()
                        val observed = forecast.now
                        Text(
                            stringResource(
                                when {
                                    observed.data == null -> R.string.widget_loading
                                    observed.isStale || observed.isOffline || observed.error != null -> R.string.widget_delayed
                                    else -> R.string.widget_refresh_explanation
                                }
                            ),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            stringResource(R.string.preview_last_success, timeLabel(forecast.now.meta?.receivedAt) ?: "—"),
                            style = WeatherTheme.type.meta
                        )
                    } else {
                        SecondaryButton(onClick = onLocations) { Text(stringResource(R.string.widget_choose_primary)) }
                    }
                }
            }
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dims.small, Alignment.End),
                verticalArrangement = Arrangement.spacedBy(Dims.small)
            ) {
                SecondaryButton(onClick = onLocations) { Text(stringResource(R.string.change_primary)) }
                PrimaryButton(onClick = onClose) { Text(stringResource(R.string.document_confirm)) }
            }
        }
    }
}
