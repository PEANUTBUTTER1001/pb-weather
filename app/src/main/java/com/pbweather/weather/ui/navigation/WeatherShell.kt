package com.pbweather.weather.ui.navigation

import android.app.Activity
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pbweather.weather.R
import com.pbweather.weather.domain.LocationProblem
import com.pbweather.weather.domain.Region
import com.pbweather.weather.ui.components.AppIcon
import com.pbweather.weather.ui.components.Rule
import com.pbweather.weather.ui.components.SectionCard
import com.pbweather.weather.ui.components.SectionTitle
import com.pbweather.weather.ui.components.UiIcon
import com.pbweather.weather.ui.components.WeatherDetails
import com.pbweather.weather.ui.components.temperature
import com.pbweather.weather.ui.home.HomeScreen
import com.pbweather.weather.ui.home.currentSky
import com.pbweather.weather.ui.permission.LocationPermissionAction
import com.pbweather.weather.ui.theme.Dims
import com.pbweather.weather.ui.theme.FontSize
import com.pbweather.weather.ui.theme.Palette
import com.pbweather.weather.ui.theme.ThemeMode
import com.pbweather.weather.ui.theme.WeatherTheme
import com.pbweather.weather.ui.theme.heroColor
import com.pbweather.weather.ui.theme.snackbarActionColor
import com.pbweather.weather.ui.theme.snackbarColor
import com.pbweather.weather.ui.theme.snackbarTextColor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withTimeoutOrNull

enum class WeatherTab(val title: Int, val icon: UiIcon) {
    HOME(R.string.home, UiIcon.HOME),
    HISTORY(R.string.history, UiIcon.HISTORY),
    LOCATIONS(R.string.locations, UiIcon.PIN),
    SETTINGS(R.string.settings, UiIcon.SETTINGS)
}
data class WeatherActions(
    val search: (String) -> Unit = {},
    val select: (Region) -> Unit = {},
    val refresh: () -> Unit = {},
    val locate: () -> Unit = {},
    val permissionChanged: () -> Unit = {},
    val undo: () -> Unit = {},
    val theme: (ThemeMode) -> Unit = {},
    val palette: (Palette) -> Unit = {},
    val font: (FontSize) -> Unit = {},
    val range: (com.pbweather.weather.domain.DateRange) -> Unit = {}
)

@Composable
fun WeatherShell(
    state: WeatherUiState,
    query: String,
    results: List<Region>,
    mode: ThemeMode,
    palette: Palette,
    actions: WeatherActions,
    notices: Flow<Notice>
) {
    var tab by rememberSaveable { mutableStateOf(WeatherTab.HOME) }
    var details by rememberSaveable(state.preferences.selected?.key) { mutableStateOf<String?>(null) }
    var selectedHour by rememberSaveable(state.preferences.selected?.key) { mutableLongStateOf(0L) }
    val homeList = rememberLazyListState()
    val hoursList = rememberLazyListState()
    val locationsList = rememberLazyListState()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val activity = remember(context) {
        var current = context
        while (current is ContextWrapper && current !is Activity) current = current.baseContext
        current as? Activity
    }
    SideEffect {
        activity?.let {
            val controller = WindowCompat.getInsetsController(it.window, it.window.decorView)
            controller.isAppearanceLightStatusBars = mode == ThemeMode.LIGHT && details == null
            controller.isAppearanceLightNavigationBars = mode == ThemeMode.LIGHT
        }
    }
    val focus = LocalFocusManager.current
    val owner = LocalLifecycleOwner.current
    var resumed by remember { mutableStateOf(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    var wasEmpty by remember { mutableStateOf(true) }
    LaunchedEffect(state.preferences.selected?.id) {
        if (state.preferences.selected == null) {
            tab = WeatherTab.LOCATIONS
        } else if (wasEmpty) {
            tab = WeatherTab.HOME
        }
        wasEmpty = state.preferences.selected == null
    }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            resumed = owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
            if (event == Lifecycle.Event.ON_RESUME) actions.permissionChanged()
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    val deleted = stringResource(R.string.favorite_deleted)
    val restored = stringResource(R.string.favorite_restored)
    val limit = stringResource(R.string.favorite_limit)
    val undo = stringResource(R.string.undo)
    LaunchedEffect(notices) {
        notices.collectLatest { notice ->
            val message = when (notice) {
                Notice.DELETED -> deleted
                Notice.RESTORED -> restored
                else -> limit
            }
            val result = withTimeoutOrNull(6000) {
                snackbar.showSnackbar(
                    message,
                    actionLabel = if (notice == Notice.DELETED) undo else null,
                    duration = SnackbarDuration.Indefinite
                )
            }
            if (result == SnackbarResult.ActionPerformed) actions.undo()
            snackbar.currentSnackbarData?.dismiss()
        }
    }
    val openSettings = {
        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)))
    }
    val pick: (Region) -> Unit = { region ->
        focus.clearFocus()
        actions.select(region)
        tab = WeatherTab.HOME
    }
    val loading =
        state.data.forecast.let { it.now.isRefreshing || it.hourly.isRefreshing || it.weekly.isRefreshing } || state.data.air.isRefreshing
    Scaffold(
        containerColor = WeatherTheme.colors.ground,
        topBar = {
            val background = if (tab == WeatherTab.HOME && state.preferences.selected != null) {
                heroColor(mode, palette, currentSky(state))
            } else {
                WeatherTheme.colors.ground
            }
            Column(Modifier.background(background).statusBarsPadding()) {
                Row(
                    Modifier.fillMaxWidth().heightIn(min = Dims.topBar).padding(horizontal = Dims.gutter),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (tab == WeatherTab.SETTINGS) {
                        Text(stringResource(R.string.settings), style = MaterialTheme.typography.titleLarge)
                    } else {
                        Row(
                            Modifier.weight(1f).clickable { tab = WeatherTab.LOCATIONS }.padding(vertical = Dims.small),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Dims.small)
                        ) {
                            AppIcon(UiIcon.PIN, null)
                            Column(Modifier.weight(1f)) {
                                Text(
                                    state.preferences.selected?.name ?: stringResource(R.string.select_region),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = WeatherTheme.type.location
                                )
                                val region = state.preferences.selected
                                Text(
                                    stringResource(
                                        when {
                                            region?.usingCurrentLocation == true -> R.string.current_location
                                            region?.id == state.preferences.primaryId && region != null -> R.string.primary_location
                                            state.preferences.favorites.any { it.id == region?.id } -> R.string.favorite_location
                                            else -> R.string.manual_location
                                        }
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = WeatherTheme.colors.secondary
                                )
                            }
                        }
                        IconButton(onClick = {
                            details = "help"
                        }, modifier = Modifier.offset(x = Dims.section)) { AppIcon(UiIcon.HELP, stringResource(R.string.help)) }
                        IconButton(
                            onClick = actions.refresh,
                            enabled = state.preferences.selected != null && !loading,
                            modifier = Modifier.offset(x = Dims.section)
                        ) {
                            AppIcon(UiIcon.REFRESH, stringResource(R.string.refresh))
                        }
                    }
                }
                if (loading && state.preferences.selected != null) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                } else if (tab != WeatherTab.HOME) {
                    Rule()
                }
            }
        },
        bottomBar = {
            Column(Modifier.background(WeatherTheme.colors.surface).navigationBarsPadding()) {
                Rule()
                Row(Modifier.fillMaxWidth().heightIn(min = Dims.navBar)) {
                    WeatherTab.entries.forEach { item ->
                        val title = stringResource(item.title)
                        val description = stringResource(R.string.nav_description, title)
                        Column(
                            Modifier.weight(1f).clickable { tab = item }.semantics { contentDescription = description }
                                .padding(vertical = Dims.small),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                Modifier.background(
                                    if (tab == item) WeatherTheme.colors.accentSoft else WeatherTheme.colors.surface,
                                    androidx.compose.foundation.shape.RoundedCornerShape(Dims.pill)
                                ).padding(horizontal = Dims.gutter, vertical = Dims.tiny)
                            ) {
                                AppIcon(
                                    item.icon,
                                    null,
                                    tint = if (tab ==
                                        item
                                    ) {
                                        WeatherTheme.colors.accentOnSoft
                                    } else {
                                        WeatherTheme.colors.secondary
                                    }
                                )
                            }
                            Text(title, style = WeatherTheme.type.meta)
                        }
                    }
                }
            }
        },
        snackbarHost = {
            SnackbarHost(snackbar) { data ->
                val shape = androidx.compose.foundation.shape.RoundedCornerShape(Dims.small)
                val border = if (mode ==
                    ThemeMode.HIGH_CONTRAST
                ) {
                    Modifier.border(Dims.strong, WeatherTheme.colors.accent, shape)
                } else {
                    Modifier
                }
                Snackbar(
                    snackbarData = data,
                    modifier = border,
                    containerColor = snackbarColor(mode),
                    contentColor = snackbarTextColor(),
                    actionColor = snackbarActionColor(mode, palette)
                )
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Box(Modifier.widthIn(max = Dims.maxWidth).fillMaxWidth()) {
                when (tab) {
                    WeatherTab.HOME -> if (state.preferences.selected ==
                        null
                    ) {
                        RegionLanding(state, query, results, actions, pick, openSettings, locationsList)
                    } else {
                        HomeScreen(
                            state, mode, palette, homeList, hoursList, resumed, selectedHour.takeIf { it != 0L },
                            { selectedHour = it }, { details = it }, { tab = WeatherTab.HISTORY }, actions.refresh
                        )
                    }
                    WeatherTab.LOCATIONS -> RegionLanding(state, query, results, actions, pick, openSettings, locationsList)
                    WeatherTab.HISTORY -> com.pbweather.weather.ui.history.HistoryScreen(state, actions.range, actions.refresh) {
                        tab =
                            WeatherTab.LOCATIONS
                    }
                    WeatherTab.SETTINGS -> SettingsLanding(state, actions) { details = "help" }
                }
            }
        }
    }
    details?.let {
        WeatherDetails(it, state, onNavigate = { target ->
            details = target
            target.removePrefix("hour:").toLongOrNull()?.let { hour -> selectedHour = hour }
        }) { details = null }
    }
}

@Composable private fun RegionLanding(
    state: WeatherUiState,
    query: String,
    results: List<Region>,
    actions: WeatherActions,
    onSelect: (Region) -> Unit,
    onSettings: () -> Unit,
    list: androidx.compose.foundation.lazy.LazyListState
) {
    LazyColumn(
        state = list,
        modifier = Modifier.padding(horizontal = Dims.gutter),
        verticalArrangement = Arrangement.spacedBy(Dims.medium)
    ) {
        item {
            SectionTitle(stringResource(R.string.search_region)) {
                Text(stringResource(R.string.offline_search), style = MaterialTheme.typography.bodySmall)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(query, actions.search, Modifier.weight(1f), placeholder = { Text(stringResource(R.string.search_hint)) })
                if (query.isNotEmpty()) TextButton(onClick = { actions.search("") }) { Text(stringResource(R.string.clear)) }
            }
            Text(
                stringResource(
                    if (query.length < 2) {
                        R.string.search_minimum
                    } else if (results.isEmpty()) {
                        R.string.search_empty
                    } else {
                        R.string.offline_search
                    }
                ),
                style = MaterialTheme.typography.bodySmall,
                color = WeatherTheme.colors.secondary
            )
        }
        items(results, key = { it.id }) { region ->
            Column(
                Modifier.fillMaxWidth().heightIn(min = Dims.touch).clickable { onSelect(region) }.padding(vertical = Dims.medium)
                    .semantics { contentDescription = region.name }
            ) {
                Text(region.name, style = MaterialTheme.typography.titleMedium)
                Rule()
            }
        }
        item {
            SectionTitle(stringResource(R.string.current_location))
            Text(stringResource(R.string.choose_without_location), style = MaterialTheme.typography.bodySmall)
            state.location.problem?.let {
                Text(
                    stringResource(
                        when (it) {
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
        }
        if (state.preferences.favorites.isNotEmpty()) {
            item {
                SectionTitle(stringResource(R.string.favorites))
                state.preferences.favorites.forEach { region ->
                    TextButton(onClick = { onSelect(region) }) { Text(region.name) }
                }
            }
        }
    }
}

@Composable private fun SettingsLanding(state: WeatherUiState, actions: WeatherActions, onHelp: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(Dims.gutter), verticalArrangement = Arrangement.spacedBy(Dims.medium)) {
        SectionCard {
            SectionTitle(stringResource(R.string.settings))
            ThemeMode.entries.forEach { mode ->
                TextButton(onClick = { actions.theme(mode) }) {
                    Text(
                        stringResource(
                            when (mode) {
                                ThemeMode.LIGHT -> R.string.theme_light
                                ThemeMode.DARK -> R.string.theme_dark
                                ThemeMode.HIGH_CONTRAST -> R.string.theme_contrast
                            }
                        )
                    )
                }
            }
            TextButton(onClick = onHelp) { Text(stringResource(R.string.help)) }
        }
    }
}
