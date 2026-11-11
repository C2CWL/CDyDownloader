package c2cw.cdydownload

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Status follows the resolver; the scan is indeterminate, never a fabricated percentage. */
@Composable internal fun ResolvePanel(
    loading: Boolean, resolved: Boolean, failed: Boolean, platform: String?,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val accent = when {
        failed -> colors.error
        resolved -> Color(0xFF55DCA0)
        else -> Color(0xFFFF863D)
    }
    Surface(modifier = Modifier.fillMaxWidth().testTag("resolvePanel"),
        shape = RoundedCornerShape(12.dp), color = colors.surface,
        border = BorderStroke(1.dp, if(loading || resolved || failed) accent.copy(alpha = .7f) else colors.outlineVariant)) {
        Column {
            Box(Modifier.fillMaxWidth().height(82.dp)) {
                ResolveScan(loading, accent, Modifier.matchParentSize())
                Row(Modifier.fillMaxSize().padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Glyph(when { failed -> "error"; resolved -> "completed"; loading -> "search"; else -> "link" },
                        modifier = Modifier.size(26.dp), tint = accent)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(when {
                            failed -> "解析未完成"
                            resolved -> "解析完成"
                            loading -> "正在解析作品"
                            else -> "作品解析"
                        }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                            color = Color.White)
                        Text(when {
                            failed -> "检查链接后重试"
                            resolved -> "预览作品，选择保存内容"
                            loading -> "正在获取作品信息，请稍候"
                            else -> "粘贴分享链接，提取视频或图集"
                        }, style = MaterialTheme.typography.bodySmall, color = Color(0xFFB8C2CE))
                    }
                    platform?.let {
                        Surface(color = accent.copy(alpha = .16f), shape = RoundedCornerShape(4.dp)) {
                            Text(it, color = accent, style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
                        }
                    }
                }
            }
            Column(content = content)
        }
    }
}

@Composable private fun ResolveScan(loading: Boolean, accent: Color, modifier: Modifier) {
    // Removing the transition when idle stops the frame loop after success, failure or cancellation.
    val phase: () -> Float = if(loading) {
        val transition = rememberInfiniteTransition(label = "resolveScan")
        val position = transition.animateFloat(0f, 1f,
            infiniteRepeatable(tween(1500, easing = LinearEasing)), label = "scanPosition")
        val readPosition: () -> Float = { position.value }
        readPosition
    } else { { 0f } }
    Canvas(modifier.testTag("resolveScan").then(if(loading) Modifier.semantics {
        progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
    } else Modifier)) {
        drawRect(Color(0xFF141D29))
        val grid = 20.dp.toPx()
        var x = 0f
        while(x < size.width) {
            drawLine(Color.White.copy(alpha = .045f), Offset(x, 0f), Offset(x, size.height))
            x += grid
        }
        var y = 0f
        while(y < size.height) {
            drawLine(Color.White.copy(alpha = .045f), Offset(0f, y), Offset(size.width, y))
            y += grid
        }
        if(loading) {
            val width = 96.dp.toPx()
            val head = phase() * (size.width + width)
            drawRect(Brush.horizontalGradient(listOf(Color.Transparent, accent.copy(alpha = .22f)),
                startX = head - width, endX = head), Offset(head - width, 0f), Size(width, size.height))
            drawLine(accent.copy(alpha = .75f), Offset(head, 0f), Offset(head, size.height), 1.dp.toPx())
            val segment = size.width / 8f
            repeat(8) { index ->
                val active = ((phase() * 8).toInt() % 8) == index
                drawRect(accent.copy(alpha = if(active) 1f else .18f),
                    Offset(index * segment, size.height - 3.dp.toPx()),
                    Size((segment - 3.dp.toPx()).coerceAtLeast(0f), 3.dp.toPx()))
            }
        } else {
            drawRect(accent, Offset(0f, size.height - 2.dp.toPx()), Size(size.width, 2.dp.toPx()))
        }
        val corner = 9.dp.toPx()
        val inset = 5.dp.toPx()
        for(right in listOf(false, true)) {
            val edge = if(right) size.width - inset else inset
            val end = edge + if(right) -corner else corner
            drawLine(accent.copy(alpha = .6f), Offset(edge, inset), Offset(end, inset))
            drawLine(accent.copy(alpha = .6f), Offset(edge, inset), Offset(edge, inset + corner))
        }
    }
}
