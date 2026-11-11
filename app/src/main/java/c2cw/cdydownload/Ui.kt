package c2cw.cdydownload

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Card as MiuixCard

/** Solid rounded card used by every page. */
@Composable internal fun UiGroup(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit) {
    MiuixCard(modifier = modifier.fillMaxWidth().clip(CardShape)
        .then(if(onClick != null) Modifier.pressable(scale = 0.99f, onClick = onClick) else Modifier),
        cornerRadius = 14.dp, insideMargin = PaddingValues(0.dp), content = content)
}

private val paths = mapOf(
    "completed" to "M22,12 A10,10 0,1 1,2,12 A10,10 0,1 1,22,12 M7,12 L10.5,15.5 L17,8.5",
    "clock" to "M22,12 A10,10 0,1 1,2,12 A10,10 0,1 1,22,12 M12,6 L12,12 L16,14",
    "search" to "M17,10 A7,7 0,1 1,3,10 A7,7 0,1 1,17,10 M15,15 L21,21",
    "layers" to "M3,8 L12,3 L21,8 L12,13 Z M3,12 L12,17 L21,12 M3,16 L12,21 L21,16",
    "error" to "M22,12 A10,10 0,1 1,2,12 A10,10 0,1 1,22,12 M12,6 L12,13 M12,17 L12,18",
    "home" to "M3,10 L12,3 L21,10 L21,21 L15,21 L15,14 L9,14 L9,21 L3,21 Z",
    "download" to "M12,3 L12,15 M6,9 L12,15 L18,9 M4,16 L4,21 L20,21 L20,16",
    "link" to "M10,13 L14,9 M8,16 L6,18 C2,22 -2,16 2,12 L6,8 C8,6 11,6 13,8 M11,16 C13,18 16,18 18,16 L22,12 C26,8 20,2 16,6 L14,8",
    "close" to "M6,6 L18,18 M18,6 L6,18",
    "back" to "M15,5 L8,12 L15,19",
    "check" to "M5,12 L10,17 L20,7",
    "share" to "M8.5,10.5 L15.5,6.5 M8.5,13.5 L15.5,17.5 M9,12 A3,3 0,1 1,3,12 A3,3 0,1 1,9,12 M21,5 A3,3 0,1 1,15,5 A3,3 0,1 1,21,5 M21,19 A3,3 0,1 1,15,19 A3,3 0,1 1,21,19",
    "pause" to "M8,5 L8,19 M16,5 L16,19",
    "image" to "M3,3 L21,3 L21,21 L3,21 Z M3,17 L9,11 L14,16 L17,13 L21,17 M15,7 L15,8",
    "music" to "M10,17 L10,5 L20,3 L20,15 M10,9 L20,7 M10,17 C10,21 3,22 3,18 C3,15 10,14 10,17 M20,15 C20,19 13,20 13,16 C13,13 20,12 20,15",
    "play" to "M8,5 L19,12 L8,19 Z",
    "retry" to "M4,10 A8,8 0,1 1,5,18 M4,4 L4,10 L10,10",
    "more" to "M12,4 L12,5 M12,11 L12,12 M12,18 L12,19",
    "clipboard" to "M9,5 L5,5 L5,21 L19,21 L19,5 L15,5 M9,3 L15,3 L15,7 L9,7 Z M9,12 L15,12 M9,16 L13,16",
    "expand" to "M6,9 L12,15 L18,9",
    "arrow" to "M5,12 L19,12 M13,6 L19,12 L13,18",
    "chevron" to "M9,6 L15,12 L9,18",
    "star" to "M12,3.5 L14.6,8.9 L20.5,9.6 L16.1,13.6 L17.3,19.5 L12,16.5 L6.7,19.5 L7.9,13.6 L3.5,9.6 L9.4,8.9 Z",
    "refresh" to "M20,12 A8,8 0,1 1,17.7,6.3 M20,4 L20,9 L15,9",
    "palette" to "M12,3 A9,9 0,1 0,12,21 C13.5,21 14,20 13.4,18.9 C12.8,17.7 13.6,16.5 15,16.5 L17,16.5 C19.2,16.5 21,14.7 21,12.5 C21,7.3 17,3 12,3 Z M7.5,12 L7.5,12.5 M10,7.5 L10,8 M15,7.5 L15,8"
)
@Composable internal fun Glyph(name: String, description: String? = null, modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) {
    if(name == "settings" || name == "trash") {
        Icon(androidx.compose.ui.res.painterResource(if(name == "settings") R.drawable.ic_settings else R.drawable.ic_delete), contentDescription = description,
            modifier = modifier.size(22.dp), tint = tint)
        return
    }
    val vector = remember(name) {
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            addPath(PathParser().parsePathString(paths.getValue(name)).toNodes(),
                stroke = SolidColor(Color.Black), strokeLineWidth = 1.7f,
                strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Round,
                strokeLineJoin = androidx.compose.ui.graphics.StrokeJoin.Round)
        }.build()
    }
    Icon(vector, contentDescription = description, modifier = modifier.size(22.dp), tint = tint)
}

@Composable internal fun appGlassStyle(tint: Color = MaterialTheme.colorScheme.background) = HazeStyle(
    backgroundColor = MaterialTheme.colorScheme.background,
    tint = HazeTint(tint.copy(alpha = 0.46f)),
    blurRadius = 14.dp, noiseFactor = 0f
)

@Composable internal fun GlassPrompt(haze: HazeState, modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(18.dp), content: @Composable () -> Unit) {
    Box(modifier.shadow(4.dp, shape).clip(shape)) {
        // Blur only the backdrop. Drawing Surface elevation after haze creates a shadow inside the glass.
        Box(Modifier.matchParentSize().hazeEffect(haze, style = appGlassStyle()))
        content()
    }
}

@Composable fun DownloaderScreen(vm: MainViewModel, requestNotifications: () -> Unit, checkClipboard: () -> Unit) {
    AccountExpiryPrompt { vm.inputVisible = false; vm.settings = true }
    val tasks by vm.store.tasks.collectAsStateWithLifecycle()
    val paused by vm.store.paused.collectAsStateWithLifecycle()
    val follows by vm.follows.authors.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val haze = remember { HazeState() }
    val updateHaze = remember { HazeState() }
    val updateManager = (context.applicationContext as DownloaderApp).updates
    val updateState by updateManager.state.collectAsStateWithLifecycle()
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 64.dp
    val pager = rememberPagerState(initialPage = vm.tab) { TAB_COUNT }
    LaunchedEffect(vm.tabRequest) {
        if(vm.tabRequest != vm.consumedTabRequest) {
            vm.consumedTabRequest = vm.tabRequest
            val distance = kotlin.math.abs(vm.requestedTab - pager.currentPage)
            // Far jumps would scroll through every page in between; land directly instead.
            if(distance > 1) pager.scrollToPage(vm.requestedTab)
            else pager.animateScrollToPage(vm.requestedTab, animationSpec = UiMotion.snappy())
        }
    }
    LaunchedEffect(pager) {
        snapshotFlow { if(pager.isScrollInProgress) null else pager.settledPage }
            .collect { page -> if(page != null) vm.onPageSettled(page) }
    }
    BackHandler(vm.author.visible || pager.currentPage != TAB_HOME) {
        if(vm.author.visible) vm.author.close() else vm.tab = TAB_HOME
    }
    LaunchedEffect(vm.notice) {
        vm.notice?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show(); vm.notice = null }
    }
    val page = pager.targetPage
    Surface(Modifier.fillMaxSize().then(if(updateState.dialog) Modifier.hazeSource(updateHaze) else Modifier),
        color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))) {
            Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Bottom))
                .hazeSource(haze)) {
                HorizontalPager(
                    state = pager, modifier = Modifier.fillMaxSize().testTag("pages"),
                    beyondViewportPageCount = 1, key = { it },
                    flingBehavior = PagerDefaults.flingBehavior(state = pager, snapPositionalThreshold = 0.3f,
                        snapAnimationSpec = UiMotion.settle())
                ) { index ->
                    when(index) {
                        TAB_HOME -> HomePage(vm, topInset) { if(vm.submit()) requestNotifications() }
                        TAB_FOLLOW -> FollowPage(vm, topInset)
                        TAB_DOWNLOADS -> DownloadsPage(tasks, vm, requestNotifications, topInset)
                        else -> SettingsPage(vm, topInset) { enabled -> vm.setClipboard(enabled); if(enabled) checkClipboard() }
                    }
                }
            }
            // Solid header: no blur pass over the scrolling pages, so scrolling stays smooth.
            Box(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).statusBarsPadding()) {
                Row(Modifier.fillMaxWidth().height(64.dp).padding(start = 20.dp, end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    AnimatedContent(page, transitionSpec = { fadeIn(UiMotion.fade()) togetherWith fadeOut(UiMotion.fade(100)) },
                        modifier = Modifier.weight(1f), label = "title") { current ->
                        Text(when(current) { TAB_HOME -> "下载"; TAB_FOLLOW -> "关注"; TAB_DOWNLOADS -> "下载记录"; else -> "设置" },
                            style = MaterialTheme.typography.headlineMedium)
                    }
                    if(page == TAB_FOLLOW && follows.isNotEmpty()) {
                        Button(onClick = { vm.followUpdates.check() }, enabled = !vm.followUpdates.running,
                            shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(horizontal = 14.dp),
                            modifier = Modifier.height(40.dp).testTag("checkUpdates")) {
                            Text(if(vm.followUpdates.running) "检查中…" else "检查更新")
                        }
                    }
                    if(page == TAB_DOWNLOADS) {
                        if(tasks.any { it.status.pending }) {
                            IconButton(onClick = {
                                if(paused) { vm.resumeDownloads(); requestNotifications() } else vm.pauseDownloads()
                            }, enabled = !vm.deleting, modifier = Modifier.testTag("queueControl"),
                                colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary)) {
                                Glyph(if(paused) "play" else "pause", if(paused) "全部开始" else "全部暂停")
                            }
                        }
                        if(tasks.isNotEmpty()) IconButton(onClick = { vm.requestDelete(tasks.map { it.id }, all = true) },
                            enabled = !vm.deleting, modifier = Modifier.testTag("clearDownloads")) {
                            Glyph("trash", "清空下载记录")
                        }
                    }
                }
            }
            val typing = WindowInsets.ime.asPaddingValues().calculateBottomPadding() > 0.dp
            AnimatedVisibility(!typing, Modifier.align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Bottom)).padding(bottom = 16.dp),
                enter = fadeIn(UiMotion.fade()), exit = fadeOut(UiMotion.fade(100))) {
                GlassBottomBar(page, haze, badge = follows.sumOf { it.newCount }) { vm.tab = it }
            }
            AnimatedVisibility(vm.author.visible, enter = UiMotion.screenEnter, exit = UiMotion.screenExit) {
                AuthorPage(vm)
            }
        }
    }
    DeleteTasksDialog(vm)
    SpecificationDialog(vm.specificationEditor)
    c2cw.cdydownload.update.UpdateOverlay(updateManager, updateHaze)
}

@Composable private fun DownloadsPage(tasks: List<DownloadTask>, vm: MainViewModel, requestNotifications: () -> Unit, topInset: Dp) {
    var filter by rememberSaveable { mutableIntStateOf(0) }
    val listState = rememberLazyListState()
    val revealTaskId = vm.revealTaskId
    LaunchedEffect(revealTaskId) {
        if(revealTaskId != null) {
            filter = 0
            withFrameNanos { }
            listState.scrollToItem(0)
            vm.taskRevealed(revealTaskId)
        }
    }
    val filtered = when(filter) {
        1 -> tasks.filter { it.status.pending }
        2 -> tasks.filter { it.status == TaskStatus.COMPLETED }
        else -> tasks
    }
    LazyColumn(state = listState, modifier = Modifier.fillMaxSize().testTag("downloadList"),
        contentPadding = PaddingValues(top = topInset, bottom = BottomBarSpace + 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item(key = "filters") { Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("全部", "进行中", "已完成").forEachIndexed { index, title ->
                FilterChip(selected = filter == index, onClick = { filter = index }, label = { Text(title) }, shape = RoundedCornerShape(10.dp))
            }
        }
        }
        if(filtered.isEmpty()) { item(key = "empty") {
            Column(Modifier.fillParentMaxHeight(0.65f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center) {
                Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
                    Box(Modifier.size(76.dp), contentAlignment = Alignment.Center) {
                        Glyph("download", modifier = Modifier.size(30.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(20.dp))
                Text(when(filter) { 1 -> "没有正在下载的作品"; 2 -> "还没有下载完成的作品"; else -> "下载的作品会出现在这里" },
                    style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text("从 B 站、抖音、小红书、YouTube、微博或微信视频号分享作品开始", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = { vm.openInput() }) { Text("添加链接") }
            }
        } } else items(filtered, key = { it.id }) { task ->
            Box(Modifier.padding(horizontal = 16.dp).animateItem(fadeInSpec = UiMotion.fade(), placementSpec = UiMotion.intOffset(),
                fadeOutSpec = UiMotion.fade())) { DownloadCard(task, vm, requestNotifications) }
        }
    }
}
@Composable private fun DownloadCard(task: DownloadTask, vm: MainViewModel, requestNotifications: () -> Unit) {
    val context = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    LaunchedEffect(task.uri, task.status) {
        if(task.status == TaskStatus.COMPLETED && task.uri.isNotBlank()) {
            val measured = withContext(Dispatchers.IO) { savedResolution(context, task) }
            val fps = withContext(Dispatchers.IO) { if(task.mimeType.startsWith("video/")) savedFrameRate(context,task.uri) else 0f }
            if((measured.isNotEmpty() && measured != task.resolution) || (fps > 0 && fps != task.fps)) vm.store.update(task.id) {
                if(it.uri == task.uri && it.status == TaskStatus.COMPLETED) it.copy(resolution = measured.ifBlank { it.resolution },fps=fps.takeIf { it > 0 } ?: it.fps) else it
            }
        }
    }
    UiGroup(modifier = Modifier.fillMaxWidth().testTag("download-${task.id}")) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PlatformIcon(task.platform)
                        Text(if(task.quality.contains("张图片")) task.quality else "",
                            modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                        Text(when(task.status) {
                                TaskStatus.DOWNLOADING -> "${formatBytes(task.bytes)} / ${if(task.total > 0) formatBytes(task.total) else "未知"}"
                                TaskStatus.COMPLETED -> formatBytes(task.bytes)
                                else -> task.status.label
                            },
                            maxLines = 1, style = MaterialTheme.typography.labelMedium,
                            fontFamily = if(task.status in setOf(TaskStatus.DOWNLOADING, TaskStatus.COMPLETED)) FontFamily.Monospace else FontFamily.Default,
                            color = if(task.status == TaskStatus.FAILED || task.status == TaskStatus.INTERRUPTED)
                                MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(task.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Box {
                    IconButton(onClick = { menu = true }, enabled = !vm.deleting, modifier = Modifier.size(36.dp)) { Glyph("more", "更多操作") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("查看原作品") }, onClick = {
                            menu = false
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(task.source))) }
                                .onFailure { vm.notice = "没有可以打开链接的应用" }
                        })
                        DropdownMenuItem(text = { Text("删除任务") }, onClick = {
                            menu = false; vm.requestDelete(listOf(task.id), withFiles = false)
                        })
                        DropdownMenuItem(text = { Text("删除任务和文件") }, onClick = {
                            menu = false; vm.requestDelete(listOf(task.id), withFiles = true)
                        })
                    }
                }
            }
            if(task.status.pending) {
                if(task.status == TaskStatus.PAUSED || (task.total > 0 && task.status == TaskStatus.DOWNLOADING)) {
                    AppProgressBar(progress = { task.progress }, modifier = Modifier.fillMaxWidth().height(3.dp))
                } else AppProgressBar(modifier = Modifier.fillMaxWidth().height(3.dp))
            }
            if(task.error.isNotEmpty()) Text(task.error, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            if(task.status == TaskStatus.COMPLETED && task.mimeType.startsWith("image/")) {
                SavedOutputButtons(task) { vm.notice = it }
            } else Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    val isVideo = !task.mimeType.startsWith("image/") && !task.quality.contains("张图片")
                    val pickable = isVideo || task.quality.contains("张图片") || task.albumContent != AlbumContent.ALL
                    Row(Modifier.then(if(pickable) Modifier.clip(RoundedCornerShape(6.dp)).clickable(enabled=!vm.deleting) {
                        vm.specificationEditor.open(task.id)
                    }.testTag("specification-${task.id}") else Modifier).padding(vertical=4.dp),verticalAlignment=Alignment.CenterVertically) {
                        Text(if(isVideo) videoSpecification(task.resolution.ifBlank { task.quality.takeIf { it.matches(Regex("[0-9]+P")) }.orEmpty() },task.fps) else task.quality.substringAfter(" · ",task.resolution.ifBlank { "—" }),
                            style=MaterialTheme.typography.labelLarge,maxLines=1,overflow=TextOverflow.Ellipsis,
                            modifier=Modifier.weight(1f,fill=false),color=MaterialTheme.colorScheme.onSurfaceVariant)
                        if(pickable) Glyph("expand",modifier=Modifier.padding(start=4.dp).size(14.dp),tint=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if(task.status == TaskStatus.DOWNLOADING) {
                        Text(buildAnnotatedString {
                            withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                                append("${formatBytes(task.speed)}/s")
                            }
                            if(task.total > 0) append(" · ${(task.progress*100).toInt()}%")
                        },
                            style = MaterialTheme.typography.labelLarge, fontFamily = FontFamily.Monospace,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if(task.status.pending) {
                    TextButton(onClick = { vm.cancel(task.id) }, enabled = !vm.deleting) { Text("取消") }
                } else if(task.status == TaskStatus.COMPLETED) {
                    SavedOutputButtons(task) { vm.notice = it }
                } else {
                    TextButton(onClick = { vm.retry(task.id); requestNotifications() }, enabled = !vm.deleting) { Text("重试") }
                }
            }
        }
    }
}
@Composable private fun DeleteTasksDialog(vm: MainViewModel) {
    val request=vm.deleteRequest ?: return
    val count=request.ids.size
    if(request.withFiles == true) {
        DestructiveFileConfirmation("删除任务和文件？","将删除 $count 个任务，以及这些任务保存的所有视频、图片和音频。文件删除后无法恢复。未完成的下载会停止，临时文件会清理。",
            "确认删除文件",vm.deleting,vm::dismissDeletion,{ vm.confirmDeletion(true) },"confirmDeleteTasks")
        return
    }
    val choice=request.withFiles == null
    ConfirmationDialogFrame(if(choice) "清空下载记录" else "删除任务？",vm::dismissDeletion,content={
        Text(if(choice) "将清空 $count 个下载任务，请选择是否保留已下载文件。"
            else "将删除 $count 个任务，已保存的视频、图片和音频会保留。")
        Spacer(Modifier.height(8.dp))
        Text("未完成的下载会停止，临时文件会清理。",style=MaterialTheme.typography.bodyMedium,
            color=MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        if(choice) {
            OutlinedButton(onClick={ vm.confirmDeletion(false) },enabled=!vm.deleting,
                modifier=Modifier.fillMaxWidth()) { Text("仅删除任务") }
            OutlinedButton(onClick=vm::chooseDeleteFiles,enabled=!vm.deleting,
                modifier=Modifier.fillMaxWidth()) { Text("删除任务和文件") }
        }
        if(vm.deleting) AppProgressBar(Modifier.fillMaxWidth())
    },actions={
        TextButton(onClick=vm::dismissDeletion,enabled=!vm.deleting) { Text("取消") }
        if(!choice) Button(onClick={ vm.confirmDeletion(false) },enabled=!vm.deleting,
            modifier=Modifier.testTag("confirmDeleteTasks")) { Text("删除任务") }
    })
}

