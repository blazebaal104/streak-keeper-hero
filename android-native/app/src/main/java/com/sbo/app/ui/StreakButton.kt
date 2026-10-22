package com.sbo.app.ui

import android.net.Uri
import android.view.TextureView
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.sbo.app.R
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

private val DISC = 192.dp

/**
 * The main SBO button.
 * fire    → real fire footage burning inside the disc; its flames are NOT cut by the circle at the
 *           top: they keep rising past the rim and fade out above it, like a block soaked in petrol.
 * ice     → photographic ice that thickens day by day.
 * neutral → the water drop.
 */
@Composable
fun StreakButton(fire: Int, ice: Int, onPress: () -> Unit, modifier: Modifier = Modifier) {
    val level = min(fire / 30f, 1f)
    val iceLevel = min(ice / 30f, 1f)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.9f else 1f, spring(dampingRatio = 0.42f, stiffness = 700f), label = "press")
    val plume: Dp = if (fire > 0) DISC * (0.55f + 0.45f * level) else 0.dp
    val base = 28.dp // room for the glow under the disc

    Box(modifier.size(width = 340.dp, height = DISC + plume + base * 2), contentAlignment = Alignment.BottomCenter) {
        when {
            fire > 0 -> FireButton(level, plume, base, scale, interaction, onPress)
            ice > 0 -> Disc(base, scale, interaction, onPress, glow = Color(0xFF38BDF8), glowAlpha = 0.35f + iceLevel * 0.4f) { IceSurface(ice, iceLevel) }
            else -> Disc(base, scale, interaction, onPress, glow = Sbo.dropGlow, glowAlpha = 0.45f) { NeutralSurface() }
        }
    }
}

@Composable
private fun Disc(
    base: Dp, scale: Float, interaction: MutableInteractionSource, onPress: () -> Unit,
    glow: Color, glowAlpha: Float, surface: @Composable () -> Unit,
) {
    Box(Modifier.padding(bottom = base).size(DISC).scale(scale), contentAlignment = Alignment.Center) {
        Glow(glow, glowAlpha)
        Box(
            Modifier.size(DISC).clip(CircleShape)
                .clickable(interaction, indication = null, onClick = onPress),
            contentAlignment = Alignment.Center,
        ) {
            surface()
            GlassDome(strong = true)
            PlusMark()
        }
    }
}

@Composable
private fun Glow(color: Color, alpha: Float, pulse: Boolean = false) {
    val t = rememberInfiniteTransition(label = "glow")
    val p by t.animateFloat(0.85f, 1f, infiniteRepeatable(tween(2200), RepeatMode.Reverse), label = "p")
    Canvas(Modifier.requiredSize(DISC * 1.9f)) {
        val r = size.minDimension / 2
        drawCircle(
            Brush.radialGradient(
                0f to color.copy(alpha = alpha * (if (pulse) p else 1f)),
                0.52f to color.copy(alpha = alpha * 0.75f * (if (pulse) p else 1f)),
                1f to Color.Transparent,
                center = center, radius = r,
            ),
            radius = r,
        )
    }
}

@Composable
private fun GlassDome(strong: Boolean) {
    Box(
        Modifier.fillMaxSize().background(
            Brush.linearGradient(
                listOf(Color.White.copy(alpha = if (strong) 0.30f else 0.10f), Color.Transparent, Color.Black.copy(alpha = if (strong) 0.35f else 0.10f)),
            ),
        ),
    )
}

@Composable
private fun PlusMark() {
    Icon(
        Icons.Rounded.Add, contentDescription = "Aggiungi 1 al contatore", tint = Color.White,
        modifier = Modifier.size(72.dp),
    )
}

@Composable
private fun NeutralSurface() {
    Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Sbo.dropA, Sbo.dropB))))
}

private val ICE = listOf(0 to R.drawable.ice_1, 3 to R.drawable.ice_2, 7 to R.drawable.ice_3, 14 to R.drawable.ice_4, 30 to R.drawable.ice_5)

@Composable
private fun IceSurface(days: Int, level: Float) {
    var i = 0
    for (k in 0 until ICE.lastIndex) if (days >= ICE[k].first) i = k
    val (aDay, a) = ICE[i]
    val (bDay, b) = ICE[min(i + 1, ICE.lastIndex)]
    val f = if (days >= ICE.last().first) 1f else ((days - aDay).toFloat() / (bDay - aDay)).coerceIn(0f, 1f)
    IcePlate(a, 1f)
    IcePlate(b, f)
    Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.06f + level * 0.3f), Color.Transparent))))
    Box(Modifier.fillMaxSize().background(Brush.radialGradient(0.6f to Color.Transparent, 1f to Color(0xFF082F49).copy(alpha = 0.55f))))
}

@Composable
private fun IcePlate(@DrawableRes res: Int, alpha: Float) {
    if (alpha <= 0f) return
    Image(painterResource(res), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().alpha(alpha))
}

// ---------------------------------------------------------------- fire

@Composable
private fun FireButton(level: Float, plume: Dp, base: Dp, scale: Float, interaction: MutableInteractionSource, onPress: () -> Unit) {
    val layerW = DISC * 1.45f
    val layerH = DISC + plume
    Box(Modifier.padding(bottom = base).size(width = layerW, height = layerH).scale(scale), contentAlignment = Alignment.BottomCenter) {
        // warm light thrown around the burning disc
        Box(Modifier.size(DISC), contentAlignment = Alignment.Center) { Glow(Color(0xFFFF7A1A), 0.55f + level * 0.4f, pulse = true) }
        // charred disc body
        Box(
            Modifier.size(DISC).clip(CircleShape).background(
                Brush.radialGradient(listOf(Color(0xFF5A1600).copy(alpha = 0.85f), Color(0xFF1A0400))),
            ),
        )
        // the fire: one continuous flame, round at the bottom and free at the top
        FlameLayer(level, layerH, Modifier.size(width = layerW, height = layerH))
        // inside the disc: soft vignette, glass, plus; also the touch target
        Box(
            Modifier.size(DISC).clip(CircleShape).clickable(interaction, indication = null, onClick = onPress),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.fillMaxSize().background(Brush.radialGradient(0.62f to Color.Transparent, 1f to Color(0xFF7D0A00).copy(alpha = 0.18f + level * 0.22f))))
            GlassDome(strong = false)
            PlusMark()
        }
        Embers(level, Modifier.size(width = layerW, height = layerH))
    }
}

/** Ellipse-shaped radial alpha (Compose radial gradients are circles, so scale the shader). */
private class EllipseBrush(val cx: Float, val cy: Float, val rx: Float, val ry: Float) : ShaderBrush() {
    override fun createShader(size: Size): Shader {
        val g = android.graphics.RadialGradient(
            cx, cy, rx,
            intArrayOf(0xFF000000.toInt(), 0xE6000000.toInt(), 0x80000000.toInt(), 0x00000000),
            floatArrayOf(0f, 0.45f, 0.75f, 1f), android.graphics.Shader.TileMode.CLAMP,
        )
        g.setLocalMatrix(android.graphics.Matrix().apply { setScale(1f, ry / rx, cx, cy) })
        return g
    }
}

@Composable
private fun FlameLayer(level: Float, layerH: Dp, modifier: Modifier) {
    val saturation = 1.25f + level * 0.6f
    Box(
        modifier
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
                blendMode = BlendMode.Screen // the footage's black background disappears
                colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(saturation) })
            }
            .drawWithContent {
                drawContent()
                val r = size.width / 1.45f / 2f // disc radius
                val c = Offset(size.width / 2f, size.height - r)
                drawIntoCanvas { canvas ->
                    // alpha mask = circle ∪ (plume above the disc, fading upwards and to the sides)
                    canvas.saveLayer(Rect(Offset.Zero, size), Paint().apply { blendMode = BlendMode.DstIn })
                    val plumeCy = c.y - r * 0.35f
                    drawRect(EllipseBrush(c.x, plumeCy, size.width * 0.5f, plumeCy))
                    // the plume only exists above the disc's equator, with a soft fade (no hard line)
                    drawRect(
                        Brush.verticalGradient(0f to Color.Black, (c.y - r * 0.45f) / size.height to Color.Black, (c.y - r * 0.05f) / size.height to Color.Transparent, 1f to Color.Transparent),
                        blendMode = BlendMode.DstIn,
                    )
                    drawCircle(Color.Black, r - 0.5f, c)
                    canvas.restore()
                }
            },
        contentAlignment = Alignment.BottomCenter,
    ) {
        // square footage, as tall as the whole layer: flame base at the bottom of the disc
        FireVideo(Modifier.requiredSize(layerH))
    }
}

@Composable
private fun FireVideo(modifier: Modifier) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse("android.resource://${context.packageName}/${R.raw.fire_loop}")))
            repeatMode = Player.REPEAT_MODE_ALL
            volume = 0f
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e ->
            when (e) { Lifecycle.Event.ON_RESUME -> player.play(); Lifecycle.Event.ON_PAUSE -> player.pause(); else -> {} }
        }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs); player.release() }
    }
    AndroidView(factory = { TextureView(it).also { tv -> player.setVideoTextureView(tv) } }, modifier = modifier)
}

/** Sparks thrown up from the rim. */
@Composable
private fun Embers(level: Float, modifier: Modifier) {
    val t = rememberInfiniteTransition(label = "embers")
    val time by t.animateFloat(0f, 1000f, infiniteRepeatable(tween(1_000_000, easing = LinearEasing)), label = "t")
    val seeds = remember { List(14) { i -> floatArrayOf(((i * 37) % 100) / 100f, 0.6f + ((i * 53) % 40) / 100f, ((i * 71) % 100) / 100f, 2f + (i % 3)) } }
    Canvas(modifier) {
        val r = size.width / 1.45f / 2f
        val rimY = size.height - 2 * r + r * 0.25f
        for (s in seeds) {
            val speed = s[1] * 0.75f
            val ph = ((time * speed + s[2]) % 1f)
            val x = size.width / 2f + (s[0] - 0.5f) * r * 1.5f + sin((ph * 2 * PI).toFloat()) * 10f
            val y = rimY - ph * (r * (0.9f + level * 0.9f))
            val a = sin((ph * PI).toFloat()).coerceAtLeast(0f)
            drawCircle(Color(0xFFFF8C1E).copy(alpha = a * 0.35f), radius = s[3].dp.toPx() * 2.2f, center = Offset(x, y))
            drawCircle(Color(0xFFFFD27A).copy(alpha = a), radius = s[3].dp.toPx() * 0.6f, center = Offset(x, y))
        }
    }
}
