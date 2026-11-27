package com.pbweather.weather.ui.history

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import com.pbweather.weather.data.local.AppPreferences
import com.pbweather.weather.domain.DateRange
import com.pbweather.weather.domain.Region
import com.pbweather.weather.ui.navigation.WeatherUiState
import com.pbweather.weather.ui.theme.WeatherTheme
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HistoryScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun chartPointOpensTheDailyObservationSheet() {
        val state = com.pbweather.weather.ui.design.PrototypeFixtures.state()
        compose.setContent { WeatherTheme { HistoryScreen(state, {}, {}, {}) } }
        compose.onNodeWithContentDescription("2026-11-14 그래프 지점 상세 열기").performScrollTo().performClick()
        val label = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext.getString(
            com.pbweather.weather.R.string.average_wind
        )
        compose.onNodeWithText(label).assertIsDisplayed()
        compose.onNodeWithContentDescription("닫기").performClick()
    }

    @Test fun invalidRangeStaysNearInputAndOneWeekChoosesSevenKstDates() {
        val region = Region("test", "테스트 지역", 60, 127, 37.57, 126.98, "108", "서울", 1.0)
        val range = DateRange(LocalDate.parse("2026-11-15"), LocalDate.parse("2026-11-24"))
        val state =
            mutableStateOf(
                WeatherUiState(
                    preferences = AppPreferences(selected = region),
                    range = range,
                    now = Instant.parse("2026-11-25T03:00:00Z").toEpochMilli()
                )
            )
        compose.setContent { WeatherTheme { HistoryScreen(state.value, { state.value = state.value.copy(range = it) }, {}, {}) } }
        compose.onNodeWithText("시작일").performScrollTo().performTextReplacement("2026-11-26")
        compose.onNodeWithText("조회").performScrollTo().performClick()
        compose.onNodeWithText("시작일은 종료일보다 늦을 수 없습니다.").assertIsDisplayed()
        assertEquals(range, state.value.range)
        compose.onNodeWithText("1주").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(7, state.value.range.dates.size) }
        compose.onNodeWithText("시작일").performScrollTo().performTextReplacement("2026-11-14")
        compose.onNodeWithText("조회").performScrollTo().performClick()
        compose.onNodeWithText("최근 10일 창 안에서 어제까지 선택하세요.").assertIsDisplayed()
    }
}
