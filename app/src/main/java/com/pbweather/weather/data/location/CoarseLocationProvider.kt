package com.pbweather.weather.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.pbweather.weather.domain.LocationException
import com.pbweather.weather.domain.LocationProblem
import com.pbweather.weather.domain.Region
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

@Singleton
class CoarseLocationProvider @Inject constructor(@param:ApplicationContext private val context: Context) {
    fun permissionGranted() = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
    fun serviceEnabled() = LocationManagerCompat.isLocationEnabled(context.getSystemService(LocationManager::class.java))

    @SuppressLint("MissingPermission")
    suspend fun resolve(resolver: RegionResolver): Region {
        if (!permissionGranted()) throw LocationException(LocationProblem.PERMISSION_DENIED)
        if (!serviceEnabled()) throw LocationException(LocationProblem.DISABLED)
        val request = CurrentLocationRequest.Builder().setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
            .setGranularity(Granularity.GRANULARITY_COARSE).setMaxUpdateAgeMillis(120000).setDurationMillis(15000).build()
        return withTimeoutOrNull(16000) {
            suspendCancellableCoroutine { continuation ->
                val token = CancellationTokenSource()
                continuation.invokeOnCancellation { token.cancel() }
                try {
                    LocationServices.getFusedLocationProviderClient(context).getCurrentLocation(request, token.token)
                        .addOnSuccessListener { location ->
                            if (!continuation.isActive) return@addOnSuccessListener
                            if (location == null) {
                                continuation.resumeWithException(LocationException(LocationProblem.UNAVAILABLE))
                            } else {
                                try {
                                    continuation.resume(resolver.current(location.latitude, location.longitude))
                                } catch (error: LocationException) {
                                    continuation.resumeWithException(error)
                                }
                            }
                        }.addOnFailureListener { error ->
                            if (continuation.isActive) {
                                continuation.resumeWithException(
                                    LocationException(
                                        if (error is SecurityException) LocationProblem.PERMISSION_DENIED else LocationProblem.UNAVAILABLE
                                    )
                                )
                            }
                        }
                } catch (_: SecurityException) {
                    if (continuation.isActive) continuation.resumeWithException(LocationException(LocationProblem.PERMISSION_DENIED))
                }
            }
        } ?: throw LocationException(LocationProblem.TIMEOUT)
    }
}
