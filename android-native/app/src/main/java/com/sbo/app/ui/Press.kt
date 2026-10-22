package com.sbo.app.ui

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * iOS-style press: the finger going down sinks the button quickly (critically damped, no wobble);
 * letting go releases it with a soft spring that overshoots a hair and settles. A quick tap still
 * plays the whole dip, it never just flickers. Only the scale is animated and it is read in the
 * draw phase (graphicsLayer), so nothing recomposes: it runs at the display's full refresh rate.
 */
@Stable
class PressScale internal constructor() {
    internal val anim = Animatable(1f)
    val value: Float get() = anim.value
}

@Composable
fun rememberPressScale(): PressScale = remember { PressScale() }

private const val DOWN = 0.9f
private const val MIN_DIP_MS = 40L
// same spring as the website's framer-motion button: stiffness 400, damping 15, mass 1 (ratio 0.375)
private val SPRING = spring<Float>(dampingRatio = 0.375f, stiffness = 400f)

fun Modifier.pressable(press: PressScale, label: String, onPress: () -> Unit): Modifier = this.composed { composedPress(press, label, onPress) }

@Composable
private fun Modifier.composedPress(press: PressScale, label: String, onPress: () -> Unit): Modifier {
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    return this
        .semantics { role = Role.Button; onClick(label) { onPress(); true } }
        .pointerInput(press, onPress) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                val downAt = System.currentTimeMillis()
                var sink: Job? = scope.launch {
                    press.anim.animateTo(DOWN, SPRING)
                }
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                val up = waitForUpOrCancellation()
                scope.launch {
                    val held = System.currentTimeMillis() - downAt
                    if (held < MIN_DIP_MS) delay(MIN_DIP_MS - held)
                    sink = null
                    // keeps the current velocity (Animatable carries it over), like framer's spring
                    press.anim.animateTo(1f, SPRING)
                }
                if (up != null) { up.consume(); onPress() }
            }
        }
}

