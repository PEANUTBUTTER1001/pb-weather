package com.pbweather.weather.domain

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

interface HistorySource {
    fun observe(region: Region, range: DateRange): Flow<Resource<List<DailyObservation>>>
    suspend fun refresh(region: Region, range: DateRange)
}
class ObserveHistory @Inject constructor(private val source: HistorySource) {
    operator fun invoke(region: Region, range: DateRange) = source.observe(region, range)
    suspend fun refresh(region: Region, range: DateRange) = source.refresh(region, range)
}
