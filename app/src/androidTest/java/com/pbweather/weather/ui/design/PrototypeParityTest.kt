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
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
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

    @Test fun compactFieldKeepsItsLabelAndEditableSemanticsTogether() {
        val value = mutableStateOf("2026-11-14")
        compose.setContent {
            WeatherTheme { com.pbweather.weather.ui.components.CompactTextField(value.value, { value.value = it }, label = "시작일") }
        }
        compose.onNodeWithText("시작일").performTextReplacement("2026-11-15")
        compose.runOnIdle { org.junit.Assert.assertEquals("2026-11-15", value.value) }
    }

    @Test fun firstLoadAndOfflineEmptyHaveDistinctViewsAndRegionAction() {
        val base = PrototypeFixtures.state()
        val state =
            mutableStateOf(
                base.copy(
                    data = base.data.copy(
                        forecast = com.pbweather.weather.domain.ForecastState(
                            now = com.pbweather.weather.domain.Resource(isRefreshing = true)
                        ),
                        air = com.pbweather.weather.domain.Resource()
                    )
                )
            )
        compose.setContent {
            WeatherTheme(ThemeMode.LIGHT, Palette.INDIGO) {
                WeatherShell(state.value, "", emptyList(), ThemeMode.LIGHT, Palette.INDIGO, WeatherActions(), emptyFlow())
            }
        }
        compose.onNodeWithContentDescription("자료를 불러오는 중입니다.").assertExists()
        capture("state-loading")
        compose.runOnUiThread {
            state.value =
                base.copy(
                    data = base.data.copy(
                        forecast = com.pbweather.weather.domain.ForecastState(
                            now = com.pbweather.weather.domain.Resource(isOffline = true)
                        ),
                        air = com.pbweather.weather.domain.Resource()
                    )
                )
        }
        compose.onNodeWithText("저장된 날씨 자료가 없습니다").assertIsDisplayed()
        capture("state-empty")
        compose.onNodeWithText("지역 선택하기").performClick()
        compose.onNodeWithText("지역 검색").assertIsDisplayed()
    }

    private fun capture(name: String) {
        val directory = InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null)!!.resolve("design").apply {
            mkdirs()
        }
        compose.onRoot().captureToImage().asAndroidBitmap().let { bitmap ->
            directory.resolve("$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

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
                    compose.onNodeWithContentDescription("$label 탭 열기").performSemanticsAction(SemanticsActions.OnClick) { it() }
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
