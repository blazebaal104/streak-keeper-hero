package com.sbo.app.data

import com.sbo.app.BuildConfig
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val id: String,
    val username: String,
    val avatar: String = "💧",
    val friend_code: String = "",
    val daily_limit: Int = 3,
    val use_adaptive_limit: Boolean = true,
    val avatar_url: String? = null,
    val slot_1: String? = null,
    val slot_2: String? = null,
    val slot_3: String? = null,
    val equipped_title: String? = null,
    val clan_id: String? = null,
    val clan_role: String = "recruit",
    val notifications_enabled: Boolean = true,
    val notif_messages: Boolean = true,
    val notif_streak: Boolean = true,
    val notif_social: Boolean = true,
)

@Serializable
data class ClanInvite(val id: String, val clan_id: String, val from_user_id: String, val to_user_id: String, val status: String = "pending")

@Serializable
private data class NewInvite(val clan_id: String, val from_user_id: String, val to_user_id: String)

@Serializable
private data class NewClan(val name: String, val tag: String, val owner_id: String)

@Serializable
private data class NameRow(val username: String)

@Serializable
data class Clan(val id: String, val name: String, val tag: String, val owner_id: String = "")

@Serializable
data class DirectMessage(val id: String, val sender_id: String, val recipient_id: String, val content: String, val created_at: String, val read_at: String? = null)

@Serializable
data class ClanMessage(val id: String, val clan_id: String, val sender_id: String? = null, val content: String, val is_system: Boolean = false, val created_at: String)

@Serializable
private data class Friendship(val user_a: String, val user_b: String)

@Serializable
private data class SenderRow(val sender_id: String)

@Serializable
private data class NewDm(val sender_id: String, val recipient_id: String, val content: String)

@Serializable
private data class NewClanMsg(val clan_id: String, val sender_id: String, val content: String)

@Serializable
data class SboEvent(val id: String, val user_id: String, val ts: String)

@Serializable
data class Badge(val id: String, val user_id: String, val category: String, val tier: String = "", val threshold: Int, val awarded_at: String = "")

@Serializable
private data class NewEvent(val user_id: String)

@Serializable
private data class NewEventAt(val user_id: String, val ts: String)

@Serializable
private data class IdRow(val id: String)

/** One Supabase client for the whole app (same backend as the website). */
object Supa {
    val client = createSupabaseClient(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_KEY) {
        install(Auth)
        install(Postgrest)
    }

    /** Returns the logged-in user id. The dev app signs into the shared test account by itself. */
    suspend fun ensureUser(): String? {
        client.auth.awaitInitialization()
        client.auth.currentUserOrNull()?.let { return it.id }
        if (BuildConfig.DEV) {
            client.auth.signInWith(Email) {
                email = BuildConfig.DEV_EMAIL
                password = BuildConfig.DEV_PASSWORD
            }
            return client.auth.currentUserOrNull()?.id
        }
        return null
    }

    suspend fun profile(uid: String): Profile =
        client.from("profiles").select { filter { eq("id", uid) } }.decodeSingle()

    suspend fun events(uid: String): List<SboEvent> =
        client.from("events").select {
            filter { eq("user_id", uid) }
            order("ts", Order.DESCENDING)
            limit(1000)
        }.decodeList()

    suspend fun badges(uid: String): List<Badge> =
        client.from("badges").select {
            filter { eq("user_id", uid) }
            order("threshold", Order.ASCENDING)
        }.decodeList()

    /** slot = 0..2; badgeId null empties the slot. */
    suspend fun setSlot(uid: String, slot: Int, badgeId: String?) {
        client.from("profiles").update({ set("slot_${slot + 1}", badgeId) }) { filter { eq("id", uid) } }
    }

    suspend fun addEvent(uid: String) {
        client.from("events").insert(NewEvent(uid))
    }

    /** Log an event at a given moment (retro-log from the calendar). */
    suspend fun addEventAt(uid: String, tsIso: String) {
        client.from("events").insert(NewEventAt(uid, tsIso))
    }

    /** Remove the most recent event between two instants (one local day). */
    suspend fun removeBetween(uid: String, fromIso: String, toIso: String) {
        val last = client.from("events").select {
            filter { eq("user_id", uid); gte("ts", fromIso); lt("ts", toIso) }
            order("ts", Order.DESCENDING)
            limit(1)
        }.decodeList<IdRow>().firstOrNull() ?: return
        client.from("events").delete { filter { eq("id", last.id) } }
    }

    // ------------------------------------------------ social

    suspend fun friends(uid: String): List<Profile> {
        val rows = client.from("friendships").select { filter { or { eq("user_a", uid); eq("user_b", uid) } } }.decodeList<Friendship>()
        val ids = rows.map { if (it.user_a == uid) it.user_b else it.user_a }
        if (ids.isEmpty()) return emptyList()
        return client.from("profiles").select { filter { isIn("id", ids) } }.decodeList()
    }

    suspend fun eventsOf(id: String): List<SboEvent> = events(id)

    /** Returns the friend's username, or throws with an Italian message. */
    suspend fun addFriendByCode(uid: String, code: String): String {
        val cleaned = code.trim().uppercase()
        val target = client.from("profiles").select { filter { eq("friend_code", cleaned) } }.decodeList<Profile>().firstOrNull()
            ?: throw IllegalArgumentException("Codice amico non trovato")
        if (target.id == uid) throw IllegalArgumentException("Non puoi aggiungere te stesso")
        val (a, b) = listOf(uid, target.id).sorted()
        runCatching { client.from("friendships").insert(Friendship(a, b)) }.onFailure { if (it.message?.contains("duplicate") != true) throw it }
        return target.username
    }

    suspend fun removeFriend(uid: String, other: String) {
        val (a, b) = listOf(uid, other).sorted()
        client.from("friendships").delete { filter { eq("user_a", a); eq("user_b", b) } }
    }

    /** Partial profile update, e.g. buildJsonObject { put("avatar", "🔥") }. */
    suspend fun updateProfile(uid: String, patch: kotlinx.serialization.json.JsonObject) {
        client.from("profiles").update(patch) { filter { eq("id", uid) } }
    }

    suspend fun profileById(id: String): Profile? = client.from("profiles").select { filter { eq("id", id) } }.decodeList<Profile>().firstOrNull()

    suspend fun badgesOf(id: String): List<Badge> = badges(id)

    suspend fun usernameOf(id: String): String = client.from("profiles").select(io.github.jan.supabase.postgrest.query.Columns.list("username")) { filter { eq("id", id) } }.decodeList<NameRow>().firstOrNull()?.username ?: ""

    suspend fun createClan(uid: String, name: String, tag: String): Clan =
        client.from("clans").insert(NewClan(name, tag.uppercase(), uid)) { select() }.decodeSingle()

    suspend fun joinClanByTag(uid: String, tag: String): Clan {
        val c = client.from("clans").select { filter { eq("tag", tag.uppercase()) } }.decodeList<Clan>().firstOrNull() ?: throw IllegalArgumentException("Clan non trovato")
        updateProfile(uid, kotlinx.serialization.json.buildJsonObject { put("clan_id", kotlinx.serialization.json.JsonPrimitive(c.id)); put("clan_role", kotlinx.serialization.json.JsonPrimitive("recruit")) })
        return c
    }

    suspend fun incomingInvites(uid: String): List<ClanInvite> =
        client.from("clan_invites").select { filter { eq("to_user_id", uid); eq("status", "pending") } }.decodeList()

    suspend fun sendClanInvite(uid: String, clanId: String, to: String, myName: String, clanName: String) {
        runCatching { client.from("clan_invites").insert(NewInvite(clanId, uid, to)) }.onFailure { if (it.message?.contains("duplicate") != true) throw it }
        sendDm(uid, to, "👋 $myName ti invita nel clan \"$clanName\"! Controlla i tuoi inviti.")
    }

    private suspend fun rpc1(fn: String, key: String, value: String) {
        client.postgrest.rpc(fn, kotlinx.serialization.json.buildJsonObject { put(key, kotlinx.serialization.json.JsonPrimitive(value)) })
    }
    suspend fun acceptInvite(id: String) = rpc1("accept_clan_invite", "_invite_id", id)
    suspend fun declineInvite(id: String) = rpc1("decline_clan_invite", "_invite_id", id)
    suspend fun kickMember(target: String) = rpc1("kick_clan_member", "_target", target)
    suspend fun setClanRole(target: String, role: String) {
        client.postgrest.rpc("set_clan_role", kotlinx.serialization.json.buildJsonObject {
            put("_target", kotlinx.serialization.json.JsonPrimitive(target)); put("_role", kotlinx.serialization.json.JsonPrimitive(role))
        })
    }

    suspend fun signOut() { client.auth.signOut() }

    suspend fun leaveClan(uid: String) {
        client.from("profiles").update({ set("clan_id", null as String?); set("clan_role", "recruit") }) { filter { eq("id", uid) } }
    }

    suspend fun clan(id: String): Clan = client.from("clans").select { filter { eq("id", id) } }.decodeSingle()

    suspend fun clanMembers(id: String): List<Profile> = client.from("profiles").select { filter { eq("clan_id", id) } }.decodeList()

    suspend fun unreadCounts(uid: String): Map<String, Int> =
        client.from("direct_messages").select(io.github.jan.supabase.postgrest.query.Columns.list("sender_id")) {
            filter { eq("recipient_id", uid); filter("read_at", io.github.jan.supabase.postgrest.query.filter.FilterOperator.IS, "null") }
        }.decodeList<SenderRow>().groupingBy { it.sender_id }.eachCount()

    suspend fun dms(uid: String, other: String): List<DirectMessage> =
        client.from("direct_messages").select {
            filter {
                or {
                    and { eq("sender_id", uid); eq("recipient_id", other) }
                    and { eq("sender_id", other); eq("recipient_id", uid) }
                }
            }
            order("created_at", Order.ASCENDING)
            limit(200)
        }.decodeList()

    suspend fun sendDm(uid: String, to: String, content: String) { client.from("direct_messages").insert(NewDm(uid, to, content)) }

    suspend fun markDmsRead(other: String) {
        client.postgrest.rpc("mark_dms_read", kotlinx.serialization.json.buildJsonObject { put("_other", kotlinx.serialization.json.JsonPrimitive(other)) })
    }

    suspend fun clanMessages(clanId: String): List<ClanMessage> =
        client.from("clan_messages").select {
            filter { eq("clan_id", clanId) }
            order("created_at", Order.ASCENDING)
            limit(200)
        }.decodeList()

    suspend fun sendClanMessage(uid: String, clanId: String, content: String) { client.from("clan_messages").insert(NewClanMsg(clanId, uid, content)) }

    suspend fun undoLast(uid: String) {
        val last = client.from("events").select {
            filter { eq("user_id", uid) }
            order("ts", Order.DESCENDING)
            limit(1)
        }.decodeList<IdRow>().firstOrNull() ?: return
        client.from("events").delete { filter { eq("id", last.id) } }
    }
}
