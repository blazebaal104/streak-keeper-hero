package com.sbo.app

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.sbo.app.data.Profile
import com.sbo.app.data.SboEvent
import com.sbo.app.ui.DropRain
import com.sbo.app.ui.HomeContent
import com.sbo.app.ui.HomeState
import com.sbo.app.ui.LocalFireStill
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Frame-by-frame captures of the animations (tabs, panels, button, fire pulse, drops) for review. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h914dp-xxhdpi")
class MotionShots {
    @get:Rule val rule = createComposeRule()
    private val profile = Profile(id = "d0", username = "tester_dev", avatar = "🧪", friend_code = "XSSZ8F", daily_limit = 3, use_adaptive_limit = true)
    private val history: List<SboEvent> = buildList {
        val end = java.time.LocalDate.of(2026, 10, 22)
        for (i in 0 until 90) {
            val d = end.minusDays(i.toLong())
            val n = listOf(0, 1, 2, 0, 3, 1, 0, 4, 2, 1, 0, 0, 2, 5, 1)[(i * 7 + i / 9) % 15] - if (i < 20) 1 else 0
            repeat(n.coerceAtLeast(0)) { k -> add(SboEvent("h$i-$k", "d0", "${d}T1${k}:00:00+00:00")) }
        }
    }
    private val day = java.time.LocalDate.of(2026, 10, 22)
    private fun state(fire: Int, ice: Int) = HomeState(
        loading = false, profile = profile, events = history, today = 1, month = 23, fire = fire, ice = ice, limit = 3,
        tip = "Fermati, tester_dev. Non sei obbligato a premere ogni volta che hai un impulso.",
    )
    private fun render(st: HomeState, rain: DropRain = DropRain()) {
        rule.mainClock.autoAdvance = false
        rule.setContent { CompositionLocalProvider(LocalFireStill provides R.drawable.fire_still_2) { HomeContent(st, {}, {}, today = day, rain = rain) } }
        rule.mainClock.advanceTimeBy(800)
    }
    private fun cap(name: String) = rule.onRoot().captureRoboImage("build/shots/motion-$name.png")
    private fun frames(prefix: String, vararg ms: Long) { var t = 0L; ms.forEach { m -> rule.mainClock.advanceTimeBy(m - t); t = m; cap("$prefix-${"%03d".format(m)}") } }

    @Test fun tabs() {
        render(state(0, 0))
        rule.onNodeWithText("Trend").performClick()
        frames("tab", 30, 60, 90, 120, 160, 220, 320, 600)
        rule.onNodeWithText("Cal").performClick()
        frames("tabback", 30, 60, 90, 120, 200, 600)
    }

    @Test fun panel() {
        render(state(0, 0))
        rule.onNodeWithText("tester_dev").performClick()
        frames("open", 16, 50, 100, 160, 240, 400)
        rule.onNodeWithContentDescription("Chiudi").performClick()
        frames("close", 40, 90, 140, 180, 260)
    }

    @Test fun firePress() {
        render(state(12, 0))
        rule.onRoot().performTouchInput { down(Offset(width / 2f, height * 0.555f)) }
        rule.mainClock.advanceTimeBy(70)
        rule.onRoot().performTouchInput { up() }
        frames("fire", 40, 120, 200, 270, 420, 600, 900, 1300)
    }

    @Test fun neutralPress() {
        render(state(0, 0))
        rule.onRoot().performTouchInput { down(Offset(width / 2f, height * 0.555f)) }
        rule.mainClock.advanceTimeBy(70)
        rule.onRoot().performTouchInput { up() }
        frames("neutral", 60, 200, 400)
    }

    @Test fun rain() {
        val rain = DropRain()
        render(state(0, 0), rain)
        rain.burst(seed = 7)
        frames("drops", 250, 500, 800, 1050, 1300, 1550, 1800)
    }

    @Test fun halloween() {
        rule.mainClock.autoAdvance = false
        rule.setContent { CompositionLocalProvider(LocalFireStill provides R.drawable.fire_still_2) { HomeContent(state(0, 0), {}, {}, today = day, holiday = "halloween") } }
        frames("halloween", 800, 4000, 6500)
    }
    @Test fun halloweenFire() {
        rule.mainClock.autoAdvance = false
        rule.setContent { CompositionLocalProvider(LocalFireStill provides R.drawable.fire_still_2) { HomeContent(state(12, 0), {}, {}, today = day, holiday = "halloween") } }
        frames("halloweenfire", 800)
    }
    @Test fun halloweenAuth() {
        rule.mainClock.autoAdvance = false
        rule.setContent { HomeContent(HomeState(loading = false, error = "login"), {}, {}, holiday = "halloween") }
        frames("halloweenauth", 800)
    }
}
