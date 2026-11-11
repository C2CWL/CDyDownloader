package c2cw.cdydownload

import dev.chrisbanes.haze.HazeProgressive
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.animation.core.FastOutSlowInEasing
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.hazeEffect
import androidx.compose.ui.unit.Dp
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.spring
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import top.yukonga.miuix.kmp.basic.Button as MiuixButton
import top.yukonga.miuix.kmp.basic.ButtonDefaults as MiuixButtonDefaults
import top.yukonga.miuix.kmp.basic.Card as MiuixCard
import top.yukonga.miuix.kmp.basic.Switch as MiuixSwitch
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val Day = lightColorScheme(
    primary = Color(0xFFFF6900), onPrimary = Color.White, primaryContainer = Color(0xFFFFEBDD),
    onPrimaryContainer = Color(0xFF9D3D00), background = Color(0xFFF5F5F5),
    surface = Color.White, surfaceContainer = Color(0xFFEEEEEE),
    onSurface = Color(0xFF1D1D1F), onSurfaceVariant = Color(0xFF666666),
    outlineVariant = Color(0xFFDADADA), error = Color(0xFFB93840)
)
private val Night = darkColorScheme(
    primary = Color(0xFFFF9A52), onPrimary = Color(0xFF351500), primaryContainer = Color(0xFF653000),
    onPrimaryContainer = Color(0xFFFFE0CC), background = Color(0xFF111111),
    surface = Color(0xFF1C1C1C), surfaceContainer = Color(0xFF252525),
    onSurface = Color(0xFFF1F1F1), onSurfaceVariant = Color(0xFFB0B0B0),
    outlineVariant = Color(0xFF393939), error = Color(0xFFFFB3B8)
)
@Composable fun DownloaderTheme(content: @Composable () -> Unit) {
    val colors = if(isSystemInDarkTheme()) Night else Day
    val miuixColors = if(isSystemInDarkTheme()) top.yukonga.miuix.kmp.theme.darkColorScheme(
        primary = colors.primary, onPrimary = colors.onPrimary, primaryVariant = colors.primary,
        primaryContainer = colors.primary, onPrimaryContainer = colors.onPrimary,
        disabledPrimary = colors.primaryContainer, disabledPrimaryButton = colors.primaryContainer,
        background = colors.background, surface = colors.background,
        surfaceContainer = colors.surface, onSurfaceContainer = colors.onSurface, onSurface = colors.onSurface,
        onBackground = colors.onSurface, onSurfaceContainerVariant = colors.onSurfaceVariant
    ) else top.yukonga.miuix.kmp.theme.lightColorScheme(
        primary = colors.primary, onPrimary = colors.onPrimary, primaryVariant = colors.primary,
        primaryContainer = colors.primary, onPrimaryContainer = colors.onPrimary,
        disabledPrimary = colors.primaryContainer, disabledPrimaryButton = colors.primaryContainer,
        background = colors.background, surface = colors.background,
        surfaceContainer = colors.surface, onSurfaceContainer = colors.onSurface, onSurface = colors.onSurface,
        onBackground = colors.onSurface, onSurfaceContainerVariant = colors.onSurfaceVariant
    )
    MiuixTheme(colors = miuixColors) {
        MaterialTheme(colorScheme = colors, typography = Typography(
            headlineMedium = androidx.compose.ui.text.TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold),
            titleLarge = androidx.compose.ui.text.TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold),
            titleMedium = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium),
            bodyLarge = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
            bodyMedium = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
            labelLarge = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium)
        ), content = content)
    }
}
internal object UiMotion {
    const val Damping = 1f
    const val Stiffness = 1100f
}

@Composable internal fun UiGroup(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    MiuixCard(modifier = modifier.fillMaxWidth().clip(shape)
        .then(if(onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        cornerRadius = 24.dp, insideMargin = PaddingValues(0.dp), content = content)
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
    "arrow" to "M5,12 L19,12 M13,6 L19,12 L13,18"
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
    val messagesLifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(vm, messagesLifecycle) {
        messagesLifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.RESUMED) {
            while(true) { vm.refreshHomeMessages(); delay(300_000) }
        }
    }
    val tasks by vm.store.tasks.collectAsStateWithLifecycle()
    val paused by vm.store.paused.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val haze = remember { HazeState() }
    val updateHaze = remember { HazeState() }
    val updateManager = (context.applicationContext as DownloaderApp).updates
    val updateState by updateManager.state.collectAsStateWithLifecycle()
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 72.dp
    val blurFadeStart = with(LocalDensity.current) { (topInset - 20.dp).toPx() }
    val blurFadeEnd = with(LocalDensity.current) { (topInset + 24.dp).toPx() }
    val pager = rememberPagerState(initialPage = vm.tab) { 2 }
    LaunchedEffect(vm.tabRequest) {
        if(vm.tabRequest != vm.consumedTabRequest) {
            vm.consumedTabRequest = vm.tabRequest
            pager.animateScrollToPage(vm.requestedTab, animationSpec = spring(
                dampingRatio = UiMotion.Damping, stiffness = UiMotion.Stiffness))
        }
    }
    LaunchedEffect(pager) {
        snapshotFlow { if(pager.isScrollInProgress) null else pager.settledPage }
            .collect { page -> if(page != null) vm.onPageSettled(page) }
    }
    BackHandler(vm.settings || pager.currentPage != 0) { if(vm.settings) vm.settings = false else vm.tab = 0 }
    LaunchedEffect(vm.notice) {
        vm.notice?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show(); vm.notice = null }
    }
    Surface(Modifier.fillMaxSize().then(if(updateState.dialog) Modifier.hazeSource(updateHaze) else Modifier),
        color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)).windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Bottom))) {
            Box(Modifier.fillMaxSize().hazeSource(haze)) {
                if(vm.settings) {
                    SettingsPage(vm, topInset) { enabled -> vm.setClipboard(enabled); if(enabled) checkClipboard() }
                } else HorizontalPager(
                    state = pager, modifier = Modifier.fillMaxSize().testTag("pages"),
                    beyondViewportPageCount = 1, key = { it }
                ) { page ->
                    if(page == 0) HomePage(vm, pager.currentPage == 0, topInset) {
                        if(vm.submit()) requestNotifications()
                    } else DownloadsPage(tasks, vm, requestNotifications, topInset)
                }
            }
            // Extend just the backdrop; layout and touch targets keep their original bounds.
            Box(Modifier.fillMaxWidth().height(topInset + 24.dp).hazeEffect(haze, style = appGlassStyle()) {
                progressive = HazeProgressive.verticalGradient(
                    startY = blurFadeStart, startIntensity = 1f,
                    endY = blurFadeEnd, endIntensity = 0f,
                    easing = FastOutSlowInEasing
                )
            })
                Row(Modifier.fillMaxWidth().statusBarsPadding().height(72.dp).padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    if(vm.settings) {
                        IconButton(onClick = { vm.settings = false }) { Glyph("back", "返回") }
                        Text("设置", style = MaterialTheme.typography.titleLarge)
                    } else {
                        Text(if(pager.currentPage == 0) "下载" else "下载记录", style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.weight(1f))
                        if(pager.currentPage == 1) {
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
                        IconButton(onClick = { vm.settings = true }) { Glyph("settings", "设置") }
                    }
                }
            if(!vm.settings) {
                FloatingTabs(pager, haze,
                    Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp)) { vm.tab = it }
            }
        }
    }
    DeleteTasksDialog(vm)
    SpecificationDialog(vm.specificationEditor)
    c2cw.cdydownload.update.UpdateOverlay(updateManager, updateHaze)
}


@Composable private fun HomePage(vm: MainViewModel, active: Boolean, topInset: Dp, submit: () -> Unit) {
    val typing = WindowInsets.ime.asPaddingValues().calculateBottomPadding() > 0.dp
    Box(Modifier.fillMaxSize().testTag("homePage").padding(top = topInset)
        .padding(horizontal = 20.dp).padding(bottom = if(typing) 12.dp else 104.dp).imePadding(),
        contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 540.dp).fillMaxWidth().verticalScroll(rememberScrollState())
            .padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("把喜欢的作品，留在本地", style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            HomeMessageCarousel(vm.homeMessages, active && !vm.inputVisible)
            if(!vm.inputVisible) ClipboardSuggestionCard(vm)
            LinkInputCard(vm, submit)
        }
    }
}

@Composable private fun ClipboardSuggestionCard(vm: MainViewModel) {
    val prompt = vm.clipboardPrompt?.takeIf { vm.clipboardEnabled } ?: return
    UiGroup(modifier = Modifier.testTag("inlineClipboardSuggestion")) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PlatformIcon(prompt.link.platform)
                Text("发现作品链接", style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f).padding(start = 10.dp))
                IconButton(onClick = vm::dismissClipboard) { Glyph("close", "忽略此提示") }
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

@Composable private fun LinkInputCard(vm: MainViewModel, submit: () -> Unit) {
    val context = LocalContext.current
    val detected = remember(vm.input) { Links.detect(vm.input) }
    LaunchedEffect(vm.input) { vm.prepareQuality(detected) }
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
        candidate = text.takeIf { link != null && link.key != Links.detect(vm.input)?.key }
    }
    LaunchedEffect(vm.clipboardEnabled) { if(!vm.clipboardEnabled) candidate = null }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        UiGroup {
            Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Glyph("link", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("作品解析", style = MaterialTheme.typography.titleMedium)
                    Text("粘贴分享链接，提取视频或图集", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            HorizontalDivider()
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("输入链接", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(value = vm.input, onValueChange = { vm.input = it.take(16000); vm.error = null; candidate = null },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 110.dp, max = 180.dp)
                        .testTag("downloadLinkInput").focusRequester(focus).onFocusChanged {
                            focused = it.isFocused
                            if(it.isFocused) vm.inputVisible = true
                        },
                    placeholder = { Text("粘贴链接，作品将在下方解析") }, shape = RoundedCornerShape(20.dp),
                    trailingIcon = if(vm.input.isNotEmpty()) {{ IconButton(onClick = {
                        vm.input = ""; vm.error = null; candidate = null; focus.requestFocus()
                    }, modifier = Modifier.testTag("clearLinkInput")) { Glyph("close", "清空输入") } }} else null,
                    isError = vm.error != null, maxLines = 5)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(if(candidate != null) "剪贴板中有${candidate?.let(Links::detect)?.platform?.label.orEmpty()}链接" else if(detected != null) "已识别：${detected.platform.label}" else "自动识别视频或图集",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if(detected != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f))
                    TextButton(onClick = {
                        vm.input = candidate ?: readClipboardText(context, excludeSensitive = false).orEmpty()
                        vm.error = null; candidate = null
                    }, modifier = Modifier.testTag("fillClipboardLink")) { Text(if(candidate != null) "填入" else "粘贴") }
                }
                if(detected != null && vm.qualityLoading) {
                    AppProgressBar(Modifier.fillMaxWidth().testTag("inlineResolveProgress"))
                    Text("正在解析作品信息…", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                vm.resolveError?.let { message ->
                    Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { vm.retryResolve(detected) }, modifier = Modifier.testTag("retryResolve")) { Text("重试解析") }
                }
            }
        }
        vm.preview?.let { preview -> PreviewCard(preview, vm) }
        if(vm.preview?.isAlbum == true) UiGroup {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("保存内容", style = MaterialTheme.typography.titleMedium)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val availableAlbumOptions = AlbumContent.entries.filter { it != AlbumContent.MUSIC || vm.preview?.hasMusic == true }
                    availableAlbumOptions.forEach { content ->
                        FilterChip(selected = vm.albumContent == content, onClick = { vm.albumContent = content },
                            label = { Text(content.label) }, shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.testTag("album-${content.name}"))
                    }
                }
            }
        } else if(vm.preview != null && !vm.preview!!.isAlbum) UiGroup {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("下载模式", style = MaterialTheme.typography.titleMedium)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VideoDownloadMode.entries.forEach { mode ->
                        FilterChip(selected = vm.videoMode == mode, onClick = { vm.videoMode = mode },
                            label = { Text(mode.label) }, shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.testTag("videoMode-${mode.name}"))
                    }
                }
            }
        }
        vm.qualityCatalog?.let { catalog ->
            InlineQualityPicker(catalog, vm.qualityChosen, detected?.key.orEmpty()) { vm.qualityChosen = it }
        }
        vm.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
        val preview = vm.preview
        val albumReady = preview?.isAlbum != true ||
            if(vm.albumContent == AlbumContent.MUSIC) preview.hasMusic else vm.selectedAlbumImages.isNotEmpty()
        val canDownload = vm.isResolved(detected) && !vm.qualityLoading && albumReady
        MiuixButton(onClick = submit, enabled = canDownload,
            colors = MiuixButtonDefaults.buttonColorsPrimary(), cornerRadius = 20.dp, minHeight = 54.dp,
            modifier = Modifier.fillMaxWidth().testTag("resolveThenDownload")) {
            Text(when {
                vm.qualityLoading -> "正在解析…"
                preview?.isAlbum == true -> albumAction(preview, vm.albumContent, vm.selectedAlbumImages.sorted(), vm.albumOutputs)
                else -> "开始下载"
            }, color = if(canDownload)
                MiuixTheme.colorScheme.onPrimary else MiuixTheme.colorScheme.disabledOnPrimaryButton)
        }
    }
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
        contentPadding = PaddingValues(top = topInset, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item(key = "filters") { Row(Modifier.padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("全部", "进行中", "已完成").forEachIndexed { index, title ->
                FilterChip(selected = filter == index, onClick = { filter = index }, label = { Text(title) }, shape = CircleShape)
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
            Box(Modifier.padding(horizontal = 24.dp)) { DownloadCard(task, vm, requestNotifications) }
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
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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

@Composable private fun SettingsPage(vm: MainViewModel, topInset: Dp, onClipboard: (Boolean) -> Unit) {
    val context = LocalContext.current
    val parallelism by vm.store.parallelism.collectAsStateWithLifecycle()
    var selectParallelism by remember { mutableStateOf(false) }
    if(selectParallelism) AlertDialog(onDismissRequest = { selectParallelism = false },
        title = { Text("最多同时下载") },
        text = { Column {
            (1..6).forEach { count ->
                Row(Modifier.fillMaxWidth().selectable(selected = parallelism == count,
                    role = androidx.compose.ui.semantics.Role.RadioButton,
                    onClick = { vm.setParallelism(count); selectParallelism = false }).padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = parallelism == count, onClick = null)
                    Text("$count 个任务" + if(count == 3) "（默认）" else "", Modifier.padding(start = 12.dp))
                }
            }
        } }, confirmButton = {}, dismissButton = { TextButton(onClick = { selectParallelism = false }) { Text("取消") } })
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = topInset).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("下载", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        UiGroup(onClick = { selectParallelism = true },
            modifier = Modifier.testTag("parallelismSetting")) {
            Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("最多同时下载", style = MaterialTheme.typography.titleMedium)
                    Text("$parallelism 个任务", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                    Text("调低后，进行中的任务继续完成，后续按新上限开始。", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Glyph("arrow")
            }
        }
        UiGroup {
            Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).padding(end = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("将下载时间设为图片拍摄时间", style = MaterialTheme.typography.titleMedium)
                    Text("适用于之后下载的图片和实况图，覆盖原有拍摄时间，方便在相册中查找。不支持写入的格式仅设置相册时间。",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                MiuixSwitch(checked = vm.imageDownloadTime, onCheckedChange = vm::changeImageDownloadTime)
            }
        }
        AccountSettings()
        Text("链接与提醒", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        UiGroup {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).padding(end = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("自动检测剪贴板", style = MaterialTheme.typography.titleMedium)
                        Text("打开应用或回到前台时，识别视频并提示下载。", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    MiuixSwitch(checked = vm.clipboardEnabled, onCheckedChange = onClipboard)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text("仅识别 B 站、抖音、小红书、YouTube、微博和微信视频号作品，每次重新复制都会提醒，同一次复制不重复提醒。普通文本与敏感内容不会保存。", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        UiGroup {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("在其他应用中添加下载", style = MaterialTheme.typography.titleMedium)
                Text("系统不允许后台读取剪贴板。在视频应用中选择「分享 → 更多 → CDy下载器」，确认后即可下载。",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        UiGroup(onClick = {
            context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
        }) {
            Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("下载通知", style = MaterialTheme.typography.titleMedium)
                    Text("管理进度与完成提醒", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Glyph("arrow")
            }
        }
        SaveLocationCard(vm)
        FileNameTemplateCard(vm)
        c2cw.cdydownload.update.AboutUpdateCard()
    }
}

@Composable private fun FileNameTemplateCard(vm: MainViewModel) {
    var editing by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf(vm.fileNameTemplate) }
    val previewName = remember(draft) {
        formatFileName(
            template = draft,
            title = "示例视频标题",
            id = "bv1xx411c7md",
            author = "作者名",
            platform = Platform.BILI,
            extension = "mp4"
        )
    }

    UiGroup(onClick = { draft = vm.fileNameTemplate; editing = !editing }) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("文件命名规则", style = MaterialTheme.typography.titleMedium)
                    Text(vm.fileNameTemplate, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                    Text("预览：$previewName", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Glyph(if(editing) "close" else "arrow")
            }
            Text("支持变量：{title} 标题、{author} 作者、{platform} 平台、{id} 作品ID、{date} 下载日期。动态照片保留 _MP 标记，图片自动追加序号。",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            AnimatedVisibility(editing,
                enter = expandVertically(spring(dampingRatio = UiMotion.Damping, stiffness = UiMotion.Stiffness)),
                exit = shrinkVertically(spring(dampingRatio = UiMotion.Damping, stiffness = UiMotion.Stiffness))) {
                Column(Modifier.fillMaxWidth().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("预设规则", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NAMING_TEMPLATE_PRESETS.forEach { (tmpl, label) ->
                            FilterChip(
                                selected = draft == tmpl,
                                onClick = { draft = tmpl },
                                label = { Text(label) },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it.take(80) },
                        label = { Text("自定义模板") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("fileNameTemplateInput")
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                        TextButton(onClick = { draft = vm.fileNameTemplate; editing = false }) { Text("取消") }
                        Button(onClick = { vm.changeFileNameTemplate(draft); editing = false }) { Text("保存") }
                    }
                }
            }
        }
    }
}


@Composable private fun SaveLocationCard(vm: MainViewModel) {
    var editing by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf(vm.saveFolder) }
    UiGroup(onClick = { draft = vm.saveFolder; editing = true }) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("保存位置", style = MaterialTheme.typography.titleMedium)
                    Text("视频：Download / ${vm.saveFolder}\n图片：Pictures / ${vm.saveFolder}\n配乐：Music / ${vm.saveFolder}",
                        style = MaterialTheme.typography.bodyLarge)
                }
                Glyph("arrow")
            }
            Text("视频保存到下载目录，图片和动图保存到系统相册，独立配乐保存到音乐目录。B 站和 YouTube 音视频自动合并；抖音和小红书图集保留实况动态，普通动图保留原格式，图片与配乐分开下载。",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    if(editing) AlertDialog(onDismissRequest = { editing = false },
        title = { Text("保存子目录") },
        text = { OutlinedTextField(value = draft, onValueChange = { draft = it.take(40) }, singleLine = true,
            placeholder = { Text(DEFAULT_SAVE_FOLDER) }, modifier = Modifier.testTag("saveFolderInput")) },
        confirmButton = { Button(onClick = { vm.changeSaveFolder(draft); editing = false }) { Text("保存") } },
        dismissButton = { TextButton(onClick = { editing = false }) { Text("取消") } })
}
