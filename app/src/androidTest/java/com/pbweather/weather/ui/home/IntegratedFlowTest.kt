package com.pbweather.weather.ui.home

import android.content.Intent
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso
import com.pbweather.weather.MainActivity
import com.pbweather.weather.di.DataEntryPoint
import com.pbweather.weather.widget.OPEN_WIDGET
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class IntegratedFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun favoritesUndoWidgetEntrySettingsAndRecreationShareSavedState() = runBlocking {
        val prefs = EntryPointAccessors.fromApplication(compose.activity.application, DataEntryPoint::class.java).preferences()
        val original = prefs.preferences.first()
        try {
            prefs.update { it.copy(favorites = emptyList(), primaryId = null, theme = "LIGHT", palette = "INDIGO", font = "NORMAL") }
            compose.onNodeWithContentDescription("지역 탭 열기").performClick()
            compose.waitUntil(10000) { compose.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty() }
            compose.onNode(hasSetTextAction()).performTextClearance()
            compose.onNode(hasSetTextAction()).performTextInput("종로")
            compose.waitUntil(15000) { compose.onAllNodesWithContentDescription("서울특별시 종로구").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithContentDescription("서울특별시 종로구").performClick()
            compose.waitUntil(5000) { runBlocking { prefs.preferences.first().selected?.name == "서울특별시 종로구" } }
            val region = prefs.preferences.first().selected!!
            compose.onNodeWithContentDescription("지역 탭 열기").performClick()
            val add = hasText("추가") and hasAnyAncestor(hasTestTag("search:${region.id}"))
            compose.onNodeWithTag("locations-list").performScrollToNode(add)
            compose.onNode(add).performClick()
            compose.waitUntil(5000) { runBlocking { prefs.preferences.first().favorites.any { it.id == region.id } } }
            val delete = hasContentDescription("${region.name} 삭제")
            compose.onNodeWithTag("locations-list").performScrollToNode(delete)
            compose.onNode(delete).performClick()
            compose.waitUntil(5000) { compose.onAllNodesWithText("되돌리기").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("되돌리기").performClick()
            compose.waitUntil(5000) { runBlocking { prefs.preferences.first().favorites.any { it.id == region.id } } }
            prefs.setPrimary(region.id)
            prefs.select(region.copy(id = "other-test-region", name = "다른 선택 지역"))
            compose.activityRule.scenario.onActivity {
                it.startActivity(Intent(it, MainActivity::class.java).putExtra(OPEN_WIDGET, true).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP))
            }
            compose.waitUntil(5000) { runBlocking { prefs.preferences.first().selected?.id == region.id } }
            compose.waitUntil(10000) { compose.onAllNodes(hasText("기상청 실황 ·", substring = true)).fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithContentDescription("설정 탭 열기").performClick()
            compose.onNodeWithText("앰버").performClick()
            compose.onNodeWithText("아주 크게").performClick()
            compose.waitUntil(5000) {
                runBlocking {
                    prefs.preferences.first().palette == "AMBER" &&
                        prefs.preferences.first().font == "EXTRA_LARGE"
                }
            }
            compose.activityRule.scenario.recreate()
            compose.onNodeWithTag("settings-list").performScrollToNode(hasText("개인정보 처리방침"))
            compose.onNodeWithText("개인정보 처리방침").performClick()
            compose.onNodeWithText("한눈에 보기").fetchSemanticsNode()
            compose.onNodeWithText("정보 지우기").fetchSemanticsNode()
            Espresso.pressBack()
            assertEquals("AMBER", prefs.preferences.first().palette)
            assertEquals("EXTRA_LARGE", prefs.preferences.first().font)
            assertEquals(region.id, prefs.preferences.first().primaryId)
        } finally {
            prefs.update { original }
        }
    }
}
