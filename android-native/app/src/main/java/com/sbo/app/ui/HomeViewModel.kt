package com.sbo.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sbo.app.data.Badge
import com.sbo.app.data.Profile
import com.sbo.app.data.SboEvent
import com.sbo.app.data.Supa
import com.sbo.app.logic.Phrases
import com.sbo.app.logic.Streaks
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeState(
    val loading: Boolean = true,
    val error: String? = null,
    val profile: Profile? = null,
    val events: List<SboEvent> = emptyList(),
    val today: Int = 0,
    val month: Int = 0,
    val fire: Int = 0,
    val ice: Int = 0,
    val limit: Int = 3,
    val tip: String = "",
    val badges: List<Badge> = emptyList(),
    val social: Social = Social(),
    val zero: ZeroData = ZeroData(),
)

data class ZeroData(
    val pz: com.sbo.app.data.ProjectZero? = null,
    val workouts: List<com.sbo.app.data.WorkoutDay> = emptyList(),
    val agent: List<com.sbo.app.data.AgentMessage> = emptyList(),
)

/** Everything the screens derive from Progetto Zero (web index.tsx, "PROGETTO ZERO" block). */
class ZeroView(z: ZeroData, uid: String?, events: List<SboEvent>, val today: java.time.LocalDate = java.time.LocalDate.now()) {
    private val zone = java.time.ZoneId.systemDefault()
    private fun localDate(iso: String) = Streaks.instant(iso).atZone(zone).toLocalDate()
    val pz = z.pz
    val active = pz?.active == true && pz.started_at != null
    val start: java.time.LocalDate? = pz?.started_at?.let(::localDate)
    val day = start?.let { java.time.temporal.ChronoUnit.DAYS.between(it, today).toInt() + 1 } ?: 0
    val league = runCatching { com.sbo.app.logic.League.valueOf(pz?.league ?: "low") }.getOrDefault(com.sbo.app.logic.League.low)
    val weeklyLimit = pz?.let { com.sbo.app.logic.Zero.weeklyLimitFor(it.start_limit, start, today, it.limit_set_at?.let(::localDate)) } ?: 3
    val weekStartKey = com.sbo.app.logic.Zero.weekStart(today).toString()
    val weekCount = events.count { localDate(it.ts).toString() >= weekStartKey }
    val byDay = z.workouts.associateBy { it.day }
    val workoutsWeek = z.workouts.count { it.day >= weekStartKey && (it.completed || it.custom.isNotEmpty()) }
    private val doneBefore = z.workouts.filter { it.day < today.toString() && (it.completed || it.custom.isNotEmpty()) }.map { it.day }.toSet()
    val plan = if (pz != null && uid != null) com.sbo.app.logic.Zero.planForDay(today, league, pz.answers, pz.last_assessment_at?.let(::localDate) ?: start, uid, doneBefore) else null
    val todayWD = plan?.let { byDay[it.key] }
    val reassessDue = pz?.last_assessment_at?.let { java.time.Duration.between(Streaks.instant(it), java.time.Instant.now()).toDays() > 30 } == true
    val status get() = if (weekCount > weeklyLimit) "over" else if (weekCount == weeklyLimit) "limit" else "ok"
    /** Calendar marks: "done" / "missed" / null. */
    fun workoutStatus(d: java.time.LocalDate): String? {
        val s = start ?: return null
        if (d.isBefore(s) || d.isAfter(today)) return null
        val w = byDay[d.toString()]
        if (w != null && (w.completed || w.custom.isNotEmpty())) return "done"
        if (d == today) return null
        return if (pz != null && com.sbo.app.logic.Zero.isTrainingDay(league, d, pz.answers)) "missed" else null
    }
    fun workoutsOn(key: String) = byDay[key]?.let { (if (it.completed) 1 else 0) + it.custom.size } ?: 0
}

data class FriendRow(val profile: Profile, val fire: Int, val ice: Int, val today: Int)
data class Social(
    val loaded: Boolean = false,
    val friends: List<FriendRow> = emptyList(),
    val clan: com.sbo.app.data.Clan? = null,
    val members: List<Profile> = emptyList(),
    val unread: Map<String, Int> = emptyMap(),
    val invites: List<InviteInfo> = emptyList(),
)
data class InviteInfo(val id: String, val clanName: String, val clanTag: String, val fromName: String)

/** Optional preview override (used by the screenshot pipeline): "fire:12", "ice:9", "neutral". */
class HomeViewModel(private val preview: String?) : ViewModel() {
    private val _s = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _s
    private var uid: String? = null

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        try {
            val id = uid ?: Supa.ensureUser() ?: run { _s.update { it.copy(loading = false, error = "login") }; return@launch }
            uid = id
            val p = Supa.profile(id)
            val ev = Supa.events(id)
            val b = runCatching { Supa.badges(id) }.getOrDefault(_s.value.badges)
            _s.value = compute(p, ev).copy(badges = b, social = _s.value.social)
            loadSocial(id, p)
            loadZero(id)
        } catch (e: Exception) {
            _s.update { it.copy(loading = false, error = e.message ?: "Errore di rete") }
        }
    }

    private fun compute(p: Profile, ev: List<SboEvent>): HomeState {
        var fire = Streaks.fire(ev)
        var ice = Streaks.ice(ev)
        preview?.let {
            val (mode, n) = (it.split(":") + "0").let { a -> a[0] to (a[1].toIntOrNull() ?: 0) }
            when (mode) { "fire" -> { fire = n.coerceAtLeast(1); ice = 0 }; "ice" -> { ice = n.coerceAtLeast(1); fire = 0 }; "neutral" -> { fire = 0; ice = 0 } }
        }
        val today = Streaks.countToday(ev)
        val limit = if (p.use_adaptive_limit) Streaks.adaptiveLimit(ev, p.daily_limit) else p.daily_limit
        val tip = when {
            today >= limit -> Phrases.slowdown(p.username)
            fire >= 3 -> Phrases.fireEncourage(p.username, fire)
            ice >= 3 -> Phrases.iceEncourage(p.username, ice)
            else -> Phrases.encourage(p.username)
        }
        return HomeState(false, null, p, ev, today, Streaks.countThisMonth(ev), fire, ice, limit, tip)
    }

    fun add() = viewModelScope.launch {
        val id = uid ?: return@launch
        runCatching { Supa.addEvent(id) }
        refresh()
    }

    private fun loadSocial(id: String, p: Profile) = viewModelScope.launch {
        runCatching {
            val friends = Supa.friends(id).map { f ->
                val ev = runCatching { Supa.eventsOf(f.id) }.getOrDefault(emptyList())
                FriendRow(f, Streaks.fire(ev), Streaks.ice(ev), Streaks.countToday(ev))
            }.sortedBy { it.profile.username }
            val clan = p.clan_id?.let { runCatching { Supa.clan(it) }.getOrNull() }
            val members = p.clan_id?.let { runCatching { Supa.clanMembers(it) }.getOrDefault(emptyList()) } ?: emptyList()
            val unread = runCatching { Supa.unreadCounts(id) }.getOrDefault(emptyMap())
            val invites = runCatching { Supa.incomingInvites(id) }.getOrDefault(emptyList()).map { inv ->
                val c = runCatching { Supa.clan(inv.clan_id) }.getOrNull()
                InviteInfo(inv.id, c?.name ?: "Clan", c?.tag ?: "", runCatching { Supa.usernameOf(inv.from_user_id) }.getOrDefault(""))
            }
            Social(true, friends, clan, members, unread, invites)
        }.onSuccess { soc -> _s.update { it.copy(social = soc) } }
    }

    fun uidOrNull() = uid

    private fun loadZero(id: String) = viewModelScope.launch {
        val since = java.time.LocalDate.now().minusDays(120).toString()
        val pz = runCatching { Supa.projectZero(id) }.getOrNull()
        val wd = runCatching { Supa.workoutDays(id, since) }.getOrDefault(_s.value.zero.workouts)
        val ag = runCatching { Supa.agentMessages(id) }.getOrDefault(_s.value.zero.agent)
        com.sbo.app.ui.Sbo.applyZeroTheme(if (pz?.active == true && pz.started_at != null) pz.theme else null)
        _s.update { it.copy(zero = ZeroData(pz, wd, ag)) }
    }

    fun reloadAgent() = viewModelScope.launch {
        val id = uid ?: return@launch
        runCatching { Supa.agentMessages(id) }.onSuccess { ag -> _s.update { it.copy(zero = it.zero.copy(agent = ag)) } }
    }

    private fun tzOffset(): Int = -java.time.ZoneId.systemDefault().rules.getOffset(java.time.Instant.now()).totalSeconds / 60

    /** Coach check-in (web runTick). */
    fun tick(reason: String) = viewModelScope.launch {
        Supa.coach("agent-tick", kotlinx.serialization.json.buildJsonObject {
            put("tzOffset", kotlinx.serialization.json.JsonPrimitive(tzOffset())); put("reason", kotlinx.serialization.json.JsonPrimitive(reason))
        })
        reloadAgent()
    }

    /** Message to Coach Zero; returns the toast to show, if any. */
    suspend fun sendCoach(text: String): String? {
        val r = Supa.coach("coach-chat", kotlinx.serialization.json.buildJsonObject {
            put("text", kotlinx.serialization.json.JsonPrimitive(text)); put("tzOffset", kotlinx.serialization.json.JsonPrimitive(tzOffset()))
        })
        reloadAgent().join()
        if (r == null) return "Messaggio non inviato. Controlla la connessione e riprova."
        val ok = (r["ok"] as? kotlinx.serialization.json.JsonPrimitive)?.content == "true"
        val left = (r["left"] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toIntOrNull() ?: 99
        return if (!ok) "Per oggi basta messaggi al coach 😤 Riprova domani." else if (left <= 3) "Ancora $left messaggi al coach per oggi." else null
    }

    fun markAgentRead() = viewModelScope.launch { val id = uid ?: return@launch; runCatching { Supa.markAgentRead(id) }; reloadAgent() }

    fun savePz(patch: kotlinx.serialization.json.JsonObject, after: suspend () -> Unit = {}) = viewModelScope.launch {
        val id = uid ?: return@launch
        runCatching { Supa.savePz(id, patch) }
        loadZero(id).join(); after()
    }

    /** Save today's workout row; returns true when it just became completed. */
    fun saveToday(doneIds: List<String>? = null, custom: List<com.sbo.app.data.CustomWorkout>? = null, onCompleted: () -> Unit = {}) {
        val id = uid ?: return
        val st = _s.value
        val v = ZeroView(st.zero, id, st.events)
        val plan = v.plan ?: return
        val ids = doneIds ?: v.todayWD?.done_ids ?: emptyList()
        val cus = custom ?: v.todayWD?.custom ?: emptyList()
        val planDone = !plan.rest && plan.exercises.all { it.id in ids }
        val completed = planDone || cus.isNotEmpty()
        val was = v.todayWD?.completed == true
        val row = com.sbo.app.data.WorkoutDay(id, plan.key, ids, plan.exercises.size, cus, completed, java.time.Instant.now().toString())
        _s.update { it.copy(zero = it.zero.copy(workouts = it.zero.workouts.filter { w -> w.day != plan.key } + row)) }
        viewModelScope.launch { runCatching { Supa.saveWorkoutDay(row) } }
        if (completed && !was) { onCompleted(); tick("workout") }
    }

    /** Add a friend by code; result message for the toast. */
    fun addFriend(code: String, done: (String) -> Unit) = viewModelScope.launch {
        val id = uid ?: return@launch
        val msg = runCatching { "✅ ${Supa.addFriendByCode(id, code)} aggiunto!" }.getOrElse { it.message ?: "Errore" }
        done(msg); refresh()
    }

    /** Optimistic partial profile update (settings, avatar, title...). */
    fun updateProfile(change: (Profile) -> Profile, patch: kotlinx.serialization.json.JsonObject) = viewModelScope.launch {
        val id = uid ?: return@launch
        _s.update { st -> st.profile?.let { st.copy(profile = change(it)) } ?: st }
        runCatching { Supa.updateProfile(id, patch) }
        refresh()
    }

    private fun act(done: (String) -> Unit = {}, ok: String? = null, block: suspend (String) -> String?) = viewModelScope.launch {
        val id = uid ?: return@launch
        val msg = runCatching { block(id) ?: ok }.getOrElse { it.message ?: "Errore" }
        msg?.let(done); refresh()
    }
    fun createClan(name: String, tag: String, done: (String) -> Unit) = act(done) { id ->
        runCatching { Supa.createClan(id, name, tag) }.fold({ "🛡️ Clan [${tag.uppercase()}] $name creato!" }, { if (it.message?.contains("duplicate") == true) "Tag già usato" else throw it })
    }
    fun joinClan(tag: String, done: (String) -> Unit) = act(done) { id -> "Ti sei unito a ${Supa.joinClanByTag(id, tag).name}!" }
    fun acceptInvite(inviteId: String, done: (String) -> Unit) = act(done) { Supa.acceptInvite(inviteId); "Ti sei unito al clan!" }
    fun declineInvite(inviteId: String, done: (String) -> Unit) = act(done) { Supa.declineInvite(inviteId); "Invito rifiutato" }
    fun inviteToClan(to: Profile, done: (String) -> Unit) = act(done) { id ->
        val st = _s.value; val clan = st.social.clan ?: return@act null
        runCatching { Supa.sendClanInvite(id, clan.id, to.id, st.profile?.username ?: "", clan.name) }.fold({ "Invito inviato a ${to.username}" }, { "Invito già presente o errore" })
    }
    fun setRole(target: String, role: String) = act { Supa.setClanRole(target, role); null }
    fun kick(target: String) = act { Supa.kickMember(target); null }
    fun signOut() = viewModelScope.launch { runCatching { Supa.signOut() }; uid = null; _s.value = HomeState(); refresh() }

    fun leaveClan() = viewModelScope.launch {
        val id = uid ?: return@launch
        runCatching { Supa.leaveClan(id) }; refresh()
    }

    fun removeFriend(other: String) = viewModelScope.launch {
        val id = uid ?: return@launch
        runCatching { Supa.removeFriend(id, other) }; refresh()
    }

    /** Put a medal in slot 0..2 (null = empty it). The UI updates at once, then syncs. */
    fun equip(slot: Int, badgeId: String?) = viewModelScope.launch {
        val id = uid ?: return@launch
        _s.update { st ->
            val p = st.profile ?: return@update st
            st.copy(profile = when (slot) { 0 -> p.copy(slot_1 = badgeId); 1 -> p.copy(slot_2 = badgeId); else -> p.copy(slot_3 = badgeId) })
        }
        runCatching { Supa.setSlot(id, slot, badgeId) }
        refresh()
    }

    fun addOnDay(day: java.time.LocalDate) = viewModelScope.launch {
        val id = uid ?: return@launch
        val zone = java.time.ZoneId.systemDefault()
        val at = if (day == java.time.LocalDate.now(zone)) java.time.Instant.now() else day.atTime(21, 0).atZone(zone).toInstant()
        runCatching { Supa.addEventAt(id, at.toString()) }
        refresh()
    }

    fun removeOnDay(day: java.time.LocalDate) = viewModelScope.launch {
        val id = uid ?: return@launch
        val zone = java.time.ZoneId.systemDefault()
        val from = day.atStartOfDay(zone).toInstant(); val to = day.plusDays(1).atStartOfDay(zone).toInstant()
        runCatching { Supa.removeBetween(id, from.toString(), to.toString()) }
        refresh()
    }

    fun undo() = viewModelScope.launch {
        val id = uid ?: return@launch
        runCatching { Supa.undoLast(id) }
        refresh()
    }
}
