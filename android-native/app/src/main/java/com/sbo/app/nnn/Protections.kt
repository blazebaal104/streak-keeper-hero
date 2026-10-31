package com.sbo.app.nnn

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Draws one protection layer centred on [c], in units [u] (1u = 1/100 of the protection area), in the state
 * reached after [step] taps (0 = untouched; the final tap is handled by the caller, which throws the layer away).
 * [t] is the clock in seconds (wind, blinking LEDs), [seed] varies the details per layer.
 */
fun DrawScope.drawProtection(kind: Kind, step: Int, c: Offset, u: Float, tex: Textures, t: Float, seed: Int) {
    when (kind) {
        Kind.BEAM -> beam(step, c, u, tex, seed)
        Kind.CHAIN_PADLOCK -> chainPadlock(step, c, u, seed)
        Kind.SAFE -> safe(step, c, u, tex, t)
        Kind.DUCT_TAPE -> ductTape(step, c, u, tex, seed)
        Kind.STEEL_PLATE -> steelPlate(step, c, u, tex, seed)
        Kind.U_LOCK -> uLock(step, c, u)
        Kind.ZIP_TIES -> zipTies(step, c, u, seed)
        Kind.BARBED_WIRE -> barbedWire(step, c, u, seed)
        Kind.GLASS_CASE -> glassCase(step, c, u, seed)
        Kind.KEYPAD -> keypad(step, c, u, tex, t)
        Kind.HANDCUFFS -> handcuffs(step, c, u)
        Kind.ROPE -> rope(step, c, u, seed)
        Kind.GRATE -> grate(step, c, u, tex)
        Kind.COMBO_PADLOCK -> comboPadlock(step, c, u)
        Kind.RATCHET_STRAP -> ratchetStrap(step, c, u)
        Kind.STRETCH_WRAP -> stretchWrap(step, c, u, seed)
        Kind.BOX -> cardboardBox(step, c, u, seed)
        Kind.SANDBAGS -> sandbags(step, c, u, tex, seed)
        Kind.BRICKS -> bricks(step, c, u, tex, seed)
        Kind.BELT -> belt(step, c, u, tex)
        Kind.CRIME_TAPE -> crimeTape(step, c, u, t, seed)
    }
}

private fun rr(c: Offset, w: Float, h: Float, r: Float) = Path().apply { addRoundRect(RoundRect(Rect(Offset(c.x - w / 2, c.y - h / 2), Size(w, h)), CornerRadius(r))) }
private fun box(c: Offset, w: Float, h: Float) = Rect(Offset(c.x - w / 2, c.y - h / 2), Size(w, h))

// 1 ─ wooden beam nailed across, two nails
private fun DrawScope.beam(step: Int, c: Offset, u: Float, tex: Textures, seed: Int) {
    val tilt = -11f + (seed % 7) + if (step >= 1) 7f else 0f
    val pivot = c + Offset(38f * u, 0f) // after the left nail goes the beam swings on the right one
    rotate(tilt + if (step >= 1) 16f else 0f, if (step >= 1) pivot else c) {
        val w = 128f * u; val h = 24f * u
        val p = rr(c, w, h, 2.5f * u)
        val b = box(c, w, h)
        solid(p, tex.brush("rough_wood", 140f * u, 90f, Offset(seed * 37f, seed * 11f), Color(0xFF8A6A48)), b, 2.6f * u, tint = Color(0xFFCFA57C), gloss = 0.18f)
        // end grain + saw marks
        listOf(-1f, 1f).forEach { s ->
            val ex = c.x + s * (w / 2 - 1.2f * u)
            drawLine(Color.Black.copy(alpha = 0.35f), Offset(ex, b.top + 2 * u), Offset(ex, b.bottom - 2 * u), 1.6f * u)
        }
        nail(c + Offset(-42f * u, 0f), 3.6f * u, pulled = step >= 1)
        nail(c + Offset(42f * u, 0f), 3.6f * u, pulled = step >= 2)
    }
}

// 2 ─ chain crossed over the button, brass padlock
private fun DrawScope.chainPadlock(step: Int, c: Offset, u: Float, seed: Int) {
    val sag = if (step >= 2) 10f * u else 0f
    chain(c + Offset(-62f * u, -40f * u), c + Offset(62f * u, 40f * u), 11f * u, sag)
    chain(c + Offset(-62f * u, 40f * u), c + Offset(62f * u, -40f * u), 11f * u, sag)
    val lc = c + Offset(0f, 18f * u)
    // shackle (pops up when open)
    val lift = if (step >= 2) 9f * u else 0f
    val sh = Path().apply {
        moveTo(lc.x - 9f * u, lc.y - 8f * u - lift)
        cubicTo(lc.x - 9f * u, lc.y - 30f * u - lift, lc.x + 9f * u, lc.y - 30f * u - lift, lc.x + 9f * u, lc.y - 8f * u - lift)
    }
    drawPath(sh, Color.Black.copy(alpha = 0.5f), style = Stroke(5.5f * u, cap = StrokeCap.Round))
    drawPath(sh, chrome(box(lc, 20f * u, 30f * u), 10f), style = Stroke(4.6f * u, cap = StrokeCap.Round))
    // body
    val body = rr(lc + Offset(0f, 6f * u), 30f * u, 26f * u, 4f * u)
    solid(body, brass(box(lc, 30f * u, 26f * u)), box(lc + Offset(0f, 6f * u), 30f * u, 26f * u), 1.6f * u, gloss = 0.35f)
    // keyway
    val kc = lc + Offset(0f, 9f * u)
    drawCircle(Color(0xFF2A1E08), 2.6f * u, kc)
    drawRect(Color(0xFF2A1E08), Offset(kc.x - 1f * u, kc.y), Size(2f * u, 5f * u))
    if (step >= 1) { // key in and turned
        rotate(90f, kc) {
            drawRect(chrome(box(kc + Offset(0f, 8f * u), 3f * u, 14f * u)), Offset(kc.x - 1.2f * u, kc.y), Size(2.4f * u, 12f * u))
            drawCircle(chrome(box(kc + Offset(0f, 19f * u), 7f * u, 7f * u), 60f), 6.5f * u, kc + Offset(0f, 18f * u))
            drawCircle(Color(0xFF3A3F45), 2f * u, kc + Offset(0f, 18f * u))
        }
    }
    label("STAHL", lc + Offset(0f, 0f), 4f * u, Color(0xFF5A3E0E))
}

// 3 ─ combination safe door
private fun DrawScope.safe(step: Int, c: Offset, u: Float, tex: Textures, t: Float) {
    val w = 100f * u; val h = 100f * u
    val b = box(c, w, h)
    // body frame
    solid(rr(c, w + 8f * u, h + 8f * u, 6f * u), tex.brush("blue_metal_plate", 120f * u, 0f, Offset(5f, 9f), Color(0xFF2E3A46)), box(c, w + 8f * u, h + 8f * u), 2.4f * u, tint = Color(0xFF454B52))
    if (step >= 5) {
        // door swung open on its left hinges: dark interior + door edge-on
        drawRect(Brush.radialGradient(0f to Color(0xFF1B1F24), 1f to Color(0xFF050608), center = c, radius = w * 0.7f), b.topLeft, b.size)
        val door = Path().apply { moveTo(b.left, b.top); lineTo(b.left - 26f * u, b.top + 10f * u); lineTo(b.left - 26f * u, b.bottom - 10f * u); lineTo(b.left, b.bottom); close() }
        solid(door, tex.brush("blue_metal_plate", 120f * u), box(Offset(b.left - 13f * u, c.y), 26f * u, h), 2f * u, tint = Color(0xFF5A626B))
        return
    }
    val door = rr(c, w - 6f * u, h - 6f * u, 4f * u)
    solid(door, tex.brush("blue_metal_plate", 110f * u, 90f, Offset(31f, 3f), Color(0xFF3A4652)), box(c, w - 6f * u, h - 6f * u), 2.2f * u, tint = Color(0xFF6A737C), gloss = 0.3f)
    // hinges
    listOf(-30f, 30f).forEach { y -> drawRoundRect(chrome(box(Offset(b.left + 2f * u, c.y + y * u), 6f * u, 14f * u), 0f, dark = true), Offset(b.left - 1f * u, c.y + y * u - 7f * u), Size(6f * u, 14f * u), CornerRadius(2f * u)) }
    // dial: right, left, right — each correct number lights a LED
    val dc = c + Offset(-6f * u, -8f * u)
    val dialRot = when (step) { 0 -> 0f; 1 -> -128f; 2 -> 64f; else -> -212f }
    drawCircle(Color.Black.copy(alpha = 0.5f), 22f * u, dc + Offset(1.5f * u, 2.5f * u))
    drawCircle(chrome(box(dc, 44f * u, 44f * u), 40f, dark = true), 21f * u, dc)
    rotate(dialRot, dc) {
        drawCircle(chrome(box(dc, 34f * u, 34f * u), 220f), 17f * u, dc)
        for (i in 0 until 50) {
            val a = (i / 50f) * 2 * PI.toFloat()
            val r0 = if (i % 5 == 0) 12.6f * u else 14.2f * u
            drawLine(Color(0xFF16181B), dc + Offset(cos(a) * r0, sin(a) * r0), dc + Offset(cos(a) * 16f * u, sin(a) * 16f * u), 0.5f * u)
        }
        for (i in 0 until 10) {
            val a = (i / 10f) * 2 * PI.toFloat() - PI.toFloat() / 2
            label("${i * 10}", dc + Offset(cos(a) * 10f * u, sin(a) * 10f * u), 2.6f * u, Color(0xFF16181B))
        }
        drawCircle(Brush.radialGradient(0f to Color(0xFFDDE1E5), 1f to Color(0xFF5E656C), center = dc + Offset(-2f * u, -2f * u), radius = 7f * u), 6.5f * u, dc)
    }
    drawPath(Path().apply { moveTo(dc.x, dc.y - 22.5f * u); lineTo(dc.x - 1.8f * u, dc.y - 25.5f * u); lineTo(dc.x + 1.8f * u, dc.y - 25.5f * u); close() }, Color(0xFFE53935))
    // LEDs
    for (i in 0 until 3) {
        val on = step > i
        val lc = c + Offset(26f * u, -26f * u + i * 7f * u)
        drawCircle(Color.Black, 2.4f * u, lc)
        drawCircle(if (on) Color(0xFF39FF6A) else Color(0xFF3B1010), 1.8f * u, lc)
        if (on) drawCircle(Brush.radialGradient(0f to Color(0xFF39FF6A).copy(alpha = 0.6f), 1f to Color.Transparent, center = lc, radius = 6f * u), 6f * u, lc)
    }
    // handle: three spokes, turns 90° when unlocked
    val hc = c + Offset(24f * u, 22f * u)
    rotate(if (step >= 4) 90f else 0f, hc) {
        for (k in 0 until 3) {
            val a = k * 2 * PI.toFloat() / 3 - PI.toFloat() / 2
            val e = hc + Offset(cos(a) * 14f * u, sin(a) * 14f * u)
            drawLine(Color.Black.copy(alpha = 0.5f), hc + Offset(1f * u, 1.5f * u), e + Offset(1f * u, 1.5f * u), 3.6f * u, StrokeCap.Round)
            drawLine(Color(0xFFB8BEC4), hc, e, 3f * u, StrokeCap.Round)
            drawCircle(chrome(box(e, 7f * u, 7f * u), 30f), 3.2f * u, e)
        }
        drawCircle(chrome(box(hc, 10f * u, 10f * u), 50f), 5f * u, hc)
    }
    label("SICUR", c + Offset(0f, 40f * u), 4.2f * u, Color(0xFFC9A44C), letter = 0.25f)
}

// 4 ─ silver duct tape, three strips
private fun DrawScope.ductTape(step: Int, c: Offset, u: Float, tex: Textures, seed: Int) {
    val strips = listOf(-38f, 34f, 4f)
    strips.forEachIndexed { i, ang ->
        if (step > i) return@forEachIndexed
        rotate(ang + (seed % 5), c) {
            val w = 132f * u; val h = 19f * u
            val left = c.x - w / 2; val right = c.x + w / 2; val top = c.y - h / 2; val bot = c.y + h / 2
            // torn zig-zag ends
            val p = Path().apply {
                moveTo(left, top)
                lineTo(right, top)
                var y = top; var k = 0
                while (y < bot) { y += h / 6; lineTo(right - if (k++ % 2 == 0) 2.4f * u else 0f, y.coerceAtMost(bot)) }
                lineTo(left, bot)
                y = bot; k = 0
                while (y > top) { y -= h / 6; lineTo(left + if (k++ % 2 == 0) 2.4f * u else 0f, y.coerceAtLeast(top)) }
                close()
            }
            val b = box(c, w, h)
            solid(p, Brush.linearGradient(0f to Color(0xFFB9BEC3), 0.5f to Color(0xFF8E949A), 1f to Color(0xFFA8AEB4), start = b.topLeft, end = b.bottomLeft), b, 0.9f * u, gloss = 0.4f, shadow = 0.45f)
            // woven scrim texture showing through
            clipPath(p) {
                drawRect(tex.brush("hessian_230", 22f * u, 0f, Offset(i * 13f, 0f), Color.Gray), b.topLeft, b.size, alpha = 0.22f, blendMode = BlendMode.Multiply)
                for (k in 0 until 6) drawLine(Color.White.copy(alpha = 0.10f), Offset(left, top + h * (0.15f + k * 0.14f)), Offset(right, top + h * (0.18f + k * 0.14f)), 0.4f * u)
                // wrinkles
                for (k in 0 until 5) {
                    val x = left + w * (0.12f + 0.19f * k + 0.03f * ((seed + k) % 3))
                    drawLine(Brush.linearGradient(0f to Color.Transparent, 0.5f to Color.Black.copy(alpha = 0.18f), 1f to Color.Transparent, start = Offset(x, top), end = Offset(x + 3f * u, bot)), Offset(x, top), Offset(x + 3f * u, bot), 1.4f * u)
                }
            }
        }
    }
}

// 5 ─ steel plate with four bolts
private fun DrawScope.steelPlate(step: Int, c: Offset, u: Float, tex: Textures, seed: Int) {
    val w = 104f * u
    val p = rr(c, w, w, 3f * u)
    solid(p, tex.brush("metal_plate_02", 120f * u, (seed % 4) * 90f, Offset(seed * 17f, 0f), Color(0xFF7A8088)), box(c, w, w), 2.6f * u, gloss = 0.35f)
    clipPath(p) { drawRect(Brush.linearGradient(0f to Color.Transparent, 0.48f to Color.White.copy(alpha = 0.22f), 0.52f to Color.Transparent, start = c + Offset(-w, -w), end = c + Offset(w, w)), Offset(c.x - w / 2, c.y - w / 2), Size(w, w)) }
    val corners = listOf(Offset(-1f, -1f), Offset(1f, -1f), Offset(1f, 1f), Offset(-1f, 1f))
    corners.forEachIndexed { i, d ->
        val bc = c + Offset(d.x * 40f * u, d.y * 40f * u)
        if (step > i) drawCircle(Brush.radialGradient(0f to Color.Black, 1f to Color(0xFF2B2F33), center = bc, radius = 4f * u), 4f * u, bc)
        else bolt(bc, 5.2f * u, 12f * i + seed)
    }
    label("NON TOCCARE", c, 6f * u, Color(0xFF1A1C1E).copy(alpha = 0.55f), letter = 0.12f)
}

// 6 ─ U-lock (bike lock)
private fun DrawScope.uLock(step: Int, c: Offset, u: Float) {
    val out = if (step >= 2) 16f * u else 0f
    val sh = Path().apply {
        moveTo(c.x - 34f * u, c.y + 26f * u - out)
        lineTo(c.x - 34f * u, c.y - 22f * u - out)
        cubicTo(c.x - 34f * u, c.y - 62f * u - out, c.x + 34f * u, c.y - 62f * u - out, c.x + 34f * u, c.y - 22f * u - out)
        lineTo(c.x + 34f * u, c.y + 26f * u - out)
    }
    drawPath(sh, Color.Black.copy(alpha = 0.5f), style = Stroke(15f * u, cap = StrokeCap.Round))
    drawPath(sh, Color(0xFF151515), style = Stroke(13f * u, cap = StrokeCap.Round)) // rubber coating
    drawPath(sh, Brush.linearGradient(0f to Color.White.copy(alpha = 0.28f), 0.5f to Color.Transparent, start = c + Offset(-40f * u, -60f * u), end = c), style = Stroke(5f * u, cap = StrokeCap.Round))
    // crossbar
    val bar = rr(c + Offset(0f, 30f * u), 96f * u, 20f * u, 10f * u)
    solid(bar, Brush.verticalGradient(0f to Color(0xFF2B2B2B), 0.4f to Color(0xFF0F0F0F), 1f to Color(0xFF050505), startY = c.y + 20f * u, endY = c.y + 40f * u), box(c + Offset(0f, 30f * u), 96f * u, 20f * u), 1.8f * u, gloss = 0.35f)
    drawRoundRect(Color(0xFFFFC400), Offset(c.x - 18f * u, c.y + 22f * u), Size(36f * u, 16f * u), CornerRadius(3f * u))
    label("KRYPTO", c + Offset(0f, 30f * u), 5f * u, Color(0xFF111111), letter = 0.12f)
    // end caps + key cylinder
    listOf(-1f, 1f).forEach { s -> drawCircle(chrome(box(c + Offset(s * 44f * u, 30f * u), 14f * u, 14f * u), 30f, dark = true), 7f * u, c + Offset(s * 44f * u, 30f * u)) }
    val kc = c + Offset(44f * u, 30f * u)
    drawRect(Color.Black, Offset(kc.x - 2.8f * u, kc.y - 0.8f * u), Size(5.6f * u, 1.6f * u))
    if (step >= 1) rotate(90f, kc) {
        drawRect(chrome(box(kc + Offset(0f, 7f * u), 3f * u, 10f * u)), Offset(kc.x - 1.2f * u, kc.y), Size(2.4f * u, 9f * u))
        drawRoundRect(Color(0xFF111111), Offset(kc.x - 5f * u, kc.y + 8f * u), Size(10f * u, 12f * u), CornerRadius(3f * u))
    }
}

// 7 ─ black nylon zip ties wrapped round the button
private fun DrawScope.zipTies(step: Int, c: Offset, u: Float, seed: Int) {
    for (i in 0 until 3) {
        if (step > i) continue
        rotate(-50f + i * 50f + (seed % 9), c) {
            val p = Path().apply { addOval(Rect(Offset(c.x - 62f * u, c.y - 14f * u), Size(124f * u, 28f * u))) }
            drawPath(p, Color.Black.copy(alpha = 0.5f), style = Stroke(6.4f * u))
            drawPath(p, Color(0xFF151618), style = Stroke(5.4f * u))
            // serrations
            drawPath(p, Color(0xFF2E3034), style = Stroke(5.4f * u, pathEffect = PathEffect.dashPathEffect(floatArrayOf(0.6f * u, 1.0f * u))))
            drawPath(p, Color.White.copy(alpha = 0.22f), style = Stroke(1.2f * u))
            // ratchet head + cut tail
            val hc = c + Offset(56f * u, -6f * u)
            drawRoundRect(Color(0xFF0E0F10), Offset(hc.x - 5f * u, hc.y - 4f * u), Size(10f * u, 8f * u), CornerRadius(1.5f * u))
            drawRoundRect(Color.White.copy(alpha = 0.18f), Offset(hc.x - 4f * u, hc.y - 3.4f * u), Size(8f * u, 1.6f * u), CornerRadius(0.8f * u))
            drawLine(Color(0xFF151618), hc + Offset(4f * u, 0f), hc + Offset(16f * u, -10f * u), 4.6f * u, StrokeCap.Butt)
        }
    }
}

// 8 ─ barbed wire coil
private fun DrawScope.barbedWire(step: Int, c: Offset, u: Float, seed: Int) {
    val r = Random(seed)
    val strands = 2
    for (s in 0 until strands) {
        val cut = step > s
        val path = Path()
        val y0 = c.y + (s * 2 - 1) * 16f * u
        val sag = if (cut) 30f * u else 0f
        path.moveTo(c.x - 66f * u, y0)
        var x = c.x - 66f * u
        while (x < c.x + 66f * u) {
            val nx = x + 12f * u
            val k = (nx - c.x) / (66f * u)
            path.quadraticTo(x + 6f * u, y0 + (if ((x / u).toInt() % 2 == 0) -9f else 9f) * u + sag * (1 - k * k), nx, y0 + sag * (1 - k * k))
            x = nx
        }
        drawPath(path, Color.Black.copy(alpha = 0.45f), style = Stroke(2.6f * u))
        drawPath(path, chrome(box(c, 130f * u, 40f * u), 80f), style = Stroke(1.8f * u))
        if (cut) continue
        // barbs every ~16u
        var bx = c.x - 58f * u
        while (bx < c.x + 60f * u) {
            val bc = Offset(bx, y0 + (r.nextFloat() - 0.5f) * 4f * u)
            for (k in 0 until 4) {
                val a = k * PI.toFloat() / 2 + PI.toFloat() / 4 + r.nextFloat() * 0.3f
                drawLine(Color.Black.copy(alpha = 0.4f), bc + Offset(0.6f * u, 0.8f * u), bc + Offset(cos(a) * 6f * u + 0.6f * u, sin(a) * 6f * u + 0.8f * u), 1.4f * u, StrokeCap.Round)
                drawLine(Color(0xFFA9AFB5), bc, bc + Offset(cos(a) * 6f * u, sin(a) * 6f * u), 1.1f * u, StrokeCap.Round)
            }
            drawCircle(chrome(box(bc, 5f * u, 5f * u), 40f), 2.2f * u, bc)
            bx += 16f * u
        }
    }
}

// 9 ─ "break glass" case
private fun DrawScope.glassCase(step: Int, c: Offset, u: Float, seed: Int) {
    val w = 104f * u
    val frame = rr(c, w, w, 5f * u)
    solid(frame, Brush.linearGradient(0f to Color(0xFFE53935), 0.5f to Color(0xFFB71C1C), 1f to Color(0xFF7F0F0F), start = c + Offset(-w / 2, -w / 2), end = c + Offset(w / 2, w / 2)), box(c, w, w), 2.4f * u, gloss = 0.4f)
    val inner = box(c + Offset(0f, 4f * u), w - 18f * u, w - 30f * u)
    label("IN CASO DI EMERGENZA", c + Offset(0f, -w / 2 + 6.5f * u), 4.6f * u, Color.White, letter = 0.06f)
    drawRect(Color.Black.copy(alpha = 0.35f), inner.topLeft, inner.size)
    if (step < 2) {
        // glass with reflections
        drawRect(Color(0x33BFE9FF), inner.topLeft, inner.size)
        drawPath(Path().apply { moveTo(inner.left, inner.top + inner.height * 0.35f); lineTo(inner.left + inner.width * 0.55f, inner.top); lineTo(inner.left + inner.width * 0.75f, inner.top); lineTo(inner.left, inner.top + inner.height * 0.62f); close() }, Color.White.copy(alpha = 0.22f))
        drawRect(Color.White.copy(alpha = 0.5f), inner.topLeft, inner.size, style = Stroke(0.7f * u))
        label("ROMPERE IL VETRO", inner.center + Offset(0f, inner.height * 0.32f), 5f * u, Color.White.copy(alpha = 0.92f), letter = 0.05f)
        if (step == 1) { // cracked: radial cracks from the impact point
            val r = Random(seed)
            val hit = inner.center + Offset(-6f * u, -4f * u)
            for (k in 0 until 11) {
                val a = r.nextFloat() * 2 * PI.toFloat()
                var p = hit
                val path = Path().apply { moveTo(hit.x, hit.y) }
                repeat(4) { j ->
                    p += Offset(cos(a + (r.nextFloat() - 0.5f) * 0.5f) * (6f + j * 3f) * u, sin(a + (r.nextFloat() - 0.5f) * 0.5f) * (6f + j * 3f) * u)
                    path.lineTo(p.x.coerceIn(inner.left, inner.right), p.y.coerceIn(inner.top, inner.bottom))
                }
                drawPath(path, Color.White.copy(alpha = 0.85f), style = Stroke(0.5f * u))
            }
            for (ring in 1..2) drawCircle(Color.White.copy(alpha = 0.55f), (6f + ring * 7f) * u, hit, style = Stroke(0.35f * u, pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f * u, 2.2f * u), ring * 2f)))
            drawCircle(Color.White.copy(alpha = 0.9f), 2.2f * u, hit)
        }
    } else {
        // broken: a few jagged shards stuck in the frame
        drawPath(Path().apply { moveTo(inner.left, inner.top); lineTo(inner.left + 14f * u, inner.top); lineTo(inner.left, inner.top + 18f * u); close() }, Color(0x66CDEFFF))
        drawPath(Path().apply { moveTo(inner.right, inner.bottom); lineTo(inner.right - 18f * u, inner.bottom); lineTo(inner.right, inner.bottom - 10f * u); close() }, Color(0x66CDEFFF))
    }
    // hammer on a chain
    val hc = c + Offset(w / 2 + 3f * u, -10f * u)
    drawLine(Color(0xFFB0B5BA), hc, hc + Offset(0f, 18f * u), 1f * u)
    drawRoundRect(Color(0xFFD32F2F), Offset(hc.x - 2f * u, hc.y + 18f * u), Size(4f * u, 16f * u), CornerRadius(1f * u))
    drawRoundRect(chrome(box(hc + Offset(0f, 34f * u), 10f * u, 4f * u), 0f), Offset(hc.x - 5f * u, hc.y + 32f * u), Size(10f * u, 4.5f * u), CornerRadius(1f * u))
}

// 10 ─ electronic keypad lock
private fun DrawScope.keypad(step: Int, c: Offset, u: Float, tex: Textures, t: Float) {
    val w = 84f * u; val h = 104f * u
    val body = rr(c, w, h, 7f * u)
    solid(body, tex.brush("metal_plate", 90f * u, 0f, Offset(3f, 3f), Color(0xFF6F757C)), box(c, w, h), 2.4f * u, tint = Color(0xFF8E959C), gloss = 0.35f)
    // LCD
    val lcd = box(c + Offset(0f, -34f * u), 62f * u, 16f * u)
    drawRoundRect(Color(0xFF0B1A10), lcd.topLeft, lcd.size, CornerRadius(2f * u))
    val code = "4729"
    val shown = if (step >= 5) "APERTO" else (code.take(step.coerceAtMost(4)).map { "•" }.joinToString(" ") + (if (step < 4 && (t * 2).toInt() % 2 == 0) " _" else ""))
    label(shown.ifBlank { "_" }, lcd.center, 8f * u, if (step >= 5) Color(0xFF5CFF8F) else Color(0xFF7CFF9F), font = android.graphics.Typeface.MONOSPACE)
    drawRoundRect(Color.White.copy(alpha = 0.08f), lcd.topLeft, Size(lcd.width, lcd.height / 2), CornerRadius(2f * u))
    // keys
    val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "✱", "0", "#")
    val pressed = code.take(step.coerceAtMost(4)).map { it.toString() }.toSet()
    keys.forEachIndexed { i, k ->
        val kc = c + Offset((i % 3 - 1) * 20f * u, -10f * u + (i / 3) * 15f * u)
        val down = k in pressed && step <= 4 && k == code.getOrNull(step - 1)?.toString()
        val kr = rr(kc + Offset(0f, if (down) 0.8f * u else 0f), 16f * u, 11f * u, 2.5f * u)
        drawPath(rr(kc + Offset(0.6f * u, 1.6f * u), 16f * u, 11f * u, 2.5f * u), Color.Black.copy(alpha = 0.5f))
        drawPath(kr, chrome(box(kc, 16f * u, 11f * u), 80f))
        label(k, kc + Offset(0f, if (down) 0.8f * u else 0f), 5f * u, Color(0xFF22262A))
    }
    // status LED
    val led = c + Offset(32f * u, -47f * u)
    val ok = step >= 5
    drawCircle(if (ok) Color(0xFF39FF6A) else Color(0xFFFF2D2D), 2f * u, led)
    drawCircle(Brush.radialGradient(0f to (if (ok) Color(0xFF39FF6A) else Color(0xFFFF2D2D)).copy(alpha = 0.55f), 1f to Color.Transparent, center = led, radius = 7f * u), 7f * u, led)
}

// 11 ─ handcuffs locked across the button
private fun DrawScope.handcuffs(step: Int, c: Offset, u: Float) {
    val l = c + Offset(-34f * u, 6f * u); val r = c + Offset(34f * u, 6f * u)
    chain(l + Offset(18f * u, -10f * u), r + Offset(-18f * u, -10f * u), 7f * u, 4f * u)
    listOf(l, r).forEachIndexed { i, cc ->
        val open = step > i
        rotate(if (i == 0) -12f else 12f, cc) {
            val ring = Path().apply { addOval(Rect(cc, 21f * u)) }
            val ringInner = Path().apply { addOval(Rect(cc, 15f * u)) }
            val band = Path().apply { op(ring, ringInner, androidx.compose.ui.graphics.PathOperation.Difference) }
            if (open) rotate(-40f, cc + Offset(0f, -18f * u)) { solid(band, chrome(Rect(cc, 21f * u), 60f), Rect(cc, 21f * u), 1.2f * u, gloss = 0.4f) }
            else solid(band, chrome(Rect(cc, 21f * u), 60f), Rect(cc, 21f * u), 1.2f * u, gloss = 0.4f)
            // lock box
            val lb = rr(cc + Offset(0f, -18f * u), 22f * u, 12f * u, 2f * u)
            solid(lb, chrome(box(cc + Offset(0f, -18f * u), 22f * u, 12f * u), 20f, dark = true), box(cc + Offset(0f, -18f * u), 22f * u, 12f * u), 1f * u)
            drawCircle(Color.Black, 1.6f * u, cc + Offset(0f, -18f * u))
        }
    }
}

// 12 ─ rope tied round twice, big knot
private fun DrawScope.rope(step: Int, c: Offset, u: Float, seed: Int) {
    val base = Color(0xFFC9A26A); val dark = Color(0xFF7A5A2E)
    val loose = step >= 1
    for (k in 0 until 2) {
        val y = c.y + (k * 2 - 1) * 10f * u + if (loose) 6f * u else 0f
        cord(listOf(Offset(c.x - 70f * u, y + 6f * u), Offset(c.x - 40f * u, y - 2f * u), Offset(c.x, y - 4f * u + (seed % 5) * 0.5f * u), Offset(c.x + 40f * u, y - 2f * u), Offset(c.x + 70f * u, y + 6f * u)), 8f * u, base, dark)
    }
    // knot
    val kc = c + Offset(6f * u, -2f * u)
    if (step < 2) {
        cord(listOf(kc + Offset(-12f * u, 8f * u), kc + Offset(-10f * u, -10f * u), kc + Offset(8f * u, -12f * u), kc + Offset(10f * u, 6f * u), kc + Offset(-6f * u, 10f * u)), 9f * u, base, dark)
        cord(listOf(kc + Offset(4f * u, 6f * u), kc + Offset(10f * u, 26f * u + (if (loose) 8f else 0f) * u), kc + Offset(4f * u, 44f * u)), 8f * u, base, dark)
        cord(listOf(kc + Offset(-4f * u, 6f * u), kc + Offset(-16f * u, 24f * u), kc + Offset(-24f * u, 40f * u)), 8f * u, base, dark)
        // frayed ends
        listOf(kc + Offset(4f * u, 44f * u), kc + Offset(-24f * u, 40f * u)).forEach { e -> for (j in -2..2) drawLine(base, e, e + Offset(j * 1.5f * u, 5f * u), 1f * u, StrokeCap.Round) }
    }
}

// 13 ─ steel grate with a sliding bolt
private fun DrawScope.grate(step: Int, c: Offset, u: Float, tex: Textures) {
    val w = 108f * u
    val frame = Path().apply {
        addRoundRect(RoundRect(box(c, w, w), CornerRadius(3f * u)))
        addRoundRect(RoundRect(box(c, w - 12f * u, w - 12f * u), CornerRadius(2f * u)))
        fillType = androidx.compose.ui.graphics.PathFillType.EvenOdd
    }
    val metal = tex.brush("rusty_metal_02", 70f * u, 0f, Offset.Zero, Color(0xFF6B4A33))
    solid(frame, metal, box(c, w, w), 1.8f * u, tint = Color(0xFF8C8F92))
    for (i in -3..3) {
        val x = c.x + i * 13f * u
        val bar = rr(Offset(x, c.y), 4.6f * u, w - 10f * u, 2.3f * u)
        solid(bar, metal, box(Offset(x, c.y), 4.6f * u, w), 0.9f * u, tint = Color(0xFF9EA3A8), shadow = 0.5f)
    }
    for (j in listOf(-1f, 1f)) {
        val bar = rr(Offset(c.x, c.y + j * 22f * u), w - 10f * u, 4.2f * u, 2f * u)
        solid(bar, metal, box(Offset(c.x, c.y + j * 22f * u), w, 4.2f * u), 0.9f * u, tint = Color(0xFF9EA3A8), shadow = 0.5f)
    }
    // sliding bolt latch on the right edge
    val lc = c + Offset(w / 2 - 2f * u, 0f)
    drawRoundRect(chrome(box(lc, 14f * u, 12f * u), 90f, dark = true), Offset(lc.x - 7f * u, lc.y - 6f * u), Size(14f * u, 12f * u), CornerRadius(1.5f * u))
    val slide = if (step >= 1) -12f * u else 0f
    drawRoundRect(chrome(box(lc, 24f * u, 4f * u), 90f), Offset(lc.x - 12f * u + slide, lc.y - 2f * u), Size(24f * u, 4f * u), CornerRadius(2f * u))
    val knob = Offset(lc.x - 2f * u + slide, lc.y - (if (step >= 2) 0f else 6f) * u)
    drawLine(chrome(box(knob, 3f * u, 8f * u), 0f), Offset(lc.x - 2f * u + slide, lc.y), knob, 2.6f * u, StrokeCap.Round)
    drawCircle(chrome(box(knob, 5f * u, 5f * u), 30f), 2.6f * u, knob)
}

// 14 ─ four-wheel combination padlock
private fun DrawScope.comboPadlock(step: Int, c: Offset, u: Float) {
    val open = step >= 5
    val lift = if (open) 12f * u else 0f
    val bc = c + Offset(0f, 12f * u)
    val sh = Path().apply { moveTo(bc.x - 16f * u, bc.y - 16f * u - lift); cubicTo(bc.x - 16f * u, bc.y - 56f * u - lift, bc.x + 16f * u, bc.y - 56f * u - lift, bc.x + 16f * u, bc.y - 16f * u - if (open) 0f else 0f) }
    drawPath(sh, Color.Black.copy(alpha = 0.5f), style = Stroke(8f * u, cap = StrokeCap.Round))
    drawPath(sh, chrome(box(bc, 40f * u, 50f * u), 0f), style = Stroke(7f * u, cap = StrokeCap.Round))
    val body = rr(bc, 52f * u, 46f * u, 6f * u)
    solid(body, chrome(box(bc, 52f * u, 46f * u), 25f, dark = true), box(bc, 52f * u, 46f * u), 1.6f * u, gloss = 0.35f)
    val digits = listOf(3, 8, 1, 6)
    for (i in 0 until 4) {
        val wc = bc + Offset((i - 1.5f) * 11f * u, 4f * u)
        val wheel = rr(wc, 9.5f * u, 22f * u, 1.6f * u)
        drawPath(wheel, Brush.verticalGradient(0f to Color(0xFF1A1A1A), 0.5f to Color(0xFF6D6D6D), 1f to Color(0xFF1A1A1A), startY = wc.y - 11f * u, endY = wc.y + 11f * u))
        val set = step > i
        val d = if (set) digits[i] else (digits[i] + 5) % 10
        label("${(d + 9) % 10}", wc + Offset(0f, -7f * u), 3.6f * u, Color.White.copy(alpha = 0.35f))
        label("$d", wc, 5.6f * u, if (set) Color(0xFFFFD54F) else Color.White)
        label("${(d + 1) % 10}", wc + Offset(0f, 7f * u), 3.6f * u, Color.White.copy(alpha = 0.35f))
    }
    drawRect(Color(0xFFE53935).copy(alpha = 0.8f), Offset(bc.x - 24f * u, bc.y + 3.4f * u), Size(48f * u, 0.8f * u))
}

// 15 ─ orange ratchet strap
private fun DrawScope.ratchetStrap(step: Int, c: Offset, u: Float) {
    val slack = if (step >= 2) 10f * u else 0f
    rotate(-18f, c) {
        val strap = Path().apply { moveTo(c.x - 80f * u, c.y - 9f * u); quadraticTo(c.x, c.y - 9f * u + slack, c.x + 80f * u, c.y - 9f * u); lineTo(c.x + 80f * u, c.y + 9f * u); quadraticTo(c.x, c.y + 9f * u + slack, c.x - 80f * u, c.y + 9f * u); close() }
        solid(strap, Brush.verticalGradient(0f to Color(0xFFFF8F1F), 0.5f to Color(0xFFE5650B), 1f to Color(0xFFB64D04), startY = c.y - 9f * u, endY = c.y + 9f * u), box(c, 160f * u, 18f * u), 1.1f * u, gloss = 0.25f)
        clipPath(strap) { var x = c.x - 80f * u; while (x < c.x + 80f * u) { drawLine(Color.Black.copy(alpha = 0.10f), Offset(x, c.y - 10f * u), Offset(x + 2f * u, c.y + 10f * u + slack), 0.5f * u); x += 1.6f * u } }
        // stitched label
        drawRect(Color.White.copy(alpha = 0.9f), Offset(c.x - 60f * u, c.y - 6f * u), Size(18f * u, 12f * u))
        label("5 t", Offset(c.x - 51f * u, c.y), 4.6f * u, Color(0xFF1565C0))
        // ratchet mechanism
        val rc = c + Offset(14f * u, 0f)
        rotate(if (step >= 1) -55f else 0f, rc + Offset(-12f * u, 0f)) {
            val handle = rr(rc, 30f * u, 22f * u, 3f * u)
            solid(handle, chrome(box(rc, 30f * u, 22f * u), 40f), box(rc, 30f * u, 22f * u), 1.4f * u, gloss = 0.5f)
            for (k in -2..2) drawLine(Color.Black.copy(alpha = 0.35f), rc + Offset(k * 4f * u, -9f * u), rc + Offset(k * 4f * u, 9f * u), 0.6f * u)
            drawRoundRect(Color(0xFF151515), Offset(rc.x + 8f * u, rc.y - 11f * u), Size(9f * u, 22f * u), CornerRadius(2f * u))
        }
        drawCircle(chrome(box(rc + Offset(-12f * u, 0f), 10f * u, 10f * u), 60f, dark = true), 5f * u, rc + Offset(-12f * u, 0f))
    }
}

// 16 ─ stretch wrap film
private fun DrawScope.stretchWrap(step: Int, c: Offset, u: Float, seed: Int) {
    val r = Random(seed)
    val layers = if (step >= 2) 2 else 4
    for (k in 0 until layers) {
        rotate(-8f + k * 7f + r.nextFloat() * 4f, c) {
            val h = 86f * u - k * 6f * u
            val p = Path().apply { moveTo(c.x - 74f * u, c.y - h / 2); quadraticTo(c.x, c.y - h / 2 - 6f * u, c.x + 74f * u, c.y - h / 2 + 3f * u); lineTo(c.x + 70f * u, c.y + h / 2); quadraticTo(c.x, c.y + h / 2 + 8f * u, c.x - 72f * u, c.y + h / 2 - 2f * u); close() }
            drawPath(p, Color(0x1FE6F2FF))
            drawPath(p, Color.White.copy(alpha = 0.32f), style = Stroke(0.7f * u))
            clipPath(p) {
                for (j in 0 until 7) {
                    val y = c.y - h / 2 + h * (0.08f + j * 0.14f) + r.nextFloat() * 4f * u
                    drawLine(Brush.horizontalGradient(0f to Color.Transparent, 0.3f to Color.White.copy(alpha = 0.32f), 0.6f to Color.White.copy(alpha = 0.05f), 1f to Color.Transparent, startX = c.x - 74f * u, endX = c.x + 74f * u), Offset(c.x - 74f * u, y), Offset(c.x + 74f * u, y + 6f * u), (0.6f + r.nextFloat() * 1.6f) * u)
                }
            }
        }
    }
    if (step >= 1) { // torn hole with curled edges
        val hc = c + Offset(-8f * u, -6f * u)
        val hole = Path().apply {
            for (i in 0 until 14) {
                val a = i / 14f * 2 * PI.toFloat(); val rr = (12f + r.nextFloat() * 7f) * u
                val p = hc + Offset(cos(a) * rr, sin(a) * rr * 0.8f)
                if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
            }
            close()
        }
        drawPath(hole, Color.White.copy(alpha = 0.55f), style = Stroke(1.6f * u, join = StrokeJoin.Round))
        drawPath(hole, Color.Black.copy(alpha = 0.10f))
    }
}

// 17 ─ cardboard box sealed with tape (seen from above)
private fun DrawScope.cardboardBox(step: Int, c: Offset, u: Float, seed: Int) {
    val w = 108f * u
    val card = Brush.linearGradient(0f to Color(0xFFD7A86E), 0.5f to Color(0xFFC08D52), 1f to Color(0xFFA9773F), start = c + Offset(-w, -w), end = c + Offset(w, w))
    solid(rr(c, w, w, 2f * u), card, box(c, w, w), 1.8f * u, gloss = 0.15f)
    // corrugated texture lines
    clipPath(rr(c, w, w, 2f * u)) { var x = c.x - w / 2; while (x < c.x + w / 2) { drawLine(Color.Black.copy(alpha = 0.05f), Offset(x, c.y - w / 2), Offset(x, c.y + w / 2), 0.6f * u); x += 2.2f * u } }
    // flaps
    val lOpen = step >= 2; val rOpen = step >= 3
    val left = Path().apply { moveTo(c.x - w / 2, c.y - w / 2); lineTo(c.x, c.y - w / 2); lineTo(c.x, c.y + w / 2); lineTo(c.x - w / 2, c.y + w / 2); close() }
    val right = Path().apply { moveTo(c.x, c.y - w / 2); lineTo(c.x + w / 2, c.y - w / 2); lineTo(c.x + w / 2, c.y + w / 2); lineTo(c.x, c.y + w / 2); close() }
    if (!lOpen) drawPath(left, Brush.horizontalGradient(0f to Color(0x22000000), 1f to Color(0x00000000), startX = c.x - w / 2, endX = c.x))
    else { drawPath(left, Color(0xFF2A1A0A)); drawPath(Path().apply { moveTo(c.x - w / 2, c.y - w / 2); lineTo(c.x - w / 2 - 30f * u, c.y - w / 2 + 6f * u); lineTo(c.x - w / 2 - 30f * u, c.y + w / 2 - 6f * u); lineTo(c.x - w / 2, c.y + w / 2); close() }, card) }
    if (rOpen) { drawPath(right, Color(0xFF2A1A0A)); drawPath(Path().apply { moveTo(c.x + w / 2, c.y - w / 2); lineTo(c.x + w / 2 + 30f * u, c.y - w / 2 + 6f * u); lineTo(c.x + w / 2 + 30f * u, c.y + w / 2 - 6f * u); lineTo(c.x + w / 2, c.y + w / 2); close() }, card) }
    drawLine(Color.Black.copy(alpha = 0.35f), Offset(c.x, c.y - w / 2), Offset(c.x, c.y + w / 2), 0.8f * u)
    // packing tape along the seam (cut after step 1)
    if (step < 1) {
        drawRect(Color(0xCCB07A3C), Offset(c.x - 9f * u, c.y - w / 2 - 6f * u), Size(18f * u, w + 12f * u))
        drawRect(Color.White.copy(alpha = 0.22f), Offset(c.x - 9f * u, c.y - w / 2 - 6f * u), Size(5f * u, w + 12f * u))
    } else if (!lOpen) {
        drawRect(Color(0xCCB07A3C), Offset(c.x - 9f * u, c.y - w / 2 - 6f * u), Size(9f * u, w + 12f * u))
    }
    if (!lOpen && !rOpen) {
        label("FRAGILE", c + Offset(-26f * u, 26f * u), 6f * u, Color(0xFFB71C1C).copy(alpha = 0.85f), letter = 0.1f)
        label("↑ ↑", c + Offset(28f * u, -30f * u), 8f * u, Color.Black.copy(alpha = 0.55f))
    }
}

// 18 ─ sandbags piled on top
private fun DrawScope.sandbags(step: Int, c: Offset, u: Float, tex: Textures, seed: Int) {
    val bags = listOf(Offset(-26f, 18f) to -6f, Offset(26f, 22f) to 8f, Offset(0f, -14f) to -2f)
    bags.forEachIndexed { i, (o, rot) ->
        if (step > 2 - i) return@forEachIndexed // top bag (last in list) goes first
        val bc = c + Offset(o.x * u, o.y * u)
        rotate(rot + (seed % 5), bc) {
            val p = Path().apply {
                moveTo(bc.x - 34f * u, bc.y - 10f * u)
                cubicTo(bc.x - 30f * u, bc.y - 22f * u, bc.x + 30f * u, bc.y - 22f * u, bc.x + 34f * u, bc.y - 10f * u)
                cubicTo(bc.x + 42f * u, bc.y, bc.x + 38f * u, bc.y + 16f * u, bc.x + 30f * u, bc.y + 18f * u)
                cubicTo(bc.x + 10f * u, bc.y + 24f * u, bc.x - 10f * u, bc.y + 24f * u, bc.x - 30f * u, bc.y + 18f * u)
                cubicTo(bc.x - 40f * u, bc.y + 14f * u, bc.x - 42f * u, bc.y, bc.x - 34f * u, bc.y - 10f * u)
                close()
            }
            solid(p, tex.brush("hessian_230", 34f * u, rot, Offset(i * 40f, i * 9f), Color(0xFFB09A72)), box(bc, 80f * u, 44f * u), 2.2f * u, tint = Color(0xFFE2CFA6), gloss = 0.1f)
            // tie at the end + seams
            drawLine(Color(0xFF5A4630), bc + Offset(-30f * u, -14f * u), bc + Offset(-36f * u, 14f * u), 1.4f * u)
            cord(listOf(bc + Offset(-37f * u, -2f * u), bc + Offset(-44f * u, 2f * u), bc + Offset(-47f * u, 8f * u)), 2.6f * u, Color(0xFFCDB487), Color(0xFF6D5735))
            label("SABBIA", bc + Offset(4f * u, 2f * u), 5f * u, Color(0xFF3B2F1F).copy(alpha = 0.55f), letter = 0.15f)
        }
    }
}

// 19 ─ bricked up
private fun DrawScope.bricks(step: Int, c: Offset, u: Float, tex: Textures, seed: Int) {
    val w = 108f * u
    drawRect(Color(0xFF9E9A92), Offset(c.x - w / 2, c.y - w / 2), Size(w, w)) // mortar
    val brickBrush = tex.brush("red_brick", 120f * u, 0f, Offset(seed * 21f, seed * 5f), Color(0xFF8E3B2A))
    val bh = 13f * u; val bw = 26f * u
    val missing = listOf(17 to 1, 10 to 2, 23 to 3) // brick index knocked out at step n
    var idx = 0
    var row = 0
    var y = c.y - w / 2 + 1f * u
    while (y < c.y + w / 2 - 1f * u) {
        var x = c.x - w / 2 + 1f * u - if (row % 2 == 1) bw / 2 else 0f
        while (x < c.x + w / 2) {
            val l = x.coerceAtLeast(c.x - w / 2 + 1f * u); val r = (x + bw - 1.6f * u).coerceAtMost(c.x + w / 2 - 1f * u)
            val gone = missing.any { it.first == idx && step >= it.second }
            if (r - l > 2f * u) {
                val b = Rect(l, y, r, y + bh - 1.6f * u)
                if (gone) drawRect(Brush.radialGradient(0f to Color(0xFF0B0B0B), 1f to Color(0xFF2A2522), center = b.center, radius = b.width), b.topLeft, b.size)
                else {
                    val p = Path().apply { addRoundRect(RoundRect(b, CornerRadius(0.8f * u))) }
                    solid(p, brickBrush, b, 0.8f * u, gloss = 0.12f, shadow = 0.25f)
                }
            }
            x += bw; idx++
        }
        y += bh; row++
    }
}

// 20 ─ leather belt round the button
private fun DrawScope.belt(step: Int, c: Offset, u: Float, tex: Textures) {
    val loose = step >= 1
    val strap = Path().apply {
        moveTo(c.x - 78f * u, c.y - 9f * u); lineTo(c.x + (if (step >= 2) 30f else 78f) * u, c.y - 9f * u)
        lineTo(c.x + (if (step >= 2) 30f else 78f) * u, c.y + 9f * u); lineTo(c.x - 78f * u, c.y + 9f * u); close()
    }
    rotate(14f, c) {
        solid(strap, tex.brush("brown_leather", 60f * u, 0f, Offset.Zero, Color(0xFF5A3A22)), box(c, 156f * u, 18f * u), 1.4f * u, tint = Color(0xFF8A5A36), gloss = 0.3f)
        drawPath(strap, Color(0xFFE6C08A).copy(alpha = 0.6f), style = Stroke(0.5f * u, pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f * u, 1.4f * u))))
        // holes
        for (k in 0 until 4) drawOval(Color(0xFF1A0E06), Offset(c.x + (34f + k * 8f) * u, c.y - 1.4f * u), Size(3.2f * u, 2.8f * u))
        // buckle
        val bc = c + Offset(-6f * u, 0f)
        val frame = Path().apply {
            addRoundRect(RoundRect(box(bc, 20f * u, 26f * u), CornerRadius(4f * u)))
            addRoundRect(RoundRect(box(bc, 13f * u, 19f * u), CornerRadius(3f * u)))
            fillType = androidx.compose.ui.graphics.PathFillType.EvenOdd
        }
        solid(frame, chrome(box(bc, 20f * u, 26f * u), 30f), box(bc, 20f * u, 26f * u), 1f * u, gloss = 0.5f)
        // prong
        if (!loose) drawLine(chrome(box(bc, 14f * u, 3f * u), 0f), bc + Offset(-7f * u, 0f), bc + Offset(9f * u, 0f), 1.8f * u, StrokeCap.Round)
        else drawLine(chrome(box(bc, 14f * u, 3f * u), 0f), bc + Offset(-7f * u, 0f), bc + Offset(-7f * u, -12f * u), 1.8f * u, StrokeCap.Round)
    }
}

// 21 ─ police tape (always the last one): red/white, three strips; a long loose end flaps in the wind
private fun DrawScope.crimeTape(step: Int, c: Offset, u: Float, t: Float, seed: Int) {
    val angles = listOf(-24f, 18f, -4f)
    angles.forEachIndexed { i, ang ->
        if (step > i) return@forEachIndexed
        rotate(ang, c) { tapeStrip(Offset(c.x - 82f * u, c.y), Offset(c.x + 82f * u, c.y), 14f * u, u, t + i) }
    }
    // the loose end: a cloth-like strip waving from the knot, always there while any strip remains
    if (step < 3) flappingTape(c + Offset(60f * u, -26f * u), u, t, seed)
}

/** One straight piece of red/white police tape with printed text. */
fun DrawScope.tapeStrip(a: Offset, b: Offset, h: Float, u: Float, phase: Float) {
    val len = (b - a).getDistance()
    val ang = Math.toDegrees(kotlin.math.atan2((b.y - a.y).toDouble(), (b.x - a.x).toDouble())).toFloat()
    rotate(ang, a) {
        val rect = Rect(a.x, a.y - h / 2, a.x + len, a.y + h / 2)
        val p = Path().apply { addRect(rect) }
        dropShadow(p, 2.6f * u, 1.6f * u, 0.45f)
        drawRect(Color.White, rect.topLeft, rect.size)
        clipPath(p) {
            var x = rect.left - h
            var k = 0
            while (x < rect.right + h) {
                drawPath(Path().apply { moveTo(x, rect.bottom); lineTo(x + h * 0.9f, rect.top); lineTo(x + h * 1.9f, rect.top); lineTo(x + h, rect.bottom); close() }, Color(0xFFD50000))
                x += h * 2.2f; k++
            }
            drawRect(Color.White.copy(alpha = 0.85f), Offset(rect.left, a.y - h * 0.26f), Size(len, h * 0.52f))
            // spacing from the real text width, so the lettering never overlaps whatever the tape height
            val txt = "POLIZIA · NON OLTREPASSARE"
            val fs = h * 0.34f
            val tw = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { textSize = fs; typeface = android.graphics.Typeface.DEFAULT_BOLD; letterSpacing = 0.05f }.measureText(txt)
            var tx = rect.left + h * 0.4f
            while (tx < rect.right) { label(txt, Offset(tx + tw / 2, a.y), fs, Color(0xFF111111), letter = 0.05f); tx += tw + h * 0.9f }
            // plastic sheen + slight wrinkle shading
            drawRect(Brush.verticalGradient(0f to Color.White.copy(alpha = 0.35f), 0.35f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.15f), startY = rect.top, endY = rect.bottom), rect.topLeft, rect.size)
            for (j in 0 until 5) {
                val wx = rect.left + len * (0.1f + j * 0.2f) + sin(phase + j) * 3f * u
                drawRect(Brush.horizontalGradient(0f to Color.Transparent, 0.5f to Color.Black.copy(alpha = 0.10f), 1f to Color.Transparent, startX = wx - 4f * u, endX = wx + 4f * u), Offset(wx - 4f * u, rect.top), Size(8f * u, h))
            }
        }
    }
}

/** A long loose piece of tape fluttering: a ribbon whose waves travel along it, flipping between front and back. */
fun DrawScope.flappingTape(anchor: Offset, u: Float, t: Float, seed: Int, length: Float = 120f, width: Float = 13f) {
    val n = 34
    val pts = ArrayList<Offset>(n + 1)
    val tw = ArrayList<Float>(n + 1)
    for (i in 0..n) {
        val s = i / n.toFloat()
        val x = anchor.x + s * length * u * 0.92f
        // gravity droop + travelling waves (two frequencies) growing toward the free end
        val y = anchor.y + s * s * 22f * u + sin(t * 6.2f - s * 9f + seed) * 11f * u * s + sin(t * 11.3f - s * 15f) * 4f * u * s
        pts += Offset(x, y)
        tw += cos(t * 5.1f - s * 7.5f + seed) // twist: <0 shows the back side
    }
    for (i in 0 until n) {
        val p0 = pts[i]; val p1 = pts[i + 1]
        val d = p1 - p0
        val l = d.getDistance().coerceAtLeast(0.001f)
        val nx = -d.y / l; val ny = d.x / l
        val w0 = width * u / 2 * kotlin.math.abs(tw[i]).coerceAtLeast(0.12f)
        val w1 = width * u / 2 * kotlin.math.abs(tw[i + 1]).coerceAtLeast(0.12f)
        val quad = Path().apply {
            moveTo(p0.x + nx * w0, p0.y + ny * w0); lineTo(p1.x + nx * w1, p1.y + ny * w1)
            lineTo(p1.x - nx * w1, p1.y - ny * w1); lineTo(p0.x - nx * w0, p0.y - ny * w0); close()
        }
        val stripe = (i / 2) % 2 == 0
        val shade = 0.55f + 0.45f * kotlin.math.abs(tw[i])
        val base = if (stripe) Color(0xFFD50000) else Color.White
        drawPath(quad, Color(base.red * shade, base.green * shade, base.blue * shade))
        if (tw[i] > 0.7f) drawPath(quad, Color.White.copy(alpha = 0.18f))
    }
    // torn end
    val e = pts.last()
    drawLine(Color.White.copy(alpha = 0.7f), e + Offset(0f, -width * u * 0.3f), e + Offset(2f * u, width * u * 0.3f), 0.8f * u)
}
