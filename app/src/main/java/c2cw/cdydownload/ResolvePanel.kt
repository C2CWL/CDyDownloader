package c2cw.cdydownload

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Real resolver states, with a short color transition rather than springing the whole card. */
@Composable internal fun ResolvePanel(
    loading: Boolean, resolved: Boolean, failed: Boolean, platform: String?,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val accent by animateColorAsState(
        if(failed) colors.error else if(loading || resolved) colors.primary else colors.outline,
        animationSpec = tween(160), label = "resolveAccent")
    OutlinedCard(modifier = Modifier.fillMaxWidth().testTag("resolvePanel"),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, accent)) {
        Surface(color = colors.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(shape = RoundedCornerShape(6.dp), color = colors.surface,
                    border = BorderStroke(1.dp, accent)) {
                    Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                        Glyph(when {
                            failed -> "error"
                            resolved -> "completed"
                            loading -> "search"
                            else -> "link"
                        }, tint = accent)
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(when {
                        failed -> "解析失败"
                        resolved -> "解析完成"
                        loading -> "正在解析作品"
                        else -> "作品解析"
                    }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(when {
                        failed -> "检查链接后重试"
                        resolved -> "预览作品，选择保存内容"
                        loading -> "正在获取作品信息，请稍候"
                        else -> "粘贴分享链接，提取视频或图集"
                    }, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
                platform?.let {
                    Surface(color = colors.primaryContainer, shape = RoundedCornerShape(4.dp)) {
                        Text(it, color = colors.onPrimaryContainer, style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
                    }
                }
            }
        }
        // The native indicator is only composed while loading; no fake percentage or idle frame loop.
        if(loading) LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth().height(3.dp).testTag("inlineResolveProgress"),
            color = colors.primary, trackColor = colors.surfaceContainer,
            strokeCap = StrokeCap.Butt)
        else HorizontalDivider(thickness = 2.dp, color = accent)
        content()
    }
}

@Composable internal fun ResolveOptions(content: @Composable ColumnScope.() -> Unit) {
    OutlinedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), content = content)
}
