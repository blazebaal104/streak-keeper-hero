package com.sbo.app.ui

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sbo.app.data.Profile

@Composable
fun HomeScreen(vm: HomeViewModel) {
    val s by vm.state.collectAsState()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Box(
        Modifier.fillMaxSize().background(Sbo.background).drawBehind {
            drawRect(Brush.radialGradient(0f to Sbo.surfaceTop, 0.7f to Sbo.background, center = Offset(size.width / 2f, 0f), radius = size.height * 0.75f))
        },
    ) {
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
                if (tab == 0) HomeTab(s, vm) else ComingSoon()
                Spacer(Modifier.height(96.dp))
            }
        }
    }
}

@Composable
private fun Header(p: Profile, fire: Int, ice: Int) {
    Row(Modifier.fillMaxWidth().padding(top = 32.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Avatar(p, 44)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(p.username, color = Sbo.foreground, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(p.friend_code, color = Sbo.mutedFg, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }
        Chip(Icons.Rounded.LocalFireDepartment, Sbo.orange, fire)
        Spacer(Modifier.width(8.dp))
        Chip(Icons.Rounded.AcUnit, Sbo.sky, ice)
    }
}

@Composable
private fun Chip(icon: ImageVector, tint: Color, n: Int) {
    Row(
        Modifier.clip(CircleShape).background(Sbo.card.copy(alpha = 0.9f)).border(1.dp, Sbo.border, CircleShape).padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text("$n", color = Sbo.foreground, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun Avatar(p: Profile, sizeDp: Int) {
    val bmp = remember(p.avatar_url) {
        p.avatar_url?.substringAfter("base64,", "")?.takeIf { it.isNotEmpty() }?.let {
            runCatching { val b = Base64.decode(it, Base64.DEFAULT); BitmapFactory.decodeByteArray(b, 0, b.size)?.asImageBitmap() }.getOrNull()
        }
    }
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
    val tabs = listOf("Home" to Icons.Rounded.WaterDrop, "Cal" to Icons.Rounded.CalendarMonth, "Trend" to Icons.Rounded.ShowChart, "Social" to Icons.Rounded.Group)
    Row(
        Modifier.fillMaxWidth().clip(CircleShape).background(Sbo.card.copy(alpha = 0.9f)).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        tabs.forEachIndexed { i, (label, icon) ->
            val on = i == selected
            Row(
                Modifier.weight(1f).clip(CircleShape).background(if (on) Sbo.background else Color.Transparent)
                    .clickable { onSelect(i) }.padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(icon, null, tint = if (on) Sbo.foreground else Sbo.mutedFg, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(label, fontSize = 12.sp, color = if (on) Sbo.foreground else Sbo.mutedFg, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun HomeTab(s: HomeState, vm: HomeViewModel) {
    val p = s.profile ?: return
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Stat("Oggi", s.today, accent = s.today >= s.limit, modifier = Modifier.weight(1f))
        Stat("Questo mese", s.month, modifier = Modifier.weight(1f))
        Stat(if (p.use_adaptive_limit) "Limite adattivo" else "Limite", s.limit, icon = if (p.use_adaptive_limit) Icons.Rounded.TrendingUp else null, modifier = Modifier.weight(1f))
    }
    Spacer(Modifier.height(32.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)) {
        repeat(3) { BadgeSlot() }
    }
    Spacer(Modifier.height(if (s.fire > 0) 8.dp else 24.dp))
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        StreakButton(s.fire, s.ice, onPress = vm::add)
        Text(
            when {
                s.fire > 0 -> "🔥 La tua fiamma cresce: ${s.fire} giorni di fila."
                s.ice > 0 -> "❄️ Sei congelato da ${s.ice} giorni. Premere rompe il ghiaccio."
                else -> "Premi per aggiungere. Tutto è salvato sul tuo account."
            },
            color = Sbo.mutedFg, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 16.dp),
        )
        if (s.events.isNotEmpty()) {
            Row(
                Modifier.padding(top = 8.dp).clip(RoundedCornerShape(10.dp)).clickable { vm.undo() }.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.AutoMirrored.Rounded.Undo, null, tint = Sbo.mutedFg, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text("Annulla ultimo", color = Sbo.mutedFg, fontSize = 14.sp)
            }
        }
    }
    Spacer(Modifier.height(24.dp))
    Card {
        Row {
            Icon(Icons.Rounded.AutoAwesome, null, tint = Sbo.accent, modifier = Modifier.padding(top = 2.dp).size(20.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Suggerimento del giorno", color = Sbo.foreground, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(s.tip, color = Sbo.mutedFg, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Sbo.card.copy(alpha = 0.9f))
            .border(1.dp, Sbo.border, RoundedCornerShape(20.dp)).padding(20.dp),
    ) { content() }
}

@Composable
private fun Stat(label: String, value: Int, modifier: Modifier = Modifier, accent: Boolean = false, icon: ImageVector? = null) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier.clip(shape).background(Sbo.card.copy(alpha = 0.9f))
            .border(if (accent) 2.dp else 1.dp, if (accent) Sbo.destructive.copy(alpha = 0.5f) else Sbo.border, shape)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) { Icon(icon, null, tint = Sbo.mutedFg, modifier = Modifier.size(14.dp)); Spacer(Modifier.width(4.dp)) }
            Text(label, color = Sbo.mutedFg, fontSize = 12.sp, lineHeight = 15.sp)
        }
        Text("$value", color = if (accent) Sbo.destructive else Sbo.foreground, fontSize = 30.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun BadgeSlot() {
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier.size(64.dp).clip(shape).background(Sbo.card.copy(alpha = 0.7f)).drawBehind {
            drawRoundRect(
                Sbo.primary.copy(alpha = 0.5f), style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx()),
            )
        },
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Rounded.Add, null, tint = Sbo.primary.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
        Text("slot", color = Sbo.primary.copy(alpha = 0.7f), fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ComingSoon() {
    Text("Questa scheda arriva nella prossima versione nativa.", color = Sbo.mutedFg, fontSize = 14.sp, modifier = Modifier.fillMaxWidth().padding(top = 48.dp), textAlign = TextAlign.Center)
}
