package com.pbweather.weather.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.pbweather.weather.domain.SkyCondition
import com.pbweather.weather.ui.theme.Dims
import com.pbweather.weather.ui.theme.WeatherTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class UiIcon { HOME, HISTORY, PIN, SETTINGS, REFRESH, HELP, STAR, DELETE, NEXT, PREVIOUS }

@Composable fun AppIcon(icon: UiIcon, description: String?, modifier: Modifier = Modifier, tint: Color = WeatherTheme.colors.text) {
    Canvas(modifier.size(Dims.icon).semantics { if (description != null) contentDescription = description }) {
        val w = size.width
        val h = size.height
        val stroke = Dims.strong.toPx()
        fun line(a: Float, b: Float, c: Float, d: Float) = drawLine(tint, Offset(w * a, h * b), Offset(w * c, h * d), stroke)
        when (icon) {
            UiIcon.HOME -> {
                line(.1f, .45f, .5f, .1f)
                line(.5f, .1f, .9f, .45f)
                line(.2f, .4f, .2f, .9f)
                line(.2f, .9f, .8f, .9f)
                line(.8f, .9f, .8f, .4f)
            }
            UiIcon.HISTORY -> {
                drawCircle(tint, w * .38f, Offset(w / 2, h / 2), style = Stroke(stroke))
                line(.5f, .25f, .5f, .5f)
                line(.5f, .5f, .7f, .6f)
            }
            UiIcon.PIN -> {
                drawCircle(tint, w * .27f, Offset(w / 2, h * .38f), style = Stroke(stroke))
                line(.27f, .55f, .5f, .95f)
                line(.73f, .55f, .5f, .95f)
            }
            UiIcon.SETTINGS -> {
                drawCircle(tint, w * .18f, style = Stroke(stroke))
                repeat(8) { i ->
                    val a = i * PI / 4
                    drawLine(
                        tint,
                        Offset((.5 + cos(a) * .28).toFloat() * w, (.5 + sin(a) * .28).toFloat() * h),
                        Offset((.5 + cos(a) * .43).toFloat() * w, (.5 + sin(a) * .43).toFloat() * h),
                        stroke
                    )
                }
            }
            UiIcon.REFRESH -> {
                drawArc(tint, 45f, 285f, false, Offset(w * .1f, h * .1f), size * .8f, style = Stroke(stroke))
                line(.8f, .15f, .88f, .4f)
                line(.88f, .4f, .62f, .37f)
            }
            UiIcon.HELP -> {
                drawCircle(tint, w * .43f, style = Stroke(stroke))
                drawArc(tint, 190f, 230f, false, Offset(w * .35f, h * .2f), size * .3f, style = Stroke(stroke))
                line(.5f, .5f, .5f, .62f)
                drawCircle(tint, stroke / 2, Offset(w * .5f, h * .78f))
            }
            UiIcon.STAR -> {
                val path = Path()
                repeat(10) { i ->
                    val a = -PI / 2 + PI * i / 5
                    val r = if (i % 2 == 0) .44 else .2
                    val x = ((.5 + cos(a) * r) * w).toFloat()
                    val y = ((.5 + sin(a) * r) * h).toFloat()
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
                drawPath(path, tint, style = Stroke(stroke))
            }
            UiIcon.DELETE -> {
                line(.2f, .25f, .8f, .25f)
                line(.3f, .25f, .35f, .9f)
                line(.35f, .9f, .65f, .9f)
                line(.65f, .9f, .7f, .25f)
                line(.4f, .1f, .6f, .1f)
            }
            UiIcon.NEXT -> {
                line(.35f, .2f, .65f, .5f)
                line(.65f, .5f, .35f, .8f)
            }
            UiIcon.PREVIOUS -> {
                line(.65f, .2f, .35f, .5f)
                line(.35f, .5f, .65f, .8f)
            }
        }
    }
}

@Composable fun WeatherSymbol(sky: SkyCondition, modifier: Modifier = Modifier) {
    val color = WeatherTheme.colors.accent
    val warm = WeatherTheme.colors.warm
    val cool = WeatherTheme.colors.rainText
    val description = skyLabel(sky)
    Canvas(modifier.size(Dims.symbol).semantics { contentDescription = description }) {
        val w = size.width
        val h = size.height
        val stroke = Dims.strong.toPx()
        if (sky == SkyCondition.UNKNOWN) {
            drawLine(color, Offset(w * .3f, h * .5f), Offset(w * .7f, h * .5f), stroke)
            return@Canvas
        }
        if (sky == SkyCondition.CLEAR || sky == SkyCondition.PARTLY) {
            drawCircle(warm, w * .18f, Offset(w * .65f, h * .3f), style = Stroke(stroke))
            repeat(8) { i ->
                val a = i * PI / 4
                drawLine(
                    warm,
                    Offset((.65 + cos(a) * .23).toFloat() * w, (.3 + sin(a) * .23).toFloat() * h),
                    Offset((.65 + cos(a) * .3).toFloat() * w, (.3 + sin(a) * .3).toFloat() * h),
                    stroke
                )
            }
        }
        if (sky != SkyCondition.CLEAR) {
            drawCircle(color, w * .18f, Offset(w * .27f, h * .48f))
            drawCircle(color, w * .24f, Offset(w * .48f, h * .4f))
            drawCircle(color, w * .17f, Offset(w * .72f, h * .5f))
            drawRoundRect(color, Offset(w * .23f, h * .44f), androidx.compose.ui.geometry.Size(w * .5f, h * .22f))
        }
        if (sky == SkyCondition.RAIN || sky == SkyCondition.SLEET) {
            repeat(3) { i ->
                drawLine(cool, Offset(w * (.3f + i * .2f), h * .73f), Offset(w * (.25f + i * .2f), h * .9f), stroke)
            }
        }
        if (sky == SkyCondition.SNOW || sky == SkyCondition.SLEET) {
            repeat(3) { i ->
                drawCircle(cool, w * .035f, Offset(w * (.35f + i * .2f), h * if (i % 2 == 0) .85f else .76f))
            }
        }
    }
}
