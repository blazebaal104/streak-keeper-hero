package com.sbo.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.sbo.app.data.Profile
import com.sbo.app.data.SboEvent
import com.sbo.app.logic.Badges
import com.sbo.app.nnn.Kind
import com.sbo.app.nnn.Nnn
import com.sbo.app.nnn.NnnBreach
import com.sbo.app.nnn.NnnFallen
import com.sbo.app.nnn.NnnKickoff
import com.sbo.app.nnn.NnnVictory
import com.sbo.app.nnn.drawProtection
import com.sbo.app.nnn.rememberTextures
import com.sbo.app.ui.BadgeArt
import com.sbo.app.ui.HomeContent
import com.sbo.app.ui.HomeState
import com.sbo.app.ui.LocalFireStill
import com.sbo.app.ui.TitleBadge
import com.sbo.app.ui.Titles
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** No Nut November: home, every protection at every step, the breach frame by frame, and the overlays. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h914dp-xxhdpi")
class NnnShots {
    @get:Rule val rule = createComposeRule()
    private val profile = Profile(id = "d0", username = "tester_dev", avatar = "🧪", friend_code = "XSSZ8F", daily_limit = 3, use_adaptive_limit = true)
    private val history: List<SboEvent> = buildList {
        val end = java.time.LocalDate.of(2026, 10, 30)
        for (i in 0 until 40) { val d = end.minusDays(i.toLong()); if (i % 3 == 0) add(SboEvent("h$i", "d0", "${d}T12:00:00+00:00")) }
    }
    private fun state(fire: Int) = HomeState(loading = false, profile = profile, events = history, today = 0, month = 0, fire = fire, ice = 0, limit = 3, tip = "")
    private fun cap(name: String) = rule.onRoot().captureRoboImage("build/shots/nnn-$name.png")
    private fun frames(prefix: String, vararg ms: Long) { var t = 0L; ms.forEach { m -> rule.mainClock.advanceTimeBy(m - t); t = m; cap("$prefix-${"%05d".format(m)}") } }
    private val ctx get() = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Before fun reset() { Nnn.forcedToday = null; Nnn.ignoreFall = false; Nnn.seedSalt = 0 }

    private fun home(day: Int, fire: Int = 12, seenKickoff: Boolean = true) {
        if (seenKickoff) Nnn.markKickoff(ctx, 2026)
        rule.mainClock.autoAdvance = false
        rule.setContent { CompositionLocalProvider(LocalFireStill provides R.drawable.fire_still_2) { HomeContent(state(fire), {}, {}, today = java.time.LocalDate.of(2026, 11, day)) } }
    }

    @Test fun homeDay12() { home(12); frames("home", 900, 1700, 2600, 3500) }
    @Test fun homeNoFire() { home(20, fire = 0); frames("home-nofire", 1200) }

    @Test fun kickoff() {
        rule.mainClock.autoAdvance = false
        rule.setContent { NnnKickoff {} }
        frames("kickoff", 300, 900, 2200, 3600, 6000)
    }

    @Test fun kickoffOnHome() { home(1, seenKickoff = false); frames("kickoff-home", 900, 5000) }

    /** Each protection at each of its steps (2 per row), drawn over a dark plate, for close inspection. */
    @Test fun protections() {
        rule.mainClock.autoAdvance = false
        val cur = androidx.compose.runtime.mutableStateOf(Kind.entries[0])
        rule.setContent {
                val k = cur.value
                val rows = (k.steps + 1) / 2
                val tex = rememberTextures()
                Column(Modifier.fillMaxSize().background(Color(0xFF0B0B0E)).padding(6.dp)) {
                    Text("${k.label} · ${k.steps} tocchi", color = Color.White, fontSize = 16.sp, modifier = Modifier.padding(6.dp))
                    for (row in 0 until rows) Row {
                        for (col in 0..1) {
                            val st = row * 2 + col
                            Box(Modifier.size(199.dp, 199.dp).padding(3.dp).background(Brush.radialGradient(listOf(Color(0xFF2A1015), Color(0xFF0B0B0E)))), contentAlignment = Alignment.TopStart) {
                                if (st < k.steps) {
                                    Canvas(Modifier.fillMaxSize()) {
                                        val c = Offset(size.width / 2, size.height / 2)
                                        drawCircle(Color(0xFFFF6D00), size.minDimension * 0.30f, c)
                                        drawProtection(k, st, c, size.minDimension / 100f * 0.78f, tex, 1.3f, 7)
                                    }
                                    Text("tocco ${st + 1}", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, modifier = Modifier.padding(4.dp))
                                }
                            }
                        }
                    }
                }
        }
        Kind.entries.forEach { k ->
            rule.runOnUiThread { cur.value = k }
            androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()
            rule.mainClock.advanceTimeBy(100); rule.mainClock.advanceTimeBy(100)
            cap("kind-${k.name.lowercase()}")
        }
    }

    /** The whole breach: zoom-in, idle hint, digging (stamps, coach, growth, debris), final press and shockwave. */
    @Test fun breach() {
        val kinds = Nnn.sequence("d0", 2026)
        rule.mainClock.autoAdvance = false
        var fell = false
        rule.setContent { CompositionLocalProvider(LocalFireStill provides R.drawable.fire_still_2) { NnnBreach(kinds, 1234, 12, 0, onFall = { fell = true }, onExit = {}) } }
        frames("breach-zoom", 60, 160, 300, 600, 1200)
        rule.mainClock.advanceTimeBy(5200); cap("breach-idle-hint")
        val total = kinds.sumOf { it.steps }
        var tap = 0
        fun press(gap: Long) {
            rule.onRoot().performTouchInput { down(Offset(width / 2f, height * 0.52f)) }
            rule.mainClock.advanceTimeBy(40)
            rule.onRoot().performTouchInput { up() }
            rule.mainClock.advanceTimeBy(gap)
            tap++
        }
        // first layer slowly: every step plus the layer flying away
        repeat(kinds[0].steps) { press(260); cap("breach-l1-tap${"%02d".format(tap)}") }
        frames("breach-l1-fly", 120, 260, 420, 700)
        // then spamming, capturing as the screen fills
        while (tap < total) {
            press(110)
            if (tap % 6 == 0 || tap == total) cap("breach-spam-${"%03d".format(tap)}")
        }
        rule.mainClock.advanceTimeBy(800); cap("breach-bare")
        press(0)
        frames("breach-blast", 30, 90, 180, 320, 520, 800, 1200, 1700)
        check(fell) { "the final press never ended the breach" }
    }

    @Test fun breachLastLayers() {
        val kinds = Nnn.sequence("d0", 2026)
        rule.mainClock.autoAdvance = false
        rule.setContent { CompositionLocalProvider(LocalFireStill provides R.drawable.fire_still_2) { NnnBreach(kinds, 99, 12, 0, onFall = {}, onExit = {}, startLayer = Nnn.LAYERS - 1) } }
        frames("tape", 1200, 1600, 2000)
    }

    @Test fun fallen() {
        val cur = androidx.compose.runtime.mutableStateOf(3)
        rule.mainClock.autoAdvance = false
        rule.setContent { androidx.compose.runtime.key(cur.value) { NnnFallen(cur.value) {} } }
        listOf(3, 11, 23, 30).forEach { d ->
            rule.runOnUiThread { cur.value = d }
            androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()
            rule.mainClock.advanceTimeBy(6500)
            cap("fallen-after-${d - 1}")
        }
    }
    @Test fun fallenMotion() { rule.mainClock.autoAdvance = false; rule.setContent { NnnFallen(3) {} }; frames("fallen-motion", 200, 700, 1800, 3200) }

    @Test fun victory() {
        rule.mainClock.autoAdvance = false
        rule.setContent { NnnVictory(2026) {} }
        frames("victory", 300, 900, 1400, 2200, 3500, 6000, 9000)
    }

    @Test fun medal() {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            Column(Modifier.fillMaxSize().background(Color(0xFF0B0B0E)).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                BadgeArt(Badges.nnn(2026), side = 220.dp)
                Spacer(Modifier.height(24.dp))
                TitleBadge(Titles.nnn(2026), "md")
                Spacer(Modifier.height(16.dp))
                TitleBadge(Titles.nnn(2026), "sm")
                Spacer(Modifier.height(24.dp))
                Row { BadgeArt(Badges.nnn(2026), side = 64.dp); Spacer(Modifier.size(12.dp)); BadgeArt(Badges.nnn(2026), side = 40.dp) }
            }
        }
        frames("medal", 200, 450, 650, 850, 1100, 2000)
    }

    @Test fun debugPanel() {
        rule.mainClock.autoAdvance = false
        rule.setContent { CompositionLocalProvider(LocalFireStill provides R.drawable.fire_still_2) { HomeContent(state(4), {}, {}, today = java.time.LocalDate.of(2026, 10, 31)) } }
        rule.mainClock.advanceTimeBy(800)
        cap("debug-entry")
        rule.onNodeWithText("🧪 Test NNN (solo dev)").performScrollTo().performClick()
        rule.mainClock.advanceTimeBy(700)
        cap("debug-panel")
    }
}
