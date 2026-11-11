package c2cw.cdydownload

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
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
 * One place for motion. Transitions are short, start quickly (fast-out) and stop without wobble.
 * Springs are critically damped except for a tiny kick on the tab indicator.
 */
internal object UiMotion {
    const val Instant = 80
    const val Fast = 120
    const val Medium = 160
    /** Fast-out, then a short settle. */
    val Emphasized = FastOutSlowInEasing
    val Exit = FastOutLinearInEasing
    /** Bottom-bar indicator only: stiff, almost no bounce. */
    const val IndicatorStiffness = 5000f
    const val IndicatorDamping = 0.9f
    /** Pager settle after a swipe, and other no-bounce springs. */
    const val SnapStiffness = 5000f
    /** Dismiss-rollback and similar short springs. */
    const val RollbackStiffness = 5000f

    fun <T> indicator(): FiniteAnimationSpec<T> =
        spring(dampingRatio = IndicatorDamping, stiffness = IndicatorStiffness)
    /** Tab / page switch: a short tween so a full-width move doesn't float. */
    fun <T> tab(): FiniteAnimationSpec<T> = tween(Fast, easing = Emphasized)
    fun <T> settle(): FiniteAnimationSpec<T> =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = SnapStiffness)
    fun <T> rollback(): FiniteAnimationSpec<T> =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = RollbackStiffness)
    fun <T> press(): FiniteAnimationSpec<T> =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh)
    fun <T> fade(duration: Int = Fast): FiniteAnimationSpec<T> = tween(duration, easing = Emphasized)
    fun <T> color(): FiniteAnimationSpec<T> = tween(Instant, easing = Emphasized)
    fun intSize(): FiniteAnimationSpec<IntSize> = tween(Medium, easing = Emphasized)
    fun intOffset(): FiniteAnimationSpec<IntOffset> = tween(Fast, easing = Emphasized)

    val expand: EnterTransition get() = expandVertically(intSize()) + fadeIn(fade())
    val collapse: ExitTransition get() = shrinkVertically(tween(Fast, easing = Exit)) + fadeOut(fade(Fast))
    val screenEnter: EnterTransition get() =
        slideInHorizontally(tween(Medium, easing = Emphasized)) { it / 8 } + fadeIn(fade())
    val screenExit: ExitTransition get() =
        slideOutHorizontally(tween(Fast, easing = Exit)) { it / 8 } + fadeOut(fade())
    val headsUpEnter: EnterTransition get() =
        slideInVertically(tween(Fast, easing = Emphasized)) { -it } + fadeIn(fade())
    val headsUpExit: ExitTransition get() =
        slideOutVertically(tween(Fast, easing = Exit)) { -it } + fadeOut(fade())
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
