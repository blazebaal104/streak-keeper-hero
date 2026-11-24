@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.sbo.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sbo.app.data.SboEvent
import com.sbo.app.logic.Streaks
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle as JTextStyle
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.pow

// ---------------------------------------------------------------- calendar

/** Locale of the web calendar (react-day-picker without a locale = English, weeks start on Sunday). */
private val CAL_LOCALE: Locale = Locale.US
private val WEEK_START = DayOfWeek.SUNDAY

@Composable
fun CalendarTab(events: List<SboEvent>, limit: Int, today: LocalDate = LocalDate.now(), zero: ZeroView? = null, onDay: (LocalDate) -> Unit) {
    val byDay = remember(events) { Streaks.byDay(events) }
    var month by remember { mutableStateOf(YearMonth.from(today)) }
    Card(padding = 16.dp) {
        Column(Modifier.fillMaxWidth()) {
            Text("Registro personale — solo per uso medico e di monitoraggio.", color = Sbo.mutedFg, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.padding(horizontal = 8.dp))
            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                MonthGrid(month, today, byDay, limit, onPrev = { month = month.minusMonths(1) }, onNext = { month = month.plusMonths(1) }, onDay = onDay, mark = zero?.let { z -> { d: LocalDate -> z.workoutStatus(d) } })
            }
            Spacer(Modifier.height(16.dp))
            androidx.compose.foundation.layout.FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                @Composable fun Item(mark: @Composable () -> Unit, t: String) = Row(verticalAlignment = Alignment.CenterVertically) { mark(); Spacer(Modifier.width(6.dp)); Text(t, color = Sbo.mutedFg, fontSize = 12.sp, lineHeight = 16.sp) }
                Item({ Box(Modifier.size(6.dp).clip(CircleShape).background(Sbo.accent)) }, "attività")
                Item({ Box(Modifier.size(8.dp).clip(CircleShape).background(Sbo.destructive)) }, "sopra il limite")
                if (zero != null) {
                    Item({ Text("✓", color = Sbo.zeroOk, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold) }, "allenamento fatto")
                    Item({ Text("✕", color = Sbo.zeroOver, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold) }, "allenamento saltato")
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("Tocca un giorno degli ultimi 7 per aggiungere o togliere eventi dimenticati.", color = Sbo.mutedFg, fontSize = 11.sp, lineHeight = 16.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
        }
    }
}

private val CELL = 32.dp

@Composable
internal fun MonthGrid(month: YearMonth, today: LocalDate, byDay: Map<LocalDate, Int>, limit: Int, onPrev: () -> Unit, onNext: () -> Unit, onDay: ((LocalDate) -> Unit)?, mark: ((LocalDate) -> String?)? = null) {
    // react-day-picker inside a shadcn Calendar: bg-background, p-3, 2rem cells
    Column(Modifier.background(Sbo.background).padding(12.dp).width(CELL * 7)) {
        Box(Modifier.fillMaxWidth().height(CELL)) {
            NavButton(Lucide.ChevronLeft, Modifier.align(Alignment.CenterStart), onPrev)
            Text(
                "${month.month.getDisplayName(JTextStyle.FULL, CAL_LOCALE)} ${month.year}",
                fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium, modifier = Modifier.align(Alignment.Center),
            )
            NavButton(Lucide.ChevronRight, Modifier.align(Alignment.CenterEnd), onNext)
        }
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth()) {
            for (i in 0 until 7) {
                val d = WEEK_START.plus(i.toLong())
                Text(
                    d.getDisplayName(JTextStyle.SHORT, CAL_LOCALE).take(2), color = Sbo.mutedFg, fontSize = 12.8.sp, lineHeight = 16.sp,
                    textAlign = TextAlign.Center, modifier = Modifier.weight(1f),
                )
            }
        }
        val first = month.atDay(1)
        val lead = ((first.dayOfWeek.value - WEEK_START.value) + 7) % 7
        var cursor = first.minusDays(lead.toLong())
        val weeks = ceil((lead + month.lengthOfMonth()) / 7.0).toInt()
        repeat(weeks) { w ->
            Spacer(Modifier.height(if (w == 0) 10.dp else 8.dp))
            Row(Modifier.fillMaxWidth()) {
                repeat(7) {
                    val d = cursor
                    DayCell(d, inMonth = d.month == month.month, today = today, count = byDay[d] ?: 0, limit = limit, onDay = onDay, mark = mark?.invoke(d))
                    cursor = cursor.plusDays(1)
                }
            }
        }
    }
}

@Composable
private fun NavButton(icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier.size(CELL).clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = Sbo.foreground, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun DayCell(d: LocalDate, inMonth: Boolean, today: LocalDate, count: Int, limit: Int, onDay: ((LocalDate) -> Unit)?, mark: String? = null) {
    // read-only calendars (friend profile) have no disabled days
    val enabled = onDay == null || (!d.isAfter(today) && !d.isBefore(today.minusDays(7)))
    val isToday = d == today
    Box(
        Modifier.size(CELL).clip(RoundedCornerShape(18.dp))
            .then(if (isToday) Modifier.background(Sbo.accent) else Modifier)
            .then(if (enabled && onDay != null) Modifier.clickable { onDay(d) } else Modifier)
            .alpha(if (enabled) 1f else 0.5f),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "${d.dayOfMonth}", fontSize = 14.sp, lineHeight = 14.sp,
            color = when { isToday -> Sbo.accentFg; !inMonth || !enabled -> Sbo.mutedFg; else -> Sbo.foreground },
        )
        if (count > 0) {
            val heavy = onDay != null && count >= limit
            Box(
                Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp).size(if (heavy) 6.dp else 4.dp)
                    .clip(CircleShape).background(if (heavy) Sbo.destructive else Sbo.accent),
            )
        }
        if (mark != null) Text(
            if (mark == "done") "✓" else "✕", color = if (mark == "done") Sbo.zeroOk else Sbo.zeroOver, fontSize = 9.sp, lineHeight = 10.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 2.dp, end = 2.dp),
        )
    }
}

/** Retro-log dialog: add or remove events on one of the last 7 days. */
@Composable
fun DayEditDialog(day: LocalDate, count: Int, onAdd: () -> Unit, onRemove: () -> Unit, onClose: () -> Unit,
                  workouts: Int? = null, onWorkoutAdd: () -> Unit = {}, onWorkoutRemove: () -> Unit = {}) {
    SboDialog(onClose) {
        val it = Locale.ITALIAN
        val title = "${day.dayOfWeek.getDisplayName(JTextStyle.FULL, it)} ${day.dayOfMonth} ${day.month.getDisplayName(JTextStyle.FULL, it)}"
        Text(title, fontSize = 18.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.45).sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(6.dp))
        Text("Correggi il registro se ti sei dimenticato (o hai premuto per sbaglio).", color = Sbo.mutedFg, fontSize = 14.sp, lineHeight = 20.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            RoundAction(Lucide.X, Sbo.secondary, Sbo.foreground, enabled = count > 0, onRemove)
            Spacer(Modifier.width(20.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$count", fontFamily = SpaceGrotesk, fontSize = 36.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold)
                Text("EVENTI", color = Sbo.mutedFg, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.55.sp)
            }
            Spacer(Modifier.width(20.dp))
            RoundAction(Lucide.Plus, Sbo.primary, Sbo.primaryFg, enabled = true, onAdd)
        }
        if (workouts != null) {
            Spacer(Modifier.height(4.dp))
            Box(Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(1.dp).background(Sbo.border))
            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                RoundAction(Lucide.X, Sbo.secondary, Sbo.foreground, enabled = workouts > 0, onWorkoutRemove)
                Spacer(Modifier.width(20.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$workouts", fontFamily = SpaceGrotesk, fontSize = 36.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold, color = Sbo.zeroOk)
                    Text("ALLENAMENTI", color = Sbo.mutedFg, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.55.sp)
                }
                Spacer(Modifier.width(20.dp))
                RoundAction(Lucide.Plus, Sbo.zeroOk, Color.White, enabled = true, onWorkoutAdd)
            }
        }
        Spacer(Modifier.height(16.dp))
        Box(Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(18.dp)).clickable(onClick = onClose), contentAlignment = Alignment.Center) {
            Text("Chiudi", fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun RoundAction(icon: androidx.compose.ui.graphics.vector.ImageVector, bg: Color, fg: Color, enabled: Boolean, onClick: () -> Unit) {
    val press = rememberPressScale()
    Box(
        Modifier.size(44.dp).graphicsScale(press).clip(CircleShape).background(bg).alpha(if (enabled) 1f else 0.5f)
            .then(if (enabled) Modifier.pressable(press, "", onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = fg, modifier = Modifier.size(20.dp)) }
}

internal fun Modifier.graphicsScale(p: PressScale) = this.graphicsLayer { scaleX = p.value; scaleY = p.value }

// ---------------------------------------------------------------- trend charts

data class DayPoint(val date: LocalDate, val count: Int)

fun dailySeries(events: List<SboEvent>, days: Int, today: LocalDate = LocalDate.now()): List<DayPoint> {
    val byDay = Streaks.byDay(events)
    return (days - 1 downTo 0).map { i -> val d = today.minusDays(i.toLong()); DayPoint(d, byDay[d] ?: 0) }
}

/** The web's 90-day line: exponential moving average, half-life ~5 days, seeded with the first week's mean. */
fun ema90(series: List<DayPoint>): List<Float> {
    val alpha = 1f - 0.5f.pow(1f / 5f)
    var ema = series.take(7).map { it.count }.average().toFloat()
    return series.map { ema += alpha * (it.count - ema); ema }
}

@Composable
fun TrendTab(events: List<SboEvent>, today: LocalDate = LocalDate.now(), tip30: Int? = null, tip90: Int? = null, zero: ZeroView? = null) {
    val s30 = remember(events) { dailySeries(events, 30, today) }
    val s90 = remember(events) { dailySeries(events, 90, today) }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(padding = 16.dp) {
            Column {
                Text("Andamento — ultimi 30 giorni", fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 4.dp))
                Spacer(Modifier.height(12.dp))
                if (s30.all { it.count == 0 }) Empty("Nessun dato ancora. Il grafico si popolerà con l'uso.", 40)
                else Chart(s30.map { it.date }, buildList {
                    if (zero != null) add(ChartSeries("Allenamenti", s30.map { zero.workoutsOn(it.date.toString()).toFloat() }, Color(0xFFEF4444), area = true, fill = listOf(0f to Color(0xFFEF4444).copy(alpha = 0.45f), 1f to Color(0xFFEF4444).copy(alpha = 0f))))
                    add(ChartSeries("Al giorno", s30.map { it.count.toFloat() }, Color(0xFFFB923C), area = true))
                    if (zero != null) add(ChartSeries("Oltre il limite", overSeries(s30, zero), Color(0xFFFF2A00), area = true, stroke = 2.5f,
                        fill = listOf(0f to Color(0xFFFFD000).copy(alpha = 0.95f), 0.45f to Color(0xFFFF3D00).copy(alpha = 0.8f), 1f to Color(0xFF7F0000).copy(alpha = 0.2f))))
                }, height = 224, initialIndex = tip30)
                if (zero != null) Row(Modifier.padding(top = 8.dp, start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    @Composable fun Dot(c: Brush, t: String) { Box(Modifier.size(8.dp).clip(CircleShape).background(c)); Spacer(Modifier.width(4.dp)); Text(t, color = Sbo.mutedFg, fontSize = 10.sp, lineHeight = 14.sp); Spacer(Modifier.width(16.dp)) }
                    Dot(androidx.compose.ui.graphics.SolidColor(Color(0xFFFB923C)), "eventi")
                    Dot(androidx.compose.ui.graphics.SolidColor(Color(0xFFEF4444)), "allenamenti")
                    Dot(Brush.verticalGradient(listOf(Color(0xFFFFD000), Color(0xFFFF2A00))), "oltre il limite")
                }
                Spacer(Modifier.height(6.dp))
            }
        }
        Card(padding = 16.dp) {
            Column {
                Text("Trend a 90 giorni", fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 4.dp))
                Spacer(Modifier.height(12.dp))
                if (s90.all { it.count == 0 }) Empty("Nessun dato ancora.", 32)
                else Chart(s90.map { it.date }, buildList {
                    add(ChartSeries("Media", ema90(s90).map { kotlin.math.round(it * 100f) / 100f }, Color(0xFF38BDF8), area = false, stroke = 2.5f))
                    if (zero != null) {
                        val raw = s90.map { zero.workoutsOn(it.date.toString()).toFloat() }
                        add(ChartSeries("Allenamenti (media 7gg)", raw.indices.map { i -> kotlin.math.round(raw.subList(maxOf(0, i - 6), i + 1).average().toFloat() * 100f) / 100f }, Color(0xFFEF4444), area = false))
                    }
                }, height = 192, initialIndex = tip90)
                Spacer(Modifier.height(14.dp))
                Text(
                    if (zero != null) "Obiettivo: la linea rossa (allenamenti) sempre sopra quella blu." else "La linea blu è la tua media: sale e scende piano piano, giorno dopo giorno. Obiettivo: portarla giù nel tempo.",
                    color = Sbo.mutedFg, fontSize = 11.sp, lineHeight = 16.sp, modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun Empty(text: String, pad: Int) {
    Text(text, color = Sbo.mutedFg, fontSize = 12.sp, lineHeight = 16.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = pad.dp))
}

/**
 * Recharts look: dashed white/15 grid, white 10px ticks, Y domain 0..max(3, ceil(max)) with integer
 * ticks, X labels MM-DD thinned so they never overlap (keeping the last one), monotone curve.
 */
data class ChartSeries(val name: String, val values: List<Float>, val color: Color, val area: Boolean, val stroke: Float = 2f, val fill: List<Pair<Float, Color>>? = null)

private val TIP_BG = Color(0xFF0F172A)
private val TIP_BORDER = Color(0xFF334155)

private fun fmtValue(v: Float): String {
    val r = kotlin.math.round(v * 100f) / 100f
    return if (r == kotlin.math.floor(r)) "${r.toInt()}" else r.toString().trimEnd('0').trimEnd('.')
}

@Composable
internal fun Chart(dates: List<LocalDate>, series: List<ChartSeries>, height: Int, initialIndex: Int? = null) {
    val measurer = rememberTextMeasurer()
    val tick = TextStyle(color = Color.White, fontSize = 10.sp, fontFamily = Inter)
    val tipStyle = TextStyle(color = Color.White, fontSize = 12.sp, fontFamily = Inter)
    var active by remember { mutableStateOf(initialIndex) }
    var touchY by remember { mutableStateOf<Float?>(null) }
    val n = dates.size
    Canvas(
        Modifier.fillMaxWidth().height(height.dp).pointerInput(n) {
            // Recharts on a phone: touching (or dragging over) the chart moves the tooltip to the nearest day; it stays after lifting.
            val l = 6.dp.toPx(); val r = size.width - 8.dp.toPx()
            fun idx(x: Float) = (((x - l) / (r - l)) * (n - 1)).let { kotlin.math.round(it).toInt() }.coerceIn(0, n - 1)
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                active = idx(down.position.x); touchY = down.position.y
                do {
                    val ev = awaitPointerEvent()
                    val c = ev.changes.firstOrNull() ?: break
                    if (c.pressed) { active = idx(c.position.x); touchY = c.position.y; if (kotlin.math.abs(c.positionChange().x) > kotlin.math.abs(c.positionChange().y)) c.consume() }
                } while (ev.changes.any { it.pressed })
            }
        },
    ) {
        val l = (28f - 22f).dp.toPx() // YAxis width 28 with margin.left -22: the plot starts 6dp in, ticks spill left
        val r = size.width - 8.dp.toPx()
        val top = 6.dp.toPx()
        val xAxisH = 30.dp.toPx()
        val axisW = 0.7.dp.toPx()
        val b = size.height - xAxisH
        val maxV = max(3f, ceil(series.maxOf { sr -> sr.values.filter { !it.isNaN() }.maxOrNull() ?: 0f }))
        val step = niceIntStep(maxV)
        val yMax = ceil(maxV / step) * step
        fun yOf(v: Float) = b - (v / yMax) * (b - top)
        fun xOf(i: Int) = l + (r - l) * i / (n - 1).coerceAtLeast(1)
        val grid = Color.White.copy(alpha = 0.15f)
        val dash = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx()))
        // horizontal grid + y ticks
        var v = 0f
        while (v <= yMax + 0.001f) {
            val y = yOf(v)
            drawLine(grid, Offset(l, y), Offset(r, y), 1f, pathEffect = dash)
            // the web's Y labels fall outside the chart (margin.left -22) and are clipped: only the tick marks show
            drawLine(Color.White, Offset(l - 5.dp.toPx(), y), Offset(l, y), axisW)
            v += step
        }
        // x ticks (preserveEnd)
        val labels = dates.map { "%02d-%02d".format(it.monthValue, it.dayOfMonth) }
        val lw = measurer.measure("00-00", tick).size.width + 6.dp.toPx()
        val every = ceil(lw / ((r - l) / (n - 1).coerceAtLeast(1))).toInt().coerceAtLeast(1)
        var i = n - 1
        while (i >= 0) {
            val x = xOf(i)
            drawLine(grid, Offset(x, top), Offset(x, b), 1f, pathEffect = dash)
            drawLine(Color.White, Offset(x, b), Offset(x, b + 5.dp.toPx()), axisW)
            val t = measurer.measure(labels[i], tick)
            drawText(t, topLeft = Offset((x - t.size.width / 2f).coerceIn(0f, size.width - t.size.width), b + 9.dp.toPx()))
            i -= every
        }
        drawLine(grid, Offset(r, top), Offset(r, b), 1f, pathEffect = dash)
        drawLine(grid, Offset(l, top), Offset(r, top), 1f, pathEffect = dash)
        // axis lines
        drawLine(Color.White, Offset(l, top), Offset(l, b), axisW)
        drawLine(Color.White, Offset(l, b), Offset(r, b), axisW)
        val a = active
        // recharts cursor: a thin grey vertical line under the series
        if (a != null) drawLine(Color(0xFFCCCCCC), Offset(xOf(a), top), Offset(xOf(a), b), 1.dp.toPx())
        for (sr in series) {
            // null values (NaN) break the line into segments, like Recharts without connectNulls
            val segs = mutableListOf<MutableList<Offset>>()
            sr.values.forEachIndexed { k, value -> if (value.isNaN()) segs.add(mutableListOf()) else { if (segs.isEmpty()) segs.add(mutableListOf()); segs.last().add(Offset(xOf(k), yOf(value))) } }
            for (pts in segs.filter { it.isNotEmpty() }) {
                val line = monotonePath(pts)
                if (sr.area) {
                    val fill = Path().apply { addPath(line); lineTo(pts.last().x, b); lineTo(pts.first().x, b); close() }
                    val stops = sr.fill?.toTypedArray() ?: arrayOf(0f to sr.color.copy(alpha = 0.55f), 1f to sr.color.copy(alpha = 0f))
                    drawPath(fill, Brush.verticalGradient(*stops, startY = top, endY = b))
                }
                if (pts.size == 1) drawCircle(sr.color, sr.stroke.dp.toPx(), pts[0])
                drawPath(line, sr.color, style = Stroke(sr.stroke.dp.toPx(), cap = StrokeCap.Butt, join = StrokeJoin.Round))
            }
        }
        if (a != null) {
            // active dots (recharts: r 4, series colour, 2px white ring)
            for (sr in series) {
                if (sr.values[a].isNaN()) continue
                val p = Offset(xOf(a), yOf(sr.values[a]))
                drawCircle(Color.White, 5.dp.toPx(), p); drawCircle(sr.color, 4.dp.toPx(), p)
            }
            // tooltip box: #0f172a, 1px #334155, radius 8, 12px text, label = the day key, then "Name : value" in each colour
            val pad = 10.dp.toPx(); val vpad = 14.dp.toPx(); val gap = 8.dp.toPx()
            val title = measurer.measure(dates[a].toString(), tipStyle)
            val rows = series.filter { !it.values[a].isNaN() }.map { measurer.measure("${it.name} : ${fmtValue(it.values[a])}", tipStyle.copy(color = it.color)) }
            val w = maxOf(title.size.width, rows.maxOf { it.size.width }) + pad * 2
            val h = title.size.height + rows.sumOf { it.size.height } + gap * rows.size + vpad * 2
            val cx = xOf(a)
            var x = cx + 10.dp.toPx()
            if (x + w > size.width) x = cx - 10.dp.toPx() - w
            x = x.coerceAtLeast(0f)
            val y = ((touchY ?: top) + 10.dp.toPx()).coerceIn(0f, (size.height - h).coerceAtLeast(0f))
            drawRoundRect(TIP_BG, Offset(x, y), androidx.compose.ui.geometry.Size(w, h), androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()))
            drawRoundRect(TIP_BORDER, Offset(x, y), androidx.compose.ui.geometry.Size(w, h), androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()), style = Stroke(1.dp.toPx()))
            drawText(title, topLeft = Offset(x + pad, y + vpad))
            var yy = y + vpad + title.size.height + gap
            rows.forEach { t -> drawText(t, topLeft = Offset(x + pad, yy)); yy += t.size.height + gap }
        }
    }
}

private fun niceIntStep(max: Float): Float {
    val raw = max / 4f
    if (raw <= 1f) return 1f
    val mag = 10f.pow(kotlin.math.floor(kotlin.math.log10(raw)))
    val f = raw / mag
    val nice = when { f <= 1f -> 1f; f <= 2f -> 2f; f <= 2.5f -> 2.5f; f <= 5f -> 5f; else -> 10f }
    return kotlin.math.ceil(nice * mag)
}

/** d3 curveMonotoneX (what Recharts' type="monotone" uses). */
private fun monotonePath(p: List<Offset>): Path {
    val path = Path()
    if (p.isEmpty()) return path
    path.moveTo(p[0].x, p[0].y)
    if (p.size == 1) return path
    val n = p.size
    val s = FloatArray(n - 1) { (p[it + 1].y - p[it].y) / (p[it + 1].x - p[it].x) }
    val t = FloatArray(n)
    for (i in 1 until n - 1) {
        val h0 = p[i].x - p[i - 1].x; val h1 = p[i + 1].x - p[i].x
        val s0 = s[i - 1]; val s1 = s[i]
        val pp = (s0 * h1 + s1 * h0) / (h0 + h1)
        t[i] = if (s0 * s1 <= 0f) 0f else (kotlin.math.sign(s0) + kotlin.math.sign(s1)) * minOf(kotlin.math.abs(s0), kotlin.math.abs(s1), 0.5f * kotlin.math.abs(pp))
    }
    t[0] = run { val h = p[1].x - p[0].x; if (h != 0f) (3f * (p[1].y - p[0].y) / h - t[1]) / 2f else t[1] }
    t[n - 1] = run { val h = p[n - 1].x - p[n - 2].x; if (h != 0f) (3f * (p[n - 1].y - p[n - 2].y) / h - t[n - 2]) / 2f else t[n - 2] }
    for (i in 0 until n - 1) {
        val dx = (p[i + 1].x - p[i].x) / 3f
        path.cubicTo(p[i].x + dx, p[i].y + dx * t[i], p[i + 1].x - dx, p[i + 1].y - dx * t[i + 1], p[i + 1].x, p[i + 1].y)
    }
    return path
}


/** Web chart30 "over": events beyond the weekly limit, counted per Monday-week, from the PZ start (null = not drawn). */
internal fun overSeries(s30: List<DayPoint>, z: ZeroView): List<Float> {
    var wk = ""; var cum = 0
    val pz = z.pz
    return s30.map { d ->
        val ws = com.sbo.app.logic.Zero.weekStart(d.date).toString()
        if (ws != wk) { wk = ws; cum = 0 }
        cum += d.count
        val start = z.start
        val lim = if (pz != null && start != null && !d.date.isBefore(start)) com.sbo.app.logic.Zero.weeklyLimitFor(pz.start_limit, start, d.date, pz.limit_set_at?.let { com.sbo.app.logic.Streaks.instant(it).atZone(java.time.ZoneId.systemDefault()).toLocalDate() }) else Int.MAX_VALUE
        val over = minOf(d.count, maxOf(0, cum - lim))
        if (over == 0) Float.NaN else over.toFloat()
    }
}
