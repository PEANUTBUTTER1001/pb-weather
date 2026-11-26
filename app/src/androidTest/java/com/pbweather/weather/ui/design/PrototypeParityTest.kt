package com.pbweather.weather.ui.design

import android.graphics.Bitmap
import android.os.SystemClock
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.pbweather.weather.ui.navigation.WeatherActions
import com.pbweather.weather.ui.navigation.WeatherShell
import com.pbweather.weather.ui.theme.Palette
import com.pbweather.weather.ui.theme.ThemeMode
import com.pbweather.weather.ui.theme.WeatherTheme
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Rule
import org.junit.Test

class PrototypeParityTest {
    @get:Rule val compose = createComposeRule()

    @Test fun captureFixedDataTabsAcrossNineColorModes() {
        compose.mainClock.autoAdvance = false
        val mode = mutableStateOf(ThemeMode.LIGHT)
        val palette = mutableStateOf(Palette.INDIGO)
        val fixture = PrototypeFixtures.state()
        compose.setContent {
            Box(Modifier.consumeWindowInsets(WindowInsets.systemBars)) {
                WeatherTheme(mode.value, palette.value) {
                    WeatherShell(
                        fixture.copy(preferences = fixture.preferences.copy(theme = mode.value.name, palette = palette.value.name)),
                        "",
                        emptyList(),
                        mode.value,
                        palette.value,
                        WeatherActions(),
                        emptyFlow(),
                        cacheBytes = 12400000
                    )
                }
            }
        }
        val directory = InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null)!!.resolve("design").apply {
            mkdirs()
        }
        for (theme in ThemeMode.entries) {
            for (color in if (theme == ThemeMode.HIGH_CONTRAST) listOf(Palette.INDIGO) else Palette.entries) {
                compose.runOnUiThread {
                    mode.value = theme
                    palette.value = color
                }
                for ((label, file) in listOf("홈" to "home", "과거" to "history", "지역" to "locations", "설정" to "settings")) {
                    compose.onNodeWithContentDescription("$label 탭 열기").performClick()
                    compose.mainClock.advanceTimeBy(500)
                    SystemClock.sleep(350)
                    compose.waitForIdle()
                    compose.onRoot().captureToImage().asAndroidBitmap().let { bitmap ->
                        directory.resolve("${theme.name}-${color.name}-$file.png").outputStream().use {
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                        }
                    }
                }
            }
        }
    }
}
