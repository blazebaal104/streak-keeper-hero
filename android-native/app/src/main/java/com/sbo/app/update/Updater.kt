package com.sbo.app.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import com.sbo.app.BuildConfig
import com.sbo.app.data.Supa
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** A published version of the app (table app_releases; a row is visible only once its available_at has passed). */
@Serializable
data class AppRelease(val app: String, val version_code: Int, val version_name: String, val url: String, val title: String? = null, val notes: String? = null)

/**
 * In-app updates: the app asks the server whether a newer signed APK is out, downloads it inside the app
 * and hands it to Android's installer, so updating is one tap.
 */
object Updater {
    suspend fun check(ctx: Context): AppRelease? = runCatching {
        Supa.client.from("app_releases").select {
            filter { eq("app", ctx.packageName); gt("version_code", BuildConfig.VERSION_CODE) }
            order("version_code", Order.DESCENDING)
            limit(1)
        }.decodeList<AppRelease>().firstOrNull()
    }.getOrNull()

    /** Downloads the APK into the app's cache, reporting progress 0..1. */
    suspend fun download(ctx: Context, r: AppRelease, progress: (Float) -> Unit): File = withContext(Dispatchers.IO) {
        val dir = File(ctx.cacheDir, "updates").apply { mkdirs() }
        dir.listFiles()?.forEach { if (!it.name.contains("-${r.version_code}.")) it.delete() }
        val out = File(dir, "sbo-${r.version_code}.apk")
        if (out.exists() && out.length() > 1_000_000) { progress(1f); return@withContext out }
        val tmp = File(dir, "sbo-${r.version_code}.part")
        var url = URL(r.url)
        var conn: HttpURLConnection
        var hops = 0
        while (true) {
            conn = (url.openConnection() as HttpURLConnection).apply { instanceFollowRedirects = false; connectTimeout = 15000; readTimeout = 30000 }
            val code = conn.responseCode
            if (code in 300..399 && hops++ < 5) { url = URL(url, conn.getHeaderField("Location")); conn.disconnect(); continue }
            if (code !in 200..299) error("HTTP $code")
            break
        }
        val total = conn.contentLengthLong.takeIf { it > 0 } ?: -1L
        conn.inputStream.use { input ->
            tmp.outputStream().use { o ->
                val buf = ByteArray(64 * 1024)
                var done = 0L
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    o.write(buf, 0, n); done += n
                    if (total > 0) progress((done.toFloat() / total).coerceIn(0f, 0.99f))
                }
            }
        }
        conn.disconnect()
        tmp.renameTo(out)
        progress(1f)
        out
    }

    /** Android asks once per app: "allow SBO to install updates". */
    fun canInstall(ctx: Context) = ctx.packageManager.canRequestPackageInstalls()

    fun openInstallPermission(ctx: Context) {
        ctx.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${ctx.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    /** Opens the system "update this app?" screen for the downloaded APK. */
    fun install(ctx: Context, apk: File) {
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.updates", apk)
        ctx.startActivity(
            Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}
