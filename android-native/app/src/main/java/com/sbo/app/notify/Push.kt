package com.sbo.app.notify

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.sbo.app.data.Supa
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.Serializable

/**
 * Instant push (like WhatsApp): the server sends each notification through Firebase Cloud Messaging
 * the moment it's queued, and Android wakes this app's service to show it, even when the app is closed.
 * The 15-minute background check stays as a safety net; both paths share ids, so nothing shows twice.
 */
object Push {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Serializable private data class TokenRow(val token: String, val user_id: String, val app: String, val updated_at: String)

    /** True once Firebase is configured in this build (google-services.json present). */
    fun available(ctx: Context): Boolean = runCatching { FirebaseApp.getApps(ctx).isNotEmpty() || FirebaseApp.initializeApp(ctx) != null }.getOrDefault(false)

    /** Sends this phone's push token to the server for the signed-in user. */
    fun register(ctx: Context, token: String? = null) {
        if (!available(ctx)) return
        scope.launch {
            runCatching {
                Supa.client.auth.awaitInitialization()
                val uid = Supa.client.auth.currentUserOrNull()?.id ?: return@launch
                val t = token ?: FirebaseMessaging.getInstance().token.await()
                Supa.client.from("fcm_tokens").upsert(TokenRow(t, uid, ctx.packageName, java.time.Instant.now().toString()))
                ctx.getSharedPreferences("sbo-notify", Context.MODE_PRIVATE).edit().putBoolean("fcm", true).apply()
            }
        }
    }

    /** Forget this phone on sign-out, so the next account on it doesn't get someone else's messages. */
    fun unregister(ctx: Context) {
        if (!available(ctx)) return
        scope.launch {
            runCatching {
                val t = FirebaseMessaging.getInstance().token.await()
                Supa.client.from("fcm_tokens").delete { filter { eq("token", t) } }
            }
        }
    }

    fun active(ctx: Context) = ctx.getSharedPreferences("sbo-notify", Context.MODE_PRIVATE).getBoolean("fcm", false)
}

class SboMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) = Push.register(applicationContext, token)

    override fun onMessageReceived(msg: RemoteMessage) {
        val d = msg.data
        val title = d["title"] ?: msg.notification?.title ?: return
        val body = d["body"] ?: msg.notification?.body ?: ""
        val id = d["id"] ?: (title + body)
        Notifier.showQueued(applicationContext, id, title, body, d["tag"], d["url"])
    }
}
