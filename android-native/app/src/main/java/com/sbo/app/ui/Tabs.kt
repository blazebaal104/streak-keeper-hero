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
fun CalendarTab(events: List<SboEvent>, limit: Int, today: LocalDate = LocalDate.now(), onDay: (LocalDate) -> Unit) {
    val byDay = remember(events) { Streaks.byDay(events) }
    var month by remember { mutableStateOf(YearMonth.from(today)) }
    Card(padding = 16.dp) {
        Column(Modifier.fillMaxWidth()) {
            Text("Registro personale — solo per uso medico e di monitoraggio.", color = Sbo.mutedFg, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.padding(horizontal = 8.dp))
            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                MonthGrid(month, today, byDay, limit, onPrev = { month = month.minusMonths(1) }, onNext = { month = month.plusMonths(1) }, onDay = onDay)
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(Sbo.accent)); Spacer(Modifier.width(6.dp))
                Text("attività", color = Sbo.mutedFg, fontSize = 12.sp, lineHeight = 16.sp)
                Spacer(Modifier.width(16.dp))
                Box(Modifier.size(8.dp).clip(CircleShape).background(Sbo.destructive)); Spacer(Modifier.width(6.dp))
                Text("sopra il limite", color = Sbo.mutedFg, fontSize = 12.sp, lineHeight = 16.sp)
            }
            Spacer(Modifier.height(12.dp))
            Text("Tocca un giorno degli ultimi 7 per aggiungere o togliere eventi dimenticati.", color = Sbo.mutedFg, fontSize = 11.sp, lineHeight = 16.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}

private val CELL = 32.dp

@Composable
private fun MonthGrid(month: YearMonth, today: LocalDate, byDay: Map<LocalDate, Int>, limit: Int, onPrev: () -> Unit, onNext: () -> Unit, onDay: (LocalDate) -> Unit) {
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
        Spacer(Modifier.height(16.dp))
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
        repeat(weeks) {
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth()) {
                repeat(7) {
                    val d = cursor
                    DayCell(d, inMonth = d.month == month.month, today = today, count = byDay[d] ?: 0, limit = limit, onDay = onDay)
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
private fun DayCell(d: LocalDate, inMonth: Boolean, today: LocalDate, count: Int, limit: Int, onDay: (LocalDate) -> Unit) {
    val enabled = !d.isAfter(today) && !d.isBefore(today.minusDays(7))
    val isToday = d == today
    Box(
        Modifier.size(CELL).clip(RoundedCornerShape(18.dp))
            .then(if (isToday) Modifier.background(Sbo.accent) else Modifier)
            .then(if (enabled) Modifier.clickable { onDay(d) } else Modifier)
            .alpha(if (enabled) 1f else 0.5f),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "${d.dayOfMonth}", fontSize = 14.sp, lineHeight = 14.sp,
            color = when { isToday -> Sbo.accentFg; !inMonth || !enabled -> Sbo.mutedFg; else -> Sbo.foreground },
        )
        if (count > 0) {
            val heavy = count >= limit
            Box(
                Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp).size(if (heavy) 6.dp else 4.dp)
                    .clip(CircleShape).background(if (heavy) Sbo.destructive else Sbo.accent),
            )
        }
    }
}

/** Retro-log dialog: add or remove events on one of the last 7 days. */
@Composable
fun DayEditDialog(day: LocalDate, count: Int, onAdd: () -> Unit, onRemove: () -> Unit, onClose: () -> Unit) {
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
fun TrendTab(events: List<SboEvent>, today: LocalDate = LocalDate.now()) {
    val s30 = remember(events) { dailySeries(events, 30, today) }
    val s90 = remember(events) { dailySeries(events, 90, today) }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(padding = 16.dp) {
            Column {
                Text("Andamento — ultimi 30 giorni", fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 4.dp))
                Spacer(Modifier.height(12.dp))
                if (s30.all { it.count == 0 }) Empty("Nessun dato ancora. Il grafico si popolerà con l'uso.", 40)
                else Chart(s30.map { it.date }, s30.map { it.count.toFloat() }, area = true, color = Color(0xFFFB923C), height = 224)
            }
        }
        Card(padding = 16.dp) {
            Column {
                Text("Trend a 90 giorni", fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 4.dp))
                Spacer(Modifier.height(12.dp))
                if (s90.all { it.count == 0 }) Empty("Nessun dato ancora.", 32)
                else Chart(s90.map { it.date }, ema90(s90), area = false, color = Color(0xFF38BDF8), height = 192, stroke = 2.5f)
                Spacer(Modifier.height(8.dp))
                Text(
                    "La linea blu è la tua media: sale e scende piano piano, giorno dopo giorno. Obiettivo: portarla giù nel tempo.",
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
@Composable
private fun Chart(dates: List<LocalDate>, values: List<Float>, area: Boolean, color: Color, height: Int, stroke: Float = 2f) {
    val measurer = rememberTextMeasurer()
    val tick = TextStyle(color = Color.White, fontSize = 10.sp, fontFamily = Inter)
    Canvas(Modifier.fillMaxWidth().height(height.dp)) {
        val l = (28f - 22f).dp.toPx() // YAxis width 28 with margin.left -22: the plot starts 6dp in, ticks spill left
        val r = size.width - 8.dp.toPx()
        val top = 6.dp.toPx()
        val xAxisH = 30.dp.toPx()
        val b = size.height - xAxisH
        val maxV = max(3f, ceil(values.max()))
        val step = niceIntStep(maxV)
        val yMax = ceil(maxV / step) * step
        fun yOf(v: Float) = b - (v / yMax) * (b - top)
        val n = values.size
        fun xOf(i: Int) = l + (r - l) * i / (n - 1).coerceAtLeast(1)
        val grid = Color.White.copy(alpha = 0.15f)
        val dash = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx()))
        // horizontal grid + y ticks
        var v = 0f
        while (v <= yMax + 0.001f) {
            val y = yOf(v)
            drawLine(grid, Offset(l, y), Offset(r, y), 1f, pathEffect = dash)
            // the web's Y labels fall outside the chart (margin.left -22) and are clipped: only the tick marks show
            drawLine(Color.White, Offset(l - 6.dp.toPx(), y), Offset(l, y), 1f)
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
            drawLine(Color.White, Offset(x, b), Offset(x, b + 6.dp.toPx()), 1f)
            val t = measurer.measure(labels[i], tick)
            drawText(t, topLeft = Offset((x - t.size.width / 2f).coerceIn(0f, size.width - t.size.width), b + 9.dp.toPx()))
            i -= every
        }
        drawLine(grid, Offset(r, top), Offset(r, b), 1f, pathEffect = dash)
        drawLine(grid, Offset(l, top), Offset(r, top), 1f, pathEffect = dash)
        // axis lines
        drawLine(Color.White, Offset(l, top), Offset(l, b), 1f)
        drawLine(Color.White, Offset(l, b), Offset(r, b), 1f)
        val pts = values.mapIndexed { k, value -> Offset(xOf(k), yOf(value)) }
        val line = monotonePath(pts)
        if (area) {
            val fill = Path().apply { addPath(line); lineTo(pts.last().x, b); lineTo(pts.first().x, b); close() }
            drawPath(fill, Brush.verticalGradient(0f to color.copy(alpha = 0.55f), 1f to color.copy(alpha = 0f), startY = top, endY = b))
        }
        drawPath(line, color, style = Stroke(stroke.dp.toPx(), cap = StrokeCap.Butt, join = StrokeJoin.Round))
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
