package com.sbo.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The app's motion language, modelled on iOS:
 *  - every tappable thing sinks a little (and dims a hair) while the finger is on it, then springs back;
 *  - panels pop in with a soft spring (scale + lift + fade) and leave quickly the way they came;
 *  - while a panel is open the page behind it recedes (scales down a touch and blurs), like an iOS sheet.
 * Everything animates in the draw phase, so nothing recomposes per frame.
 */
object Motion {
    /** iOS "default" spring for things that appear: fast, a whisper of overshoot. */
    fun <T> pop() = spring<T>(dampingRatio = 0.78f, stiffness = 340f)
    /** iOS-like exit curve (ease-in, short). */
    val EXIT = CubicBezierEasing(0.4f, 0f, 1f, 1f)
}

// ------------------------------------------------------------------ press feedback for every .clickable

/** Default indication for the whole app (replaces Material's ripple): scale-down + dim, spring back on release. */
object IosIndication : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode = IosPressNode(interactionSource)
    override fun equals(other: Any?) = other === this
    override fun hashCode() = 7
}

private class IosPressNode(private val src: InteractionSource) : Modifier.Node(), DrawModifierNode {
    private val scale = Animatable(1f)
    private val dim = Animatable(0f)
    private val paint = Paint()

    override fun onAttach() {
        coroutineScope.launch {
            var downAt = 0L
            var release: Job? = null
            src.interactions.collect { i ->
                when (i) {
                    is PressInteraction.Press -> {
                        release?.cancel()
                        downAt = System.nanoTime()
                        launch { scale.animateTo(0.955f, spring(dampingRatio = 1f, stiffness = 1100f)) }
                        launch { dim.animateTo(1f, tween(90)) }
                    }
                    is PressInteraction.Release, is PressInteraction.Cancel -> {
                        release = launch {
                            // a quick tap still shows the whole dip
                            val held = (System.nanoTime() - downAt) / 1_000_000
                            if (held < 90) delay(90 - held)
                            launch { scale.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = 420f)) }
                            dim.animateTo(0f, tween(320))
                        }
                    }
                }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        val s = scale.value
        val d = dim.value
        if (s == 1f && d == 0f) { drawContent(); return }
        scale(s) {
            if (d > 0.01f) {
                paint.alpha = 1f - 0.18f * d
                drawIntoCanvas { c ->
                    c.saveLayer(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height), paint)
                    this@draw.drawContent()
                    c.restore()
                }
            } else this@draw.drawContent()
        }
    }
}

// ------------------------------------------------------------------ panels

/** Progress of the panel being shown: 0 = gone, 1 = fully in (springs slightly past 1). */
val LocalPop = compositionLocalOf<(() -> Float)?> { null }

/** Every open panel registers its progress here; the page behind reads the biggest one to recede. */
object Backdrop {
    internal val open = mutableStateListOf<() -> Float>()
    fun level(): Float = open.maxOfOrNull { it().coerceIn(0f, 1f) } ?: 0f
}

/**
 * Keeps [content] on screen while it animates out. Use instead of `if (visible) Panel()`;
 * the panel (SboDialog & co.) reads the progress from [LocalPop].
 */
@Composable
fun Pop(visible: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(visible, enter = EnterTransition.None, exit = ExitTransition.None) {
        val p = transition.animateFloat(
            transitionSpec = { if (targetState == EnterExitState.Visible) Motion.pop() else tween(190, easing = Motion.EXIT) },
            label = "pop",
        ) { if (it == EnterExitState.Visible) 1f else 0f }
        val read = remember(p) { { p.value } }
        androidx.compose.runtime.CompositionLocalProvider(LocalPop provides read) { content() }
    }
}

/** Same as [Pop] for panels driven by a nullable value (`x?.let { Panel(it) }`): the last value stays during the exit. */
@Composable
fun <T : Any> PopOf(value: T?, content: @Composable (T) -> Unit) {
    val last = remember { arrayOfNulls<Any>(1) }
    if (value != null) last[0] = value
    Pop(value != null) {
        @Suppress("UNCHECKED_CAST")
        (last[0] as T?)?.let { content(it) }
    }
}

/** For a panel: the progress from the enclosing [Pop], or its own spring-in if it isn't wrapped. Registers with [Backdrop]. */
@Composable
fun rememberPanelProgress(): () -> Float {
    val outer = LocalPop.current
    val own = remember { Animatable(0f) }
    if (outer == null) LaunchedEffect(Unit) { own.animateTo(1f, Motion.pop()) }
    val read = outer ?: remember { { own.value } }
    DisposableEffect(read) { Backdrop.open += read; onDispose { Backdrop.open.remove(read) } }
    return read
}

/** The dark scrim behind a panel. */
fun Modifier.scrim(p: () -> Float) = this.graphicsLayer { alpha = p().coerceIn(0f, 1f) }

/** The panel itself: pops from 90% with a small lift, fades in quickly. */
fun Modifier.panel(p: () -> Float, lift: Float = 28f, from: Float = 0.9f) = this.graphicsLayer {
    val v = p()
    val k = from + (1f - from) * v
    scaleX = k; scaleY = k
    translationY = (1f - v) * lift.dp.toPx()
    alpha = (v * 1.8f).coerceIn(0f, 1f)
}

/** The page behind open panels: recedes and softly blurs (blur on Android 12+). */
fun Modifier.recede() = this.graphicsLayer {
    val l = Backdrop.level()
    if (l > 0.001f) {
        val k = 1f - 0.035f * l
        scaleX = k; scaleY = k
        if (android.os.Build.VERSION.SDK_INT >= 31) {
            val r = 10.dp.toPx() * l
            renderEffect = androidx.compose.ui.graphics.BlurEffect(r, r, androidx.compose.ui.graphics.TileMode.Decal)
        }
    } else { scaleX = 1f; scaleY = 1f; renderEffect = null }
}
