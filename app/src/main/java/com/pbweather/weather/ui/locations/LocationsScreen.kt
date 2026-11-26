package com.pbweather.weather.ui.locations

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.pbweather.weather.R
import com.pbweather.weather.domain.ForecastState
import com.pbweather.weather.domain.LocationProblem
import com.pbweather.weather.domain.Region
import com.pbweather.weather.domain.WeatherCondition
import com.pbweather.weather.ui.components.AppIcon
import com.pbweather.weather.ui.components.Rule
import com.pbweather.weather.ui.components.SectionTitle
import com.pbweather.weather.ui.components.UiIcon
import com.pbweather.weather.ui.components.skyLabel
import com.pbweather.weather.ui.components.temperature
import com.pbweather.weather.ui.navigation.WeatherActions
import com.pbweather.weather.ui.navigation.WeatherUiState
import com.pbweather.weather.ui.permission.LocationPermissionAction
import com.pbweather.weather.ui.theme.Dims
import com.pbweather.weather.ui.theme.WeatherTheme

@Composable
fun LocationsScreen(
    state: WeatherUiState,
    query: String,
    results: List<Region>,
    forecasts: Map<String, ForecastState>,
    actions: WeatherActions,
    onSelect: (Region) -> Unit,
    onSettings: () -> Unit,
    list: LazyListState
) {
    LazyColumn(
        state = list,
        modifier = Modifier.padding(horizontal = Dims.gutter),
        verticalArrangement = Arrangement.spacedBy(Dims.small)
    ) {
        item(key = "search") {
            SectionTitle(stringResource(R.string.search_region)) {
                Text(stringResource(R.string.offline_search), style = MaterialTheme.typography.bodySmall)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(query, actions.search, Modifier.weight(1f), singleLine = true, placeholder = {
                    Text(stringResource(R.string.search_hint))
                })
                if (query.isNotEmpty()) TextButton(onClick = { actions.search("") }) { Text(stringResource(R.string.clear)) }
            }
            if (query.length < 2 ||
                results.isEmpty()
            ) {
                Text(stringResource(if (query.length < 2) R.string.search_minimum else R.string.search_empty))
            }
            Rule()
        }
        items(results, key = { "search_${it.id}" }) { region ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                RegionLabel(region, forecasts[region.id], state.now, Modifier.weight(1f), onSelect)
                val saved = state.preferences.favorites.any { it.id == region.id }
                OutlinedButton(onClick = {
                    actions.favorite(region)
                }, enabled = !saved) { Text(stringResource(if (saved) R.string.added else R.string.add)) }
            }
            Rule()
        }
        item(key = "current") {
            SectionTitle(stringResource(R.string.current_location))
            Text(stringResource(R.string.choose_without_location), style = MaterialTheme.typography.bodySmall)
            state.location.problem?.let { problem ->
                Text(
                    stringResource(
                        when (problem) {
                            LocationProblem.PERMISSION_DENIED -> R.string.location_denied
                            LocationProblem.DISABLED -> R.string.location_disabled
                            LocationProblem.TIMEOUT -> R.string.location_timeout
                            LocationProblem.UNAVAILABLE -> R.string.location_unavailable
                            LocationProblem.OUTSIDE_COVERAGE -> R.string.location_outside
                        }
                    ),
                    color = WeatherTheme.colors.warning
                )
            }
            if (state.location.working) Text(stringResource(R.string.locating))
            LocationPermissionAction(onResult = { granted ->
                actions.permissionChanged()
                if (granted) actions.locate()
            }) { request ->
                TextButton(onClick = if (state.location.granted) actions.locate else request, enabled = !state.location.working) {
                    Text(stringResource(R.string.use_location))
                }
            }
            if (!state.location.granted) TextButton(onClick = onSettings) { Text(stringResource(R.string.open_settings)) }
            Rule()
        }
        item(key = "favorites-title") {
            SectionTitle(stringResource(R.string.favorites))
            Text(stringResource(R.string.favorite_count, state.preferences.favorites.size), style = MaterialTheme.typography.bodySmall)
            if (state.preferences.favorites.isEmpty()) Text(stringResource(R.string.favorites_empty))
            Rule()
        }
        items(state.preferences.favorites, key = { "favorite_${it.id}" }) { region ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row {
                        if (state.preferences.primaryId ==
                            region.id
                        ) {
                            Text(stringResource(R.string.primary_badge), color = WeatherTheme.colors.accent)
                        }
                        if (state.preferences.selected?.id ==
                            region.id
                        ) {
                            Text(stringResource(R.string.displaying), Modifier.padding(start = Dims.small))
                        }
                    }
                    RegionLabel(region, forecasts[region.id], state.now, Modifier.fillMaxWidth(), onSelect)
                }
                IconButton(onClick = { actions.primary(region.id) }) {
                    AppIcon(
                        UiIcon.STAR,
                        stringResource(R.string.set_primary_named, region.name),
                        tint = if (state.preferences.primaryId ==
                            region.id
                        ) {
                            WeatherTheme.colors.accent
                        } else {
                            WeatherTheme.colors.secondary
                        }
                    )
                }
                IconButton(onClick = { actions.deleteFavorite(region.id) }, modifier = Modifier.offset(x = Dims.medium)) {
                    AppIcon(UiIcon.DELETE, stringResource(R.string.delete_named, region.name), tint = WeatherTheme.colors.error)
                }
            }
            Rule()
        }
    }
}

@Composable private fun RegionLabel(region: Region, forecast: ForecastState?, now: Long, modifier: Modifier, onSelect: (Region) -> Unit) {
    Column(
        modifier.heightIn(min = Dims.touch).clickable {
            onSelect(region)
        }.semantics { contentDescription = region.name }.padding(vertical = Dims.medium)
    ) {
        Text(region.name, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
        val hour = forecast?.hourly?.data?.hours?.minByOrNull { kotlin.math.abs(it.at - now) }
        Text(
            if (forecast?.now?.data != null) {
                temperature(forecast.now.data.temperature) + " · " +
                    skyLabel(WeatherCondition.from(hour?.sky, forecast.now.data.precipitationType))
            } else {
                stringResource(R.string.region_cached_weather_missing)
            },
            style = MaterialTheme.typography.bodySmall,
            color = WeatherTheme.colors.secondary
        )
        if (forecast?.now?.isStale == true) Text(stringResource(R.string.stale), style = MaterialTheme.typography.bodySmall)
    }
}
