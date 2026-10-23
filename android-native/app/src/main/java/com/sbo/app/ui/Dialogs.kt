package com.sbo.app.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sbo.app.data.Badge
import com.sbo.app.data.Profile
import com.sbo.app.data.SboEvent
import com.sbo.app.data.Supa
import com.sbo.app.logic.BadgeDef
import com.sbo.app.logic.Badges
import com.sbo.app.logic.Streaks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.YearMonth

val AVATAR_CHOICES = listOf("💧", "🌊", "⚡", "🌟", "🔥", "🌙", "⚔️", "👑", "🐉", "🦁", "🐺", "🦅", "🌸", "🍀", "🎯", "🧊", "☄️", "🌈")

fun patch(vararg kv: Pair<String, Any?>): JsonObject = buildJsonObject {
    kv.forEach { (k, v) ->
        put(k, when (v) { null -> JsonNull; is Boolean -> JsonPrimitive(v); is Number -> JsonPrimitive(v); else -> JsonPrimitive(v.toString()) })
    }
}

/** Web compressImageToDataUrl: centre square crop, 640 x 640, JPEG 0.82, as a data URL. */
private fun compressToDataUrl(ctx: android.content.Context, uri: Uri): String? = runCatching {
    val src = ctx.contentResolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it) }
    val s = minOf(src.width, src.height)
    val crop = Bitmap.createBitmap(src, (src.width - s) / 2, (src.height - s) / 2, s, s)
    val out = Bitmap.createScaledBitmap(crop, 640, 640, true)
    val bos = ByteArrayOutputStream(); out.compress(Bitmap.CompressFormat.JPEG, 82, bos)
    "data:image/jpeg;base64," + Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP)
}.getOrNull()

@Composable
private fun Section(label: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth()) { UpperLabel(label); content() }
}

/** Web BadgeSlot (shared by the home and the profile). */
@Composable
fun MedalSlot(def: BadgeDef?, onClick: () -> Unit) {
    val press = rememberPressScale()
    val m = Modifier.graphicsScale(press).pressable(press, def?.name ?: "Slot medaglia", onClick)
    if (def != null) { Box(m) { BadgeArt(def, 64.dp) }; return }
    Column(
        m.size(64.dp).clip(RoundedCornerShape(28.dp)).background(Sbo.card.copy(alpha = 0.7f)).dashedBorder(Sbo.primary.copy(alpha = 0.5f), 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        Icon(Lucide.Plus, null, tint = Sbo.primary.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
        Text("slot", color = Sbo.primary.copy(alpha = 0.7f), fontSize = 9.sp, lineHeight = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

internal fun Modifier.dashedBorder(color: Color, radius: androidx.compose.ui.unit.Dp, width: androidx.compose.ui.unit.Dp = 2.dp) = this.drawBehind {
    val sw = width.toPx()
    drawRoundRect(
        color, topLeft = androidx.compose.ui.geometry.Offset(sw / 2, sw / 2), size = androidx.compose.ui.geometry.Size(size.width - sw, size.height - sw),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius.toPx()),
        style = androidx.compose.ui.graphics.drawscope.Stroke(sw, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))),
    )
}

/** "Il tuo profilo" (tap your avatar): photo, emoji avatar, username + title, code, medal slots, next medals, sign out. */
@Composable
fun ProfileDialog(p: Profile, badges: List<Badge>, onClose: () -> Unit, onUpdate: (Profile, JsonObject) -> Unit, onSlot: (Int) -> Unit, onSignOut: () -> Unit, onToast: (String) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var uploading by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            uploading = true
            val url = withContext(Dispatchers.Default) { compressToDataUrl(ctx, uri) }
            uploading = false
            if (url != null) { onUpdate(p.copy(avatar_url = url), patch("avatar_url" to url)); onToast("Foto profilo aggiornata!") } else onToast("Impossibile caricare l'immagine")
        }
    }
    SboDialog(onClose) {
        DialogTitle("Il tuo profilo")
        Spacer(Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(27.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(72.dp).clip(CircleShape).border(2.dp, Sbo.primary.copy(alpha = 0.4f), CircleShape)) { AvatarSmall(p, 72) }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallButton(if (uploading) "Carico…" else "Carica foto", Lucide.Camera, modifier = Modifier.fillMaxWidth(), enabled = !uploading) {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                    if (p.avatar_url != null) SmallButton("Rimuovi foto", variant = "ghost", modifier = Modifier.fillMaxWidth()) { onUpdate(p.copy(avatar_url = null), patch("avatar_url" to null)) }
                }
            }
            Section("Avatar emoji") {
                Spacer(Modifier.height(8.dp))
                AVATAR_CHOICES.chunked(9).forEach { row ->
                    Row(Modifier.fillMaxWidth().padding(bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        row.forEach { a ->
                            val on = p.avatar == a
                            Box(
                                Modifier.size(36.dp).clip(RoundedCornerShape(20.dp))
                                    .then(if (on) Modifier.background(Sbo.primary.copy(alpha = 0.3f)).border(2.dp, Sbo.primary, RoundedCornerShape(20.dp)) else Modifier)
                                    .clickable { onUpdate(p.copy(avatar = a), patch("avatar" to a)) },
                                contentAlignment = Alignment.Center,
                            ) { Text(a, fontSize = 18.sp, lineHeight = 24.sp) }
                        }
                    }
                }
                Text("Usato quando non hai una foto.", color = Sbo.mutedFg, fontSize = 10.sp, lineHeight = 14.sp)
            }
            Section("Username") {
                Spacer(Modifier.height(4.dp))
                Text(p.username, fontSize = 18.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold)
                Titles.byId(p.equipped_title)?.let { TitleBadge(it, "sm") }
                Spacer(Modifier.height(5.dp))
                Text("L'username non è modificabile dopo la creazione.", color = Sbo.mutedFg, fontSize = 11.sp, lineHeight = 16.sp)
            }
            Section("Codice amico") {
                Spacer(Modifier.height(9.dp))
                Text(p.friend_code, fontFamily = FontFamily.Monospace, fontSize = 18.sp, lineHeight = 28.sp, letterSpacing = 1.8.sp)
            }
            Section("Slot medaglie") {
                Spacer(Modifier.height(4.dp))
                Text("Tocca uno slot per scegliere dalla tua collezione.", color = Sbo.mutedFg, fontSize = 11.sp, lineHeight = 16.sp)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)) {
                    listOf(p.slot_1, p.slot_2, p.slot_3).forEachIndexed { i, id ->
                        val b = id?.let { bid -> badges.firstOrNull { it.id == bid } }
                        MedalSlot(b?.let { Badges.def(it.category, it.threshold) }) { onSlot(i) }
                    }
                }
            }
            StreakProgress(badges)
            SboButton("Esci dall'account", Lucide.LogOut, variant = "destructive", onClick = onSignOut)
        }
    }
}

@Composable
private fun StreakProgress(badges: List<Badge>) {
    val fire = badges.filter { it.category == "fire" || it.category == "streak" }.map { it.threshold }.toSet()
    val ice = badges.filter { it.category == "ice" }.map { it.threshold }.toSet()
    val nf = Badges.FIRE.firstOrNull { it.threshold !in fire }
    val ni = Badges.ICE.firstOrNull { it.threshold !in ice }
    if (nf == null && ni == null) { Text("🏆 Hai tutte le medaglie!", color = Sbo.mutedFg, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()); return }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        @Composable fun Box1(title: String, n: BadgeDef, label: String) {
            Column(Modifier.fillMaxWidth().outlined().padding(horizontal = 12.dp, vertical = 17.dp)) {
                Text(title, color = Sbo.mutedFg, fontSize = 12.sp, lineHeight = 16.sp)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(n.emoji, fontSize = 20.sp, lineHeight = 28.sp)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            androidx.compose.ui.text.buildAnnotatedString {
                                append(n.name); append(" ")
                                pushStyle(androidx.compose.ui.text.SpanStyle(fontSize = 10.sp, fontWeight = FontWeight.Normal, color = Sbo.mutedFg)); append("(${n.tier.label})"); pop()
                            },
                            fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold,
                        )
                        Text("$label: ${n.threshold} giorni", color = Sbo.mutedFg, fontSize = 11.sp, lineHeight = 16.sp)
                    }
                }
            }
        }
        if (nf != null) Box1("🔥 Prossima medaglia di fuoco", nf, "streak")
        if (ni != null) Box1("❄️ Prossima medaglia di ghiaccio", ni, "giorni puliti")
    }
}

/** Settings (web: limits + notifications; Progetto Zero lives on top once enabled). */
@Composable
fun SettingsDialog(p: Profile, onClose: () -> Unit, onUpdate: (Profile, JsonObject) -> Unit, onToast: (String) -> Unit, onEnableNotifications: () -> Unit = {}, onTestPush: () -> Unit = {}, pzCard: @Composable () -> Unit = { PzCard(false, "Trasforma SBO nel tuo personal trainer. Minimo 7 giorni.") { onToast("Progetto Zero arriva nella prossima versione nativa.") } }) {
    SboDialog(onClose) {
        DialogTitle("Impostazioni")
        DialogDesc("Personalizza SBO come vuoi tu.")
        Spacer(Modifier.height(24.dp))
        // web: the options scroll inside a max-h-[65vh] box, "Fatto" stays visible under it
        val maxH = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp * 0.65f
        Column(Modifier.heightIn(max = maxH).verticalScroll(androidx.compose.foundation.rememberScrollState()).padding(end = 5.dp, top = 8.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            pzCard()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    FieldLabel("Limite adattivo")
                    Spacer(Modifier.height(2.dp))
                    Text("Basato sulle tue ultime 2 settimane", color = Sbo.mutedFg, fontSize = 12.sp, lineHeight = 16.sp)
                }
                SboSwitch(p.use_adaptive_limit) { onUpdate(p.copy(use_adaptive_limit = it), patch("use_adaptive_limit" to it)) }
            }
            if (!p.use_adaptive_limit) Column {
                Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) { FieldLabel("Limite giornaliero") }
                    Text("${p.daily_limit}", fontFamily = FontFamily.Monospace, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
                var draft by remember(p.daily_limit) { mutableStateOf(p.daily_limit) }
                SboSlider(draft, 1, 10, onChange = { draft = it }, onCommit = { onUpdate(p.copy(daily_limit = it), patch("daily_limit" to it)) })
            }
            Row(Modifier.fillMaxWidth().outlined().padding(horizontal = 12.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    FieldLabel("Notifiche di incoraggiamento")
                    Spacer(Modifier.height(2.dp))
                    Text("Toast in-app e promemoria streak", color = Sbo.mutedFg, fontSize = 12.sp, lineHeight = 16.sp)
                }
                SboSwitch(p.notifications_enabled) { onUpdate(p.copy(notifications_enabled = it), patch("notifications_enabled" to it)) }
            }
            Column(Modifier.fillMaxWidth().outlined().padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                UpperLabel("Cosa vuoi ricevere")
                @Composable fun Line(label: String, v: Boolean, key: String, copy: (Boolean) -> Profile) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(label, fontSize = 14.sp, lineHeight = 14.sp, modifier = Modifier.weight(1f))
                        SboSwitch(v) { onUpdate(copy(it), patch(key to it)) }
                    }
                }
                Line("💬 Messaggi", p.notif_messages, "notif_messages") { p.copy(notif_messages = it) }
                Line("🔥 Streak a rischio", p.notif_streak, "notif_streak") { p.copy(notif_streak = it) }
                Line("🛡️ Amici e clan", p.notif_social, "notif_social") { p.copy(notif_social = it) }
            }
            SboButton("Attiva notifiche del sistema", Lucide.Bell, variant = "secondary", onClick = onEnableNotifications)
            SboButton("Invia notifica di prova", variant = "ghost", fontSize = 12, onClick = onTestPush)
        }
        Spacer(Modifier.height(16.dp))
        SboButton("Fatto", onClick = onClose)
    }
}

/** A friend's profile (web FriendProfileDialog). */
@Composable
fun FriendProfileDialog(friend: Profile, onClose: () -> Unit, onChat: () -> Unit, today: LocalDate = LocalDate.now(), preload: Pair<List<SboEvent>, List<Badge>>? = null) {
    var prof by remember { mutableStateOf(friend) }
    var events by remember { mutableStateOf(preload?.first ?: emptyList()) }
    var badges by remember { mutableStateOf(preload?.second ?: emptyList()) }
    var full by remember { mutableStateOf(false) }
    LaunchedEffect(friend.id) {
        if (preload != null) return@LaunchedEffect
        runCatching { Supa.profileById(friend.id) }.getOrNull()?.let { prof = it }
        events = runCatching { Supa.eventsOf(friend.id) }.getOrDefault(emptyList())
        badges = runCatching { Supa.badgesOf(friend.id) }.getOrDefault(emptyList())
    }
    val byDay = remember(events) { Streaks.byDay(events) }
    var month by remember { mutableStateOf(YearMonth.from(today)) }
    SboDialog(onClose) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(56.dp).clip(CircleShape).border(2.dp, Sbo.primary.copy(alpha = 0.4f), CircleShape).clickable { full = true }) { AvatarSmall(prof, 56) }
            Spacer(Modifier.width(12.dp))
            // shadcn DialogHeader is text-center on phones
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(prof.username, fontSize = 18.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.45).sp)
                Spacer(Modifier.height(2.dp))
                Titles.byId(prof.equipped_title)?.let { TitleBadge(it, "sm") }
                    ?: Text(prof.friend_code, color = Sbo.mutedFg, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
        }
        Spacer(Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MiniStat("Fuoco", "${Streaks.fire(events)}", Lucide.Flame, Color(0xFFFB923C), Modifier.weight(1f))
                MiniStat("Ghiaccio", "${Streaks.ice(events)}", Lucide.Snowflake, Color(0xFF7DD3FC), Modifier.weight(1f))
                MiniStat("Mese", "${Streaks.countThisMonth(events)}", null, Color.Unspecified, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MiniStat("🔥 Record fuoco", "${Streaks.maxFire(events)} gg", null, Color.Unspecified, Modifier.weight(1f), big = false)
                MiniStat("❄️ Record ghiaccio", "${Streaks.maxIce(events)} gg", null, Color.Unspecified, Modifier.weight(1f), big = false)
            }
            Column {
                UpperLabel("Medaglie (${badges.size})")
                Spacer(Modifier.height(8.dp))
                if (badges.isEmpty()) Text("Nessuna ancora.", color = Sbo.mutedFg, fontSize = 12.sp)
                else badges.mapNotNull { Badges.def(it.category, it.threshold) }.chunked(5).forEach { row ->
                    Row(Modifier.padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { row.forEach { BadgeArt(it, 55.dp) } }
                }
            }
            Column {
                UpperLabel("Calendario")
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    MonthGrid(month, today, byDay, Int.MAX_VALUE, onPrev = { month = month.minusMonths(1) }, onNext = { month = month.plusMonths(1) }, onDay = null)
                }
            }
            Column {
                UpperLabel("Andamento (30gg)")
                Spacer(Modifier.height(8.dp))
                val s30 = remember(events) { dailySeries(events, 30, today) }
                if (s30.all { it.count == 0 }) Text("Nessun dato ancora.", color = Sbo.mutedFg, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp))
                else {
                    val counts = s30.map { it.count.toFloat() }
                    // web: an AreaChart, which draws only its Area (the "Media" Line child is not rendered)
                    Chart(s30.map { it.date }, listOf(ChartSeries("Al giorno", counts, Color(0xFFFB923C), area = true)), height = 160)
                }
            }
            SboButton("Manda un messaggio", Lucide.MessageCircle, onClick = onChat)
        }
    }
    if (full) AvatarFullscreen(prof) { full = false }
}

@Composable
private fun MiniStat(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector?, tint: Color, modifier: Modifier, big: Boolean = true) {
    Column(modifier.sboCard().padding(horizontal = 12.dp, vertical = 15.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) { Icon(icon, null, tint = tint, modifier = Modifier.size(12.dp)); Spacer(Modifier.width(4.dp)) }
            Text(label, color = Sbo.mutedFg, fontSize = 12.sp, lineHeight = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(4.dp))
        Text(value, fontSize = if (big) 24.sp else 20.sp, lineHeight = if (big) 32.sp else 28.sp, fontWeight = FontWeight.Bold)
    }
}

/** Full-screen photo (or emoji) on black, tap anywhere to close. */
@Composable
fun AvatarFullscreen(p: Profile, onClose: () -> Unit) {
    androidx.activity.compose.BackHandler(onBack = onClose)
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.95f)).clickable(remember { MutableInteractionSource() }, indication = null, onClick = onClose),
        contentAlignment = Alignment.Center,
    ) {
        val bmp = rememberAvatarBitmap(p.avatar_url)
        if (bmp != null) Image(bmp, "Foto di ${p.username}", contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth())
        else Text(p.avatar, fontSize = 200.sp, lineHeight = 220.sp)
    }
}

/** Bell: pending clan invites. */
@Composable
fun InvitesDialog(invites: List<InviteInfo>, onClose: () -> Unit, onAccept: (String) -> Unit, onDecline: (String) -> Unit) {
    SboDialog(onClose) {
        DialogTitle("Notifiche")
        DialogDesc("Inviti al clan in attesa.")
        Spacer(Modifier.height(16.dp))
        if (invites.isEmpty()) Text("Nessun invito in sospeso.", color = Sbo.mutedFg, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp))
        else Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            invites.forEach { inv ->
                Row(Modifier.fillMaxWidth().outlined().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Lucide.Shield, null, tint = Sbo.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            androidx.compose.ui.text.buildAnnotatedString {
                                append(inv.clanName); append(" ")
                                pushStyle(androidx.compose.ui.text.SpanStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Sbo.mutedFg, fontWeight = FontWeight.Normal)); append("[${inv.clanTag}]"); pop()
                            },
                            fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
                        )
                        Text("Invito da ${inv.fromName.ifEmpty { "…" }}", color = Sbo.mutedFg, fontSize = 11.sp, lineHeight = 16.sp)
                    }
                    SmallButton(icon = Lucide.Check, variant = "default") { onAccept(inv.id) }
                    Spacer(Modifier.width(12.dp))
                    SmallButton(icon = Lucide.X, variant = "ghost") { onDecline(inv.id) }
                }
            }
        }
    }
}

/** "Gestisci clan": create (name + 2-5 letter tag) or join by tag. */
@Composable
fun ClanManageDialog(onClose: () -> Unit, onCreate: (String, String) -> Unit, onJoin: (String) -> Unit) {
    var tab by remember { mutableStateOf(0) }
    var name by remember { mutableStateOf("") }
    var tag by remember { mutableStateOf("") }
    var join by remember { mutableStateOf("") }
    SboDialog(onClose) {
        DialogTitle("Gestisci clan")
        Spacer(Modifier.height(16.dp))
        SegTabs(listOf("Crea", "Unisciti"), tab) { tab = it }
        Spacer(Modifier.height(16.dp))
        val mono = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp, color = Sbo.foreground)
        if (tab == 0) Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column { FieldLabel("Nome del clan"); Spacer(Modifier.height(8.dp)); SboInput(name, { name = it.take(24) }, "I Guerrieri della Goccia", Modifier.fillMaxWidth()) }
            Column { FieldLabel("Tag (2-5 caratteri)"); Spacer(Modifier.height(8.dp)); SboInput(tag, { tag = it.uppercase().take(5) }, "ABC", Modifier.fillMaxWidth(), style = mono, caps = true) }
            SboButton("Crea clan", enabled = name.trim().length >= 3 && tag.trim().length >= 2) { onCreate(name.trim(), tag.trim()) }
        } else Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column { FieldLabel("Tag del clan a cui unirti"); Spacer(Modifier.height(8.dp)); SboInput(join, { join = it.uppercase().take(5) }, "ABC", Modifier.fillMaxWidth(), style = mono, caps = true) }
            SboButton("Unisciti", enabled = join.trim().length >= 2) { onJoin(join.trim()) }
        }
    }
}

@Composable
fun InviteFriendToClanDialog(friends: List<Profile>, memberIds: Set<String>, onClose: () -> Unit, onInvite: (Profile) -> Unit) {
    val available = friends.filter { it.id !in memberIds }
    SboDialog(onClose) {
        DialogTitle("Invita un amico nel clan")
        DialogDesc("Riceverà un messaggio privato con il tuo invito.")
        Spacer(Modifier.height(16.dp))
        if (available.isEmpty()) Text("Tutti i tuoi amici sono già nel clan, o non ne hai ancora.", color = Sbo.mutedFg, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp))
        else Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            available.forEach { f ->
                Row(Modifier.fillMaxWidth().outlined().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    AvatarSmall(f, 36); Spacer(Modifier.width(12.dp))
                    Text(f.username, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    SmallButton("Invita", variant = "default") { onInvite(f) }
                }
            }
        }
    }
}

@Composable
fun ConfirmDialog(question: String, confirm: String, onClose: () -> Unit, onConfirm: () -> Unit) {
    SboDialog(onClose) {
        DialogTitle(question)
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SboButton("Annulla", variant = "secondary", modifier = Modifier.weight(1f), onClick = onClose)
            SboButton(confirm, modifier = Modifier.weight(1f)) { onConfirm(); onClose() }
        }
    }
}

/** Progetto Zero box at the top of the settings (web: rounded-2xl, primary/40 border, primary/10 fill, blurred glow top-right). */
@Composable
fun PzCard(active: Boolean, desc: String, extra: @Composable () -> Unit = {}, onToggle: (Boolean) -> Unit) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Sbo.primary.copy(alpha = 0.10f))
            .drawBehind {
                // web: a 96px primary/30 circle at -32px,-32px of the top-right corner, blurred 40px
                val c = androidx.compose.ui.geometry.Offset(size.width + 16.dp.toPx(), 16.dp.toPx())
                val rad = 48.dp.toPx() + 40.dp.toPx()
                drawCircle(androidx.compose.ui.graphics.Brush.radialGradient(0f to Sbo.primary.copy(alpha = 0.30f), 0.45f to Sbo.primary.copy(alpha = 0.18f), 1f to Color.Transparent, center = c, radius = rad), radius = rad, center = c)
            }
            .border(1.dp, Sbo.primary.copy(alpha = 0.4f), RoundedCornerShape(28.dp)),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Lucide.Target, null, tint = Sbo.primary, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp))
                        Text("Progetto Zero", fontSize = 16.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(desc, color = Sbo.mutedFg, fontSize = 12.sp, lineHeight = 16.sp)
                }
                Spacer(Modifier.width(12.dp))
                SboSwitch(active, onToggle)
            }
            extra()
        }
    }
}
