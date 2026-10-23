package com.sbo.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/** shadcn Switch: 36 x 20, primary when on, input (white/10) when off, 16dp thumb in the background colour. */
@Composable
fun SboSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    val x by animateFloatAsState(if (checked) 1f else 0f, tween(150), label = "sw")
    Box(
        Modifier.size(width = 36.dp, height = 20.dp).clip(CircleShape)
            .background(if (checked) Sbo.primary else Color.White.copy(alpha = 0.10f))
            .clickable { onChange(!checked) }.padding(2.dp),
    ) {
        Box(Modifier.offset(x = 16.dp * x).size(16.dp).shadow(4.dp, CircleShape).clip(CircleShape).background(Sbo.background))
    }
}

/** shadcn Slider: 6dp track primary/20, primary range, 16dp thumb (background fill, primary/50 border). */
@Composable
fun SboSlider(value: Int, min: Int, max: Int, onChange: (Int) -> Unit, onCommit: (Int) -> Unit = {}) {
    BoxWithConstraints(Modifier.fillMaxWidth().height(20.dp)) {
        val w = constraints.maxWidth.toFloat()
        val thumb = 16.dp
        val mw = maxWidth
        fun pick(px: Float): Int {
            val f = ((px - 8f) / (w - 16f)).coerceIn(0f, 1f)
            return (min + f * (max - min)).roundToInt()
        }
        val f = (value - min).toFloat() / (max - min).coerceAtLeast(1)
        Box(
            Modifier.fillMaxWidth().fillMaxHeight()
                .pointerInput(min, max) { detectTapGestures { onChange(pick(it.x)); onCommit(pick(it.x)) } }
                .pointerInput(min, max) {
                    var last = value
                    detectHorizontalDragGestures(onDragEnd = { onCommit(last) }) { c, _ -> last = pick(c.position.x); onChange(last) }
                },
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(Sbo.primary.copy(alpha = 0.2f))) {
                Box(Modifier.fillMaxWidth(f).fillMaxHeight().background(Sbo.primary))
            }
            Box(
                Modifier.offset(x = (mw - thumb) * f).size(thumb).shadow(2.dp, CircleShape).clip(CircleShape)
                    .background(Sbo.background).border(1.dp, Sbo.primary.copy(alpha = 0.5f), CircleShape),
            )
        }
    }
}

/** shadcn Button default size (h-9, px-4, text-sm, rounded-md): default / secondary / destructive / ghost. */
@Composable
fun SboButton(
    text: String, icon: androidx.compose.ui.graphics.vector.ImageVector? = null, variant: String = "default",
    modifier: Modifier = Modifier.fillMaxWidth(), enabled: Boolean = true, fontSize: Int = 14, onClick: () -> Unit,
) {
    val press = rememberPressScale()
    val bg = when (variant) { "default" -> Sbo.primary; "secondary" -> Sbo.secondary; "destructive" -> Sbo.destructive; else -> Color.Transparent }
    val fg = when (variant) { "default" -> Sbo.primaryFg; "destructive" -> Color.White; else -> Sbo.foreground }
    Row(
        modifier.height(36.dp).graphicsScale(press).clip(RoundedCornerShape(18.dp)).background(bg.copy(alpha = bg.alpha * if (enabled) 1f else 0.5f))
            .then(if (enabled) Modifier.pressable(press, text, onClick) else Modifier).padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) { androidx.compose.material3.Icon(icon, null, tint = fg, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(8.dp)) }
        Text(text, color = fg.copy(alpha = if (enabled) 1f else 0.5f), fontSize = fontSize.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium)
    }
}

/** shadcn TabsList (h-9, muted, p-1) with equal triggers; the active one gets the background colour. */
@Composable
fun SegTabs(items: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().height(36.dp).clip(RoundedCornerShape(20.dp)).background(Sbo.muted).padding(4.dp)) {
        items.forEachIndexed { i, t ->
            val on = i == selected
            Box(
                Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(18.dp))
                    .then(if (on) Modifier.shadow(1.dp, RoundedCornerShape(18.dp)).background(Sbo.background) else Modifier)
                    .clickable { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) { Text(t, color = if (on) Sbo.foreground else Sbo.mutedFg, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium) }
        }
    }
}

@Composable
fun FieldLabel(text: String) = Text(text, fontSize = 14.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium)

@Composable
fun UpperLabel(text: String) = Text(text.uppercase(), color = Sbo.mutedFg, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.6.sp)

@Composable
fun DialogTitle(text: String, center: Boolean = true) =
    Text(text, fontSize = 18.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.45).sp, textAlign = if (center) TextAlign.Center else TextAlign.Start, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))

@Composable
fun DialogDesc(text: String) =
    Text(text, color = Sbo.mutedFg, fontSize = 14.sp, lineHeight = 20.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))

fun Modifier.outlined(radius: Dp = 20.dp) = this.border(1.dp, Sbo.border, RoundedCornerShape(radius))
