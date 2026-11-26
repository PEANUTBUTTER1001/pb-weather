package com.pbweather.weather

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class WeatherApp : Application() {
    @javax.inject.Inject lateinit var widgetSync: com.pbweather.weather.work.WidgetSync
    override fun onCreate() {
        super.onCreate()
        widgetSync.start()
    }
}
