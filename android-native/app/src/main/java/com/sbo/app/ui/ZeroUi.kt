@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.sbo.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sbo.app.R
import com.sbo.app.data.AgentMessage
import com.sbo.app.data.CustomWorkout
import com.sbo.app.data.WorkoutDay
import com.sbo.app.logic.DayPlan
import com.sbo.app.logic.League
import com.sbo.app.logic.Zero
import com.sbo.app.logic.ZeroAnswers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

fun zeroColor(status: String) = when (status) { "over" -> Sbo.zeroOver; "limit" -> Sbo.zeroLimit; else -> Sbo.zeroOk }

private fun fmtAmount(v: Double) = if (v == Math.floor(v)) v.toLong().toString() else v.toString()

// ---------------------------------------------------------------- header chips (PZ mode)

@Composable
fun ZeroChip(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, text: String) {
    Row(
        Modifier.height(38.dp).clip(CircleShape).surface().border(1.dp, Sbo.border, CircleShape).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ---------------------------------------------------------------- banner

@Composable
fun ZeroBanner(day: Int, league: League, weekCount: Int, limit: Int, workoutsWeek: Int) {
    val status = if (weekCount > limit) "over" else if (weekCount == limit) "limit" else "ok"
    val t = rememberInfiniteTransition(label = "ping")
    val ping by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1000, easing = LinearEasing)), label = "p")
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Sbo.card.copy(alpha = 0.9f))
            .drawBehind {
                val c = Offset(size.width - 24.dp.toPx(), 24.dp.toPx())
                drawCircle(Brush.radialGradient(0f to Sbo.primary.copy(alpha = 0.2f), 1f to Color.Transparent, center = c, radius = 104.dp.toPx()), 104.dp.toPx(), c)
            }
            .border(1.dp, Sbo.primary.copy(alpha = 0.3f), RoundedCornerShape(28.dp)).padding(horizontal = 16.dp, vertical = 20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp), contentAlignment = Alignment.Center) {
                        // animate-ping: a copy scales to 2x and fades out every second
                        Box(Modifier.size(8.dp).graphicsLayer { val k = 1f + ping; scaleX = k; scaleY = k; alpha = 0.6f * (1f - ping) }.clip(CircleShape).background(Sbo.primary))
                        Box(Modifier.size(8.dp).clip(CircleShape).background(Sbo.primary))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("PROGETTO ZERO IN CORSO", color = Sbo.primary, fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.2.sp)
                }
                Spacer(Modifier.height(7.dp))
                Text("Giorno $day", fontFamily = SpaceGrotesk, fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold)
                Text("${league.label} · $workoutsWeek allenamenti questa settimana", color = Sbo.mutedFg, fontSize = 12.sp, lineHeight = 16.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("SETTIMANA", color = Sbo.mutedFg, fontSize = 10.sp, lineHeight = 14.sp, letterSpacing = 0.5.sp)
                Text(
                    androidx.compose.ui.text.buildAnnotatedString {
                        append("$weekCount")
                        pushStyle(androidx.compose.ui.text.SpanStyle(fontSize = 16.sp, color = Sbo.mutedFg)); append("/$limit"); pop()
                    },
                    fontFamily = SpaceGrotesk, fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold, color = zeroColor(status),
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(maxOf(limit, weekCount)) { i ->
                        Box(Modifier.size(width = 12.dp, height = 6.dp).clip(CircleShape).background(if (i < weekCount) zeroColor(if (i >= limit) "over" else status) else Sbo.muted))
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- two buttons

@Composable
fun ZeroButtons(weekCount: Int, limit: Int, workoutDone: Boolean, rest: Boolean, onSbo: () -> Unit, onWorkout: () -> Unit) {
    val status = if (weekCount > limit) "over" else if (weekCount == limit) "limit" else "ok"
    val col = zeroColor(status)
    val label = when (status) { "over" -> "Oltre il limite"; "limit" -> "Al limite"; else -> "Ancora ${limit - weekCount} di margine" }
    var hit by remember { mutableIntStateOf(0) }
    val t = rememberInfiniteTransition(label = "zb")
    // over: shake (0.5 s) every 3 s; pulsing ring every 1.2 s
    val shake by t.animateFloat(0f, 3f, infiniteRepeatable(tween(3000, easing = LinearEasing)), label = "shake")
    val ring by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1200, easing = LinearEasing)), label = "ring")
    val spin by t.animateFloat(0f, 360f, infiniteRepeatable(tween(18000, easing = LinearEasing)), label = "spin")
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            val press = rememberPressScale()
            val flash = remember { Animatable(1f) }
            LaunchedEffect(hit) { if (hit > 0) { flash.snapTo(0f); flash.animateTo(1f, tween(600)) } }
            Box(
                Modifier.widthIn(max = 160.dp).fillMaxWidth().aspectRatio(1f)
                    .graphicsLayer {
                        // web whileTap 0.88 (our spring dips to 0.9; stretch it to 0.88)
                        val k = 1f - (1f - press.value) * 1.2f
                        scaleX = k; scaleY = k
                        if (status == "over" && shake < 0.5f) { val p = shake / 0.5f; translationX = 4.dp.toPx() * kotlin.math.sin(p * 4f * Math.PI.toFloat()) * (1f - p) }
                    }
                    .drawBehind {
                        // glow: 0 20px 60px -15px status, 0 0 40/80px -10px status
                        drawCircle(Brush.radialGradient(0f to col.copy(alpha = 0.45f), 1f to Color.Transparent, center = Offset(center.x, center.y + 20.dp.toPx()), radius = size.minDimension * 0.7f), size.minDimension * 0.7f, Offset(center.x, center.y + 20.dp.toPx()))
                    }
                    .clip(CircleShape)
                    .drawBehind {
                        drawRect(Brush.radialGradient(0f to lerp(col, Color.White, 0.3f), 0.55f to col, 1f to lerp(col, Color.Black, 0.5f), center = Offset(size.width * 0.35f, size.height * 0.3f), radius = size.maxDimension * 0.85f))
                        if (status == "over") {
                            val k = 1f + 0.25f * ring
                            drawCircle(Sbo.zeroOver.copy(alpha = 0.8f * (1f - ring)), size.minDimension / 2f * k - 2.dp.toPx(), style = Stroke(4.dp.toPx()))
                        }
                        if (hit > 0 && flash.value < 1f) drawCircle(Color.White.copy(alpha = 0.4f * 0.8f / 0.8f * (1f - flash.value)), size.minDimension / 2f * (0.2f + 1.2f * flash.value))
                    }
                    .pressable(press, "Aggiungi evento SBO") { hit++; onSbo() },
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Lucide.Droplets, null, tint = Color.White, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.height(4.dp))
                    Text("$weekCount/$limit", fontFamily = SpaceGrotesk, fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold, color = Color.White,
                        style = TextStyle(shadow = androidx.compose.ui.graphics.Shadow(Color.Black.copy(alpha = 0.5f), Offset(0f, 4f), 12f)))
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(label, color = col, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            val press = rememberPressScale()
            Box(
                Modifier.widthIn(max = 160.dp).fillMaxWidth().aspectRatio(1f).graphicsScale(press)
                    .drawBehind { drawCircle(Brush.radialGradient(0f to Color.Black.copy(alpha = 0.35f), 1f to Color.Transparent, center = Offset(center.x, center.y + 8.dp.toPx()), radius = size.minDimension * 0.62f), size.minDimension * 0.62f, Offset(center.x, center.y + 8.dp.toPx())) }
                    .clip(CircleShape)
                    .drawBehind {
                        val c = Offset(size.width * 0.35f, size.height * 0.3f)
                        if (workoutDone) drawRect(Brush.radialGradient(0f to lerp(Sbo.zeroOk, Color.White, 0.35f), 0.6f to Sbo.zeroOk, 1f to lerp(Sbo.zeroOk, Color.Black, 0.55f), center = c, radius = size.maxDimension * 0.85f))
                        else drawRect(Brush.radialGradient(0f to lerp(Sbo.card, Sbo.primary, 0.35f), 0.75f to Sbo.card, 1f to Sbo.card, center = c, radius = size.maxDimension * 0.85f))
                        if (!workoutDone && !rest) rotate(spin) {
                            drawCircle(Sbo.primary.copy(alpha = 0.6f), size.minDimension / 2f - 9.dp.toPx(), style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))))
                        }
                    }
                    .border(2.dp, Sbo.primary.copy(alpha = 0.5f), CircleShape)
                    .pressable(press, "Allenamento", onWorkout),
                contentAlignment = Alignment.Center,
            ) {
                val fg = if (workoutDone) Color.White else Sbo.primary
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(if (workoutDone) Lucide.CheckBold else if (rest) Lucide.Moon else Lucide.Dumbbell, null, tint = fg, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.height(4.dp))
                    Text(if (workoutDone) "FATTO" else if (rest) "RIPOSO" else "ALLENATI", color = fg, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(if (workoutDone) "Allenamento completato" else if (rest) "Tocca per allenamento libero" else "Apri la scheda di oggi", color = Sbo.mutedFg, fontSize = 12.sp, lineHeight = 16.sp, textAlign = TextAlign.Center)
        }
    }
}

// ---------------------------------------------------------------- today's plan

@Composable
private fun PlanList(plan: DayPlan, wd: WorkoutDay?, onToggle: (String) -> Unit, onSkip: ((String) -> Unit)?) {
    val done = (wd?.done_ids ?: emptyList()).toSet()
    val skipUsed = done.any { it.startsWith("skip:") }
    if (plan.rest) {
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Sbo.secondary.copy(alpha = 0.4f)).padding(16.dp)) {
            Icon(Lucide.Moon, null, tint = Sbo.primary, modifier = Modifier.padding(top = 2.dp).size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text(plan.restTip ?: "", color = Sbo.mutedFg, fontSize = 14.sp, lineHeight = 20.sp)
        }
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        plan.exercises.forEach { e ->
            val ok = e.id in done
            val skipped = "skip:${e.id}" in done
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    Modifier.weight(1f).clip(RoundedCornerShape(24.dp))
                        .background(if (ok) Sbo.zeroOk.copy(alpha = 0.15f) else Sbo.secondary.copy(alpha = 0.3f))
                        .border(1.dp, if (ok) Color.Transparent else Sbo.border, RoundedCornerShape(24.dp))
                        .clickable { onToggle(e.id) }.padding(horizontal = 12.dp, vertical = 15.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(28.dp).clip(CircleShape).then(if (ok) Modifier.background(Sbo.zeroOk) else Modifier)
                            .border(2.dp, if (ok) Sbo.zeroOk else Sbo.mutedFg.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { if (ok) Icon(Lucide.CheckBold, null, tint = Color.White, modifier = Modifier.size(16.dp)) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(e.name, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, color = Sbo.foreground.copy(alpha = if (ok) 0.6f else 1f), textDecoration = if (ok) TextDecoration.LineThrough else null)
                        Spacer(Modifier.height(2.dp))
                        Text(e.tip, color = Sbo.mutedFg, fontSize = 11.sp, lineHeight = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        if (skipped) androidx.compose.ui.text.AnnotatedString("skip") else androidx.compose.ui.text.buildAnnotatedString {
                            append((if (e.sets > 1) "${e.sets}×" else "") + fmtAmount(e.amount))
                            pushStyle(androidx.compose.ui.text.SpanStyle(fontSize = 10.sp, fontWeight = FontWeight.Normal, color = Sbo.mutedFg))
                            append(when (e.unit) { "sec" -> "s"; "min" -> " min"; "km" -> " km"; else -> "" }); pop()
                        },
                        fontFamily = FontFamily.Monospace, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold,
                    )
                }
                if (onSkip != null && !ok && !skipUsed) {
                    Spacer(Modifier.width(8.dp))
                    Text("Skip", color = Sbo.mutedFg, fontSize = 10.sp, lineHeight = 14.sp,
                        modifier = Modifier.clip(CircleShape).border(1.dp, Sbo.border, CircleShape).clickable { onSkip(e.id) }.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }
        }
    }
}

@Composable
fun TodayPlanCard(plan: DayPlan, wd: WorkoutDay?, onToggle: (String) -> Unit, onSkip: (String) -> Unit, onCustom: () -> Unit, onExpand: () -> Unit) {
    val doneN = plan.exercises.count { it.id in (wd?.done_ids ?: emptyList()) }
    Column(Modifier.fillMaxWidth().clip(CardShapeZ).surface().border(1.dp, Sbo.primary.copy(alpha = 0.25f), CardShapeZ).padding(16.dp)) {
        Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("OBIETTIVI DI OGGI", color = Sbo.primary, fontSize = 10.sp, lineHeight = 14.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp)
                Text(
                    androidx.compose.ui.text.buildAnnotatedString {
                        append(plan.title)
                        plan.intensity?.let { pushStyle(androidx.compose.ui.text.SpanStyle(fontFamily = Inter, fontSize = 12.sp, fontWeight = FontWeight.Normal, color = Sbo.mutedFg)); append("  · $it"); pop() }
                    },
                    fontFamily = SpaceGrotesk, fontSize = 18.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold,
                )
            }
            if (!plan.rest) { Text("$doneN/${plan.exercises.size}", fontFamily = FontFamily.Monospace, color = Sbo.mutedFg, fontSize = 12.sp); Spacer(Modifier.width(4.dp)) }
            Box(Modifier.size(32.dp).clip(RoundedCornerShape(18.dp)).clickable(onClick = onExpand), contentAlignment = Alignment.Center) {
                Icon(Lucide.Maximize2, "Apri a schermo intero", tint = Sbo.foreground, modifier = Modifier.size(16.dp))
            }
        }
        PlanList(plan, wd, onToggle, onSkip)
        if ((wd?.custom?.size ?: 0) > 0) {
            Spacer(Modifier.height(12.dp))
            wd!!.custom.forEach { c ->
                Row(Modifier.padding(bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Lucide.Check, null, tint = Sbo.zeroOk, modifier = Modifier.size(14.dp)); Spacer(Modifier.width(8.dp))
                    Text(c.name + (c.note?.takeIf { it.isNotBlank() }?.let { " — $it" } ?: ""), color = Sbo.mutedFg, fontSize = 12.sp, lineHeight = 16.sp)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth().height(32.dp).clip(RoundedCornerShape(18.dp)).clickable(onClick = onCustom), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(Lucide.Plus, null, tint = Sbo.mutedFg, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(14.dp))
            Text("Aggiungi un allenamento tuo", color = Sbo.mutedFg, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}

private val CardShapeZ = RoundedCornerShape(24.dp)

/** Full-screen plan (opens by itself on the first visit of the day). */
@Composable
fun PlanOverlay(plan: DayPlan, wd: WorkoutDay?, name: String, day: Int, onToggle: (String) -> Unit, onSkip: (String) -> Unit, onCustom: () -> Unit, onClose: () -> Unit) {
    androidx.activity.compose.BackHandler(onBack = onClose)
    val a = rememberPanelProgress()
    val doneN = plan.exercises.count { it.id in (wd?.done_ids ?: emptyList()) }
    val pct = if (plan.rest) 100 else Math.round(doneN * 100f / maxOf(1, plan.exercises.size))
    val ring by animateFloatAsState(pct / 100f, tween(600), label = "ring")
    Box(Modifier.fillMaxSize().graphicsLayer { alpha = a().coerceIn(0f, 1f) }.background(Sbo.background.copy(alpha = 0.95f)).clickable(remember { MutableInteractionSource() }, indication = null) {}) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding()
                .graphicsLayer { val v = a(); translationY = (1f - v) * 48.dp.toPx(); val k = 0.96f + 0.04f * v; scaleX = k; scaleY = k }
                .padding(start = 24.dp, end = 24.dp, top = 40.dp, bottom = 64.dp),
        ) {
            Row {
                Column(Modifier.weight(1f)) {
                    Text("PROGETTO ZERO · GIORNO $day", color = Sbo.primary, fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.75.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(if (plan.rest) "Oggi riposi" else plan.title, fontFamily = SpaceGrotesk, fontSize = 36.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(if (plan.rest) "$name, il recupero fa parte del piano." else "$name, ecco la tua scheda. Spunta ogni esercizio quando lo finisci.", color = Sbo.mutedFg, fontSize = 14.sp, lineHeight = 20.sp)
                }
                Spacer(Modifier.width(12.dp))
                Box(Modifier.size(36.dp).clip(CircleShape).background(Sbo.secondary).clickable(onClick = onClose), contentAlignment = Alignment.Center) {
                    Icon(Lucide.X, "Chiudi", tint = Sbo.foreground, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.height(34.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(80.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.size(80.dp)) {
                        val sw = size.width * 3f / 36f
                        val r = size.width * 15.5f / 36f
                        drawCircle(Sbo.muted, r, style = Stroke(sw))
                        drawArc(Sbo.primary, -90f, 360f * ring, false, topLeft = Offset(center.x - r, center.y - r), size = androidx.compose.ui.geometry.Size(r * 2, r * 2), style = Stroke(sw, cap = StrokeCap.Round))
                    }
                    Text("$pct%", fontFamily = SpaceGrotesk, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(16.dp))
                Text(
                    if (plan.rest) "Niente da spuntare oggi. Se vuoi comunque muoverti, aggiungi un allenamento libero." else if (pct == 100) "Fatto tutto. Il coach è fiero di te." else "$doneN di ${plan.exercises.size} esercizi completati.",
                    color = Sbo.mutedFg, fontSize = 14.sp, lineHeight = 20.sp,
                )
            }
            Spacer(Modifier.height(24.dp))
            PlanList(plan, wd, onToggle, onSkip)
            Spacer(Modifier.height(16.dp))
            SboButton("Aggiungi un allenamento tuo", Lucide.Plus, variant = "secondary", onClick = onCustom)
            Spacer(Modifier.height(8.dp))
            SboButton("Vai alla home", onClick = onClose)
        }
    }
}

@Composable
private fun Chip(text: String, on: Boolean, onClick: () -> Unit) {
    Text(
        text, color = if (on) Sbo.primary else Sbo.foreground, fontSize = 12.sp, lineHeight = 16.sp,
        modifier = Modifier.clip(CircleShape).then(if (on) Modifier.background(Sbo.primary.copy(alpha = 0.15f)) else Modifier)
            .border(1.dp, if (on) Sbo.primary else Sbo.border, CircleShape).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 4.dp),
    )
}

@Composable
fun CustomWorkoutDialog(onClose: () -> Unit, onSave: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    SboDialog(onClose) {
        DialogTitle("Allenamento libero")
        DialogDesc("Ti sei allenato per conto tuo? Vale esattamente come quello del piano.")
        Spacer(Modifier.height(16.dp))
        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Palestra", "Flessioni", "Corsa", "Calcio", "Nuoto", "Bici", "Pesi", "Arti marziali").forEach { p -> Chip(p, name == p) { name = p } }
        }
        Spacer(Modifier.height(16.dp))
        FieldLabel("Cosa hai fatto"); Spacer(Modifier.height(8.dp))
        SboInput(name, { name = it.take(60) }, "Es. 100 flessioni in palestra", Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        FieldLabel("Note (facoltative)"); Spacer(Modifier.height(8.dp))
        SboInput(note, { note = it.take(120) }, "Durata, serie, sensazioni…", Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
        SboButton("Salva allenamento", enabled = name.isNotBlank()) { onSave(name.trim(), note.trim()) }
    }
}

// ---------------------------------------------------------------- coach

@Composable
private fun CoachAvatar(size: Int) {
    Image(painterResource(R.drawable.coach_zero), "Coach Zero", contentScale = ContentScale.Crop, modifier = Modifier.size(size.dp).clip(CircleShape))
}

@Composable
fun AgentRow(messages: List<AgentMessage>, onOpen: () -> Unit) {
    val last = messages.lastOrNull()
    val unread = messages.count { it.read_at == null }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Sbo.card.copy(alpha = 0.9f)).border(1.dp, Sbo.primary.copy(alpha = 0.3f), RoundedCornerShape(28.dp))
            .clickable(onClick = onOpen).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            CoachAvatar(44)
            Box(Modifier.align(Alignment.BottomEnd).offset(x = 2.dp, y = 2.dp).size(12.dp).clip(CircleShape).background(Sbo.card).padding(2.dp).clip(CircleShape).background(Sbo.zeroOk))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Coach Zero", fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(8.dp))
                Text("TRAINER", color = Sbo.primary, fontSize = 9.sp, lineHeight = 12.sp, letterSpacing = 0.45.sp,
                    modifier = Modifier.clip(CircleShape).background(Sbo.primary.copy(alpha = 0.15f)).padding(horizontal = 6.dp))
            }
            Text(last?.content ?: "Ti scriverà presto.", color = Sbo.mutedFg, fontSize = 12.sp, lineHeight = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (unread > 0) {
            Spacer(Modifier.width(12.dp))
            Box(Modifier.height(20.dp).widthIn(min = 20.dp).clip(CircleShape).background(Sbo.destructive).padding(horizontal = 6.dp), contentAlignment = Alignment.Center) {
                Text("$unread", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private val DAY_FMT = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.ITALIAN)
private val HM = DateTimeFormatter.ofPattern("HH:mm")

@Composable
fun AgentChatDialog(messages: List<AgentMessage>, onClose: () -> Unit, onPoll: () -> Unit, onSend: suspend (String) -> Unit) {
    androidx.activity.compose.BackHandler(onBack = onClose)
    var draft by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val list = rememberLazyListState()
    LaunchedEffect(Unit) { while (true) { delay(5000); onPoll() } }
    val zone = ZoneId.systemDefault()
    val rows = remember(messages) {
        val out = mutableListOf<Pair<String?, AgentMessage?>>()
        var lastLabel: String? = null
        messages.forEach { m ->
            val lab = com.sbo.app.logic.Streaks.instant(m.created_at).atZone(zone).format(DAY_FMT)
            if (lab != lastLabel) { out += lab to null; lastLabel = lab }
            out += null to m
        }
        out
    }
    LaunchedEffect(rows.size, sending) { if (rows.isNotEmpty()) list.animateScrollToItem(rows.size - 1 + if (sending) 1 else 0) }
    val h = LocalConfiguration.current.screenHeightDp.dp * 0.85f
    val pop = rememberPanelProgress()
    Box(Modifier.fillMaxSize().scrim(pop).background(Color.Black.copy(alpha = 0.72f)).clickable(remember { MutableInteractionSource() }, indication = null, onClick = onClose).imePadding(), contentAlignment = Alignment.Center) {
        Column(Modifier.fillMaxWidth().height(h).panel(pop).background(Sbo.card).border(1.dp, Sbo.border).clickable(remember { MutableInteractionSource() }, indication = null) {}) {
            Box(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    CoachAvatar(40); Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Coach Zero", fontSize = 18.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.45).sp)
                        Spacer(Modifier.height(4.dp))
                        Text("Il tuo personal trainer · AI", color = Sbo.mutedFg, fontSize = 12.sp, lineHeight = 16.sp)
                    }
                }
                Icon(Lucide.X, "Chiudi", tint = Sbo.foreground.copy(alpha = 0.7f), modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).size(16.dp).clickable(onClick = onClose))
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Sbo.border))
            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (messages.isEmpty()) Text("Nessun messaggio ancora.", color = Sbo.mutedFg, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp))
                LazyColumn(state = list, modifier = Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, top = 33.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(rows.size) { i ->
                        val (label, m) = rows[i]
                        if (label != null) Text(label.uppercase(), color = Sbo.mutedFg, fontSize = 10.sp, lineHeight = 14.sp, letterSpacing = 0.5.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = if (i > 0) 8.dp else 0.dp))
                        if (m != null) {
                            val mine = m.kind == "user"
                            val angry = m.kind == "rant" || m.kind == "quit"
                            val shape = if (mine) RoundedCornerShape(28.dp, 4.dp, 28.dp, 28.dp) else RoundedCornerShape(4.dp, 28.dp, 28.dp, 28.dp)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
                                Column(
                                    Modifier.fillMaxWidth(0.85f).clip(shape)
                                        .background(if (mine) Sbo.primary else if (angry) Sbo.destructive.copy(alpha = 0.15f) else if (m.kind == "tip") Sbo.primary.copy(alpha = 0.10f) else Sbo.secondary)
                                        .then(if (angry) Modifier.border(1.dp, Sbo.destructive.copy(alpha = 0.5f), shape) else if (m.kind == "tip") Modifier.border(1.dp, Sbo.primary.copy(alpha = 0.3f), shape) else Modifier)
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                ) {
                                    Text(m.content, color = if (mine) Sbo.primaryFg else Sbo.foreground, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = if (angry) FontWeight.SemiBold else FontWeight.Normal)
                                    Text(
                                        com.sbo.app.logic.Streaks.instant(m.created_at).atZone(zone).format(HM), color = if (mine) Sbo.primaryFg.copy(alpha = 0.7f) else Sbo.mutedFg,
                                        fontSize = 10.sp, lineHeight = 14.sp, textAlign = TextAlign.End, modifier = Modifier.align(Alignment.End).padding(top = 8.dp, bottom = 2.dp),
                                    )
                                }
                            }
                        }
                    }
                    if (sending) item {
                        val tr = rememberInfiniteTransition(label = "typing")
                        val a by tr.animateFloat(0.5f, 1f, infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "a")
                        Text("Coach Zero sta scrivendo…", color = Sbo.mutedFg.copy(alpha = a), fontSize = 14.sp,
                            modifier = Modifier.clip(RoundedCornerShape(4.dp, 28.dp, 28.dp, 28.dp)).background(Sbo.secondary).padding(horizontal = 14.dp, vertical = 8.dp))
                    }
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Sbo.border))
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.Bottom) {
                val focus = remember { MutableInteractionSource() }
                val focused by focus.collectIsFocusedAsState()
                BasicTextField(
                    draft, { draft = it.take(1000) }, textStyle = TextStyle(fontFamily = Inter, fontSize = 14.sp, color = Sbo.foreground), cursorBrush = SolidColor(Sbo.primary), maxLines = 4,
                    interactionSource = focus,
                    modifier = Modifier.weight(1f).heightIn(min = 40.dp).clip(RoundedCornerShape(28.dp)).background(Sbo.background).border(1.dp, if (focused) Sbo.primary else Sbo.border, RoundedCornerShape(28.dp)).padding(horizontal = 14.dp, vertical = 10.dp),
                    decorationBox = { inner -> Box { if (draft.isEmpty()) Text("Scrivi al coach…", color = Sbo.mutedFg, fontSize = 14.sp); inner() } },
                )
                Spacer(Modifier.width(8.dp))
                val can = !sending && draft.isNotBlank()
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(Sbo.primary.copy(alpha = if (can) 1f else 0.4f)).clickable(enabled = can) {
                        val t = draft.trim(); draft = ""; sending = true
                        scope.launch { try { onSend(t) } finally { sending = false } }
                    },
                    contentAlignment = Alignment.Center,
                ) { Icon(Lucide.Send, "Invia", tint = Sbo.primaryFg, modifier = Modifier.size(16.dp)) }
            }
        }
    }
}

// ---------------------------------------------------------------- themes + questionnaire

@Composable
fun ThemePicker(value: String, onChange: (String) -> Unit) {
    Zero.THEMES.chunked(2).forEach { row ->
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            row.forEach { t ->
                val on = value == t.id
                Column(
                    Modifier.weight(1f).then(if (on) Modifier.border(4.dp, Sbo.primary.copy(alpha = 0.4f), RoundedCornerShape(26.dp)).padding(2.dp) else Modifier)
                        .clip(RoundedCornerShape(24.dp)).border(1.dp, if (on) Sbo.primary else Sbo.border, RoundedCornerShape(24.dp)).clickable { onChange(t.id) }.padding(horizontal = 12.dp, vertical = 14.dp),
                ) {
                    Row(Modifier.fillMaxWidth().height(32.dp).clip(RoundedCornerShape(18.dp))) { t.colors.forEach { c -> Box(Modifier.weight(1f).fillMaxHeight().background(Color(c))) } }
                    Spacer(Modifier.height(8.dp))
                    Text(t.name, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium)
                }
            }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

private data class Q(val key: String, val title: String, val hint: String, val min: Int, val max: Int, val step: Int, val unit: String)
private val QUESTIONS = listOf(
    Q("age", "Quanti anni hai?", "Serve per calibrare i valori di riferimento.", 12, 60, 1, "anni"),
    Q("pushups", "Quante flessioni fai di fila?", "Fatte bene, petto quasi a terra. Media 15-20 anni: 15-35.", 0, 100, 1, "flessioni"),
    Q("plank", "Quanti secondi tieni il plank?", "Corpo dritto sugli avambracci. Media 15-20 anni: 45-120s.", 0, 300, 5, "secondi"),
    Q("pullups", "Quante trazioni fai di fila?", "Mento sopra la sbarra. Media 15-20 anni: 3-10. Zero va benissimo.", 0, 40, 1, "trazioni"),
    Q("squats", "Quanti squat fai di fila?", "Squat a corpo libero, cosce parallele. Media: 30-60.", 0, 150, 5, "squat"),
    Q("weeklyNow", "Quante volte a settimana lo fai adesso?", "Sii onesto: serve a darti un limite iniziale realistico (max 5, ideale 3 o meno).", 0, 40, 1, "volte/settimana"),
)
private val WD = listOf("Dom", "Lun", "Mar", "Mer", "Gio", "Ven", "Sab")

private fun ZeroAnswers.get(k: String) = when (k) { "age" -> age; "pushups" -> pushups; "plank" -> plank; "pullups" -> pullups; "squats" -> squats; else -> weeklyNow }
private fun ZeroAnswers.set(k: String, v: Int) = when (k) { "age" -> copy(age = v); "pushups" -> copy(pushups = v); "plank" -> copy(plank = v); "pullups" -> copy(pullups = v); "squats" -> copy(squats = v); else -> copy(weeklyNow = v) }

@Composable
fun ZeroQuestionnaire(reassess: Boolean, redo: Boolean, initial: ZeroAnswers?, initialTheme: String?, onClose: () -> Unit, onSubmit: (ZeroAnswers, String, League, Int) -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    var ans by remember { mutableStateOf(initial ?: ZeroAnswers()) }
    var theme by remember { mutableStateOf(initialTheme ?: "obsidian") }
    val total = QUESTIONS.size + 4
    val league = Zero.computeLeague(ans)
    val limit = Zero.startLimitFrom(ans.weeklyNow)
    val progress by animateFloatAsState((step + 1f) / total, tween(300), label = "prog")
    SboDialog(onClose) {
        Box(Modifier.fillMaxWidth().padding(end = 24.dp).height(4.dp).clip(CircleShape).background(Sbo.muted)) {
            Box(Modifier.fillMaxWidth(progress).fillMaxHeight().background(Sbo.primary))
        }
        Spacer(Modifier.height(16.dp))
        val anim = remember(step) { Animatable(0f) }
        LaunchedEffect(step) { anim.animateTo(1f, tween(200)) }
        Column(Modifier.fillMaxWidth().heightIn(min = 280.dp).padding(vertical = 8.dp).graphicsLayer { alpha = anim.value; translationX = (1f - anim.value) * 30.dp.toPx() }) {
            val qi = step - 1
            when {
                step == 0 -> Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(Modifier.size(64.dp).clip(RoundedCornerShape(28.dp)).background(Sbo.primary.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                        Icon(Lucide.Target, null, tint = Sbo.primary, modifier = Modifier.size(32.dp))
                    }
                    Text(if (redo) "Progetto Zero aggiornato" else if (reassess) "Nuova valutazione" else "Progetto Zero", fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold)
                    when {
                        redo -> Text("Ci sono stati dei problemi tecnici e il Progetto Zero è stato aggiornato con tante novità (allenamenti nuovi, manubri, palestra, giorni di riposo). Per questo devi rifare il questionario. Ci vuole un minuto.", color = Sbo.mutedFg, fontSize = 14.sp, lineHeight = 20.sp)
                        reassess -> Text("È passato un mese. Rispondi con i tuoi numeri di oggi: il coach ricalcola la tua lega e il tuo piano.", color = Sbo.mutedFg, fontSize = 14.sp, lineHeight = 20.sp)
                        else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            val b = androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold, color = Sbo.foreground)
                            @Composable fun Li(t: androidx.compose.ui.text.AnnotatedString) = Text(t, color = Sbo.mutedFg, fontSize = 14.sp, lineHeight = 20.sp)
                            Li(androidx.compose.ui.text.AnnotatedString("• SBO diventa il tuo personal trainer: allenamenti ogni giorno, giorni di riposo, un coach che ti scrive."))
                            Li(androidx.compose.ui.text.buildAnnotatedString { append("• Non vince chi lo fa di più: vince chi lo fa "); pushStyle(b); append("di meno"); pop(); append(". Limite settimanale da 5 a 3, ideale sotto.") })
                            Li(androidx.compose.ui.text.buildAnnotatedString { append("• Dura "); pushStyle(b); append("almeno 7 giorni"); pop(); append(": prima non puoi disattivarlo.") })
                            Li(androidx.compose.ui.text.AnnotatedString("• Se sfori il limite, il coach si incazza. Davvero."))
                        }
                    }
                }
                qi < QUESTIONS.size -> {
                    val q = QUESTIONS[qi]
                    val v = ans.get(q.key)
                    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        Text("DOMANDA ${qi + 1} DI ${QUESTIONS.size}", color = Sbo.mutedFg, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.6.sp)
                        Text(q.title, fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold)
                        Text(q.hint, color = Sbo.mutedFg, fontSize = 14.sp, lineHeight = 20.sp)
                        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.Bottom) {
                            BasicTextField(
                                "$v", { t -> ans = ans.set(q.key, (t.filter(Char::isDigit).take(3).toIntOrNull() ?: 0).coerceIn(q.min, q.max)) },
                                singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), cursorBrush = SolidColor(Sbo.primary),
                                textStyle = TextStyle(fontFamily = SpaceGrotesk, fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Sbo.foreground, textAlign = TextAlign.Center),
                                modifier = Modifier.size(width = 128.dp, height = 64.dp).clip(RoundedCornerShape(18.dp)).border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(18.dp)).padding(top = 10.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(q.unit, color = Sbo.mutedFg, fontSize = 14.sp, modifier = Modifier.padding(bottom = 12.dp))
                        }
                        SboSlider(v, q.min, q.max, onChange = { n -> ans = ans.set(q.key, (n / q.step) * q.step) })
                    }
                }
                qi == QUESTIONS.size -> Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Ancora due cose", fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold)
                    FieldLabel("Come ti chiami? (così ti chiama il coach)")
                    SboInput(ans.realName ?: "", { ans = ans.copy(realName = it.take(24)) }, "Es. Franco", Modifier.fillMaxWidth())
                    FieldLabel("Hai un manubrio? Peso in kg (0 = no)")
                    SboInput("${ans.dumbbellKg ?: 0}", { t -> ans = ans.copy(dumbbellKg = (t.filter(Char::isDigit).take(2).toIntOrNull() ?: 0).coerceIn(0, 40)) }, "0", Modifier.fillMaxWidth())
                    Row(Modifier.clickable { ans = ans.copy(hasBar = !(ans.hasBar ?: false)) }, verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(16.dp).clip(RoundedCornerShape(3.dp)).background(if (ans.hasBar == true) Sbo.primary else Color.Transparent).border(1.dp, Sbo.mutedFg, RoundedCornerShape(3.dp)), contentAlignment = Alignment.Center) {
                            if (ans.hasBar == true) Icon(Lucide.CheckBold, null, tint = Sbo.primaryFg, modifier = Modifier.size(12.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("Ho una barra per trazioni a casa", fontSize = 14.sp)
                    }
                    FieldLabel("Che giorni vai in palestra o al parco?")
                    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(1, 2, 3, 4, 5, 6, 0).forEach { d ->
                            val on = d in (ans.gymDays ?: emptyList())
                            Chip(WD[d], on) { ans = ans.copy(gymDays = if (on) (ans.gymDays ?: emptyList()) - d else (ans.gymDays ?: emptyList()) + d) }
                        }
                    }
                }
                qi == QUESTIONS.size + 1 -> Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Scegli il tuo tema", fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold)
                    Text("Con il Progetto Zero attivo l'app cambia faccia. Puoi cambiarlo quando vuoi dalle impostazioni.", color = Sbo.mutedFg, fontSize = 14.sp, lineHeight = 20.sp)
                    Column { ThemePicker(theme) { theme = it } }
                }
                else -> Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("RISULTATO", color = Sbo.mutedFg, fontSize = 12.sp, letterSpacing = 0.6.sp)
                    val pop = remember { Animatable(0.6f) }
                    LaunchedEffect(Unit) { pop.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 200f)) }
                    Box(
                        Modifier.size(112.dp).graphicsLayer { scaleX = pop.value; scaleY = pop.value; alpha = ((pop.value - 0.6f) / 0.4f).coerceIn(0f, 1f) }
                            .clip(CircleShape).background(Sbo.primary.copy(alpha = 0.10f)).border(4.dp, Sbo.primary, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { Text(league.label.removePrefix("Lega "), fontFamily = SpaceGrotesk, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Sbo.primary) }
                    Text(league.label, fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold)
                    Text(
                        when (league) { League.low -> "Si parte dalle basi. Esercizi semplici per gli altri, una sfida vera per te."; League.mid -> "Sei nella media. Adesso si costruisce sopra."; else -> "Sei sopra la media. Il piano non ti farà sconti." },
                        color = Sbo.mutedFg, fontSize = 14.sp, lineHeight = 20.sp, textAlign = TextAlign.Center,
                    )
                    if (!reassess) Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Sbo.secondary.copy(alpha = 0.4f)).border(1.dp, Sbo.border, RoundedCornerShape(24.dp)).padding(16.dp)) {
                        Text(androidx.compose.ui.text.buildAnnotatedString { pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold)); append("Limite iniziale:"); pop(); append(" $limit a settimana") }, fontSize = 14.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(if (limit > 3) "Scende di uno ogni due settimane fino a 3." else "Il limite ideale. Stare sotto è ancora meglio.", color = Sbo.mutedFg, fontSize = 12.sp)
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (step > 0) Box(Modifier.size(36.dp).clip(RoundedCornerShape(18.dp)).background(Sbo.secondary).clickable { step-- }, contentAlignment = Alignment.Center) {
                Icon(Lucide.ChevronLeft, null, tint = Sbo.foreground, modifier = Modifier.size(16.dp))
            }
            if (step < total - 1) SboButton(if (step == 0) "Iniziamo" else "Avanti", modifier = Modifier.weight(1f), trailing = Lucide.ChevronRight) { step++ }
            else SboButton(if (reassess && !redo) "Aggiorna il piano" else "Attiva il Progetto Zero", modifier = Modifier.weight(1f)) { onSubmit(ans, theme, league, limit) }
        }
    }
}


// ---------------------------------------------------------------- home + settings (PZ mode)

/** Home while Progetto Zero is on (web: banner, reassess, two buttons, undo, plan card, medal slots — no stats, no tip). */
@Composable
fun ZeroHome(
    s: HomeState, zv: ZeroView, onAdd: () -> Unit, onUndo: () -> Unit, onReassess: () -> Unit, onWorkout: () -> Unit,
    onToggle: (String) -> Unit, onSkip: (String) -> Unit, onCustom: () -> Unit, onExpand: () -> Unit, onSlot: (Int) -> Unit,
) {
    val plan = zv.plan ?: return
    val p = s.profile ?: return
    Column(verticalArrangement = Arrangement.spacedBy(32.dp)) {
        ZeroBanner(zv.day, zv.league, zv.weekCount, zv.weeklyLimit, zv.workoutsWeek)
        if (zv.reassessDue) Text(
            androidx.compose.ui.text.buildAnnotatedString {
                pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold, color = Sbo.primary)); append("È passato un mese."); pop()
                append(" Rifai il questionario e scopri se sali di lega →")
            },
            fontSize = 14.sp, lineHeight = 20.sp,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Sbo.primary.copy(alpha = 0.10f)).dashedBorder(Sbo.primary.copy(alpha = 0.6f), 28.dp, 1.dp).clickable(onClick = onReassess).padding(12.dp),
        )
        Column {
            ZeroButtons(zv.weekCount, zv.weeklyLimit, zv.todayWD?.completed == true, plan.rest, onAdd, onWorkout)
            if (s.events.isNotEmpty()) Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.Center) {
                Row(Modifier.height(32.dp).clip(RoundedCornerShape(18.dp)).clickable(onClick = onUndo).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Lucide.Undo2, null, tint = Sbo.mutedFg, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(14.dp))
                    Text("Annulla ultimo evento", color = Sbo.mutedFg, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
        TodayPlanCard(plan, zv.todayWD, onToggle, onSkip, onCustom, onExpand)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)) {
            listOf(p.slot_1, p.slot_2, p.slot_3).forEachIndexed { i, id ->
                val b = id?.let { bid -> s.badges.firstOrNull { it.id == bid } }
                MedalSlot(b?.let { com.sbo.app.logic.Badges.def(it.category, it.threshold) }) { onSlot(i) }
            }
        }
    }
}

@Composable
fun ZeroSettingsCard(zv: ZeroView, onStart: () -> Unit, onQuit: () -> Unit, onLimit: (Int) -> Unit, onTheme: (String) -> Unit, onReassess: () -> Unit) {
    val desc = if (zv.active) "Attivo da ${zv.day} giorni · ${zv.league.label}" else "Trasforma SBO nel tuo personal trainer. Minimo 7 giorni."
    PzCard(zv.active, desc, extra = {
        if (zv.active) Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            var draft by remember(zv.weeklyLimit) { mutableStateOf(zv.weeklyLimit) }
            Column {
                Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) { FieldLabel("Limite settimanale") }
                    Text("$draft", fontFamily = FontFamily.Monospace, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
                SboSlider(draft, 0, 5, onChange = { draft = it }, onCommit = onLimit)
                Text("Da 0 a 5 a settimana. Vale da subito e resta fisso finché non lo cambi.", color = Sbo.mutedFg, fontSize = 11.sp, lineHeight = 16.sp)
            }
            UpperLabel("Tema")
            Column(Modifier.padding(top = 0.dp)) { ThemePicker(zv.pz?.theme ?: "obsidian", onTheme) }
            SmallButton("Rifai il questionario", modifier = Modifier.fillMaxWidth(), onClick = onReassess)
        }
    }) { on -> if (on) onStart() else onQuit() }
}
