package com.pbweather.weather.widget

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
            compose.setContent { AndroidView(factory = { view }, modifier = Modifier.fillMaxWidth().height(300.dp)) }
            WeatherWidget().updateAll(context)
            compose.waitUntil(20000) { text(view).contains("대표 지역이 지정되지 않았습니다") }
            val region = Region("1100000000", "서울특별시", 60, 127, 37.5635694, 126.9800083, "108", "서울", 1.0)
            prefs.addFavorite(region)
            compose.waitUntil(30000) { text(view).contains("서울특별시") && !text(view).contains("대표 지역이 지정되지 않았습니다") }
            val file = context.getExternalFilesDir(null)!!.resolve("widget-host.png")
            file.outputStream().use {
                compose.onRoot().captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
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
}
