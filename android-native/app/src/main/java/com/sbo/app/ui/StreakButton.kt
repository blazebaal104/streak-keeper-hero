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
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
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

/** Screenshot tests: show this still frame of the fire footage instead of playing the video. */
val LocalFireStill = staticCompositionLocalOf<Int?> { null }

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
    val press = rememberPressScale()
    val base = 28.dp // room for the glow under the disc
    // Same footprint in every state: the flames overflow upwards without pushing the layout.
    Box(modifier.size(width = 340.dp, height = DISC + base * 2), contentAlignment = Alignment.BottomCenter) {
        when {
            fire > 0 -> FireButton(level, base, press, onPress)
            ice > 0 -> Disc(base, press, onPress, glow = Color(0xFF38BDF8), glowAlpha = 0.3f + iceLevel * 0.4f, iceAura = true) { IceSurface(ice, iceLevel) }
            else -> Disc(base, press, onPress, glow = Sbo.dropGlow, glowAlpha = 0.45f) { NeutralSurface() }
        }
    }
}

@Composable
private fun Disc(
    base: Dp, press: PressScale, onPress: () -> Unit,
    glow: Color, glowAlpha: Float, iceAura: Boolean = false, surface: @Composable () -> Unit,
) {
    Box(Modifier.padding(bottom = base).size(DISC).graphicsLayer { scaleX = press.value; scaleY = press.value }, contentAlignment = Alignment.Center) {
        Glow(glow, glowAlpha, reach = 1.55f, flicker = if (iceAura) 0.25f else 0f, speed = 3600)
        Box(
            Modifier.size(DISC).clip(CircleShape).pressable(press, "Aggiungi 1 al contatore", onPress),
            contentAlignment = Alignment.Center,
        ) {
            surface()
            GlassDome(strong = true)
            PlusMark()
        }
    }
}

/** Light thrown around the disc. `reach` = outer radius in disc radii. */
@Composable
private fun Glow(color: Color, alpha: Float, reach: Float, flicker: Float = 0f, speed: Int = 2200) {
    val t = rememberInfiniteTransition(label = "glow")
    val a by t.animateFloat(0f, 1f, infiniteRepeatable(tween(speed, easing = LinearEasing)), label = "a")
    Canvas(Modifier.requiredSize(DISC * reach)) {
        val k = if (flicker > 0f) 1f - flicker * (0.5f + 0.3f * sin(a * 2 * PI.toFloat()) + 0.2f * sin(a * 7 * PI.toFloat())) else 1f
        val r = size.minDimension / 2
        val inner = (1f / reach) * 0.92f
        drawCircle(
            Brush.radialGradient(
                0f to color.copy(alpha = alpha * k), inner to color.copy(alpha = alpha * k),
                (inner + 1f) / 2f to color.copy(alpha = alpha * 0.35f * k), 1f to Color.Transparent,
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
    Icon(Lucide.PlusBold, contentDescription = null, tint = Color.White, modifier = Modifier.size(64.dp))
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

/**
 * One fire. Inside the disc it is cut round; at the top it is not: the same flames carry on past the
 * rim, from roughly 10 to 2 o'clock, and die out 25-35% of a diameter above it. A white-hot band sits
 * exactly on the rim where the fire breaks out, so inside and outside read as one burning body.
 */
@Composable
private fun FireButton(level: Float, base: Dp, press: PressScale, onPress: () -> Unit) {
    val overflow = DISC * (0.2f + 0.035f * level)
    val layerW = DISC * 1.5f
    val layerH = DISC + overflow
    Box(
        Modifier
            .layout { measurable, constraints ->
                // taller than the slot we get: anchor the disc at the bottom, let the flames spill upwards
                val p = measurable.measure(Constraints.fixed(layerW.roundToPx(), layerH.roundToPx()))
                layout(constraints.maxWidth, constraints.maxHeight) {
                    p.place((constraints.maxWidth - p.width) / 2, constraints.maxHeight - base.roundToPx() - p.height)
                }
            }
            .graphicsLayer {
                scaleX = press.value; scaleY = press.value
                transformOrigin = TransformOrigin(0.5f, 1f - (DISC / 2) / layerH) // press around the disc centre
            },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Box(Modifier.size(DISC), contentAlignment = Alignment.Center) {
            Glow(Color(0xFFFF7A1A), 0.5f + level * 0.35f, reach = 1.45f, flicker = 0.35f, speed = 1300)
        }
        // body of the disc: a hot red-orange core, never black
        Box(Modifier.size(DISC).clip(CircleShape).background(Brush.radialGradient(0f to Color(0xFFE0661A), 0.65f to Color(0xFFC0500F), 1f to Color(0xFFB04A10))))
        FlameLayer(level, overflow, Modifier.size(width = layerW, height = layerH))
        HotRim(level, Modifier.size(width = layerW, height = layerH))
        Box(
            Modifier.size(DISC).clip(CircleShape).pressable(press, "Aggiungi 1 al contatore", onPress),
            contentAlignment = Alignment.Center,
        ) {
            PlusMark()
        }
        Sparks(level, overflow, Modifier.size(width = layerW, height = layerH))
    }
}

@Composable
private fun FlameLayer(level: Float, overflow: Dp, modifier: Modifier) {
    val saturation = 1.25f + level * 0.5f
    val layerPaint = remember(saturation) {
        Paint().apply {
            blendMode = BlendMode.Screen // the footage's black background disappears, flames add light
            colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(saturation) })
        }
    }
    Box(
        modifier.drawWithContent {
            val r = DISC.toPx() / 2f
            val c = Offset(size.width / 2f, size.height - r)
            val reach = (r + overflow.toPx()) / r
            drawIntoCanvas { canvas ->
                canvas.saveLayer(Rect(Offset.Zero, size), layerPaint)
                drawContent()
                // mask = disc ∪ a band hugging the upper rim; it follows the arc (so the flame tops are
                // shaped by the footage itself, not by a straight line) and dies out gently
                canvas.saveLayer(Rect(Offset.Zero, size), Paint().apply { blendMode = BlendMode.DstIn })
                drawRect(
                    Brush.radialGradient(
                        0f to Color.Black, (1f / reach) to Color.Black,
                        (0.35f / reach + 0.65f) to Color.Black.copy(alpha = 0.7f), 1f to Color.Transparent,
                        center = c, radius = r * reach,
                    ),
                )
                // only the upper part breaks out: fade the band away towards the equator (no hard line)
                drawRect(
                    Brush.verticalGradient(
                        0f to Color.Black, (c.y - r * 0.35f) / size.height to Color.Black,
                        (c.y + r * 0.1f) / size.height to Color.Transparent, 1f to Color.Transparent,
                    ),
                    blendMode = BlendMode.DstIn,
                )
                drawCircle(Color.Black, r - 0.5f, c)
                canvas.restore()
                canvas.restore()
            }
        },
    ) { FireVideo(Modifier.fillMaxSize()) }
}

/** White-hot line exactly where the fire crosses the rim (upper half), screen-blended. */
@Composable
private fun HotRim(level: Float, modifier: Modifier) {
    val t = rememberInfiniteTransition(label = "rim")
    val a by t.animateFloat(0f, 1f, infiniteRepeatable(tween(900, easing = LinearEasing)), label = "a")
    Canvas(modifier) {
        val r = DISC.toPx() / 2f
        val c = Offset(size.width / 2f, size.height - r)
        val k = 0.8f + 0.2f * sin(a * 2 * PI.toFloat())
        val band = r * 0.1f
        val stops = arrayOf(
            0f to Color.Transparent,
            (r - band) / (r + band) to Color.Transparent,
            (r - band * 0.25f) / (r + band) to Color(0xFFFFE6A8).copy(alpha = (0.35f + level * 0.15f) * k),
            r / (r + band) to Color(0xFFFFB347).copy(alpha = 0.22f * k),
            1f to Color.Transparent,
        )
        drawIntoCanvas { canvas ->
            canvas.saveLayer(Rect(Offset.Zero, size), Paint().apply { blendMode = BlendMode.Screen })
            drawCircle(Brush.radialGradient(*stops, center = c, radius = r + band), radius = r + band, center = c)
            drawRect(
                Brush.verticalGradient(0f to Color.Black, (c.y - r * 0.55f) / size.height to Color.Black, (c.y - r * 0.2f) / size.height to Color.Transparent, 1f to Color.Transparent),
                blendMode = BlendMode.DstIn,
            )
            canvas.restore()
        }
    }
}

@Composable
private fun FireVideo(modifier: Modifier) {
    LocalFireStill.current?.let { still ->
        Image(painterResource(still), null, contentScale = ContentScale.FillBounds, modifier = modifier)
        return
    }
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    var ready by remember { mutableStateOf(false) }
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
        val listener = object : Player.Listener { override fun onRenderedFirstFrame() { ready = true } }
        player.addListener(listener)
        val obs = LifecycleEventObserver { _, e ->
            when (e) { Lifecycle.Event.ON_RESUME -> player.play(); Lifecycle.Event.ON_PAUSE -> player.pause(); else -> {} }
        }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs); player.removeListener(listener); player.release() }
    }
    Box(modifier) {
        // still frame of the same footage until the video has its first frame: the fire never blinks off
        if (!ready) Image(painterResource(R.drawable.fire_poster), null, contentScale = ContentScale.FillBounds, modifier = Modifier.fillMaxSize())
        AndroidView(factory = { TextureView(it).also { tv -> player.setVideoTextureView(tv) } }, modifier = Modifier.fillMaxSize())
    }
}

/** Small sharp sparks born at random points of the upper rim, rising with jitter and fading. */
@Composable
private fun Sparks(level: Float, overflow: Dp, modifier: Modifier) {
    val t = rememberInfiniteTransition(label = "sparks")
    val time by t.animateFloat(0f, 1000f, infiniteRepeatable(tween(1_000_000, easing = LinearEasing)), label = "t")
    val n = 16
    Canvas(modifier) {
        val r = DISC.toPx() / 2f
        val c = Offset(size.width / 2f, size.height - r)
        for (i in 0 until n) {
            val life = 1.1f + hash(i, 0) * 0.9f
            val local = (time + hash(i, 1) * life) / life
            val cycle = local.toInt()
            val ph = local - cycle
            val sx = ((hash(i, cycle + 7) + hash(i, cycle + 13) + hash(i * 3 + 1, cycle)) / 1.5f - 1f).coerceIn(-0.8f, 0.8f) // -0.8r .. 0.8r, independent per spark and per life
            val start = Offset(c.x + sx * r, c.y - r * kotlin.math.sqrt(1f - sx * sx) - overflow.toPx() * (0.3f + 0.3f * hash(i, cycle + 11)))
            val rise = (overflow.toPx() * 0.45f) * (0.6f + 0.4f * hash(i, cycle + 3))
            val drift = (hash(i, cycle + 5) - 0.5f) * r * 0.5f
            val p = Offset(start.x + drift * ph + sin(ph * 9f + i) * 3f, start.y - rise * ph)
            val a = (1f - ph) * (if (ph < 0.12f) ph / 0.12f else 1f)
            val sz = (1.2f + 1.6f * hash(i, cycle + 9)).dp.toPx()
            drawCircle(Color(0xFFFFE1A0).copy(alpha = a), radius = sz * (1f - 0.5f * ph), center = p)
        }
    }
}

private fun hash(i: Int, j: Int): Float {
    var h = i * 374761393 + j * 668265263
    h = (h xor (h ushr 13)) * 1274126177
    return ((h xor (h ushr 16)) and 0xFFFFFF) / 16777215f
}
