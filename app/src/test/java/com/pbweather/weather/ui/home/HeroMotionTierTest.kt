package com.pbweather.weather.ui.home

import com.pbweather.weather.data.local.AppPreferences
import com.pbweather.weather.domain.SkyCondition
import com.pbweather.weather.ui.theme.ThemeMode
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HeroMotionTierTest {
    private fun baseSignals() = HeroMotionSignals(
        sdkInt = 35,
        mode = ThemeMode.LIGHT,
        sky = SkyCondition.CLEAR,
        userEnabled = true,
        animatorScale = 1f,
        powerSave = false,
        lowRam = false,
        totalMemoryMb = 12288L,
        cpuCores = 8
    )

    @Test fun resolveReturnsStaticForRestrictedOrUnsupportedConditions() {
        val base = baseSignals()
        assertEquals(HeroMotionTier.STATIC, HeroMotionPolicy.resolve(base.copy(sdkInt = 32)))
        assertEquals(HeroMotionTier.STATIC, HeroMotionPolicy.resolve(base.copy(mode = ThemeMode.HIGH_CONTRAST)))
        assertEquals(HeroMotionTier.STATIC, HeroMotionPolicy.resolve(base.copy(sky = SkyCondition.UNKNOWN)))
        assertEquals(HeroMotionTier.STATIC, HeroMotionPolicy.resolve(base.copy(userEnabled = false)))
        assertEquals(HeroMotionTier.STATIC, HeroMotionPolicy.resolve(base.copy(animatorScale = 0f)))
        assertEquals(HeroMotionTier.STATIC, HeroMotionPolicy.resolve(base.copy(animatorScale = Float.NaN)))
        assertEquals(HeroMotionTier.STATIC, HeroMotionPolicy.resolve(base.copy(powerSave = true)))
        assertEquals(HeroMotionTier.STATIC, HeroMotionPolicy.resolve(base.copy(lowRam = true)))
    }

    @Test fun resolveSeparatesMediumBudgetAndHighFlagshipDevices() {
        val base = baseSignals()
        assertEquals(HeroMotionTier.HIGH, HeroMotionPolicy.resolve(base))
        assertEquals(HeroMotionTier.MEDIUM, HeroMotionPolicy.resolve(base.copy(totalMemoryMb = 4096L)))
        assertEquals(HeroMotionTier.MEDIUM, HeroMotionPolicy.resolve(base.copy(cpuCores = 4)))
    }

    @Test fun frameIntervalAndRenderScaleMatchTier() {
        assertEquals(16_666_667L, HeroMotionPolicy.frameIntervalNanos(HeroMotionTier.HIGH))
        assertEquals(33_333_333L, HeroMotionPolicy.frameIntervalNanos(HeroMotionTier.MEDIUM))
        assertEquals(Long.MAX_VALUE, HeroMotionPolicy.frameIntervalNanos(HeroMotionTier.STATIC))
        assertEquals(1.0f, HeroMotionPolicy.renderScale(HeroMotionTier.HIGH), 0.001f)
        assertEquals(0.5f, HeroMotionPolicy.renderScale(HeroMotionTier.MEDIUM), 0.001f)
        assertEquals(1.0f, HeroMotionPolicy.renderScale(HeroMotionTier.STATIC), 0.001f)
    }

    @Test fun normalizeWindDayPhaseAndWrapTimeHandleEdgeCases() {
        assertEquals(0f, HeroMotionPolicy.normalizeWind(null), 0.001f)
        assertEquals(0f, HeroMotionPolicy.normalizeWind(Double.NaN), 0.001f)
        assertEquals(0f, HeroMotionPolicy.normalizeWind(Double.POSITIVE_INFINITY), 0.001f)
        assertEquals(0f, HeroMotionPolicy.normalizeWind(-3.0), 0.001f)
        assertEquals(0.5f, HeroMotionPolicy.normalizeWind(10.0), 0.001f)
        assertEquals(1.0f, HeroMotionPolicy.normalizeWind(35.0), 0.001f)

        assertEquals(0f, HeroMotionPolicy.dayPhase(null), 0.001f)
        assertEquals(0f, HeroMotionPolicy.dayPhase(12), 0.001f)
        assertEquals(1f, HeroMotionPolicy.dayPhase(6), 0.001f)
        assertEquals(1f, HeroMotionPolicy.dayPhase(18), 0.001f)
        assertEquals(2f, HeroMotionPolicy.dayPhase(23), 0.001f)

        assertEquals(0f, HeroMotionPolicy.wrapTimeSeconds(-1f), 0.001f)
        assertEquals(0f, HeroMotionPolicy.wrapTimeSeconds(Float.NaN), 0.001f)
        assertEquals(125f, HeroMotionPolicy.wrapTimeSeconds(3725f), 0.001f)
    }

    @Test fun shaderFallbackCapturesFailureWithoutThrowing() {
        val failed = HeroShaderFallbackState<String> { error("invalid AGSL") }
        assertTrue(failed.failed)
        assertNull(failed.instance)

        val ok = HeroShaderFallbackState { "compiled" }
        assertFalse(ok.failed)
        assertNotNull(ok.instance)
    }

    @Test fun legacyAppPreferencesJsonDefaultsWeatherMotionEnabled() {
        val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            explicitNulls = false
        }
        val decoded = json.decodeFromString<AppPreferences>("""{"theme":"DARK"}""")
        assertEquals("DARK", decoded.theme)
        assertTrue(decoded.weatherMotion)
    }
}
