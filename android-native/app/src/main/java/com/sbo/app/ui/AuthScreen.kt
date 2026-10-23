package com.sbo.app.ui

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sbo.app.R
import com.sbo.app.data.Supa
import kotlinx.coroutines.launch

/**
 * Sign in / sign up / forgot password (web src/routes/auth.tsx), for the official app.
 * Same validation and the same Italian messages as the website.
 */
@Composable
fun AuthScreen(onSignedIn: () -> Unit) {
    var mode by remember { mutableStateOf("signup") } // signup | login | forgot
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var codeSent by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    val toast = remember { ToastState() }
    val scope = rememberCoroutineScope()
    val emailOk = { e: String -> Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(e.trim()) }

    fun submit() = scope.launch {
        loading = true
        try {
            when {
                mode == "forgot" && !codeSent -> {
                    if (!emailOk(email)) { toast.show("Email non valida"); return@launch }
                    runCatching { Supa.sendRecovery(email.trim()) }.onFailure { e ->
                        val m = e.message ?: ""
                        toast.show(if (Regex("rate|seconds", RegexOption.IGNORE_CASE).containsMatchIn(m)) "Hai chiesto troppi codici: aspetta un minuto e riprova."
                            else if (m.contains("sending", true)) "Non riusciamo a inviare l'email in questo momento. Riprova più tardi." else m)
                        return@launch
                    }
                    codeSent = true
                    toast.show("Se l'email è registrata, ti abbiamo mandato un codice. Controlla anche lo spam.")
                }
                mode == "forgot" -> {
                    if (password.length < 8) { toast.show("La nuova password deve avere almeno 8 caratteri"); return@launch }
                    val token = code.filter { !it.isWhitespace() }
                    if (!Regex("^\\d{6,10}$").matches(token)) { toast.show("Inserisci il codice che hai ricevuto via email"); return@launch }
                    if (runCatching { Supa.verifyRecovery(email.trim(), token) }.isFailure) { toast.show("Codice sbagliato o scaduto. Chiedine uno nuovo."); return@launch }
                    runCatching { Supa.updatePassword(password) }.onFailure { e ->
                        toast.show(if (Regex("different|same", RegexOption.IGNORE_CASE).containsMatchIn(e.message ?: "")) "Scegli una password diversa da quella vecchia." else (e.message ?: "Errore")); return@launch
                    }
                    toast.show("Password cambiata! Bentornato 💧"); onSignedIn()
                }
                mode == "signup" -> {
                    val u = username.trim()
                    when {
                        u.length < 3 -> { toast.show("Almeno 3 caratteri"); return@launch }
                        u.length > 20 -> { toast.show("Massimo 20 caratteri"); return@launch }
                        !Regex("^[a-zA-Z0-9_]+$").matches(u) -> { toast.show("Solo lettere, numeri e _"); return@launch }
                        !emailOk(email) -> { toast.show("Email non valida"); return@launch }
                        password.length < 8 -> { toast.show("Almeno 8 caratteri"); return@launch }
                    }
                    if (runCatching { Supa.usernameTaken(u.lowercase()) }.getOrDefault(false)) { toast.show("Username già preso, scegline un altro."); return@launch }
                    runCatching { Supa.signUp(email.trim(), password, u.lowercase()) }.onFailure { toast.show(it.message ?: "Errore"); return@launch }
                    toast.show("Account creato! Benvenuto in SBO 💧")
                    if (Supa.signedIn()) onSignedIn() else { mode = "login"; toast.show("Controlla la tua email per confermare l'account, poi accedi.") }
                }
                else -> {
                    if (!emailOk(email)) { toast.show("Email non valida"); return@launch }
                    if (password.isEmpty()) return@launch
                    if (runCatching { Supa.signIn(email.trim(), password) }.isFailure) { toast.show("Email o password errata"); return@launch }
                    onSignedIn()
                }
            }
        } finally { loading = false }
    }

    Box(
        Modifier.fillMaxSize().background(Sbo.background).drawBehind {
            drawRect(Brush.radialGradient(0f to Sbo.surfaceTop, 0.7f to Sbo.background, center = Offset(size.width / 2f, 0f), radius = size.height * 0.75f))
        },
    ) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 16.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
        ) {
            Column(
                Modifier.widthIn(max = 448.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Sbo.card.copy(alpha = 0.9f))
                    .border(1.dp, Sbo.border.copy(alpha = 0.5f), RoundedCornerShape(24.dp)).padding(32.dp),
            ) {
                Column(Modifier.fillMaxWidth().padding(bottom = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.drawBehind {
                        drawCircle(Brush.radialGradient(0f to Sbo.dropGlow.copy(alpha = 0.45f), 1f to Color.Transparent, center = center, radius = size.width * 0.8f), size.width * 0.8f)
                    }) { Image(painterResource(R.drawable.sbo_logo), "SBO", modifier = Modifier.size(72.dp)) }
                    Spacer(Modifier.height(16.dp))
                    Text("SBO", fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.75).sp)
                    Spacer(Modifier.height(4.dp))
                    Text(when (mode) { "signup" -> "Crea il tuo account"; "login" -> "Bentornato"; else -> "Recupera la password" }, color = Sbo.mutedFg, fontSize = 14.sp, textAlign = TextAlign.Center)
                }
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (mode == "forgot") {
                        Field("Email dell'account", email, { email = it }, "tu@esempio.com", KeyboardType.Email, enabled = !codeSent)
                        if (codeSent) Field("Codice ricevuto via email", code, { code = it.take(12) }, "123456", KeyboardType.Number, big = true)
                        if (codeSent) Field("Nuova password", password, { password = it }, "", KeyboardType.Password, secret = true)
                        BigButton(if (!codeSent) "Mandami il codice" else "Salva la nuova password", loading) { submit() }
                        if (codeSent) Text("Non è arrivato? Richiedi un nuovo codice", color = Sbo.mutedFg, fontSize = 12.sp, textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().clickable { codeSent = false; code = "" })
                    } else {
                        if (mode == "signup") Column {
                            Field("Username", username, { username = it.take(20) }, "il_tuo_nome", KeyboardType.Text)
                            Spacer(Modifier.height(4.dp))
                            Text("Puoi modificarlo dopo.", color = Sbo.mutedFg, fontSize = 11.sp)
                        }
                        Field("Email", email, { email = it }, "tu@esempio.com", KeyboardType.Email)
                        Column {
                            Field("Password", password, { password = it }, "", KeyboardType.Password, secret = true)
                            if (mode == "login") Text("Password dimenticata?", color = Sbo.primary, fontSize = 12.sp,
                                modifier = Modifier.padding(top = 6.dp).clickable { mode = "forgot"; password = ""; codeSent = false; code = "" })
                        }
                        BigButton(if (mode == "signup") "Crea account" else "Accedi", loading) { submit() }
                    }
                }
                Spacer(Modifier.height(20.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    val (q, link, to) = when (mode) {
                        "forgot" -> Triple("Te la sei ricordata? ", "Torna ad accedere", "login")
                        "signup" -> Triple("Hai già un account? ", "Accedi", "login")
                        else -> Triple("Nuovo su SBO? ", "Crea un account", "signup")
                    }
                    Text(q, color = Sbo.mutedFg, fontSize = 14.sp)
                    Text(link, color = Sbo.primary, fontSize = 14.sp, modifier = Modifier.clickable { mode = to; password = "" })
                }
            }
        }
        ToastHost(toast)
    }
}

@Composable
private fun Field(label: String, value: String, onChange: (String) -> Unit, placeholder: String, type: KeyboardType, secret: Boolean = false, enabled: Boolean = true, big: Boolean = false) {
    Column {
        Text(label, fontSize = 14.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))
        val style = if (big) TextStyle(fontFamily = Inter, fontSize = 18.sp, color = Sbo.foreground, textAlign = TextAlign.Center, letterSpacing = 5.4.sp)
        else TextStyle(fontFamily = Inter, fontSize = 16.sp, color = Sbo.foreground.copy(alpha = if (enabled) 1f else 0.5f))
        BasicTextField(
            value, onChange, enabled = enabled, singleLine = true, textStyle = style, cursorBrush = SolidColor(Sbo.primary),
            visualTransformation = if (secret) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = type),
            modifier = Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(18.dp)).border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(18.dp)).padding(horizontal = 12.dp),
            decorationBox = { inner ->
                Box(Modifier.fillMaxSize(), contentAlignment = if (big) Alignment.Center else Alignment.CenterStart) {
                    if (value.isEmpty() && placeholder.isNotEmpty()) Text(placeholder, style = style.copy(color = Sbo.mutedFg))
                    inner()
                }
            },
        )
    }
}

@Composable
private fun BigButton(text: String, loading: Boolean, onClick: () -> Unit) {
    val press = rememberPressScale()
    Row(
        Modifier.fillMaxWidth().height(44.dp).graphicsScale(press).clip(RoundedCornerShape(18.dp)).background(Sbo.primary.copy(alpha = if (loading) 0.5f else 1f))
            .then(if (!loading) Modifier.pressable(press, text, onClick) else Modifier),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        if (loading) { CircularProgressIndicator(color = Sbo.primaryFg, strokeWidth = 2.dp, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(8.dp)) }
        Text(text, color = Sbo.primaryFg, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}
