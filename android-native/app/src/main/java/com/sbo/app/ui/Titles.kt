package com.sbo.app.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sbo.app.data.SboEvent
import com.sbo.app.logic.Streaks
import com.sbo.app.logic.Tier
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Honorific titles from the TOTALS (web src/lib/sbo-titles.ts): fire = total events, ice = total clean days. */
data class TitleDef(val category: String, val threshold: Int, val tier: Tier, val name: String, val emoji: String, val gradient: List<Color>) {
    val id get() = "$category:$threshold"
    val tierLabel get() = when (tier) {
        Tier.common -> "Comune"; Tier.uncommon -> "Non comune"; Tier.rare -> "Raro"; Tier.super_rare -> "Super raro"
        Tier.epic -> "Epico"; Tier.mythic -> "Mitico"; Tier.legendary -> "Leggendario"
    }
}

private fun c(h: Long) = Color(0xFF000000 or h)

private val grayPaint = androidx.compose.ui.graphics.Paint().apply {
    colorFilter = androidx.compose.ui.graphics.ColorFilter.colorMatrix(androidx.compose.ui.graphics.ColorMatrix().apply { setToSaturation(0f) })
}

object Titles {
    val FIRE = listOf(
        TitleDef("fire", 5, Tier.common, "Apprendista della Forgia", "🪓", listOf(c(0x9ca3af), c(0x4b5563))),
        TitleDef("fire", 15, Tier.common, "Guardiano della Brace", "🪔", listOf(c(0xa3a3a3), c(0x525252))),
        TitleDef("fire", 30, Tier.uncommon, "Il Fabbro Modello", "🔨", listOf(c(0x4ade80), c(0x16a34a))),
        TitleDef("fire", 60, Tier.uncommon, "Maestro delle Fiamme", "🔥", listOf(c(0x34d399), c(0x059669))),
        TitleDef("fire", 100, Tier.rare, "Domatore di Fornaci", "⚒️", listOf(c(0x38bdf8), c(0x0284c7))),
        TitleDef("fire", 200, Tier.super_rare, "Signore del Magma", "🌋", listOf(c(0x60a5fa), c(0x2563eb))),
        TitleDef("fire", 350, Tier.epic, "Arcano dell'Incendio Eterno", "⚡", listOf(c(0xa78bfa), c(0x7c3aed))),
        TitleDef("fire", 500, Tier.mythic, "Titano della Combustione", "☄️", listOf(c(0xf472b6), c(0xa855f7), c(0xfacc15))),
        TitleDef("fire", 750, Tier.mythic, "Imperatore del Sole Nero", "🌑", listOf(c(0xf472b6), c(0xc084fc), c(0x38bdf8))),
        TitleDef("fire", 1000, Tier.legendary, "Divinità Incandescente Primordiale", "👑", listOf(c(0xfbbf24), c(0xf59e0b), c(0xd97706))),
        TitleDef("fire", 2000, Tier.legendary, "Cataclisma Vivente dei Mille Soli", "🌞", listOf(c(0xfacc15), c(0xf97316), c(0xdc2626), c(0xa855f7))),
    )
    val ICE = listOf(
        TitleDef("ice", 3, Tier.common, "Novizio della Brina", "🌨️", listOf(c(0xbae6fd), c(0x7dd3fc))),
        TitleDef("ice", 10, Tier.uncommon, "Custode del Gelo", "❄️", listOf(c(0x7dd3fc), c(0x0ea5e9))),
        TitleDef("ice", 25, Tier.rare, "Scultore di Cristalli", "💎", listOf(c(0xa5f3fc), c(0x22d3ee))),
        TitleDef("ice", 50, Tier.super_rare, "Sentinella dell'Iceberg", "🏔️", listOf(c(0xe0f2fe), c(0x38bdf8), c(0x1e3a8a))),
        TitleDef("ice", 100, Tier.epic, "Sovrano del Ghiacciaio Perenne", "🧊", listOf(c(0xc7d2fe), c(0x818cf8), c(0x4f46e5))),
        TitleDef("ice", 200, Tier.mythic, "Colosso dell'Inverno Infinito", "🐻‍❄️", listOf(c(0xffffff), c(0xe0f2fe), c(0x38bdf8), c(0x6366f1))),
        TitleDef("ice", 365, Tier.legendary, "Monarca dello Zero Assoluto", "🌌", listOf(c(0xffffff), c(0xa5f3fc), c(0x818cf8), c(0xc026d3))),
        TitleDef("ice", 730, Tier.legendary, "Entità Glaciale Oltre il Tempo", "🪐", listOf(c(0xffffff), c(0xbae6fd), c(0x6366f1), c(0x0f172a))),
    )
    val ALL = FIRE + ICE
    fun byId(id: String?) = ALL.firstOrNull { it.id == id }
    fun current(list: List<TitleDef>, total: Int) = list.lastOrNull { total >= it.threshold }
    fun next(list: List<TitleDef>, total: Int) = list.firstOrNull { total < it.threshold }

    /** Days without events from the first event to today (web totalCleanDays). */
    fun cleanDays(events: List<SboEvent>, today: LocalDate = LocalDate.now()): Int {
        if (events.isEmpty()) return 0
        val byDay = Streaks.byDay(events)
        val first = byDay.keys.min()
        val days = ChronoUnit.DAYS.between(first, today).toInt() + 1
        return (0 until days).count { !byDay.containsKey(first.plusDays(it.toLong())) }
    }
}

/**
 * The equipped title under a nickname (web TitleBadge): emoji + name painted with the tier's gradient;
 * mythic/legendary flow the gradient, rare+ glow softly, super rare+ get a sheen, mythic+ get sparks.
 */
@Composable
fun TitleBadge(t: TitleDef, variant: String = "sm", modifier: Modifier = Modifier) {
    val fs: TextUnit = when (variant) { "md" -> 14.sp; "sm" -> 12.sp; else -> 10.sp }
    val flow = t.tier == Tier.mythic || t.tier == Tier.legendary
    val sheenOn = t.tier.rank >= Tier.super_rare.rank
    val glowOn = t.tier == Tier.rare || t.tier.rank >= Tier.epic.rank
    val sparks = flow
    val tr = rememberInfiniteTransition(label = "title")
    val flowP by tr.animateFloat(0f, 1f, infiniteRepeatable(tween(4000, easing = LinearEasing)), label = "flow")
    val glow by tr.animateFloat(0f, 1f, infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "glow")
    val sheen by tr.animateFloat(0f, 1f, infiniteRepeatable(keyframes { durationMillis = 3600; 0f at 0 using FastOutSlowInEasing; 1f at 1080; 1f at 3600 }), label = "sheen")
    val spark = tr.animateFloat(0f, 1000f, infiniteRepeatable(tween(1_000_000, easing = LinearEasing)), label = "spark")
    Row(
        modifier.clip(RoundedCornerShape(2.dp)).drawWithContent {
            drawContent()
            if (sheenOn) {
                // 32px white band sweeping left → right (translateX -130% → 240% of its width, skewed)
                val bw = 32.dp.toPx()
                val x = (-1.3f + 3.7f * sheen) * bw
                drawRect(
                    Brush.horizontalGradient(0f to Color.Transparent, 0.5f to Color.White.copy(alpha = 0.55f), 1f to Color.Transparent, startX = x, endX = x + bw),
                    topLeft = Offset(x, 0f), size = androidx.compose.ui.geometry.Size(bw, size.height),
                )
            }
            if (sparks) listOf(0.12f, 0.42f, 0.72f).forEachIndexed { i, left ->
                val dur = 2f + i * 0.4f
                val ph = (((spark.value - i * 0.5f) / dur) % 1f + 1f) % 1f
                val k = 0.5f - 0.5f * kotlin.math.cos(ph * 2f * Math.PI.toFloat())
                drawCircle(Color.White.copy(alpha = k), radius = 2.dp.toPx() * (0.5f + 0.5f * k), center = Offset(size.width * left + 2.dp.toPx(), 2.dp.toPx() - 6.dp.toPx() * k))
            }
        },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(t.emoji, fontSize = fs, lineHeight = fs * 1.3f)
        Spacer(Modifier.width(4.dp))
        Box {
            // glow: soft halo in the gradient's first colour (CSS drop-shadow 2 → 10 px)
            Text(
                t.name, fontSize = fs, lineHeight = fs * 1.4f, fontWeight = FontWeight.SemiBold, letterSpacing = fs * 0.025f, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }.drawWithContent {
                    drawContent()
                    val w = size.width
                    // gradient text: background-size 200% when flowing, sliding by its own width over 4 s
                    val span = if (flow) w * 2f else w
                    val off = if (flow) -flowP * w * 2f else 0f
                    val brush = if (flow) Brush.linearGradient(t.gradient + t.gradient, start = Offset(off, 0f), end = Offset(off + span * 2f, size.height * 0.5f))
                    else Brush.linearGradient(t.gradient, start = Offset.Zero, end = Offset(w, size.height))
                    drawRect(brush, blendMode = BlendMode.SrcIn)
                },
            )
        }
    }
}

@Composable
private fun Label(text: String) = Text(text.uppercase(), color = Sbo.mutedFg, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp)

/** Titles section at the bottom of the medal inventory (web TitlesSection). */
@Composable
fun TitlesSection(totalEvents: Int, totalClean: Int, equipped: String?, onEquip: (String?) -> Unit) {
    val eq = Titles.byId(equipped)
    Spacer(Modifier.height(24.dp))
    Box(Modifier.fillMaxWidth().height(1.dp).background(Sbo.border))
    Spacer(Modifier.height(8.dp))
    Text("🏅 Titoli", fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold)
    Text("Si sbloccano con il totale complessivo, non con le streak.", color = Sbo.mutedFg, fontSize = 11.sp, lineHeight = 16.sp)
    Spacer(Modifier.height(12.dp))
    Column(
        Modifier.fillMaxWidth().drawBehind {
            val w = 2.dp.toPx()
            drawRoundRect(
                Color.White.copy(alpha = 0.10f), topLeft = Offset(w / 2, w / 2), size = androidx.compose.ui.geometry.Size(size.width - w, size.height - w),
                cornerRadius = CornerRadius(24.dp.toPx()), style = Stroke(w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))),
            )
        }.padding(12.dp),
    ) {
        Text("SLOT TITOLO", color = Sbo.mutedFg, fontSize = 10.sp, lineHeight = 14.sp, letterSpacing = 0.5.sp)
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth().height(28.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                if (eq != null) TitleBadge(eq, "md") else Text("Nessun titolo equipaggiato", color = Sbo.mutedFg, fontSize = 12.sp, lineHeight = 16.sp)
            }
            if (eq != null) SmallButton("Rimuovi", Lucide.X, variant = "ghost", height = 28.dp, fontSize = 11) { onEquip(null) }
        }
        Spacer(Modifier.height(4.dp))
        Text("Appare sotto il tuo nome. Tocca un titolo sbloccato qui sotto per equipaggiarlo.", color = Sbo.mutedFg, fontSize = 10.sp, lineHeight = 14.sp)
    }
    TitleGroup("🔥 Titoli di fuoco", Titles.FIRE, totalEvents, "eventi", equipped, onEquip)
    TitleGroup("❄️ Titoli di ghiaccio", Titles.ICE, totalClean, "giorni puliti", equipped, onEquip)
}

@Composable
private fun TitleGroup(title: String, list: List<TitleDef>, total: Int, unit: String, equipped: String?, onEquip: (String?) -> Unit) {
    val cur = Titles.current(list, total)
    val next = Titles.next(list, total)
    Spacer(Modifier.height(16.dp))
    Label(title)
    Spacer(Modifier.height(8.dp))
    Column(Modifier.fillMaxWidth().border(1.dp, Sbo.border, RoundedCornerShape(24.dp)).padding(12.dp)) {
        Text(
            androidx.compose.ui.text.buildAnnotatedString {
                append("Totale: ")
                pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold, color = Sbo.foreground)); append("$total"); pop()
                append(" $unit")
            },
            color = Sbo.mutedFg, fontSize = 12.sp, lineHeight = 16.sp,
        )
        Spacer(Modifier.height(4.dp))
        Text(if (cur != null) "${cur.emoji} ${cur.name}" else "Nessun titolo — ancora", fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold)
        if (cur != null) Text(cur.tierLabel, color = Sbo.mutedFg, fontSize = 11.sp, lineHeight = 16.sp)
        if (next != null) {
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(Sbo.muted)) {
                Box(Modifier.fillMaxWidth((total.toFloat() / next.threshold).coerceIn(0f, 1f)).fillMaxHeight().clip(CircleShape).background(Sbo.primary))
            }
            Spacer(Modifier.height(4.dp))
            Text(
                androidx.compose.ui.text.buildAnnotatedString {
                    append("Prossimo: ")
                    pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold)); append("${next.emoji} ${next.name}"); pop()
                    append(" a ${next.threshold} $unit")
                },
                color = Sbo.mutedFg, fontSize = 11.sp, lineHeight = 16.sp,
            )
        }
    }
    Spacer(Modifier.height(8.dp))
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        list.forEach { t ->
            val owned = total >= t.threshold
            val on = equipped == t.id
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                    .then(if (on) Modifier.background(Sbo.primary.copy(alpha = 0.10f)) else Modifier)
                    .border(1.dp, if (on) Sbo.primary else Sbo.border, RoundedCornerShape(20.dp))
                    .then(if (owned) Modifier.clickable { onEquip(if (on) null else t.id) } else Modifier)
                    .alpha(if (owned) 1f else 0.5f)
                    .padding(horizontal = 10.dp, vertical = if (owned) 10.dp else 9.5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (owned) Text(t.emoji, fontSize = 16.sp, lineHeight = 24.sp)
                else Box(Modifier.graphicsLayer { alpha = 0.6f }.drawWithContent {
                    drawIntoCanvas { c -> c.saveLayer(androidx.compose.ui.geometry.Rect(Offset.Zero, size), grayPaint); drawContent(); c.restore() }
                }) { Text(t.emoji, fontSize = 16.sp, lineHeight = 24.sp) }
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    if (owned) TitleBadge(t, "sm") else Text(t.name, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${t.tierLabel} · ${t.threshold} $unit", color = Sbo.mutedFg, fontSize = 10.sp, lineHeight = 14.sp)
                }
                if (!owned) Icon(Lucide.Lock, null, tint = Sbo.mutedFg, modifier = Modifier.size(12.dp))
                if (on) Text(
                    "Equipaggiato", color = Sbo.primaryFg, fontSize = 9.sp, lineHeight = 12.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Sbo.primary).padding(horizontal = 4.dp, vertical = 2.dp),
                )
            }
        }
    }
}
