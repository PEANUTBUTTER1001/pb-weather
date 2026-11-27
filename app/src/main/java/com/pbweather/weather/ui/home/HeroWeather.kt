package com.pbweather.weather.ui.home

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import com.pbweather.weather.domain.SkyCondition
import com.pbweather.weather.ui.theme.Palette
import com.pbweather.weather.ui.theme.ThemeMode
import com.pbweather.weather.ui.theme.WeatherTheme
import com.pbweather.weather.ui.theme.heroColor

/** Static weather gradient; no frame loop, particles, or system animation observer. */
@Composable
fun HeroWeather(sky: SkyCondition, mode: ThemeMode, palette: Palette, modifier: Modifier = Modifier) {
    val ground = WeatherTheme.colors.ground
    val start = heroColor(mode, palette, sky)
    Spacer(
        modifier.fillMaxSize().drawWithCache {
            val gradient = Brush.verticalGradient(listOf(start, ground))
            val bottomAlpha = if (mode == ThemeMode.DARK) {
                floatArrayOf(.78f, .8f, .8f, .8f)[palette.ordinal]
            } else {
                floatArrayOf(.74f, .78f, .76f, .78f)[palette.ordinal]
            }
            val scrim = Brush.verticalGradient(
                listOf(ground.copy(alpha = if (mode == ThemeMode.DARK) .12f else .08f), ground.copy(alpha = bottomAlpha)),
                endY = size.height * .92f
            )
            onDrawBehind {
                drawRect(gradient)
                if (mode != ThemeMode.HIGH_CONTRAST) drawRect(scrim)
            }
        }
    )
}
