package com.pbweather.weather.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.pbweather.weather.domain.SkyCondition
import com.pbweather.weather.ui.theme.Palette
import com.pbweather.weather.ui.theme.ThemeMode
import com.pbweather.weather.ui.theme.WeatherTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HeroAnimationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun leavingHeroStopsFramesAndHighContrastHasStaticFallback() {
        compose.mainClock.autoAdvance = false
        val active = mutableStateOf(true)
        val mode = mutableStateOf(ThemeMode.LIGHT)
        compose.setContent {
            WeatherTheme(mode.value) {
                Box(Modifier.size(160.dp)) { HeroWeather(SkyCondition.RAIN, mode.value, Palette.INDIGO, active.value) }
            }
        }
        compose.mainClock.advanceTimeBy(1000)
        val first = compose.onRoot().captureToImage().asAndroidBitmap()
        compose.mainClock.advanceTimeBy(1000)
        val animated = compose.onRoot().captureToImage().asAndroidBitmap()
        assertFalse(first.sameAs(animated))
        compose.runOnUiThread { active.value = false }
        compose.mainClock.advanceTimeBy(32)
        val paused = compose.onRoot().captureToImage().asAndroidBitmap()
        compose.mainClock.advanceTimeBy(1000)
        assertTrue(paused.sameAs(compose.onRoot().captureToImage().asAndroidBitmap()))
        compose.runOnUiThread {
            active.value = true
            mode.value = ThemeMode.HIGH_CONTRAST
        }
        compose.mainClock.advanceTimeBy(32)
        val contrast = compose.onRoot().captureToImage().asAndroidBitmap()
        compose.mainClock.advanceTimeBy(1000)
        assertTrue(contrast.sameAs(compose.onRoot().captureToImage().asAndroidBitmap()))
    }
}
