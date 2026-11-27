package com.pbweather.weather.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.pbweather.weather.R
import com.pbweather.weather.ui.theme.Dims
import com.pbweather.weather.ui.theme.ThemeMode
import com.pbweather.weather.ui.theme.WeatherTheme

@Composable fun HomeLoadingSkeleton() {
    val label = stringResource(R.string.loading)
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).semantics {
            contentDescription = label
        }.padding(Dims.gutter),
        verticalArrangement = Arrangement.spacedBy(Dims.gutter)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(verticalArrangement = Arrangement.spacedBy(Dims.small)) {
                Skeleton(Modifier.width(Dims.skeletonTemperatureWidth).height(Dims.skeletonTemperatureHeight))
                Skeleton(Modifier.width(Dims.skeletonSubtitleWidth).height(Dims.large))
            }
            Skeleton(Modifier.size(Dims.weeklyWeather))
        }
        repeat(2) {
            Row(horizontalArrangement = Arrangement.spacedBy(Dims.medium)) {
                repeat(2) { Skeleton(Modifier.weight(1f).height(Dims.action)) }
            }
        }
        Skeleton(Modifier.fillMaxWidth().height(Dims.touch))
        listOf(Dims.skeletonTemperatureHeight, Dims.skeletonForecast, Dims.skeletonWeekly).forEach { height ->
            SectionCard { Skeleton(Modifier.fillMaxWidth().height(height)) }
        }
    }
}

@Composable private fun Skeleton(modifier: Modifier) {
    Surface(modifier, shape = RoundedCornerShape(Dims.small), color = WeatherTheme.colors.tonal) {
        Box(Modifier.fillMaxSize())
    }
}

@Composable fun EmptyWeather(onRetry: () -> Unit, onSelect: () -> Unit) {
    val large = LocalDensity.current.fontScale * WeatherTheme.fontScale >= 1.8f

    @Composable fun actions() {
        PrimaryButton(onRetry) { Text(stringResource(R.string.retry)) }
        SecondaryButton(onSelect) { Text(stringResource(R.string.select_region)) }
    }
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = Dims.gutter, vertical = Dims.weatherSmall)) {
        Surface(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(Dims.radius),
            color = WeatherTheme.colors.surface,
            shadowElevation = if (WeatherTheme.mode == ThemeMode.HIGH_CONTRAST) Dims.zero else Dims.cardElevation,
            border = BorderStroke(Dims.hairline, WeatherTheme.colors.border)
        ) {
            Column(
                Modifier.padding(horizontal = Dims.sheet, vertical = Dims.emptyPadding),
                verticalArrangement = Arrangement.spacedBy(Dims.medium),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier.size(Dims.navPillWidth).background(WeatherTheme.colors.tonal, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    AppIcon(UiIcon.EMPTY, null, Modifier.size(Dims.weatherSmall))
                }
                Text(
                    stringResource(R.string.design_no_saved_weather),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
                Text(
                    stringResource(R.string.design_no_saved_explanation),
                    style = MaterialTheme.typography.bodyLarge,
                    color = WeatherTheme.colors.secondary,
                    textAlign = TextAlign.Center
                )
                if (large) {
                    Column(verticalArrangement = Arrangement.spacedBy(Dims.small), horizontalAlignment = Alignment.CenterHorizontally) {
                        actions()
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(Dims.small)) { actions() }
                }
            }
        }
    }
}

@Composable fun CompactOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(contentColor = WeatherTheme.colors.text),
    borderColor: Color = WeatherTheme.colors.borderStrong,
    content: @Composable RowScope.() -> Unit
) {
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
        OutlinedButton(
            onClick,
            modifier.widthIn(min = Dims.hairline).heightIn(min = Dims.action),
            enabled,
            shape = RoundedCornerShape(Dims.small),
            colors = colors,
            border = BorderStroke(Dims.hairline, borderColor),
            contentPadding = PaddingValues(horizontal = Dims.medium, vertical = Dims.tiny),
            content = content
        )
    }
}

@Composable
fun PrimaryButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable RowScope.() -> Unit) {
    Button(
        onClick,
        modifier.heightIn(min = Dims.field),
        enabled,
        shape = RoundedCornerShape(Dims.groupRadius),
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
        IconButton(onClick = onClose, modifier = Modifier.size(Dims.field).offset(x = Dims.gridGap)) {
            AppIcon(UiIcon.CLOSE, stringResource(R.string.close))
        }
    }
}

@Composable fun KeyValueGrid(values: List<Pair<String, String>>, expanded: Boolean, tones: List<Color?> = emptyList()) {
    Column(verticalArrangement = Arrangement.spacedBy(Dims.gridGap)) {
        values.chunked(if (expanded) 1 else 2).forEachIndexed { rowIndex, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(Dims.gridGap)) {
                row.forEachIndexed { columnIndex, (label, value) ->
                    Surface(Modifier.weight(1f), shape = RoundedCornerShape(Dims.small), color = WeatherTheme.colors.tonal) {
                        Column(
                            Modifier.padding(horizontal = Dims.medium, vertical = Dims.gridGap),
                            verticalArrangement = Arrangement.spacedBy(Dims.segmentGap)
                        ) {
                            Text(label, style = WeatherTheme.type.kvLabel, color = WeatherTheme.colors.secondary)
                            Text(
                                value,
                                style = WeatherTheme.type.kvValue,
                                color =
                                tones.getOrNull(rowIndex * (if (expanded) 1 else 2) + columnIndex) ?: WeatherTheme.colors.text
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable fun CompactTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    isError: Boolean = false,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Dims.tiny)) {
        val colors = WeatherTheme.colors
        BasicTextField(
            value,
            onValueChange,
            Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.text),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(colors.accent),
            decorationBox = { inner ->
                Column(verticalArrangement = Arrangement.spacedBy(Dims.tiny)) {
                    if (label != null) Text(label, style = WeatherTheme.type.meta, color = WeatherTheme.colors.secondary)
                    Surface(
                        shape = RoundedCornerShape(Dims.small),
                        color = colors.surface,
                        border = BorderStroke(Dims.hairline, if (isError) colors.error else colors.borderStrong)
                    ) {
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = Dims.field).padding(horizontal = Dims.medium, vertical = Dims.small),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.weight(1f)) {
                                if (value.isEmpty() &&
                                    placeholder != null
                                ) {
                                    Text(
                                        placeholder,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = colors.tertiary,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                                inner()
                            }
                            trailingIcon?.invoke()
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun SecondaryButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable RowScope.() -> Unit) {
    OutlinedButton(
        onClick,
        modifier.heightIn(min = Dims.field),
        enabled = enabled,
        shape = RoundedCornerShape(Dims.pill),
        border = BorderStroke(Dims.hairline, WeatherTheme.colors.borderStrong),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = WeatherTheme.colors.text),
        contentPadding = PaddingValues(horizontal = Dims.gutter, vertical = Dims.small),
        content = content
    )
}

@Composable fun PrototypeConfirmDialog(
    title: String,
    body: String,
    confirmLabel: String,
    cancelLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.padding(horizontal = Dims.gutter).widthIn(max = Dims.dialogWidth).fillMaxWidth(),
            shape = RoundedCornerShape(Dims.sheetRadius),
            color = WeatherTheme.colors.surface,
            border = BorderStroke(
                if (WeatherTheme.mode == ThemeMode.HIGH_CONTRAST) Dims.strong else Dims.hairline,
                WeatherTheme.colors.border
            )
        ) {
            Column(
                Modifier.heightIn(max = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp * .88f)
                    .verticalScroll(rememberScrollState()).padding(Dims.gutter),
                verticalArrangement = Arrangement.spacedBy(Dims.medium)
            ) {
                Text(title, style = WeatherTheme.type.sheetTitle)
                Text(body, style = MaterialTheme.typography.bodyLarge, color = WeatherTheme.colors.secondary)
                androidx.compose.foundation.layout.FlowRow(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dims.small, Alignment.End),
                    verticalArrangement = Arrangement.spacedBy(Dims.small)
                ) {
                    SecondaryButton(onDismiss) { Text(cancelLabel) }
                    if (destructive) {
                        Button(
                            onClick = onConfirm,
                            modifier = Modifier.heightIn(min = Dims.field),
                            shape = RoundedCornerShape(Dims.pill),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WeatherTheme.colors.destructive,
                                contentColor = WeatherTheme.colors.onAccent
                            )
                        ) {
                            Text(confirmLabel)
                        }
                    } else {
                        PrimaryButton(onConfirm) { Text(confirmLabel) }
                    }
                }
            }
        }
    }
}

@Composable fun MetaRow(label: String, value: String, tone: Color = WeatherTheme.colors.text) {
    val style = WeatherTheme.type.metaRow
    val secondary = WeatherTheme.colors.secondary
    val primary = tone
    Column {
        Rule()
        SubcomposeLayout(Modifier.fillMaxWidth().padding(vertical = Dims.small)) { constraints ->
            val key = subcompose("key") {
                Text(label, style = style, color = secondary)
            }.single().measure(Constraints(maxWidth = constraints.maxWidth))
            val data = subcompose("value") {
                Text(value, style = style, color = primary)
            }.single().measure(Constraints(maxWidth = constraints.maxWidth))
            val inline = key.width + Dims.gridGap.roundToPx() + data.width <= constraints.maxWidth
            val height = if (inline) maxOf(key.height, data.height) else key.height + Dims.segmentGap.roundToPx() + data.height
            layout(constraints.maxWidth, height) {
                key.placeRelative(0, if (inline) (height - key.height) / 2 else 0)
                data.placeRelative(
                    if (inline) constraints.maxWidth - data.width else 0,
                    if (inline) {
                        (height - data.height) / 2
                    } else {
                        key.height +
                            Dims.segmentGap.roundToPx()
                    }
                )
            }
        }
    }
}

@Composable fun prototypeSheetModifier(): Modifier = Modifier
    .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
    .padding(start = Dims.medium, end = Dims.medium, bottom = Dims.medium)

@Composable fun prototypeSheetBorder(): Modifier = Modifier.border(
    if (WeatherTheme.mode ==
        ThemeMode.HIGH_CONTRAST
    ) {
        Dims.strong
    } else {
        Dims.hairline
    },
    WeatherTheme.colors.border,
    RoundedCornerShape(Dims.sheetRadius)
)
