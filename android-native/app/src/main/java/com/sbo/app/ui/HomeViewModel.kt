package com.sbo.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sbo.app.data.Profile
import com.sbo.app.data.SboEvent
import com.sbo.app.data.Supa
import com.sbo.app.logic.Phrases
import com.sbo.app.logic.Streaks
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeState(
    val loading: Boolean = true,
    val error: String? = null,
    val profile: Profile? = null,
    val events: List<SboEvent> = emptyList(),
    val today: Int = 0,
    val month: Int = 0,
    val fire: Int = 0,
    val ice: Int = 0,
    val limit: Int = 3,
    val tip: String = "",
)

/** Optional preview override (used by the screenshot pipeline): "fire:12", "ice:9", "neutral". */
class HomeViewModel(private val preview: String?) : ViewModel() {
    private val _s = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _s
    private var uid: String? = null

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        try {
            val id = uid ?: Supa.ensureUser() ?: run { _s.update { it.copy(loading = false, error = "login") }; return@launch }
            uid = id
            val p = Supa.profile(id)
            val ev = Supa.events(id)
            _s.value = compute(p, ev)
        } catch (e: Exception) {
            _s.update { it.copy(loading = false, error = e.message ?: "Errore di rete") }
        }
    }

    private fun compute(p: Profile, ev: List<SboEvent>): HomeState {
        var fire = Streaks.fire(ev)
        var ice = Streaks.ice(ev)
        preview?.let {
            val (mode, n) = (it.split(":") + "0").let { a -> a[0] to (a[1].toIntOrNull() ?: 0) }
            when (mode) { "fire" -> { fire = n.coerceAtLeast(1); ice = 0 }; "ice" -> { ice = n.coerceAtLeast(1); fire = 0 }; "neutral" -> { fire = 0; ice = 0 } }
        }
        val today = Streaks.countToday(ev)
        val limit = if (p.use_adaptive_limit) Streaks.adaptiveLimit(ev, p.daily_limit) else p.daily_limit
        val tip = when {
            today >= limit -> Phrases.slowdown(p.username)
            fire >= 3 -> Phrases.fireEncourage(p.username, fire)
            ice >= 3 -> Phrases.iceEncourage(p.username, ice)
            else -> Phrases.encourage(p.username)
        }
        return HomeState(false, null, p, ev, today, Streaks.countThisMonth(ev), fire, ice, limit, tip)
    }

    fun add() = viewModelScope.launch {
        val id = uid ?: return@launch
        runCatching { Supa.addEvent(id) }
        refresh()
    }

    fun undo() = viewModelScope.launch {
        val id = uid ?: return@launch
        runCatching { Supa.undoLast(id) }
        refresh()
    }
}
