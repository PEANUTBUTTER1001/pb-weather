package com.pbweather.weather.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.pow
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherThemeTest {
    private fun luminance(color: Color): Double {
        fun linear(value: Float): Double = if (value <= 0.04045f) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
        return 0.2126 * linear(color.red) + 0.7152 * linear(color.green) + 0.0722 * linear(color.blue)
    }
    private fun ratio(a: Color, b: Color): Double {
        val x = luminance(a)
        val y = luminance(b)
        return (maxOf(x, y) + 0.05) / (minOf(x, y) + 0.05)
    }

    @Test fun `text and data labels meet contrast across all nine themes`() {
        for (mode in ThemeMode.entries) {
            for (palette in Palette.entries) {
                val c = weatherColors(mode, palette)
                for (background in listOf(c.surface, c.tonal, c.ground)) {
                    for (text in listOf(c.text, c.secondary, c.tertiary, c.rainText)) {
                        assertTrue("$mode/$palette contrast ${ratio(text,background)}", ratio(text, background) >= 4.5)
                    }
                }
                assertTrue(ratio(c.accent, c.onAccent) >= 4.5)
                assertTrue(ratio(c.accentSoft, c.accentOnSoft) >= 4.5)
            }
        }
    }
}
