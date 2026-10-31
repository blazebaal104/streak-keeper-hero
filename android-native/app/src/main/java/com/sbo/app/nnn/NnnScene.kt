package com.sbo.app.nnn

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt

/** Seconds since this composable appeared, ticking every frame (read it in draw lambdas only). */
@Composable
fun rememberClock(): State<Float> {
    val t = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (true) withFrameNanos { t.floatValue = (it - start) / 1e9f }
    }
    return t
}

/** Neon flicker: mostly fully lit, with irregular quick dips like a tired tube. */
fun neon(t: Float, seed: Float): Float {
    val slow = 0.88f + 0.12f * sin(t * 2.1f + seed)
    val f = ((t * 7.3f + seed * 13f) % 9f)
    val dip = when {
        f < 0.12f -> 0.25f
        f in 0.2f..0.28f -> 0.55f
        f in 4.1f..4.16f -> 0.4f
        else -> 1f
    }
    return slow * dip
}

/**
 * The NNN backdrop behind the home: a huge neon "NNN" with "IN CORSO" under it, breathing and flickering,
 * warning signs hanging from the top on ropes and swinging like pendulums, and a cloth banner rippling in the wind.
 */
@Composable
fun NnnBackdrop(modifier: Modifier = Modifier, day: Int) {
    val clock = rememberClock()
    val tex = rememberTextures()
    Canvas(modifier.fillMaxSize()) {
        val t = clock.value
        val w = size.width; val h = size.height; val d = density
        // dark red vignette breathing
        val breath = 0.5f + 0.5f * sin(t * 1.3f)
        drawRect(Brush.radialGradient(0f to Color(0xFF3A0008).copy(alpha = 0.55f + 0.15f * breath), 1f to Color.Transparent, center = Offset(w / 2, h * 0.55f), radius = h * 0.7f))
        // giant neon NNN
        val k = neon(t, 1.7f)
        val ty = h * 0.11f
        neonText("NNN", Offset(w / 2, ty), w * 0.30f, Color(0xFFFF1744), k * (0.75f + 0.25f * breath), d)
        neonText("IN CORSO", Offset(w / 2, ty + w * 0.185f), w * 0.085f, Color(0xFFFF5252), neon(t, 4.2f) * 0.85f, d, spacing = 0.32f)
        neonText("GIORNO $day / 30", Offset(w / 2, ty + w * 0.27f), w * 0.04f, Color(0xFFFFB3B3), 0.6f, d, spacing = 0.2f)
        // hanging signs (pendulums: ω = √(g/L))
        signs.forEachIndexed { i, s ->
            val anchor = Offset(w * s.x, 0f)
            val len = if (s.rope < 0f) -s.rope * h else s.rope * d
            val omega = sqrt(9.81f / (len / d / 160f)).coerceIn(1.2f, 3.2f)
            val ang = s.amp * sin(t * omega + s.phase) + 2.2f * sin(t * 0.37f + i)
            rotate(ang, anchor) { hangingSign(s, anchor, len, d, tex, t) }
        }
        // banner rippling in the wind
        windBanner(Offset(w * 0.03f, h * 0.9f), Offset(w * 0.97f, h * 0.895f), 28f * d, t, "NO NUT NOVEMBER · NO NUT NOVEMBER · NO NUT NOVEMBER")
    }
}

private class SignSpec(val x: Float, val rope: Float, val amp: Float, val phase: Float, val type: Int, val scale: Float)

// rope < 0 = fraction of the area height
private val signs = listOf(
    SignSpec(0.12f, 14f, 7f, 0.3f, 0, 0.82f),
    SignSpec(0.88f, 30f, 8f, 4.0f, 1, 0.78f),
    SignSpec(0.5f, -0.27f, 3.2f, 2.1f, 2, 1f),
)

private fun DrawScope.hangingSign(s: SignSpec, anchor: Offset, len: Float, d: Float, tex: Textures, t: Float) {
    val sw = when (s.type) { 2 -> 150f; else -> 92f } * d * s.scale
    val sh = when (s.type) { 2 -> 54f; else -> 104f } * d * s.scale
    val top = anchor.y + len
    val c = Offset(anchor.x, top + sh / 2)
    // two ropes from one hook to the sign's corners
    val hook = anchor + Offset(0f, 6f * d)
    drawCircle(com.sbo.app.nnn.chrome(Rect(hook, 5f * d), 30f), 3.4f * d, hook)
    listOf(-1f, 1f).forEach { side ->
        val end = Offset(c.x + side * sw * 0.32f, top + 4f * d)
        cord(listOf(hook, Offset((hook.x + end.x) / 2f + side * 2f * d, (hook.y + end.y) / 2f), end), 3.2f * d, Color(0xFFB89B6A), Color(0xFF5E4A2C))
    }
    when (s.type) {
        0 -> { // yellow triangle ATTENZIONE + plate
            val tri = Path().apply { moveTo(c.x, top); lineTo(c.x + sw / 2, top + sw * 0.86f); lineTo(c.x - sw / 2, top + sw * 0.86f); close() }
            solid(tri, Brush.linearGradient(0f to Color(0xFFFFE04A), 1f to Color(0xFFF2B705), start = Offset(c.x - sw, top), end = Offset(c.x + sw, top + sw)), Rect(c.x - sw / 2, top, c.x + sw / 2, top + sw * 0.86f), 2.6f * d, gloss = 0.35f)
            clipPath(tri) { drawRect(tex.brush("rusty_metal_02", 120f * d, 30f, Offset(s.phase * 50f, 0f)), Offset(c.x - sw, top), Size(sw * 2, sw), alpha = 0.28f, blendMode = BlendMode.Multiply) }
            val inner = Path().apply { moveTo(c.x, top + 9f * d); lineTo(c.x + sw / 2 - 9f * d, top + sw * 0.86f - 6f * d); lineTo(c.x - sw / 2 + 9f * d, top + sw * 0.86f - 6f * d); close() }
            drawPath(inner, Color(0xFF111111), style = Stroke(4.2f * d, join = androidx.compose.ui.graphics.StrokeJoin.Round))
            label("!", Offset(c.x, top + sw * 0.56f), 42f * d, Color(0xFF111111))
            val pl = Rect(c.x - sw * 0.55f, top + sw * 0.9f, c.x + sw * 0.55f, top + sw * 0.9f + 22f * d)
            solid(Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(pl, CornerRadius(3f * d))) }, Brush.verticalGradient(0f to Color(0xFFFFE04A), 1f to Color(0xFFE0A800), startY = pl.top, endY = pl.bottom), pl, 1.6f * d)
            label("ATTENZIONE", pl.center, 13f * d, Color(0xFF111111), letter = 0.08f)
        }
        1 -> { // red prohibition disc
            val r = sw / 2
            val disc = Path().apply { addOval(Rect(c, r)) }
            solid(disc, Brush.radialGradient(0f to Color.White, 1f to Color(0xFFEDEDED), center = c, radius = r), Rect(c, r), 2.4f * d, gloss = 0.4f)
            drawCircle(Color(0xFFD50000), r * 0.86f, c, style = Stroke(r * 0.18f))
            rotate(-45f, c) { drawLine(Color(0xFFD50000), c + Offset(-r * 0.72f, 0f), c + Offset(r * 0.72f, 0f), r * 0.18f) }
            // hand pressing a button pictogram
            drawCircle(Color(0xFF111111), r * 0.22f, c + Offset(0f, r * 0.2f))
            drawRoundRect(Color(0xFF111111), c + Offset(-r * 0.06f, -r * 0.45f), Size(r * 0.12f, r * 0.5f), CornerRadius(r * 0.06f))
            clipPath(disc) { drawRect(tex.brush("rusty_metal_02", 90f * d, 60f), c - Offset(r, r), Size(r * 2, r * 2), alpha = 0.22f, blendMode = BlendMode.Multiply) }
            val pl = Rect(c.x - sw * 0.62f, c.y + r + 6f * d, c.x + sw * 0.62f, c.y + r + 28f * d)
            solid(Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(pl, CornerRadius(3f * d))) }, Brush.verticalGradient(0f to Color(0xFFE53935), 1f to Color(0xFFB71C1C), startY = pl.top, endY = pl.bottom), pl, 1.6f * d)
            label("VIETATO PREMERE", pl.center, 11f * d, Color.White, letter = 0.06f)
        }
        else -> { // hazard-striped plate with flickering bulbs
            val pl = Rect(c.x - sw / 2, top, c.x + sw / 2, top + sh)
            val p = Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(pl, CornerRadius(4f * d))) }
            solid(p, Brush.verticalGradient(0f to Color(0xFF262626), 1f to Color(0xFF0C0C0C), startY = pl.top, endY = pl.bottom), pl, 2f * d, gloss = 0.3f)
            clipPath(p) {
                var x = pl.left - sh
                while (x < pl.right) {
                    drawPath(Path().apply { moveTo(x, pl.bottom); lineTo(x + sh * 0.3f, pl.bottom - 8f * d); lineTo(x + sh * 0.3f + 12f * d, pl.bottom - 8f * d); lineTo(x + 12f * d, pl.bottom); close() }, Color(0xFFFFC400))
                    drawPath(Path().apply { moveTo(x, pl.top + 8f * d); lineTo(x + sh * 0.3f, pl.top); lineTo(x + sh * 0.3f + 12f * d, pl.top); lineTo(x + 12f * d, pl.top + 8f * d); close() }, Color(0xFFFFC400))
                    x += 24f * d
                }
            }
            val on = neon(t, 9.1f)
            label("NON PREMERE", pl.center + Offset(0f, -1f * d), 17f * d, Color(0xFFFF1744).copy(alpha = 0.4f + 0.6f * on), letter = 0.14f)
            drawIntoCanvas { cv ->
                val pp = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.argb((140 * on).toInt(), 255, 23, 68); textSize = 17f * d; textAlign = android.graphics.Paint.Align.CENTER
                    typeface = android.graphics.Typeface.DEFAULT_BOLD; letterSpacing = 0.14f
                    maskFilter = android.graphics.BlurMaskFilter(8f * d, android.graphics.BlurMaskFilter.Blur.NORMAL)
                }
                cv.nativeCanvas.drawText("NON PREMERE", pl.center.x, pl.center.y + 6f * d, pp)
            }
            listOf(pl.left + 9f * d, pl.right - 9f * d).forEach { x -> bolt(Offset(x, pl.center.y), 3f * d) }
        }
    }
}

/** Big glowing neon lettering: blurred halos + bright core tube. */
fun DrawScope.neonText(text: String, c: Offset, sizePx: Float, color: Color, k: Float, d: Float, spacing: Float = 0.06f) {
    drawIntoCanvas { cv ->
        fun paint(alpha: Int, blur: Float, col: Int, stroke: Float) = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            this.color = col; this.alpha = alpha; textSize = sizePx; textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.create("sans-serif-condensed", android.graphics.Typeface.BOLD); letterSpacing = spacing
            if (blur > 0f) maskFilter = android.graphics.BlurMaskFilter(blur, android.graphics.BlurMaskFilter.Blur.NORMAL)
            if (stroke > 0f) { style = android.graphics.Paint.Style.STROKE; strokeWidth = stroke }
        }
        val col = android.graphics.Color.rgb((color.red * 255).toInt(), (color.green * 255).toInt(), (color.blue * 255).toInt())
        val y = c.y + sizePx * 0.35f
        cv.nativeCanvas.drawText(text, c.x, y, paint((90 * k).toInt(), sizePx * 0.22f, col, 0f))
        cv.nativeCanvas.drawText(text, c.x, y, paint((150 * k).toInt(), sizePx * 0.06f, col, sizePx * 0.03f))
        cv.nativeCanvas.drawText(text, c.x, y, paint((255 * k).toInt(), 0f, col, sizePx * 0.022f))
        cv.nativeCanvas.drawText(text, c.x, y, paint((230 * k).toInt(), 0f, android.graphics.Color.rgb(255, 225, 225), sizePx * 0.008f))
    }
}

/** A cloth banner hung between two points, rippling: vertical waves travel along it with light/dark folds. */
fun DrawScope.windBanner(a: Offset, b: Offset, hgt: Float, t: Float, text: String) {
    val n = 40
    val pts = (0..n).map { i ->
        val s = i / n.toFloat()
        val sag = sin(s * PI.toFloat()) * hgt * 0.35f
        Offset(a.x + (b.x - a.x) * s, a.y + (b.y - a.y) * s + sag + sin(t * 3.3f - s * 10f) * hgt * 0.12f * sin(s * PI.toFloat()))
    }
    for (i in 0 until n) {
        val p0 = pts[i]; val p1 = pts[i + 1]
        val fold = sin(t * 3.3f - (i / n.toFloat()) * 10f + 1.2f)
        val shade = 0.62f + 0.38f * (fold * 0.5f + 0.5f)
        val q = Path().apply { moveTo(p0.x, p0.y); lineTo(p1.x + 0.5f, p1.y); lineTo(p1.x + 0.5f, p1.y + hgt); lineTo(p0.x, p0.y + hgt); close() }
        drawPath(q, Color(0.48f * shade, 0.02f * shade, 0.06f * shade))
    }
    // text following the first point heights (approximate)
    drawIntoCanvas { cv ->
        val path = android.graphics.Path().apply { moveTo(pts[0].x, pts[0].y + hgt * 0.68f); for (p in pts.drop(1)) lineTo(p.x, p.y + hgt * 0.68f) }
        val pp = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(230, 255, 230, 230); textSize = hgt * 0.5f; typeface = android.graphics.Typeface.create("sans-serif-condensed", android.graphics.Typeface.BOLD); letterSpacing = 0.18f
        }
        cv.nativeCanvas.drawTextOnPath(text, path, hgt * 0.4f, 0f, pp)
    }
    // ropes at the ends
    drawLine(Color(0xFF8D7550), a, a + Offset(-8f * density, -30f * density), 2.4f * density, StrokeCap.Round)
    drawLine(Color(0xFF8D7550), b, b + Offset(8f * density, -30f * density), 2.4f * density, StrokeCap.Round)
}

fun swingAngle(t: Float, amp: Float, omega: Float, phase: Float) = amp * sin(t * omega + phase) * (0.85f + 0.15f * abs(sin(t * 0.21f)))
