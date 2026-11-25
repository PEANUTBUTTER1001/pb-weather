package com.pbweather.weather

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pbweather.weather.ui.navigation.WeatherActions
import com.pbweather.weather.ui.navigation.WeatherShell
import com.pbweather.weather.ui.navigation.WeatherViewModel
import com.pbweather.weather.ui.theme.FontSize
import com.pbweather.weather.ui.theme.Palette
import com.pbweather.weather.ui.theme.ThemeMode
import com.pbweather.weather.ui.theme.WeatherTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val model: WeatherViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleWidgetIntent(intent)
        setContent {
            val state by model.ui.collectAsStateWithLifecycle()
            val query by model.query.collectAsStateWithLifecycle()
            val results by model.results.collectAsStateWithLifecycle()
            val widgetTab by model.widgetTab.collectAsStateWithLifecycle()
            val mode = ThemeMode.entries.firstOrNull { it.name == state.preferences.theme } ?: ThemeMode.LIGHT
            val palette = Palette.entries.firstOrNull { it.name == state.preferences.palette } ?: Palette.INDIGO
            val font = FontSize.entries.firstOrNull { it.name == state.preferences.font } ?: FontSize.NORMAL
            val actions = remember(model) {
                WeatherActions(
                    model::search, model::select, model::refresh, model::locate,
                    model::updatePermission, model::undo, model::theme, model::palette, model::font
                )
            }
            WeatherTheme(mode, palette, font) { WeatherShell(state, query, results, mode, palette, actions, model.notices, widgetTab) }
        }
    }
    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleWidgetIntent(intent)
    }
    private fun handleWidgetIntent(intent: android.content.Intent?) {
        if (intent?.getBooleanExtra(com.pbweather.weather.widget.OPEN_WIDGET, false) == true) {
            intent.removeExtra(com.pbweather.weather.widget.OPEN_WIDGET)
            model.openWidget()
        }
    }
}
