package c2cw.cdydownload

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Shows resolved media before queueing and lets album images be chosen individually. */
@Composable internal fun PreviewCard(preview: MediaPreview, vm: MainViewModel) {
    Surface(modifier = Modifier.fillMaxWidth().testTag("previewCard"),
        shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if(preview.cover.isNotBlank())
                Box(Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(8.dp))) {
                    RemoteImage(preview.cover, Modifier.fillMaxSize(), referer = preview.referer, userAgent = preview.userAgent)
                    Row(Modifier.align(Alignment.TopStart).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        PreviewBadge("${preview.platform.label} · 无水印")
                        if(preview.isAlbum) PreviewBadge("${preview.images.size} 张图片")
                    }
                }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(preview.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                    maxLines = 3, overflow = TextOverflow.Ellipsis)
                val meta = listOfNotNull(
                    preview.author.takeIf { it.isNotBlank() }?.let { "作者 $it" },
                    preview.quality.takeIf { it.isNotBlank() && !preview.isAlbum },
                    preview.publishedAt.takeIf { it > 0 }?.let(::previewDate)
                )
                if(meta.isNotEmpty()) Text(meta.joinToString(" · "), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                val stats = listOfNotNull(
                    engagement("点赞", preview.likes), engagement("评论", preview.comments),
                    engagement("转发", preview.shares), "含配乐".takeIf { preview.hasMusic }
                )
                if(stats.isNotEmpty()) Text(stats.joinToString(" · "), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if(preview.isAlbum) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("已选 ${vm.selectedAlbumImages.size} / ${preview.images.size} 张",
                        style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    TextButton(onClick = vm::selectAllAlbumImages, modifier = Modifier.testTag("albumSelectAll")) { Text("全选") }
                    TextButton(onClick = vm::clearAlbumImageSelection, modifier = Modifier.testTag("albumSelectNone")) { Text("清空") }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(preview.images) { index, url ->
                        val selected = index in vm.selectedAlbumImages
                        Box(Modifier.size(84.dp).clip(RoundedCornerShape(6.dp))
                            .clickable { vm.toggleAlbumImage(index) }
                            .testTag("albumImage-$index")) {
                            RemoteImage(url, Modifier.fillMaxSize(), referer = preview.referer, userAgent = preview.userAgent)
                            PreviewBadge("${index + 1}")
                            Checkbox(checked = selected, onCheckedChange = { vm.toggleAlbumImage(index) },
                                modifier = Modifier.align(Alignment.TopEnd).testTag("albumImageCheck-$index"))
                        }
                    }
                }
                if(vm.selectedAlbumImages.isEmpty() && vm.albumContent != AlbumContent.MUSIC)
                    Text("至少选择一张图片才能下载", color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
            }

        }
    }
}

@Composable private fun PreviewBadge(text: String) {
    Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f), shape = RoundedCornerShape(8.dp)) {
        Text(text, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
    }
}

internal fun albumAction(preview: MediaPreview, content: AlbumContent, selectedCount: Int) = when(content) {
    AlbumContent.MUSIC -> "只保存配乐"
    AlbumContent.IMAGES -> "保存所选 $selectedCount 张图片"
    AlbumContent.ALL -> if(preview.hasMusic) "保存所选 $selectedCount 张图片和配乐" else "保存所选 $selectedCount 张图片"
}

private fun engagement(label: String, count: Int): String? = when {
    count < 0 -> null
    count >= 10000 -> "$label " + String.format(Locale.US, "%.1f万", count / 10000.0)
    else -> "$label $count"
}

private fun previewDate(millis: Long) = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(millis))
