package c2cw.cdydownload

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 关注: followed Douyin authors with new-work badges. */
@Composable internal fun FollowPage(vm: MainViewModel, topInset: Dp) {
    val authors by vm.follows.authors.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme
    var unfollow by remember { mutableStateOf<FollowedAuthor?>(null) }
    val fresh = authors.count { it.newCount > 0 }
    LazyColumn(Modifier.fillMaxSize().testTag("followList"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = topInset, bottom = BottomBarSpace + 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item(key = "summary") {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(listOf("暂时只支持抖音", "共 ${authors.size} 位作者", if(fresh > 0) "$fresh 位有新作品" else null)
                    .filterNotNull().joinToString(" · "), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                val updates = vm.followUpdates
                if(updates.running) {
                    AppProgressBar(Modifier.fillMaxWidth(), progress = { if(updates.total == 0) 0f else updates.done / updates.total.toFloat() })
                    Text("正在检查 ${updates.done} / ${updates.total}", style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant)
                }
            }
        }
        if(authors.isEmpty()) item(key = "empty") {
            Column(Modifier.fillParentMaxHeight(0.6f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center) {
                Box(Modifier.size(72.dp).clip(RoundedCornerShape(18.dp)).background(colors.surfaceContainer),
                    contentAlignment = Alignment.Center) {
                    Glyph("user-heart", modifier = Modifier.size(30.dp), tint = colors.onSurfaceVariant)
                }
                Spacer(Modifier.height(18.dp))
                Text("还没有关注的作者", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text("解析抖音作品后点「全部作品」，或粘贴作者主页链接，在作者页点「关注此人」",
                    style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
        }
        items(authors, key = { it.secUid }) { author ->
            FollowRow(author, Modifier.animateItem(fadeInSpec = UiMotion.fade(), placementSpec = UiMotion.intOffset(),
                fadeOutSpec = UiMotion.fade()), onOpen = { vm.openAuthor(author.secUid) }) { unfollow = author }
        }
        if(authors.isNotEmpty()) item(key = "hint") {
            Text("长按作者可以取消关注。关注列表只保存在这台手机上。", style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp, start = 4.dp))
        }
    }
    unfollow?.let { author ->
        AlertDialog(onDismissRequest = { unfollow = null }, title = { Text("取消关注？") },
            text = { Text("不再检查「${author.nickname}」的新作品。") }, shape = RoundedCornerShape(20.dp),
            confirmButton = { Button(onClick = { vm.follows.unfollow(author.secUid); unfollow = null },
                modifier = Modifier.testTag("confirmUnfollow")) { Text("取消关注") } },
            dismissButton = { TextButton(onClick = { unfollow = null }) { Text("保留") } })
    }
}

@Composable private fun FollowRow(author: FollowedAuthor, modifier: Modifier, onOpen: () -> Unit, onLongPress: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    UiGroup(modifier = modifier.testTag("follow-${author.secUid}")) {
        Row(Modifier.fillMaxWidth().pressable(scale = 0.98f, onLongClick = onLongPress, onClick = onOpen)
            .padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(author.avatar, author.nickname, 50.dp)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(author.nickname, style = MaterialTheme.typography.titleMedium, maxLines = 1,
                        overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    if(author.newCount > 0) TagBadge("${author.newCount} 条新", strong = true)
                }
                Text(listOfNotNull(author.works.takeIf { it >= 0 }?.let { "作品 $it" },
                    "没有新作品".takeIf { author.newCount == 0 }, "检查于 ${checkedLabel(author.lastChecked)}")
                    .joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
            Glyph("chevron", tint = colors.onSurfaceVariant)
        }
    }
}

private fun checkedLabel(millis: Long): String {
    if(millis <= 0) return "—"
    val day = SimpleDateFormat("M 月 d 日", Locale.getDefault())
    val today = day.format(Date())
    val value = day.format(Date(millis))
    return if(value == today) SimpleDateFormat("今天 HH:mm", Locale.getDefault()).format(Date(millis)) else value
}
