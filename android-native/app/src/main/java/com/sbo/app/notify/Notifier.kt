package com.sbo.app.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.sbo.app.MainActivity
import com.sbo.app.R
import com.sbo.app.data.Supa
import com.sbo.app.logic.Streaks
import io.github.jan.supabase.auth.auth
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/**
 * Safety net for system notifications: a background job reads the same queue the web push uses
 * (scheduled_notifications: messages, badges, invites, reminders…), plus Coach Zero's new messages and the
 * evening "streak at risk" reminder, and shows them as Android notifications. Runs every 15 minutes
 * (Android's minimum for background jobs) and right away whenever the app goes to the background.
 */
object Notifier {
    private const val CHANNEL = "sbo"
    private const val PREFS = "sbo-notify"

    /** App on screen / chat currently open (no notification for the chat you're reading). */
    @Volatile var foreground = false
    @Volatile var openChat: String? = null
    /** Instant push (FCM) is set up on this phone. */
    @Volatile var fcm = false
    /** Called when a push lands while the app is running (the screen refreshes at once). */
    @Volatile var onPush: (() -> Unit)? = null

    /**
     * Shows one queued notification (from FCM or the background check) exactly once:
     * both paths use the server row id, so the same message never pops up twice.
     */
    fun showQueued(ctx: Context, id: String, title: String, body: String, tag: String?, url: String?) {
        onPush?.invoke()
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        synchronized(this) {
            val seen = prefs.getString("shown", "")!!.split(',').filter { it.isNotEmpty() }
            if (id in seen) return
            prefs.edit().putString("shown", (seen.takeLast(300) + id).joinToString(",")).apply()
        }
        if (foreground && tag != null && openChat != null && tag == "dm:$openChat") return
        // one notification per conversation (like WhatsApp): a new message replaces the previous one of the same chat
        val nid = (tag?.takeIf { it.startsWith("dm:") || it.startsWith("clan:") } ?: id).hashCode()
        show(ctx, nid, title, body, openOf(url))
    }

    fun ensureChannel(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL) == null) {
            nm.createNotificationChannel(NotificationChannel(CHANNEL, "SBO", NotificationManager.IMPORTANCE_HIGH).apply { description = "Messaggi, inviti, medaglie e promemoria" })
        }
    }

    fun allowed(ctx: Context) = (android.os.Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
        NotificationManagerCompat.from(ctx).areNotificationsEnabled()

    fun schedule(ctx: Context) {
        val req = PeriodicWorkRequestBuilder<PollWorker>(15, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build()
        WorkManager.getInstance(ctx).enqueueUniquePeriodicWork("sbo-poll", ExistingPeriodicWorkPolicy.KEEP, req)
    }

    fun pollSoon(ctx: Context, delaySec: Long = 0) {
        val req = OneTimeWorkRequestBuilder<PollWorker>().setInitialDelay(delaySec, TimeUnit.SECONDS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build()
        WorkManager.getInstance(ctx).enqueueUniqueWork("sbo-poll-now", ExistingWorkPolicy.REPLACE, req)
    }

    fun show(ctx: Context, id: Int, title: String, body: String, open: String?) {
        if (!allowed(ctx)) return
        ensureChannel(ctx)
        val intent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (open != null) putExtra("open", open)
        }
        val pi = PendingIntent.getActivity(ctx, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_notify).setContentTitle(title).setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body)).setAutoCancel(true).setContentIntent(pi)
            .setPriority(NotificationCompat.PRIORITY_HIGH).build()
        try { NotificationManagerCompat.from(ctx).notify(id, n) } catch (_: SecurityException) {}
    }

    /** "/?open=dm:<id>" → "dm:<id>" (same deep links as the web push). */
    fun openOf(url: String?): String? = url?.substringAfter("open=", "")?.substringBefore("&")?.takeIf { it.isNotEmpty() }

    /** Show a queue row right away (test push) and move the cursor so the background job won't repeat it. */
    fun showNow(ctx: Context, uid: String, n: com.sbo.app.data.ScheduledNotification) {
        showQueued(ctx, n.id, n.title, n.body, n.tag, n.url)
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val cur = prefs.getString("after:$uid", null)
        if (cur == null || Instant.parse(normIso(n.deliver_at)) > Instant.parse(normIso(cur))) prefs.edit().putString("after:$uid", n.deliver_at).apply()
    }

    /** Postgres "2026-10-23 09:00:42.18+00" → ISO-8601 Instant text. */
    private fun normIso(t: String): String = runCatching { java.time.OffsetDateTime.parse(t.replace(' ', 'T').replace(Regex("([+-]\\d\\d)$"), "$1:00")).toInstant().toString() }.getOrDefault(t)

    suspend fun poll(ctx: Context) {
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        Supa.client.auth.awaitInitialization()
        val uid = Supa.client.auth.currentUserOrNull()?.id ?: return
        val now = Instant.now()
        // 1) the server's notification queue
        val after = prefs.getString("after:$uid", null) ?: now.minusSeconds(3600).toString().also { prefs.edit().putString("after:$uid", it).apply() }
        val due = runCatching { Supa.dueNotifications(uid, after, now.toString()) }.getOrNull() ?: return
        due.forEach { n -> showQueued(ctx, n.id, n.title, n.body, n.tag, n.url) }
        due.lastOrNull()?.let { prefs.edit().putString("after:$uid", it.deliver_at).apply() }
        // Coach Zero's automatic messages and the evening "streak at risk" reminder are sent by the server now
        // (only what matters reaches the lock screen: messages, invites, new friends, medals, streak about to die).
    }
}

class PollWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result { runCatching { Notifier.poll(applicationContext) }; return Result.success() }
}
