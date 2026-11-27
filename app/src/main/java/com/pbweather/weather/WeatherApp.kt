package com.pbweather.weather

import android.app.Application
import com.pbweather.weather.work.WidgetSync
import dagger.Lazy
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class WeatherApp : Application() {
    @Inject lateinit var widgetSync: Lazy<WidgetSync>
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        appScope.launch { widgetSync.get().start() }
    }
}
