package com.sbo.app

import androidx.compose.runtime.CompositionLocalProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import com.sbo.app.data.Profile
import com.sbo.app.data.SboEvent
import com.sbo.app.ui.HomeContent
import com.sbo.app.ui.DropRain
import com.sbo.app.ui.InventoryDialog
import com.sbo.app.ui.Sbo
import com.sbo.app.data.Badge
import com.sbo.app.logic.Badges
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.performTouchInput
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
    @get:Rule val rule = createComposeRule()
    private val profile = Profile(id = "d0", username = "tester_dev", avatar = "🧪", friend_code = "XSSZ8F", daily_limit = 3, use_adaptive_limit = true)
    private val ev = listOf(SboEvent("e1", "d0", "2026-10-22T10:00:00+00:00"))
    private val brina = Badge("b1", "d0", "ice", "rare", 1)
    private fun state(fire: Int, ice: Int, equipped: Boolean = false) = HomeState(
        badges = listOf(brina),
        loading = false, profile = if (equipped) profile.copy(slot_1 = "b1") else profile, events = ev, today = 1, month = 23, fire = fire, ice = ice, limit = 1,
        tip = "Fermati, tester_dev. Non sei obbligato a premere ogni volta che hai un impulso.",
    )

    private fun shot(name: String, fire: Int, ice: Int, still: Int? = null) {
        // The UI has endless animations (fire, glow, sparks): drive the clock by hand or it never idles.
        rule.mainClock.autoAdvance = false
        rule.setContent {
            CompositionLocalProvider(LocalFireStill provides (still ?: R.drawable.fire_still_1)) {
                HomeContent(state(fire, ice), onAdd = {}, onUndo = {})
            }
        }
        rule.mainClock.advanceTimeBy(700)
        rule.onRoot().captureRoboImage("build/shots/native-$name.png")
    }

    private fun render(still: Int = R.drawable.fire_still_1, theme: String? = null, body: @androidx.compose.runtime.Composable () -> Unit) {
        com.sbo.app.ui.Sbo.applyZeroTheme(theme)
        rule.mainClock.autoAdvance = false
        rule.setContent { CompositionLocalProvider(LocalFireStill provides still) { body() } }
    }
    private fun cap(name: String) = rule.onRoot().captureRoboImage("build/shots/native-$name.png")

    @Test fun equipped() { render { HomeContent(state(0, 9, equipped = true), {}, {}) }; rule.mainClock.advanceTimeBy(700); cap("equipped") }
    @Test fun inventory() { render { HomeContent(state(0, 9, equipped = true), {}, {}, openInventory = 0) }; rule.mainClock.advanceTimeBy(700); cap("inventory") }
    @Test fun preview() {
        render {
            Box(Modifier.fillMaxSize().background(Sbo.background)) {
                InventoryDialog(0, profile, listOf(brina), onClose = {}, initialPreview = Badges.def("ice", 3)) {}
            }
        }
        rule.mainClock.advanceTimeBy(900); cap("preview")
    }
    @Test fun drops() {
        val rain = DropRain()
        render { HomeContent(state(0, 0), {}, {}, rain = rain) }
        rain.burst(seed = 7)
        rule.mainClock.advanceTimeBy(300); cap("drops-300")
        rule.mainClock.advanceTimeBy(300); cap("drops-600")
        rule.mainClock.advanceTimeBy(400); cap("drops-1000")
    }
    /** Frames of a quick tap: finger down at 0, up at 60 ms (the dip still completes, then the spring release). */
    @Test fun press() {
        render(R.drawable.fire_still_2) { HomeContent(state(12, 0), {}, {}) }
        rule.mainClock.advanceTimeBy(500)
        rule.onRoot().performTouchInput { down(Offset(width / 2f, height * 0.555f)) }
        rule.mainClock.advanceTimeBy(60); cap("press-060")
        rule.onRoot().performTouchInput { up() }
        rule.mainClock.advanceTimeBy(60); cap("press-120")
        rule.mainClock.advanceTimeBy(100); cap("press-220")
        rule.mainClock.advanceTimeBy(120); cap("press-340")
        rule.mainClock.advanceTimeBy(400); cap("press-740")
    }

    /** 90 days of varied activity ending on 2026-10-22 (for the calendar and the charts). */
    private val history: List<SboEvent> = buildList {
        val end = java.time.LocalDate.of(2026, 10, 22)
        for (i in 0 until 90) {
            val d = end.minusDays(i.toLong())
            val n = listOf(0, 1, 2, 0, 3, 1, 0, 4, 2, 1, 0, 0, 2, 5, 1)[(i * 7 + i / 9) % 15] - if (i < 20) 1 else 0
            repeat(n.coerceAtLeast(0)) { k -> add(SboEvent("h$i-$k", "d0", "${d}T1${k}:00:00+00:00")) }
        }
    }
    private val day = java.time.LocalDate.of(2026, 10, 22)
    @Test fun calendar() { render { HomeContent(state(0, 0).copy(events = history, limit = 2), {}, {}, startTab = 1, today = day) }; rule.mainClock.advanceTimeBy(700); cap("tab-cal") }
    @Test fun trend() { render { HomeContent(state(0, 0).copy(events = history, limit = 2), {}, {}, startTab = 2, today = day) }; rule.mainClock.advanceTimeBy(700); cap("tab-trend") }

    @Test fun social() {
        val robot = { id: String, name: String, role: String -> Profile(id = id, username = name, avatar = "🤖", friend_code = "AAAAAA", clan_id = "c1", clan_role = role) }
        val soc = com.sbo.app.ui.Social(
            loaded = true,
            friends = listOf(com.sbo.app.ui.FriendRow(robot("f1", "amico_test", "leader"), 0, 3, 0), com.sbo.app.ui.FriendRow(robot("f2", "amica_test", "recruit"), 0, 5, 0)),
            clan = com.sbo.app.data.Clan("c1", "Clan di Prova", "DEV"),
            members = listOf(robot("f2", "amica_test", "recruit"), profile.copy(clan_id = "c1"), robot("f1", "amico_test", "leader")),
            unread = mapOf("f1" to 2),
        )
        render { HomeContent(state(0, 0).copy(social = soc, profile = profile.copy(clan_id = "c1")), {}, {}, startTab = 3) }
        rule.mainClock.advanceTimeBy(700); cap("tab-social")
    }

    private fun open(name: String, what: String, end: Boolean = false, st: HomeState = state(0, 0).copy(events = history, limit = 2)) {
        render {
            CompositionLocalProvider(com.sbo.app.ui.LocalDialogScrollToEnd provides end) {
                HomeContent(st, {}, {}, open = what, today = day, friendPreload = history to listOf(brina))
            }
        }
        rule.mainClock.advanceTimeBy(900); cap(name)
    }
    private fun socialState(): HomeState {
        val robot = { id: String, name: String, role: String -> Profile(id = id, username = name, avatar = "🤖", friend_code = "AAAAAA", clan_id = "c1", clan_role = role) }
        return state(0, 0).copy(events = history, limit = 2, social = com.sbo.app.ui.Social(
            loaded = true, friends = listOf(com.sbo.app.ui.FriendRow(robot("f1", "amico_test", "leader"), 0, 3, 0)),
            invites = listOf(com.sbo.app.ui.InviteInfo("i1", "Clan di Prova", "DEV", "amico_test")),
        ))
    }
    @Test fun profileDlg() = open("dlg-profile", "profile")
    @Test fun profileDlg2() = open("dlg-profile-2", "profile", end = true)
    @Test fun settingsDlg() = open("dlg-settings", "settings")
    @Test fun settingsDlg2() = open("dlg-settings-2", "settings", end = true, st = state(0, 0).copy(profile = profile.copy(use_adaptive_limit = false)))
    @Test fun invitesDlg() = open("dlg-invites", "invites", st = socialState())
    @Test fun clanDlg() = open("dlg-clan", "clan")
    @Test fun friendDlg() = open("dlg-friend", "friend", st = socialState())
    @Test fun friendDlg2() = open("dlg-friend-2", "friend", end = true, st = socialState())
    @Test fun headerTitle() { render { HomeContent(state(0, 0).copy(profile = profile.copy(equipped_title = "ice:365")), {}, {}) }; rule.mainClock.advanceTimeBy(1500); cap("header-title") }
    @Test fun titles() {
        render {
            CompositionLocalProvider(com.sbo.app.ui.LocalDialogScrollToEnd provides true) {
                androidx.compose.material3.MaterialTheme {
                    HomeContent(state(0, 0).copy(events = history, profile = profile.copy(equipped_title = "fire:30")), {}, {}, openInventory = 0, today = day)
                }
            }
        }
        rule.mainClock.advanceTimeBy(900); cap("inventory-titles")
    }
    @Test fun trendTips() { render { HomeContent(state(0, 0).copy(events = history, limit = 2), {}, {}, startTab = 2, today = day, tip30 = 19, tip90 = 59) }; rule.mainClock.advanceTimeBy(700); cap("tab-trend-tips") }
    @Test fun shatter() {
        render { HomeContent(state(0, 9), {}, {}) }
        rule.mainClock.advanceTimeBy(500)
        rule.onRoot().performTouchInput { down(Offset(width / 2f, height * 0.555f)) }
        rule.mainClock.advanceTimeBy(40)
        rule.onRoot().performTouchInput { up() }
        rule.mainClock.advanceTimeBy(120); cap("shatter-160")
        rule.mainClock.advanceTimeBy(240); cap("shatter-400")
        rule.mainClock.advanceTimeBy(400); cap("shatter-800")
    }

    // ---------------- Progetto Zero (test day 2026-10-22, started 9 days earlier)
    private fun zeroState(): HomeState {
        val started = "2026-10-13T08:00:00+00:00"
        val pz = com.sbo.app.data.ProjectZero("d0", true, started, "mid", 4, "obsidian",
            com.sbo.app.logic.ZeroAnswers(20, 25, 60, 4, 40, 5, realName = "Tester", dumbbellKg = 8, hasBar = true, gymDays = listOf(6)), started)
        val wds = listOf(19, 17, 16, 14).map { d -> com.sbo.app.data.WorkoutDay("d0", "2026-10-$d", emptyList(), 4, listOf(com.sbo.app.data.CustomWorkout("Corsa", "", started)), true) }
        val msgs = listOf(
            com.sbo.app.data.AgentMessage("m1", "d0", "Buongiorno Tester. Oggi si lavora: niente scuse.", "daily", "2026-10-22T08:00:00+00:00"),
            com.sbo.app.data.AgentMessage("m2", "d0", "Ok coach, ci provo", "user", "2026-10-22T09:00:00+00:00", "2026-10-22T09:00:00+00:00"),
            com.sbo.app.data.AgentMessage("m3", "d0", "PROVARE NON BASTA. FALLO.", "rant", "2026-10-22T09:01:00+00:00"),
        )
        val weekEvents = listOf(SboEvent("w1", "d0", "2026-10-20T10:00:00+00:00"), SboEvent("w2", "d0", "2026-10-21T10:00:00+00:00"), SboEvent("w3", "d0", "2026-10-22T10:00:00+00:00"))
        return state(0, 0).copy(events = history.filter { !it.ts.startsWith("2026-10-2") } + weekEvents, zero = com.sbo.app.ui.ZeroData(pz, wds, msgs))
    }
    private fun pz(name: String, open: String? = null, tab: Int = 0) {
        render(theme = "obsidian") { HomeContent(zeroState(), {}, {}, open = open, startTab = tab, today = day) }
        rule.mainClock.advanceTimeBy(900); cap(name)
    }
    @Test fun pzHome() = pz("pz-home")
    @Test fun pzPlan() = pz("pz-plan", "plan")
    @Test fun pzSettings() = pz("pz-settings", "settings")
    @Test fun pzCoach() = pz("pz-coach", "agent")
    @Test fun pzCal() = pz("pz-cal", tab = 1)
    @Test fun pzTrend() = pz("pz-trend", tab = 2)
    @Test fun pzSocial() = pz("pz-social", tab = 3)
    @Test fun quiz1() { render { HomeContent(state(0, 0), {}, {}, open = "quiz") }; rule.mainClock.advanceTimeBy(900); cap("quiz-1") }

    @Test fun fire2a() = shot("fire-2-1", 2, 0, R.drawable.fire_still_1)
    @Test fun fire2b() = shot("fire-2-2", 2, 0, R.drawable.fire_still_2)
    @Test fun fire2c() = shot("fire-2-3", 2, 0, R.drawable.fire_still_3)
    @Test fun fire30() = shot("fire-30", 30, 0, R.drawable.fire_still_2)
    @Test fun ice9() = shot("ice-9", 0, 9)
    @Test fun neutral() = shot("neutral", 0, 0)
}
