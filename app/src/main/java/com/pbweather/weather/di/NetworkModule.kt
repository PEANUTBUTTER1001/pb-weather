package com.pbweather.weather.di

import com.pbweather.weather.BuildConfig
import com.pbweather.weather.core.time.ReleaseTimeCalculator
import com.pbweather.weather.data.local.PreferencesRepository
import com.pbweather.weather.data.remote.ApiClient
import com.pbweather.weather.data.remote.PersistentQuotaRegistry
import com.pbweather.weather.data.remote.PublicDataApi
import com.pbweather.weather.data.remote.QuotaRegistry
import com.pbweather.weather.data.remote.ServiceKeyInterceptor
import com.pbweather.weather.data.repository.ForecastRepository
import com.pbweather.weather.domain.ForecastSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.Call
import okhttp3.EventListener
import okhttp3.OkHttpClient
import retrofit2.Retrofit

@Singleton
class ApiTraffic @Inject constructor() {
    private val bytes = AtomicLong()
    fun add(count: Long) {
        bytes.addAndGet(count)
    }
    fun byteCount(): Long = bytes.get()
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides @Singleton
    fun client(traffic: ApiTraffic): OkHttpClient = OkHttpClient.Builder()
        .callTimeout(10, TimeUnit.SECONDS).connectTimeout(10, TimeUnit.SECONDS).readTimeout(10, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false)
        .addInterceptor(ServiceKeyInterceptor(BuildConfig.KMA_SERVICE_KEY, BuildConfig.AIRKOREA_SERVICE_KEY))
        .eventListener(object : EventListener() {
            override fun responseBodyEnd(call: Call, byteCount: Long) {
                traffic.add(byteCount)
            }
            override fun requestBodyEnd(call: Call, byteCount: Long) {
                traffic.add(byteCount)
            }
        }).build()

    @Provides @Singleton
    fun api(client: OkHttpClient): PublicDataApi = Retrofit.Builder()
        .baseUrl("https://apis.data.go.kr/").client(client).build().create(PublicDataApi::class.java)

    @Provides @Singleton
    fun quota(prefs: PreferencesRepository, clock: Clock): QuotaRegistry = PersistentQuotaRegistry(prefs, clock)

    @Provides @Singleton
    fun releases(clock: Clock) = ReleaseTimeCalculator(clock)

    @Provides @Singleton
    fun forecasts(repo: ForecastRepository): ForecastSource = repo
}
