package com.sbo.app;

import android.app.Application;

/** Runs every time the app process starts: puts the right launcher icon in place. */
public class SboApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        HolidayIcon.apply(this);
    }
}
