package com.sbo.app.nnn

import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Matrix
import android.graphics.Shader
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import kotlin.math.cos
import kotlin.math.sin

/**
 * Real CC0 photo textures (Poly Haven, 2K) fetched at build time into res/drawable-nodpi/tex_*.webp.
 * Looked up by name so the app still builds (flat colours) if a texture is missing.
 */
class Textures(private val ctx: android.content.Context) {
    private val cache = HashMap<String, android.graphics.Bitmap?>()
    fun bitmap(name: String): android.graphics.Bitmap? = cache.getOrPut(name) {
        val id = ctx.resources.getIdentifier("tex_$name", "drawable", ctx.packageName)
        if (id == 0) null else runCatching { BitmapFactory.decodeResource(ctx.resources, id, BitmapFactory.Options().apply { inScaled = false }) }.getOrNull()
    }

    /**
     * A brush painting [name] across the drawing with the texture [scale] (px per texture tile),
     * rotated [deg] and shifted by [shift] so neighbouring objects never show the same patch.
     */
    fun brush(name: String, scale: Float, deg: Float = 0f, shift: Offset = Offset.Zero, fallback: Color = Color.Gray): Brush {
        val b = bitmap(name) ?: return androidx.compose.ui.graphics.SolidColor(fallback)
        val sh = BitmapShader(b, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
        val k = scale / b.width
        sh.setLocalMatrix(Matrix().apply { postScale(k, k); postRotate(deg); postTranslate(shift.x, shift.y) })
        return ShaderBrush(sh)
    }
}

@Composable
fun rememberTextures(): Textures {
    val ctx = LocalContext.current
    return remember { Textures(ctx.applicationContext) }
}

// ---------------------------------------------------------------- lighting helpers (light from the top-left)

/** Soft contact shadow under a shape (real blur via a paint shadow layer). */
fun DrawScope.dropShadow(path: Path, radius: Float, dy: Float, alpha: Float = 0.55f) {
    drawIntoCanvas { c ->
        val p = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.TRANSPARENT
            setShadowLayer(radius, radius * 0.25f, dy, android.graphics.Color.argb((alpha * 255).toInt(), 0, 0, 0))
        }
        c.nativeCanvas.drawPath(path.asAndroidPath(), p)
    }
}

/** Fill [path] with a texture, then shade it like a solid object: lit top-left, darker bottom-right, bevelled rim. */
fun DrawScope.solid(path: Path, fill: Brush, bounds: Rect, bevel: Float, tint: Color? = null, gloss: Float = 0.25f, shadow: Float = 0.55f) {
    if (shadow > 0f) dropShadow(path, bevel * 3.2f, bevel * 1.6f, shadow)
    drawPath(path, fill)
    if (tint != null) drawPath(path, tint, blendMode = BlendMode.Multiply)
    clipPath(path) {
        // broad light falloff
        drawRect(
            Brush.linearGradient(
                0f to Color.White.copy(alpha = gloss), 0.45f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.45f),
                start = bounds.topLeft, end = bounds.bottomRight,
            ),
        )
    }
    // bevel: light rim on top-left, dark rim on bottom-right
    drawPath(path, Brush.linearGradient(0f to Color.White.copy(alpha = 0.55f), 0.5f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.6f), start = bounds.topLeft, end = bounds.bottomRight), style = Stroke(bevel, join = StrokeJoin.Round))
    drawPath(path, Color.Black.copy(alpha = 0.55f), style = Stroke(bevel * 0.22f))
}

/** Polished steel / chrome: banded reflections across [bounds] (environment-map look). */
fun chrome(bounds: Rect, deg: Float = 35f, dark: Boolean = false): Brush {
    val a = Math.toRadians(deg.toDouble())
    val r = maxOf(bounds.width, bounds.height) / 2f
    val c = bounds.center
    val s = Offset(c.x - (cos(a) * r).toFloat(), c.y - (sin(a) * r).toFloat())
    val e = Offset(c.x + (cos(a) * r).toFloat(), c.y + (sin(a) * r).toFloat())
    return if (dark) Brush.linearGradient(
        0f to Color(0xFF4A4F55), 0.18f to Color(0xFFB9C0C7), 0.32f to Color(0xFF3A3F45), 0.55f to Color(0xFF22262B),
        0.72f to Color(0xFF8C939A), 0.86f to Color(0xFF2E3237), 1f to Color(0xFF5C6268), start = s, end = e,
    ) else Brush.linearGradient(
        0f to Color(0xFF8E959C), 0.15f to Color(0xFFF2F5F7), 0.3f to Color(0xFF9AA2A9), 0.5f to Color(0xFF545A61),
        0.68f to Color(0xFFD9DEE2), 0.82f to Color(0xFF6E757C), 1f to Color(0xFFB4BBC1), start = s, end = e,
    )
}

/** Brass (padlocks, keys). */
fun brass(bounds: Rect): Brush = Brush.linearGradient(
    0f to Color(0xFF7A5A1C), 0.2f to Color(0xFFF6DA8A), 0.38f to Color(0xFFC59A3B), 0.6f to Color(0xFF8A6420),
    0.8f to Color(0xFFE9C46A), 1f to Color(0xFF6E4E15), start = bounds.topLeft, end = bounds.bottomRight,
)

/** Small specular glint. */
fun DrawScope.glint(at: Offset, r: Float, alpha: Float = 0.9f) {
    drawCircle(Brush.radialGradient(0f to Color.White.copy(alpha = alpha), 1f to Color.Transparent, center = at, radius = r), r, at)
}

/** Hex bolt head seen from above, [turned] 0..1 animates it backing out. */
fun DrawScope.bolt(c: Offset, r: Float, rot: Float = 0f) {
    val p = Path()
    for (i in 0 until 6) {
        val a = Math.toRadians((rot + i * 60).toDouble())
        val x = c.x + (cos(a) * r).toFloat(); val y = c.y + (sin(a) * r).toFloat()
        if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
    }
    p.close()
    drawCircle(Color.Black.copy(alpha = 0.5f), r * 1.35f, c + Offset(r * 0.18f, r * 0.3f)) // washer shadow
    drawCircle(chrome(Rect(c, r * 1.3f), 120f, dark = true), r * 1.3f, c) // washer
    solid(p, chrome(Rect(c, r), 30f), Rect(c, r), r * 0.18f, shadow = 0.4f)
    drawCircle(Color.Black.copy(alpha = 0.25f), r * 0.45f, c)
    glint(c + Offset(-r * 0.35f, -r * 0.4f), r * 0.5f, 0.7f)
}

/** Nail head (round, slightly domed) or the hole it leaves. */
fun DrawScope.nail(c: Offset, r: Float, pulled: Boolean) {
    if (pulled) {
        drawCircle(Brush.radialGradient(0f to Color.Black, 0.7f to Color(0xFF1A120A), 1f to Color.Transparent, center = c, radius = r * 0.9f), r * 0.9f, c)
        return
    }
    drawCircle(Color.Black.copy(alpha = 0.55f), r * 1.15f, c + Offset(r * 0.25f, r * 0.35f))
    drawCircle(chrome(Rect(c, r), 50f), r, c)
    drawCircle(Brush.radialGradient(0f to Color.White.copy(alpha = 0.7f), 0.6f to Color.Transparent, center = c + Offset(-r * 0.35f, -r * 0.35f), radius = r), r, c)
    drawCircle(Color.Black.copy(alpha = 0.5f), r, c, style = Stroke(r * 0.12f))
    // hammer dents
    drawCircle(Color.Black.copy(alpha = 0.18f), r * 0.35f, c + Offset(r * 0.2f, r * 0.1f))
}

/** A thick cord along [pts] with twisted strands (rope, cables). */
fun DrawScope.cord(pts: List<Offset>, w: Float, base: Color, dark: Color, twist: Float = 1f) {
    if (pts.size < 2) return
    val path = Path().apply {
        moveTo(pts[0].x, pts[0].y)
        for (i in 1 until pts.size) {
            val a = pts[i - 1]; val b = pts[i]
            quadraticTo(a.x, a.y, (a.x + b.x) / 2f, (a.y + b.y) / 2f)
        }
        lineTo(pts.last().x, pts.last().y)
    }
    drawPath(path, Color.Black.copy(alpha = 0.45f), style = Stroke(w * 1.1f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(path, dark, style = Stroke(w, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(path, base, style = Stroke(w * 0.78f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    // twisted strands: short diagonal ticks along the cord
    val pm = android.graphics.PathMeasure(path.asAndroidPath(), false)
    val len = pm.length
    val step = w * 0.55f / twist
    var d = 0f
    val pos = FloatArray(2); val tan = FloatArray(2)
    while (d < len) {
        pm.getPosTan(d, pos, tan)
        val nx = -tan[1]; val ny = tan[0]
        val p0 = Offset(pos[0] + nx * w * 0.38f - tan[0] * w * 0.22f, pos[1] + ny * w * 0.38f - tan[1] * w * 0.22f)
        val p1 = Offset(pos[0] - nx * w * 0.38f + tan[0] * w * 0.22f, pos[1] - ny * w * 0.38f + tan[1] * w * 0.22f)
        drawLine(dark.copy(alpha = 0.75f), p0, p1, w * 0.13f, cap = StrokeCap.Round)
        drawLine(Color.White.copy(alpha = 0.18f), p0 + Offset(-w * 0.06f, -w * 0.06f), Offset((p0.x + p1.x) / 2f, (p0.y + p1.y) / 2f), w * 0.08f, cap = StrokeCap.Round)
        d += step
    }
    // light on top
    drawPath(path, Color.White.copy(alpha = 0.12f), style = Stroke(w * 0.25f, cap = StrokeCap.Round))
}

/** Chain of interlocking oval links along a straight segment. */
fun DrawScope.chain(a: Offset, b: Offset, link: Float, sag: Float = 0f) {
    val dx = b.x - a.x; val dy = b.y - a.y
    val len = kotlin.math.hypot(dx, dy)
    val n = (len / (link * 0.78f)).toInt().coerceAtLeast(2)
    val ang = Math.toDegrees(kotlin.math.atan2(dy, dx).toDouble()).toFloat()
    for (i in 0..n) {
        val t = i / n.toFloat()
        val c = Offset(a.x + dx * t, a.y + dy * t + sag * 4f * t * (1 - t))
        val flat = i % 2 == 0
        val w = link; val h = if (flat) link * 0.58f else link * 0.24f
        androidx.compose.ui.graphics.drawscope.rotate(ang, c) {
            val r = Rect(Offset(c.x - w / 2, c.y - h / 2), Size(w, h))
            drawOval(Color.Black.copy(alpha = 0.5f), r.topLeft + Offset(link * 0.08f, link * 0.12f), r.size, style = Stroke(link * 0.2f))
            drawOval(chrome(r, 70f, dark = !flat), r.topLeft, r.size, style = Stroke(link * 0.19f))
            if (flat) drawOval(Color.White.copy(alpha = 0.35f), r.topLeft + Offset(w * 0.15f, h * 0.05f), Size(w * 0.4f, h * 0.3f), style = Stroke(link * 0.05f))
        }
    }
}

/** Text drawn straight onto the canvas (signs, labels, LCD). */
fun DrawScope.label(text: String, at: Offset, sizePx: Float, color: Color, bold: Boolean = true, align: android.graphics.Paint.Align = android.graphics.Paint.Align.CENTER, font: android.graphics.Typeface? = null, letter: Float = 0f) {
    drawIntoCanvas { c ->
        val p = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            this.color = android.graphics.Color.argb((color.alpha * 255).toInt(), (color.red * 255).toInt(), (color.green * 255).toInt(), (color.blue * 255).toInt())
            textSize = sizePx; textAlign = align
            typeface = font ?: android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, if (bold) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
            letterSpacing = letter
        }
        c.nativeCanvas.drawText(text, at.x, at.y + sizePx * 0.35f, p)
    }
}

