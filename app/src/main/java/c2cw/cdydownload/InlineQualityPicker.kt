package c2cw.cdydownload

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable internal fun InlineQualityPicker(catalog: MediaSpecifications, chosen: String?, linkKey: String,
    choose: (String?) -> Unit) {
    val options = listOf(TrackOption("", "自动（推荐）")) + catalog.videos
    UiGroup {
        InlineTrackPicker("画质", options, chosen.orEmpty(), true, "quality", linkKey) {
            choose(it.takeIf(String::isNotEmpty))
        }
    }
}

@Composable internal fun InlineTrackPicker(label: String, options: List<TrackOption>, selected: String?,
    enabled: Boolean, tag: String, resetKey: String = "", emptyLabel: String? = null, choose: (String) -> Unit) {
    var expanded by remember(resetKey) { mutableStateOf(false) }
    val option = options.find { it.id == selected }
    val summary = if(selected.isNullOrEmpty() && emptyLabel != null) emptyLabel else option?.label ?: "请选择$label"
    Column {
        Row(Modifier.fillMaxWidth().testTag(tag).semantics {
            stateDescription = if(expanded) "已展开" else "已收起"
        }.clickable(enabled = enabled && options.isNotEmpty(), onClickLabel = "选择$label") {
            expanded = !expanded
        }.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(summary, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if(enabled) Glyph(if(expanded) "close" else "expand", modifier = Modifier.size(18.dp))
        }
        AnimatedVisibility(expanded && enabled,
            enter = UiMotion.expand, exit = UiMotion.collapse) {
            Column(Modifier.fillMaxWidth().selectableGroup().padding(bottom = 8.dp)) {
                options.forEach { item ->
                    Row(Modifier.fillMaxWidth().testTag("$tag-${item.id.ifEmpty { "auto" }}")
                        .selectable(selected = item.id == selected, role = Role.RadioButton,
                            onClick = { choose(item.id); expanded = false })
                        .padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(item.label, style = MaterialTheme.typography.bodyMedium)
                            if(item.detail.isNotBlank()) Text(item.detail, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        RadioButton(selected = item.id == selected, onClick = null)
                    }
                }
            }
        }
    }
}
