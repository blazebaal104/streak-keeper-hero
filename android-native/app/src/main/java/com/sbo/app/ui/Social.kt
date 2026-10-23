package com.sbo.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sbo.app.data.ClanMessage
import com.sbo.app.data.DirectMessage
import com.sbo.app.data.Profile
import com.sbo.app.data.Supa
import com.sbo.app.logic.Streaks
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// ---------------------------------------------------------------- small shadcn pieces

/** shadcn Button size "sm" (h-8, px-3, text-xs, rounded-md); variant secondary / ghost / default. */
@Composable
internal fun SmallButton(
    text: String? = null, icon: ImageVector? = null, variant: String = "secondary", modifier: Modifier = Modifier,
    enabled: Boolean = true, height: Dp = 32.dp, fontSize: Int = 12, onClick: () -> Unit,
) {
    val press = rememberPressScale()
    val bg = when (variant) { "secondary" -> Sbo.secondary; "default" -> Sbo.primary; else -> Color.Transparent }
    val fg = when (variant) { "default" -> Sbo.primaryFg; else -> Sbo.foreground }
    Row(
        modifier.height(height).graphicsLayer { scaleX = press.value; scaleY = press.value; alpha = if (enabled) 1f else 0.5f }
            .clip(RoundedCornerShape(18.dp)).background(bg)
            .then(if (enabled) Modifier.pressable(press, text ?: "", onClick) else Modifier)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Icon(icon, null, tint = if (variant == "ghost") Sbo.mutedFg else fg, modifier = Modifier.size(16.dp))
        if (icon != null && text != null) Spacer(Modifier.width(14.dp)) // web: icon mr-1.5 + gap-2
        if (text != null) Text(text, color = fg, fontSize = fontSize.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text.uppercase(), color = Sbo.mutedFg, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.6.sp)
}

@Composable
private fun DashedCard(icon: ImageVector, text: String, action: (@Composable () -> Unit)? = null) {
    Column(
        Modifier.fillMaxWidth().drawBehind {
            val w = 1.dp.toPx()
            drawRoundRect(
                Color.White.copy(alpha = 0.10f), topLeft = Offset(w / 2, w / 2), size = androidx.compose.ui.geometry.Size(size.width - w, size.height - w),
                cornerRadius = CornerRadius(24.dp.toPx()), style = Stroke(w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))),
            )
        }.padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null, tint = Sbo.mutedFg, modifier = Modifier.size(32.dp))
        Spacer(Modifier.height(8.dp))
        Text(text, color = Sbo.mutedFg, fontSize = 14.sp, lineHeight = 20.sp, textAlign = TextAlign.Center)
        if (action != null) { Spacer(Modifier.height(12.dp)); action() }
    }
}

/** Web AvatarView: round, bg-card/90, emoji at 55% of the size, or the uploaded picture. */
@Composable
internal fun AvatarSmall(p: Profile, size: Int) {
    Box(Modifier.size(size.dp).clip(CircleShape).background(Sbo.card.copy(alpha = 0.9f)), contentAlignment = Alignment.Center) {
        val bmp = rememberAvatarBitmap(p.avatar_url)
        if (bmp != null) androidx.compose.foundation.Image(bmp, null, contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.fillMaxSize())
        else Text(p.avatar, fontSize = (size * 0.55f * 0.8f).sp, lineHeight = (size * 0.6f).sp)
    }
}

// ---------------------------------------------------------------- the tab

@Composable
fun SocialTab(
    me: Profile, social: com.sbo.app.ui.Social,
    onToast: (String) -> Unit, onAddFriend: () -> Unit, onChat: (Profile) -> Unit, onClanChat: () -> Unit, onRemove: (Profile) -> Unit, onLeaveClan: () -> Unit = {},
    onOpenFriend: (Profile) -> Unit = {}, onManageClan: () -> Unit = {}, onInviteToClan: () -> Unit = {},
    onSetRole: (String, String) -> Unit = { _, _ -> }, onKick: (Profile) -> Unit = {},
    coach: (@Composable () -> Unit)? = null,
) {
    val clip = LocalClipboardManager.current
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        coach?.invoke()
        Card(padding = 20.dp) {
            Column {
                SectionLabel("Il tuo codice amico")
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(me.friend_code, fontFamily = FontFamily.Monospace, fontSize = 24.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.4.sp, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    SmallButton("Copia", Lucide.Copy) { clip.setText(AnnotatedString(me.friend_code)); onToast("📋 Codice copiato!") }
                }
            }
        }
        Column {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { SectionLabel("Clan") }
                if (social.clan != null) Row(Modifier.clickable(onClick = onLeaveClan), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Lucide.X, null, tint = Sbo.mutedFg, modifier = Modifier.size(12.dp)); Spacer(Modifier.width(4.dp))
                    Text("esci", color = Sbo.mutedFg, fontSize = 12.sp, lineHeight = 16.sp)
                }
            }
            val clan = social.clan
            if (clan != null) ClanCard(clan, social.members, me, onClanChat, onInviteToClan, onSetRole, onKick)
            else DashedCard(Lucide.Shield, "Crea o unisciti a un clan per chattare e competere.") {
                SmallButton("Gestisci clan", variant = "default", onClick = onManageClan)
            }
        }
        Column {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { SectionLabel("Amici") }
                SmallButton("Aggiungi", Lucide.UserPlus, onClick = onAddFriend)
            }
            if (social.friends.isEmpty()) DashedCard(Lucide.Users, "Non hai ancora amici. Aggiungine uno con il suo codice.")
            else Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                social.friends.forEach { f -> FriendCard(f, social.unread[f.profile.id] ?: 0, onOpen = { onOpenFriend(f.profile) }, onChat = { onChat(f.profile) }, onRemove = { onRemove(f.profile) }) }
            }
        }
    }
}

@Composable
private fun FriendCard(f: FriendRow, unread: Int, onOpen: () -> Unit, onChat: () -> Unit, onRemove: () -> Unit) {
    Card(padding = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Row(Modifier.weight(1f).clickable(remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null, onClick = onOpen), verticalAlignment = Alignment.CenterVertically) {
            AvatarSmall(f.profile, 40)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(f.profile.username, fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Stat(Lucide.Flame, Color(0xFFFB923C), "${f.fire}"); Spacer(Modifier.width(8.dp))
                    Stat(Lucide.Snowflake, Color(0xFF7DD3FC), "${f.ice}"); Spacer(Modifier.width(8.dp))
                    Stat(Lucide.Droplets, Sbo.primary, "${f.today} oggi")
                }
            }
          }
            Spacer(Modifier.width(12.dp))
            Box {
                SmallButton(icon = Lucide.MessageCircle, onClick = onChat)
                if (unread > 0) Box(
                    Modifier.align(Alignment.TopEnd).offset(x = 6.dp, y = (-6).dp).height(16.dp).widthIn(min = 16.dp)
                        .clip(CircleShape).background(Sbo.destructive).padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center,
                ) { Text(if (unread > 9) "9+" else "$unread", color = Color.White, fontSize = 10.sp, lineHeight = 12.sp, fontWeight = FontWeight.Bold) }
            }
            Spacer(Modifier.width(12.dp))
            SmallButton(icon = Lucide.Trash2, variant = "ghost", onClick = onRemove)
        }
    }
}

@Composable
private fun Stat(icon: ImageVector, tint: Color, text: String) {
    Icon(icon, null, tint = tint, modifier = Modifier.size(12.dp))
    Spacer(Modifier.width(2.dp))
    Text(text, color = Sbo.mutedFg, fontSize = 11.sp, lineHeight = 16.sp)
}

@Composable
private fun ClanCard(clan: com.sbo.app.data.Clan, members: List<Profile>, me: Profile, onChat: () -> Unit, onInvite: () -> Unit, onSetRole: (String, String) -> Unit, onKick: (Profile) -> Unit) {
    val canInvite = me.clan_role in setOf("leader", "co_leader", "recruit")
    val leader = me.clan_role == "leader"
    Card(padding = 20.dp) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(44.dp).clip(RoundedCornerShape(24.dp)).background(Brush.linearGradient(listOf(Sbo.accent, Sbo.primary), start = Offset.Zero, end = Offset.Infinite)),
                    contentAlignment = Alignment.Center,
                ) { Icon(Lucide.Shield, null, tint = Sbo.primaryFg, modifier = Modifier.size(20.dp)) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("[${clan.tag}]", color = Sbo.mutedFg, fontSize = 12.sp, lineHeight = 16.sp)
                    Text(clan.name, fontSize = 18.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold)
                }
                SmallButton("Chat", Lucide.MessageCircle, onClick = onChat)
            }
            if (canInvite) {
                Spacer(Modifier.height(12.dp))
                SmallButton("Invita un amico", Lucide.UserPlus, modifier = Modifier.fillMaxWidth(), onClick = onInvite)
            }
            Spacer(Modifier.height(16.dp))
            val order = mapOf("leader" to 0, "co_leader" to 1, "recruit" to 2)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                members.sortedBy { order[it.clan_role] ?: 3 }.forEach { m ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Sbo.background.copy(alpha = 0.3f)).padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AvatarSmall(m, 28)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            m.username + if (m.id == me.id) " (tu)" else "", fontSize = 14.sp, lineHeight = 20.sp,
                            fontWeight = if (m.id == me.id) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.width(12.dp))
                        RoleBadge(m.clan_role)
                        Spacer(Modifier.weight(1f))
                        if (m.id != me.id && leader) Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (m.clan_role == "recruit") RoleAction(Lucide.Star, Color(0xFFFBBF24)) { onSetRole(m.id, "co_leader") }
                            if (m.clan_role == "co_leader") RoleAction(Lucide.UserMinus, Sbo.mutedFg) { onSetRole(m.id, "recruit") }
                            RoleAction(Lucide.X, Sbo.destructive) { onKick(m) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoleAction(icon: ImageVector, tint: Color, onClick: () -> Unit) {
    Box(Modifier.clip(RoundedCornerShape(4.dp)).clickable(onClick = onClick).padding(4.dp)) { Icon(icon, null, tint = tint, modifier = Modifier.size(14.dp)) }
}

@Composable
private fun RoleBadge(role: String) {
    val (label, bg, fg, border) = when (role) {
        "leader" -> listOf("Capo", Color(0xFFF59E0B).copy(alpha = 0.25f), Color(0xFFFCD34D), Color(0xFFFBBF24).copy(alpha = 0.4f))
        "co_leader" -> listOf("Vice", Sbo.primary.copy(alpha = 0.25f), Sbo.primary, Sbo.primary.copy(alpha = 0.4f))
        else -> listOf("Recluta", Sbo.muted.copy(alpha = 0.3f), Sbo.mutedFg, Color.White.copy(alpha = 0.10f))
    }
    Row(
        Modifier.clip(CircleShape).background(bg as Color).border(1.dp, border as Color, CircleShape).padding(horizontal = 7.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (role == "leader") { Icon(Lucide.Crown, null, tint = fg as Color, modifier = Modifier.size(12.dp)); Spacer(Modifier.width(4.dp)) }
        if (role == "co_leader") { Icon(Lucide.Star, null, tint = fg as Color, modifier = Modifier.size(12.dp)); Spacer(Modifier.width(4.dp)) }
        Text(label as String, color = fg as Color, fontSize = 9.sp, lineHeight = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ---------------------------------------------------------------- dialogs

@Composable
internal fun SboInput(
    value: String, onChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier,
    style: TextStyle = TextStyle(fontFamily = Inter, fontSize = 14.sp, color = Sbo.foreground),
    caps: Boolean = false, onDone: () -> Unit = {}, height: Dp = 36.dp,
) {
    BasicTextField(
        value, onChange, singleLine = true, textStyle = style, cursorBrush = SolidColor(Sbo.primary),
        keyboardOptions = KeyboardOptions(capitalization = if (caps) KeyboardCapitalization.Characters else KeyboardCapitalization.Sentences, imeAction = ImeAction.Send),
        keyboardActions = KeyboardActions(onSend = { onDone() }),
        modifier = modifier.height(height).clip(RoundedCornerShape(18.dp)).border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(18.dp)).padding(horizontal = 12.dp),
        decorationBox = { inner ->
            Box(Modifier.fillMaxHeight(), contentAlignment = if (style.textAlign == TextAlign.Center) Alignment.Center else Alignment.CenterStart) {
                if (value.isEmpty()) Text(placeholder, style = style.copy(color = Sbo.mutedFg))
                inner()
            }
        },
    )
}

@Composable
fun AddFriendDialog(onClose: () -> Unit, onAdd: (String) -> Unit) {
    var code by remember { mutableStateOf("") }
    SboDialog(onClose) {
        Text("Aggiungi un amico", fontSize = 18.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.45).sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(6.dp))
        Text("Inserisci il codice a 6 caratteri del tuo amico.", color = Sbo.mutedFg, fontSize = 14.sp, lineHeight = 20.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
        SboInput(
            code, { code = it.uppercase().take(6) }, "XXXXXX", Modifier.fillMaxWidth(),
            style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 20.sp, color = Sbo.foreground, letterSpacing = 2.sp, textAlign = TextAlign.Center),
            caps = true, onDone = { if (code.length >= 4) onAdd(code) },
        )
        Spacer(Modifier.height(16.dp))
        SmallButton("Aggiungi", variant = "default", modifier = Modifier.fillMaxWidth(), enabled = code.length >= 4, height = 36.dp, fontSize = 14) { onAdd(code) }
    }
}

private val TIME = DateTimeFormatter.ofPattern("HH:mm")

private fun localDay(iso: String) = Streaks.instant(iso).atZone(ZoneId.systemDefault()).toLocalDate()

private fun dayLabel(d: LocalDate): String {
    val today = LocalDate.now()
    return when (d) {
        today -> "Oggi"
        today.minusDays(1) -> "Ieri"
        else -> d.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ITALIAN))
    }
}

/** Full-height chat dialog (web: h-[80vh]); messages refresh every few seconds while it is open. */
@Composable
private fun ChatShell(onClose: () -> Unit, header: @Composable () -> Unit, input: @Composable () -> Unit, body: @Composable BoxScope.() -> Unit) {
    androidx.activity.compose.BackHandler(onBack = onClose)
    val anim = remember { Animatable(0f) }
    LaunchedEffect(Unit) { anim.animateTo(1f, tween(200)) }
    val h = LocalConfiguration.current.screenHeightDp.dp * 0.8f
    Box(
        Modifier.fillMaxSize().graphicsLayer { alpha = anim.value }.background(Color.Black.copy(alpha = 0.8f))
            .clickable(remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null, onClick = onClose)
            .imePadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.fillMaxWidth().height(h).graphicsLayer { val k = 0.95f + 0.05f * anim.value; scaleX = k; scaleY = k }
                .background(Sbo.card).border(1.dp, Sbo.border)
                .clickable(remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) {}
                .padding(24.dp),
        ) {
            Box(Modifier.fillMaxWidth()) {
                header()
                Icon(Lucide.X, "Chiudi", tint = Sbo.foreground.copy(alpha = 0.7f), modifier = Modifier.align(Alignment.TopEnd).offset(x = 8.dp, y = (-8).dp).size(16.dp).clickable(onClick = onClose))
            }
            Spacer(Modifier.height(16.dp))
            Box(Modifier.weight(1f).fillMaxWidth()) { body() }
            Spacer(Modifier.height(16.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(Sbo.border))
            Spacer(Modifier.height(8.dp))
            input()
        }
    }
}

@Composable
private fun Composer(placeholder: String, onSend: suspend (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val send = { val t = text.trim(); if (t.isNotEmpty()) { text = ""; scope.launch { onSend(t) } } }
    Row(verticalAlignment = Alignment.CenterVertically) {
        SboInput(text, { text = it.take(500) }, placeholder, Modifier.weight(1f), onDone = { send() })
        Spacer(Modifier.width(8.dp))
        SmallButton(icon = Lucide.Send, variant = "default", enabled = text.isNotBlank(), height = 36.dp) { send() }
    }
}

@Composable
private fun Bubble(mine: Boolean, content: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
        Box(
            Modifier.widthIn(max = ((LocalConfiguration.current.screenWidthDp - 48) * 0.75f).dp)
                .clip(RoundedCornerShape(28.dp)).background(if (mine) Sbo.secondary else Sbo.primary).padding(horizontal = 12.dp, vertical = 8.dp),
        ) { content() }
    }
}

@Composable
fun DirectChatDialog(me: Profile, other: Profile, onClose: () -> Unit) {
    var msgs by remember { mutableStateOf<List<DirectMessage>?>(null) }
    LaunchedEffect(other.id) {
        while (true) {
            runCatching { Supa.dms(me.id, other.id) }.onSuccess { list ->
                msgs = list
                if (list.any { it.sender_id == other.id && it.read_at == null }) runCatching { Supa.markDmsRead(other.id) }
            }
            delay(3000)
        }
    }
    val list = msgs ?: emptyList()
    val state = rememberLazyListState()
    LaunchedEffect(list.size) { if (list.isNotEmpty()) state.animateScrollToItem(list.size - 1) }
    ChatShell(
        onClose,
        header = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AvatarSmall(other, 32); Spacer(Modifier.width(8.dp))
                Text(other.username, fontSize = 18.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.45).sp)
            }
        },
        input = { Composer("Scrivi un messaggio…") { t -> runCatching { Supa.sendDm(me.id, other.id, t) }; msgs = runCatching { Supa.dms(me.id, other.id) }.getOrNull() ?: msgs } },
    ) {
        if (msgs != null && list.isEmpty()) Text("Inizia la conversazione, ${me.username}!", color = Sbo.mutedFg, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp))
        LazyColumn(state = state, verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
            itemsIndexed(list, key = { _, m -> m.id }) { i, m ->
                val mine = m.sender_id == me.id
                val d = localDay(m.created_at)
                Column {
                    if (i == 0 || localDay(list[i - 1].created_at) != d) {
                        Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                            Text(
                                dayLabel(d).uppercase(), color = Sbo.mutedFg, fontSize = 10.sp, lineHeight = 14.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.25.sp,
                                modifier = Modifier.clip(CircleShape).background(Sbo.muted).padding(horizontal = 12.dp, vertical = 4.dp),
                            )
                        }
                    }
                    Bubble(mine) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(m.content, color = if (mine) Sbo.foreground else Sbo.primaryFg, fontSize = 14.sp, lineHeight = 20.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val c = (if (mine) Sbo.foreground else Sbo.primaryFg).copy(alpha = 0.7f)
                                Text(Streaks.instant(m.created_at).atZone(ZoneId.systemDefault()).format(TIME), color = c, fontSize = 10.sp, lineHeight = 14.sp)
                                if (mine) { Spacer(Modifier.width(4.dp)); Icon(if (m.read_at != null) Lucide.CheckCheck else Lucide.Check, null, tint = if (m.read_at != null) Color(0xFF7DD3FC) else c, modifier = Modifier.size(12.dp)) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ClanChatDialog(me: Profile, clan: com.sbo.app.data.Clan, members: List<Profile>, onClose: () -> Unit) {
    var msgs by remember { mutableStateOf<List<ClanMessage>?>(null) }
    LaunchedEffect(clan.id) { while (true) { runCatching { Supa.clanMessages(clan.id) }.onSuccess { msgs = it }; delay(3000) } }
    val list = msgs ?: emptyList()
    val byId = remember(members) { members.associateBy { it.id } }
    val state = rememberLazyListState()
    LaunchedEffect(list.size) { if (list.isNotEmpty()) state.animateScrollToItem(list.size - 1) }
    ChatShell(
        onClose,
        header = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Lucide.Shield, null, tint = Sbo.foreground, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(8.dp))
                    Text(clan.name, fontSize = 18.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.45).sp)
                }
                Spacer(Modifier.height(6.dp))
                Text("Gli annunci automatici arrivano quando qualcuno vince una medaglia epica o leggendaria.", color = Sbo.mutedFg, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.padding(end = 16.dp))
            }
        },
        input = { Composer("Messaggio al clan…") { t -> runCatching { Supa.sendClanMessage(me.id, clan.id, t) }; msgs = runCatching { Supa.clanMessages(clan.id) }.getOrNull() ?: msgs } },
    ) {
        if (msgs != null && list.isEmpty()) Text("Nessun messaggio ancora. Rompi il ghiaccio!", color = Sbo.mutedFg, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp))
        LazyColumn(state = state, verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
            itemsIndexed(list, key = { _, m -> m.id }) { _, m ->
                if (m.is_system) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            m.content, fontSize = 12.sp, lineHeight = 16.sp, textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(0.85f).clip(RoundedCornerShape(24.dp))
                                .background(Brush.horizontalGradient(listOf(Color(0xFFF59E0B).copy(alpha = 0.2f), Color(0xFFD946EF).copy(alpha = 0.2f))))
                                .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f), RoundedCornerShape(24.dp)).padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                    }
                } else {
                    val mine = m.sender_id == me.id
                    Bubble(mine) {
                        Column {
                            val sender = m.sender_id?.let { byId[it] }
                            if (!mine && sender != null) Text(sender.username, color = Sbo.primaryFg.copy(alpha = 0.8f), fontSize = 10.sp, lineHeight = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text(m.content, color = if (mine) Sbo.foreground else Sbo.primaryFg, fontSize = 14.sp, lineHeight = 20.sp)
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- toast (sonner, top-center, dark)

class ToastState { var text by mutableStateOf<String?>(null); var desc by mutableStateOf<String?>(null); internal var serial by mutableStateOf(0) }

@Composable
fun ToastHost(state: ToastState) {
    val t = state.text ?: return
    val a = remember(state.serial) { Animatable(0f) }
    LaunchedEffect(state.serial) { a.animateTo(1f, tween(220)); delay(2600); a.animateTo(0f, tween(220)); state.text = null }
    Box(Modifier.fillMaxSize().statusBarsPadding().padding(top = 16.dp, start = 16.dp, end = 16.dp), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.graphicsLayer { alpha = a.value; translationY = (1f - a.value) * -16.dp.toPx() }
                .fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color.Black).border(1.dp, Color(0xFF333333), RoundedCornerShape(8.dp)).padding(16.dp),
        ) {
            Text(t, fontSize = 13.sp, lineHeight = 18.sp, color = Sbo.foreground, fontWeight = FontWeight.Medium)
            state.desc?.let { Spacer(Modifier.height(4.dp)); Text(it, fontSize = 13.sp, lineHeight = 20.sp, color = Sbo.foreground.copy(alpha = 0.9f)) }
            Spacer(Modifier.height(3.dp))
        }
    }
}

fun ToastState.show(msg: String, description: String? = null) { text = msg; desc = description; serial++ }
