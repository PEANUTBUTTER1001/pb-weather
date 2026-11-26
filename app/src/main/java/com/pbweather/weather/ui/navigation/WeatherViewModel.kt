package com.pbweather.weather.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pbweather.weather.data.local.AppPreferences
import com.pbweather.weather.data.local.DeletedFavorite
import com.pbweather.weather.data.local.PreferencesRepository
import com.pbweather.weather.domain.AirQualityData
import com.pbweather.weather.domain.DailyObservation
import com.pbweather.weather.domain.DateRange
import com.pbweather.weather.domain.FindRegions
import com.pbweather.weather.domain.ForecastState
import com.pbweather.weather.domain.HistoryWindow
import com.pbweather.weather.domain.LocationException
import com.pbweather.weather.domain.LocationProblem
import com.pbweather.weather.domain.LocationSource
import com.pbweather.weather.domain.ObserveAirQuality
import com.pbweather.weather.domain.ObserveForecast
import com.pbweather.weather.domain.ObserveHistory
import com.pbweather.weather.domain.Region
import com.pbweather.weather.domain.Resource
import com.pbweather.weather.ui.theme.FontSize
import com.pbweather.weather.ui.theme.Palette
import com.pbweather.weather.ui.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

data class WeatherData(
    val key: String? = null,
    val forecast: ForecastState = ForecastState(),
    val air: Resource<AirQualityData> = Resource(),
    val history: Resource<List<DailyObservation>> = Resource()
)
data class LocationUi(val working: Boolean = false, val granted: Boolean = false, val problem: LocationProblem? = null)
data class WeatherUiState(
    val preferences: AppPreferences = AppPreferences(),
    val data: WeatherData = WeatherData(),
    val range: DateRange,
    val location: LocationUi = LocationUi(),
    val now: Long = 0,
    val preferencesLoaded: Boolean = true
)
enum class Notice { FAVORITE_LIMIT, DELETED, RESTORED, CACHE_CLEARED, CACHE_CLEAR_FAILED }

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val preferences: PreferencesRepository,
    private val forecast: ObserveForecast,
    private val air: ObserveAirQuality,
    private val history: ObserveHistory,
    private val findRegions: FindRegions,
    private val locations: LocationSource,
    private val clock: Clock,
    private val cache: com.pbweather.weather.data.local.CacheStore
) : ViewModel() {
    val widgetTab = MutableStateFlow<Pair<WeatherTab, Long>?>(null)
    fun openWidget() {
        viewModelScope.launch {
            val prefs = preferences.preferences.first()
            val primary = prefs.favorites.firstOrNull { it.id == prefs.primaryId }
            if (primary != null) preferences.select(primary)
            widgetTab.value = (if (primary == null) WeatherTab.LOCATIONS else WeatherTab.HOME) to ((widgetTab.value?.second ?: 0L) + 1L)
        }
    }
    val cacheBytes = cache.dao.observeCacheBytes().map<Long, Long?> {
        it
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val clearingCache = MutableStateFlow(false)
    fun clearCache() {
        if (clearingCache.value) return
        clearingCache.value = true
        viewModelScope.launch {
            try {
                refreshJob?.cancelAndJoin()
                cache.clearWeather()
                notices.emit(Notice.CACHE_CLEARED)
            } catch (
                cancelled: CancellationException
            ) {
                throw cancelled
            } catch (_: Exception) {
                notices.emit(Notice.CACHE_CLEAR_FAILED)
            } finally {
                clearingCache.value = false
            }
        }
    }
    val query = MutableStateFlow("")
    private val range = MutableStateFlow(HistoryWindow.recent(clock))
    private val location = MutableStateFlow(LocationUi(granted = locations.permissionGranted()))
    private val saved = preferences.preferences.stateIn(viewModelScope, SharingStarted.Eagerly, AppPreferences())
    private val selected = saved.map { it.selected }.distinctUntilChanged()
    val notices = MutableSharedFlow<Notice>()
    private val deleted = ArrayDeque<DeletedFavorite>()
    private var refreshJob: Job? = null
    private val weather = combine(selected, range) { region, dates -> region to dates }.flatMapLatest { (region, dates) ->
        if (region == null) {
            flowOf(WeatherData())
        } else {
            combine(
                forecast(region),
                air(region),
                history(region, HistoryWindow.recent(clock))
            ) { f, a, h -> WeatherData(region.key, f, a, h) }
        }
    }
    private val ticks = flow {
        while (true) {
            emit(clock.instant().toEpochMilli())
            val current = HistoryWindow.recent(clock)
            if (range.value.start < current.start || range.value.end > current.end) range.value = current
            delay(60000)
        }
    }
    val ui: StateFlow<WeatherUiState> = combine(preferences.preferences, weather, range, location, ticks) {
            prefs,
            data,
            dates,
            permission,
            now
        ->
        WeatherUiState(prefs, if (data.key == prefs.selected?.key) data else WeatherData(), dates, permission, now)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WeatherUiState(range = range.value, preferencesLoaded = false))
    val results = query.debounce(100).flatMapLatest { text -> flow { emit(findRegions(text)) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val locationForecasts: StateFlow<Map<String, ForecastState>> = combine(results, saved) { matches, prefs ->
        (matches + prefs.favorites).distinctBy { it.id }
    }.flatMapLatest { regions ->
        if (regions.isEmpty()) {
            flowOf(emptyMap())
        } else {
            combine(
                regions.map { region ->
                    forecast(region).map { region.id to it }
                }
            ) { entries -> entries.toMap() }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())
    val primaryForecast = saved.map { prefs -> prefs.favorites.firstOrNull { it.id == prefs.primaryId } }
        .distinctUntilChanged().flatMapLatest { region -> region?.let { forecast(it) } ?: flowOf(ForecastState()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ForecastState())
    init {
        viewModelScope.launch { locations.prepare() }
        viewModelScope.launch {
            combine(selected, range) { a, b -> a to b }.collect { (region, _) ->
                if (region != null) refresh()
            }
        }
    }
    fun refresh() {
        val region = saved.value.selected ?: return
        val dates = HistoryWindow.recent(clock)
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            supervisorScope {
                listOf(
                    async {
                        forecast.refresh(region)
                    },
                    async { air.refresh(region) },
                    async { history.refresh(region, dates) }
                ).awaitAll()
            }
        }
    }
    fun select(region: Region) {
        viewModelScope.launch { preferences.select(region) }
    }
    fun search(text: String) {
        query.value = text.take(30)
    }
    fun setRange(value: DateRange) {
        if (HistoryWindow.validate(value.start.toString(), value.end.toString(), clock).error == null) range.value = value
    }
    fun validateRange(start: String, end: String) = HistoryWindow.validate(start, end, clock)
    fun currentWindow() = HistoryWindow.recent(clock)
    fun updatePermission() {
        location.value = location.value.copy(granted = locations.permissionGranted())
    }
    fun locate() {
        viewModelScope.launch {
            location.value = LocationUi(working = true, granted = locations.permissionGranted())
            try {
                val region = locations.current()
                preferences.select(region)
                location.value = LocationUi(granted = true)
            } catch (error: LocationException) {
                location.value = LocationUi(granted = locations.permissionGranted(), problem = error.problem)
            } finally {
                location.value = location.value.copy(working = false)
            }
        }
    }
    fun favorite(region: Region) {
        viewModelScope.launch {
            if (saved.value.favorites.size >= 10 && saved.value.favorites.none { it.id == region.id }) {
                notices.emit(Notice.FAVORITE_LIMIT)
            } else {
                preferences.addFavorite(region)
            }
        }
    }
    fun deleteFavorite(id: String) {
        viewModelScope.launch {
            preferences.removeFavorite(id)?.let {
                deleted.addLast(it)
                notices.emit(Notice.DELETED)
            }
        }
    }
    fun undo() {
        viewModelScope.launch {
            deleted.lastOrNull()?.let {
                if (preferences.restore(it)) {
                    deleted.removeLast()
                    notices.emit(Notice.RESTORED)
                } else {
                    notices.emit(Notice.FAVORITE_LIMIT)
                }
            }
        }
    }
    fun primary(id: String) {
        viewModelScope.launch { preferences.setPrimary(id) }
    }
    fun theme(value: ThemeMode) {
        viewModelScope.launch { preferences.update { it.copy(theme = value.name) } }
    }
    fun palette(value: Palette) {
        viewModelScope.launch { preferences.update { it.copy(palette = value.name) } }
    }
    fun font(value: FontSize) {
        viewModelScope.launch { preferences.update { it.copy(font = value.name) } }
    }
}
