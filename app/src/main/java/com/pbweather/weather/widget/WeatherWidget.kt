package com.pbweather.weather.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.pbweather.weather.MainActivity
import com.pbweather.weather.R
import com.pbweather.weather.data.repository.WidgetData
import com.pbweather.weather.di.DataEntryPoint
import com.pbweather.weather.domain.SkyCondition
import com.pbweather.weather.domain.WeatherCondition
import com.pbweather.weather.ui.components.number
import com.pbweather.weather.ui.components.timeLabel
import com.pbweather.weather.ui.theme.Dims
import com.pbweather.weather.ui.theme.FontSize
import com.pbweather.weather.ui.theme.Palette
import com.pbweather.weather.ui.theme.ThemeMode
import com.pbweather.weather.ui.theme.weatherColors
import com.pbweather.weather.ui.theme.weatherTypography
import com.pbweather.weather.work.WidgetScheduler
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first

const val OPEN_WIDGET = "com.pbweather.weather.OPEN_WIDGET"

class WeatherWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Single
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = EntryPointAccessors.fromApplication(context, DataEntryPoint::class.java).widgets()
        val initial = repository.observe().first()
        provideContent {
            val state by repository.observe().collectAsState(initial)
            WidgetContent(context, state)
        }
    }
}

@Composable private fun WidgetContent(context: Context, state: WidgetData) {
    val mode = ThemeMode.entries.firstOrNull { it.name == state.preferences.theme } ?: ThemeMode.LIGHT
    val palette = Palette.entries.firstOrNull { it.name == state.preferences.palette } ?: Palette.INDIGO
    val colors = weatherColors(mode, palette)
    val type = weatherTypography(FontSize.NORMAL)
    val normal = TextStyle(color = ColorProvider(colors.text), fontSize = type.material.bodyMedium.fontSize)
    val intent = Intent(
        context,
        MainActivity::class.java
    ).putExtra(OPEN_WIDGET, true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    Column(
        GlanceModifier.fillMaxSize().background(colors.border).cornerRadius(Dims.widgetRadius)
            .clickable(actionStartActivity(intent)).padding(Dims.hairline)
    ) {
        Column(
            GlanceModifier.fillMaxSize().background(colors.surface)
                .cornerRadius(Dims.widgetRadius - Dims.hairline).padding(Dims.gutter)
        ) {
            if (state.region == null) {
                Text(context.getString(R.string.widget_no_primary), style = normal)
                Text(context.getString(R.string.widget_choose_primary), style = normal)
            } else {
                Text(state.region.name, style = normal)
                val observed = state.forecast.now
                val temperature =
                    observed.data?.temperature?.let { context.getString(R.string.temperature, number(it).orEmpty()) }
                        ?: context.getString(R.string.missing)
                val hour = state.forecast.hourly.data?.hours?.minByOrNull { kotlin.math.abs(it.at - (observed.meta?.observedAt ?: it.at)) }
                val sky = WeatherCondition.from(hour?.sky, observed.data?.precipitationType)
                Row(GlanceModifier.fillMaxWidth()) {
                    Text(
                        temperature,
                        modifier = GlanceModifier.defaultWeight(),
                        style = TextStyle(color = ColorProvider(colors.text), fontSize = type.material.headlineMedium.fontSize)
                    )
                    Image(
                        ImageProvider(
                            when (sky) {
                                SkyCondition.CLEAR -> R.drawable.widget_symbol_clear
                                SkyCondition.RAIN, SkyCondition.SLEET -> R.drawable.widget_symbol_rain
                                SkyCondition.SNOW -> R.drawable.widget_symbol_snow
                                else -> R.drawable.widget_symbol_cloud
                            }
                        ),
                        contentDescription = null,
                        modifier = GlanceModifier.size(Dims.touch),
                        colorFilter = ColorFilter.tint(ColorProvider(colors.text))
                    )
                }
                Text(
                    context.getString(
                        when (sky) {
                            SkyCondition.CLEAR -> R.string.sky_clear
                            SkyCondition.PARTLY -> R.string.sky_partly
                            SkyCondition.CLOUDY -> R.string.sky_cloudy
                            SkyCondition.RAIN -> R.string.sky_rain
                            SkyCondition.SLEET -> R.string.sky_sleet
                            SkyCondition.SNOW -> R.string.sky_snow
                            else -> R.string.sky_unknown
                        }
                    ),
                    style = normal
                )
                val day = state.forecast.weekly.data?.firstOrNull { it.date == state.today }
                Text(context.getString(R.string.widget_extremes, number(day?.maximum) ?: "—", number(day?.minimum) ?: "—"), style = normal)
                Box(GlanceModifier.fillMaxWidth().height(Dims.hairline).background(colors.border)) {}
                val delayed = state.preferences.widgetError || observed.isStale || observed.isOffline || observed.error != null
                Text(
                    context.getString(
                        if (observed.data ==
                            null
                        ) {
                            R.string.widget_loading
                        } else if (delayed) {
                            R.string.widget_delayed
                        } else {
                            R.string.widget_frequency
                        }
                    ),
                    style = normal
                )
                Text(context.getString(R.string.widget_last_success, timeLabel(observed.meta?.receivedAt) ?: "—"), style = normal)
            }
        }
    }
}

class WeatherWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WeatherWidget()
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetScheduler.ensure(context)
    }
}
