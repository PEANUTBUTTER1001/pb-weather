package com.pbweather.weather.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.pbweather.weather.R
import com.pbweather.weather.ui.theme.Dims
import com.pbweather.weather.ui.theme.ThemeMode
import com.pbweather.weather.ui.theme.WeatherTheme

@Composable fun CompactOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(contentColor = WeatherTheme.colors.text),
    content: @Composable RowScope.() -> Unit
) {
    OutlinedButton(
        onClick,
        modifier.heightIn(min = Dims.action),
        enabled,
        shape = RoundedCornerShape(Dims.small),
        colors = colors,
        border = BorderStroke(Dims.hairline, WeatherTheme.colors.borderStrong),
        contentPadding = PaddingValues(horizontal = Dims.medium, vertical = Dims.tiny),
        content = content
    )
}

@Composable
fun PrimaryButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable RowScope.() -> Unit) {
    Button(
        onClick,
        modifier.heightIn(min = Dims.field),
        enabled,
        contentPadding = PaddingValues(horizontal = Dims.gutter, vertical = Dims.small),
        content = content
    )
}

@Composable fun DeltaBadge(label: String, value: Double?, modifier: Modifier = Modifier) {
    val colors = WeatherTheme.colors
    val color = when {
        value == null || kotlin.math.abs(value) < .05 -> colors.text
        value > 0 -> colors.warm
        else -> colors.cool
    }
    Surface(
        modifier,
        shape = RoundedCornerShape(Dims.pill),
        color = colors.tonal,
        border = if (WeatherTheme.mode == ThemeMode.HIGH_CONTRAST) BorderStroke(Dims.hairline, colors.border) else null
    ) {
        Text(
            label,
            Modifier.padding(horizontal = Dims.small, vertical = Dims.segmentPad),
            color = color,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable fun SheetHeader(title: String, onClose: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), style = WeatherTheme.type.sheetTitle)
        IconButton(onClick = onClose) { AppIcon(UiIcon.CLOSE, stringResource(R.string.close)) }
    }
}

@Composable fun KeyValueGrid(values: List<Pair<String, String>>, expanded: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(Dims.gridGap)) {
        values.chunked(if (expanded) 1 else 2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(Dims.gridGap)) {
                row.forEach { (label, value) ->
                    Surface(Modifier.weight(1f), shape = RoundedCornerShape(Dims.small), color = WeatherTheme.colors.tonal) {
                        Column(Modifier.padding(horizontal = Dims.medium, vertical = Dims.gridGap)) {
                            Text(label, style = MaterialTheme.typography.bodySmall, color = WeatherTheme.colors.secondary)
                            Text(value, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
    }
}
