package com.sbo.app.ui

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.sbo.app.R

/** Same fonts as the website: Inter for text, Space Grotesk for headings. */
@OptIn(ExperimentalTextApi::class)
private fun variable(res: Int, vararg weights: Int) = FontFamily(
    weights.map { w -> Font(res, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w))) },
)

val Inter = variable(R.font.inter, 400, 500, 600, 700)
val SpaceGrotesk = variable(R.font.space_grotesk, 500, 600, 700)
