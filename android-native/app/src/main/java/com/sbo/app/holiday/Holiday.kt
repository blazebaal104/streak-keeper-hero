package com.sbo.app.holiday

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.sbo.app.BuildConfig
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Holidays (port of the web sbo-holiday.ts). Halloween = 31 October, 00:00 → 1 November 00:00 local time.
 * Both apps dress up on the day by themselves. The dev app is on by default and can be switched
 * on / off / auto from Settings (web: the 🎃 button in the DEV strip); the official app follows the calendar.
 */
object Holiday {
    const val HALLOWEEN = "halloween"
    private const val PREFS = "sbo"
    private const val KEY = "devHoliday" // "auto" | "halloween" | "none"

    /** Holidays are on in every build (official: by the calendar only). */
    val enabledInBuild: Boolean get() = true
    /** Dev app only: manual on / off / auto switch in Settings. */
    val devToggle: Boolean get() = BuildConfig.DEV

    fun at(d: LocalDate): String? = if (d.monthValue == 10 && d.dayOfMonth == 31) HALLOWEEN else null

    fun override(ctx: Context): String =
        if (!devToggle) "auto" else ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, HALLOWEEN) ?: HALLOWEEN

    /** Bumped when the dev override changes, so the screen re-reads it. */
    var version by mutableStateOf(0)
        private set

    fun setOverride(ctx: Context, v: String) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, v).apply()
        version++
        LauncherIcon.apply(ctx)
    }

    fun current(ctx: Context, today: LocalDate = LocalDate.now()): String? {
        if (!enabledInBuild) return null
        return when (override(ctx)) { HALLOWEEN -> HALLOWEEN; "none" -> null; else -> at(today) }
    }
}

/** Current holiday; switches by itself at midnight even if the app stays open. */
@Composable
fun rememberHoliday(today: LocalDate? = null): String? {
    val ctx = LocalContext.current
    var h by androidx.compose.runtime.remember { mutableStateOf(Holiday.current(ctx, today ?: LocalDate.now())) }
    val v = Holiday.version
    LaunchedEffect(v, today) {
        while (true) {
            h = Holiday.current(ctx, today ?: LocalDate.now())
            if (today != null) break
            val now = LocalDateTime.now()
            val next = now.toLocalDate().plusDays(1).atStartOfDay().plusSeconds(1)
            delay(minOf(java.time.Duration.between(now, next).toMillis(), 3_600_000L))
        }
    }
    return h
}

/**
 * Swaps the launcher icon: two activity-aliases of MainActivity (Default / Halloween), exactly one enabled.
 * An alarm wakes the app at the next switch moment (00:00) so the icon changes even if nobody opens it.
 */
object LauncherIcon {
    fun apply(ctx: Context) {
        val halloween = Holiday.current(ctx) == Holiday.HALLOWEEN
        val pkg = ctx.packageName
        set(ctx, ComponentName(pkg, "com.sbo.app.LauncherHalloween"), halloween)
        set(ctx, ComponentName(pkg, "com.sbo.app.LauncherDefault"), !halloween)
        if (Holiday.enabledInBuild && Holiday.override(ctx) == "auto") scheduleNext(ctx)
    }

    private fun set(ctx: Context, c: ComponentName, enabled: Boolean) {
        val pm = ctx.packageManager
        val want = if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        runCatching { if (pm.getComponentEnabledSetting(c) != want) pm.setComponentEnabledSetting(c, want, PackageManager.DONT_KILL_APP) }
    }

    /** Next 31 Oct 00:00 or 1 Nov 00:00, whichever comes first. */
    private fun scheduleNext(ctx: Context) {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        var next = if (Holiday.at(today) != null) today.plusDays(1) else LocalDate.of(today.year, 10, 31)
        if (!next.isAfter(today)) next = next.plusYears(1)
        val at = next.atStartOfDay(zone).plusSeconds(5).toInstant().toEpochMilli()
        val am = ctx.getSystemService(AlarmManager::class.java) ?: return
        val pi = PendingIntent.getBroadcast(ctx, 1031, Intent(ctx, HolidayReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        runCatching { am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi) }
    }
}

/** Midnight alarm, reboot, update, clock or timezone change: re-check which icon to show. */
class HolidayReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = LauncherIcon.apply(context)
}
