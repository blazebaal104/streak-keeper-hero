package com.sbo.app.ui

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sbo.app.data.Profile
import com.sbo.app.logic.BadgeDef
import com.sbo.app.logic.Badges
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.graphicsLayer

private val CardShape = RoundedCornerShape(24.dp) // web: rounded-xl = 1.25rem + 4px

/** Clan operations, each reporting a message for the toast. */
class ClanActions(
    val create: (String, String, (String) -> Unit) -> Unit = { _, _, _ -> },
    val join: (String, (String) -> Unit) -> Unit = { _, _ -> },
    val accept: (String, (String) -> Unit) -> Unit = { _, _ -> },
    val decline: (String, (String) -> Unit) -> Unit = { _, _ -> },
    val invite: (Profile, (String) -> Unit) -> Unit = { _, _ -> },
    val kick: (String) -> Unit = {},
)

@Composable
fun HomeScreen(vm: HomeViewModel) {
    val s by vm.state.collectAsState()
    HomeContent(s, onAdd = vm::add, onUndo = vm::undo, onEquip = vm::equip, onDayAdd = vm::addOnDay, onDayRemove = vm::removeOnDay, onAddFriend = vm::addFriend, onRemoveFriend = vm::removeFriend, onLeaveClan = vm::leaveClan,
        onUpdateProfile = { p, j -> vm.updateProfile({ p }, j) }, onSignOut = vm::signOut, onSetRole = vm::setRole,
        clan = ClanActions(vm::createClan, vm::joinClan, vm::acceptInvite, vm::declineInvite, vm::inviteToClan, vm::kick),
        zero = ZeroActions(
            save = { j, after -> vm.savePz(j) { after() } }, tick = { vm.tick(it) }, send = vm::sendCoach, poll = { vm.reloadAgent() }, markRead = { vm.markAgentRead() },
            saveToday = { ids, cus, done -> vm.saveToday(ids, cus, done) },
        ),
    )
    // Coach check-in once a day on open (web: localStorage sbo:pzTick:<day>)
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val zv = ZeroView(s.zero, s.profile?.id, s.events)
    LaunchedEffect(zv.active) {
        if (!zv.active) return@LaunchedEffect
        val prefs = ctx.getSharedPreferences("sbo", android.content.Context.MODE_PRIVATE)
        val k = "pzTick:${java.time.LocalDate.now()}"
        if (!prefs.getBoolean(k, false)) { prefs.edit().putBoolean(k, true).apply(); vm.tick("open") }
    }
}

/** Progetto Zero operations (data lives in the view model). */
class ZeroActions(
    val save: (kotlinx.serialization.json.JsonObject, suspend () -> Unit) -> Unit = { _, _ -> },
    val tick: (String) -> Unit = {},
    val send: suspend (String) -> String? = { null },
    val poll: () -> Unit = {},
    val markRead: () -> Unit = {},
    val saveToday: (List<String>?, List<com.sbo.app.data.CustomWorkout>?, () -> Unit) -> Unit = { _, _, _ -> },
)

@Composable
fun HomeContent(
    s: HomeState, onAdd: () -> Unit, onUndo: () -> Unit,
    onEquip: (slot: Int, badgeId: String?) -> Unit = { _, _ -> },
    onDayAdd: (java.time.LocalDate) -> Unit = {},
    onDayRemove: (java.time.LocalDate) -> Unit = {},
    onAddFriend: (String, (String) -> Unit) -> Unit = { _, _ -> },
    onRemoveFriend: (String) -> Unit = {},
    onLeaveClan: () -> Unit = {},
    onUpdateProfile: (Profile, kotlinx.serialization.json.JsonObject) -> Unit = { _, _ -> },
    onSignOut: () -> Unit = {},
    clan: ClanActions = ClanActions(),
    onSetRole: (String, String) -> Unit = { _, _ -> },
    open: String? = null,
    tip30: Int? = null, tip90: Int? = null,
    friendPreload: Pair<List<com.sbo.app.data.SboEvent>, List<com.sbo.app.data.Badge>>? = null,
    zero: ZeroActions = ZeroActions(),
    rain: DropRain = remember { DropRain() },
    openInventory: Int? = null,
    startTab: Int = 0,
    today: java.time.LocalDate = java.time.LocalDate.now(),
) {
    var tab by rememberSaveable { mutableIntStateOf(startTab) }
    val toastRef = remember { mutableStateOf<ToastState?>(null) }
    var dayEdit by remember { mutableStateOf<java.time.LocalDate?>(null) }
    var addFriend by remember { mutableStateOf(false) }
    var chatWith by remember { mutableStateOf<Profile?>(null) }
    var clanChat by remember { mutableStateOf(false) }
    var confirmRemove by remember { mutableStateOf<Profile?>(null) }
    val toast = remember { ToastState() }
    toastRef.value = toast
    var confirmLeave by remember { mutableStateOf(false) }
    var showProfile by remember { mutableStateOf(open == "profile") }
    var showSettings by remember { mutableStateOf(open == "settings") }
    var showInvites by remember { mutableStateOf(open == "invites") }
    var showClanManage by remember { mutableStateOf(open == "clan") }
    var showClanInvite by remember { mutableStateOf(false) }
    var viewFriend by remember { mutableStateOf<Profile?>(if (open == "friend") s.social.friends.firstOrNull()?.profile else null) }
    var confirmKick by remember { mutableStateOf<Profile?>(null) }
    var shatter by remember { mutableIntStateOf(0) }
    var inventory by remember { mutableStateOf(openInventory) }
    val zv = remember(s.zero, s.events, s.profile?.id, today) { ZeroView(s.zero, s.profile?.id, s.events, today) }
    var quiz by remember { mutableStateOf<String?>(if (open == "quiz") "start" else null) }
    var showPlan by remember { mutableStateOf(open == "plan") }
    var showCustom by remember { mutableStateOf(false) }
    var showAgent by remember { mutableStateOf(open == "agent") }
    var confirmQuit by remember { mutableStateOf(false) }
    val ctx = androidx.compose.ui.platform.LocalContext.current
    // big plan on the first open of the day (web: localStorage sbo:pzPlanSeen:<key>)
    LaunchedEffect(zv.active, zv.plan?.key) {
        val k = zv.plan?.key ?: return@LaunchedEffect
        if (!zv.active || open != null) return@LaunchedEffect
        val prefs = ctx.getSharedPreferences("sbo", android.content.Context.MODE_PRIVATE)
        if (!prefs.getBoolean("pzPlanSeen:$k", false)) { prefs.edit().putBoolean("pzPlanSeen:$k", true).apply(); showPlan = true }
    }
    // "redo" questionnaire flagged by the server
    LaunchedEffect(zv.pz) { val pz = zv.pz; if (pz != null && !pz.active && pz.answers._redo == true) quiz = "redo" }
    // toast when the coach writes
    val seenAgent = remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(s.zero.agent.size) {
        val n = s.zero.agent.size
        val seen = seenAgent.value
        if (seen != null && n > seen && !showAgent) s.zero.agent.lastOrNull()?.let { toastRef.value?.show("🏋️ Coach Zero", it.content.take(140)) }
        seenAgent.value = n
    }
    val saveToday: (List<String>?, List<com.sbo.app.data.CustomWorkout>?) -> Unit = { ids, cus ->
        zero.saveToday(ids, cus) { toastRef.value?.show("💪 Allenamento completato!", "Spunta verde sul calendario. Il coach approva."); rain.burst() }
    }
    val toggleExercise: (String) -> Unit = { id ->
        val cur = (zv.todayWD?.done_ids ?: emptyList()).toMutableSet()
        if (!cur.add(id)) cur.remove(id)
        saveToday(cur.toList(), null)
    }
    val skipExercise: (String) -> Unit = { id ->
        val cur = zv.todayWD?.done_ids ?: emptyList()
        if (cur.none { it.startsWith("skip:") }) saveToday(cur + "skip:$id" + id, null)
    }
    val add = {
      if (zv.active) {
        if (zv.weekCount + 1 >= zv.weeklyLimit) zero.tick("event")
        rain.burst()
        val n = zv.weekCount + 1; val l = zv.weeklyLimit
        when {
            n > l -> toastRef.value?.show("🚨 $n/$l — OLTRE IL LIMITE", "Il coach ti sta scrivendo. Non sarà gentile.")
            n == l -> toastRef.value?.show("$n/$l — limite raggiunto", "Da qui a lunedì: zero.")
            else -> toastRef.value?.show("$n/$l questa settimana", "Più stai lontano dal limite, meglio è.")
        }
        onAdd()
      } else {
        rain.burst()
        if (s.ice > 0) shatter++
        s.profile?.takeIf { it.notifications_enabled }?.let { pr ->
            val n = pr.username
            when {
                s.today + 1 >= s.limit -> toastRef.value?.show("🌊 Fermati un attimo", com.sbo.app.logic.Phrases.slowdown(n))
                s.ice > 0 -> toastRef.value?.show("❄️ Streak di ghiaccio rotta", com.sbo.app.logic.Phrases.restart(n))
                s.fire > 0 -> toastRef.value?.show(com.sbo.app.logic.Phrases.fireEncourage(n, s.fire + if (s.today == 0) 1 else 0))
                else -> toastRef.value?.show(com.sbo.app.logic.Phrases.encourage(n))
            }
        }
        onAdd()
      }
    }
    CompositionLocalProvider(LocalTextStyle provides TextStyle(fontFamily = Inter, color = Sbo.foreground)) {
        Box(
            Modifier.fillMaxSize().background(Sbo.background).drawBehind {
                drawRect(Brush.radialGradient(0f to Sbo.surfaceTop, 0.7f to Sbo.background, center = Offset(size.width / 2f, 0f), radius = size.height * 0.75f))
            },
        ) {
            AmbientMood(if (zv.active) "neutral" else if (s.fire > 0) "fire" else if (s.ice > 0) "ice" else "neutral")
            val p = s.profile
            when {
                p == null && s.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Sbo.primary) }
                p == null -> Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(if (s.error == "login") "Accesso non ancora disponibile in questa versione." else "Errore: ${s.error}", color = Sbo.mutedFg, textAlign = TextAlign.Center)
                }
                else -> Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp),
                ) {
                    Header(p, s.fire, s.ice, s.social.invites.size, zv = zv, onProfile = { showProfile = true }, onBell = { showInvites = true }, onSettings = { showSettings = true })
                    TabsBar(tab) { tab = it }
                    Spacer(Modifier.height(32.dp))
                    when (tab) {
                        0 -> if (zv.active && zv.plan != null) ZeroHome(s, zv, add, onUndo, onReassess = { quiz = "reassess" },
                                onWorkout = { if (zv.plan.rest || zv.todayWD?.completed == true) showCustom = true else showPlan = true },
                                onToggle = toggleExercise, onSkip = skipExercise, onCustom = { showCustom = true }, onExpand = { showPlan = true }) { inventory = it }
                            else HomeTab(s, add, onUndo, shatter) { inventory = it }
                        1 -> CalendarTab(s.events, s.limit, today, zero = zv.takeIf { it.active }) { dayEdit = it }
                        2 -> TrendTab(s.events, today, tip30 = tip30, tip90 = tip90, zero = zv.takeIf { it.active })
                        else -> SocialTab(
                            p, s.social, onToast = toast::show, onAddFriend = { addFriend = true },
                            onChat = { chatWith = it }, onClanChat = { clanChat = true }, onRemove = { confirmRemove = it }, onLeaveClan = { confirmLeave = true },
                            onOpenFriend = { viewFriend = it }, onManageClan = { showClanManage = true }, onInviteToClan = { showClanInvite = true },
                            onSetRole = onSetRole, onKick = { confirmKick = it },
                            coach = if (zv.active || s.zero.agent.isNotEmpty()) ({ AgentRow(s.zero.agent) { showAgent = true; zero.markRead() } }) else null,
                        )
                    }
                    Spacer(Modifier.height(96.dp))
                }
            }
            DropRainOverlay(rain)
            val me = p
            if (me != null) {
                if (showProfile) ProfileDialog(me, s.badges, onClose = { showProfile = false }, onUpdate = onUpdateProfile, onSlot = { showProfile = false; inventory = it }, onSignOut = { showProfile = false; onSignOut() }, onToast = { toast.show(it) })
                if (showSettings) SettingsDialog(me, onClose = { showSettings = false }, onUpdate = onUpdateProfile, onToast = { toast.show(it) }, pzCard = {
                    ZeroSettingsCard(zv, onStart = { quiz = "start" }, onQuit = {
                        if (zv.day <= 7) toast.show("Il Progetto Zero dura almeno 7 giorni. Potrai disattivarlo tra ${8 - zv.day} giorni.") else confirmQuit = true
                    }, onLimit = { v -> zero.save(patch("start_limit" to v, "limit_set_at" to java.time.Instant.now().toString())) { toast.show("Limite settimanale: $v") } },
                        onTheme = { t -> zero.save(patch("theme" to t)) {} }, onReassess = { quiz = "reassess" })
                })
                if (confirmQuit) ConfirmDialog("Vuoi davvero mollare il Progetto Zero? Il coach non la prenderà bene.", "Molla", onClose = { confirmQuit = false }) {
                    zero.tick("quit"); zero.save(patch("active" to false)) { toast.show("Progetto Zero disattivato") }
                }
                quiz?.let { mode ->
                    ZeroQuestionnaire(reassess = mode == "reassess", redo = mode == "redo", initial = zv.pz?.answers, initialTheme = zv.pz?.theme, onClose = { quiz = null }) { ans, theme, league, startLimit ->
                        val nowIso = java.time.Instant.now().toString()
                        val a = kotlinx.serialization.json.Json { encodeDefaults = false; explicitNulls = false }.encodeToJsonElement(com.sbo.app.logic.ZeroAnswers.serializer(), ans.copy(_redo = null))
                        val base = buildMap<String, kotlinx.serialization.json.JsonElement> {
                            put("answers", a); put("league", kotlinx.serialization.json.JsonPrimitive(league.name)); put("theme", kotlinx.serialization.json.JsonPrimitive(theme))
                            put("last_assessment_at", kotlinx.serialization.json.JsonPrimitive(nowIso))
                            if (mode != "reassess" || zv.pz == null) {
                                put("active", kotlinx.serialization.json.JsonPrimitive(true)); put("started_at", kotlinx.serialization.json.JsonPrimitive(nowIso))
                                put("start_limit", kotlinx.serialization.json.JsonPrimitive(startLimit))
                            }
                        }
                        zero.save(kotlinx.serialization.json.JsonObject(base)) {
                            if (mode == "reassess" && zv.pz != null) toast.show("Piano aggiornato: ${league.label}")
                            else { toast.show("🎯 Progetto Zero attivato. Il coach ti ha scritto."); kotlinx.coroutines.delay(400); zero.tick("open") }
                        }
                        quiz = null; showSettings = false
                    }
                }
                val plan = zv.plan
                if (zv.active && plan != null && showPlan) PlanOverlay(plan, zv.todayWD, zv.pz?.answers?.realName?.takeIf { it.isNotBlank() } ?: me.username, zv.day, toggleExercise, skipExercise, onCustom = { showCustom = true }) { showPlan = false }
                if (showCustom) CustomWorkoutDialog(onClose = { showCustom = false }) { n, note ->
                    saveToday(null, (zv.todayWD?.custom ?: emptyList()) + com.sbo.app.data.CustomWorkout(n, note, java.time.Instant.now().toString())); showCustom = false
                }
                if (showAgent) {
                    AgentChatDialog(s.zero.agent, onClose = { showAgent = false }, onPoll = zero.poll) { t -> zero.send(t)?.let { toast.show(it) } }
                }
                if (showInvites) InvitesDialog(s.social.invites, onClose = { showInvites = false }, onAccept = { clan.accept(it) { m -> toast.show(m) } }, onDecline = { clan.decline(it) { m -> toast.show(m) } })
                if (showClanManage) ClanManageDialog(
                    onClose = { showClanManage = false },
                    onCreate = { n, t -> clan.create(n, t) { m -> toast.show(m); if (!m.startsWith("Tag") && !m.startsWith("Errore")) showClanManage = false } },
                    onJoin = { t -> clan.join(t) { m -> toast.show(m); if (m.startsWith("Ti sei")) showClanManage = false } },
                )
                if (showClanInvite) InviteFriendToClanDialog(s.social.friends.map { it.profile }, s.social.members.map { it.id }.toSet(), onClose = { showClanInvite = false }) { f -> clan.invite(f) { m -> toast.show(m) } }
                viewFriend?.let { f -> FriendProfileDialog(f, onClose = { viewFriend = null }, onChat = { chatWith = f; viewFriend = null }, today = today, preload = friendPreload) }
                confirmKick?.let { m -> ConfirmDialog("Espellere ${m.username}?", "Espelli", onClose = { confirmKick = null }) { clan.kick(m.id) } }
            }
            ToastHost(toast)
            if (p != null && addFriend) AddFriendDialog(onClose = { addFriend = false }) { code ->
                onAddFriend(code) { msg -> toast.show(msg) }; addFriend = false
            }
            if (p != null) chatWith?.let { DirectChatDialog(p, it) { chatWith = null } }
            val clan = s.social.clan
            if (p != null && clanChat && clan != null) ClanChatDialog(p, clan, s.social.members) { clanChat = false }
            if (confirmLeave) SboDialog({ confirmLeave = false }) {
                Text("Uscire dal clan?", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallButton("Annulla", modifier = Modifier.weight(1f), height = 36.dp, fontSize = 14) { confirmLeave = false }
                    SmallButton("Esci", variant = "default", modifier = Modifier.weight(1f), height = 36.dp, fontSize = 14) { onLeaveClan(); confirmLeave = false; toast.show("Hai lasciato il clan") }
                }
            }
            confirmRemove?.let { f ->
                SboDialog({ confirmRemove = null }) {
                    Text("Rimuovere ${f.username}?", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SmallButton("Annulla", modifier = Modifier.weight(1f), height = 36.dp, fontSize = 14) { confirmRemove = null }
                        SmallButton("Rimuovi", variant = "default", modifier = Modifier.weight(1f), height = 36.dp, fontSize = 14) { onRemoveFriend(f.id); confirmRemove = null }
                    }
                }
            }
            dayEdit?.let { d ->
                val n = remember(s.events, d) { com.sbo.app.logic.Streaks.byDay(s.events)[d] ?: 0 }
                DayEditDialog(d, n, onAdd = { onDayAdd(d) }, onRemove = { onDayRemove(d) }, onClose = { dayEdit = null })
            }
            val slot = inventory
            if (p != null && slot != null) InventoryDialog(
                slot, p, s.badges, onClose = { inventory = null },
                titles = { TitlesSection(s.events.size, com.sbo.app.ui.Titles.cleanDays(s.events, today), p.equipped_title) { id -> onUpdateProfile(p.copy(equipped_title = id), patch("equipped_title" to id)) } },
            ) { id ->
                onEquip(slot, id); inventory = null
            }
        }
    }
}

/** Page-wide mood, like the web AmbientMood: warm burning edges on fire, frozen haze on ice. */
@Composable
private fun AmbientMood(mode: String) {
    if (mode == "neutral") return
    val t = rememberInfiniteTransition(label = "mood")
    val k by t.animateFloat(0.75f, 1f, infiniteRepeatable(tween(if (mode == "fire") 4200 else 6000), RepeatMode.Reverse), label = "k")
    Canvas(Modifier.fillMaxSize()) {
        val edge = if (mode == "fire") Color(0xFFF97316).copy(alpha = 0.30f * k) else Color(0xFF38BDF8).copy(alpha = 0.24f * k)
        val w = size.width * 0.32f
        drawRect(Brush.horizontalGradient(0f to edge, 1f to Color.Transparent, startX = 0f, endX = w))
        drawRect(Brush.horizontalGradient(0f to Color.Transparent, 1f to edge, startX = size.width - w, endX = size.width))
        drawRect(Brush.verticalGradient(0f to edge.copy(alpha = edge.alpha * 0.6f), 1f to Color.Transparent, startY = 0f, endY = size.height * 0.12f))
        if (mode == "fire") {
            drawRect(Brush.radialGradient(0f to Color(0xFFFF7814).copy(alpha = 0.18f * k), 1f to Color.Transparent, center = Offset(size.width / 2f, size.height), radius = size.width * 0.9f))
        } else {
            drawRect(Brush.radialGradient(0f to Color(0xFF7DD3FC).copy(alpha = 0.14f * k), 1f to Color.Transparent, center = Offset(size.width / 2f, size.height * 0.4f), radius = size.width * 0.9f))
        }
    }
}

@Composable
private fun Header(p: Profile, fire: Int, ice: Int, invites: Int, zv: ZeroView? = null, onProfile: () -> Unit, onBell: () -> Unit, onSettings: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 32.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(1f).clickable(remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null, onClick = onProfile), verticalAlignment = Alignment.CenterVertically) {
            Avatar(p, 44)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(p.username, fontFamily = SpaceGrotesk, fontSize = 18.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.45).sp, maxLines = 1)
                val title = Titles.byId(p.equipped_title)
                if (title != null) TitleBadge(title, "xs")
                else Text(p.friend_code, color = Sbo.mutedFg, fontSize = 11.sp, lineHeight = 16.sp, fontFamily = FontFamily.Monospace)
            }
        }
        if (zv != null && zv.active) {
            ZeroChip(Lucide.Droplets, zeroColor(zv.status), "${zv.weekCount}/${zv.weeklyLimit}")
            Spacer(Modifier.width(6.dp))
            ZeroChip(Lucide.Dumbbell, Sbo.primary, "${zv.workoutsWeek}")
        } else {
            Chip(Lucide.Flame, Sbo.orange, fire)
            Spacer(Modifier.width(6.dp))
            Chip(Lucide.Snowflake, Sbo.sky, ice)
        }
        Spacer(Modifier.width(6.dp))
        Box {
            RoundIcon(Lucide.Bell, onBell)
            if (invites > 0) Box(
                Modifier.align(Alignment.TopEnd).offset(x = 2.dp, y = (-2).dp).size(16.dp).clip(CircleShape).background(Sbo.destructive),
                contentAlignment = Alignment.Center,
            ) { Text("$invites", color = Color.White, fontSize = 10.sp, lineHeight = 12.sp, fontWeight = FontWeight.Bold) }
        }
        Spacer(Modifier.width(8.dp))
        RoundIcon(Lucide.Settings, onSettings)
    }
}

@Composable
private fun Chip(icon: ImageVector, tint: Color, n: Int) {
    Row(
        Modifier.height(38.dp).clip(CircleShape).surface().border(1.dp, Sbo.border, CircleShape).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text("$n", fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun RoundIcon(icon: ImageVector, onClick: () -> Unit = {}) {
    Box(
        Modifier.size(37.dp).clip(CircleShape).surface().border(1.dp, Sbo.border, CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = Sbo.foreground, modifier = Modifier.size(16.dp)) }
}

/** Avatars are stored as data URLs (base64 JPEG) in the profile. */
@Composable
internal fun rememberAvatarBitmap(url: String?) = remember(url) {
    url?.substringAfter("base64,", "")?.takeIf { it.isNotEmpty() }?.let {
        runCatching { val b = Base64.decode(it, Base64.DEFAULT); BitmapFactory.decodeByteArray(b, 0, b.size)?.asImageBitmap() }.getOrNull()
    }
}

@Composable
fun Avatar(p: Profile, sizeDp: Int) {
    val bmp = rememberAvatarBitmap(p.avatar_url)
    Box(
        Modifier.size(sizeDp.dp).clip(CircleShape).background(Sbo.card).border(2.dp, Sbo.primary.copy(alpha = 0.4f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (bmp != null) Image(bmp, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        else Text(p.avatar, fontSize = (sizeDp * 0.5f).sp)
    }
}

@Composable
private fun TabsBar(selected: Int, onSelect: (Int) -> Unit) {
    val tabs = listOf("Home" to Lucide.Droplets, "Cal" to Lucide.CalendarDays, "Trend" to Lucide.ChartLine, "Social" to Lucide.Users)
    Row(
        Modifier.fillMaxWidth().height(35.dp).clip(CircleShape).surface().padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        tabs.forEachIndexed { i, (label, icon) ->
            val on = i == selected
            Row(
                Modifier.weight(1f).fillMaxHeight().clip(CircleShape).background(if (on) Sbo.background.copy(alpha = 0.55f) else Color.Transparent).clickable { onSelect(i) },
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(icon, null, tint = if (on) Sbo.foreground else Sbo.mutedFg, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(label, fontSize = 12.sp, lineHeight = 16.sp, color = if (on) Sbo.foreground else Sbo.mutedFg, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun HomeTab(s: HomeState, onAdd: () -> Unit, onUndo: () -> Unit, shatter: Int = 0, onSlot: (Int) -> Unit) {
    val p = s.profile ?: return
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Stat("Oggi", s.today, accent = s.today >= s.limit, modifier = Modifier.weight(1f).fillMaxHeight())
        Stat("Questo mese", s.month, modifier = Modifier.weight(1f).fillMaxHeight())
        Stat(if (p.use_adaptive_limit) "Limite adattivo" else "Limite", s.limit, icon = if (p.use_adaptive_limit) Lucide.TrendingUp else null, modifier = Modifier.weight(1f).fillMaxHeight())
    }
    Spacer(Modifier.height(29.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)) {
        listOf(p.slot_1, p.slot_2, p.slot_3).forEachIndexed { i, id ->
            val b = id?.let { bid -> s.badges.firstOrNull { it.id == bid } }
            BadgeSlot(b?.let { Badges.def(it.category, it.threshold) }) { onSlot(i) }
        }
    }
    Spacer(Modifier.height(24.dp))
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        StreakButton(s.fire, s.ice, onPress = onAdd, shatter = shatter)
        Text(
            when {
                s.fire > 0 -> "🔥 La tua fiamma cresce: ${s.fire} giorni di fila."
                s.ice > 0 -> "❄️ Sei congelato da ${s.ice} giorni. Premere rompe il ghiaccio."
                else -> "Premi per aggiungere. Tutto è salvato sul tuo account."
            },
            color = Sbo.mutedFg, fontSize = 14.sp, lineHeight = 20.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 16.dp),
        )
        if (s.events.isNotEmpty()) {
            Row(
                Modifier.padding(top = 8.dp).height(36.dp).clip(RoundedCornerShape(10.dp)).clickable { onUndo() }.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Lucide.Undo2, null, tint = Sbo.mutedFg, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text("Annulla ultimo", color = Sbo.mutedFg, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
    Spacer(Modifier.height(56.dp))
    Card {
        Row {
            Icon(Lucide.Sparkles, null, tint = Sbo.accent, modifier = Modifier.padding(top = 4.dp).size(20.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Suggerimento del giorno", fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold)
                Text(s.tip, color = Sbo.mutedFg, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

/** Card look of the web (bg-card/90 over the page gradient, very faint border). */
internal fun Modifier.sboCard(borderColor: Color = Sbo.border, borderWidth: Float = 1f) =
    this.clip(CardShape).surface().border(borderWidth.dp, borderColor, CardShape)

/** Translucent surface like the web (card colour at ~75% + a faint light from the top), so the page tint shows through. */
internal fun Modifier.surface() = this.background(
    Brush.verticalGradient(listOf(Color(0xFF1A2A35).copy(alpha = 0.72f), Sbo.card.copy(alpha = 0.66f))),
)

@Composable
internal fun Card(padding: androidx.compose.ui.unit.Dp = 20.dp, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxWidth().sboCard(Sbo.border.copy(alpha = 0.04f)).padding(padding)) { content() }
}

@Composable
private fun Stat(label: String, value: Int, modifier: Modifier = Modifier, accent: Boolean = false, icon: ImageVector? = null) {
    Column(
        modifier.sboCard(if (accent) Sbo.destructive.copy(alpha = 0.5f) else Sbo.border, if (accent) 2f else 1f).padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 22.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) { Icon(icon, null, tint = Sbo.mutedFg, modifier = Modifier.size(14.dp)); Spacer(Modifier.width(4.dp)) }
            Text(label, color = Sbo.mutedFg, fontSize = 12.sp, lineHeight = 16.sp)
        }
        Text("$value", color = if (accent) Sbo.destructive else Sbo.foreground, fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun BadgeSlot(def: BadgeDef?, onClick: () -> Unit) {
    val press = rememberPressScale()
    val m = Modifier.graphicsLayer { scaleX = press.value; scaleY = press.value }.pressable(press, def?.name ?: "Slot medaglia", onClick)
    if (def != null) { Box(m) { BadgeArt(def, 64.dp) }; return }
    Column(
        m.size(64.dp).clip(CircleShape).background(Sbo.card.copy(alpha = 0.7f)).drawBehind {
            val w = 2.dp.toPx()
            drawCircle(
                Sbo.primary.copy(alpha = 0.5f), radius = size.minDimension / 2f - w / 2f,
                style = Stroke(width = w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))),
            )
        },
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        Icon(Lucide.Plus, null, tint = Sbo.primary.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
        Text("slot", color = Sbo.primary.copy(alpha = 0.7f), fontSize = 9.sp, lineHeight = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ComingSoon() {
    Text("Questa scheda arriva nella prossima versione nativa.", color = Sbo.mutedFg, fontSize = 14.sp, modifier = Modifier.fillMaxWidth().padding(top = 48.dp), textAlign = TextAlign.Center)
}
