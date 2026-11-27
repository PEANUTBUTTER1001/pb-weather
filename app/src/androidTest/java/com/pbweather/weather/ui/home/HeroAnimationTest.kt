package com.pbweather.weather.ui.home

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.pbweather.weather.domain.SkyCondition
import com.pbweather.weather.ui.theme.Palette
import com.pbweather.weather.ui.theme.ThemeMode
import com.pbweather.weather.ui.theme.WeatherTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HeroAnimationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun backgroundRemainsStaticAcrossWeatherAndTheme() {
        compose.mainClock.autoAdvance = false
        val sky = mutableStateOf(SkyCondition.CLEAR)
        val mode = mutableStateOf(ThemeMode.LIGHT)
        compose.setContent {
            WeatherTheme(mode.value, Palette.INDIGO) {
                Box(Modifier.size(320.dp, 300.dp)) { HeroWeather(sky.value, mode.value, Palette.INDIGO) }
            }
        }
        val directory = InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null)!!
            .resolve("design-static").apply { mkdirs() }
        for (theme in ThemeMode.entries) {
            for (condition in SkyCondition.entries) {
                compose.runOnUiThread {
                    mode.value = theme
                    sky.value = condition
                }
                compose.mainClock.advanceTimeBy(32)
                val first = compose.onRoot().captureToImage().asAndroidBitmap()
                compose.mainClock.advanceTimeBy(1000)
                val later = compose.onRoot().captureToImage().asAndroidBitmap()
                assertTrue("Weather background must remain static: $theme/$condition", first.sameAs(later))
                directory.resolve("${theme.name}-${condition.name}.png").outputStream().use {
                    first.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
            }
        }
    }
}
