package com.sbo.app

import android.app.Application
import com.sbo.app.holiday.LauncherIcon

class SboApp : Application() {
    /** Every process start: put the right launcher icon in place (Halloween in the dev app). */
    override fun onCreate() { super.onCreate(); ctx = this; LauncherIcon.apply(this) }
    companion object { lateinit var ctx: Application; private set }
}
