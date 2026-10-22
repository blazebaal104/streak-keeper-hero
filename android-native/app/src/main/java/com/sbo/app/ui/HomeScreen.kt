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

@Composable
fun HomeScreen(vm: HomeViewModel) {
    val s by vm.state.collectAsState()
    HomeContent(s, onAdd = vm::add, onUndo = vm::undo, onEquip = vm::equip, onDayAdd = vm::addOnDay, onDayRemove = vm::removeOnDay, onAddFriend = vm::addFriend, onRemoveFriend = vm::removeFriend, onLeaveClan = vm::leaveClan)
}

@Composable
fun HomeContent(
    s: HomeState, onAdd: () -> Unit, onUndo: () -> Unit,
    onEquip: (slot: Int, badgeId: String?) -> Unit = { _, _ -> },
    onDayAdd: (java.time.LocalDate) -> Unit = {},
    onDayRemove: (java.time.LocalDate) -> Unit = {},
    onAddFriend: (String, (String) -> Unit) -> Unit = { _, _ -> },
    onRemoveFriend: (String) -> Unit = {},
    onLeaveClan: () -> Unit = {},
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
    var inventory by remember { mutableStateOf(openInventory) }
    val add = {
        rain.burst()
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
    CompositionLocalProvider(LocalTextStyle provides TextStyle(fontFamily = Inter, color = Sbo.foreground)) {
        Box(
            Modifier.fillMaxSize().background(Sbo.background).drawBehind {
                drawRect(Brush.radialGradient(0f to Sbo.surfaceTop, 0.7f to Sbo.background, center = Offset(size.width / 2f, 0f), radius = size.height * 0.75f))
            },
        ) {
            AmbientMood(if (s.fire > 0) "fire" else if (s.ice > 0) "ice" else "neutral")
            val p = s.profile
            when {
                p == null && s.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Sbo.primary) }
                p == null -> Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(if (s.error == "login") "Accesso non ancora disponibile in questa versione." else "Errore: ${s.error}", color = Sbo.mutedFg, textAlign = TextAlign.Center)
                }
                else -> Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp),
                ) {
                    Header(p, s.fire, s.ice)
                    TabsBar(tab) { tab = it }
                    Spacer(Modifier.height(32.dp))
                    when (tab) {
                        0 -> HomeTab(s, add, onUndo) { inventory = it }
                        1 -> CalendarTab(s.events, s.limit, today) { dayEdit = it }
                        2 -> TrendTab(s.events, today)
                        else -> SocialTab(
                            p, s.social, onToast = toast::show, onAddFriend = { addFriend = true },
                            onChat = { chatWith = it }, onClanChat = { clanChat = true }, onRemove = { confirmRemove = it }, onLeaveClan = { confirmLeave = true },
                        )
                    }
                    Spacer(Modifier.height(96.dp))
                }
            }
            DropRainOverlay(rain)
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
            if (p != null && slot != null) InventoryDialog(slot, p, s.badges, onClose = { inventory = null }) { id ->
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
private fun Header(p: Profile, fire: Int, ice: Int) {
    Row(Modifier.fillMaxWidth().padding(top = 32.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Avatar(p, 44)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(p.username, fontFamily = SpaceGrotesk, fontSize = 18.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.45).sp, maxLines = 1)
            Text(p.friend_code, color = Sbo.mutedFg, fontSize = 11.sp, lineHeight = 16.sp, fontFamily = FontFamily.Monospace)
        }
        Chip(Lucide.Flame, Sbo.orange, fire)
        Spacer(Modifier.width(6.dp))
        Chip(Lucide.Snowflake, Sbo.sky, ice)
        Spacer(Modifier.width(6.dp))
        RoundIcon(Lucide.Bell)
        Spacer(Modifier.width(8.dp))
        RoundIcon(Lucide.Settings)
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
private fun HomeTab(s: HomeState, onAdd: () -> Unit, onUndo: () -> Unit, onSlot: (Int) -> Unit) {
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
        StreakButton(s.fire, s.ice, onPress = onAdd)
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
