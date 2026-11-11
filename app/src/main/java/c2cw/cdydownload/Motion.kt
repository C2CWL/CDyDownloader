package c2cw.cdydownload

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

/**
 * One place for motion. Everything is short and critically damped: it starts immediately,
 * follows the finger and stops without wobble.
 */
internal object UiMotion {
    const val Fast = 150
    const val Medium = 220
    /** Decelerates hard at the end so things land crisply. */
    val Emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val Exit = CubicBezierEasing(0.3f, 0f, 1f, 1f)
    /** Page switches and indicator movement. */
    fun <T> snappy(): FiniteAnimationSpec<T> = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 1600f)
    /** Pager settle after a swipe. */
    fun <T> settle(): FiniteAnimationSpec<T> = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
    /** Press feedback. */
    fun <T> press(): FiniteAnimationSpec<T> = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh)
    fun <T> fade(duration: Int = Fast): FiniteAnimationSpec<T> = tween(duration, easing = Emphasized)
    fun intSize(): FiniteAnimationSpec<IntSize> = tween(Medium, easing = Emphasized)
    fun intOffset(): FiniteAnimationSpec<IntOffset> = tween(Medium, easing = Emphasized)

    val expand: EnterTransition get() = expandVertically(intSize()) + fadeIn(fade())
    val collapse: ExitTransition get() = shrinkVertically(tween(Fast, easing = Exit)) + fadeOut(tween(Fast - 30))
    val screenEnter: EnterTransition get() =
        slideInHorizontally(tween(Medium, easing = Emphasized)) { it / 5 } + fadeIn(tween(Fast))
    val screenExit: ExitTransition get() =
        slideOutHorizontally(tween(Fast, easing = Exit)) { it / 5 } + fadeOut(tween(Fast))
}

/** Click with a quick, small scale-down while pressed. */
internal fun Modifier.pressable(enabled: Boolean = true, scale: Float = 0.97f, onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val value by animateFloatAsState(if(pressed && enabled) scale else 1f, UiMotion.press(), label = "press")
    graphicsLayer { scaleX = value; scaleY = value }.then(
        if(onLongClick != null) Modifier.combinedClickable(interactionSource = source,
            indication = androidx.compose.foundation.LocalIndication.current, enabled = enabled,
            onLongClick = onLongClick, onClick = onClick)
        else Modifier.clickable(interactionSource = source,
            indication = androidx.compose.foundation.LocalIndication.current, enabled = enabled, onClick = onClick))
}
