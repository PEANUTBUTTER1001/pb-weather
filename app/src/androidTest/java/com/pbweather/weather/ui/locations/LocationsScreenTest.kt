package com.pbweather.weather.ui.locations

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.pbweather.weather.data.local.AppPreferences
import com.pbweather.weather.data.local.FavoriteRules
import com.pbweather.weather.domain.DateRange
import com.pbweather.weather.domain.Region
import com.pbweather.weather.ui.navigation.WeatherActions
import com.pbweather.weather.ui.navigation.WeatherUiState
import com.pbweather.weather.ui.theme.WeatherTheme
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class LocationsScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun savePrimaryAndDeleteAreConnectedWithoutChangingCurrentSelection() {
        val region = Region("test", "테스트 지역", 60, 127, 37.57, 126.98, "108", "서울", 1.0)
        val prefs = mutableStateOf(AppPreferences(selected = region))
        val range = DateRange(LocalDate.parse("2026-11-15"), LocalDate.parse("2026-11-24"))
        compose.setContent {
            WeatherTheme {
                LocationsScreen(
                    WeatherUiState(preferences = prefs.value, range = range),
                    "테스트",
                    listOf(region),
                    emptyMap(),
                    WeatherActions(
                        favorite = { prefs.value = FavoriteRules.add(prefs.value, it) },
                        primary = {
                            prefs.value =
                                prefs.value.copy(primaryId = it)
                        },
                        deleteFavorite = { prefs.value = FavoriteRules.remove(prefs.value, it).first }
                    ),
                    {},
                    {},
                    rememberLazyListState()
                )
            }
        }
        compose.onNodeWithText("추가").performClick()
        compose.onNodeWithText("저장됨").assertIsNotEnabled()
        compose.onNodeWithContentDescription("테스트 지역 대표 지정").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(region.id, prefs.value.primaryId) }
        compose.onNodeWithContentDescription("테스트 지역 삭제").performClick()
        compose.runOnIdle {
            assertNull(prefs.value.primaryId)
            assertEquals(region, prefs.value.selected)
        }
    }
}
