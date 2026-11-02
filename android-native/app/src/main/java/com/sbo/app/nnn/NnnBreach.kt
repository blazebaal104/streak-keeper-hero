package com.sbo.app.nnn

import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sbo.app.R
import com.sbo.app.ui.StreakButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/** The real button's disc (StreakButton), the reference every sprite is sized against: sprite side = 2 discs. */
private val DISC = 192.dp

private class Flying(val img: ImageBitmap?, val at: Offset, val side: Float, val born: Float, val vx: Float, val vy: Float, val spin: Float, val tilt: Float, val color: Color = Color.White, val r: Float = 0f)
private class Shout(val img: ImageBitmap, val at: Offset, val rot: Float, val born: Float, val dir: Offset)

/**
 * The breach. Tapping the protected button on the home zooms it until it's the only thing left on a nearly black
 * screen. Every tap is one real action on the outermost protection; when one comes off it jumps up and drops
 * off the screen and the button grows a little. Coach Zero's red shouts keep piling up, the screen turns redder
 * and redder. Once the last protection is gone, taps are ignored for a moment (no accidental press while
 * spamming); the next deliberate press blows everything away and "NNN PERSA" rises from the dark.
 */
@Composable
fun NnnBreach(
    kinds: List<Kind>, seed: Int, fire: Int, ice: Int, day: Int, from: Rect?,
    onFall: () -> Unit, onDone: () -> Unit, startLayer: Int = 0,
) {
    val view = LocalView.current
    val clock = rememberClock()
    val scope = rememberCoroutineScope()
    var layer by remember { mutableIntStateOf(startLayer) }
    var step by remember { mutableIntStateOf(0) }
    var removed by remember { mutableIntStateOf(0) }
    var phase by remember { mutableStateOf("dig") } // dig | bare | blast | lost
    var lockedUntil by remember { mutableFloatStateOf(0f) }
    var lastTap by remember { mutableFloatStateOf(0f) }
    val flying = remember { mutableStateListOf<Flying>() }
    val shouts = remember { mutableStateListOf<Shout>() }
    val bubbles = remember { mutableStateListOf<Pair<String, Float>>() }
    val zoom = remember { Animatable(0f) }      // 0 = where it sat on the home, 1 = centre stage
    val grow = remember { Animatable(0f) }      // extra size earned by every layer removed
    val shake = remember { Animatable(0f) }
    val blast = remember { Animatable(0f) }
    val flash = remember { Animatable(0f) }
    val red = remember { Animatable(0f) }       // how red the whole screen has turned
    val r = remember { Random(seed * 7 + 3) }
    val sprites = rememberSprites(kinds, layer)
    val stamps = remember { StampMaker(view.context) }
    BackHandler(enabled = phase == "dig" || phase == "bare") { onDone() }
    LaunchedEffect(Unit) {
        lastTap = clock.value
        zoom.animateTo(1f, spring(dampingRatio = 0.86f, stiffness = 170f))
    }
    // the shouting never stops while you dig: more and faster the deeper you get
    LaunchedEffect(Unit) {
        while (true) {
            // quiet at first, then a crescendo: from one every few seconds to a flood near the end
            val p = (removed / kinds.size.toFloat()).coerceIn(0f, 1f)
            val i = if (phase == "bare") 1f else p.pow(1.6f)
            delay((2800 * (1f - i) + 70f).toLong())
            if ((phase == "dig" && p > 0.15f) || phase == "bare") repeat(1 + (i * 3.2f).toInt()) { shouts += stamps.next(r, clock.value, removed) }
            while (shouts.size > 220) shouts.removeAt(0)
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = LocalDensity.current.density
        val wPx = constraints.maxWidth.toFloat(); val hPx = constraints.maxHeight.toFloat()
        val discPx = DISC.value * density
        val statusTop = WindowInsets.statusBars.getTop(LocalDensity.current).toFloat()
        // the pile lives between Coach Zero's chat and the reply bar, and never grows into either
        val top = (statusTop + 250f * density); val bottom = hPx - 96f * density
        val target = Offset(wPx / 2f, (top + bottom) / 2f)
        val start = from?.center ?: target
        val startScale = (from?.width?.div(discPx * 2f) ?: 0.92f).coerceIn(0.4f, 1.2f)
        val maxGrow = 1f + 0.62f * (1f - 0.86f.pow(kinds.size))
        // the objects fill ~72% of their sprite: at full growth they must still fit the free band and the width
        val fullScale = minOf(wPx * 0.56f / discPx, (bottom - top) / (0.72f * 2f * discPx) / maxGrow, wPx * 1.02f / (0.72f * 2f * discPx) / maxGrow)
        fun center(): Offset = start + (target - start) * zoom.value
        fun scaleNow(): Float {
            val base = startScale + (fullScale - startScale) * zoom.value
            return base * (1f + 0.62f * grow.value)
        }
        stamps.bounds(wPx, hPx, density)

        fun tap() {
            val now = clock.value
            lastTap = now
            when (phase) {
                "bare" -> {
                    if (now < lockedUntil) return // still spamming from the last layer: not a real press
                    phase = "blast"
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    onFall()
                    scope.launch { flash.snapTo(1f); flash.animateTo(0f, tween(700)) }
                    scope.launch { grow.animateTo(0f, spring(dampingRatio = 0.6f, stiffness = 140f)) }
                    scope.launch {
                        blast.animateTo(1f, tween(1100, easing = FastOutSlowInEasing))
                        phase = "lost"
                    }
                    return
                }
                "dig" -> Unit
                else -> return
            }
            val k = kinds[layer]
            step++
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            scope.launch { shake.snapTo(1f); shake.animateTo(0f, tween(200)) }
            val c = center(); val side = discPx * 2f * scaleNow()
            // a puff of dust/chips from where you hit it
            repeat(7) {
                val a = r.nextFloat() * 2f * PI.toFloat()
                flying += Flying(null, c + Offset(cos(a), sin(a)) * side * 0.12f, 0f, now, cos(a) * (200f + r.nextFloat() * 380f) * density / 2.6f, -(300f + r.nextFloat() * 500f) * density / 2.6f, 0f, 0f,
                    if (r.nextBoolean()) Color(0xFFBDB7AE) else Color(0xFF6E6A66), (1.5f + r.nextFloat() * 2.5f) * density)
            }
            // few shouts on the first layers so you can see what you're tapping; a climax towards the end
            val p = (removed / kinds.size.toFloat()).coerceIn(0f, 1f)
            val i = p.pow(1.8f)
            if (r.nextFloat() < 0.06f + 0.94f * i) shouts += stamps.next(r, now, removed)
            repeat((i * 3.5f + r.nextFloat() * i).toInt()) { shouts += stamps.next(r, now, removed) }   // the flood at the end
            if (step >= k.steps) {
                // the whole thing is torn off: it jumps up, spins, and drops off the bottom of the screen
                view.performHapticFeedback(if (android.os.Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.LONG_PRESS)
                val img = sprites[sprites.name(k, (k.steps - 1).let { if (k == Kind.TAPE) 1 else it }, kinds.getOrNull(layer + 1))]
                flying += Flying(img, c, side, now, (r.nextFloat() - 0.5f) * 900f * density / 2.6f, -(1800f + r.nextFloat() * 300f) * density / 2.6f, (if (r.nextBoolean()) 1f else -1f) * (220f + r.nextFloat() * 200f), tiltOf(seed, layer))
                layer++; step = 0; removed++
                if (removed >= 2) shouts += stamps.next(r, now, removed, big = true)
                if (removed % 2 == 0) {
                    val recent = bubbles.takeLast(4).map { it.first }.toSet()
                    bubbles += (NnnPhrases.coach.filter { it !in recent }.shuffled(r).firstOrNull() ?: NnnPhrases.coachLine(r)) to now
                }
                scope.launch { grow.animateTo(1f - 0.86f.pow(removed), spring(dampingRatio = 0.55f, stiffness = 220f)) }
                scope.launch { red.animateTo((removed / kinds.size.toFloat()).coerceAtMost(1f), tween(900)) }
                if (layer >= kinds.size) { phase = "bare"; lockedUntil = now + 1.3f }
            }
        }

        // ---------------- the scene (one canvas: everything is a bitmap or a cheap primitive)
        Canvas(Modifier.fillMaxSize()) {
            val t = clock.value
            val z = zoom.value
            drawRect(Color.Black.copy(alpha = 0.95f * z))
            val c = center()
            // the screen bleeds red as you go deeper
            val rd = red.value
            drawRect(Brush.radialGradient(0f to Color(0xFF7A0012).copy(alpha = (0.18f + 0.5f * rd + 0.08f * sin(t * 3f) * rd) * z), 1f to Color(0xFF2A0006).copy(alpha = (0.1f + 0.55f * rd) * z), center = c, radius = hPx * 0.75f))
        }
        // the real button underneath, scaled along with the pile
        Box(
            Modifier.fillMaxSize().graphicsLayer {
                val s = scaleNow() * (1f - 0.12f * blast.value)
                scaleX = s; scaleY = s
                val c = center(); translationX = c.x - wPx / 2f; translationY = c.y - hPx / 2f
                alpha = 1f - blast.value
            },
            contentAlignment = Alignment.Center,
        ) { StreakButton(fire, ice, onPress = {}) }
        Canvas(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { tap() } }) {
            val t = clock.value
            val c = center()
            val side = discPx * 2f * scaleNow()
            val sh = shake.value
            if (phase == "dig") drawPile(sprites, kinds, layer, step, c, side, t, seed, Offset(sin(t * 90f) * sh * 4f * density, cos(t * 77f) * sh * 3f * density))
            // torn-off layers and chips flying with gravity
            val g = 9000f * density / 2.6f
            flying.removeAll { t - it.born > 2.2f }
            for (f in flying) {
                val dt = t - f.born
                val p = f.at + Offset(f.vx * dt, f.vy * dt + 0.5f * g * dt * dt)
                if (p.y > size.height + f.side) continue
                if (f.img != null) rotate(f.tilt + f.spin * dt, p) { sprite(f.img, p, f.side) }
                else drawCircle(f.color.copy(alpha = (1f - dt / 1.2f).coerceIn(0f, 1f)), f.r, p)
            }
            // Coach Zero's shouts, piling up; the shockwave flings them out
            val b = blast.value
            for (s in shouts) {
                val age = t - s.born
                val pop = (age / 0.14f).coerceIn(0f, 1f)
                val sc = 1.7f - 0.7f * (1f - (1f - pop) * (1f - pop))
                val away = if (phase == "blast" || phase == "lost") s.dir * (b * b * 2400f * density) else Offset.Zero
                val a = (pop * (1f - b)).coerceIn(0f, 1f)
                if (a <= 0f) continue
                val p = s.at + away
                rotate(s.rot + b * 60f * s.dir.x, p) {
                    val w = s.img.width * sc; val h = s.img.height * sc
                    drawImage(s.img, srcOffset = IntOffset.Zero, srcSize = IntSize(s.img.width, s.img.height), dstOffset = IntOffset((p.x - w / 2).roundToInt(), (p.y - h / 2).roundToInt()), dstSize = IntSize(w.roundToInt(), h.roundToInt()), alpha = a)
                }
            }
            if (phase == "blast" || phase == "lost") {
                val e = 1f - (1f - b) * (1f - b) * (1f - b)
                val rr = e * maxOf(size.width, size.height) * 1.2f
                drawCircle(Brush.radialGradient(0f to Color.White.copy(alpha = flash.value), 0.4f to Color(0xFFFFD0C0).copy(alpha = flash.value * 0.6f), 1f to Color.Transparent, center = c, radius = (0.12f + e) * size.width * 0.8f), (0.12f + e) * size.width * 0.8f, c)
                drawCircle(Color(0xFFFF5252).copy(alpha = (1f - b) * 0.85f), rr, c, style = Stroke(16f * density * (1f - b) + 1f))
                drawCircle(Color.White.copy(alpha = (1f - b) * 0.7f), rr * 0.86f, c, style = Stroke(5f * density * (1f - b) + 1f))
                val sr = Random(seed + 77)
                for (k in 0 until 48) {
                    val a = sr.nextFloat() * 2f * PI.toFloat()
                    val dist = e * maxOf(size.width, size.height) * (0.35f + sr.nextFloat() * 0.9f)
                    val dir = Offset(cos(a), sin(a))
                    drawLine((if (k % 3 == 0) Color(0xFFFF5252) else Color(0xFFFFE082)).copy(alpha = (1f - b).coerceIn(0f, 1f)), c + dir * dist * 0.75f, c + dir * dist, 3f * density * (1f - 0.6f * b))
                }
                drawRect(Color.White.copy(alpha = flash.value * flash.value * 0.7f))
            }
        }
        // the whisper: when you stop, something next to the button tells you what to do
        if (phase == "dig" || phase == "bare") Whisper(clock, { lastTap }, phase == "bare", Modifier.graphicsLayer {
            val c = center(); val s = scaleNow()
            translationX = (c.x + discPx * s * 0.62f).coerceAtMost(wPx - 150f * density)
            translationY = c.y + discPx * s * 0.45f
            alpha = zoom.value
        })
        // Coach Zero writing, and you can't answer
        Box(Modifier.fillMaxWidth().height(230.dp).graphicsLayer { alpha = zoom.value * (1f - blast.value) }.background(Brush.verticalGradient(0f to Color(0xF2050506), 0.72f to Color(0xD9050506), 1f to Color.Transparent)))
        Column(
            Modifier.fillMaxWidth().statusBarsPadding().padding(start = 14.dp, end = 14.dp, top = 46.dp, bottom = 10.dp).graphicsLayer {
                alpha = zoom.value * (1f - blast.value); translationY = -blast.value * 600f
            },
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) { bubbles.takeLast(3).forEach { (text, born) -> CoachBubble(text, born, clock) } }
        Row(
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 18.dp, start = 16.dp, end = 16.dp).fillMaxWidth().height(44.dp)
                .graphicsLayer { alpha = zoom.value * (1f - blast.value) }
                .clip(RoundedCornerShape(22.dp)).background(Color(0xFF151518)).border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(22.dp)).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("🔒", fontSize = 14.sp)
            Spacer(Modifier.width(8.dp))
            Text("Non puoi rispondere: dovresti uscire da qui.", color = Color.White.copy(alpha = 0.45f), fontSize = 13.sp)
        }
        if (phase == "dig") Text(
            "Esci", color = Color.White.copy(alpha = 0.55f), fontSize = 13.sp,
            modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top = 4.dp, end = 4.dp).clip(CircleShape)
                .clickable(remember { MutableInteractionSource() }, null) { onDone() }.padding(10.dp).graphicsLayer { alpha = zoom.value },
        )
        if (phase == "lost") NnnLost(day, onDone)
    }
}

/** The pile on the home: the real button under its protections, the tape's end swinging. */
@Composable
fun ProtectedButton(kinds: List<Kind>, seed: Int, fire: Int, ice: Int, modifier: Modifier = Modifier, onBounds: (Rect) -> Unit = {}, onTap: () -> Unit) {
    val sprites = rememberSprites(kinds, 0)
    val clock = rememberClock()
    Box(
        modifier.requiredSize(DISC * 1.84f).onGloballyPositioned { onBounds(it.boundsInRoot()) }.pointerInput(Unit) { detectTapGestures { onTap() } },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.requiredSize(340.dp, DISC + 56.dp).graphicsLayer { scaleX = 0.92f; scaleY = 0.92f }, contentAlignment = Alignment.Center) { StreakButton(fire, ice, onPress = {}) }
        Canvas(Modifier.requiredSize(DISC * 1.84f)) {
            drawPile(sprites, kinds, 0, 0, center, size.width, clock.value, seed)
        }
    }
}

@Composable
private fun CoachBubble(text: String, born: Float, clock: androidx.compose.runtime.State<Float>) {
    Row(
        Modifier.fillMaxWidth().graphicsLayer {
            val k = ((clock.value - born) / 0.35f).coerceIn(0f, 1f)
            val e = 1f - (1f - k) * (1f - k) * (1f - k)
            alpha = e; translationY = (1f - e) * -40f
        },
        verticalAlignment = Alignment.Top,
    ) {
        Image(painterResource(R.drawable.coach_zero), null, contentScale = ContentScale.Crop, modifier = Modifier.size(30.dp).clip(CircleShape))
        Spacer(Modifier.width(8.dp))
        Column(Modifier.clip(RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 16.dp)).background(Color(0xFF1F1F24)).padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text("Coach Zero", color = Color(0xFFFF5252), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(text, color = Color.White, fontSize = 13.sp, lineHeight = 17.sp)
        }
    }
}

/**
 * The whisper next to the button: after a few seconds of hesitation, dim italic words type themselves out
 * letter by letter, linger, and fade — like someone leaning in to your ear.
 */
@Composable
private fun Whisper(clock: androidx.compose.runtime.State<Float>, lastTap: () -> Float, bare: Boolean, modifier: Modifier) {
    var text by remember { mutableStateOf("") }
    var alpha by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(bare) {
        val lines = if (bare) NnnPhrases.whisperFinal else NnnPhrases.whisper
        var i = 0
        while (true) {
            delay(250)
            if (clock.value - lastTap() < 5f) { text = ""; alpha = 0f; continue }
            val line = lines[i++ % lines.size]
            alpha = 1f
            for (n in 1..line.length) {
                if (clock.value - lastTap() < 5f) break
                text = line.take(n)
                delay(if (line[n - 1] == ' ') 40L else 70L + (n * 37 % 50))
            }
            delay(1600)
            repeat(10) { alpha -= 0.1f; delay(45) }
            text = ""; delay(700)
        }
    }
    Box(modifier) {
        Text(
            text, color = Color(0xFFB9B4BE).copy(alpha = 0.78f * alpha.coerceIn(0f, 1f)), fontSize = 17.sp, lineHeight = 22.sp,
            fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic, letterSpacing = 0.6.sp, modifier = Modifier.widthIn(max = 150.dp),
        )
    }
}

/** Coach Zero's shouts, each pre-rendered once into a small bitmap (glow + outline + fill) so drawing them is free. */
private class StampMaker(ctx: android.content.Context) {
    private val tf = android.graphics.Typeface.create("sans-serif-condensed", android.graphics.Typeface.BOLD)
    private var w = 1000f; private var h = 2000f; private var d = 2.6f
    fun bounds(w: Float, h: Float, d: Float) { this.w = w; this.h = h; this.d = d }

    fun next(r: Random, now: Float, depth: Int, big: Boolean = false): Shout {
        val text = NnnPhrases.stamp(r)
        val size = (if (big) 34f else 16f + r.nextFloat() * (14f + depth)) * d
        val red = r.nextFloat() < 0.82f
        val img = render(text, size, red)
        val at = Offset(w * (0.08f + 0.84f * r.nextFloat()), h * (0.22f + 0.66f * r.nextFloat()))
        val ang = kotlin.math.atan2(at.y - h * 0.53f, at.x - w / 2f)
        return Shout(img, at, (r.nextFloat() - 0.5f) * 26f, now, Offset(cos(ang), sin(ang)))
    }

    private fun render(text: String, size: Float, red: Boolean): ImageBitmap {
        val col = if (red) android.graphics.Color.rgb(255, 23, 68) else android.graphics.Color.WHITE
        val fill = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = col; textSize = size; typeface = tf; letterSpacing = 0.04f; textAlign = android.graphics.Paint.Align.CENTER }
        val tw = fill.measureText(text)
        val pad = size * 0.6f
        val bw = (tw + pad * 2).toInt().coerceAtLeast(1); val bh = (size * 1.2f + pad * 2).toInt()
        val bmp = android.graphics.Bitmap.createBitmap(bw, bh, android.graphics.Bitmap.Config.ARGB_8888)
        val c = android.graphics.Canvas(bmp)
        val x = bw / 2f; val y = bh / 2f + size * 0.35f
        c.drawText(text, x, y, android.graphics.Paint(fill).apply { alpha = 110; maskFilter = android.graphics.BlurMaskFilter(size * 0.35f, android.graphics.BlurMaskFilter.Blur.NORMAL) })
        c.drawText(text, x, y, android.graphics.Paint(fill).apply { color = android.graphics.Color.BLACK; alpha = 220; style = android.graphics.Paint.Style.STROKE; strokeWidth = size * 0.12f; strokeJoin = android.graphics.Paint.Join.ROUND })
        c.drawText(text, x, y, fill)
        bmp.prepareToDraw()
        return bmp.asImageBitmap()
    }
}
