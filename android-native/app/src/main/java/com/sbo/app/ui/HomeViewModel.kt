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
)

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
