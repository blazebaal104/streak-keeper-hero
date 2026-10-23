package com.sbo.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/** One palette = the CSS variables a theme overrides (web src/styles.css, converted from OKLCH). */
data class Palette(
    val background: Color, val surfaceTop: Color, val card: Color, val primary: Color, val primaryFg: Color,
    val secondary: Color, val muted: Color, val mutedFg: Color, val accent: Color, val accentFg: Color,
)

object Palettes {
    val default = Palette(
        Color(0xFF060E15), Color(0xFF002332), Color(0xFF0D1A23), Color(0xFF64E0FF), Color(0xFF040C13),
        Color(0xFF192834), Color(0xFF14212A), Color(0xFF85919A), Color(0xFF00B9C3), Color(0xFF040C13),
    )
    /** Halloween (html[data-holiday="halloween"]): night purple + pumpkin; overrides every other theme. */
    val halloween = Palette(
        Color(0xFF0B0615), Color(0xFF2C1D45), Color(0xFF170E25), Color(0xFFFF8421), Color(0xFF0F0A18),
        Color(0xFF251C36), Color(0xFF20182F), Color(0xFFAE9CBD), Color(0xFFAE6DEE), Color(0xFFF9F4EE),
    )
    /** Progetto Zero themes (html[data-pz-theme=...]). */
    val zero = mapOf(
        "obsidian" to Palette(Color(0xFF080706), Color(0xFF22190A), Color(0xFF13110E), Color(0xFFEBBD57), Color(0xFF0D0B07), Color(0xFF221F1A), Color(0xFF1D1A15), Color(0xFF9F978B), Color(0xFFE8A95C), Color(0xFF040C13)),
        "forest" to Palette(Color(0xFF020F06), Color(0xFF042C17), Color(0xFF071B0F), Color(0xFF75E07C), Color(0xFF020F06), Color(0xFF132A1B), Color(0xFF112418), Color(0xFF8DA692), Color(0xFF90BF5B), Color(0xFF040C13)),
        "abyss" to Palette(Color(0xFF000C12), Color(0xFF002D31), Color(0xFF001820), Color(0xFF45E2D2), Color(0xFF000E14), Color(0xFF00272F), Color(0xFF012129), Color(0xFF82A6A8), Color(0xFF2AC4CC), Color(0xFF040C13)),
        "ember" to Palette(Color(0xFF120604), Color(0xFF3C1304), Color(0xFF1F0F0A), Color(0xFFFF7F3B), Color(0xFF130806), Color(0xFF321A14), Color(0xFF2B1711), Color(0xFFB5978C), Color(0xFFF66E5C), Color(0xFF040C13)),
        "steel" to Palette(Color(0xFF0A0E10), Color(0xFF20282D), Color(0xFF14191C), Color(0xFFCCDAE4), Color(0xFF090E11), Color(0xFF21272C), Color(0xFF1D2226), Color(0xFF999FA4), Color(0xFFA5BBC7), Color(0xFF040C13)),
    )
}

/**
 * App colours. The themeable ones are snapshot state: switching a Progetto Zero theme recolours every
 * screen that reads them, like the web swapping CSS variables.
 */
object Sbo {
    /** Theme chosen by the user (default or a Progetto Zero theme). */
    var base by mutableStateOf(Palettes.default)
    /** Holiday in progress ("halloween" or null): its palette wins over everything. */
    var holiday by mutableStateOf<String?>(null)
    val palette: Palette get() = if (holiday == "halloween") Palettes.halloween else base
    val background get() = palette.background
    val surfaceTop get() = palette.surfaceTop
    val card get() = palette.card
    val primary get() = palette.primary
    val primaryFg get() = palette.primaryFg
    val secondary get() = palette.secondary
    val muted get() = palette.muted
    val mutedFg get() = palette.mutedFg
    val accent get() = palette.accent
    val accentFg get() = palette.accentFg
    val foreground = Color(0xFFF6F9FB)
    val destructive = Color(0xFFF94144)
    val border get() = if (holiday == "halloween") Color(0x24C7AFF5) else Color(0x0FFFFFFF)
    val dropA = Color(0xFFD9F4FD)
    val dropB = Color(0xFF00C6E2)
    val dropGlow = Color(0xFF00CAEC)
    val orange = Color(0xFFFB923C)
    val sky = Color(0xFF7DD3FC)
    // Progetto Zero status colours
    val zeroOk = Color(0xFF3FC168)
    val zeroLimit = Color(0xFFF3B01D)
    val zeroOver = Color(0xFFF92434)

    fun applyZeroTheme(theme: String?) { base = theme?.let { Palettes.zero[it] } ?: Palettes.default }
}
