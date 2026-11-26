package com.pbweather.weather.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.pbweather.weather.R
import com.pbweather.weather.domain.SkyCondition
import com.pbweather.weather.ui.theme.Dims
import com.pbweather.weather.ui.theme.WeatherTheme

enum class UiIcon {
    HOME,
    HISTORY,
    PIN,
    SEARCH,
    SETTINGS,
    REFRESH,
    HELP,
    STAR,
    STAR_FILLED,
    DELETE,
    NEXT,
    PREVIOUS,
    CLOSE,
    SUN,
    MOON,
    CHECK,
    SHIELD,
    EMPTY,
    CALENDAR
}

@Composable fun AppIcon(icon: UiIcon, description: String?, modifier: Modifier = Modifier, tint: Color = WeatherTheme.colors.text) {
    val resource = when (icon) {
        UiIcon.HOME -> R.drawable.design_icon_home
        UiIcon.HISTORY -> R.drawable.design_icon_history
        UiIcon.PIN -> R.drawable.design_icon_pin
        UiIcon.SEARCH -> R.drawable.design_icon_search
        UiIcon.SETTINGS -> R.drawable.design_icon_settings
        UiIcon.REFRESH -> R.drawable.design_icon_refresh
        UiIcon.HELP -> R.drawable.design_icon_help
        UiIcon.STAR -> R.drawable.design_icon_star
        UiIcon.STAR_FILLED -> R.drawable.design_icon_star_filled
        UiIcon.DELETE -> R.drawable.design_icon_delete
        UiIcon.NEXT -> R.drawable.design_icon_next
        UiIcon.PREVIOUS -> R.drawable.design_icon_previous
        UiIcon.CLOSE -> R.drawable.design_icon_close
        UiIcon.SUN -> R.drawable.design_icon_sun
        UiIcon.MOON -> R.drawable.design_icon_moon
        UiIcon.CHECK -> R.drawable.design_icon_check
        UiIcon.SHIELD -> R.drawable.design_icon_shield
        UiIcon.EMPTY -> R.drawable.design_icon_empty
        UiIcon.CALENDAR -> R.drawable.design_icon_calendar
    }
    Icon(painterResource(resource), description, modifier.size(Dims.icon), tint)
}

@Composable fun WeatherSymbol(sky: SkyCondition, modifier: Modifier = Modifier) {
    val resource = when (sky) {
        SkyCondition.CLEAR -> R.drawable.design_weather_clear
        SkyCondition.PARTLY -> R.drawable.design_weather_partly
        SkyCondition.CLOUDY -> R.drawable.design_weather_cloudy
        SkyCondition.SLEET, SkyCondition.SNOW -> R.drawable.design_weather_sleet
        else -> R.drawable.design_weather_rain
    }
    if (sky == SkyCondition.UNKNOWN) {
        Text(missing(null), modifier)
    } else {
        Image(painterResource(resource), skyLabel(sky), modifier.size(Dims.symbol))
    }
}
