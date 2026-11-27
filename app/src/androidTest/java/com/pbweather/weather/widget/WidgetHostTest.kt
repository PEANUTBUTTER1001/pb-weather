package com.pbweather.weather.widget

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.glance.appwidget.updateAll
import androidx.test.platform.app.InstrumentationRegistry
import com.pbweather.weather.di.DataEntryPoint
import com.pbweather.weather.domain.Region
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class WidgetHostTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private fun text(view: View): String = if (view is TextView) {
        view.text.toString()
    } else if (view is ViewGroup) {
        (0 until view.childCount).joinToString { text(view.getChildAt(it)) }
    } else {
        ""
    }

    @Test fun actualRemoteViewsReflectPrimaryChangeAndLastDeletion() = runBlocking {
        val context = compose.activity
        val prefs = EntryPointAccessors.fromApplication(context, DataEntryPoint::class.java).preferences()
        val original = prefs.preferences.first()
        val host = AppWidgetHost(context, 412)
        val manager = AppWidgetManager.getInstance(context)
        val id = host.allocateAppWidgetId()
        lateinit var view: AppWidgetHostView
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        automation.adoptShellPermissionIdentity("android.permission.BIND_APPWIDGET")
        try {
            prefs.update { it.copy(favorites = emptyList(), primaryId = null) }
            assertTrue(
                "host=${context.packageName}/${context.opPackageName}",
                manager.bindAppWidgetIdIfAllowed(id, ComponentName(context, WeatherWidgetReceiver::class.java))
            )
            compose.runOnUiThread {
                host.startListening()
                view = host.createView(context, id, manager.getAppWidgetInfo(id))
            }
            val width = InstrumentationRegistry.getArguments().getString("widgetWidthDp")?.toIntOrNull()?.coerceIn(180, 400) ?: 240
            compose.setContent { AndroidView(factory = { view }, modifier = Modifier.width(width.dp).height(200.dp)) }
            WeatherWidget().updateAll(context)
            compose.waitUntil(20000) { text(view).contains("대표 지역이 지정되지 않았습니다") }
            val region = Region("1100000000", "서울특별시", 60, 127, 37.5635694, 126.9800083, "108", "서울", 1.0)
            prefs.addFavorite(region)
            compose.waitUntil(30000) { text(view).contains("서울특별시") && !text(view).contains("대표 지역이 지정되지 않았습니다") }
            val file = context.getExternalFilesDir(null)!!.resolve("widget-host.png")
            file.outputStream().use {
                compose.onRoot().captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
            }
            if (context.resources.configuration.fontScale >= 1.8f) {
                compose.runOnIdle {
                    assertVisibleTextFits(view, checkViewport = false)
                    descendants(view).filterIsInstance<TextView>().filter {
                        it.text.matches(Regex("-?[0-9]+[.][0-9]°"))
                    }.forEach { value ->
                        assertTrue("Temperature must remain on one line", value.layout.lineCount == 1)
                        assertTrue(
                            "Negative temperature needs the full numeric row",
                            value.paint.measureText("-99.9°") <= value.width - value.compoundPaddingLeft - value.compoundPaddingRight
                        )
                    }
                    val list = descendants(view).filterIsInstance<ListView>().single()
                    assertTrue("Large font widget must allow scrolling", list.canScrollVertically(1))
                }
                for (step in 0 until 16) {
                    val reached = compose.runOnIdle {
                        descendants(view).filterIsInstance<TextView>().any {
                            it.text.startsWith("마지막 성공") && fullyVisible(it)
                        }
                    }
                    if (reached) break
                    compose.runOnIdle { descendants(view).filterIsInstance<ListView>().single().scrollListBy(view.height / 2) }
                    android.os.SystemClock.sleep(60)
                }
                compose.waitUntil(10000) {
                    descendants(view).filterIsInstance<TextView>().any {
                        it.text.startsWith("마지막 성공") && fullyVisible(it)
                    }
                }
                context.getExternalFilesDir(null)!!.resolve("widget-host-scrolled.png").outputStream().use {
                    compose.onRoot().captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
                }
            } else {
                compose.runOnIdle { assertVisibleTextFits(view) }
            }
            prefs.removeFavorite(region.id)
            compose.waitUntil(20000) { text(view).contains("대표 지역이 지정되지 않았습니다") && !text(view).contains("서울특별시") }
        } finally {
            host.deleteAppWidgetId(id)
            host.stopListening()
            prefs.update { original }
            automation.dropShellPermissionIdentity()
        }
    }

    private fun descendants(root: View): List<View> = listOf(root) + if (root is ViewGroup) {
        (0 until root.childCount).flatMap { descendants(root.getChildAt(it)) }
    } else {
        emptyList()
    }

    private fun fullyVisible(view: View): Boolean {
        val bounds = android.graphics.Rect()
        return view.getGlobalVisibleRect(bounds) && bounds.height() >= view.height
    }

    private fun assertVisibleTextFits(root: View, checkViewport: Boolean = true) {
        if (root is TextView && root.visibility == View.VISIBLE && root.text.isNotBlank()) {
            val bounds = android.graphics.Rect()
            if (checkViewport) {
                assertTrue("Hidden widget text: ${root.text}", root.getGlobalVisibleRect(bounds))
                assertTrue("Clipped widget text: ${root.text}", bounds.height() >= root.height)
            }
            root.layout?.let { layout ->
                assertTrue("Truncated widget text: ${root.text}", (0 until layout.lineCount).all { layout.getEllipsisCount(it) == 0 })
                assertTrue(
                    "Vertically clipped widget text: ${root.text}; layout=${layout.height}, view=${root.height}, " +
                        "padding=${root.compoundPaddingTop + root.compoundPaddingBottom}",
                    layout.height <= root.height - root.compoundPaddingTop - root.compoundPaddingBottom
                )
            }
        }
        if (root is ViewGroup) (0 until root.childCount).forEach { assertVisibleTextFits(root.getChildAt(it), checkViewport) }
    }
}
