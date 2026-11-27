package com.pbweather.weather.work

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.pbweather.weather.data.local.PreferencesRepository
import com.pbweather.weather.data.repository.WidgetRepository
import com.pbweather.weather.widget.WeatherWidget
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Singleton
class WidgetSync @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: PreferencesRepository,
    private val widgets: WidgetRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val started = AtomicBoolean(false)
    fun start() {
        if (!started.compareAndSet(false, true)) return
        scope.launch { WidgetScheduler.ensure(context) }
        scope.launch {
            preferences.preferences.map { prefs -> prefs.favorites.firstOrNull { it.id == prefs.primaryId } }
                .distinctUntilChanged().catch { }.collect { region ->
                    preferences.update { it.copy(widgetError = false) }
                    WeatherWidget().updateAll(context)
                    if (region != null) WidgetScheduler.refresh(context)
                }
        }
        scope.launch {
            widgets.observe().distinctUntilChanged().catch { }.collect { WeatherWidget().updateAll(context) }
        }
    }
}
