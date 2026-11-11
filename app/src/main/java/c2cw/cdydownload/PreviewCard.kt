package c2cw.cdydownload

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
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
@OptIn(ExperimentalLayoutApi::class)
@Composable internal fun PreviewCard(preview: MediaPreview, vm: MainViewModel) {
    UiGroup(modifier = Modifier.fillMaxWidth().testTag("previewCard")) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if(preview.cover.isNotBlank())
                Box(Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(20.dp))) {
                    RemoteImage(preview.cover, Modifier.fillMaxSize(), referer = preview.referer, userAgent = preview.userAgent)
                    Row(Modifier.align(Alignment.TopStart).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        PreviewBadge("${preview.platform.label} · 无水印")
                        if(preview.isAlbum) PreviewBadge("${preview.images.size} 项 · ${preview.imageVideos.count { it != null }} 项实况")
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
                    Text("已选 ${vm.selectedAlbumImages.size} / ${preview.images.size} 项",
                        style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    TextButton(onClick = vm::selectAllAlbumImages, modifier = Modifier.testTag("albumSelectAll")) { Text("全选") }
                    TextButton(onClick = vm::clearAlbumImageSelection, modifier = Modifier.testTag("albumSelectNone")) { Text("清空") }
                }
                Text("动态保存为视频，实况保存为含动态的照片", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(albumSummary(preview.images.indices.toList(), preview.imageVideos,
                    emptyMap()),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.testTag("albumGrid")) {
                    preview.images.indices.chunked(2).forEach { indices ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            indices.forEach { index ->
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(Modifier.fillMaxWidth().aspectRatio(0.75f).clip(RoundedCornerShape(16.dp))
                                        .clickable { vm.toggleAlbumImage(index) }.testTag("albumImage-$index")) {
                                        RemoteImage(preview.images[index], Modifier.fillMaxSize(), referer = preview.referer, userAgent = preview.userAgent)
                                        PreviewBadge("${index + 1} · ${if(preview.imageVideos.getOrNull(index) != null) "实况" else "静图"}")
                                        Checkbox(checked = index in vm.selectedAlbumImages,
                                            onCheckedChange = { vm.toggleAlbumImage(index) },
                                            modifier = Modifier.align(Alignment.TopEnd).testTag("albumImageCheck-$index"))
                                    }
                                    if(preview.imageVideos.getOrNull(index) != null) {
                                        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                          AlbumOutput.entries.forEach { output ->
                                            FilterChip(selected = albumOutput(index, preview.imageVideos, vm.albumOutputs) == output,
                                                onClick = { vm.chooseAlbumOutput(index, output) },
                                                label = { Text(when(output) { AlbumOutput.STATIC -> "静图"; AlbumOutput.VIDEO -> "动态"; AlbumOutput.MOTION -> "实况" }) },
                                                modifier = Modifier.testTag("albumOutput-$index-${output.name}"))
                                          }
                                        }
                                    } else Text("静图", style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            if(indices.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
                if(vm.selectedAlbumImages.isEmpty() && vm.albumContent != AlbumContent.MUSIC)
                    Text("至少选择一项内容才能下载", color = MaterialTheme.colorScheme.error,
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

internal fun albumAction(preview: MediaPreview, content: AlbumContent, indices: List<Int>, outputs: Map<Int, AlbumOutput>) =
    if(content == AlbumContent.MUSIC) "只保存配乐"
    else "保存所选 " + albumSummary(indices, preview.imageVideos, outputs) +
        if(content == AlbumContent.ALL && preview.hasMusic) "和配乐" else ""

private fun engagement(label: String, count: Int): String? = when {
    count < 0 -> null
    count >= 10000 -> "$label " + String.format(Locale.US, "%.1f万", count / 10000.0)
    else -> "$label $count"
}

private fun previewDate(millis: Long) = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(millis))
