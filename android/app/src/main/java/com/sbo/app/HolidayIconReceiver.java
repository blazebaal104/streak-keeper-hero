package com.sbo.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Midnight alarm, reboot, clock or timezone change: re-check which icon to show. */
public class HolidayIconReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        HolidayIcon.apply(context);
    }
}
