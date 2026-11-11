package c2cw.cdydownload

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Button as MiuixButton
import top.yukonga.miuix.kmp.basic.ButtonDefaults as MiuixButtonDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 首页: paste a link, see the work, choose what to keep, download. */
@Composable internal fun HomePage(vm: MainViewModel, topInset: Dp, submit: () -> Unit) {
    val typing = WindowInsets.ime.asPaddingValues().calculateBottomPadding() > 0.dp
    val detected = remember(vm.input) { Links.detect(vm.input) }
    val preview = vm.preview
    val showAction = preview != null || (detected != null && vm.qualityLoading)
    Box(Modifier.fillMaxSize().testTag("homePage")) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = topInset)
            .padding(horizontal = 16.dp)
            .padding(bottom = if(typing) 12.dp else BottomBarSpace + if(showAction) 70.dp else 8.dp).imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Column(Modifier.widthIn(max = 560.dp).fillMaxWidth().padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if(!vm.inputVisible) ClipboardSuggestionCard(vm)
                LinkInputCard(vm, detected)
                preview?.let { WorkResult(it, vm) }
                vm.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
            }
        }
        AnimatedVisibility(showAction && !typing, Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(tween(UiMotion.Medium, easing = UiMotion.Emphasized)) { it / 2 } + fadeIn(UiMotion.fade()),
            exit = slideOutVertically(tween(UiMotion.Fast)) { it / 2 } + fadeOut(tween(UiMotion.Fast))) {
            Box(Modifier.widthIn(max = 560.dp).fillMaxWidth().padding(horizontal = 16.dp)
                .padding(bottom = BottomBarSpace + 4.dp)) {
                DownloadAction(vm, detected, submit)
            }
        }
    }
}

@Composable private fun DownloadAction(vm: MainViewModel, detected: VideoLink?, submit: () -> Unit) {
    val preview = vm.preview
    val albumReady = preview?.isAlbum != true ||
        if(vm.albumContent == AlbumContent.MUSIC) preview.hasMusic else vm.selectedAlbumImages.isNotEmpty()
    val canDownload = vm.isResolved(detected) && !vm.qualityLoading && albumReady
    MiuixButton(onClick = submit, enabled = canDownload,
        colors = MiuixButtonDefaults.buttonColorsPrimary(), cornerRadius = 14.dp, minHeight = 52.dp,
        modifier = Modifier.fillMaxWidth().testTag("resolveThenDownload")) {
        Text(when {
            vm.qualityLoading -> "正在解析…"
            preview?.isAlbum == true -> albumDownloadLabel(preview, vm.albumContent, vm.selectedAlbumImages.sorted(), vm.albumOutputs)
            else -> "开始下载"
        }, fontWeight = FontWeight.SemiBold, color = if(canDownload)
            MiuixTheme.colorScheme.onPrimary else MiuixTheme.colorScheme.disabledOnPrimaryButton)
    }
}

/** "下载选中（6 张，其中 4 张存实况）" */
internal fun albumDownloadLabel(preview: MediaPreview, content: AlbumContent, indices: List<Int>, outputs: Map<Int, AlbumOutput>): String {
    if(content == AlbumContent.MUSIC) return "只下载配乐"
    val live = indices.count { albumOutput(it, preview.imageVideos, outputs) == AlbumOutput.MOTION }
    val video = indices.count { albumOutput(it, preview.imageVideos, outputs) == AlbumOutput.VIDEO }
    val parts = buildString {
        append("${indices.size} 张")
        if(live > 0) append("，其中 $live 张存实况")
        if(video > 0) append("，$video 段视频")
    }
    return "下载选中（$parts）" + if(content == AlbumContent.ALL && preview.hasMusic) " + 配乐" else ""
}

@Composable private fun ClipboardSuggestionCard(vm: MainViewModel) {
    val prompt = vm.clipboardPrompt?.takeIf { vm.clipboardEnabled } ?: return
    UiGroup(modifier = Modifier.testTag("inlineClipboardSuggestion")) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PlatformIcon(prompt.link.platform)
                Text("发现作品链接", style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f).padding(start = 10.dp))
                IconButton(onClick = vm::dismissClipboard, modifier = Modifier.size(32.dp)) { Glyph("close", "忽略此提示") }
            }
            when(prompt) {
                is ClipboardPrompt.Resolving -> {
                    AppProgressBar(Modifier.fillMaxWidth())
                    Text("正在解析链接，请稍候…", style = MaterialTheme.typography.bodyMedium)
                }
                is ClipboardPrompt.Ready -> Text(prompt.info.title, maxLines = 2,
                    overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                is ClipboardPrompt.Failed -> Text("解析失败，请检查网络后重试。",
                    color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if(prompt is ClipboardPrompt.Failed) TextButton(onClick = vm::retryClipboard) { Text("重试") }
                TextButton(onClick = { vm.openInput(prompt.link.url) },
                    modifier = Modifier.testTag("openClipboardPreview")) { Text("查看并选择") }
            }
        }
    }
}

@Composable private fun LinkInputCard(vm: MainViewModel, detected: VideoLink?) {
    val context = LocalContext.current
    val authorLink = remember(vm.input) { DouyinUserLinks.detect(vm.input) }
    LaunchedEffect(vm.input) { if(!vm.routeAuthorInput(vm.input)) vm.prepareQuality(detected) }
    val focus = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }
    var candidate by remember { mutableStateOf<String?>(null) }
    var inspected by remember { mutableStateOf<ClipboardContent?>(null) }
    val windowFocused = LocalWindowInfo.current.isWindowFocused
    LaunchedEffect(focused, windowFocused, vm.clipboardEnabled) {
        if(!windowFocused || !vm.clipboardEnabled) return@LaunchedEffect
        val content = readClipboardContent(context, excludeSensitive = true) ?: return@LaunchedEffect
        if(content == inspected) return@LaunchedEffect
        inspected = content
        val text = content.text
        val link = Links.detect(text)
        candidate = text.takeIf { (link != null && link.key != Links.detect(vm.input)?.key) ||
            (link == null && DouyinUserLinks.detect(text) != null && text != vm.input) }
    }
    LaunchedEffect(vm.clipboardEnabled) { if(!vm.clipboardEnabled) candidate = null }
    val colors = MaterialTheme.colorScheme
    UiGroup {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(value = vm.input, onValueChange = { vm.input = it.take(16000); vm.error = null; candidate = null },
                modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp, max = 168.dp)
                    .testTag("downloadLinkInput").focusRequester(focus).onFocusChanged {
                        focused = it.isFocused
                        if(it.isFocused) vm.inputVisible = true
                    },
                placeholder = { Text("粘贴作品分享文案或链接，也支持抖音作者主页链接", color = colors.onSurfaceVariant) },
                shape = RoundedCornerShape(12.dp), textStyle = MaterialTheme.typography.bodyMedium,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = colors.surfaceContainer, focusedContainerColor = colors.surfaceContainer,
                    unfocusedBorderColor = Color.Transparent, focusedBorderColor = colors.primary.copy(alpha = 0.6f),
                    errorContainerColor = colors.surfaceContainer),
                isError = vm.error != null, maxLines = 6)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    vm.input = candidate ?: readClipboardText(context, excludeSensitive = false).orEmpty()
                    vm.error = null; candidate = null
                }, shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(horizontal = 14.dp),
                    modifier = Modifier.height(44.dp).testTag("fillClipboardLink")) { Text(if(candidate != null) "填入" else "粘贴") }
                Button(onClick = {
                    when {
                        authorLink != null -> vm.openAuthor(authorLink)
                        detected == null -> vm.error = if(vm.input.isBlank()) "请先粘贴链接" else "没有识别到支持的作品链接"
                        !vm.isResolved(detected) && !vm.qualityLoading -> vm.retryResolve(detected)
                    }
                }, shape = RoundedCornerShape(12.dp), enabled = !vm.qualityLoading,
                    modifier = Modifier.weight(1f).height(44.dp).testTag("startResolve")) {
                    Text(when {
                        vm.qualityLoading -> "正在解析…"
                        authorLink != null -> "打开作者主页"
                        else -> "开始解析"
                    }, fontWeight = FontWeight.SemiBold)
                }
                TextButton(onClick = {
                    vm.input = ""; vm.error = null; candidate = null; focus.requestFocus()
                }, enabled = vm.input.isNotEmpty(), modifier = Modifier.height(44.dp).testTag("clearLinkInput")) { Text("清空") }
            }
            val status = when {
                candidate != null -> "剪贴板中有${candidate?.let(Links::detect)?.platform?.label ?: "抖音作者"}链接，点「填入」使用"
                authorLink != null -> "已识别：抖音作者主页"
                detected != null -> "已识别：${detected.platform.label}"
                else -> null
            }
            if(status != null) Text(status, style = MaterialTheme.typography.bodySmall,
                color = if(detected != null || authorLink != null) colors.primary else colors.onSurfaceVariant)
            if(detected != null && vm.qualityLoading) AppProgressBar(Modifier.fillMaxWidth().testTag("inlineResolveProgress"))
            vm.resolveError?.let { message ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(message, color = colors.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    TextButton(onClick = { vm.retryResolve(detected) }, modifier = Modifier.testTag("retryResolve")) { Text("重试") }
                }
            }
        }
    }
}

@Composable private fun WorkResult(preview: MediaPreview, vm: MainViewModel) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().testTag("previewCard"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if(preview.cover.isNotBlank()) Box(Modifier.fillMaxWidth().height(210.dp).clip(CardShape)
            .background(colors.surfaceContainer)) {
            RemoteImage(preview.cover, Modifier.fillMaxSize(), referer = preview.referer, userAgent = preview.userAgent)
            ImageBadge(preview.platform.label, Modifier.align(Alignment.TopStart).padding(10.dp))
        }
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(preview.title, style = MaterialTheme.typography.titleMedium, maxLines = 3,
                overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
            TagBadge("无水印", Modifier.padding(top = 2.dp))
        }
        if(preview.author.isNotBlank()) {
            val canOpen = preview.platform == Platform.DOUYIN && preview.authorSecUid.isNotBlank()
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                .then(if(canOpen) Modifier.clickable { vm.openAuthor(preview.authorSecUid) }.testTag("openAuthor") else Modifier)
                .padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Avatar(preview.authorAvatar, preview.author, 30.dp)
                Text("@${preview.author}", style = MaterialTheme.typography.titleSmall, maxLines = 1,
                    overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(start = 10.dp))
                if(canOpen) Text("全部作品 ›", style = MaterialTheme.typography.labelLarge, color = colors.primary)
            }
        }
        val stats = listOfNotNull(
            engagement("点赞", preview.likes), engagement("评论", preview.comments), engagement("分享", preview.shares),
            preview.publishedAt.takeIf { it > 0 }?.let { "发布于 ${previewDate(it)}" },
            preview.quality.takeIf { it.isNotBlank() && !preview.isAlbum }
        )
        if(stats.isNotEmpty()) Text(stats.joinToString("  ·  "), style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant)
        if(preview.isAlbum) AlbumSection(preview, vm) else VideoOptions(vm)
    }
}

@Composable private fun VideoOptions(vm: MainViewModel) {
    UiGroup {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("下载模式", style = MaterialTheme.typography.titleSmall)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VideoDownloadMode.entries.forEach { mode ->
                    FilterChip(selected = vm.videoMode == mode, onClick = { vm.videoMode = mode },
                        label = { Text(mode.label) }, shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("videoMode-${mode.name}"))
                }
            }
        }
    }
    vm.qualityCatalog?.let { catalog ->
        InlineQualityPicker(catalog, vm.qualityChosen, Links.detect(vm.input)?.key.orEmpty()) { vm.qualityChosen = it }
    }
}

@Composable private fun AlbumSection(preview: MediaPreview, vm: MainViewModel) {
    val colors = MaterialTheme.colorScheme
    val liveIndices = preview.imageVideos.indices.filter { preview.imageVideos[it] != null }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("图集", style = MaterialTheme.typography.titleMedium)
        Text("（${preview.images.size}）" + if(liveIndices.isNotEmpty()) " · 含 ${liveIndices.size} 份实况" else "",
            style = MaterialTheme.typography.titleSmall, color = colors.primary)
    }
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AlbumContent.entries.filter { it != AlbumContent.MUSIC || preview.hasMusic }.forEach { content ->
            FilterChip(selected = vm.albumContent == content, onClick = { vm.albumContent = content },
                label = { Text(when(content) { AlbumContent.ALL -> if(preview.hasMusic) "图片和配乐" else "下载选中"
                    AlbumContent.IMAGES -> "只要图片"; AlbumContent.MUSIC -> "只要配乐" }) },
                shape = RoundedCornerShape(10.dp), modifier = Modifier.testTag("album-${content.name}"))
        }
    }
    if(vm.albumContent == AlbumContent.MUSIC) return
    if(liveIndices.isNotEmpty()) {
        val outputs = liveIndices.map { albumOutput(it, preview.imageVideos, vm.albumOutputs) }.toSet()
        SegmentedControl(listOf("全部存实况", "全部存静图"), when(outputs) {
            setOf(AlbumOutput.MOTION) -> 0; setOf(AlbumOutput.STATIC) -> 1; else -> -1
        }, Modifier.fillMaxWidth(), tagPrefix = "albumAll") {
            vm.chooseAllAlbumOutputs(if(it == 0) AlbumOutput.MOTION else AlbumOutput.STATIC)
        }
        Text("原图、无水印。实况保存为相册里的实况照片（动图），不会转成视频；也可以点每张左下角单独切换。",
            style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("已选 ${vm.selectedAlbumImages.size} / ${preview.images.size}", style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f))
        TextButton(onClick = vm::selectAllAlbumImages, modifier = Modifier.testTag("albumSelectAll")) { Text("全选") }
        TextButton(onClick = vm::clearAlbumImageSelection, modifier = Modifier.testTag("albumSelectNone")) { Text("清空") }
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.testTag("albumGrid")) {
        preview.images.indices.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { index -> AlbumTile(preview, index, vm, Modifier.weight(1f)) }
                if(row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
    if(vm.selectedAlbumImages.isEmpty())
        Text("至少选择一项内容才能下载", color = colors.error, style = MaterialTheme.typography.bodySmall)
}

@Composable private fun AlbumTile(preview: MediaPreview, index: Int, vm: MainViewModel, modifier: Modifier) {
    val checked = index in vm.selectedAlbumImages
    val live = preview.imageVideos.getOrNull(index) != null
    Box(modifier.aspectRatio(0.75f).clip(TileShape).background(MaterialTheme.colorScheme.surfaceContainer)
        .pressable(scale = 0.97f) { vm.toggleAlbumImage(index) }.testTag("albumImage-$index")) {
        RemoteImage(preview.images[index], Modifier.fillMaxSize().alpha(if(checked) 1f else 0.55f),
            referer = preview.referer, userAgent = preview.userAgent)
        SelectMark(checked, Modifier.align(Alignment.TopStart).padding(8.dp).testTag("albumImageCheck-$index"))
        ImageBadge(if(live) "实况" else "${index + 1}", Modifier.align(Alignment.TopEnd).padding(8.dp))
        if(live) {
            val output = albumOutput(index, preview.imageVideos, vm.albumOutputs)
            MiniToggle(listOf("实况", "静图"), when(output) { AlbumOutput.MOTION -> 0; AlbumOutput.STATIC -> 1; else -> -1 },
                Modifier.align(Alignment.BottomStart).padding(8.dp),
                tags = listOf("albumOutput-$index-MOTION", "albumOutput-$index-STATIC")) {
                vm.chooseAlbumOutput(index, if(it == 0) AlbumOutput.MOTION else AlbumOutput.STATIC)
            }
        }
    }
}

private fun engagement(label: String, count: Int): String? = when {
    count < 0 -> null
    else -> "$label ${compactCount(count.toLong())}"
}

private fun previewDate(millis: Long) = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(millis))
