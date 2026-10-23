package com.sbo.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sbo.app.R
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

/*
 * Halloween night (port of the web components/sbo/halloween.tsx): moon, cobwebs, bats crossing the sky,
 * a witch on her broom passing the moon now and then, a graveyard with a ruined church along the bottom
 * and drifting fog. One frame clock, everything drawn in the draw phase.
 */

private val INK = Color(0xFF120A1C)
private const val BAT = "M0 10C4 4 9 4 12 8C13 5 15 4 16 6L17 3L18 6C19 4 21 5 22 8C25 4 30 4 34 10C30 9 28 11 27 14C25 11 22 12 21 14C20 12 18 12 17 15C16 12 14 12 13 14C12 12 9 11 7 14C6 11 4 9 0 10Z"

private fun svg(d: String): Path = PathParser().parsePathString(d).toPath()

private class Bat(val top: Float, val size: Float, val dur: Float, val delay: Float, val dir: Int, val bob: Float, val phase: Float)

/** Smooth ease-in-out, like framer's "easeInOut" (cubic). */
private fun easeInOut(t: Float): Float = if (t < 0.5f) 4f * t * t * t else 1f - (-2f * t + 2f).let { it * it * it } / 2f

/** Keyframes evenly spaced over 0..1 with ease-in-out between them (framer default). */
private fun keys(t: Float, vararg v: Float): Float {
    val n = v.size - 1
    val x = (t.coerceIn(0f, 1f) * n)
    val i = x.toInt().coerceAtMost(n - 1)
    val f = easeInOut(x - i)
    return v[i] + (v[i + 1] - v[i]) * f
}

@Composable
fun HalloweenNight(modifier: Modifier = Modifier) {
    val clock = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (true) withFrameNanos { clock.floatValue = (it - start) / 1e9f }
    }
    val bats = remember {
        val r = Random(31)
        List(7) { i -> Bat(0.06f + r.nextFloat() * 0.45f, 18f + r.nextFloat() * 26f, 14f + r.nextFloat() * 14f, i * 2.6f + r.nextFloat() * 2f, if (i % 2 == 1) 1 else -1, 10f + r.nextFloat() * 25f, r.nextFloat()) }
    }
    val bat = remember { svg(BAT) }
    val scene = remember { Graveyard() }
    val witch = remember { WitchShapes() }
    Canvas(modifier.fillMaxSize()) {
        val t = clock.floatValue
        val w = size.width; val h = size.height; val dp = density
        // moon (web: -right-10 top-10 h-44 w-44, opacity .85, soft glow)
        val mr = 88f * dp
        val mc = Offset(w + 40f * dp - mr, 40f * dp + mr)
        drawCircle(Brush.radialGradient(0f to Color(0x38F5D58A), 0.55f to Color(0x14F5D58A), 1f to Color.Transparent, center = mc, radius = mr + 120f * dp), mr + 120f * dp, mc)
        drawCircle(
            Brush.radialGradient(0f to Color(0xFFFFF7D6), 0.45f to Color(0xFFF5D58A), 0.7f to Color(0xFFD9A441), 1f to Color(0xFFD9A441), center = Offset(mc.x - mr * 0.2f, mc.y - mr * 0.2f), radius = mr * 1.25f),
            mr, mc, alpha = 0.85f,
        )
        // cobwebs in the top corners
        cobweb(Offset(-8f * dp, -8f * dp), 160f * dp, mirror = false)
        cobweb(Offset(w + 8f * dp, -8f * dp), 128f * dp, mirror = true)
        // witch crossing in front of the moon: 11 s flight, 16 s pause
        run {
            val cyc = t % 27f
            if (cyc < 11f) {
                val u = cyc / 11f
                val x = -0.2f * w + easeInOut(u) * 1.4f * w
                val y = 0.09f * h + keys(u, 40f, -10f, 25f, -5f) * dp
                val sw = 96f * dp
                withTransform({ translate(x, y); scale(sw / 120f, sw / 120f, pivot = Offset.Zero) }) { witch.draw(this, t) }
            }
        }
        // bats
        for (b in bats) {
            val tt = t - b.delay
            if (tt < 0f) continue
            val u = (tt % b.dur) / b.dur
            val bw = b.size * dp
            val x0 = if (b.dir > 0) -0.1f * w else 1.1f * w
            val x = x0 + b.dir * u * 1.25f * w
            val y = b.top * h + keys(((tt + b.phase * 3.2f) % 3.2f) / 3.2f, 0f, -b.bob, b.bob, 0f) * dp
            val flap = keys(((tt + b.phase) % 0.28f) / 0.28f, 1f, 0.55f, 1f)
            val k = bw / 34f
            withTransform({ translate(x, y); scale(k, k * flap, pivot = Offset(0f, 8f * k)) }) { drawPath(bat, INK) }
        }
        // graveyard and abandoned church along the bottom (viewBox 1000 x 220, xMidYMax slice, 192 dp tall)
        val gh = 192f * dp
        val gs = max(w / 1000f, gh / 220f)
        withTransform({ translate((w - 1000f * gs) / 2f, h - 220f * gs); scale(gs, gs, pivot = Offset.Zero) }) { scene.draw(this, t) }
        // low fog drifting
        val fx = keys((t % 18f) / 18f, -0.04f, 0.04f, -0.04f) * w * 1.4f
        val fh = 224f * dp
        fog(Offset(-0.2f * w + fx, h - fh), Size(1.4f * w, fh))
    }
}

private fun DrawScope.fog(at: Offset, s: Size) {
    // two radial ellipses (60% x 100%) anchored at the bottom, at 30% and 75%
    listOf(0.3f to Color(0x33B49AE6), 0.75f to Color(0x24A0B4C8)).forEach { (cx, c) ->
        val rx = s.width * 0.6f; val ry = s.height
        val center = Offset(at.x + s.width * cx, at.y + s.height)
        withTransform({ scale(1f, ry / rx, pivot = center) }) {
            drawCircle(Brush.radialGradient(0f to c, 0.7f to Color.Transparent, center = center, radius = rx), rx, center)
        }
    }
}

private fun DrawScope.cobweb(corner: Offset, side: Float, mirror: Boolean) {
    val k = side / 100f
    val col = Color(0x26E6DCFF)
    val sw = 0.6f * k
    val rays = listOf(0, 15, 30, 45, 60, 75, 90)
    fun p(r: Float, deg: Float): Offset {
        val a = deg * PI.toFloat() / 180f
        val x = r * cos(a) * k; val y = r * sin(a) * k
        return Offset(corner.x + if (mirror) -x else x, corner.y + y)
    }
    rays.forEach { a -> drawLine(col, corner, p(100f, a.toFloat()), sw) }
    listOf(22f, 40f, 58f, 76f).forEach { r ->
        val path = Path()
        rays.dropLast(1).forEachIndexed { i, a ->
            val b = rays[i + 1]
            val s0 = p(r, a.toFloat()); val e = p(r, b.toFloat()); val q = p(r * 0.86f, (a + b) / 2f)
            if (i == 0) path.moveTo(s0.x, s0.y)
            path.quadraticTo(q.x, q.y, e.x, e.y)
        }
        drawPath(path, col, style = Stroke(sw))
    }
}

private class WitchShapes {
    val broomHead = svg("M88 31 L112 22 L116 30 L114 38 L90 34 Z")
    val bristles = svg("M96 31 L118 26 M96 33 L119 33 M95 35 L116 39")
    val body = svg("M46 36 Q52 18 60 18 Q66 22 64 36 Z")
    val legs = svg("M56 35 L48 46 L52 47 L60 36 Z")
    val brim = svg("M52 11 L70 11 L72 13 L50 13 Z")
    val hat = svg("M56 11 L64 11 L58 -2 Z")
    val nose = svg("M64 14 L69 16 L64 16 Z")
    val hair = svg("M56 15 Q50 20 52 26 Q54 20 58 17 Z")
    private val capeA = floatArrayOf(30f, 26f, 22f, 44f, 36f, 38f)
    private val capeB = floatArrayOf(28f, 22f, 16f, 36f, 34f, 36f)

    fun draw(d: DrawScope, t: Float) = with(d) {
        drawLine(INK, Offset(8f, 44f), Offset(92f, 30f), 2.6f, StrokeCap.Round)
        drawPath(broomHead, INK)
        drawPath(bristles, INK, style = Stroke(1.2f))
        // cape flapping behind (0.7 s)
        val f = keys((t % 0.7f) / 0.7f, 0f, 1f, 0f)
        val c = FloatArray(6) { capeA[it] + (capeB[it] - capeA[it]) * f }
        drawPath(Path().apply { moveTo(48f, 20f); quadraticTo(c[0], c[1], c[2], c[3]); quadraticTo(c[4], c[5], 50f, 36f); close() }, INK)
        drawPath(body, INK); drawPath(legs, INK)
        drawCircle(INK, 5f, Offset(60f, 14f))
        drawPath(brim, INK); drawPath(hat, INK); drawPath(nose, INK); drawPath(hair, INK)
    }
}

private class Graveyard {
    val farHill = svg("M0 150 Q160 110 330 140 T680 128 T1000 140 L1000 220 L0 220 Z")
    val nearHill = svg("M0 180 Q120 156 260 172 T560 168 T820 174 T1000 166 L1000 220 L0 220 Z")
    val church = listOf(svg("M0 40 L90 40 L90 110 L0 110 Z"), svg("M-6 42 L45 6 L96 42 Z"), svg("M64 -6 L88 -6 L88 42 L64 42 Z"), svg("M60 -4 L76 -34 L92 -4 Z"), svg("M74 -50 L78 -50 L78 -32 L74 -32 Z"), svg("M68 -44 L84 -44 L84 -40 L68 -40 Z"))
    val roofGap = svg("M18 30 L30 22 L26 34 Z")
    val win1 = svg("M14 70 Q14 58 22 58 Q30 58 30 70 L30 88 L14 88 Z")
    val win2 = svg("M58 70 Q58 58 66 58 Q74 58 74 70 L74 88 L58 88 Z")
    val door = svg("M36 110 L36 84 Q45 74 54 84 L54 110 Z")
    val trunk = svg("M800 160 Q806 120 798 92")
    val branches = svg("M800 118 Q830 100 850 78 M846 82 Q856 76 866 78 M802 104 Q772 90 760 66 M764 72 Q752 66 744 70 M799 94 Q802 74 818 60")
    data class Stone(val x: Float, val w: Float, val h: Float)
    val stones = listOf(Stone(40f, 22f, 30f), Stone(96f, 18f, 24f), Stone(330f, 20f, 28f), Stone(372f, 16f, 20f), Stone(600f, 22f, 30f), Stone(650f, 18f, 22f), Stone(860f, 24f, 32f), Stone(905f, 16f, 20f))
    val stonePaths = stones.map { s -> svg("M${s.x} 190 L${s.x} ${190 - s.h + s.w / 2} Q${s.x} ${190 - s.h} ${s.x + s.w / 2} ${190 - s.h} Q${s.x + s.w} ${190 - s.h} ${s.x + s.w} ${190 - s.h + s.w / 2} L${s.x + s.w} 190 Z") }
    val crosses = listOf(140f to 34f, 410f to 30f, 700f to 36f, 950f to 30f)
    val spikes = List(13) { i -> val x = 192f + i * 9.5f; svg("M$x 196 L$x 168 L${x - 1.5f} 168 L${x + 1.5f} 162 L${x + 4.5f} 168 L${x + 3f} 168 L${x + 3f} 196 Z") }
    val pumpkinFace = svg("M-7 -11 L-3 -11 L-5 -15 Z M3 -11 L7 -11 L5 -15 Z M-7 -5 L7 -5 L4 -2 L0 -4 L-4 -2 Z")
    val glow = Brush.radialGradient(0f to Color(0xFFFFD27A), 1f to Color(0xFFF97316), center = Offset(22f, 73f), radius = 16f)

    fun draw(d: DrawScope, t: Float) = with(d) {
        drawPath(farHill, Color(0xFF1D1230), alpha = 0.9f)
        translate(470f, 58f) {
            church.forEach { drawPath(it, Color(0xFF140B22)) }
            drawPath(roofGap, Color(0xFF1D1230))
            val o = keys((t % 4f) / 4f, 0.55f, 1f, 0.7f, 0.95f, 0.55f)
            drawPath(win1, Brush.radialGradient(0f to Color(0xFFFFD27A), 1f to Color(0xFFF97316), center = Offset(22f, 73f), radius = 16f), alpha = o)
            drawPath(win2, Brush.radialGradient(0f to Color(0xFFFFD27A), 1f to Color(0xFFF97316), center = Offset(66f, 73f), radius = 16f), alpha = o * 0.8f)
            drawCircle(Brush.radialGradient(0f to Color(0xFFFFD27A), 1f to Color(0xFFF97316), center = Offset(76f, 12f), radius = 5f), 5f, Offset(76f, 12f), alpha = o)
            // warm light spilling out of the windows
            drawCircle(Brush.radialGradient(0f to Color(0xFFF97316).copy(alpha = 0.18f * o), 1f to Color.Transparent, center = Offset(44f, 74f), radius = 60f), 60f, Offset(44f, 74f))
            drawPath(door, Color(0xFF0B0613))
        }
        drawPath(trunk, Color(0xFF0C0716), style = Stroke(9f, cap = StrokeCap.Round))
        drawPath(branches, Color(0xFF0C0716), style = Stroke(4f, cap = StrokeCap.Round))
        drawPath(nearHill, Brush.verticalGradient(0f to Color(0xFF1A0F29), 1f to Color(0xFF07040C), startY = 156f, endY = 220f))
        stones.forEachIndexed { i, s -> rotate((i % 3) * 4f - 4f, pivot = Offset(s.x + s.w / 2f, 190f)) { drawPath(stonePaths[i], INK) } }
        crosses.forEachIndexed { i, (x, ch) ->
            rotate(if (i % 2 == 1) 6f else -5f, pivot = Offset(x, 190f)) {
                drawRect(INK, Offset(x - 2.5f, 190f - ch), Size(5f, ch))
                drawRect(INK, Offset(x - 10f, 190f - ch + 8f), Size(20f, 5f))
            }
        }
        val iron = Color(0xFF0E0818)
        drawRect(iron, Offset(190f, 176f), Size(120f, 3f)); drawRect(iron, Offset(190f, 190f), Size(120f, 3f))
        spikes.forEach { drawPath(it, iron) }
        listOf(Triple(70f, 1f, 1.6f), Triple(640f, 0.8f, 2.0f), Triple(930f, 0.9f, 2.4f)).forEach { (x, s, dur) ->
            val o = keys((t % dur) / dur, 0.8f, 1f, 0.85f)
            translate(x, 196f) {
                scale(s, s, pivot = Offset.Zero) {
                    drawCircle(Brush.radialGradient(0f to Color(0xFFF97316).copy(alpha = 0.35f * o), 1f to Color.Transparent, center = Offset(0f, -8f), radius = 26f), 26f, Offset(0f, -8f))
                    drawOval(Color(0xFFEA580C), Offset(-13f, -18f), Size(26f, 20f), alpha = o)
                    drawRect(Color(0xFF3F6212), Offset(-1.5f, -21f), Size(3f, 5f), alpha = o)
                    drawPath(pumpkinFace, Color(0xFFFFD27A), alpha = o)
                }
            }
        }
    }
}

/** The SBO droplet dressed up for Halloween: witch hat and a little pumpkin. */
@Composable
fun HalloweenDrop(size: Int = 72) {
    Box(Modifier.size(size.dp)) {
        Box(Modifier.fillMaxSize().drawBehind {
            drawCircle(Brush.radialGradient(0f to Color(0x26F97316), 1f to Color.Transparent), radius = this.size.minDimension * 0.6f)
        })
        Image(painterResource(R.drawable.sbo_logo), "SBO", modifier = Modifier.fillMaxSize())
        val hat = remember {
            listOf(svg("M30 64 L52 6 Q56 0 60 8 L72 64 Z"), svg("M31 58 L71 58 L72 64 L30 64 Z"))
        }
        Canvas(
            Modifier.offset(x = (size * 0.26f).dp, y = (-size * 0.17f).dp)
                .size(width = (size * 0.52f).dp, height = (size * 0.52f * 0.8f).dp).graphicsLayer { rotationZ = -10f },
        ) {
            val k = this.size.width / 100f
            scale(k, k, pivot = Offset.Zero) {
                drawOval(Color(0xFF1C1029), Offset(4f, 57f), Size(92f, 18f))
                drawPath(hat[0], Color(0xFF241436))
                drawPath(hat[1], Color(0xFFF97316))
                drawRoundRect(Color(0xFFFBBF24), Offset(46f, 56f), Size(10f, 9f), androidx.compose.ui.geometry.CornerRadius(1.5f))
            }
        }
        Text("🎃", fontSize = (size * 0.34f).sp, lineHeight = (size * 0.4f).sp, modifier = Modifier.align(Alignment.BottomEnd).offset(x = 8.dp, y = 4.dp))
    }
}

/** Home banner on Halloween night. */
@Composable
fun HalloweenBanner() {
    val shape = RoundedCornerShape(24.dp)
    Row(
        Modifier.fillMaxWidth().drawBehind {
            // soft pumpkin glow (web: box-shadow 0 0 40px -12px rgba(249,115,22,.55))
            drawRoundRect(Brush.radialGradient(0f to Color(0x55F97316), 1f to Color.Transparent, center = center, radius = size.width * 0.6f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(24.dp.toPx()))
        }.background(Sbo.card.copy(alpha = 0.9f), shape).border(1.dp, Sbo.primary.copy(alpha = 0.4f), shape).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HalloweenDrop(64)
        Spacer(Modifier.width(4.dp))
        Column {
            Text("Notte di Halloween 🦇", fontFamily = SpaceGrotesk, fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.45).sp)
            Text("Solo per oggi SBO si è travestito. A mezzanotte torna tutto normale.", color = Sbo.mutedFg, fontSize = 12.sp, lineHeight = 16.sp)
        }
    }
}
