package com.sbo.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sbo.app.update.AppRelease
import com.sbo.app.update.Updater
import kotlinx.coroutines.launch
import java.io.File

/**
 * "Nuovo aggiornamento disponibile": one button downloads the new version inside the app,
 * then Android's own installer asks "Aggiornare SBO?". [preview] only fakes the states for screenshots.
 */
@Composable
fun UpdateDialog(r: AppRelease, onLater: () -> Unit, preview: Float? = null) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var progress by remember { mutableStateOf(preview) }
    var apk by remember { mutableStateOf<File?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var needPermission by remember { mutableStateOf(false) }
    val shown by animateFloatAsState(progress ?: 0f, tween(250), label = "dl")

    fun installOrAsk(f: File) {
        if (Updater.canInstall(ctx)) { needPermission = false; Updater.install(ctx, f) }
        else { needPermission = true; Updater.openInstallPermission(ctx) }
    }

    SboDialog(onLater) {
        Text(if (r.title.isNullOrBlank()) "Nuovo aggiornamento disponibile" else r.title, fontFamily = SpaceGrotesk, fontSize = 22.sp, lineHeight = 26.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
        Spacer(Modifier.height(8.dp))
        Text(r.notes ?: "C'è una nuova versione di SBO.", color = Sbo.mutedFg, fontSize = 14.sp, lineHeight = 20.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(6.dp))
        Text("Versione ${r.version_name}", color = Sbo.mutedFg.copy(alpha = 0.7f), fontSize = 12.sp, lineHeight = 16.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(24.dp))
        val p = progress
        when {
            needPermission -> {
                Text("Android ti chiede di permettere a SBO di installare aggiornamenti: attiva l'interruttore, torna qui e premi di nuovo.",
                    fontSize = 13.sp, lineHeight = 18.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                SboButton("Aggiorna ora") { apk?.let { installOrAsk(it) } }
            }
            apk != null || (p != null && p >= 1f) -> SboButton("Aggiorna ora") { apk?.let { installOrAsk(it) } }
            p != null -> {
                Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(Sbo.muted)) {
                    Box(Modifier.fillMaxWidth(shown).fillMaxHeight().clip(CircleShape).background(Sbo.primary))
                }
                Spacer(Modifier.height(10.dp))
                Text("Download in corso… ${(shown * 100).toInt()}%", color = Sbo.mutedFg, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
            else -> SboButton("Scarica aggiornamento") {
                error = null
                progress = 0f
                scope.launch {
                    runCatching { Updater.download(ctx, r) { v -> progress = v } }
                        .onSuccess { f -> apk = f; installOrAsk(f) }
                        .onFailure { progress = null; error = "Download non riuscito: controlla la connessione e riprova." }
                }
            }
        }
        error?.let { Spacer(Modifier.height(10.dp)); Text(it, color = Sbo.destructive, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
        Spacer(Modifier.height(8.dp))
        SboButton("Più tardi", variant = "ghost", fontSize = 13, onClick = onLater)
    }
}
