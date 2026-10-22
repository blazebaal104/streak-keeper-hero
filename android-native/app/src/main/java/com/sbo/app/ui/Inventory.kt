package com.sbo.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sbo.app.data.Badge
import com.sbo.app.data.Profile
import com.sbo.app.logic.BadgeDef
import com.sbo.app.logic.Badges

/**
 * The web's shadcn Dialog on a phone: full width, square corners, card background, 1px border,
 * 24dp padding, black/80 backdrop, fade + zoom-in from 95%. Drawn inside the app's own tree
 * (not a separate window) so it composes with everything else.
 */
@Composable
fun SboDialog(onClose: () -> Unit, content: @Composable () -> Unit) {
    BackHandler(onBack = onClose)
    val anim = remember { Animatable(0f) }
    LaunchedEffect(Unit) { anim.animateTo(1f, tween(200, easing = FastOutSlowInEasing)) }
    val maxH = LocalConfiguration.current.screenHeightDp.dp * 0.9f
    Box(
        Modifier.fillMaxSize()
            .graphicsLayer { alpha = anim.value }
            .background(Color.Black.copy(alpha = 0.8f))
            .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onClose),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.fillMaxWidth().heightIn(max = maxH)
                .graphicsLayer { val k = 0.95f + 0.05f * anim.value; scaleX = k; scaleY = k }
                .background(Sbo.card).border(1.dp, Sbo.border)
                .clickable(remember { MutableInteractionSource() }, indication = null) {},
        ) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp)) { content() }
            Icon(
                Lucide.X, "Chiudi", tint = Sbo.foreground.copy(alpha = 0.7f),
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 16.dp, end = 16.dp).size(16.dp).clickable(onClick = onClose),
            )
        }
    }
}

@Composable
private fun DialogHeader(title: String, description: String? = null) {
    Text(title, fontSize = 18.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.45).sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
    if (description != null) {
        Spacer(Modifier.height(6.dp))
        Text(description, color = Sbo.mutedFg, fontSize = 14.sp, lineHeight = 20.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

/** Medal inventory for one slot (web InventoryDialog): tap an owned medal to equip it, a locked one to see how to get it. */
@Composable
fun InventoryDialog(slot: Int, profile: Profile, badges: List<Badge>, onClose: () -> Unit, initialPreview: BadgeDef? = null, onEquip: (String?) -> Unit) {
    var preview by remember { mutableStateOf(initialPreview) }
    val equipped = setOfNotNull(profile.slot_1, profile.slot_2, profile.slot_3)
    SboDialog(onClose) {
        DialogHeader("Inventario medaglie — Slot ${slot + 1}", "Le medaglie che non hai sono in grigio. Toccale per vedere come sbloccarle.")
        Spacer(Modifier.height(16.dp))
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).drawBehind {
                val w = 2.dp.toPx()
                drawRoundRect(
                    Sbo.border, cornerRadius = androidx.compose.ui.geometry.CornerRadius(24.dp.toPx()),
                    topLeft = Offset(w / 2, w / 2), size = androidx.compose.ui.geometry.Size(size.width - w, size.height - w),
                    style = Stroke(w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))),
                )
            }.clickable { onEquip(null) }.padding(12.dp),
            horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Lucide.X, null, tint = Sbo.mutedFg, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("Rimuovi da questo slot", color = Sbo.mutedFg, fontSize = 12.sp, lineHeight = 16.sp)
        }
        Group("🔥 Streak di fuoco (fai ogni giorno)", Badges.FIRE, "fire", badges, equipped, onEquip) { preview = it }
        Group("❄️ Streak di ghiaccio (giorni puliti)", Badges.ICE, "ice", badges, equipped, onEquip) { preview = it }
        Group("💪 Allenamenti totali", Badges.WORKOUT, "workout", badges, equipped, onEquip) { preview = it }
    }
    preview?.let { LockedPreview(it) { preview = null } }
}

@Composable
private fun Group(title: String, defs: List<BadgeDef>, group: String, badges: List<Badge>, equipped: Set<String>, onEquip: (String?) -> Unit, onPreview: (BadgeDef) -> Unit) {
    Spacer(Modifier.height(16.dp))
    Text(title.uppercase(), color = Sbo.mutedFg, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp)
    Spacer(Modifier.height(8.dp))
    defs.chunked(3).forEachIndexed { r, row ->
        if (r > 0) Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            row.forEach { def ->
                val owned = badges.firstOrNull { it.threshold == def.threshold && Badges.sameGroup(it.category, group) }
                val isEquipped = owned != null && owned.id in equipped
                Box(Modifier.weight(1f)) { MedalCell(def, owned, isEquipped, onClick = { if (owned != null) onEquip(owned.id) else onPreview(def) }) }
            }
            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun MedalCell(def: BadgeDef, owned: Badge?, isEquipped: Boolean, onClick: () -> Unit) {
    val press = rememberPressScale()
    Column(
        Modifier.fillMaxWidth()
            .graphicsLayer { scaleX = press.value; scaleY = press.value }
            .then(if (isEquipped) Modifier.border(2.dp, Sbo.primary, RoundedCornerShape(24.dp)) else Modifier)
            .pressable(press, def.name, onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            BadgeArt(def, side = null, locked = owned == null)
            if (owned == null) Icon(Lucide.Lock, null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(14.dp))
            if (isEquipped) Text(
                "Equipaggiata", color = Sbo.background, fontSize = 9.sp, lineHeight = 12.sp,
                modifier = Modifier.align(Alignment.TopStart).padding(4.dp).clip(RoundedCornerShape(4.dp)).background(Sbo.primary).padding(horizontal = 4.dp, vertical = 2.dp),
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(def.name, color = if (owned != null) Sbo.foreground else Sbo.mutedFg, fontSize = 10.sp, lineHeight = 12.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text("${def.threshold}gg", color = Sbo.mutedFg, fontSize = 9.sp, lineHeight = 12.sp)
    }
}

/** "Not unlocked yet" card: the medal floating on its own gradient, plus how to earn it. */
@Composable
private fun LockedPreview(def: BadgeDef, onClose: () -> Unit) {
    val t = rememberInfiniteTransition(label = "float")
    val p by t.animateFloat(0f, 1f, infiniteRepeatable(keyframes { durationMillis = 3000; 0f at 0; 1f at 3000 }), label = "p")
    val sparkleClock = t.animateFloat(0f, 1000f, infiniteRepeatable(tween(1_000_000, easing = androidx.compose.animation.core.LinearEasing)), label = "c")
    SboDialog(onClose) {
        Row(Modifier.fillMaxWidth().padding(end = 24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(Lucide.Lock, null, tint = Sbo.foreground, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("${def.name} — ${def.tier.label}", fontSize = 18.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.45).sp)
        }
        Spacer(Modifier.height(16.dp))
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                .background(Brush.linearGradient(def.gradient, start = Offset.Zero, end = Offset.Infinite))
                .padding(vertical = 32.dp),
            contentAlignment = Alignment.Center,
        ) {
            FloatingSparkles(def.threshold, 14, sparkleClock)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.graphicsLayer {
                    // y: 0 → -8 → 0, rotate: 0 → 6 → -6 → 0 over 3 s, ease in-out
                    val e = 0.5f - 0.5f * kotlin.math.cos(p * 2f * Math.PI.toFloat())
                    translationY = -8.dp.toPx() * e
                    rotationZ = when { p < 1f / 3 -> 6f * ease(p * 3); p < 2f / 3 -> 6f - 12f * ease((p - 1f / 3) * 3); else -> -6f + 6f * ease((p - 2f / 3) * 3) }
                    alpha = 0.7f
                }) {
                    GrayEmoji(def.emoji)
                }
                Spacer(Modifier.height(16.dp))
                Text(def.name, color = Color.White, fontSize = 18.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold)
                Text("Non ancora sbloccata", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp, lineHeight = 20.sp)
            }
        }
        Spacer(Modifier.height(16.dp))
        Column(Modifier.fillMaxWidth().border(1.dp, Sbo.border, RoundedCornerShape(20.dp)).padding(16.dp)) {
            Text("Come ottenerla", fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            val b = SpanStyle(fontWeight = FontWeight.Bold)
            Text(
                buildAnnotatedString {
                    when (def.category) {
                        "workout" -> { append("Completa "); withStyle(b) { append("${def.threshold} allenamenti") }; append(" in totale. 💪") }
                        "ice" -> { append("Non registrare alcun evento per "); withStyle(b) { append("${def.threshold} giorni di fila") }; append(". Le medaglie di ghiaccio sono le più preziose. ❄️") }
                        else -> { append("Registra almeno un evento ogni giorno per "); withStyle(b) { append("${def.threshold} giorni di fila") }; append(". 🔥") }
                    }
                },
                color = Sbo.mutedFg, fontSize = 14.sp, lineHeight = 20.sp,
            )
        }
    }
}

private fun ease(x: Float) = 0.5f - 0.5f * kotlin.math.cos(x.coerceIn(0f, 1f) * Math.PI.toFloat())

private val grayPaint = androidx.compose.ui.graphics.Paint().apply {
    colorFilter = androidx.compose.ui.graphics.ColorFilter.colorMatrix(androidx.compose.ui.graphics.ColorMatrix().apply { setToSaturation(0f) })
}

@Composable
private fun GrayEmoji(emoji: String) {
    Box(Modifier.drawWithContent {
        drawIntoCanvas { c -> c.saveLayer(androidx.compose.ui.geometry.Rect(Offset.Zero, size), grayPaint); drawContent(); c.restore() }
    }) { Text(emoji, fontSize = 72.sp, lineHeight = 72.sp) }
}

/** Little ✦ twinkling at fixed random spots (web FloatingSparkles). */
@Composable
private fun BoxScope.FloatingSparkles(seed: Int, count: Int, clock: androidx.compose.runtime.State<Float>) {
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.matchParentSize()) {
        val w = maxWidth; val h = maxHeight
        for (i in 0 until count) {
            val r = { k: Int -> val x = kotlin.math.sin(seed * 12.9898 + (i + k) * 78.233) * 43758.5453; (x - kotlin.math.floor(x)).toFloat() }
            val fx = r(1); val fy = r(2); val dur = 1.6f + r(3) * 2f; val delay = r(4) * 2f; val fs = 8f + r(5) * 8f
            Text(
                "✦", color = Color.White, fontSize = fs.sp, lineHeight = fs.sp,
                modifier = Modifier.graphicsLayer {
                    val ph = (((clock.value - delay) / dur) % 1f + 1f) % 1f
                    val k = 0.5f - 0.5f * kotlin.math.cos(ph * 2f * Math.PI.toFloat())
                    translationX = fx * (w - 12.dp).toPx()
                    translationY = fy * (h - 12.dp).toPx() - 8.dp.toPx() * k
                    alpha = k * 0.9f; scaleX = 0.5f + 0.5f * k; scaleY = scaleX
                },
            )
        }
    }
}
