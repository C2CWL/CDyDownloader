package c2cw.cdydownload

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

internal const val TAB_HOME = 0
internal const val TAB_FOLLOW = 1
internal const val TAB_DOWNLOADS = 2
internal const val TAB_SETTINGS = 3
internal const val TAB_COUNT = 4

private data class BarTab(val label: String, val icon: String)
private val Tabs = listOf(BarTab("首页", "home"), BarTab("关注", "user-heart"), BarTab("下载记录", "download"), BarTab("设置", "settings"))

private val ItemWidth = 78.dp
private val BarHeight = 62.dp
private val BarInset = 5.dp

/** Space the floating bar occupies above the navigation bar; scrolling content pads by this. */
internal val BottomBarSpace = BarHeight + 18.dp + 16.dp

/**
 * Liquid-glass pill. One backdrop blur (no per-frame clipping or cutouts), a thin highlight rim,
 * and a tinted pill that slides to the selected tab.
 */
@Composable internal fun GlassBottomBar(selected: Int, haze: HazeState, badge: Int = 0, modifier: Modifier = Modifier,
    onSelect: (Int) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val dark = colors.background.luminance() < 0.5f
    val shape = RoundedCornerShape(BarHeight / 2)
    val indicator by animateDpAsState(ItemWidth * selected, UiMotion.snappy(), label = "tabIndicator")
    val style = HazeStyle(backgroundColor = colors.background,
        tint = HazeTint(colors.surface.copy(alpha = if(dark) 0.55f else 0.62f)), blurRadius = 22.dp, noiseFactor = 0f)
    val rim = Brush.verticalGradient(listOf(Color.White.copy(alpha = if(dark) 0.22f else 0.75f),
        Color.White.copy(alpha = if(dark) 0.04f else 0.25f)))
    Box(modifier.testTag("tabIsland").shadow(14.dp, shape, ambientColor = Color.Black.copy(alpha = .25f),
        spotColor = Color.Black.copy(alpha = .25f)).clip(shape)
        .hazeEffect(haze, style = style)
        .border(0.8.dp, rim, shape)
        .padding(BarInset)) {
        Box(Modifier.offset(x = indicator).width(ItemWidth).height(BarHeight - BarInset * 2)
            .clip(RoundedCornerShape((BarHeight - BarInset * 2) / 2))
            .background(colors.primary.copy(alpha = if(dark) 0.26f else 0.16f)).testTag("tabCapsule"))
        Row(Modifier.height(BarHeight - BarInset * 2).selectableGroup()) {
            Tabs.forEachIndexed { index, tab ->
                val active = index == selected
                val tint by animateColorAsState(if(active) colors.primary else colors.onSurfaceVariant,
                    UiMotion.fade(), label = "tabTint")
                Column(Modifier.width(ItemWidth).fillMaxHeight().clip(RoundedCornerShape(26.dp))
                    .selectable(selected = active, role = Role.Tab, onClick = { onSelect(index) })
                    .testTag("tab$index"),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Box {
                        Glyph(tab.icon, modifier = Modifier.size(22.dp), tint = tint)
                        if(index == TAB_FOLLOW && badge > 0) Box(Modifier.align(Alignment.TopEnd).offset(4.dp, (-2).dp)
                            .size(8.dp).clip(RoundedCornerShape(4.dp)).background(colors.error))
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(tab.label, style = MaterialTheme.typography.labelSmall, color = tint, maxLines = 1)
                }
            }
        }
    }
}
