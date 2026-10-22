package com.sbo.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import java.util.Calendar;

/**
 * Swaps the launcher icon for holidays. The two launcher entries are activity-aliases of the
 * same TWA activity (Default / Halloween); exactly one is enabled at a time. An alarm wakes the
 * app at the next switch moment (00:00 local time) so the icon changes even if nobody opens it.
 */
public final class HolidayIcon {
    private HolidayIcon() {}

    static boolean isHalloween(Calendar c) {
        return c.get(Calendar.MONTH) == Calendar.OCTOBER && c.get(Calendar.DAY_OF_MONTH) == 31;
    }

    public static void apply(Context ctx) {
        Calendar now = Calendar.getInstance();
        boolean halloween = BuildConfig.FORCE_HALLOWEEN || isHalloween(now);
        String pkg = ctx.getPackageName();
        set(ctx, new ComponentName(pkg, "com.sbo.app.LauncherHalloween"), halloween);
        set(ctx, new ComponentName(pkg, "com.sbo.app.LauncherDefault"), !halloween);
        if (!BuildConfig.FORCE_HALLOWEEN) scheduleNext(ctx, now);
    }

    private static void set(Context ctx, ComponentName c, boolean enabled) {
        PackageManager pm = ctx.getPackageManager();
        int want = enabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        if (pm.getComponentEnabledSetting(c) == want) return;
        pm.setComponentEnabledSetting(c, want, PackageManager.DONT_KILL_APP);
    }

    /** Next 31 Oct 00:00 or 1 Nov 00:00, whichever comes first. */
    private static void scheduleNext(Context ctx, Calendar now) {
        Calendar next = (Calendar) now.clone();
        next.set(Calendar.HOUR_OF_DAY, 0);
        next.set(Calendar.MINUTE, 0);
        next.set(Calendar.SECOND, 5);
        next.set(Calendar.MILLISECOND, 0);
        if (isHalloween(now)) {
            next.add(Calendar.DAY_OF_MONTH, 1); // 1 November
        } else {
            next.set(Calendar.MONTH, Calendar.OCTOBER);
            next.set(Calendar.DAY_OF_MONTH, 31);
            if (!next.after(now)) next.add(Calendar.YEAR, 1);
        }
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        Intent i = new Intent(ctx, HolidayIconReceiver.class);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0);
        PendingIntent pi = PendingIntent.getBroadcast(ctx, 1031, i, flags);
        if (Build.VERSION.SDK_INT >= 23) am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.getTimeInMillis(), pi);
        else am.set(AlarmManager.RTC_WAKEUP, next.getTimeInMillis(), pi);
    }
}
