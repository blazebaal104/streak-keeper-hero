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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * The white drops that rain down after a press (same burst as the website: 14-40 drops, staggered over 0.7 s),
 * rendered as real water instead of flat shapes:
 *  - physics: gravity + air drag (terminal speed by size), light drops flutter, heavy ones fall straight;
 *  - depth: far drops are smaller, slower and dimmer (parallax), near ones big and crisp;
 *  - look: a transparent body that brightens towards the rim (Fresnel), a sharp specular highlight,
 *    a caustic glint at the bottom and a faint motion streak when fast;
 *  - life: a freshly released drop wobbles (surface tension) before settling into its falling shape,
 *    and on reaching the bottom of the screen it splashes: a flat ripple plus a crown of tiny droplets
 *    thrown up that fall back under the same gravity.
 * Everything is computed analytically from one frame clock in the draw phase: no recomposition per frame.
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
        val depth: Float,  // 0 far … 1 near
        val seed: Int,
    ) {
        var hitH = -1f    // floor (px) the hit time was solved for
        var hitT = -1f    // s after the drop's own start
    }
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
                val depth = (heavy * 0.75f + r.nextFloat() * 0.25f).coerceIn(0f, 1f)
                Drop(
                    x = r.nextFloat(), delay = r.nextFloat() * 0.7f, w = w,
                    vt = (620f + heavy * 900f + r.nextFloat() * 120f) * (0.78f + 0.22f * depth),
                    drift = (r.nextFloat() - 0.5f) * 60f,
                    sway = (1f - heavy) * (3f + r.nextFloat() * 6f), swayF = 5f + r.nextFloat() * 5f, phase = r.nextFloat() * 6.28f,
                    alpha = (0.55f + r.nextFloat() * 0.45f) * (0.55f + 0.45f * depth), tilt = (r.nextFloat() - 0.5f) * 20f,
                    depth = depth, seed = r.nextInt(),
                )
            },
        )
    }
}

private const val G = 4200f   // dp/s²
private const val V0 = 320f   // dp/s: drops are already falling when they enter
private const val SPLASH_LIFE = 0.9f

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
            rain.bursts.removeAll { it.start >= 0 && (now.longValue - it.start) / 1e9f > 4.2f }
        }
    }
    if (!active) return
    Canvas(modifier.fillMaxSize()) {
        val t0 = now.longValue
        val h = size.height
        val dp = density
        val floor = h - 14f * dp
        // far drops first so near ones overlap them
        for (b in rain.bursts) {
            if (b.start < 0) continue
            val tb = (t0 - b.start) / 1e9f
            for (d in b.drops.sortedBy { it.depth }) {
                val t = tb - d.delay
                if (t <= 0f) continue
                val tau = d.vt / G
                if (d.hitH != floor) { d.hitH = floor; d.hitT = solveHit(d, tau, floor / dp) }
                if (t >= d.hitT) {
                    splash(d, t - d.hitT, xAt(d, d.hitT, size.width, dp), floor, dp)
                    continue
                }
                val e = exp(-t / tau)
                val y = yAt(d, t, tau) * dp
                val v = d.vt - (d.vt - V0) * e
                val x = xAt(d, t, size.width, dp)
                val fadeIn = (t / 0.12f).coerceAtMost(1f)
                val a = d.alpha * fadeIn
                if (a <= 0.01f) continue
                val grow = 0.8f + 0.2f * (t / 0.35f).coerceAtMost(1f)
                val stretch = 1f + 0.24f * (v / 1500f)
                // surface-tension wobble right after release, damped away in ~0.5 s
                val wob = 0.11f * exp(-t / 0.42f) * sin(19f * t + d.phase)
                drawWaterDrop(drop, Offset(x, y), d.w * dp * grow, stretch, 1f + wob, d.tilt * e, a, v, d.depth)
            }
        }
    }
}

private fun yAt(d: DropRain.Drop, t: Float, tau: Float): Float =
    // v(t) = vt - (vt - v0)·e^(-t/τ)  →  y(t) = vt·t - (vt - v0)·τ·(1 - e^(-t/τ))
    -60f + d.vt * t - (d.vt - V0) * tau * (1f - exp(-t / tau))

private fun xAt(d: DropRain.Drop, t: Float, width: Float, dp: Float): Float {
    val e = exp(-t / (d.vt / G))
    return d.x * width + (d.drift * (1f - e) + d.sway * sin(d.swayF * t + d.phase)) * dp
}

/** Time (s) at which the bottom of the drop reaches the floor (dp). */
private fun solveHit(d: DropRain.Drop, tau: Float, floorDp: Float): Float {
    val target = floorDp - d.w * 1.35f
    var lo = 0f; var hi = 6f
    repeat(28) { val m = (lo + hi) / 2f; if (yAt(d, m, tau) < target) lo = m else hi = m }
    return hi
}

private fun DrawScope.drawWaterDrop(path: Path, at: Offset, w: Float, stretch: Float, wobble: Float, tilt: Float, a: Float, v: Float, depth: Float) {
    val unit = w / 20f
    // motion streak above fast drops
    if (v > 950f) {
        val len = ((v - 950f) / 700f).coerceIn(0f, 1f) * w * 1.4f * 2f
        val sw = w * 0.2f
        // tapered streak: wide at the drop, a hair at the top
        val streak = Path().apply {
            moveTo(at.x, at.y - len)
            lineTo(at.x + sw / 2f, at.y + w * 0.45f)
            lineTo(at.x - sw / 2f, at.y + w * 0.45f)
            close()
        }
        drawPath(streak, Brush.verticalGradient(0f to Color.Transparent, 1f to Color.White.copy(alpha = 0.22f * a), startY = at.y - len, endY = at.y + w * 0.45f))
    }
    // soft light around the bulb (the web's drop-shadow-glow), weaker for far drops
    drawCircle(
        Brush.radialGradient(0f to Color.White.copy(alpha = 0.12f * a * (0.5f + 0.5f * depth)), 1f to Color.Transparent, center = Offset(at.x, at.y + w * 0.95f), radius = w * 0.75f),
        radius = w * 0.75f, center = Offset(at.x, at.y + w * 0.95f),
    )
    translate(at.x - w / 2f, at.y) {
        rotate(tilt, pivot = Offset(w / 2f, w * 0.7f)) {
            // wobble keeps the volume: wider ⇄ taller
            scale(unit * wobble, unit * stretch / wobble, pivot = Offset.Zero) {
                val bulb = Offset(10f, 20f)
                // Fresnel body: nearly clear in the middle, bright towards the rim
                drawPath(
                    path,
                    Brush.radialGradient(
                        // centre pushed up-right: the rim lights up mostly at the bottom-left, fades towards the tip
                        0f to Color.White.copy(alpha = 0.07f * a), 0.45f to Color.White.copy(alpha = 0.09f * a),
                        0.62f to Color.White.copy(alpha = 0.22f * a), 0.74f to Color.White.copy(alpha = 0.5f * a),
                        1f to Color.White.copy(alpha = 0.08f * a),
                        center = Offset(12.6f, 16.8f), radius = 13f,
                    ),
                )
                // thin rim, brighter at the bottom where light gathers
                drawPath(
                    path,
                    Brush.verticalGradient(0f to Color.White.copy(alpha = 0.04f * a), 0.5f to Color.White.copy(alpha = 0.18f * a), 1f to Color.White.copy(alpha = 0.45f * a), startY = 1f, endY = 28f),
                    style = Stroke(0.3f),
                )
                // specular highlight (light from top-left) + caustic glint
                rotate(-28f, pivot = Offset(7f, 17.5f)) {
                    drawOval(Color.White.copy(alpha = 0.95f * a), topLeft = Offset(5.4f, 14.8f), size = Size(3.0f, 5.4f))
                }
                drawArc(
                    Brush.radialGradient(0f to Color.White.copy(alpha = 0.6f * a), 1f to Color.White.copy(alpha = 0f), center = Offset(13.2f, 25f), radius = 4.5f),
                    startAngle = 25f, sweepAngle = 60f, useCenter = false,
                    topLeft = Offset(bulb.x - 6.4f, bulb.y - 6.4f + 0.9f), size = Size(12.8f, 12.8f), style = Stroke(1.4f, cap = androidx.compose.ui.graphics.StrokeCap.Round),
                )
            }
        }
    }
}

/** Splash at the floor: a flat expanding ripple plus a crown of droplets thrown up that fall back. */
private fun DrawScope.splash(d: DropRain.Drop, t: Float, x: Float, floor: Float, dp: Float) {
    if (t > SPLASH_LIFE) return
    val a0 = d.alpha
    val big = ((d.w - 5f) / 33f).coerceIn(0f, 1f)
    // ripple
    val u = (t / 0.35f).coerceIn(0f, 1f)
    if (u < 1f) {
        val ease = 1f - (1f - u) * (1f - u) * (1f - u)
        val rx = d.w * dp * (0.3f + 1.0f * ease)
        val ry = rx * 0.18f
        drawOval(
            Color.White.copy(alpha = 0.08f * a0 * (1f - u)), topLeft = Offset(x - rx, floor - ry), size = Size(rx * 2f, ry * 2f),
            style = Stroke(5f * dp),
        )
        drawOval(
            Color.White.copy(alpha = 0.3f * a0 * (1f - u) * (1f - u)), topLeft = Offset(x - rx, floor - ry), size = Size(rx * 2f, ry * 2f),
            style = Stroke((1.1f * (1f - u) + 0.4f) * dp),
        )
        // flash of the impact
        val f = (1f - t / 0.12f).coerceIn(0f, 1f)
        if (f > 0f) drawOval(Color.White.copy(alpha = 0.45f * a0 * f), topLeft = Offset(x - d.w * dp * 0.55f, floor - d.w * dp * 0.12f), size = Size(d.w * dp * 1.1f, d.w * dp * 0.24f))
    }
    // crown droplets (only drops big enough to throw some)
    if (d.w < 8f) return
    val r = Random(d.seed)
    val n = 3 + (d.w / 7f).toInt()
    repeat(n) {
        val ang = (-62f + r.nextFloat() * 124f) * (Math.PI.toFloat() / 180f) // from vertical
        val sp = (240f + r.nextFloat() * 300f) * (0.6f + 0.6f * big)
        val vx = sin(ang) * sp; val vy = -cos(ang) * sp
        val px = x + vx * t * dp
        val py = floor + (vy * t + 0.5f * G * 0.9f * t * t) * dp
        if (py > floor + 2f * dp) return@repeat
        val size = (1.5f + r.nextFloat() * 1.5f) * dp
        val fade = (1f - t / SPLASH_LIFE).coerceIn(0f, 1f)
        val al = a0 * 0.9f * fade
        // tiny drops stretch along their motion
        val vyNow = vy + G * 0.9f * t
        val speed = sqrt(vx * vx + vyNow * vyNow)
        val st = 1f + (speed / 900f).coerceAtMost(0.8f)
        val deg = Math.toDegrees(kotlin.math.atan2(vyNow.toDouble(), vx.toDouble())).toFloat() - 90f
        rotate(deg, pivot = Offset(px, py)) {
            drawOval(Color.White.copy(alpha = al * 0.12f), topLeft = Offset(px - size * 1.4f, py - size * 1.4f * st), size = Size(size * 2.8f, size * 2.8f * st))
            drawOval(Color.White.copy(alpha = al), topLeft = Offset(px - size / 2f, py - size / 2f * st), size = Size(size, size * st))
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
