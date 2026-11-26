package com.pbweather.weather.ui.home

import android.animation.ValueAnimator
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import com.pbweather.weather.domain.SkyCondition
import com.pbweather.weather.ui.theme.Dims
import com.pbweather.weather.ui.theme.Palette
import com.pbweather.weather.ui.theme.ThemeMode
import com.pbweather.weather.ui.theme.WeatherTheme
import com.pbweather.weather.ui.theme.heroColor
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.isActive

@Composable
fun HeroWeather(sky: SkyCondition, mode: ThemeMode, palette: Palette, active: Boolean, modifier: Modifier = Modifier) {
    val colors = WeatherTheme.colors
    val start = heroColor(mode, palette, sky)
    val ground = colors.ground
    val density = LocalDensity.current
    val context = LocalContext.current
    var durationScale by remember {
        mutableFloatStateOf(Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f))
    }
    DisposableEffect(context) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                durationScale = Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
            }
        }
        context.contentResolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, observer)
        onDispose { context.contentResolver.unregisterContentObserver(observer) }
    }
    val canAnimate = active && mode != ThemeMode.HIGH_CONTRAST && durationScale > 0f && ValueAnimator.areAnimatorsEnabled()
    val frame = remember { mutableLongStateOf(0L) }
    val particles = remember {
        val random = Random(321)
        FloatArray(64 * 4) { index -> if (index % 4 == 2) .25f + random.nextFloat() * .75f else random.nextFloat() }
    }
    val gradient = remember(start, ground) { Brush.verticalGradient(listOf(start, ground)) }
    val bottomAlpha = if (mode == ThemeMode.DARK) {
        floatArrayOf(.78f, .8f, .8f, .8f)[palette.ordinal]
    } else {
        floatArrayOf(.74f, .78f, .76f, .78f)[palette.ordinal]
    }
    val scrim = remember(ground, bottomAlpha, mode) {
        Brush.verticalGradient(listOf(ground.copy(alpha = if (mode == ThemeMode.DARK) .12f else .08f), ground.copy(alpha = bottomAlpha)))
    }
    val corona = with(density) { Dims.corona.toPx() }
    val pulse = with(density) { Dims.coronaPulse.toPx() }
    val beamLength = with(density) { Dims.beamLength.toPx() }
    val sunInset = with(density) { Dims.sunInset.toPx() }
    val sunTop = with(density) { Dims.weatherSmall.toPx() }
    val beam = remember(density) {
        val near = with(density) { Dims.beamNear.toPx() }
        val far = with(density) { Dims.beamFar.toPx() }
        Path().apply {
            moveTo(0f, -near)
            lineTo(beamLength, -far)
            lineTo(beamLength, far)
            lineTo(0f, near)
            close()
        }
    }
    val beamBrush = remember(colors, mode, beamLength) {
        Brush.horizontalGradient(
            listOf(
                colors.sunGlow.copy(
                    alpha = if (mode ==
                        ThemeMode.DARK
                    ) {
                        .16f
                    } else {
                        .24f
                    }
                ),
                colors.sunGlow.copy(alpha = 0f)
            ),
            endX = beamLength
        )
    }
    val haloBrush = remember(colors, mode, corona) {
        Brush.radialGradient(
            0f to colors.sunGlow.copy(alpha = if (mode == ThemeMode.DARK) .28f else .42f),
            .55f to colors.sunHalo.copy(alpha = if (mode == ThemeMode.DARK) .12f else .16f),
            1f to colors.sunGlow.copy(alpha = 0f),
            center = Offset.Zero,
            radius = corona
        )
    }
    LaunchedEffect(canAnimate) {
        var previous = 0L
        val updateFrame: (Long) -> Unit = { current ->
            if (previous != 0L) frame.longValue += current - previous
            previous = current
        }
        if (canAnimate) while (isActive) withInfiniteAnimationFrameNanos(updateFrame)
    }
    Canvas(modifier.fillMaxSize()) {
        drawRect(gradient)
        if (mode == ThemeMode.HIGH_CONTRAST) return@Canvas
        val t = (frame.longValue / 1000000000.0).toFloat()
        val w = size.width
        val h = size.height
        val center = Offset(w - sunInset, sunTop)
        val beams = when (sky) {
            SkyCondition.CLEAR -> 8
            SkyCondition.PARTLY -> 5
            else -> 0
        }
        if (beams > 0) {
            withTransform({
                translate(center.x, center.y)
                rotate(t * 5.15662f, Offset.Zero)
            }) {
                repeat(beams) { i ->
                    withTransform({ rotate((i + 1) * 360f / beams, Offset.Zero) }) { drawPath(beam, beamBrush) }
                }
            }
            val radius = corona + sin(t * 1.4f) * pulse
            withTransform({
                translate(center.x, center.y)
                scale(radius / corona, radius / corona, Offset.Zero)
            }) {
                drawCircle(haloBrush, corona, Offset.Zero)
            }
        }
        val clouds = when (sky) {
            SkyCondition.CLEAR, SkyCondition.UNKNOWN -> 0
            SkyCondition.CLOUDY -> 7
            else -> 5
        }
        repeat(clouds) { i ->
            val x = ((particles[i * 4] + t * .006f * particles[i * 4 + 2]) % 1.5f - .25f) * w
            val y = (.08f + particles[i * 4 + 1] * .55f) * h
            drawCircle(colors.secondary.copy(alpha = .055f), w * (.18f + particles[i * 4 + 3] * .13f), Offset(x, y))
        }
        val rain = if (sky == SkyCondition.RAIN) {
            64
        } else if (sky == SkyCondition.SLEET) {
            32
        } else {
            0
        }
        repeat(rain) { i ->
            val x = ((particles[i * 4] - t * .18f * particles[i * 4 + 2]) % 1.2f + 1.2f) % 1.2f * w
            val y = ((particles[i * 4 + 1] + t * .65f * particles[i * 4 + 2]) % 1.2f) * h
            drawLine(colors.rain.copy(alpha = .17f), Offset(x, y), Offset(x - w * .025f, y + h * .065f), Dims.hairline.toPx())
        }
        val snow = sky == SkyCondition.SNOW || sky == SkyCondition.SLEET
        val count = when (sky) {
            SkyCondition.CLEAR -> 28
            SkyCondition.PARTLY -> 18
            SkyCondition.CLOUDY -> 26
            SkyCondition.SLEET, SkyCondition.SNOW -> 46
            else -> 0
        }
        repeat(count) { i ->
            val speed = particles[i * 4 + 2]
            val x = ((particles[i * 4] + (if (snow) sin(t * .35f + i) * .04f else t * .006f * speed)) % 1f + 1f) % 1f * w
            val y = ((particles[i * 4 + 1] + if (snow) t * .045f * speed else 0f) % 1f) * h
            drawCircle(
                (if (snow) colors.text else colors.accent).copy(alpha = if (snow) .14f else .035f),
                Dims.strong.toPx() * (1f + speed),
                Offset(x, y)
            )
        }
        drawRect(scrim)
    }
}
