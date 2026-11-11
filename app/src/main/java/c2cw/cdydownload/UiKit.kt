package c2cw.cdydownload

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.util.Locale

internal val CardShape = RoundedCornerShape(14.dp)
internal val TileShape = RoundedCornerShape(12.dp)

/** Small uppercase-style section title above a group of cards. */
@Composable internal fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(start = 4.dp, top = 6.dp))
}

/** Pill-shaped segmented control. [selected] = -1 shows no selection (mixed state). */
@Composable internal fun SegmentedControl(options: List<String>, selected: Int, modifier: Modifier = Modifier,
    tagPrefix: String = "segment", onSelect: (Int) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(modifier.clip(RoundedCornerShape(12.dp)).background(colors.surfaceContainerHigh).padding(3.dp)) {
        options.forEachIndexed { index, label ->
            val active = index == selected
            val bg by animateColorAsState(if(active) colors.primary else Color.Transparent, UiMotion.fade(), label = "segBg")
            val fg by animateColorAsState(if(active) colors.onPrimary else colors.onSurface, UiMotion.fade(), label = "segFg")
            Box(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(bg)
                .clickable(role = Role.Tab) { onSelect(index) }.semantics { this.selected = active }
                .testTag("$tagPrefix-$index").padding(vertical = 9.dp), contentAlignment = Alignment.Center) {
                Text(label, style = MaterialTheme.typography.labelLarge, color = fg, maxLines = 1)
            }
        }
    }
}

/** Compact two-option toggle drawn on top of images. */
@Composable internal fun MiniToggle(options: List<String>, selected: Int, modifier: Modifier = Modifier,
    tags: List<String> = emptyList(), onSelect: (Int) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(modifier.clip(RoundedCornerShape(8.dp)).background(Color.Black.copy(alpha = 0.55f)).padding(2.dp)) {
        options.forEachIndexed { index, label ->
            val active = index == selected
            val bg by animateColorAsState(if(active) colors.primary else Color.Transparent, UiMotion.fade(), label = "miniBg")
            Box(Modifier.clip(RoundedCornerShape(6.dp)).background(bg)
                .clickable(role = Role.Tab) { onSelect(index) }.semantics { this.selected = active }
                .then(tags.getOrNull(index)?.let { Modifier.testTag(it) } ?: Modifier)
                .padding(horizontal = 8.dp, vertical = 3.dp)) {
                Text(label, style = MaterialTheme.typography.labelSmall,
                    color = if(active) colors.onPrimary else Color.White, maxLines = 1)
            }
        }
    }
}

@Composable internal fun TagBadge(text: String, modifier: Modifier = Modifier, strong: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    Surface(modifier, shape = RoundedCornerShape(6.dp),
        color = if(strong) colors.primary else colors.primaryContainer) {
        Text(text, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold,
            color = if(strong) colors.onPrimary else colors.onPrimaryContainer, maxLines = 1,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
    }
}

/** Dark translucent label for use over images. */
@Composable internal fun ImageBadge(text: String, modifier: Modifier = Modifier) {
    Box(modifier.clip(RoundedCornerShape(6.dp)).background(Color.Black.copy(alpha = 0.55f))
        .padding(horizontal = 6.dp, vertical = 2.dp)) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = Color.White, maxLines = 1)
    }
}

/** Round selection mark for grid tiles. */
@Composable internal fun SelectMark(checked: Boolean, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val bg by animateColorAsState(if(checked) colors.primary else Color.Black.copy(alpha = 0.25f), UiMotion.fade(), label = "mark")
    Box(modifier.size(22.dp).clip(RoundedCornerShape(7.dp)).background(bg)
        .border(1.5.dp, if(checked) colors.primary else Color.White.copy(alpha = 0.9f), RoundedCornerShape(7.dp))
        .semantics { role = Role.Checkbox; selected = checked }, contentAlignment = Alignment.Center) {
        if(checked) Glyph("check", modifier = Modifier.size(16.dp), tint = colors.onPrimary)
    }
}

@Composable internal fun Avatar(url: String, name: String, size: Dp, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Box(modifier.size(size).clip(CircleShape).background(colors.primaryContainer), contentAlignment = Alignment.Center) {
        if(url.isBlank()) Text(name.trim().take(1).ifBlank { "?" }, color = colors.onPrimaryContainer,
            style = if(size >= 56.dp) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium)
        else RemoteImage(url, Modifier.fillMaxSize(), referer = "https://www.douyin.com/", userAgent = VideoResolver.MOBILE)
    }
}

/** Card row with title, optional value and description, and a trailing slot. */
@Composable internal fun SettingRow(title: String, modifier: Modifier = Modifier, value: String? = null,
    description: String? = null, onClick: (() -> Unit)? = null, trailing: @Composable (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().then(if(onClick != null) Modifier.pressable(scale = 0.99f, onClick = onClick) else Modifier)
        .padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if(value != null) Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            if(description != null) Text(description, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if(trailing != null) trailing() else if(onClick != null) Glyph("chevron", tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

internal fun compactCount(count: Long): String = when {
    count < 0 -> "—"
    count >= 100_000_000 -> String.format(Locale.US, "%.1f亿", count / 100_000_000.0).replace(".0亿", "亿")
    count >= 10_000 -> String.format(Locale.US, "%.1f万", count / 10_000.0).replace(".0万", "万")
    else -> count.toString()
}
