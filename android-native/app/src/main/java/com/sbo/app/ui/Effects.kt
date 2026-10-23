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
fun ShockWave(trigger: Int, color: Color, disc: Dp) {
    if (trigger == 0) return
    val a = remember(trigger) { Animatable(0f) }
    val b = remember(trigger) { Animatable(0f) }
    LaunchedEffect(trigger) {
        launch { a.animateTo(1f, tween(760, easing = OUT)) }
        delay(110)
        b.animateTo(1f, tween(820, easing = OUT))
    }
    Canvas(Modifier.requiredSize(disc * 2.3f)) {
        val r0 = disc.toPx() / 2f
        fun ring(t: Float, strength: Float) {
            if (t <= 0f || t >= 1f) return
            val r = r0 * (0.98f + 0.95f * t)
            val fade = (1f - t)
            val w = (7.dp.toPx() * (1f - t * 0.85f))
            // soft halo + crisp core
            drawCircle(color.copy(alpha = 0.18f * fade * strength), r, style = Stroke(w * 3.2f), blendMode = BlendMode.Screen)
            drawCircle(color.copy(alpha = 0.55f * fade * fade * strength), r, style = Stroke(w), blendMode = BlendMode.Screen)
        }
        // bloom on the rim right after the tap
        val bloom = (1f - a.value * 2.2f).coerceIn(0f, 1f)
        if (bloom > 0f) drawCircle(
            Brush.radialGradient(0f to Color.Transparent, 0.78f to Color.Transparent, 0.88f to color.copy(alpha = 0.45f * bloom), 1f to Color.Transparent, center = center, radius = r0 * 1.35f),
            radius = r0 * 1.35f, blendMode = BlendMode.Screen,
        )
        ring(a.value, 1f)
        ring(b.value, 0.6f)
    }
}

/**
 * Fire-streak immersion: the whole screen edge flares in fire colours with a heartbeat (two beats),
 * strongest from the bottom where the flames are, with a glowing rounded border that follows the display corners.
 */
@Composable
fun FirePulse(trigger: Int) {
    if (trigger == 0) return
    val k = remember(trigger) { Animatable(0f) }
    val view = androidx.compose.ui.platform.LocalView.current
    LaunchedEffect(trigger) {
        launch {
            k.animateTo(0f, keyframes {
                durationMillis = 1250
                // easing belongs to the segment that starts at each keyframe
                0f at 0 using CubicBezierEasing(0.2f, 0.8f, 0.3f, 1f)
                1f at 120 using CubicBezierEasing(0.4f, 0f, 0.6f, 1f)
                0.38f at 300 using CubicBezierEasing(0.2f, 0.8f, 0.3f, 1f)
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
        val hot = Color(0xFFFFB347)
        val side = w * 0.2f
        drawRect(Brush.horizontalGradient(0f to core.copy(alpha = 0.55f * v), 1f to Color.Transparent, startX = 0f, endX = side), blendMode = BlendMode.Screen)
        drawRect(Brush.horizontalGradient(0f to Color.Transparent, 1f to core.copy(alpha = 0.55f * v), startX = w - side, endX = w), blendMode = BlendMode.Screen)
        drawRect(Brush.verticalGradient(0f to Color.Transparent, 1f to core.copy(alpha = 0.7f * v), startY = h * 0.68f, endY = h), blendMode = BlendMode.Screen)
        drawRect(Brush.verticalGradient(0f to core.copy(alpha = 0.32f * v), 1f to Color.Transparent, startY = 0f, endY = h * 0.1f), blendMode = BlendMode.Screen)
        // glowing border hugging the screen's rounded corners
        val cr = CornerRadius(40.dp.toPx())
        listOf(16.dp to 0.12f, 7.dp to 0.28f, 2.5.dp to 0.75f).forEach { (sw, al) ->
            val px = sw.toPx()
            drawRoundRect(hot.copy(alpha = al * v), topLeft = Offset(px / 2, px / 2), size = Size(w - px, h - px), cornerRadius = cr, style = Stroke(px), blendMode = BlendMode.Screen)
        }
        drawRect(core.copy(alpha = 0.05f * v), blendMode = BlendMode.Screen)
    }
}
