package com.pbweather.weather.ui.home

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.view.Choreographer
import android.view.View
import androidx.annotation.RequiresApi
import androidx.core.graphics.ColorUtils
import com.pbweather.weather.domain.SkyCondition
import com.pbweather.weather.ui.theme.Palette
import com.pbweather.weather.ui.theme.ThemeMode

/** Safe wrapper capturing runtime shader compilation failures without crashing UI rendering. */
internal class HeroShaderFallbackState<T>(factory: () -> T) {
    val instance: T?
    val failed: Boolean

    init {
        val result = runCatching(factory)
        instance = result.getOrNull()
        failed = result.isFailure
    }
}

@RequiresApi(33)
internal fun createRuntimeShader(): HeroShaderFallbackState<RuntimeShader> =
    HeroShaderFallbackState { RuntimeShader(HeroShaderSource.AGSL) }

/**
 * Hardware-accelerated View rendering AGSL atmospheric weather shaders via Choreographer
 * without triggering Compose snapshot invalidations or Recomposer work.
 */
@RequiresApi(33)
internal class HeroShaderView(context: Context) : View(context) {
    private val shaderState = createRuntimeShader()
    private val shaderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val scrimPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var sky = SkyCondition.CLEAR
    private var mode = ThemeMode.LIGHT
    private var palette = Palette.INDIGO
    private var startColor = 0
    private var bottomColor = 0
    private var accentColor = 0
    private var tier = HeroMotionTier.STATIC
    private var windNorm = 0f
    private var dayPhase = 0f
    private var active = false

    private var elapsedSec = 0f
    private var lastFrameNanos = 0L
    private var running = false

    private var cachedWidth = -1
    private var cachedHeight = -1
    private var cachedStartColor = 0
    private var cachedBottomColor = 0
    private var cachedMode = ThemeMode.LIGHT
    private var cachedPalette = Palette.INDIGO
    private var fallbackGradient: LinearGradient? = null

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!running) return
            val interval = HeroMotionPolicy.frameIntervalNanos(tier)
            if (lastFrameNanos == 0L) {
                lastFrameNanos = frameTimeNanos
                elapsedSec = HeroMotionPolicy.wrapTimeSeconds(elapsedSec + 0.016f)
                invalidate()
            } else if (frameTimeNanos - lastFrameNanos >= interval) {
                val deltaSec = (frameTimeNanos - lastFrameNanos) / 1_000_000_000f
                lastFrameNanos = frameTimeNanos
                elapsedSec = HeroMotionPolicy.wrapTimeSeconds(elapsedSec + deltaSec)
                invalidate()
            }
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    fun update(
        sky: SkyCondition,
        mode: ThemeMode,
        palette: Palette,
        startColor: Int,
        bottomColor: Int,
        accentColor: Int,
        tier: HeroMotionTier,
        windNorm: Float,
        dayPhase: Float,
        active: Boolean
    ) {
        val visualChanged = this.sky != sky ||
            this.mode != mode ||
            this.palette != palette ||
            this.startColor != startColor ||
            this.bottomColor != bottomColor ||
            this.accentColor != accentColor ||
            this.tier != tier ||
            this.windNorm != windNorm ||
            this.dayPhase != dayPhase

        this.sky = sky
        this.mode = mode
        this.palette = palette
        this.startColor = startColor
        this.bottomColor = bottomColor
        this.accentColor = accentColor
        this.tier = tier
        this.windNorm = windNorm
        this.dayPhase = dayPhase
        this.active = active

        syncLoop()
        if (visualChanged) invalidate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        syncLoop()
    }

    override fun onDetachedFromWindow() {
        stopLoop()
        super.onDetachedFromWindow()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        cachedWidth = -1
    }

    private fun shouldRun(): Boolean = active &&
        isAttachedToWindow &&
        tier != HeroMotionTier.STATIC &&
        !shaderState.failed &&
        HeroMotionPolicy.shaderModeIndex(sky) >= 0f

    private fun syncLoop() {
        if (shouldRun()) startLoop() else stopLoop()
    }

    private fun startLoop() {
        if (running) return
        running = true
        lastFrameNanos = 0L
        Choreographer.getInstance().postFrameCallback(frameCallback)
    }

    private fun stopLoop() {
        if (!running) return
        running = false
        lastFrameNanos = 0L
        Choreographer.getInstance().removeFrameCallback(frameCallback)
    }

    private fun ensureGradients(w: Int, h: Int) {
        if (cachedWidth == w &&
            cachedHeight == h &&
            cachedStartColor == startColor &&
            cachedBottomColor == bottomColor &&
            cachedMode == mode &&
            cachedPalette == palette &&
            fallbackGradient != null
        ) {
            return
        }
        val hf = h.toFloat().coerceAtLeast(1f)
        fallbackGradient = LinearGradient(0f, 0f, 0f, hf, startColor, bottomColor, Shader.TileMode.CLAMP)
        val isDark = mode == ThemeMode.DARK
        val bottomAlpha = if (isDark) {
            floatArrayOf(.78f, .8f, .8f, .8f)[palette.ordinal]
        } else {
            floatArrayOf(.74f, .78f, .76f, .78f)[palette.ordinal]
        }
        val topAlpha = if (isDark) .12f else .08f
        val scrimTop = ColorUtils.setAlphaComponent(bottomColor, (topAlpha * 255f).toInt())
        val scrimBottom = ColorUtils.setAlphaComponent(bottomColor, (bottomAlpha * 255f).toInt())
        scrimPaint.shader = LinearGradient(0f, 0f, 0f, hf * .92f, scrimTop, scrimBottom, Shader.TileMode.CLAMP)
        cachedWidth = w
        cachedHeight = h
        cachedStartColor = startColor
        cachedBottomColor = bottomColor
        cachedMode = mode
        cachedPalette = palette
    }

    override fun onDraw(canvas: Canvas) {
        val w = width
        val h = height
        if (w <= 0 || h <= 0) return
        ensureGradients(w, h)
        val wf = w.toFloat()
        val hf = h.toFloat()
        val shader = shaderState.instance
        val modeIdx = HeroMotionPolicy.shaderModeIndex(sky)
        if (shader != null && tier != HeroMotionTier.STATIC && modeIdx >= 0f) {
            shader.setFloatUniform("uResolution", wf, hf)
            shader.setFloatUniform("uTime", elapsedSec)
            shader.setFloatUniform("uScale", HeroMotionPolicy.renderScale(tier))
            shader.setFloatUniform("uMode", modeIdx)
            shader.setFloatUniform("uWind", windNorm)
            shader.setFloatUniform("uDayPhase", dayPhase)
            shader.setFloatUniform("uDark", if (mode == ThemeMode.DARK) 1f else 0f)
            shader.setColorUniform("uTopColor", startColor)
            shader.setColorUniform("uBottomColor", bottomColor)
            shader.setColorUniform("uAccentColor", accentColor)
            shaderPaint.shader = shader
            canvas.drawRect(0f, 0f, wf, hf, shaderPaint)
        } else {
            shaderPaint.shader = fallbackGradient
            canvas.drawRect(0f, 0f, wf, hf, shaderPaint)
        }
        if (mode != ThemeMode.HIGH_CONTRAST) {
            canvas.drawRect(0f, 0f, wf, hf, scrimPaint)
        }
    }
}

/** Compact AGSL shader implementing atmospheric scattering, cloud drift, rain streaks, and bokeh snow. */
internal object HeroShaderSource {
    val AGSL = """
        uniform float2 uResolution;
        uniform float uTime;
        uniform float uScale;
        uniform float uMode;
        uniform float uWind;
        uniform float uDayPhase;
        uniform float uDark;
        layout(color) uniform half4 uTopColor;
        layout(color) uniform half4 uBottomColor;
        layout(color) uniform half4 uAccentColor;

        float hash21(float2 p) {
            p = fract(p * float2(123.34, 456.21));
            p += dot(p, p + 45.32);
            return fract(p.x * p.y);
        }

        float valueNoise(float2 p) {
            float2 i = floor(p);
            float2 f = fract(p);
            f = f * f * (3.0 - 2.0 * f);
            float a = hash21(i);
            float b = hash21(i + float2(1.0, 0.0));
            float c = hash21(i + float2(0.0, 1.0));
            float d = hash21(i + float2(1.0, 1.0));
            return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
        }

        float fbm(float2 p, float scale) {
            float sum = 0.0;
            float amp = 0.52;
            float2 shift = float2(19.3, 7.1);
            for (int i = 0; i < 3; i++) {
                if (i == 2 && scale < 0.75) {
                    break;
                }
                sum += amp * valueNoise(p);
                p = p * 2.02 + shift;
                amp *= 0.5;
            }
            return sum;
        }

        float rainLayer(float2 uv, float t, float wind, float seed, float density) {
            float slant = 0.14 + wind * 0.24;
            float2 p = float2(uv.x + uv.y * slant, uv.y);
            p.x *= density;
            p.y *= density * 0.22;
            p.y -= t * (1.8 + wind * 0.9);
            float2 id = floor(p);
            float2 gv = fract(p) - 0.5;
            float rnd = hash21(id + seed);
            float xOff = (rnd - 0.5) * 0.55;
            float yOff = fract(rnd * 17.3) - 0.5;
            float dx = abs(gv.x - xOff);
            float dy = abs(gv.y - yOff * 0.35);
            float streak = smoothstep(0.085, 0.0, dx) * smoothstep(0.42, 0.02, dy);
            return streak * step(0.42, rnd);
        }

        float snowLayer(float2 uv, float t, float wind, float seed, float grid) {
            float2 p = uv * grid;
            p.x -= t * (0.12 + wind * 0.38);
            p.y -= t * (0.28 + seed * 0.06);
            float2 id = floor(p);
            float2 gv = fract(p) - 0.5;
            float rnd = hash21(id + seed);
            float drift = sin(t * 0.9 + rnd * 6.2831) * 0.22;
            float2 offset = float2((rnd - 0.5) * 0.5 + drift, (fract(rnd * 13.7) - 0.5) * 0.45);
            float d = length(gv - offset);
            float radius = mix(0.08, 0.17, fract(rnd * 31.1));
            float flake = smoothstep(radius, radius * 0.22, d);
            return flake * step(0.54, rnd);
        }

        half4 main(float2 fragCoord) {
            float2 res = max(uResolution, float2(1.0, 1.0));
            float2 uv = fragCoord / res;
            float aspect = res.x / res.y;
            float2 auv = float2(uv.x * aspect, uv.y);
            float2 sampleCoord = (uScale < 0.75)
                ? floor(auv * 140.0) / 140.0
                : auv;

            float3 topCol = float3(uTopColor.rgb);
            float3 botCol = float3(uBottomColor.rgb);
            float3 accentCol = float3(uAccentColor.rgb);
            float3 col = mix(topCol, botCol, pow(max(clamp(uv.y, 0.0, 1.0), 0.0001), 0.92));

            // Subtle KST day-phase tinting (twilight warmth / night depth)
            float isTwilight = 1.0 - step(0.5, abs(uDayPhase - 1.0));
            float isNight = step(1.5, uDayPhase);
            float3 warmTint = (uDark > 0.5) ? float3(0.45, 0.26, 0.22) : float3(0.98, 0.80, 0.62);
            float3 nightTint = (uDark > 0.5) ? float3(0.06, 0.10, 0.20) : float3(0.76, 0.82, 0.92);
            float hDelta = (uv.y - 0.28) * 2.2;
            float horizonBand = exp(-(hDelta * hDelta));
            col = mix(col, warmTint, isTwilight * horizonBand * (uDark > 0.5 ? 0.22 : 0.18));
            col = mix(col, nightTint, isNight * (1.0 - uv.y) * (uDark > 0.5 ? 0.28 : 0.12));

            float windSpeed = 0.015 + uWind * 0.055;
            float2 sunPos = float2(0.83 * aspect, 0.17);
            float sunDist = length(auv - sunPos);

            // 0: CLEAR, 1: PARTLY -> soft atmospheric solar corona and slow scattering rays
            if (uMode < 1.5) {
                float clearWeight = (uMode < 0.5) ? 1.0 : 0.55;
                float breathe = 0.92 + 0.08 * sin(uTime * 0.5);
                float halo = exp(-sunDist * 2.4 / breathe);
                float core = smoothstep(0.24, 0.02, sunDist);
                float2 delta = auv - sunPos;
                float angle = atan(delta.y, delta.x + 0.0001) + uTime * 0.04;
                float rays = 0.5 + 0.5 * cos(angle * 6.0);
                float rayMask = exp(-sunDist * 1.9) * smoothstep(0.01, 0.06, sunDist) * (rays * rays * rays);
                float3 sunGlow = mix(float3(1.0, 0.92, 0.72), warmTint, isTwilight * 0.6);
                sunGlow = mix(sunGlow, accentCol, isNight * 0.55);
                float glowStrength = (uDark > 0.5) ? 0.24 : 0.30;
                col += sunGlow * (halo * glowStrength + core * 0.14 + rayMask * 0.08) * clearWeight;
            }

            // 1: PARTLY, 2: CLOUDY, 3: RAIN, 4: SLEET, 5: SNOW -> soft FBM cloud veil
            if (uMode > 0.5) {
                float cloudDensity = (uMode < 1.5) ? 0.36 : ((uMode < 2.5) ? 0.62 : 0.52);
                float2 cloudUv = sampleCoord * float2(1.4, 2.6) + float2(-uTime * windSpeed, 0.0);
                float n1 = fbm(cloudUv, uScale);
                float n2 = fbm(cloudUv * 1.65 + float2(-uTime * windSpeed * 0.6, 3.4), uScale);
                float cloud = smoothstep(0.36, 0.82, n1 * 0.65 + n2 * 0.35);
                float cloudFade = (1.0 - smoothstep(0.15, 0.95, uv.y)) * cloudDensity;
                float3 cloudCol = (uDark > 0.5)
                    ? mix(col * 1.25 + 0.05, float3(0.45, 0.54, 0.68), 0.35)
                    : mix(float3(1.0, 1.0, 1.0), topCol * 0.92, 0.28);
                col = mix(col, cloudCol, clamp(cloud * cloudFade, 0.0, 0.52));
            }

            // 3: RAIN, 4: SLEET -> multi-layer streaks + atmospheric mist
            if (uMode > 2.5 && uMode < 4.5) {
                float rainGain = (uMode < 3.5) ? 1.0 : 0.55;
                float r = rainLayer(auv, uTime, uWind, 1.1, 18.0) * 0.55;
                r += rainLayer(auv, uTime * 1.28, uWind, 4.7, 27.0) * 0.42;
                if (uScale >= 0.75) {
                    r += rainLayer(auv, uTime * 1.52, uWind, 8.3, 36.0) * 0.28;
                }
                float3 rainTint = (uDark > 0.5) ? float3(0.65, 0.84, 1.0) : mix(accentCol, float3(0.28, 0.52, 0.82), 0.6);
                float mist = smoothstep(0.95, 0.25, uv.y) * 0.08;
                col = mix(col, rainTint, clamp(r * rainGain * (uDark > 0.5 ? 0.34 : 0.24) + mist * 0.3, 0.0, 0.45));
            }

            // 4: SLEET, 5: SNOW -> soft depth-of-field bokeh snowflakes
            if (uMode > 3.5) {
                float snowGain = (uMode > 4.5) ? 1.0 : 0.65;
                float s = snowLayer(auv, uTime, uWind, 2.3, 8.5) * 0.65;
                s += snowLayer(auv, uTime * 0.85, uWind, 7.9, 13.5) * 0.48;
                if (uScale >= 0.75) {
                    s += snowLayer(auv, uTime * 1.15, uWind, 12.4, 18.0) * 0.32;
                }
                float3 snowTint = (uDark > 0.5) ? float3(0.92, 0.96, 1.0) : float3(1.0, 1.0, 1.0);
                col = mix(col, snowTint, clamp(s * snowGain * (uDark > 0.5 ? 0.48 : 0.55), 0.0, 0.65));
            }

            return half4(half3(clamp(col, 0.0, 1.0)), 1.0);
        }
    """.trimIndent()
}
