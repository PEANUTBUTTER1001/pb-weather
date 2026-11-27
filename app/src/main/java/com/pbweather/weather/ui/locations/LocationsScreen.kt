package com.pbweather.weather.ui.locations

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.pbweather.weather.R
import com.pbweather.weather.domain.ForecastState
import com.pbweather.weather.domain.LocationProblem
import com.pbweather.weather.domain.Region
import com.pbweather.weather.domain.WeatherCondition
import com.pbweather.weather.ui.components.AppIcon
import com.pbweather.weather.ui.components.CompactOutlinedButton as OutlinedButton
import com.pbweather.weather.ui.components.CompactTextField
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
        modifier = Modifier.testTag("locations-list"),
        verticalArrangement = Arrangement.spacedBy(Dims.zero),
        contentPadding = PaddingValues(horizontal = Dims.gutter, vertical = Dims.pageTop)
    ) {
        item(key = "search") {
            Column(verticalArrangement = Arrangement.spacedBy(Dims.small)) {
                GroupHeading(stringResource(R.string.search_region)) {
                    Text(stringResource(R.string.offline_search), style = WeatherTheme.type.caption, color = WeatherTheme.colors.secondary)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CompactTextField(query, actions.search, Modifier.weight(1f), placeholder = stringResource(R.string.search_hint))
                    if (query.isNotEmpty()) TextButton(onClick = { actions.search("") }) { Text(stringResource(R.string.clear)) }
                }
                if (query.length < 2 ||
                    results.isEmpty()
                ) {
                    Text(
                        stringResource(
                            if (query.length <
                                2
                            ) {
                                R.string.design_search_examples
                            } else {
                                R.string.search_empty
                            }
                        ),
                        style = WeatherTheme.type.caption,
                        color = WeatherTheme.colors.secondary
                    )
                }
            }
        }
        items(results, key = { "search_${it.id}" }) { region ->
            Row(Modifier.fillMaxWidth().testTag("search:${region.id}"), verticalAlignment = Alignment.CenterVertically) {
                RegionLabel(region, forecasts[region.id], state.now, Modifier.weight(1f), onSelect)
                val saved = state.preferences.favorites.any { it.id == region.id }
                OutlinedButton(onClick = {
                    actions.favorite(region)
                }, enabled = !saved) { Text(stringResource(if (saved) R.string.added else R.string.add)) }
            }
            Rule()
        }
        item(key = "current") {
            Spacer(Modifier.height(Dims.gutter))
            GroupHeading(stringResource(R.string.current_location))
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
            FlatRule()
            LocationPermissionAction(onResult = { granted ->
                actions.permissionChanged()
                if (granted) actions.locate()
            }) { request ->
                Row(Modifier.padding(vertical = Dims.medium), verticalAlignment = Alignment.CenterVertically) {
                    Column(
                        Modifier.weight(
                            1f
                        ).clickable(enabled = !state.location.working, onClick = if (state.location.granted) actions.locate else request)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(Dims.small), verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.design_current_auto), style = MaterialTheme.typography.titleSmall)
                            if (state.preferences.selected?.usingCurrentLocation ==
                                true
                            ) {
                                androidx.compose.material3.Surface(
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(Dims.pill),
                                    color = WeatherTheme.colors.accentSoft
                                ) {
                                    Text(
                                        stringResource(R.string.design_using_current),
                                        Modifier.padding(horizontal = Dims.badgeInset, vertical = Dims.segmentGap),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = WeatherTheme.colors.accentOnSoft
                                    )
                                }
                            }
                        }
                        Text(
                            stringResource(R.string.design_current_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = WeatherTheme.colors.secondary
                        )
                    }
                    OutlinedButton(onClick = request, enabled = !state.location.working) {
                        Text(stringResource(R.string.design_location_help))
                    }
                }
            }
            if (!state.location.granted) TextButton(onClick = onSettings) { Text(stringResource(R.string.open_settings)) }
            FlatRule()
        }
        item(key = "favorites-title") {
            Spacer(Modifier.height(Dims.gutter))
            GroupHeading(stringResource(R.string.favorites)) {
                Text(stringResource(R.string.design_favorite_count, state.preferences.favorites.size), style = WeatherTheme.type.caption)
            }
            if (state.preferences.favorites.isEmpty()) {
                Text(
                    stringResource(R.string.favorites_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = WeatherTheme.colors.secondary
                )
            }
            FlatRule()
        }
        items(state.preferences.favorites, key = { "favorite_${it.id}" }) { region ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                RegionLabel(
                    region,
                    forecasts[region.id],
                    state.now,
                    Modifier.weight(1f),
                    onSelect,
                    primary =
                    state.preferences.primaryId == region.id
                )
                Row(
                    Modifier.offset(x = Dims.small),
                    horizontalArrangement = Arrangement.spacedBy(Dims.tiny),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            actions.primary(region.id)
                        },
                        modifier = Modifier.size(Dims.action).semantics {
                            selected =
                                state.preferences.primaryId == region.id
                        }
                    ) {
                        AppIcon(
                            if (state.preferences.primaryId == region.id) UiIcon.STAR_FILLED else UiIcon.STAR,
                            stringResource(R.string.set_primary_named, region.name),
                            tint = WeatherTheme.colors.text
                        )
                    }
                    IconButton(onClick = { actions.deleteFavorite(region.id) }, modifier = Modifier.size(Dims.action)) {
                        AppIcon(UiIcon.DELETE, stringResource(R.string.delete_named, region.name), tint = WeatherTheme.colors.destructive)
                    }
                }
            }
            FlatRule(strong = region.id == state.preferences.favorites.lastOrNull()?.id)
        }
    }
}

@Composable private fun RegionLabel(
    region: Region,
    forecast: ForecastState?,
    now: Long,
    modifier: Modifier,
    onSelect: (Region) -> Unit,
    primary: Boolean = false
) {
    Column(
        modifier.heightIn(min = Dims.touch).clickable {
            onSelect(region)
        }.semantics { contentDescription = region.name }.padding(vertical = Dims.medium)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Dims.small), verticalAlignment = Alignment.CenterVertically) {
            Text(
                regionHeading(region.name),
                Modifier.weight(1f, fill = false),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleSmall
            )
            if (primary) {
                androidx.compose.material3.Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(Dims.pill),
                    color = WeatherTheme.colors.accentSoft
                ) {
                    Text(
                        stringResource(R.string.primary_badge),
                        Modifier.padding(horizontal = Dims.small, vertical = Dims.segmentGap),
                        style = MaterialTheme.typography.labelSmall,
                        color = WeatherTheme.colors.accentOnSoft
                    )
                }
            }
        }
        val hour = forecast?.hourly?.data?.hours?.minByOrNull { kotlin.math.abs(it.at - now) }
        Text(
            if (forecast?.now?.data != null) {
                region.name + " · " + temperature(forecast.now.data.temperature) + " · " +
                    skyLabel(WeatherCondition.from(hour?.sky, forecast.now.data.precipitationType))
            } else {
                region.name + " · " + stringResource(R.string.region_cached_weather_missing)
            },
            style = MaterialTheme.typography.bodySmall,
            color = WeatherTheme.colors.secondary
        )
        if (forecast?.now?.isStale == true) Text(stringResource(R.string.stale), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable private fun GroupHeading(title: String, action: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), style = WeatherTheme.type.groupTitle, color = WeatherTheme.colors.secondary)
        action?.invoke()
    }
}

@Composable private fun regionHeading(name: String): String {
    val parts = name.split(' ')
    val heading = (if (parts.size > 2) parts.dropLast(1) else parts).joinToString(" ")
    val full = stringArrayResource(R.array.region_full_names)
    val short = stringArrayResource(R.array.region_short_names)
    val first = heading.substringBefore(' ')
    val index = full.indexOf(first)
    return (if (index >= 0) short[index] else first) + heading.removePrefix(first)
}

@Composable private fun FlatRule(strong: Boolean = true) {
    androidx.compose.material3.HorizontalDivider(
        color = if (strong) WeatherTheme.colors.borderStrong else WeatherTheme.colors.border,
        thickness = Dims.hairline
    )
}
