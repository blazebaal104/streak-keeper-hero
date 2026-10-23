package com.sbo.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val OUT = CubicBezierEasing(0.16f, 1f, 0.3f, 1f) // easeOutExpo-ish

/**
 * The tap's shockwave: two thin rings of light leave the edge of the disc and fade as they grow,
 * with a soft bloom right at the rim. Colour follows the button's mood (fire / ice / neutral).
 */
@Composable
fun ShockWave(trigger: Int, color: Color, disc: Dp, core: Color = Color(0xFFFFF4E0)) {
    if (trigger == 0) return
    val a = remember(trigger) { Animatable(0f) }
    val b = remember(trigger) { Animatable(0f) }
    LaunchedEffect(trigger) {
        val ease = CubicBezierEasing(0.25f, 0.8f, 0.35f, 1f)
        launch { a.animateTo(1f, tween(560, easing = ease)) }
        delay(100)
        b.animateTo(1f, tween(620, easing = ease))
    }
    Canvas(Modifier.requiredSize(disc * 2.3f)) {
        val r0 = disc.toPx() / 2f
        fun ring(t: Float, strength: Float) {
            if (t <= 0f || t >= 1f) return
            val r = r0 * (1f + 0.62f * t)
            val fade = (1f - t)
            val thin = 1f - t * 0.67f
            // glow (stacked soft strokes) around a hot, thin core: reads as light, not paint
            listOf(18f to 0.035f, 13f to 0.05f, 8f to 0.08f, 4.5f to 0.16f).forEach { (sw, al) ->
                drawCircle(color.copy(alpha = al * fade * strength), r, style = Stroke(sw.dp.toPx() * thin), blendMode = BlendMode.Screen)
            }
            drawCircle(core.copy(alpha = 0.9f * fade * strength), r, style = Stroke(3.dp.toPx() * thin), blendMode = BlendMode.Screen)
        }
        // bloom on the rim right after the tap
        val bloom = (1f - a.value * 2.2f).coerceIn(0f, 1f)
        if (bloom > 0f) drawCircle(
            Brush.radialGradient(0f to Color.Transparent, 0.78f to Color.Transparent, 0.88f to color.copy(alpha = 0.45f * bloom), 1f to Color.Transparent, center = center, radius = r0 * 1.35f),
            radius = r0 * 1.35f, blendMode = BlendMode.Screen,
        )
        ring(a.value, 1f)
        // second wave: glow only, so a single crisp ring reads at a time
        val tb = b.value
        if (tb > 0f && tb < 1f) {
            val r = r0 * (1f + 0.62f * tb)
            drawCircle(color.copy(alpha = 0.06f * (1f - tb)), r, style = Stroke(20.dp.toPx() * (1f - tb * 0.5f)), blendMode = BlendMode.Screen)
        }
    }
}

/**
 * Fire-streak immersion: the whole screen edge flares in fire colours with a heartbeat (two beats),
 * strongest from the bottom where the flames are, with a glowing rounded border that follows the display corners.
 */
@Composable
fun FirePulse(trigger: Int, duck: Animatable<Float, androidx.compose.animation.core.AnimationVector1D>? = null) {
    if (trigger == 0) return
    val k = remember(trigger) { Animatable(0f) }
    val view = androidx.compose.ui.platform.LocalView.current
    LaunchedEffect(trigger) {
        if (duck != null) launch { duck.animateTo(1f, tween(100)); delay(650); duck.animateTo(0f, tween(500)) }
        launch {
            k.animateTo(0f, keyframes {
                durationMillis = 1250
                // easing belongs to the segment that starts at each keyframe
                0f at 0 using CubicBezierEasing(0.2f, 0.8f, 0.3f, 1f)
                1f at 120 using CubicBezierEasing(0.4f, 0f, 0.6f, 1f)
                0.22f at 270 using CubicBezierEasing(0.2f, 0.8f, 0.3f, 1f)
                0.82f at 420 using CubicBezierEasing(0.33f, 0f, 0.2f, 1f)
                0f at 1250
            })
        }
        delay(300)
        view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK) // second heartbeat
    }
    Canvas(Modifier.fillMaxSize()) {
        val v = k.value
        if (v <= 0.002f) return@Canvas
        val w = size.width; val h = size.height
        val core = Color(0xFFFF6A1A)
        val hot = Color(0xFFFFE3A0)
        val side = w * 0.12f
        drawRect(Brush.horizontalGradient(0f to core.copy(alpha = 0.45f * v), 1f to Color.Transparent, startX = 0f, endX = side), blendMode = BlendMode.Screen)
        drawRect(Brush.horizontalGradient(0f to Color.Transparent, 1f to core.copy(alpha = 0.45f * v), startX = w - side, endX = w), blendMode = BlendMode.Screen)
        drawRect(Brush.verticalGradient(0f to Color.Transparent, 1f to core.copy(alpha = 0.7f * v), startY = h * 0.86f, endY = h), blendMode = BlendMode.Screen)
        drawRect(Brush.verticalGradient(0f to core.copy(alpha = 0.32f * v), 1f to Color.Transparent, startY = 0f, endY = h * 0.05f), blendMode = BlendMode.Screen)
        // glowing border hugging the screen's rounded corners
        val cr = CornerRadius(40.dp.toPx())
        listOf(22.dp to 0.10f, 12.dp to 0.18f, 6.dp to 0.32f, 2.5.dp to 0.8f).forEach { (sw, al) ->
            val px = sw.toPx()
            drawRoundRect(hot.copy(alpha = al * v), topLeft = Offset(px / 2, px / 2), size = Size(w - px, h - px), cornerRadius = cr, style = Stroke(px), blendMode = BlendMode.Screen)
        }
            }
}
