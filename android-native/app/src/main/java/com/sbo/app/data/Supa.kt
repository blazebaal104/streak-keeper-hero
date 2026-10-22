package com.sbo.app.data

import com.sbo.app.BuildConfig
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
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
)

@Serializable
data class SboEvent(val id: String, val user_id: String, val ts: String)

@Serializable
data class Badge(val id: String, val user_id: String, val category: String, val tier: String = "", val threshold: Int, val awarded_at: String = "")

@Serializable
private data class NewEvent(val user_id: String)

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

    suspend fun undoLast(uid: String) {
        val last = client.from("events").select {
            filter { eq("user_id", uid) }
            order("ts", Order.DESCENDING)
            limit(1)
        }.decodeList<IdRow>().firstOrNull() ?: return
        client.from("events").delete { filter { eq("id", last.id) } }
    }
}
