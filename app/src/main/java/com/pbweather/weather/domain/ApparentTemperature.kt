package com.pbweather.weather.domain

import com.pbweather.weather.core.time.ReleaseTimeCalculator
import java.time.Instant
import kotlin.math.atan
import kotlin.math.pow
import kotlin.math.sqrt

enum class ApparentStatus { AVAILABLE, MISSING, OUTSIDE_RANGE, INPUT_SPEC_UNCONFIRMED }
data class ApparentResult(val status: ApparentStatus, val celsius: Double? = null, val summer: Boolean)
object ApparentTemperature {
    /** Seasonal calculated index. Winter requires an explicitly confirmed matching wind mean interval. */
    fun calculate(weather: ObservedWeather?, observedAt: Long?, windMeanConfirmed: Boolean = false): ApparentResult {
        val month = observedAt?.let { Instant.ofEpochMilli(it).atZone(ReleaseTimeCalculator.KST).monthValue }
        val summer = month != null && month in 5..9
        val temperature = weather?.temperature?.takeIf { it.isFinite() }
        if (temperature == null || month == null) return ApparentResult(ApparentStatus.MISSING, summer = summer)
        if (summer) {
            val humidity = weather.humidity?.takeIf { it.isFinite() }
                ?: return ApparentResult(ApparentStatus.MISSING, summer = true)
            if (temperature !in 0.0..50.0 || humidity !in 5.0..99.0) {
                return ApparentResult(ApparentStatus.OUTSIDE_RANGE, summer = true)
            }
            val wetBulb = temperature * atan(0.151977 * sqrt(humidity + 8.313659)) + atan(temperature + humidity) -
                atan(humidity - 1.676331) + 0.00391838 * humidity.pow(1.5) * atan(0.023101 * humidity) - 4.686035
            val result = -0.2442 + 0.55399 * wetBulb + 0.45535 * temperature - 0.0022 * wetBulb.pow(2) +
                0.00278 * wetBulb * temperature + 3.0
            return ApparentResult(ApparentStatus.AVAILABLE, result, true)
        }
        val wind = weather.windMs?.takeIf { it.isFinite() && it >= 0 }
            ?: return ApparentResult(ApparentStatus.MISSING, summer = false)
        if (temperature > 10 || wind < 1.3) return ApparentResult(ApparentStatus.OUTSIDE_RANGE, summer = false)
        if (!windMeanConfirmed) return ApparentResult(ApparentStatus.INPUT_SPEC_UNCONFIRMED, summer = false)
        val speed = (wind * 3.6).pow(0.16)
        return ApparentResult(ApparentStatus.AVAILABLE, 13.12 + 0.6215 * temperature - 11.37 * speed + 0.3965 * temperature * speed, false)
    }
}
