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
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.semantics.selected
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
import com.pbweather.weather.ui.history.HistoryScreen
import com.pbweather.weather.ui.home.HomeScreen
import com.pbweather.weather.ui.home.currentSky
import com.pbweather.weather.ui.locations.LocationsScreen
import com.pbweather.weather.ui.permission.LocationPermissionAction
import com.pbweather.weather.ui.settings.SettingsScreen
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
    LOCATIONS(R.string.locations, UiIcon.SEARCH),
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
    val range: (com.pbweather.weather.domain.DateRange) -> Unit = {},
    val favorite: (Region) -> Unit = {},
    val deleteFavorite: (String) -> Unit = {},
    val primary: (String) -> Unit = {},
    val clearCache: () -> Unit = {},
    val refreshWidget: () -> Unit = {},
    val openWidget: () -> Unit = {},
    val weatherMotion: (Boolean) -> Unit = {}
)

@Composable
fun WeatherShell(
    state: WeatherUiState,
    query: String,
    results: List<Region>,
    mode: ThemeMode,
    palette: Palette,
    actions: WeatherActions,
    notices: Flow<Notice>,
    regionForecasts: Map<String, com.pbweather.weather.domain.ForecastState> = emptyMap(),
    widgetTab: Pair<WeatherTab, Long>? = null,
    cacheBytes: Long? = null,
    clearingCache: Boolean = false,
    primaryForecast: com.pbweather.weather.domain.ForecastState = com.pbweather.weather.domain.ForecastState()
) {
    var userNavigated by rememberSaveable { mutableStateOf(false) }
    var showSettingsLocation by rememberSaveable { mutableStateOf(false) }
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
    var wasEmpty by rememberSaveable { mutableStateOf(state.preferences.selected == null) }
    var lastWidgetRequest by rememberSaveable { mutableLongStateOf(-1L) }
    LaunchedEffect(state.preferences.selected?.id, state.preferencesLoaded, widgetTab) {
        if (!state.preferencesLoaded) return@LaunchedEffect
        if (widgetTab != null && widgetTab.second != lastWidgetRequest) {
            tab = widgetTab.first
            details = null
            lastWidgetRequest = widgetTab.second
        } else if (state.preferences.selected == null && !userNavigated) {
            tab = WeatherTab.LOCATIONS
        } else if (wasEmpty && !userNavigated) {
            tab = WeatherTab.HOME
        }
        wasEmpty = state.preferences.selected == null
    }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) actions.permissionChanged()
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    val deleted = stringResource(R.string.favorite_deleted)
    val restored = stringResource(R.string.favorite_restored)
    val limit = stringResource(R.string.favorite_limit)
    val undo = stringResource(R.string.undo)
    val cleared = stringResource(R.string.cache_cleared)
    val clearFailed = stringResource(R.string.cache_clear_failed)
    LaunchedEffect(notices) {
        notices.collectLatest { notice ->
            val message = when (notice) {
                Notice.DELETED -> deleted
                Notice.RESTORED -> restored
                Notice.CACHE_CLEARED -> cleared
                Notice.CACHE_CLEAR_FAILED -> clearFailed
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
        userNavigated = true
        focus.clearFocus()
        actions.select(region)
        tab = WeatherTab.HOME
    }
    val loading =
        state.data.forecast.let { it.now.isRefreshing || it.hourly.isRefreshing || it.weekly.isRefreshing } || state.data.air.isRefreshing
    Scaffold(
        containerColor = WeatherTheme.colors.ground,
        topBar = {
            val background = if (tab == WeatherTab.HOME && state.preferences.selected != null &&
                (
                    !state.data.forecast.now.isOffline || state.data.forecast.now.data != null ||
                        !state.data.forecast.hourly.data?.hours.isNullOrEmpty() ||
                        !state.data.forecast.weekly.data.isNullOrEmpty()
                    )
            ) {
                heroColor(mode, palette, currentSky(state))
            } else {
                WeatherTheme.colors.ground
            }
            Column(Modifier.background(background).statusBarsPadding()) {
                Row(
                    Modifier.fillMaxWidth().heightIn(min = Dims.topBar).padding(horizontal = Dims.gutter),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    run {
                        Row(
                            Modifier.weight(1f).clickable {
                                userNavigated = true
                                tab = WeatherTab.LOCATIONS
                            }.padding(vertical = Dims.small),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Dims.badgeInset)
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
                        if (tab == WeatherTab.SETTINGS) {
                            Text(
                                stringResource(R.string.settings),
                                Modifier.padding(end = Dims.tiny),
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                        Row(
                            Modifier.offset(x = Dims.small),
                            horizontalArrangement = Arrangement.spacedBy(Dims.tiny),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = {
                                details = "help"
                            }, modifier = Modifier.size(Dims.action)) {
                                AppIcon(UiIcon.HELP, stringResource(R.string.help))
                            }
                            IconButton(
                                onClick = actions.refresh,
                                enabled = state.preferences.selected != null && !loading,
                                modifier = Modifier.size(Dims.action)
                            ) {
                                AppIcon(UiIcon.REFRESH, stringResource(R.string.refresh))
                            }
                        }
                    }
                }
                if (loading && state.preferences.selected != null &&
                    (
                        state.data.forecast.now.data != null || !state.data.forecast.hourly.data?.hours.isNullOrEmpty() ||
                            !state.data.forecast.weekly.data.isNullOrEmpty()
                        )
                ) {
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
                            Modifier.weight(1f).clickable {
                                userNavigated = true
                                showSettingsLocation = false
                                tab = item
                            }.semantics {
                                contentDescription =
                                    description
                                selected = tab == item
                            }
                                .heightIn(min = Dims.navBar).padding(vertical = Dims.tiny),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                Modifier.background(
                                    if (tab == item) WeatherTheme.colors.accentSoft else WeatherTheme.colors.surface,
                                    androidx.compose.foundation.shape.RoundedCornerShape(Dims.pill)
                                ).then(
                                    if (mode == ThemeMode.HIGH_CONTRAST &&
                                        tab == item
                                    ) {
                                        Modifier.border(
                                            Dims.strong,
                                            WeatherTheme.colors.accent,
                                            androidx.compose.foundation.shape.RoundedCornerShape(Dims.pill)
                                        )
                                    } else {
                                        Modifier
                                    }
                                ).size(Dims.navPillWidth, Dims.navPillHeight),
                                contentAlignment = Alignment.Center
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
                if (!state.preferencesLoaded) {
                    Text(stringResource(R.string.loading), Modifier.padding(Dims.gutter))
                } else {
                    when (tab) {
                        WeatherTab.HOME -> if (state.preferences.selected ==
                            null
                        ) {
                            LocationsScreen(state, query, results, regionForecasts, actions, pick, {
                                showSettingsLocation = true
                                tab =
                                    WeatherTab.SETTINGS
                            }, locationsList)
                        } else {
                            HomeScreen(
                                state, mode, palette, homeList, hoursList, selectedHour.takeIf { it != 0L },
                                { selectedHour = it }, { details = it }, { tab = WeatherTab.HISTORY }, actions.refresh,
                                onSelectRegion = { tab = WeatherTab.LOCATIONS }
                            )
                        }
                        WeatherTab.LOCATIONS -> LocationsScreen(state, query, results, regionForecasts, actions, pick, {
                            showSettingsLocation =
                                true
                            tab = WeatherTab.SETTINGS
                        }, locationsList)
                        WeatherTab.HISTORY -> HistoryScreen(state, actions.range, actions.refresh) { tab = WeatherTab.LOCATIONS }
                        WeatherTab.SETTINGS -> SettingsScreen(
                            state, cacheBytes, clearingCache, primaryForecast, actions,
                            {
                                showSettingsLocation = false
                                tab = WeatherTab.LOCATIONS
                            }, openSettings,
                            onDocument = { details = it }, showLocation = showSettingsLocation
                        )
                    }
                }
            }
        }
    }
    details?.let { target ->
        if (target in setOf("help", "privacy", "licenses", "usage")) {
            com.pbweather.weather.ui.settings.LegalDocSheet(target) { details = null }
        } else {
            WeatherDetails(target, state, onNavigate = { next ->
                details = next
                next.removePrefix("hour:").toLongOrNull()?.let { selectedHour = it }
            }) { details = null }
        }
    }
}
