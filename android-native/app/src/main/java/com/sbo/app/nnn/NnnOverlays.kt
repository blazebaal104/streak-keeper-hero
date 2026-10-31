package com.sbo.app.nnn

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sbo.app.R
import com.sbo.app.logic.Badges
import com.sbo.app.ui.BadgeArt
import com.sbo.app.ui.SboButton
import com.sbo.app.ui.SpaceGrotesk
import com.sbo.app.ui.TitleBadge
import com.sbo.app.ui.Titles
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Coach Zero's chat bubbles revealed one by one, like he's typing them. */
@Composable
private fun CoachLines(lines: List<String>, angry: Boolean, startDelay: Long = 600) {
    var shown by remember(lines) { mutableIntStateOf(0) }
    LaunchedEffect(lines) { delay(startDelay); while (shown < lines.size) { delay(if (shown == 0) 300 else 1100); shown++ } }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        lines.take(shown).forEach { l ->
            val a = remember { Animatable(0f) }
            LaunchedEffect(Unit) { a.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = 300f)) }
            Row(Modifier.graphicsLayer { alpha = a.value.coerceIn(0f, 1f); translationY = (1f - a.value) * 30f }, verticalAlignment = Alignment.Top) {
                Image(painterResource(R.drawable.coach_zero), null, contentScale = ContentScale.Crop, modifier = Modifier.size(34.dp).clip(CircleShape))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.clip(RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 18.dp)).background(Color(0xFF1C1C21)).padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Text("Coach Zero", color = if (angry) Color(0xFFFF5252) else Color(0xFFFFD54F), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(l, color = Color.White, fontSize = 15.sp, lineHeight = 21.sp)
                }
            }
        }
    }
}

/** 1 November, first open: the challenge starts. */
@Composable
fun NnnKickoff(onAccept: () -> Unit) {
    BackHandler { onAccept() }
    val clock = rememberClock()
    val enter = remember { Animatable(0f) }
    LaunchedEffect(Unit) { enter.animateTo(1f, tween(700, easing = FastOutSlowInEasing)) }
    Box(Modifier.fillMaxSize().graphicsLayer { alpha = enter.value }.background(Color(0xF2050507)).clickable(remember { MutableInteractionSource() }, null) {}) {
        Canvas(Modifier.fillMaxSize()) {
            val t = clock.value
            drawRect(Brush.radialGradient(0f to Color(0xFF4A0010).copy(alpha = 0.6f), 1f to Color.Transparent, center = Offset(size.width / 2, size.height * 0.2f), radius = size.height * 0.6f))
            neonText("NNN", Offset(size.width / 2, size.height * 0.15f), size.width * 0.34f, Color(0xFFFF1744), neon(t, 1.1f), density)
            neonText("1 – 30 NOVEMBRE", Offset(size.width / 2, size.height * 0.15f + size.width * 0.2f), size.width * 0.06f, Color(0xFFFFB3B3), neon(t, 3.3f) * 0.8f, density, spacing = 0.3f)
        }
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(240.dp))
            val kinds = remember { Nnn.sequence("kickoff", java.time.LocalDate.now().year) }
            Box(Modifier.graphicsLayer { val k = enter.value; scaleX = 0.62f + 0.08f * k; scaleY = 0.62f + 0.08f * k; alpha = k }.height(210.dp), contentAlignment = Alignment.Center) {
                ProtectedButton(kinds, 17, 0, 0) {}
            }
            Spacer(Modifier.weight(1f))
            CoachLines(NnnPhrases.kickoff, angry = false)
            Spacer(Modifier.height(20.dp))
            SboButton("Accetto la sfida 💪", onClick = onAccept)
            Spacer(Modifier.height(18.dp))
        }
    }
}

/** Right after the fall: the verdict and Coach Zero's reaction, harsher the sooner you broke. */
@Composable
fun NnnFallen(day: Int, onClose: () -> Unit) {
    BackHandler { onClose() }
    val lasted = (day - 1).coerceAtLeast(0)
    val clock = rememberClock()
    val enter = remember { Animatable(0f) }
    LaunchedEffect(Unit) { enter.animateTo(1f, tween(500)) }
    Box(Modifier.fillMaxSize().graphicsLayer { alpha = enter.value }.background(Color(0xF5070000)).clickable(remember { MutableInteractionSource() }, null) {}) {
        Canvas(Modifier.fillMaxSize()) {
            val t = clock.value
            drawRect(Brush.radialGradient(0f to Color(0xFF700010).copy(alpha = 0.55f + 0.1f * sin(t * 2f)), 1f to Color.Transparent, center = Offset(size.width / 2, size.height * 0.18f), radius = size.height * 0.55f))
            neonText("NNN PERSA", Offset(size.width / 2, size.height * 0.12f), size.width * 0.15f, Color(0xFFFF1744), neon(t, 2.2f), density)
            val mid = Offset(size.width / 2, size.height * 0.33f)
            val uu = size.width / 260f
            tapeStrip(Offset(-20f * uu, mid.y - 70f * uu), Offset(size.width + 20f * uu, mid.y + 40f * uu), 26f * uu, uu, t * 0.2f)
            tapeStrip(Offset(-20f * uu, mid.y + 50f * uu), Offset(size.width + 20f * uu, mid.y - 60f * uu), 26f * uu, uu, t * 0.2f + 0.5f)
            flappingTape(Offset(size.width - 6f * uu, mid.y - 64f * uu), uu, t, 5, 90f, 12f)
            neonText(if (lasted == 0) "IL PRIMO GIORNO" else "DOPO $lasted ${if (lasted == 1) "GIORNO" else "GIORNI"}", Offset(size.width / 2, size.height * 0.12f + size.width * 0.13f), size.width * 0.06f, Color(0xFFFFCDD2), 0.85f, density, spacing = 0.25f)
        }
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp), verticalArrangement = Arrangement.Bottom) {
            CoachLines(NnnPhrases.reaction(lasted), angry = lasted < 15 || lasted >= 29)
            Spacer(Modifier.height(14.dp))
            Text("Tutto il gruppo è stato avvisato.", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            SboButton("Ok…", variant = "secondary", onClick = onClose)
            Spacer(Modifier.height(18.dp))
        }
    }
}

/** 1 December, for whoever made it: a party. Confetti, fireworks, golden drops, the medal and the title. */
@Composable
fun NnnVictory(year: Int, onClose: () -> Unit) {
    BackHandler { onClose() }
    val clock = rememberClock()
    val medal = remember { Animatable(0f) }
    val title = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(900); medal.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 90f))
        delay(300); title.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 160f))
    }
    val confetti = remember { val r = Random(year); List(140) { Triple(r.nextFloat(), r.nextFloat(), r.nextInt(6)) } }
    Box(Modifier.fillMaxSize().background(Color(0xFF07040F)).clickable(remember { MutableInteractionSource() }, null) {}) {
        Canvas(Modifier.fillMaxSize()) {
            val t = clock.value
            // purple/gold glow
            drawRect(Brush.radialGradient(0f to Color(0xFF6D28D9).copy(alpha = 0.55f), 0.5f to Color(0xFF3B0764).copy(alpha = 0.25f), 1f to Color.Transparent, center = Offset(size.width / 2, size.height * 0.38f), radius = size.height * 0.7f))
            // rotating god rays behind the medal
            val mc = Offset(size.width / 2, size.height * 0.36f)
            rotate(t * 12f, mc) {
                for (i in 0 until 14) {
                    val a = i / 14f * 2 * PI.toFloat()
                    val p = Path().apply { moveTo(mc.x, mc.y); lineTo(mc.x + cos(a - 0.07f) * size.height, mc.y + sin(a - 0.07f) * size.height); lineTo(mc.x + cos(a + 0.07f) * size.height, mc.y + sin(a + 0.07f) * size.height); close() }
                    drawPath(p, Brush.radialGradient(0f to Color(0xFFFFE082).copy(alpha = 0.22f * medal.value), 1f to Color.Transparent, center = mc, radius = size.height * 0.7f))
                }
            }
            fireworks(t, size.width, size.height)
            // golden drops raining + confetti tumbling
            confetti.forEachIndexed { i, (x, ph, kind) ->
                val speed = 0.12f + (i % 7) * 0.025f
                val y = ((ph + t * speed) % 1.1f) * size.height * 1.05f - 20f * density
                val xx = x * size.width + sin(t * (1.5f + i % 5) + i) * 18f * density
                val cols = listOf(Color(0xFFFFD54F), Color(0xFFC084FC), Color(0xFFFF4081), Color(0xFF40C4FF), Color(0xFF69F0AE), Color.White)
                if (kind == 0) { // golden drop
                    val w = 9f * density
                    val drop = Path().apply { moveTo(xx, y); cubicTo(xx, y + w * 0.4f, xx + w * 0.45f, y + w * 0.7f, xx + w * 0.45f, y + w); cubicTo(xx + w * 0.45f, y + w * 1.35f, xx - w * 0.45f, y + w * 1.35f, xx - w * 0.45f, y + w); cubicTo(xx - w * 0.45f, y + w * 0.7f, xx, y + w * 0.4f, xx, y); close() }
                    drawPath(drop, Brush.linearGradient(0f to Color(0xFFFFF3B0), 1f to Color(0xFFE0A800), start = Offset(xx - w, y), end = Offset(xx + w, y + w * 1.4f)))
                } else rotate(t * (200f + i * 7) % 360f, Offset(xx, y)) {
                    val flip = kotlin.math.abs(sin(t * 4f + i))
                    drawRect(cols[kind], Offset(xx - 4f * density, y - 2.5f * density * flip), Size(8f * density, 5f * density * flip.coerceAtLeast(0.15f)))
                }
            }
            neonText("CE L'HAI FATTA", Offset(size.width / 2, size.height * 0.1f), size.width * 0.105f, Color(0xFFFFD54F), neon(t, 0.6f), density, spacing = 0.08f)
            neonText("NNN $year COMPLETATA", Offset(size.width / 2, size.height * 0.1f + size.width * 0.1f), size.width * 0.05f, Color(0xFFE9D5FF), 0.9f, density, spacing = 0.25f)
        }
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(168.dp))
            Box(Modifier.graphicsLayer { val k = medal.value; scaleX = 0.2f + 0.8f * k; scaleY = 0.2f + 0.8f * k; alpha = k.coerceIn(0f, 1f); rotationY = (1f - k) * 540f }) {
                BadgeArt(Badges.nnn(year), side = 150.dp)
            }
            Spacer(Modifier.height(14.dp))
            Box(Modifier.graphicsLayer { val k = title.value; alpha = k.coerceIn(0f, 1f); scaleX = 0.6f + 0.4f * k; scaleY = 0.6f + 0.4f * k }) {
                Box(Modifier.clip(RoundedCornerShape(20.dp)).background(Color(0xCC12081F)).border(1.dp, Color(0xFFC084FC).copy(alpha = 0.6f), RoundedCornerShape(20.dp)).padding(horizontal = 16.dp, vertical = 8.dp)) {
                    TitleBadge(Titles.nnn(year), "md")
                }
            }
            Spacer(Modifier.weight(1f))
            CoachLines(NnnPhrases.victory(year), angry = false, startDelay = 2000)
            Spacer(Modifier.height(14.dp))
            SboButton("Grazie, Coach 🏆", onClick = onClose)
            Spacer(Modifier.height(18.dp))
        }
    }
}

/** Fireworks: bursts of sparks on a loop, each with its own colour, trail and gravity. */
private fun DrawScope.fireworks(t: Float, w: Float, h: Float) {
    val d = density
    for (k in 0 until 5) {
        val period = 2.6f + k * 0.37f
        val local = (t + k * 0.9f) % period
        val n = ((t + k * 0.9f) / period).toInt()
        val r = Random(k * 1000 + n)
        val c = Offset(w * (0.15f + 0.7f * r.nextFloat()), h * (0.12f + 0.35f * r.nextFloat()))
        val col = listOf(Color(0xFFFFD54F), Color(0xFFE040FB), Color(0xFF40C4FF), Color(0xFFFF5252), Color(0xFF69F0AE))[r.nextInt(5)]
        if (local < 0.5f) { // rocket going up
            val y = h - (h - c.y) * (local / 0.5f)
            drawCircle(Color.White, 2f * d, Offset(c.x, y))
            drawLine(col.copy(alpha = 0.5f), Offset(c.x, y), Offset(c.x, y + 30f * d), 1.5f * d)
            continue
        }
        val e = local - 0.5f
        val fade = (1f - e / (period - 0.5f)).coerceIn(0f, 1f)
        for (i in 0 until 36) {
            val a = i / 36f * 2 * PI.toFloat() + r.nextFloat() * 0.1f
            val sp = (130f + r.nextFloat() * 60f) * d
            val p = c + Offset(cos(a) * sp * e, sin(a) * sp * e + 60f * d * e * e)
            val tail = c + Offset(cos(a) * sp * (e - 0.08f).coerceAtLeast(0f), sin(a) * sp * (e - 0.08f).coerceAtLeast(0f) + 60f * d * (e - 0.08f).coerceAtLeast(0f).let { it * it })
            drawLine(col.copy(alpha = fade * 0.6f), tail, p, 1.6f * d)
            drawCircle(Color.White.copy(alpha = fade), 1.6f * d, p)
            drawCircle(col.copy(alpha = fade * 0.5f), 4f * d, p)
        }
    }
}

/** The NNN medal emblem: a closed golden padlock (the button stayed locked all month) inside a laurel wreath. */
@Composable
fun NnnEmblem(modifier: Modifier = Modifier) {
    val clock = rememberClock()
    Canvas(modifier) {
        val t = clock.value
        val c = center
        val s = size.minDimension / 100f
        // laurel wreath
        for (side in listOf(-1f, 1f)) {
            for (i in 0 until 8) {
                val a = PI.toFloat() * (0.62f + i * 0.085f)
                val p = c + Offset(side * cos(a) * -34f * s, sin(a) * 34f * s)
                rotate(side * (i * 12f - 30f), p) {
                    drawOval(Brush.linearGradient(0f to Color(0xFFFFF1A6), 1f to Color(0xFFC58B12), start = p, end = p + Offset(8f * s, 5f * s)), p - Offset(5f * s, 2.6f * s), Size(10f * s, 5.2f * s))
                }
            }
        }
        // padlock shackle + body
        val bc = c + Offset(0f, 6f * s)
        val sh = Path().apply { moveTo(bc.x - 13f * s, bc.y - 6f * s); cubicTo(bc.x - 13f * s, bc.y - 36f * s, bc.x + 13f * s, bc.y - 36f * s, bc.x + 13f * s, bc.y - 6f * s) }
        drawPath(sh, Color.Black.copy(alpha = 0.35f), style = Stroke(7f * s))
        drawPath(sh, chrome(Rect(bc, 24f * s), 20f), style = Stroke(6f * s))
        val body = Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(Rect(bc.x - 22f * s, bc.y - 8f * s, bc.x + 22f * s, bc.y + 26f * s), CornerRadius(6f * s))) }
        solid(body, brass(Rect(bc, 26f * s)), Rect(bc.x - 22f * s, bc.y - 8f * s, bc.x + 22f * s, bc.y + 26f * s), 1.6f * s, gloss = 0.45f, shadow = 0.5f)
        label("NNN", bc + Offset(0f, 6f * s), 12f * s, Color(0xFF5B3A06), letter = 0.06f)
        // light sweep
        val x = ((t % 2.4f) / 2.4f) * 140f * s - 70f * s
        drawRect(Brush.linearGradient(0f to Color.Transparent, 0.5f to Color.White.copy(alpha = 0.45f), 1f to Color.Transparent, start = c + Offset(x - 10f * s, -40f * s), end = c + Offset(x + 10f * s, 40f * s)), Offset(c.x - 50f * s, c.y - 50f * s), Size(100f * s, 100f * s), blendMode = androidx.compose.ui.graphics.BlendMode.Overlay)
    }
}
