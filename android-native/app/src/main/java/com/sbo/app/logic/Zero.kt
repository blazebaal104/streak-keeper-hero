package com.sbo.app.logic

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * Progetto Zero — exact port of src/lib/sbo-zero.ts. The daily plan must come out identical to the
 * website's for the same user and day, so the seeded RNG reproduces JavaScript's number semantics and
 * the shuffles reproduce V8's Array.prototype.sort (TimSort → binary insertion sort for short arrays).
 */
@Serializable
data class ZeroAnswers(
    val age: Int = 17, val pushups: Int = 15, val plank: Int = 45, val pullups: Int = 2, val squats: Int = 30, val weeklyNow: Int = 5,
    val goal: String? = null, val realName: String? = null, val dumbbellKg: Int? = null, val hasBar: Boolean? = null,
    val gymDays: List<Int>? = null, val _redo: Boolean? = null,
)

enum class League(val label: String) { low("Lega Bassa"), mid("Lega Media"), high("Lega Alta") }

data class Exercise(val id: String, val name: String, val sets: Int, val amount: Double, val unit: String, val tip: String)
data class DayPlan(
    val key: String, val rest: Boolean, val focus: String, val title: String, val exercises: List<Exercise>,
    val restTip: String? = null, val intensity: String? = null, val gym: Boolean = false, val autoRest: String? = null,
)

/** JS getDay(): 0 = Sunday. */
fun LocalDate.jsDay(): Int = dayOfWeek.value % 7

object Zero {
    fun computeLeague(a: ZeroAnswers): League {
        fun band(v: Int, lo: Int, hi: Int) = if (v >= hi) 2 else if (v >= lo) 1 else 0
        val canPull = a.hasBar == true || (a.gymDays?.size ?: 0) > 0
        val parts = mutableListOf(band(a.pushups, 15, 35), band(a.plank, 45, 120), band(a.squats, 30, 60))
        if (canPull) parts += band(a.pullups, 3, 10)
        val score = parts.sum().toDouble() / parts.size * 4
        return if (score <= 2.5) League.low else if (score <= 5.5) League.mid else League.high
    }

    fun startLimitFrom(weeklyNow: Int) = max(3, min(5, jsRound(weeklyNow.toDouble()).toInt()))

    fun key(d: LocalDate) = d.toString()

    /** Monday of the week containing d. */
    fun weekStart(d: LocalDate): LocalDate = d.minusDays(((d.jsDay() + 6) % 7).toLong())

    fun weeksBetween(a: LocalDate, b: LocalDate) = max(0, floor(ChronoUnit.DAYS.between(weekStart(a), weekStart(b)) / 7.0).toInt())

    /** Weekly limit: starts at 3–5 and drops by one every two weeks until 3; a hand-picked limit is fixed from the day it was set. */
    fun weeklyLimitFor(startLimit: Int, startedAt: LocalDate?, now: LocalDate = LocalDate.now(), limitSetAt: LocalDate? = null): Int {
        val s = max(0, min(5, startLimit))
        if (limitSetAt != null && !now.isBefore(limitSetAt)) return s
        if (startedAt == null) return s
        return max(min(3, s), s - weeksBetween(startedAt, now) / 2)
    }

    private val BASE_DAYS = mapOf(League.low to listOf(1, 3, 5), League.mid to listOf(1, 3, 5), League.high to listOf(1, 2, 4, 6))
    private val REST_TIPS = listOf(
        "Oggi riposo. I muscoli crescono adesso, non mentre ti alleni. Bevi tanto e dormi presto.",
        "Giorno off. Una passeggiata va benissimo, il resto lo fa il recupero.",
        "Riposo. Vai a scuola, esci con gli amici, vivi. Domani si torna a spingere.",
        "Oggi niente scheda. Se proprio vuoi muoverti, 10 minuti di stretching e basta.",
        "Recupero. Mangia bene, proteine a pranzo e cena, e stanotte niente telefono a letto.",
    )

    /** The web's rng(seed): FNV-1a hash + mulberry32, with JS double/int32 semantics. */
    class Rng(seed: String) {
        private var h: Double
        init {
            var x = 2166136261.0
            for (ch in seed) x = ((toInt32(x) xor ch.code) * 16777619).toDouble()
            h = x
        }
        fun next(): Double {
            h += 0x6d2b79f5.toDouble()
            var t = toInt32(h)
            t = (t xor (t ushr 15)) * (t or 1)
            t = t xor (t + (t xor (t ushr 7)) * (t or 61))
            return ((t xor (t ushr 14)).toLong() and 0xFFFFFFFFL).toDouble() / 4294967296.0
        }
        // JS ToInt32 of an integral double: low 32 bits (h stays an exact integer far below 2^53)
        private fun toInt32(d: Double): Int = d.toLong().toInt()
    }

    /** V8 sort for arrays shorter than 64: CountAndMakeRun + BinaryInsertionSort, with the comparator `() => r() - 0.5`. */
    fun <T> jsShuffle(arr: List<T>, r: Rng): List<T> {
        val a = arr.toMutableList()
        val n = a.size
        if (n < 2) return a
        fun cmp(): Double = r.next() - 0.5
        // CountAndMakeRun(0, n)
        var run = 2
        val desc = cmp() < 0
        var idx = 2
        while (idx < n) {
            val o = cmp()
            if (desc) { if (o >= 0) break } else { if (o < 0) break }
            run++; idx++
        }
        if (desc) a.subList(0, run).reverse()
        // BinaryInsertionSort(0, run, n)
        var start = if (run == 0) 1 else run
        while (start < n) {
            var left = 0; var right = start
            val pivot = a[start]
            while (left < right) {
                val mid = left + ((right - left) shr 1)
                if (cmp() < 0) right = mid else left = mid + 1
            }
            for (p in start downTo left + 1) a[p] = a[p - 1]
            a[left] = pivot
            start++
        }
        return a
    }

    fun trainingDays(league: League, a: ZeroAnswers): List<Int> {
        val gym = (a.gymDays ?: emptyList()).filter { it in 0..6 }
        val set = LinkedHashSet(BASE_DAYS.getValue(league) + gym)
        fun on(d: Int) = set.contains(((d % 7) + 7) % 7)
        repeat(3) {
            for (d in 0 until 7) {
                if (on(d) && on(d + 1) && on(d + 2)) {
                    val cand = listOf(d + 1, d, d + 2).map { it % 7 }.firstOrNull { it !in gym }
                    if (cand != null) set.remove(cand)
                }
            }
        }
        val maxN = if (league == League.high) 5 else 4
        for (d in listOf(0, 6, 3, 2, 4, 5, 1)) { if (set.size <= maxN) break; if (d !in gym) set.remove(d) }
        return set.toList().sortedBy { it.toString() } // JS default sort compares as strings (single digits: same as numeric)
    }

    fun isTrainingDay(league: League, d: LocalDate, a: ZeroAnswers) = d.jsDay() in trainingDays(league, a)
    private fun isGymDay(a: ZeroAnswers, d: LocalDate) = d.jsDay() in (a.gymDays ?: emptyList())

    private class Tpl(val name: String, val unit: String, val amt: (ZeroAnswers, League) -> Double, val tip: String)
    private fun lv(l: League, lo: Double, mid: Double, hi: Double) = when (l) { League.low -> lo; League.mid -> mid; else -> hi }
    private fun clamp(v: Double, lo: Double, hi: Double) = max(lo, min(hi, v))

    private val PUSH = listOf(
        Tpl("Flessioni", "rip", { a, l -> clamp(a.pushups * 0.5, lv(l, 5.0, 8.0, 12.0), lv(l, 12.0, 18.0, 25.0)) }, "Corpo dritto, petto quasi a terra."),
        Tpl("Flessioni larghe", "rip", { a, l -> clamp(a.pushups * 0.4, lv(l, 4.0, 6.0, 10.0), lv(l, 10.0, 15.0, 20.0)) }, "Mani più larghe delle spalle: più petto."),
        Tpl("Flessioni strette", "rip", { a, l -> clamp(a.pushups * 0.3, lv(l, 3.0, 5.0, 8.0), lv(l, 8.0, 12.0, 16.0)) }, "Mani sotto il petto: lavorano i tricipiti."),
    )
    private val ABS = listOf(
        Tpl("Plank", "sec", { a, l -> clamp(a.plank * 0.5, lv(l, 20.0, 30.0, 40.0), lv(l, 40.0, 50.0, 70.0)) }, "Addome duro, sedere in linea con la schiena."),
        Tpl("Crunch", "rip", { _, l -> lv(l, 12.0, 15.0, 20.0) }, "Sali con la pancia, non tirare il collo."),
        Tpl("Sit-up", "rip", { _, l -> lv(l, 8.0, 12.0, 15.0) }, "Piedi a terra, sali fino a sederti."),
        Tpl("Leg raise", "rip", { _, l -> lv(l, 8.0, 10.0, 14.0) }, "Sdraiato, gambe tese su e giù piano."),
        Tpl("Plank laterale", "sec", { a, l -> clamp(a.plank * 0.3, lv(l, 15.0, 20.0, 25.0), lv(l, 25.0, 35.0, 45.0)) }, "Per lato. Fianco alto."),
    )
    private val LEGS = listOf(
        Tpl("Squat", "rip", { a, l -> clamp(a.squats * 0.4, lv(l, 10.0, 12.0, 15.0), lv(l, 15.0, 20.0, 25.0)) }, "Talloni a terra, sedere indietro."),
        Tpl("Affondi", "rip", { _, l -> lv(l, 8.0, 10.0, 12.0) }, "Per gamba. Ginocchio dietro che sfiora terra."),
    )
    private fun dbReps(kg: Double, light: Double) = if (kg <= light) 15.0 else if (kg <= light * 1.6) 12.0 else if (kg <= light * 2.4) 10.0 else if (kg <= light * 3.2) 8.0 else 6.0
    private val DUMB = listOf(
        Tpl("Curl bicipiti con manubrio", "rip", { a, _ -> dbReps((a.dumbbellKg ?: 0).toDouble(), 5.0) }, "Per braccio. Gomito fermo vicino al fianco."),
        Tpl("Lento avanti con manubrio", "rip", { a, _ -> dbReps((a.dumbbellKg ?: 0).toDouble(), 4.0) }, "Per braccio. Spingi sopra la testa, schiena dritta."),
        Tpl("Rematore con manubrio", "rip", { a, _ -> dbReps((a.dumbbellKg ?: 0).toDouble(), 7.0) }, "Per braccio. Mano e ginocchio sulla sedia, tira al fianco."),
        Tpl("Squat con manubrio", "rip", { a, _ -> dbReps((a.dumbbellKg ?: 0).toDouble(), 8.0) }, "Manubrio al petto, scendi lento."),
    )
    private val PULL = listOf(
        Tpl("Trazioni", "rip", { a, _ -> clamp(jsRound(a.pullups * 0.6), 1.0, 12.0) }, "Mento sopra la sbarra, scendi controllato. Se non sali, salta su e scendi piano."),
    )
    private val OUTDOOR = listOf(
        Tpl("Corsa", "min", { _, l -> lv(l, 12.0, 18.0, 25.0) }, "Ritmo che ti permette di parlare. Niente sprint."),
        Tpl("Corsa", "km", { _, l -> lv(l, 2.0, 3.0, 4.0) }, "Distanza, non velocità. Cammina se serve, ma finisci."),
        Tpl("Dip alle parallele", "rip", { a, l -> clamp(a.pushups * 0.25, lv(l, 3.0, 5.0, 8.0), lv(l, 6.0, 10.0, 12.0)) }, "Scendi fino a 90° coi gomiti."),
    )
    private val INTENSITY = mapOf("leggero" to 0.8, "normale" to 1.0, "tosto" to 1.2)

    /** JS Math.round (halves round up). */
    fun jsRound(v: Double) = floor(v + 0.5)

    /** Deterministic daily plan + auto-rest when the user already trained a lot. seed = the user id. */
    fun planForDay(d: LocalDate, league: League, a: ZeroAnswers, assessedAt: LocalDate?, seed: String, doneKeys: Set<String>? = null): DayPlan {
        val key = key(d)
        val r0 = Rng("$seed:$key:rest")
        fun restPlan(auto: String? = null) = DayPlan(key, true, "rest", "Riposo", emptyList(), auto ?: REST_TIPS[floor(r0.next() * REST_TIPS.size).toInt()], autoRest = auto)
        if (doneKeys != null) {
            fun back(n: Long) = key(d.minusDays(n))
            if (back(1) in doneKeys && back(2) in doneKeys) return restPlan("Ti sei allenato due giorni di fila: oggi riposo obbligato. Il recupero è metà dell'allenamento.")
            val w = (1..6L).count { back(it) in doneKeys }
            if (w >= 5) return restPlan("Cinque allenamenti negli ultimi giorni. Oggi stop, senza discutere.")
        }
        if (!isTrainingDay(league, d, a)) return restPlan()
        val r = Rng("$seed:$key")
        val gym = isGymDay(a, d)
        val hasDb = (a.dumbbellKg ?: 0) > 0
        val canPull = gym || a.hasBar == true
        val roll = r.next()
        val intensity = if (roll < 0.3) "leggero" else if (roll < 0.8) "normale" else "tosto"
        val weeks = if (assessedAt != null) weeksBetween(assessedAt, d) else 0
        val prog = 1 + min(0.3, weeks * 0.04)
        fun <T> pick(arr: List<T>) = arr[floor(r.next() * arr.size).toInt()]
        fun <T> shuffled(arr: List<T>) = jsShuffle(arr, r)
        val title: String
        var list: MutableList<Tpl>
        if (gym) {
            title = "Parco / Palestra"
            list = mutableListOf(PULL[0], pick(OUTDOOR.subList(0, 2)), OUTDOOR[2], pick(ABS))
        } else {
            val types = listOf("push", "abs", "mix") + (if (hasDb) listOf("arms") else emptyList())
            val t = types[floor(r.next() * types.size).toInt()]
            when (t) {
                "push" -> { title = "Flessioni & addome"; list = (shuffled(PUSH).take(2) + shuffled(ABS).take(2)).toMutableList() }
                "abs" -> { title = "Addome & core"; list = (shuffled(ABS).take(3) + pick(LEGS)).toMutableList() }
                "arms" -> { title = "Pesi"; list = (shuffled(DUMB).take(3) + pick(PUSH)).toMutableList() }
                else -> { title = "Total body"; list = mutableListOf(pick(PUSH), pick(ABS), pick(LEGS), if (hasDb) pick(DUMB) else pick(ABS.filter { it.name != "Plank" })) }
            }
            if (canPull && r.next() < 0.35) list[list.size - 1] = PULL[0]
        }
        val n = if (intensity == "leggero") 3 else if (intensity == "tosto") (if (league == League.low) 4 else 5) else (if (league == League.low) 3 else 4)
        if (intensity == "tosto" && list.size < n) list.add(pick(PUSH))
        list = list.take(n).toMutableList()
        val sets = if (league == League.low) 2 else 3
        val mult = INTENSITY.getValue(intensity) * prog
        val seen = HashSet<String>()
        val exercises = list.filter { seen.add(it.name + it.unit) }.mapIndexed { i, t ->
            val raw = t.amt(a, league) * (if (t.unit == "min" || t.unit == "km") INTENSITY.getValue(intensity) else mult)
            val amount = when (t.unit) { "sec" -> max(15.0, jsRound(raw / 5) * 5); "km" -> max(1.0, jsRound(raw * 2) / 2); else -> max(1.0, jsRound(raw)) }
            val s = if (t.unit == "min" || t.unit == "km") 1 else sets
            Exercise("$key:$i", t.name, s, amount, t.unit, t.tip)
        }
        return DayPlan(key, false, title, title, exercises, intensity = intensity, gym = gym)
    }

    data class ZeroTheme(val id: String, val name: String, val colors: List<Long>)
    val THEMES = listOf(
        ZeroTheme("obsidian", "Ossidiana", listOf(0xFF0b0b0d, 0xFF1a1a1f, 0xFFe9c46a, 0xFFf4f1e8)),
        ZeroTheme("forest", "Foresta", listOf(0xFF07140f, 0xFF10261c, 0xFF7ee08a, 0xFFe7f5ea)),
        ZeroTheme("abyss", "Abisso", listOf(0xFF04101a, 0xFF0b2232, 0xFF3ee0d0, 0xFFe6f7f9)),
        ZeroTheme("ember", "Brace", listOf(0xFF140806, 0xFF2a110c, 0xFFff7a45, 0xFFfbeee8)),
        ZeroTheme("steel", "Acciaio", listOf(0xFF0e1114, 0xFF1c2126, 0xFFc7d2dc, 0xFFf2f5f7)),
    )
}
