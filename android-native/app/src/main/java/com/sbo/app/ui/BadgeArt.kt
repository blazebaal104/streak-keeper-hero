package com.sbo.app.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sbo.app.logic.BadgeDef
import kotlin.math.floor
import kotlin.math.sin

val BadgeShape = RoundedCornerShape(28.dp) // web rounded-2xl = radius + 8px

private fun rnd(seed: Int, i: Int): Float { val x = sin(seed * 17.3 + i * 91.7) * 10000; return (x - floor(x)).toFloat() }

private val grayPaint = Paint().apply { colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) }

/**
 * The medal artwork, same layers as the web BadgeArt: 135° gradient tile with a coloured ring,
 * glass highlight, and richness that grows with rarity — shine sweep (rare+), ember glow (fire,
 * super rare+), bobbing emblem (super rare+), rotating sheen (epic+), sparkles, frost (ice).
 * `size` = null fills the width it is given (inventory grid).
 */
@Composable
fun BadgeArt(def: BadgeDef, side: Dp? = 64.dp, locked: Boolean = false, modifier: Modifier = Modifier) {
    val rank = def.tier.rank
    val isIce = def.category == "ice"
    val seed = def.threshold * (if (isIce) 7 else 3)
    val sparkles = when { rank <= 2 -> 0; rank == 3 -> 2; rank == 4 -> 3; rank == 5 -> 4; rank == 6 -> 5; else -> 6 }
    val t = rememberInfiniteTransition(label = "badge")
    val spin by t.animateFloat(0f, 360f, infiniteRepeatable(tween(if (rank >= 7) 4000 else 7000, easing = LinearEasing)), label = "spin")
    val sheenDur = if (rank >= 6) 3000 else 5000
    val sheen by t.animateFloat(0f, 1f, infiniteRepeatable(keyframes { durationMillis = sheenDur; 0f at 0 using FastOutSlowInEasing; 1f at (sheenDur * 0.22f).toInt(); 1f at sheenDur }), label = "sheen")
    val ember by t.animateFloat(0.4f, 0.9f, infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "ember")
    val bob by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1700, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bob")
    val clock = t.animateFloat(0f, 1000f, infiniteRepeatable(tween(1_000_000, easing = LinearEasing)), label = "clock")

    val sized = if (side != null) modifier.size(side) else modifier.fillMaxWidth().aspectRatio(1f)
    BoxWithConstraints(
        sized.clip(BadgeShape).border(2.dp, def.ring, BadgeShape)
            .drawBehind {
                drawRect(Brush.linearGradient(def.gradient, start = Offset.Zero, end = Offset(size.width, size.height)))
                if (rank >= 5) rotate(spin, pivot = center) {
                    val tint = if (isIce) Color.White.copy(alpha = 0.9f) else Color(0xFFFFF0B4).copy(alpha = 0.9f)
                    drawCircle(
                        Brush.sweepGradient(0f to Color.Transparent, 0.06f to tint.copy(alpha = tint.alpha * 0.4f), 0.2f to tint.copy(alpha = tint.alpha * 0.4f), 0.4f to Color.Transparent, 1f to Color.Transparent, center = center),
                        radius = size.maxDimension, center = center,
                    )
                }
                // glass highlight: white/35 → transparent → black/20 (to bottom right)
                drawRect(Brush.linearGradient(0f to Color.White.copy(alpha = 0.35f), 0.5f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.2f), start = Offset.Zero, end = Offset(size.width, size.height)))
                if (rank >= 3) {
                    val w = size.width / 3f
                    val x = (-1.2f + 3.4f * sheen) * w
                    withTransform({ translate(x, 0f); transform(androidx.compose.ui.graphics.Matrix().apply { values[4] = -0.21f }) }) {
                        drawRect(Brush.horizontalGradient(0f to Color.Transparent, 0.15f to Color.White.copy(alpha = 0.35f), 0.85f to Color.White.copy(alpha = 0.35f), 1f to Color.Transparent, startX = 0f, endX = w), size = androidx.compose.ui.geometry.Size(w, size.height))
                    }
                }
                if (isIce) {
                    val s = size.width / 100f
                    val st = 1.2f * s
                    val frost = Color.White.copy(alpha = 0.4f)
                    fun l(x1: Float, y1: Float, x2: Float, y2: Float) = drawLine(frost, Offset(x1 * s, y1 * size.height / 100f), Offset(x2 * s, y2 * size.height / 100f), st, StrokeCap.Butt)
                    l(50f, 6f, 50f, 94f); l(6f, 50f, 94f, 50f); l(18f, 18f, 82f, 82f); l(82f, 18f, 18f, 82f)
                    l(50f, 22f, 40f, 32f); l(50f, 22f, 60f, 32f); l(50f, 78f, 40f, 68f); l(50f, 78f, 60f, 68f)
                }
                if (!isIce && rank >= 4) {
                    drawRect(Brush.radialGradient(0f to Color(0xFFFFC850).copy(alpha = 0.55f * ember), 0.6f to Color.Transparent, center = Offset(size.width / 2f, size.height * 0.8f), radius = size.maxDimension * 0.75f))
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        val px = maxWidth
        // emblem
        Box(
            Modifier.fillMaxSize().graphicsLayer {
                if (rank >= 4) { translationY = -0.06f * bob * px.toPx() * 0.46f; rotationZ = 3f * bob }
            }.then(if (locked) Modifier.drawWithContent {
                drawIntoCanvas { c -> c.saveLayer(Rect(Offset.Zero, size), grayPaint); drawContent(); c.restore() }
            }.graphicsLayer { alpha = 0.5f } else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            if (def.category == "nnn") com.sbo.app.nnn.NnnEmblem(Modifier.fillMaxSize(0.82f))
            else Text(def.emoji, fontSize = (px.value * 0.46f).sp, lineHeight = (px.value * 0.5f).sp, textAlign = TextAlign.Center)
        }
        // sparkles ✦
        for (i in 0 until sparkles) {
            val dur = 1.4f + rnd(seed, i + 120) * 1.8f
            val delay = rnd(seed, i + 160) * 2f
            val fs = maxOf(6f, px.value * (0.08f + rnd(seed, i + 80) * 0.08f))
            Text(
                "✦", color = if (isIce) Color.White else Color(0xFFFEF9C3), fontSize = fs.sp, lineHeight = fs.sp,
                modifier = Modifier.align(Alignment.TopStart).graphicsLayer {
                    val fx = rnd(seed, i) * 0.88f; var fy = rnd(seed, i + 40) * 0.84f
                    // the NNN emblem has lettering in the middle: keep sparkles off it
                    if (def.category == "nnn" && fx in 0.18f..0.7f && fy in 0.22f..0.72f) fy = if (fy < 0.47f) 0.06f else 0.8f
                    translationX = fx * px.toPx(); translationY = fy * px.toPx()
                    val ph = (((clock.value - delay) / dur) % 1f + 1f) % 1f
                    val k = 0.5f - 0.5f * kotlin.math.cos(ph * 2f * Math.PI.toFloat())
                    alpha = k; scaleX = 0.4f + 0.6f * k; scaleY = 0.4f + 0.6f * k
                },
            )
        }
        if (locked) Box(Modifier.fillMaxSize().drawBehind { drawRect(Color.Black.copy(alpha = 0.55f)) })
    }
}

