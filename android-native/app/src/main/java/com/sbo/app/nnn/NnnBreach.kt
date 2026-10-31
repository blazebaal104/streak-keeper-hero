package com.sbo.app.nnn

import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sbo.app.R
import com.sbo.app.ui.StreakButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/** Side of the square the protections are drawn in (the 192 dp disc sits in the middle). */
val NNN_AREA: Dp = 260.dp

/** Per-layer tilt so the pile never looks stacked by a machine. */
private fun tiltOf(seed: Int, i: Int) = ((seed * 31 + i * 97) % 23 - 11).toFloat()

/**
 * Draws the protection pile: the next layers under the current one, the police tape at the bottom (its loose
 * end flapping from under everything) and the current layer at [step]. Only the top few are drawn: the rest
 * are hidden under them anyway.
 */
fun DrawScope.drawPile(kinds: List<Kind>, layer: Int, step: Int, c: Offset, u: Float, tex: Textures, t: Float, seed: Int, shake: Float = 0f) {
    if (layer >= kinds.size) return
    val last = kinds.lastIndex
    val visible = (layer..minOf(layer + 2, last)).toMutableList()
    if (last !in visible) visible += last
    for (i in visible.sortedDescending()) {
        val k = kinds[i]
        val st = if (i == layer) step else 0
        val jitter = if (i == layer) Offset(sin(t * 90f) * shake * 3f * u, cos(t * 77f) * shake * 2f * u) else Offset.Zero
        rotate(if (k == Kind.CRIME_TAPE) 0f else tiltOf(seed, i), c) {
            drawProtection(k, st, c + jitter, u, tex, t, seed + i * 13)
        }
    }
}

private class Debris(val kind: Kind?, val piece: Int, val step: Int, val at: Offset, val born: Float, val vx: Float, val vy: Float, val spin: Float, val tilt: Float, val seed: Int)
private class Stamp(val text: String, val at: Offset, val rot: Float, val size: Float, val born: Float, val dir: Offset, val red: Boolean)

/**
 * The breach: tapping the protected button zooms it in until it's the only thing on screen. Every tap is one
 * real action on the outermost protection (a nail, a dial number, a bolt…); when it's off it's blown away,
 * jumping up and falling off screen. Every 2–3 layers the button grows (less each time), Coach Zero's red
 * stamps and messages pile up, and after the police tape the bare button waits for the final press: a
 * shockwave blows every word away and the button shrinks back to normal. [onFall] then records the event.
 */
@Composable
fun NnnBreach(kinds: List<Kind>, seed: Int, fire: Int, ice: Int, onFall: () -> Unit, onExit: () -> Unit, startLayer: Int = 0) {
    val view = LocalView.current
    val tex = rememberTextures()
    val clock = rememberClock()
    val scope = rememberCoroutineScope()
    var layer by remember { mutableIntStateOf(startLayer) }
    var step by remember { mutableIntStateOf(0) }
    var removed by remember { mutableIntStateOf(0) }
    var growthLevel by remember { mutableIntStateOf(0) }
    var lastTap by remember { mutableFloatStateOf(0f) }
    var phase by remember { mutableStateOf("dig") } // dig | blast | done
    val debris = remember { mutableStateListOf<Debris>() }
    val stamps = remember { mutableStateListOf<Stamp>() }
    val bubbles = remember { mutableStateListOf<Pair<String, Float>>() }
    val zoom = remember { Animatable(0f) }      // 0 = home size, 1 = filling the screen
    val grow = remember { Animatable(0f) }      // growth on top of the zoom
    val shake = remember { Animatable(0f) }
    val blast = remember { Animatable(0f) }
    val flash = remember { Animatable(0f) }
    val tapTimes = remember { ArrayList<Float>() }
    val r = remember { Random(seed * 7 + 3) }
    val growEvery = remember { List(20) { if ((seed + it) % 3 == 0) 3 else 2 } }
    var nextGrowAt by remember { mutableIntStateOf(growEvery[0]) }
    BackHandler(enabled = phase == "dig") { onExit() }
    LaunchedEffect(Unit) {
        lastTap = clock.value
        zoom.animateTo(1f, spring(dampingRatio = 0.78f, stiffness = 140f))
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.0f))) {
        val wPx = constraints.maxWidth.toFloat()
        val hPx = constraints.maxHeight.toFloat()
        val density = androidx.compose.ui.platform.LocalDensity.current.density
        val area = NNN_AREA.value * density
        val homeScale = 0.92f
        val fullScale = (wPx * 0.62f / area).coerceAtMost(hPx * 0.36f / area) // leaves room to grow to ~full width
        fun scaleNow(): Float {
            val z = zoom.value
            val g = grow.value
            val base = homeScale + (fullScale - homeScale) * z
            return base * (1f + 0.55f * g) * (1f - 0.35f * blast.value.coerceIn(0f, 1f) * if (phase == "blast") 1f else 0f)
        }
        val center = Offset(wPx / 2f, hPx * 0.52f)

        fun spawnStamp(now: Float, big: Boolean = false) {
            if (stamps.size > 70) stamps.removeAt(0)
            val ang = r.nextFloat() * 2 * PI.toFloat()
            val at = Offset(wPx * (0.06f + 0.88f * r.nextFloat()), hPx * (0.08f + 0.86f * r.nextFloat()))
            stamps += Stamp(NnnPhrases.stamp(r), at, (r.nextFloat() - 0.5f) * 28f, (if (big) 34f else 18f + r.nextFloat() * 18f) * density, now, Offset(cos(ang), sin(ang)), r.nextFloat() < 0.85f)
        }

        fun tap() {
            val now = clock.value
            lastTap = now
            tapTimes += now; while (tapTimes.isNotEmpty() && now - tapTimes.first() > 2f) tapTimes.removeAt(0)
            if (phase != "dig") return
            if (layer >= kinds.size) { // the bare button: final press
                phase = "blast"
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                scope.launch { flash.snapTo(1f); flash.animateTo(0f, tween(900)) }
                scope.launch {
                    blast.animateTo(1f, tween(1400, easing = FastOutSlowInEasing))
                    phase = "done"
                    delay(250)
                    onFall()
                }
                scope.launch { grow.animateTo(0f, spring(dampingRatio = 0.55f, stiffness = 120f)) }
                scope.launch { zoom.animateTo(0f, tween(1300, easing = FastOutSlowInEasing)) }
                return
            }
            val k = kinds[layer]
            step++
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            scope.launch { shake.snapTo(1f); shake.animateTo(0f, tween(220)) }
            val sc = scaleNow()
            val u = area / 100f * sc
            // small parts flying off on intermediate steps
            smallPiece(k, step)?.let { (piece, off) ->
                val n = if (piece == PIECE_SHARD) 9 else 1
                repeat(n) { j ->
                    debris += Debris(null, piece, 0, center + Offset(off.x * u, off.y * u), now, (r.nextFloat() - 0.5f) * 900f * density / 2.6f, -(500f + r.nextFloat() * 500f) * density / 2.6f, (r.nextFloat() - 0.5f) * 900f, r.nextFloat() * 360f, seed + j)
                }
            }
            // spamming fills the screen with stamps
            val rate = tapTimes.size / 2f
            if (r.nextFloat() < (rate / 7f).coerceIn(0.08f, 0.9f)) spawnStamp(now)
            if (step >= k.steps) {
                // the whole layer is blown off: jumps up, spins, falls off screen
                view.performHapticFeedback(if (android.os.Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.LONG_PRESS)
                debris += Debris(k, -1, k.steps - 1, center, now, (r.nextFloat() - 0.5f) * 700f * density / 2.6f, -(900f + r.nextFloat() * 400f) * density / 2.6f, (r.nextFloat() - 0.5f) * 260f, tiltOf(seed, layer), seed + layer * 13)
                layer++; step = 0; removed++
                spawnStamp(now, big = true)
                if (removed % 2 == 0) bubbles += NnnPhrases.coachLine(r) to now
                if (removed >= nextGrowAt) {
                    growthLevel++
                    nextGrowAt += growEvery[growthLevel.coerceAtMost(growEvery.lastIndex)]
                    // big jumps at first, smaller and smaller: 1 - 0.68^n
                    scope.launch { grow.animateTo(1f - 0.68f.pow(growthLevel), spring(dampingRatio = 0.62f, stiffness = 260f)) }
                }
            }
        }

        // backdrop: darkness swallowing the app, red pulse
        Canvas(Modifier.fillMaxSize()) {
            val t = clock.value
            val z = zoom.value
            drawRect(Color.Black.copy(alpha = 0.88f * z))
            drawRect(Brush.radialGradient(0f to Color(0xFF5A0010).copy(alpha = (0.35f + 0.12f * sin(t * 3f)) * z), 1f to Color.Transparent, center = center, radius = hPx * 0.6f))
        }
        // the real button, scaled with everything else
        Box(
            Modifier.fillMaxSize().graphicsLayer {
                val s = scaleNow()
                scaleX = s / homeScale * 0.92f; scaleY = s / homeScale * 0.92f
                translationY = center.y - hPx / 2f
            },
            contentAlignment = Alignment.Center,
        ) { StreakButton(fire, ice, onPress = {}) }
        // protections, debris, stamps, shockwave
        Canvas(
            Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { tap() } },
        ) {
            val t = clock.value
            val s = scaleNow()
            val u = area / 100f * s
            if (phase == "dig") drawPile(kinds, layer, step, center, u, tex, t, seed, shake.value)
            // flying debris
            val g = 2600f * density / 2.6f
            debris.removeAll { t - it.born > 3.5f }
            for (d in debris) {
                val dt = t - d.born
                val p = d.at + Offset(d.vx * dt, d.vy * dt + 0.5f * g * dt * dt)
                if (p.y > size.height + 400f * density) continue
                translate(p.x - d.at.x, p.y - d.at.y) {
                    rotate(d.tilt + d.spin * dt, d.at) {
                        if (d.kind != null) drawProtection(d.kind, d.step, d.at, u, tex, t, d.seed)
                        else drawPiece(d.piece, d.at, u, tex, d.seed)
                    }
                }
            }
            // red stamps piling up (blown outwards by the shockwave)
            val b = blast.value
            for (st in stamps) {
                val age = t - st.born
                val pop = (age / 0.16f).coerceIn(0f, 1f)
                val sc = 1.9f - 0.9f * (1f - (1f - pop) * (1f - pop))
                val away = if (phase != "dig") st.dir * (b * b * 1600f * density) + Offset(0f, b * b * 900f * density) else Offset.Zero
                val a = (pop * (1f - b)).coerceIn(0f, 1f)
                if (a <= 0f) continue
                rotate(st.rot + b * 220f * st.dir.x, st.at + away) {
                    stampText(st.text, st.at + away, st.size * sc, a, st.red)
                }
            }
            // final press: shockwave rings + red flash
            if (phase != "dig") {
                val rr = b * maxOf(size.width, size.height) * 1.1f
                drawCircle(Color(0xFFFF5252).copy(alpha = (1f - b) * 0.9f), rr, center, style = Stroke(26f * density * (1f - b) + 2f))
                drawCircle(Color.White.copy(alpha = (1f - b) * 0.8f), rr * 0.82f, center, style = Stroke(8f * density * (1f - b) + 1f))
                drawRect(Color(0xFFFF1744).copy(alpha = flash.value * 0.55f))
            }
        }
        // Coach Zero writing, and you can't answer
        Column(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 14.dp, vertical = 10.dp).graphicsLayer {
                alpha = zoom.value * (1f - blast.value); translationY = -blast.value * 600f
            },
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            bubbles.takeLast(3).forEach { (text, born) -> CoachBubble(text, born, clock) }
        }
        // hint after 5 s of doubt, and the locked reply bar
        Column(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 18.dp).graphicsLayer { alpha = zoom.value * (1f - blast.value) }, horizontalAlignment = Alignment.CenterHorizontally) {
            Canvas(Modifier.fillMaxWidth().height(40.dp)) {
                val t = clock.value
                val idle = t - lastTap
                if (idle > 5f && phase == "dig") {
                    val k = ((idle - 5f) / 0.6f).coerceIn(0f, 1f) * (0.65f + 0.35f * sin(t * 4f))
                    val txt = if (layer >= kinds.size) "ULTIMO TOCCO. PENSACI BENE." else "PREMILO. LO SAI CHE VUOI FARLO."
                    neonText(txt, Offset(size.width / 2, size.height / 2), 15f * density, Color.White, k, density, spacing = 0.12f)
                }
            }
            Row(
                Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(44.dp).clip(RoundedCornerShape(22.dp)).background(Color(0xFF151518)).border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(22.dp)).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("🔒", fontSize = 14.sp)
                Spacer(Modifier.width(8.dp))
                Text("Non puoi rispondere: dovresti uscire da qui.", color = Color.White.copy(alpha = 0.45f), fontSize = 13.sp)
            }
        }
        // escape hatch while digging
        if (phase == "dig") Text(
            "Esci", color = Color.White.copy(alpha = 0.55f), fontSize = 13.sp,
            modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top = 4.dp, end = 4.dp).clip(CircleShape)
                .clickable(remember { MutableInteractionSource() }, null) { onExit() }.padding(10.dp).graphicsLayer { alpha = zoom.value },
        )
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

/** A red rubber-stamp shout: heavy condensed caps with a dark outline and a soft glow. */
fun DrawScope.stampText(text: String, at: Offset, sizePx: Float, alpha: Float, red: Boolean) {
    drawIntoCanvas { c ->
        val tf = android.graphics.Typeface.create("sans-serif-condensed", android.graphics.Typeface.BOLD)
        val col = if (red) android.graphics.Color.rgb(255, 23, 68) else android.graphics.Color.WHITE
        val glow = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = col; this.alpha = (110 * alpha).toInt(); textSize = sizePx; textAlign = android.graphics.Paint.Align.CENTER; typeface = tf
            maskFilter = android.graphics.BlurMaskFilter(sizePx * 0.35f, android.graphics.BlurMaskFilter.Blur.NORMAL); letterSpacing = 0.04f
        }
        val outline = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.BLACK; this.alpha = (220 * alpha).toInt(); textSize = sizePx; textAlign = android.graphics.Paint.Align.CENTER; typeface = tf
            style = android.graphics.Paint.Style.STROKE; strokeWidth = sizePx * 0.12f; strokeJoin = android.graphics.Paint.Join.ROUND; letterSpacing = 0.04f
        }
        val fill = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = col; this.alpha = (255 * alpha).toInt(); textSize = sizePx; textAlign = android.graphics.Paint.Align.CENTER; typeface = tf; letterSpacing = 0.04f
        }
        val y = at.y + sizePx * 0.35f
        c.nativeCanvas.drawText(text, at.x, y, glow)
        c.nativeCanvas.drawText(text, at.x, y, outline)
        c.nativeCanvas.drawText(text, at.x, y, fill)
    }
}

// ---------------------------------------------------------------- small flying parts

const val PIECE_NAIL = 0
const val PIECE_BOLT = 1
const val PIECE_SHARD = 2
const val PIECE_TIE = 3
const val PIECE_BRICK = 4
const val PIECE_BAG = 5
const val PIECE_TAPE = 6
const val PIECE_POLICE = 7

/** Which small part flies off at intermediate [step] of [k] (offset in units from the centre), if any. */
private fun smallPiece(k: Kind, step: Int): Pair<Int, Offset>? = when (k) {
    Kind.BEAM -> if (step < 3) PIECE_NAIL to Offset(if (step == 1) -42f else 42f, 0f) else null
    Kind.STEEL_PLATE -> if (step <= 4) PIECE_BOLT to listOf(Offset(-40f, -40f), Offset(40f, -40f), Offset(40f, 40f), Offset(-40f, 40f))[step - 1] else null
    Kind.GLASS_CASE -> if (step == 2) PIECE_SHARD to Offset(0f, 4f) else null
    Kind.ZIP_TIES -> if (step < 3) PIECE_TIE to Offset(40f, 0f) else null
    Kind.BRICKS -> if (step < 4) PIECE_BRICK to listOf(Offset(-10f, -10f), Offset(18f, -24f), Offset(4f, 16f))[step - 1] else null
    Kind.SANDBAGS -> if (step < 3) PIECE_BAG to Offset(0f, -14f) else null
    Kind.DUCT_TAPE -> if (step < 3) PIECE_TAPE to Offset(0f, 0f) else null
    Kind.CRIME_TAPE -> if (step < 3) PIECE_POLICE to Offset(0f, 0f) else null
    else -> null
}

private fun DrawScope.drawPiece(piece: Int, c: Offset, u: Float, tex: Textures, seed: Int) {
    when (piece) {
        PIECE_NAIL -> {
            drawLine(chrome(androidx.compose.ui.geometry.Rect(c, 12f * u), 0f), c, c + Offset(0f, 22f * u), 1.6f * u, StrokeCap.Round)
            nail(c, 3.6f * u, false)
        }
        PIECE_BOLT -> {
            drawRect(chrome(androidx.compose.ui.geometry.Rect(c, 10f * u), 90f), c + Offset(-2f * u, 0f), Size(4f * u, 16f * u))
            for (k in 0 until 6) drawLine(Color.Black.copy(alpha = 0.4f), c + Offset(-2f * u, (2f + k * 2.4f) * u), c + Offset(2f * u, (3.2f + k * 2.4f) * u), 0.5f * u)
            bolt(c, 5.2f * u)
        }
        PIECE_SHARD -> {
            val r = Random(seed)
            val p = Path().apply { moveTo(c.x, c.y); lineTo(c.x + (6f + r.nextFloat() * 10f) * u, c.y + r.nextFloat() * 6f * u); lineTo(c.x + r.nextFloat() * 6f * u, c.y + (8f + r.nextFloat() * 10f) * u); close() }
            drawPath(p, Color(0x88D7F3FF)); drawPath(p, Color.White.copy(alpha = 0.8f), style = Stroke(0.5f * u))
        }
        PIECE_TIE -> {
            drawLine(Color(0xFF151618), c, c + Offset(28f * u, 6f * u), 4.6f * u, StrokeCap.Butt)
            drawRoundRect(Color(0xFF0E0F10), c + Offset(-5f * u, -4f * u), Size(10f * u, 8f * u))
        }
        PIECE_BRICK -> {
            val b = androidx.compose.ui.geometry.Rect(c.x - 12f * u, c.y - 6f * u, c.x + 12f * u, c.y + 6f * u)
            solid(Path().apply { addRect(b) }, tex.brush("red_brick", 120f * u, 0f, Offset(seed * 9f, 0f), Color(0xFF8E3B2A)), b, 0.8f * u)
        }
        PIECE_BAG -> {
            val p = Path().apply { addOval(androidx.compose.ui.geometry.Rect(c.x - 34f * u, c.y - 18f * u, c.x + 34f * u, c.y + 18f * u)) }
            solid(p, tex.brush("hessian_230", 34f * u, 0f, Offset.Zero, Color(0xFFB09A72)), androidx.compose.ui.geometry.Rect(c.x - 34f * u, c.y - 18f * u, c.x + 34f * u, c.y + 18f * u), 2f * u, tint = Color(0xFFE2CFA6))
        }
        PIECE_TAPE -> {
            val b = androidx.compose.ui.geometry.Rect(c.x - 60f * u, c.y - 9f * u, c.x + 60f * u, c.y + 9f * u)
            drawRect(Brush.verticalGradient(0f to Color(0xFFC3C8CD), 1f to Color(0xFF8E949A), startY = b.top, endY = b.bottom), b.topLeft, b.size)
        }
        PIECE_POLICE -> tapeStrip(c + Offset(-70f * u, 0f), c + Offset(70f * u, 0f), 14f * u, u, seed.toFloat())
    }
}

/** The protected button as seen on the NNN home: the pile on top of the real button, slightly breathing. */
@Composable
fun ProtectedButton(kinds: List<Kind>, seed: Int, fire: Int, ice: Int, modifier: Modifier = Modifier, onTap: () -> Unit) {
    val tex = rememberTextures()
    val clock = rememberClock()
    Box(modifier.requiredSize(NNN_AREA + 60.dp).pointerInput(Unit) { detectTapGestures { onTap() } }, contentAlignment = Alignment.Center) {
        Box(Modifier.graphicsLayer { scaleX = 0.92f; scaleY = 0.92f }, contentAlignment = Alignment.Center) { StreakButton(fire, ice, onPress = {}) }
        Canvas(Modifier.requiredSize(NNN_AREA + 60.dp)) {
            val u = NNN_AREA.toPx() / 100f * 0.92f
            drawPile(kinds, 0, 0, center, u, tex, clock.value, seed)
        }
    }
}
