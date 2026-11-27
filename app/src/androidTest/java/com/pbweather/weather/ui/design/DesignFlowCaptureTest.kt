package com.pbweather.weather.ui.design

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.platform.app.InstrumentationRegistry
import com.pbweather.weather.R
import com.pbweather.weather.domain.DomainError
import com.pbweather.weather.ui.components.PrototypeConfirmDialog
import com.pbweather.weather.ui.components.WeatherDetails
import com.pbweather.weather.ui.settings.LegalDocSheet
import com.pbweather.weather.ui.settings.WidgetPreviewSheet
import com.pbweather.weather.ui.theme.Palette
import com.pbweather.weather.ui.theme.ThemeMode
import com.pbweather.weather.ui.theme.WeatherTheme
import java.time.Instant
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DesignFlowCaptureTest {
    @get:Rule val compose = createComposeRule()

    @Test fun captureDetailsDocumentsAndConfirmationsInThreeThemes() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val base = PrototypeFixtures.state()
        val summerAt = Instant.parse("2026-07-01T03:00:00Z").toEpochMilli()
        val summer = base.copy(
            now = summerAt,
            data = base.data.copy(
                forecast = base.data.forecast.copy(
                    now = base.data.forecast.now.copy(
                        data = base.data.forecast.now.data!!.copy(temperature = 30.0, humidity = 70.0, windMs = 5.0),
                        meta = base.data.forecast.now.meta!!.copy(observedAt = summerAt)
                    )
                )
            )
        )
        val offline = base.copy(
            data = base.data.copy(
                forecast = base.data.forecast.copy(
                    now = base.data.forecast.now.copy(isOffline = true, isStale = true, error = DomainError.OFFLINE)
                )
            )
        )
        val scene = mutableStateOf("closed")
        val mode = mutableStateOf(ThemeMode.LIGHT)
        val close = { scene.value = "closed" }
        compose.setContent {
            WeatherTheme(mode.value, Palette.INDIGO) {
                Box(Modifier.fillMaxSize().background(WeatherTheme.colors.ground)) {
                    when (val target = scene.value) {
                        "closed" -> Unit
                        "help", "privacy", "licenses", "usage" -> LegalDocSheet(target, close)
                        "widget" -> WidgetPreviewSheet(base.preferences.selected?.name, base.data.forecast, close, close)
                        "widget-empty" -> WidgetPreviewSheet(null, com.pbweather.weather.domain.ForecastState(), close, close)
                        "widget-offline" -> WidgetPreviewSheet(base.preferences.selected?.name, offline.data.forecast, close, close)
                        "confirm" -> PrototypeConfirmDialog(
                            context.getString(R.string.clear_weather_title),
                            context.getString(R.string.clear_weather_explanation),
                            context.getString(R.string.delete),
                            context.getString(R.string.cancel),
                            close,
                            close,
                            destructive = true
                        )
                        "permission-guide" -> PrototypeConfirmDialog(
                            context.getString(R.string.location_permission_title),
                            context.getString(R.string.location_permission_body),
                            context.getString(R.string.location_permission_request),
                            context.getString(R.string.location_search_instead),
                            close,
                            close
                        )
                        "summer" -> WeatherDetails("apparent", summer, onClose = close)
                        "offline-source" -> WeatherDetails("sourceNow", offline, onClose = close)
                        else -> WeatherDetails(target, base, { scene.value = it }, close)
                    }
                }
            }
        }
        val directory = context.getExternalFilesDir(null)!!.resolve("design-flows").apply { mkdirs() }
        val firstHour = base.data.forecast.hourly.data!!.hours.first { it.at > base.now }.at
        val targets = listOf(
            "sourceNow", "sourceHourly", "sourceWeekly", "sourceAir", "hour:$firstHour",
            "day:2026-11-25", "day:2026-12-04", "air", "apparent", "summer", "offline-source",
            "help", "privacy", "licenses", "usage", "widget", "widget-empty", "widget-offline", "confirm", "permission-guide"
        )
        for (theme in ThemeMode.entries) {
            compose.runOnUiThread { mode.value = theme }
            for (target in targets) {
                compose.runOnUiThread { scene.value = target }
                compose.onNode(isDialog()).assertExists()
                if (target == "summer") compose.onNodeWithText("31.3℃").assertExists()
                if (target ==
                    "licenses"
                ) {
                    compose.onNodeWithText(
                        context.getString(R.string.license_androidx)
                    ).performSemanticsAction(SemanticsActions.OnClick) {
                        it()
                    }
                }
                compose.onNode(isDialog()).captureToImage().asAndroidBitmap().let { bitmap ->
                    directory.resolve("${theme.name}-${target.replace(':', '-')}.png").outputStream().use {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                    }
                }
                if (target == "confirm" || target == "permission-guide") {
                    val label = context.getString(if (target == "confirm") R.string.cancel else R.string.location_search_instead)
                    compose.onNodeWithText(label).performSemanticsAction(SemanticsActions.OnClick) { it() }
                } else {
                    compose.onNodeWithContentDescription(
                        context.getString(R.string.close)
                    ).performSemanticsAction(SemanticsActions.OnClick) {
                        it()
                    }
                }
                compose.waitForIdle()
                assertTrue(compose.onAllNodes(isDialog()).fetchSemanticsNodes().isEmpty())
            }
        }
    }
}
