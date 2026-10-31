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
     * Protection sequence for this user, in removal order: different for everyone, stable through the month.
     * The police tape is always the last one, right on the button (its loose end flaps out from under the pile).
     */
    fun sequence(userId: String, year: Int): List<Kind> {
        val r = Random((userId + year + seedSalt).hashCode())
        val pool = Kind.entries.filter { it != Kind.CRIME_TAPE }.shuffled(r)
        return pool.take(LAYERS - 1) + Kind.CRIME_TAPE
    }

    /** Number of layers on the button (outermost included). Enough to feel endless, few enough to finish. */
    const val LAYERS = 14

    private const val SEEN = "nnn-seen"
    fun seenKickoff(ctx: Context, year: Int) = ctx.getSharedPreferences(SEEN, Context.MODE_PRIVATE).getBoolean("kickoff:$year", false)
    fun markKickoff(ctx: Context, year: Int) = ctx.getSharedPreferences(SEEN, Context.MODE_PRIVATE).edit().putBoolean("kickoff:$year", true).apply()
    fun seenParty(ctx: Context, year: Int) = ctx.getSharedPreferences(SEEN, Context.MODE_PRIVATE).getBoolean("party:$year", false)
    fun markParty(ctx: Context, year: Int) = ctx.getSharedPreferences(SEEN, Context.MODE_PRIVATE).edit().putBoolean("party:$year", true).apply()
}

/**
 * Every protection is a different everyday object that really is used to lock or seal something.
 * [steps] = the real actions needed to get it off, one tap each; the last one always lifts the object away.
 */
enum class Kind(val label: String, val steps: Int) {
    BEAM("Trave inchiodata", 3),            // 2 nails out, beam off
    CHAIN_PADLOCK("Catena con lucchetto", 3), // key in and turn, shackle open, chain unwound
    SAFE("Cassaforte a combinazione", 6),   // dial right, left, right (3 numbers), handle, door open, safe away
    DUCT_TAPE("Nastro americano", 3),      // three strips peeled
    STEEL_PLATE("Piastra d'acciaio imbullonata", 5), // 4 bolts, plate off
    U_LOCK("Lucchetto a U", 3),             // key, shackle pulled, lock off
    ZIP_TIES("Fascette", 3),                // each tie snipped
    BARBED_WIRE("Filo spinato", 3),         // 2 cuts, coil pulled away
    GLASS_CASE("Teca di vetro", 3),         // crack, shatter, frame away
    KEYPAD("Serratura a codice", 6),        // 4 digits, enter, door open
    HANDCUFFS("Manette", 3),                // 2 cuffs unlocked, cuffs off
    ROPE("Corda annodata", 3),              // loosen knot, pull through, unwrap
    GRATE("Grata con chiavistello", 3),     // slide bolt, lift latch, swing open
    COMBO_PADLOCK("Lucchetto a rotelle", 6), // 4 wheels, pull open, off
    RATCHET_STRAP("Cinghia a cricchetto", 3), // release lever, open ratchet, pull strap
    STRETCH_WRAP("Pellicola", 3),           // tear, unwind twice
    BOX("Scatola sigillata", 4),            // cut tape, flap, flap, lift
    SANDBAGS("Sacchi di sabbia", 3),        // one bag at a time
    BRICKS("Muro di mattoni", 4),           // three bricks knocked out, wall collapses
    BELT("Cintura di cuoio", 3),            // unbuckle, pull through, off
    CRIME_TAPE("Nastro della polizia", 3);  // three strips torn
}
