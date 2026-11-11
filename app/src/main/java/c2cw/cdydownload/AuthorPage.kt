package c2cw.cdydownload

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Douyin author homepage: profile, follow toggle and a selectable two-column work grid. */
@Composable internal fun AuthorPage(vm: MainViewModel) {
    val state = vm.author
    val followed by vm.follows.authors.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme
    val grid = rememberLazyGridState()
    val nearEnd by remember { derivedStateOf {
        val last = grid.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        last >= grid.layoutInfo.totalItemsCount - 4
    } }
    LaunchedEffect(nearEnd, state.works.size, state.hasMore) { if(nearEnd && state.hasMore) state.loadMore() }
    Surface(Modifier.fillMaxSize().testTag("authorPage"), color = colors.background) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top + WindowInsetsSides.Bottom))) {
            Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = state::close, modifier = Modifier.testTag("authorBack")) { Glyph("back", "返回") }
                Text("作者", style = MaterialTheme.typography.titleLarge)
            }
            val profile = state.profile
            when {
                state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        AppProgressSpinner(Modifier.size(32.dp))
                        Text("正在打开作者主页…", color = colors.onSurfaceVariant)
                    }
                }
                profile == null -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(state.error ?: "作者主页加载失败", color = colors.error, style = MaterialTheme.typography.bodyMedium)
                        Button(onClick = state::retry, shape = RoundedCornerShape(12.dp)) { Text("重试") }
                    }
                }
                else -> LazyVerticalGrid(GridCells.Fixed(2), state = grid, modifier = Modifier.fillMaxSize().testTag("authorWorks"),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item(span = { GridItemSpan(2) }, key = "header") {
                        AuthorHeader(profile, state.isFollowed(followed), state::toggleFollow)
                    }
                    item(span = { GridItemSpan(2) }, key = "actions") {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("已加载 ${state.works.size} 条" + (if(profile.works >= 0) " / 共 ${profile.works} 条" else ""), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant,
                                    modifier = Modifier.weight(1f))
                                Text("已选 ${state.selected.size}", style = MaterialTheme.typography.bodyMedium,
                                    color = colors.primary, modifier = Modifier.padding(end = 6.dp))
                                OutlinedButton(onClick = state::selectAll, shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp), modifier = Modifier.height(34.dp)) { Text("全选") }
                                Spacer(Modifier.width(6.dp))
                                OutlinedButton(onClick = state::clearSelection, shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp), modifier = Modifier.height(34.dp)) { Text("取消选择") }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(onClick = state::downloadSelected, enabled = state.selected.isNotEmpty(),
                                    shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f).height(46.dp)
                                        .testTag("authorDownloadSelected")) { Text("下载选中", fontWeight = FontWeight.SemiBold) }
                                FilledTonalButton(onClick = state::downloadAll, enabled = !state.downloadingAll && state.works.isNotEmpty(),
                                    shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f).height(46.dp)
                                        .testTag("authorDownloadAll")) {
                                    Text(if(state.downloadingAll) "正在加载全部…" else "下载全部", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                    items(state.works, key = { it.id }) { work ->
                        WorkTile(work, work.id in state.selected, work.id in state.freshIds,
                            Modifier.animateItem(fadeInSpec = UiMotion.fade(), placementSpec = UiMotion.intOffset(), fadeOutSpec = null),
                            onToggle = { state.toggle(work.id) }) { vm.openWork(work) }
                    }
                    item(span = { GridItemSpan(2) }, key = "footer") {
                        Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                            when {
                                state.loadingMore -> AppProgressSpinner()
                                state.worksError != null -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(state.worksError.orEmpty(), color = colors.error, style = MaterialTheme.typography.bodySmall)
                                    TextButton(onClick = state::loadMore) { Text("重试") }
                                }
                                state.hasMore -> TextButton(onClick = state::loadMore) { Text("加载更多") }
                                state.works.isEmpty() -> Text("没有公开作品", color = colors.onSurfaceVariant)
                                else -> Text("没有更多了", color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable private fun AuthorHeader(profile: DouyinAuthor, followed: Boolean, onFollow: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.padding(top = 4.dp, bottom = 6.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(profile.avatar, profile.nickname, 76.dp)
            Column(Modifier.padding(start = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(profile.nickname, style = MaterialTheme.typography.headlineSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if(profile.douyinId.isNotBlank()) Text("抖音号 ${profile.douyinId}", style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant)
                if(profile.signature.isNotBlank()) Text(profile.signature, style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            listOf("粉丝" to profile.followers, "获赞" to profile.likes, "作品" to profile.works.toLong(),
                "关注" to profile.following.toLong()).forEach { (label, value) ->
                if(value >= 0) Row(verticalAlignment = Alignment.Bottom) {
                    Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    Text(" ${compactCount(value)}", style = MaterialTheme.typography.titleSmall)
                }
            }
        }
        if(followed) FilledTonalButton(onClick = onFollow, shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(46.dp).testTag("followToggle")) { Text("已关注") }
        else OutlinedButton(onClick = onFollow, shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.primary),
            modifier = Modifier.fillMaxWidth().height(46.dp).testTag("followToggle")) {
            Glyph("star", modifier = Modifier.size(18.dp), tint = colors.primary)
            Text("  关注此人", color = colors.primary, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable private fun WorkTile(work: DouyinWork, checked: Boolean, fresh: Boolean, modifier: Modifier,
    onToggle: () -> Unit, onOpen: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(modifier.clip(CardShape).background(colors.surface).pressable(onLongClick = onToggle, onClick = onOpen)
        .testTag("work-${work.id}")) {
        Box(Modifier.fillMaxWidth().aspectRatio(0.82f).background(colors.surfaceContainer)) {
            if(work.cover.isNotBlank()) RemoteImage(work.cover, Modifier.fillMaxSize(),
                referer = "https://www.douyin.com/", userAgent = VideoResolver.MOBILE)
            Box(Modifier.align(Alignment.TopStart).clip(RoundedCornerShape(8.dp))
                .pressable(scale = 0.9f, onClick = onToggle).padding(8.dp).testTag("workCheck-${work.id}")) {
                SelectMark(checked)
            }
            Row(Modifier.align(Alignment.TopEnd).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if(fresh) TagBadge("新", strong = true)
                if(work.pinned) ImageBadge("置顶")
                ImageBadge(when {
                    work.isAlbum && work.liveCount > 0 -> "图集 ${work.imageCount} · 实况"
                    work.isAlbum -> "图集 ${work.imageCount}"
                    else -> "视频"
                })
            }
        }
        Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(work.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.heightIn(min = 38.dp))
            Text(if(work.likes >= 0) "${compactCount(work.likes)} 赞" else " ", style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant)
        }
    }
}
