package com.sbo.app.nnn

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sbo.app.data.Badge
import com.sbo.app.logic.Badges
import com.sbo.app.ui.BadgeArt
import com.sbo.app.ui.Pop
import com.sbo.app.ui.SboButton
import com.sbo.app.ui.SboDialog
import com.sbo.app.ui.TitleBadge
import com.sbo.app.ui.Titles
import com.sbo.app.ui.UpperLabel
import java.time.LocalDate

/** Which NNN screens are open (owned by the home screen). */
class NnnController {
    var breach by mutableStateOf(false)
    var preview by mutableStateOf(false)      // dev: the breach doesn't record anything
    var startLayer by mutableStateOf(0)
    var fallenDay by mutableStateOf<Int?>(null)
    var kickoff by mutableStateOf(false)
    var victory by mutableStateOf<Int?>(null)
    var medal by mutableStateOf(false)
    var debug by mutableStateOf(false)
}

/**
 * The home during November while you're still standing: neon "NNN IN CORSO", swinging warning signs,
 * a wind-blown banner, and the add button buried under its pile of protections.
 */
@Composable
fun NnnHome(userId: String, fire: Int, ice: Int, today: LocalDate, ctl: NnnController) {
    val kinds = remember(userId, today.year, Nnn.seedSalt) { Nnn.sequence(userId, today.year) }
    val seed = remember(userId, Nnn.seedSalt) { (userId.hashCode() + Nnn.seedSalt * 977) and 0xffff }
    var standing by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    LaunchedEffect(today.year) { standing = runCatching { com.sbo.app.data.Supa.nnnStanding(today.year) }.getOrNull() }
    Box(Modifier.fillMaxWidth().height(722.dp)) {
        NnnBackdrop(Modifier.matchParentSize(), today.dayOfMonth)
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(238.dp))
            ProtectedButton(kinds, seed, fire, ice) { ctl.preview = false; ctl.startLayer = 0; ctl.breach = true }
            Spacer(Modifier.height(24.dp))
            Text(
                NnnPhrases.reminders[today.dayOfMonth % NnnPhrases.reminders.size], color = Color.White.copy(alpha = 0.78f), fontSize = 13.sp, lineHeight = 18.sp,
                textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 18.dp),
            )
            standing?.let { (up, all) ->
                Spacer(Modifier.height(10.dp))
                Text("Ancora in piedi: $up su $all", color = Color(0xFFFF8A80), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
            }
        }
    }
}

/** All the full-screen NNN moments, drawn above the app. */
@Composable
fun NnnLayer(ctl: NnnController, userId: String?, fire: Int, ice: Int, today: LocalDate, badges: List<Badge>, onRealFall: () -> Unit) {
    val ctx = LocalContext.current
    val year = today.year
    // first open in November → Coach Zero kicks it off; first open in December as a winner → party
    LaunchedEffect(userId, today) {
        if (userId == null) return@LaunchedEffect
        if (Nnn.active(today) && !Nnn.seenKickoff(ctx, year)) ctl.kickoff = true
        if (Nnn.enabledInBuild && today.monthValue == 12 && badges.any { it.category == "nnn" && it.threshold == year } && !Nnn.seenParty(ctx, year)) ctl.victory = year
    }
    val kinds = remember(userId, year, Nnn.seedSalt) { Nnn.sequence(userId ?: "x", year) }
    val seed = remember(userId, Nnn.seedSalt) { ((userId ?: "x").hashCode() + Nnn.seedSalt * 977) and 0xffff }
    if (ctl.breach) NnnBreach(
        kinds, seed, fire, ice, startLayer = ctl.startLayer,
        onExit = { ctl.breach = false },
        onFall = {
            ctl.breach = false
            if (!ctl.preview) onRealFall()
            ctl.fallenDay = today.dayOfMonth
        },
    )
    ctl.fallenDay?.let { d -> NnnFallen(d) { ctl.fallenDay = null } }
    if (ctl.kickoff) NnnKickoff { Nnn.markKickoff(ctx, year); ctl.kickoff = false }
    ctl.victory?.let { y -> NnnVictory(y) { Nnn.markParty(ctx, y); ctl.victory = null } }
    Pop(ctl.medal) {
        SboDialog({ ctl.medal = false }) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                BadgeArt(Badges.nnn(year), side = 140.dp)
                Spacer(Modifier.height(14.dp))
                TitleBadge(Titles.nnn(year), "md")
                Spacer(Modifier.height(6.dp))
                Text("Medaglia leggendaria e titolo esclusivo", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
            }
        }
    }
    Pop(ctl.debug) { NnnDebugPanel(ctl, today, year) }
}

/** DEV only: a control room that triggers every NNN moment, to review animations and texts quickly. */
@Composable
private fun NnnDebugPanel(ctl: NnnController, today: LocalDate, year: Int) {
    val ctx = LocalContext.current
    SboDialog({ ctl.debug = false }) {
        Text("🧪 Test NNN", fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Text("Prova ogni momento della NNN senza toccare i dati veri.", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(14.dp))
        @Composable fun Section(t: String) { Spacer(Modifier.height(10.dp)); UpperLabel(t); Spacer(Modifier.height(6.dp)) }
        @Composable fun Btn(t: String, variant: String = "secondary", go: () -> Unit) { SboButton(t, variant = variant, fontSize = 13) { go() }; Spacer(Modifier.height(6.dp)) }
        Section("Home di novembre")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(1, 12, 30).forEach { d ->
                val on = Nnn.forcedToday?.dayOfMonth == d && Nnn.forcedToday?.monthValue == 11
                Box(Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).background(if (on) Color(0xFFFF1744) else Color(0xFF26262B)).clickable { Nnn.forcedToday = LocalDate.of(year, 11, d); Nnn.ignoreFall = true }.padding(vertical = 9.dp), contentAlignment = Alignment.Center) {
                    Text("Giorno $d", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Box(Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).background(if (Nnn.forcedToday == null) Color(0xFF3A3A40) else Color(0xFF26262B)).clickable { Nnn.forcedToday = null; Nnn.ignoreFall = false }.padding(vertical = 9.dp), contentAlignment = Alignment.Center) {
                Text("Reale", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(6.dp))
        Btn("🎲 Nuova sequenza di protezioni") { Nnn.seedSalt++ }
        Section("Momenti")
        Btn("🔔 1 novembre: apertura con Coach Zero") { ctl.debug = false; ctl.kickoff = true }
        Btn("🔓 Sfonda il pulsante (prova, non perdi)") { ctl.debug = false; ctl.preview = true; ctl.startLayer = 0; ctl.breach = true }
        Btn("⏩ Solo gli ultimi 3 strati + pressione finale") { ctl.debug = false; ctl.preview = true; ctl.startLayer = Nnn.LAYERS - 3; ctl.breach = true }
        Section("Reazione del Coach alla caduta")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(3, 11, 23, 30).forEach { d ->
                Box(Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).background(Color(0xFF26262B)).border(1.dp, Color(0xFFFF5252).copy(alpha = 0.4f), RoundedCornerShape(18.dp)).clickable { ctl.debug = false; ctl.fallenDay = d }.padding(vertical = 9.dp), contentAlignment = Alignment.Center) {
                    Text("${d - 1} gg", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Btn("💀 Notifica: un amico è caduto") {
            com.sbo.app.notify.Notifier.show(ctx, 99001, "💀 marco_test è caduto", "marco_test ha ceduto e ha perso la NNN dopo 6 giorni. Ancora in piedi: 7.", null)
        }
        Section("Dicembre")
        Btn("🏆 Festa per chi ha completato la NNN") { ctl.debug = false; ctl.victory = year }
        Btn("🎖️ Medaglia e titolo") { ctl.debug = false; ctl.medal = true }
        Spacer(Modifier.height(8.dp))
        SboButton("Chiudi", variant = "ghost") { ctl.debug = false }
    }
}
