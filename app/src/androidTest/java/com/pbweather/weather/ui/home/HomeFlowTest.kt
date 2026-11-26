package com.pbweather.weather.ui.home

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso
import com.pbweather.weather.MainActivity
import org.junit.Rule
import org.junit.Test

class HomeFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun manualSearchOpensRealWeatherAndSourceSheetWithoutLocationRequest() {
        compose.onNodeWithContentDescription("지역 탭 열기").performClick()
        compose.waitUntil(10000) { compose.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(hasSetTextAction()).performTextInput("종로")
        compose.waitUntil(timeoutMillis = 15000) {
            compose.onAllNodesWithContentDescription("서울특별시 종로구").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription("서울특별시 종로구").performClick()
        val source = hasText("기상청 실황 ·", substring = true) and !hasText("미제공", substring = true)
        compose.waitUntil(timeoutMillis = 30000) { compose.onAllNodes(source).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(source).performScrollTo().performClick()
        compose.waitUntil(timeoutMillis = 5000) {
            compose.onAllNodesWithText("앱 수신 시각").fetchSemanticsNodes().isNotEmpty()
        }
        Espresso.pressBack()
        compose.waitUntil(timeoutMillis = 5000) {
            compose.onAllNodesWithText("앱 수신 시각").fetchSemanticsNodes().isEmpty()
        }
    }
}
