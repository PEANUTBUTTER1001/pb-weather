package com.pbweather.weather.ui.home

import com.pbweather.weather.domain.SkyCondition
import com.pbweather.weather.ui.theme.ThemeMode

/** Device and accessibility tier controlling hero background shader complexity. */
enum class HeroMotionTier { HIGH, MEDIUM, STATIC }

/** Snapshot of runtime device, accessibility, and preference inputs for tier selection. */
data class HeroMotionSignals(
    val sdkInt: Int,
    val mode: ThemeMode,
    val sky: SkyCondition,
    val userEnabled: Boolean,
    val animatorScale: Float,
    val powerSave: Boolean,
    val lowRam: Boolean,
    val totalMemoryMb: Long,
    val cpuCores: Int
)

/** Policy mapping device capabilities, accessibility constraints, and live weather inputs to shader uniforms. */
object HeroMotionPolicy {
    const val MIN_AGSL_SDK = 33
    const val LOW_TIER_MEMORY_MB = 4096L
    const val LOW_TIER_CORES = 4
    const val MAX_TIME_SECONDS = 3600f
    private const val MAX_WIND_MS = 20.0

    fun resolve(signals: HeroMotionSignals): HeroMotionTier {
        val disabledByEnvironment = signals.sdkInt < MIN_AGSL_SDK ||
            signals.mode == ThemeMode.HIGH_CONTRAST ||
            signals.sky == SkyCondition.UNKNOWN ||
            !signals.userEnabled ||
            !signals.animatorScale.isFinite() ||
            signals.animatorScale <= 0f ||
            signals.powerSave ||
            signals.lowRam
        if (disabledByEnvironment) return HeroMotionTier.STATIC
        val budgetHardware = (signals.totalMemoryMb in 1L..LOW_TIER_MEMORY_MB) ||
            (signals.cpuCores in 1..LOW_TIER_CORES)
        return if (budgetHardware) HeroMotionTier.MEDIUM else HeroMotionTier.HIGH
    }

    fun frameIntervalNanos(tier: HeroMotionTier): Long = when (tier) {
        HeroMotionTier.HIGH -> 16_666_667L
        HeroMotionTier.MEDIUM -> 33_333_333L
        HeroMotionTier.STATIC -> Long.MAX_VALUE
    }

    fun renderScale(tier: HeroMotionTier): Float = when (tier) {
        HeroMotionTier.MEDIUM -> 0.5f
        HeroMotionTier.HIGH, HeroMotionTier.STATIC -> 1.0f
    }

    fun normalizeWind(windMs: Double?): Float {
        if (windMs == null || !windMs.isFinite() || windMs <= 0.0) return 0f
        return (windMs.coerceIn(0.0, MAX_WIND_MS) / MAX_WIND_MS).toFloat()
    }

    fun dayPhase(hourOfDay: Int?): Float = when (hourOfDay) {
        null -> 0f
        in 7..16 -> 0f
        in 5..6, in 17..19 -> 1f
        else -> 2f
    }

    fun wrapTimeSeconds(seconds: Float): Float {
        if (!seconds.isFinite() || seconds <= 0f) return 0f
        return seconds % MAX_TIME_SECONDS
    }

    fun shaderModeIndex(sky: SkyCondition): Float = when (sky) {
        SkyCondition.CLEAR -> 0f
        SkyCondition.PARTLY -> 1f
        SkyCondition.CLOUDY -> 2f
        SkyCondition.RAIN -> 3f
        SkyCondition.SLEET -> 4f
        SkyCondition.SNOW -> 5f
        SkyCondition.UNKNOWN -> -1f
    }
}
