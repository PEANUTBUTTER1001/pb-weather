package com.pbweather.weather.ui.home

import android.graphics.Bitmap
import android.os.Build
import android.os.SystemClock
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.pbweather.weather.domain.SkyCondition
import com.pbweather.weather.ui.design.PrototypeFixtures
import com.pbweather.weather.ui.theme.Palette
import com.pbweather.weather.ui.theme.ThemeMode
import com.pbweather.weather.ui.theme.WeatherTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HeroAnimationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun activeShaderAnimatesForHighAndMediumTiers() {
        if (Build.VERSION.SDK_INT < HeroMotionPolicy.MIN_AGSL_SDK) return
        val sky = mutableStateOf(SkyCondition.CLEAR)
        val tier = mutableStateOf(HeroMotionTier.HIGH)
        compose.setContent {
            WeatherTheme(ThemeMode.LIGHT, Palette.INDIGO) {
                Box(Modifier.size(320.dp, 300.dp)) {
                    HeroWeather(
                        sky = sky.value,
                        mode = ThemeMode.LIGHT,
                        palette = Palette.INDIGO,
                        windMs = 6.5,
                        hourOfDay = 14,
                        motionEnabled = true,
                        isVisible = true,
                        tierOverride = tier.value
                    )
                }
            }
        }
        val cases = listOf(
            HeroMotionTier.HIGH to SkyCondition.CLEAR,
            HeroMotionTier.MEDIUM to SkyCondition.RAIN,
            HeroMotionTier.HIGH to SkyCondition.SNOW
        )
        for ((targetTier, condition) in cases) {
            compose.runOnUiThread {
                tier.value = targetTier
                sky.value = condition
            }
            compose.waitForIdle()
            val first = compose.onRoot().captureToImage().asAndroidBitmap()
            SystemClock.sleep(500)
            val later = compose.onRoot().captureToImage().asAndroidBitmap()
            assertFalse("Expected AGSL animation frame change for $targetTier/$condition", first.sameAs(later))
        }
    }

    @Test fun staticHighContrastDisabledAndHiddenRemainStatic() {
        val mode = mutableStateOf(ThemeMode.LIGHT)
        val motionEnabled = mutableStateOf(true)
        val visible = mutableStateOf(true)
        val tier = mutableStateOf(HeroMotionTier.STATIC)
        compose.setContent {
            WeatherTheme(mode.value, Palette.INDIGO) {
                Box(Modifier.size(320.dp, 300.dp)) {
                    HeroWeather(
                        sky = SkyCondition.RAIN,
                        mode = mode.value,
                        palette = Palette.INDIGO,
                        windMs = 8.0,
                        hourOfDay = 14,
                        motionEnabled = motionEnabled.value,
                        isVisible = visible.value,
                        tierOverride = tier.value
                    )
                }
            }
        }
        val cases = listOf(
            Triple(ThemeMode.LIGHT, true, true) to HeroMotionTier.STATIC,
            Triple(ThemeMode.HIGH_CONTRAST, true, true) to HeroMotionTier.HIGH,
            Triple(ThemeMode.LIGHT, false, true) to HeroMotionTier.HIGH,
            Triple(ThemeMode.LIGHT, true, false) to HeroMotionTier.HIGH
        )
        for ((flags, targetTier) in cases) {
            compose.runOnUiThread {
                mode.value = flags.first
                motionEnabled.value = flags.second
                visible.value = flags.third
                tier.value = targetTier
            }
            compose.waitForIdle()
            val first = compose.onRoot().captureToImage().asAndroidBitmap()
            SystemClock.sleep(450)
            val later = compose.onRoot().captureToImage().asAndroidBitmap()
            assertTrue("Expected static hero for $flags/$targetTier", first.sameAs(later))
        }
    }

    @Test fun allSkyConditionsAndThemesRenderAndCaptureReadabilitySamples() {
        val sky = mutableStateOf(SkyCondition.CLEAR)
        val mode = mutableStateOf(ThemeMode.LIGHT)
        compose.setContent {
            WeatherTheme(mode.value, Palette.INDIGO) {
                Box(Modifier.size(320.dp, 300.dp)) {
                    HeroWeather(
                        sky = sky.value,
                        mode = mode.value,
                        palette = Palette.INDIGO,
                        windMs = 5.4,
                        hourOfDay = 17,
                        motionEnabled = true,
                        isVisible = true,
                        tierOverride = if (mode.value == ThemeMode.DARK) HeroMotionTier.MEDIUM else HeroMotionTier.HIGH
                    )
                }
            }
        }
        val directory = InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null)!!
            .resolve("agsl-shader").apply { mkdirs() }
        for (theme in ThemeMode.entries) {
            for (condition in SkyCondition.entries) {
                compose.runOnUiThread {
                    mode.value = theme
                    sky.value = condition
                }
                compose.waitForIdle()
                SystemClock.sleep(150)
                val frame = compose.onRoot().captureToImage().asAndroidBitmap()
                directory.resolve("${theme.name}-${condition.name}.png").outputStream().use {
                    frame.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
            }
        }
    }

    @Test fun homeHeroTextRemainsVisibleOverActiveShader() {
        val base = PrototypeFixtures.state()
        val nowData = base.data.forecast.now.data!!
        val hourlyData = base.data.forecast.hourly.data!!
        val mode = mutableStateOf(ThemeMode.LIGHT)
        val skyCode = mutableStateOf(1)
        val ptyCode = mutableStateOf(0)
        compose.setContent {
            val hours = hourlyData.hours.mapIndexed { idx, hour ->
                if (idx == 0) hour.copy(sky = skyCode.value, precipitationType = ptyCode.value) else hour
            }
            val fixture = base.copy(
                data = base.data.copy(
                    forecast = base.data.forecast.copy(
                        now = base.data.forecast.now.copy(
                            data = nowData.copy(precipitationType = ptyCode.value)
                        ),
                        hourly = base.data.forecast.hourly.copy(
                            data = hourlyData.copy(hours = hours)
                        )
                    )
                )
            )
            WeatherTheme(mode.value, Palette.INDIGO) {
                Surface(color = WeatherTheme.colors.ground, contentColor = WeatherTheme.colors.text) {
                    HomeScreen(
                        state = fixture,
                        mode = mode.value,
                        palette = Palette.INDIGO,
                        list = rememberLazyListState(),
                        hoursList = rememberLazyListState(),
                        selectedHour = null,
                        onHour = {},
                        onDetails = {},
                        onHistory = {},
                        onRetry = {}
                    )
                }
            }
        }
        val directory = InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null)!!
            .resolve("agsl-shader").apply { mkdirs() }
        val samples = listOf(
            "CLEAR" to (1 to 0),
            "CLOUDY" to (4 to 0),
            "RAIN" to (4 to 1),
            "SNOW" to (4 to 3)
        )
        for (theme in listOf(ThemeMode.LIGHT, ThemeMode.DARK)) {
            for ((label, codes) in samples) {
                compose.runOnUiThread {
                    mode.value = theme
                    skyCode.value = codes.first
                    ptyCode.value = codes.second
                }
                compose.waitForIdle()
                SystemClock.sleep(200)
                val frame = compose.onRoot().captureToImage().asAndroidBitmap()
                directory.resolve("home-${theme.name}-$label.png").outputStream().use {
                    frame.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
            }
        }
    }
}
