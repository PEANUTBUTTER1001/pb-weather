package com.pbweather.weather.ui.permission

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pbweather.weather.ui.theme.WeatherTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PermissionRationaleTest {
    @get:Rule val compose = createComposeRule()

    @Test fun explanationOffersSearchBeforeRequestingPermission() {
        var result: Boolean? = null
        compose.setContent {
            WeatherTheme {
                LocationPermissionAction(onResult = { result = it }) { request ->
                    Button(onClick = request) { Text("request") }
                }
            }
        }
        compose.onNodeWithText("request").performClick()
        compose.onNodeWithText("대략적 위치로 날씨 찾기").assertIsDisplayed()
        compose.onNodeWithText("지역 검색하기").performClick()
        compose.runOnIdle { assertEquals(false, result) }
    }
}
