package com.sbo.app.logic

import com.sbo.app.data.SboEvent
import java.time.Instant
import java.time.OffsetDateTime
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.max
import kotlin.math.roundToInt

/** Same rules as src/lib/sbo-cloud.ts, on local calendar days. */
object Streaks {
    private val zone: ZoneId get() = ZoneId.systemDefault()
    // Supabase returns "2026-10-22T14:03:11.123+00:00".
    fun instant(ts: String): Instant = OffsetDateTime.parse(ts).toInstant()
    private fun day(e: SboEvent): LocalDate = instant(e.ts).atZone(zone).toLocalDate()

    fun countToday(events: List<SboEvent>): Int { val t = LocalDate.now(zone); return events.count { day(it) == t } }

    fun countThisMonth(events: List<SboEvent>): Int {
        val t = LocalDate.now(zone)
        return events.count { val d = day(it); d.month == t.month && d.year == t.year }
    }

    fun byDay(events: List<SboEvent>): Map<LocalDate, Int> = events.groupingBy { day(it) }.eachCount()

    /** Ice with 24h grace: last event N>=2 days ago → N-1. */
    fun ice(events: List<SboEvent>): Int {
        if (events.isEmpty()) return 0
        val last = events.maxOf { day(it) }
        val since = ChronoUnit.DAYS.between(last, LocalDate.now(zone)).toInt()
        return if (since <= 1) 0 else since - 1
    }

    /** Fire: consecutive days with >=1 event ending today or yesterday. */
    fun fire(events: List<SboEvent>): Int {
        if (events.isEmpty()) return 0
        val days = byDay(events)
        val today = LocalDate.now(zone)
        var cursor = when {
            days.containsKey(today) -> today
            days.containsKey(today.minusDays(1)) -> today.minusDays(1)
            else -> return 0
        }
        var n = 0
        while (n < 3650 && days.containsKey(cursor)) { n++; cursor = cursor.minusDays(1) }
        return n
    }

    /** Longest run of consecutive active days. */
    fun maxFire(events: List<SboEvent>): Int {
        if (events.isEmpty()) return 0
        val days = byDay(events).keys.sorted()
        var best = 1; var cur = 1
        for (i in 1 until days.size) { cur = if (ChronoUnit.DAYS.between(days[i - 1], days[i]) == 1L) cur + 1 else 1; best = max(best, cur) }
        return best
    }

    /** Longest gap between active days minus the 24h grace day, or the current ice streak. */
    fun maxIce(events: List<SboEvent>): Int {
        if (events.isEmpty()) return 0
        val days = byDay(events).keys.sorted()
        var best = 0
        for (i in 1 until days.size) { val gap = ChronoUnit.DAYS.between(days[i - 1], days[i]).toInt() - 1; best = max(best, max(0, gap - 1)) }
        return max(best, ice(events))
    }

    fun adaptiveLimit(events: List<SboEvent>, fallback: Int = 3): Int {
        val cutoff = Instant.now().minus(14, ChronoUnit.DAYS)
        val recent = events.count { instant(it.ts).isAfter(cutoff) }
        if (recent < 5) return fallback
        return max(1, (recent / 14.0).roundToInt())
    }
}
