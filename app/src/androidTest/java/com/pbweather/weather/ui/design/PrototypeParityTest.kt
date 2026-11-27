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
import com.pbweather.weather.ui.theme.FontSize
import com.pbweather.weather.ui.theme.Palette
import com.pbweather.weather.ui.theme.ThemeMode
import com.pbweather.weather.ui.theme.WeatherTheme
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Rule
import org.junit.Test

class PrototypeParityTest {
    @get:Rule val compose = createComposeRule()

    @Test fun favoriteMarkerVectorsContainVisiblePixels() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        for (resource in listOf(
            com.pbweather.weather.R.drawable.design_icon_star,
            com.pbweather.weather.R.drawable.design_icon_star_filled
        )) {
            val bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
            val drawable = context.getDrawable(resource)!!
            drawable.setBounds(0, 0, 32, 32)
            drawable.draw(android.graphics.Canvas(bitmap))
            val pixels = IntArray(32 * 32)
            bitmap.getPixels(pixels, 0, 32, 0, 0, 32, 32)
            org.junit.Assert.assertTrue("Favorite marker is blank: $resource", pixels.any { it ushr 24 != 0 })
            bitmap.recycle()
        }
    }

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
        compose.mainClock.autoAdvance = false
        compose.mainClock.advanceTimeBy(32)
        val loading = compose.onRoot().captureToImage().asAndroidBitmap()
        compose.mainClock.advanceTimeBy(1000)
        org.junit.Assert.assertTrue(
            "Loading skeleton must remain static",
            loading.sameAs(compose.onRoot().captureToImage().asAndroidBitmap())
        )
        compose.mainClock.autoAdvance = true
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

    @Test fun cachedWeatherSurvivesExceptionalStatesAndLongText() {
        val base = PrototypeFixtures.state()
        val state = mutableStateOf(base)
        val mode = mutableStateOf(ThemeMode.LIGHT)
        compose.setContent {
            WeatherTheme(mode.value) {
                WeatherShell(state.value, "", emptyList(), mode.value, Palette.INDIGO, WeatherActions(), emptyFlow())
            }
        }
        val original = base.data.forecast.now
        val cases = listOf(
            "offline" to base.copy(
                data = base.data.copy(
                    forecast = base.data.forecast.copy(
                        now = original.copy(isOffline = true, isStale = true, error = com.pbweather.weather.domain.DomainError.OFFLINE)
                    )
                )
            ),
            "quota" to base.copy(
                data = base.data.copy(
                    forecast = base.data.forecast.copy(
                        now = original.copy(isStale = true, error = com.pbweather.weather.domain.DomainError.QUOTA)
                    )
                )
            ),
            "refreshing" to base.copy(
                data = base.data.copy(
                    forecast = base.data.forecast.copy(
                        now = original.copy(isStale = true, isRefreshing = true)
                    )
                )
            ),
            "partial-air" to base.copy(
                data = base.data.copy(
                    air = base.data.air.copy(
                        data = null,
                        error = com.pbweather.weather.domain.DomainError.OFFLINE
                    )
                )
            ),
            "permission" to base.copy(
                location = base.location.copy(
                    granted = false,
                    problem = com.pbweather.weather.domain.LocationProblem.PERMISSION_DENIED
                )
            ),
            "longest" to base.copy(
                preferences = base.preferences.copy(
                    selected = PrototypeFixtures.region.copy(
                        name = "제주특별자치도 서귀포시 대정읍 하모리 주변 관측 지역"
                    )
                )
            )
        )
        for (theme in ThemeMode.entries) {
            for ((name, fixture) in cases) {
                compose.runOnUiThread {
                    mode.value = theme
                    state.value = fixture
                }
                compose.onNodeWithText("7.4°").assertExists()
                capture("state-${theme.name}-$name")
            }
        }
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
        val appFont = FontSize.entries.firstOrNull {
            it.name == InstrumentationRegistry.getArguments().getString("appFont")
        } ?: FontSize.NORMAL
        val fixture = PrototypeFixtures.state().let { it.copy(preferences = it.preferences.copy(font = appFont.name)) }
        val measured = org.json.JSONArray()
        compose.setContent {
            Box(Modifier.consumeWindowInsets(WindowInsets.systemBars)) {
                WeatherTheme(mode.value, palette.value, appFont) {
                    WeatherShell(
                        fixture.copy(preferences = fixture.preferences.copy(theme = mode.value.name, palette = palette.value.name)),
                        "",
                        emptyList(),
                        mode.value,
                        palette.value,
                        WeatherActions(),
                        emptyFlow(),
                        regionForecasts = PrototypeFixtures.favoriteForecasts(),
                        primaryForecast = fixture.data.forecast,
                        cacheBytes = 13002342
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
                        val context = InstrumentationRegistry.getInstrumentation().targetContext
                        val density = context.resources.displayMetrics.density
                        measured.put(
                            org.json.JSONObject().apply {
                                put("screen", file)
                                put("theme", theme.name)
                                put("palette", color.name)
                                put("appFont", appFont.name)
                                put("systemFontScale", context.resources.configuration.fontScale)
                                put("widthDp", context.resources.configuration.screenWidthDp)
                                put("bitmapWidth", bitmap.width)
                                put("bitmapHeight", bitmap.height)
                                put("logicalWidth", bitmap.width / density)
                                put("logicalHeight", bitmap.height / density)
                            }
                        )
                        directory.resolve("${theme.name}-${color.name}-$file.png").outputStream().use {
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                        }
                    }
                }
            }
        }
        directory.resolve("measurements.json").writeText(measured.toString(2))
    }
}
