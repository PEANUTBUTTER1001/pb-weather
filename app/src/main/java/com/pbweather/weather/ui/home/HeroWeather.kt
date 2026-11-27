package com.pbweather.weather.ui.home

import android.app.ActivityManager
import android.content.Context
import android.database.ContentObserver
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pbweather.weather.domain.SkyCondition
import com.pbweather.weather.ui.theme.Palette
import com.pbweather.weather.ui.theme.ThemeMode
import com.pbweather.weather.ui.theme.WeatherTheme
import com.pbweather.weather.ui.theme.heroColor

/**
 * Renders the home hero weather background using Android 13+ AGSL shaders via a dedicated
 * Choreographer-driven View for active tiers, or a static gradient fallback otherwise.
 */
@Composable
fun HeroWeather(
    sky: SkyCondition,
    mode: ThemeMode,
    palette: Palette,
    modifier: Modifier = Modifier,
    windMs: Double? = null,
    hourOfDay: Int? = null,
    motionEnabled: Boolean = true,
    isVisible: Boolean = true,
    tierOverride: HeroMotionTier? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val colors = WeatherTheme.colors
    val ground = colors.ground
    val start = heroColor(mode, palette, sky)
    val accent = colors.accent

    var animatorScale by remember(context) { mutableFloatStateOf(readAnimatorScale(context)) }
    var powerSave by remember(context) { mutableStateOf(readPowerSave(context)) }
    var resumed by remember(lifecycleOwner) {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
    }

    DisposableEffect(context, lifecycleOwner) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                animatorScale = readAnimatorScale(context)
            }
        }
        runCatching {
            context.contentResolver.registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
                false,
                observer
            )
        }
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START, Lifecycle.Event.ON_RESUME -> {
                    resumed = true
                    animatorScale = readAnimatorScale(context)
                    powerSave = readPowerSave(context)
                }
                Lifecycle.Event.ON_STOP, Lifecycle.Event.ON_PAUSE -> {
                    resumed = false
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        onDispose {
            runCatching { context.contentResolver.unregisterContentObserver(observer) }
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
        }
    }

    val hardware = remember(context) { readDeviceHardware(context) }
    val resolvedPolicyTier = HeroMotionPolicy.resolve(
        HeroMotionSignals(
            sdkInt = Build.VERSION.SDK_INT,
            mode = mode,
            sky = sky,
            userEnabled = motionEnabled,
            animatorScale = animatorScale,
            powerSave = powerSave,
            lowRam = hardware.lowRam,
            totalMemoryMb = hardware.totalMemoryMb,
            cpuCores = hardware.cpuCores
        )
    )
    val tier = if (Build.VERSION.SDK_INT < HeroMotionPolicy.MIN_AGSL_SDK ||
        mode == ThemeMode.HIGH_CONTRAST ||
        sky == SkyCondition.UNKNOWN ||
        !motionEnabled
    ) {
        HeroMotionTier.STATIC
    } else {
        tierOverride ?: resolvedPolicyTier
    }

    if (Build.VERSION.SDK_INT >= HeroMotionPolicy.MIN_AGSL_SDK && tier != HeroMotionTier.STATIC) {
        val windNorm = HeroMotionPolicy.normalizeWind(windMs)
        val phase = HeroMotionPolicy.dayPhase(hourOfDay)
        val startArgb = start.toArgb()
        val groundArgb = ground.toArgb()
        val accentArgb = accent.toArgb()
        val active = resumed && isVisible
        AndroidView(
            factory = { ctx -> HeroShaderView(ctx) },
            update = { view ->
                view.update(
                    sky = sky,
                    mode = mode,
                    palette = palette,
                    startColor = startArgb,
                    bottomColor = groundArgb,
                    accentColor = accentArgb,
                    tier = tier,
                    windNorm = windNorm,
                    dayPhase = phase,
                    active = active
                )
            },
            modifier = modifier.fillMaxSize()
        )
    } else {
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
}

private data class DeviceHardwareInfo(val lowRam: Boolean, val totalMemoryMb: Long, val cpuCores: Int)

private fun readDeviceHardware(context: Context): DeviceHardwareInfo {
    val am = context.getSystemService(ActivityManager::class.java)
    val info = ActivityManager.MemoryInfo()
    am?.getMemoryInfo(info)
    return DeviceHardwareInfo(
        lowRam = am?.isLowRamDevice == true,
        totalMemoryMb = info.totalMem / (1024L * 1024L),
        cpuCores = Runtime.getRuntime().availableProcessors()
    )
}

private fun readAnimatorScale(context: Context): Float = runCatching {
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
}.getOrDefault(1f)

private fun readPowerSave(context: Context): Boolean = context.getSystemService(PowerManager::class.java)?.isPowerSaveMode == true
