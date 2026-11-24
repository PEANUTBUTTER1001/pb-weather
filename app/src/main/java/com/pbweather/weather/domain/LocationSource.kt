package com.pbweather.weather.domain

import javax.inject.Inject

enum class LocationProblem { PERMISSION_DENIED, DISABLED, TIMEOUT, UNAVAILABLE, OUTSIDE_COVERAGE }
class LocationException(val problem: LocationProblem) : Exception(problem.name)
interface LocationSource {
    suspend fun search(query: String): List<Region>
    suspend fun current(): Region
    fun permissionGranted(): Boolean
    fun serviceEnabled(): Boolean
}
class FindRegions @Inject constructor(private val locations: LocationSource) {
    suspend operator fun invoke(query: String) = locations.search(query)
}
class ResolveCurrentRegion @Inject constructor(private val locations: LocationSource) {
    suspend operator fun invoke() = locations.current()
}
