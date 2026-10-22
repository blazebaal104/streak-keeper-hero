package com.sbo.app

import androidx.compose.runtime.CompositionLocalProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.sbo.app.data.Profile
import com.sbo.app.data.SboEvent
import com.sbo.app.ui.HomeContent
import com.sbo.app.ui.HomeState
import com.sbo.app.ui.LocalFireStill
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Renders the home screen in every button state at the web screenshot's size (411 x 914 dp). */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h914dp-xxhdpi")
class HomeShots {
    private val profile = Profile(id = "d0", username = "tester_dev", avatar = "🧪", friend_code = "XSSZ8F", daily_limit = 3, use_adaptive_limit = true)
    private val ev = listOf(SboEvent("e1", "d0", "2026-10-22T10:00:00+00:00"))
    private fun state(fire: Int, ice: Int) = HomeState(
        loading = false, profile = profile, events = ev, today = 1, month = 23, fire = fire, ice = ice, limit = 1,
        tip = "Fermati, tester_dev. Non sei obbligato a premere ogni volta che hai un impulso.",
    )

    private fun shot(name: String, fire: Int, ice: Int, still: Int? = null) {
        captureRoboImage("build/shots/native-$name.png") {
            CompositionLocalProvider(LocalFireStill provides (still ?: R.drawable.fire_still_1)) {
                HomeContent(state(fire, ice), onAdd = {}, onUndo = {})
            }
        }
    }

    @Test fun fire2a() = shot("fire-2-1", 2, 0, R.drawable.fire_still_1)
    @Test fun fire2b() = shot("fire-2-2", 2, 0, R.drawable.fire_still_2)
    @Test fun fire2c() = shot("fire-2-3", 2, 0, R.drawable.fire_still_3)
    @Test fun fire30() = shot("fire-30", 30, 0, R.drawable.fire_still_2)
    @Test fun ice9() = shot("ice-9", 0, 9)
    @Test fun neutral() = shot("neutral", 0, 0)
}
