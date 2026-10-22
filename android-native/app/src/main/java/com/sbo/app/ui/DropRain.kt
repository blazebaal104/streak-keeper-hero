package com.sbo.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * The white drops that rain down the screen after a press (same burst as the website: 14-40 drops,
 * tiny / medium / big, staggered over 0.7 s), but with real physics instead of a tween:
 * each drop accelerates under gravity until air drag caps it at its terminal speed. Big drops are
 * heavy (fast, straight), tiny ones are light (slower, they flutter sideways). A drop stretches a
 * little as it picks up speed, like real water.
 *
 * Everything happens in the draw phase from a single frame clock: no recomposition per frame.
 */
@Stable
class DropRain {
    internal class Drop(
        val x: Float,      // 0..1 of the width
        val delay: Float,  // s
        val w: Float,      // dp
        val vt: Float,     // terminal speed, dp/s
        val drift: Float,  // dp of sideways travel
        val sway: Float,   // dp of flutter amplitude
        val swayF: Float,  // rad/s
        val phase: Float,
        val alpha: Float,
        val tilt: Float,   // deg
    )
    internal class Burst(val drops: List<Drop>) { var start = -1L }
    internal val bursts = mutableStateListOf<Burst>()

    fun burst(seed: Int = Random.nextInt()) {
        val r = Random(seed)
        val count = (14 + r.nextFloat() * 26).toInt()
        bursts += Burst(
            List(count) {
                val k = r.nextFloat()
                val w = when { k > 0.85f -> 20f + r.nextFloat() * 18f; k < 0.35f -> 5f + r.nextFloat() * 5f; else -> 9f + r.nextFloat() * 8f }
                val heavy = ((w - 5f) / 33f).coerceIn(0f, 1f) // 0 = tiny, 1 = biggest
                Drop(
                    x = r.nextFloat(), delay = r.nextFloat() * 0.7f, w = w,
                    vt = 620f + heavy * 900f + r.nextFloat() * 120f,
                    drift = (r.nextFloat() - 0.5f) * 60f,
                    sway = (1f - heavy) * (3f + r.nextFloat() * 6f), swayF = 5f + r.nextFloat() * 5f, phase = r.nextFloat() * 6.28f,
                    alpha = 0.55f + r.nextFloat() * 0.45f, tilt = (r.nextFloat() - 0.5f) * 20f,
                )
            },
        )
    }
}

private const val G = 4200f // dp/s²
private const val V0 = 320f // dp/s: drops are already falling when they enter

@Composable
fun DropRainOverlay(rain: DropRain, modifier: Modifier = Modifier) {
    val now = remember { mutableLongStateOf(0L) }
    val drop = remember { dropPath() }
    val active = rain.bursts.isNotEmpty()
    LaunchedEffect(active) {
        while (rain.bursts.isNotEmpty()) {
            withFrameNanos { t ->
                now.longValue = t
                rain.bursts.forEach { if (it.start < 0) it.start = t }
            }
            rain.bursts.removeAll { it.start >= 0 && (now.longValue - it.start) / 1e9f > 3.2f }
        }
    }
    if (!active) return
    Canvas(modifier.fillMaxSize()) {
        val t0 = now.longValue
        val h = size.height
        val dp = density
        for (b in rain.bursts) {
            if (b.start < 0) continue
            val tb = (t0 - b.start) / 1e9f
            for (d in b.drops) {
                val t = tb - d.delay
                if (t <= 0f) continue
                val tau = d.vt / G
                val e = exp(-t / tau)
                // v(t) = vt - (vt - v0)·e^(-t/τ)  →  y(t) = vt·t - (vt - v0)·τ·(1 - e^(-t/τ))
                val y = (-60f + d.vt * t - (d.vt - V0) * tau * (1f - e)) * dp
                if (y > h + 60f * dp) continue
                val v = d.vt - (d.vt - V0) * e
                val x = d.x * size.width + (d.drift * (1f - e) + d.sway * sin(d.swayF * t + d.phase)) * dp
                val fadeIn = (t / 0.12f).coerceAtMost(1f)
                val fadeOut = (1f - ((y / h) - 0.82f) / 0.25f).coerceIn(0f, 1f)
                val a = d.alpha * fadeIn * fadeOut
                if (a <= 0.01f) continue
                val grow = 0.8f + 0.2f * (t / 0.35f).coerceAtMost(1f)
                val stretch = 1f + 0.22f * (v / 1500f)
                drawDrop(drop, Offset(x, y), d.w * dp * grow, stretch, d.tilt * e, a)
            }
        }
    }
}

private fun DrawScope.drawDrop(path: Path, at: Offset, w: Float, stretch: Float, tilt: Float, a: Float) {
    // soft glow behind (the web's drop-shadow-glow)
    drawCircle(
        Brush.radialGradient(0f to Color.White.copy(alpha = 0.15f * a), 1f to Color.Transparent, center = Offset(at.x, at.y + w * 0.95f), radius = w * 0.6f),
        radius = w * 0.6f, center = Offset(at.x, at.y + w * 0.95f),
    )
    translate(at.x - w / 2f, at.y) {
        rotate(tilt, pivot = Offset(w / 2f, w * 0.7f)) {
            scale(w / 20f, w / 20f * stretch, pivot = Offset.Zero) {
                drawPath(path, Color.White.copy(alpha = a))
            }
        }
    }
}

/** The website's drop: M10 1 C 10 8, 18 14, 18 20 A 8 8 0 0 1 2 20 C 2 14, 10 8, 10 1 Z (20 x 28). */
private fun dropPath() = Path().apply {
    moveTo(10f, 1f)
    cubicTo(10f, 8f, 18f, 14f, 18f, 20f)
    arcTo(Rect(2f, 12f, 18f, 28f), 0f, 180f, false)
    cubicTo(2f, 14f, 10f, 8f, 10f, 1f)
    close()
}
