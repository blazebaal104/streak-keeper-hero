package com.sbo.app

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.sbo.app.notify.Notifier
import com.sbo.app.ui.HomeScreen
import com.sbo.app.ui.HomeViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var vm: HomeViewModel
    private var explicitAsk = false

    private val askNotifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) Notifier.schedule(this)
        if (explicitAsk) Toast.makeText(this, if (ok) "Notifiche attivate 🔔 Arriveranno anche ad app chiusa." else "Notifiche bloccate: abilitale nelle impostazioni del telefono per SBO.", Toast.LENGTH_LONG).show()
        explicitAsk = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        val preview = if (BuildConfig.DEV) intent.getStringExtra("preview") else null
        vm = ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = HomeViewModel(preview) as T
        })[HomeViewModel::class.java]
        vm.deepLink.value = intent.getStringExtra("open")
        vm.onTestPushQueued = { Notifier.pollSoon(this, 8) }
        lifecycleScope.launch { vm.enableNotifications.collect { explicitAsk = true; ask() } }
        // like the web: ask for notifications once, then keep the background check running
        Notifier.ensureChannel(this)
        if (Notifier.allowed(this)) Notifier.schedule(this) else ask()
        setContent { HomeScreen(vm) }
    }

    private fun ask() {
        if (android.os.Build.VERSION.SDK_INT >= 33 && !Notifier.allowed(this)) askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        else { Notifier.schedule(this); if (explicitAsk) { Toast.makeText(this, "Notifiche attivate 🔔 Arriveranno anche ad app chiusa.", Toast.LENGTH_LONG).show(); explicitAsk = false } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra("open")?.let { vm.deepLink.value = it }
    }

    override fun onStop() {
        super.onStop()
        // check right away once we're in the background, then every 15 minutes
        Notifier.pollSoon(this, 60)
    }
}
