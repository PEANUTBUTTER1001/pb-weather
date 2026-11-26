package com.pbweather.weather.work

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.pbweather.weather.di.DataEntryPoint
import com.pbweather.weather.widget.WeatherWidget
import dagger.hilt.android.EntryPointAccessors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException

class WeatherRefreshWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = try {
        EntryPointAccessors.fromApplication(applicationContext, DataEntryPoint::class.java).widgets().refreshPrimary()
        WeatherWidget().updateAll(applicationContext)
        Result.success()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        Result.failure()
    }
}

object WidgetScheduler {
    const val PERIODIC_NAME = "weather-widget-hourly"
    private fun constraints() = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
    fun ensure(context: Context) = WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        PERIODIC_NAME,
        ExistingPeriodicWorkPolicy.KEEP,
        PeriodicWorkRequestBuilder<WeatherRefreshWorker>(60, TimeUnit.MINUTES).setConstraints(constraints()).build()
    )
    fun refresh(context: Context) = WorkManager.getInstance(context).enqueueUniqueWork(
        "weather-widget-primary",
        ExistingWorkPolicy.REPLACE,
        OneTimeWorkRequestBuilder<WeatherRefreshWorker>().setConstraints(constraints()).build()
    )
}
