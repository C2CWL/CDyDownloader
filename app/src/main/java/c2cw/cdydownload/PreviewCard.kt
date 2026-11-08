package c2cw.cdydownload

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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

/** Shows what the parser found, so the work itself is visible before a task is queued. */
@Composable internal fun PreviewCard(preview: MediaPreview, vm: MainViewModel, submit: () -> Unit) {
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth().testTag("previewCard")) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if(preview.cover.isNotBlank())
                Box(Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(14.dp))) {
                    RemoteImage(preview.cover, Modifier.fillMaxSize())
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
            if(preview.images.size > 1) LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(preview.images.take(30)) { url ->
                    RemoteImage(url, Modifier.size(76.dp).clip(RoundedCornerShape(10.dp)))
                }
            }
            if(preview.isAlbum) Button(onClick = submit, shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().testTag("previewDownload")) {
                Text(albumAction(preview, vm.albumContent))
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

private fun albumAction(preview: MediaPreview, content: AlbumContent) = when(content) {
    AlbumContent.MUSIC -> "只保存配乐"
    AlbumContent.IMAGES -> "保存 ${preview.images.size} 张图片"
    AlbumContent.ALL -> if(preview.hasMusic) "保存 ${preview.images.size} 张图片和配乐" else "保存 ${preview.images.size} 张图片"
}

private fun engagement(label: String, count: Int): String? = when {
    count < 0 -> null
    count >= 10000 -> "$label " + String.format(Locale.US, "%.1f万", count / 10000.0)
    else -> "$label $count"
}

private fun previewDate(millis: Long) = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(millis))
