package com.sbo.app.logic

import androidx.compose.ui.graphics.Color

/** Medal definitions, same list as the website (src/lib/sbo-badges.ts) and the DB award function. */
enum class Tier(val label: String, val rank: Int) {
    common("Comune", 1), uncommon("Non comune", 2), rare("Rara", 3), super_rare("Super rara", 4),
    epic("Epica", 5), mythic("Mitica", 6), legendary("Leggendaria", 7),
}

data class BadgeDef(
    val category: String, // fire | ice | workout
    val threshold: Int,
    val tier: Tier,
    val name: String,
    val emoji: String,
    val gradient: List<Color>, // the web's 135° linear-gradient stops
    val ring: Color,
)

private fun c(hex: Long) = Color(0xFF000000 or hex)
private fun ring(hex: Long, a: Float) = c(hex).copy(alpha = a)

object Badges {
    val FIRE = listOf(
        BadgeDef("fire", 3, Tier.common, "Scintilla", "✨", listOf(c(0x9ca3af), c(0x4b5563)), ring(0x94a3b8, .4f)),
        BadgeDef("fire", 7, Tier.common, "Falò", "🔥", listOf(c(0xa3a3a3), c(0x525252)), ring(0xa3a3a3, .4f)),
        BadgeDef("fire", 14, Tier.uncommon, "Braciere", "🪵", listOf(c(0x4ade80), c(0x16a34a)), ring(0x4ade80, .5f)),
        BadgeDef("fire", 21, Tier.uncommon, "Camino", "🏮", listOf(c(0x34d399), c(0x059669)), ring(0x34d399, .5f)),
        BadgeDef("fire", 30, Tier.rare, "Fornace", "⚒️", listOf(c(0x38bdf8), c(0x0284c7)), ring(0x38bdf8, .6f)),
        BadgeDef("fire", 50, Tier.super_rare, "Vulcano", "🌋", listOf(c(0x60a5fa), c(0x2563eb), c(0x1e40af)), ring(0x60a5fa, .6f)),
        BadgeDef("fire", 75, Tier.epic, "Inferno", "⚡", listOf(c(0xa78bfa), c(0x7c3aed)), ring(0xa78bfa, .7f)),
        BadgeDef("fire", 100, Tier.mythic, "Sole", "☀️", listOf(c(0xf472b6), c(0xa855f7), c(0xfacc15)), ring(0xe879f9, .7f)),
        BadgeDef("fire", 150, Tier.mythic, "Cometa", "☄️", listOf(c(0xf472b6), c(0xc084fc), c(0x38bdf8)), ring(0xf472b6, .7f)),
        BadgeDef("fire", 200, Tier.legendary, "Stella", "🌟", listOf(c(0xfbbf24), c(0xf59e0b), c(0xd97706)), ring(0xfbbf24, .8f)),
        BadgeDef("fire", 300, Tier.legendary, "Nova", "👑", listOf(c(0xfacc15), c(0xf97316), c(0xdc2626), c(0xa855f7)), ring(0xfacc15, .8f)),
    )
    val ICE = listOf(
        BadgeDef("ice", 1, Tier.rare, "Brina", "🌨️", listOf(c(0xbae6fd), c(0x7dd3fc)), ring(0x7dd3fc, .6f)),
        BadgeDef("ice", 3, Tier.epic, "Gelo", "❄️", listOf(c(0x7dd3fc), c(0x0ea5e9)), ring(0x38bdf8, .7f)),
        BadgeDef("ice", 7, Tier.epic, "Cristallo", "💎", listOf(c(0xa5f3fc), c(0x22d3ee), c(0x0891b2)), ring(0x22d3ee, .7f)),
        BadgeDef("ice", 14, Tier.mythic, "Iceberg", "🏔️", listOf(c(0xe0f2fe), c(0x38bdf8), c(0x1e3a8a)), ring(0x67e8f9, .8f)),
        BadgeDef("ice", 30, Tier.mythic, "Ghiacciaio", "🧊", listOf(c(0xc7d2fe), c(0x818cf8), c(0x4f46e5)), ring(0xa5b4fc, .8f)),
        BadgeDef("ice", 60, Tier.legendary, "Polare", "🐻‍❄️", listOf(c(0xffffff), c(0xe0f2fe), c(0x38bdf8), c(0x6366f1)), ring(0xffffff, .8f)),
        BadgeDef("ice", 100, Tier.legendary, "Assoluto Zero", "🌌", listOf(c(0xffffff), c(0xa5f3fc), c(0x818cf8), c(0xc026d3)), ring(0xf0abfc, .8f)),
    )
    val WORKOUT = listOf(
        BadgeDef("workout", 5, Tier.common, "Primi passi", "👟", listOf(c(0x9ca3af), c(0x4b5563)), ring(0x94a3b8, .4f)),
        BadgeDef("workout", 10, Tier.common, "Principiante", "💪", listOf(c(0xa3a3a3), c(0x525252)), ring(0xa3a3a3, .4f)),
        BadgeDef("workout", 25, Tier.uncommon, "Costante", "🏃", listOf(c(0x4ade80), c(0x16a34a)), ring(0x4ade80, .5f)),
        BadgeDef("workout", 50, Tier.rare, "Atleta", "🏅", listOf(c(0x38bdf8), c(0x0284c7)), ring(0x38bdf8, .6f)),
        BadgeDef("workout", 75, Tier.super_rare, "Guerriero", "⚔️", listOf(c(0x60a5fa), c(0x2563eb), c(0x1e40af)), ring(0x60a5fa, .6f)),
        BadgeDef("workout", 100, Tier.epic, "Atleta professionista", "🏆", listOf(c(0xa78bfa), c(0x7c3aed)), ring(0xa78bfa, .7f)),
        BadgeDef("workout", 200, Tier.mythic, "Macchina", "🦾", listOf(c(0xf472b6), c(0xa855f7), c(0xfacc15)), ring(0xe879f9, .7f)),
        BadgeDef("workout", 365, Tier.legendary, "Leggenda", "👑", listOf(c(0xfacc15), c(0xf97316), c(0xdc2626), c(0xa855f7)), ring(0xfacc15, .8f)),
    )
    val ALL = FIRE + ICE + WORKOUT

    /** Old rows use category "streak" for fire medals. */
    fun def(category: String, threshold: Int): BadgeDef? =
        ALL.firstOrNull { it.category == category && it.threshold == threshold }
            ?: if (category == "streak") FIRE.firstOrNull { it.threshold == threshold } else null

    fun sameGroup(rowCategory: String, group: String) = rowCategory == group || (group == "fire" && rowCategory == "streak")
}
