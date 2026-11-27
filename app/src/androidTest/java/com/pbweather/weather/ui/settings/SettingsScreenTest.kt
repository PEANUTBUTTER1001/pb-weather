package com.pbweather.weather.ui.settings

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import com.pbweather.weather.data.local.AppPreferences
import com.pbweather.weather.domain.DateRange
import com.pbweather.weather.domain.ForecastState
import com.pbweather.weather.ui.navigation.WeatherActions
import com.pbweather.weather.ui.navigation.WeatherUiState
import com.pbweather.weather.ui.theme.FontSize
import com.pbweather.weather.ui.theme.Palette
import com.pbweather.weather.ui.theme.ThemeMode
import com.pbweather.weather.ui.theme.WeatherTheme
import java.time.LocalDate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun widgetPreviewRefreshesPrimaryAndOpensItsHome() {
        val base = com.pbweather.weather.ui.design.PrototypeFixtures.state()
        var refreshed = 0
        var opened = 0
        compose.setContent {
            WeatherTheme {
                SettingsScreen(
                    base,
                    13002342,
                    false,
                    base.data.forecast,
                    WeatherActions(refreshWidget = { refreshed++ }, openWidget = { opened++ }),
                    {},
                    {},
                    {}
                )
            }
        }
        compose.onNodeWithText("열기").performScrollTo().performClick()
        compose.onNodeWithText("홈 화면 위젯").assertExists()
        compose.onNodeWithText("새로고침").performScrollTo().performClick()
        compose.runOnIdle { org.junit.Assert.assertEquals(1, refreshed) }
        compose.onNodeWithTag("widget-preview-card").performScrollTo().performClick()
        compose.runOnIdle { org.junit.Assert.assertEquals(1, opened) }
        compose.onNodeWithText("홈 화면 위젯").assertDoesNotExist()
    }

    @Test fun highContrastDisablesPaletteAndDeletionRequiresConfirmation() {
        val prefs = mutableStateOf(AppPreferences())
        var deleted = false
        val range = DateRange(LocalDate.parse("2026-11-15"), LocalDate.parse("2026-11-24"))
        compose.setContent {
            WeatherTheme(ThemeMode.valueOf(prefs.value.theme), Palette.valueOf(prefs.value.palette), FontSize.valueOf(prefs.value.font)) {
                SettingsScreen(
                    WeatherUiState(preferences = prefs.value, range = range),
                    1024,
                    false,
                    ForecastState(),
                    WeatherActions(theme = {
                        prefs.value = prefs.value.copy(theme = it.name)
                    }, clearCache = { deleted = true }),
                    {},
                    {},
                    {}
                )
            }
        }
        compose.onNodeWithText("고대비").performScrollTo().performClick()
        compose.onNodeWithText("인디고").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("settings-list").performScrollToNode(hasText("삭제"))
        compose.onNodeWithText("삭제").performClick()
        assertFalse(deleted)
        compose.onNode(hasText("삭제") and hasAnyAncestor(isDialog())).performClick()
        compose.runOnIdle { assertTrue(deleted) }
    }
}
