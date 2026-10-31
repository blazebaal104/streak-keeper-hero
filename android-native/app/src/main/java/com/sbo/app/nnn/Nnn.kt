package com.sbo.app.nnn

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sbo.app.BuildConfig
import com.sbo.app.data.SboEvent
import com.sbo.app.logic.Streaks
import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random

/**
 * No Nut November: from 1 to 30 November the counter is off limits. The add button is buried under
 * a random pile of real-world protections; whoever digs through all of them and presses has "fallen"
 * (the server tells the whole group). Survivors get a legendary medal and title on 1 December.
 *
 * For now only the dev app has it; there a DEV panel can force every moment of the month for review.
 */
object Nnn {
    val enabledInBuild: Boolean get() = BuildConfig.DEV

    /** Dev review override: null = follow the calendar, else the forced date. */
    var forcedToday by mutableStateOf<LocalDate?>(null)
    /** Dev review: pretend not fallen even with November events (to replay the breach). */
    var ignoreFall by mutableStateOf(false)
    /** Dev review: re-roll the protection sequence. */
    var seedSalt by mutableStateOf(0)

    fun today(): LocalDate = forcedToday ?: LocalDate.now()

    fun active(today: LocalDate = today()): Boolean = enabledInBuild && today.monthValue == 11

    /** Day of November the user fell on (first event in November), or null if still standing. */
    fun fallDay(events: List<SboEvent>, year: Int): Int? {
        if (ignoreFall) return null
        val zone = ZoneId.systemDefault()
        return events.asSequence()
            .map { Streaks.instant(it.ts).atZone(zone).toLocalDate() }
            .filter { it.year == year && it.monthValue == 11 }
            .minOrNull()?.dayOfMonth
    }

    /**
     * Protection sequence for this user, outermost first: different for everyone, stable through the month.
     * The police tape always comes first: it is wound round the whole pile, so it's the first thing to tear off.
     */
    fun sequence(userId: String, year: Int): List<Kind> {
        val r = Random((userId + year + seedSalt).hashCode())
        val pool = Kind.entries.filter { it != Kind.TAPE }.shuffled(r)
        return listOf(Kind.TAPE) + pool.take(LAYERS - 1)
    }

    /** Number of layers on the button (tape included). Enough to feel endless, few enough to finish. */
    const val LAYERS = 14

    private const val SEEN = "nnn-seen"
    fun seenKickoff(ctx: Context, year: Int) = ctx.getSharedPreferences(SEEN, Context.MODE_PRIVATE).getBoolean("kickoff:$year", false)
    fun markKickoff(ctx: Context, year: Int) = ctx.getSharedPreferences(SEEN, Context.MODE_PRIVATE).edit().putBoolean("kickoff:$year", true).apply()
    fun seenParty(ctx: Context, year: Int) = ctx.getSharedPreferences(SEEN, Context.MODE_PRIVATE).getBoolean("party:$year", false)
    fun markParty(ctx: Context, year: Int) = ctx.getSharedPreferences(SEEN, Context.MODE_PRIVATE).edit().putBoolean("party:$year", true).apply()
}

/**
 * Every protection is a real everyday object that really is used to lock or seal something, pre-rendered in
 * Blender at every tap state (res/drawable-nodpi/nnn_<id>_<state>.webp). [steps] = the real actions needed to get
 * it off, one tap each; the last one always lifts the object away.
 */
enum class Kind(val id: String, val label: String, val steps: Int) {
    TAPE("tape", "Nastro della polizia", 3),          // cut, unwind half, pull it all off
    SAFE("safe", "Cassaforte a combinazione", 6),    // 4 numbers, handle (door opens), safe away
    BEAM("beam", "Assi inchiodate", 3),              // 2 nails yanked, planks off
    STEEL_PLATE("steel_plate", "Piastra imbullonata", 5), // 4 bolts, plate off
    CHAIN_PADLOCK("chain_padlock", "Catena con lucchetto", 3), // key, shackle open, chain off
    HANDCUFFS("handcuffs", "Manette", 3),            // cuff 1, cuff 2, off
    U_LOCK("u_lock", "Lucchetto a U", 3),            // key, shackle out, off
    ZIP_TIES("zip_ties", "Fascette", 3),             // snip, snip, snip
    BARBED_WIRE("barbed_wire", "Filo spinato", 3),   // cut, cut, pull away
    GLASS_CASE("glass_case", "Teca d'emergenza", 3), // crack, smash, away
    ROPE("rope", "Corda annodata", 3),               // loosen knot, untie, unwind
    GRATE("grate", "Grata con chiavistello", 3),     // slide bolt, swing door, away
    BOX("box", "Scatola sigillata", 4),              // slit tape, flap, flap, lift
    SANDBAGS("sandbags", "Sacchi di sabbia", 3),     // one bag at a time
    BRICKS("bricks", "Muro di mattoni", 4),          // three clusters knocked out, wall down
    DUCT_TAPE("duct_tape", "Nastro americano", 3);   // three strips ripped
}
