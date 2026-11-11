package c2cw.cdydownload

import android.app.Application
import android.content.Intent
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val context get() = getApplication<DownloaderApp>()
    val store get() = context.store
    private val prefs = app.getSharedPreferences("settings", 0)
    var imageDownloadTime by mutableStateOf(prefs.getBoolean(IMAGE_DOWNLOAD_TIME, false))
        private set
    fun changeImageDownloadTime(enabled: Boolean) {
        imageDownloadTime = enabled
        prefs.edit().putBoolean(IMAGE_DOWNLOAD_TIME, enabled).apply()
    }
    var clipboardEnabled by mutableStateOf(prefs.getBoolean("clipboard", true))
        private set
    var saveFolderVideo by mutableStateOf(readSaveFolder(prefs, SaveKind.VIDEO))
        private set
    var saveFolderImage by mutableStateOf(readSaveFolder(prefs, SaveKind.IMAGE))
        private set
    var saveFolderMusic by mutableStateOf(readSaveFolder(prefs, SaveKind.MUSIC))
        private set
    fun changeSaveFolder(kind: SaveKind, name: String) {
        val value = optionalSaveFolder(name)
        when(kind) {
            SaveKind.VIDEO -> saveFolderVideo = value
            SaveKind.IMAGE -> saveFolderImage = value
            SaveKind.MUSIC -> saveFolderMusic = value
        }
        val old = saveTree(kind)
        setSaveTreeState(kind, "")
        prefs.edit().putString(saveFolderKey(kind), value).remove(saveTreeKey(kind)).apply()
        SaveTree.releaseUnused(context, old, SaveKind.entries.map(::saveTree))
    }
    var saveTreeVideo by mutableStateOf(readSaveTree(prefs, SaveKind.VIDEO))
        private set
    var saveTreeImage by mutableStateOf(readSaveTree(prefs, SaveKind.IMAGE))
        private set
    var saveTreeMusic by mutableStateOf(readSaveTree(prefs, SaveKind.MUSIC))
        private set
    fun saveTree(kind: SaveKind) = when(kind) { SaveKind.VIDEO -> saveTreeVideo; SaveKind.IMAGE -> saveTreeImage; SaveKind.MUSIC -> saveTreeMusic }
    fun saveFolder(kind: SaveKind) = when(kind) { SaveKind.VIDEO -> saveFolderVideo; SaveKind.IMAGE -> saveFolderImage; SaveKind.MUSIC -> saveFolderMusic }
    private fun setSaveTreeState(kind: SaveKind, value: String) {
        when(kind) {
            SaveKind.VIDEO -> saveTreeVideo = value
            SaveKind.IMAGE -> saveTreeImage = value
            SaveKind.MUSIC -> saveTreeMusic = value
        }
    }
    /** A folder picked with the system picker. Earlier typed folder names keep working until this is used. */
    fun chooseSaveTree(kind: SaveKind, uri: android.net.Uri) {
        try { SaveTree.persist(context, uri) } catch(_: SecurityException) {
            notice = "没有获得这个文件夹的写入权限，请换一个文件夹"; return
        }
        val old = saveTree(kind)
        setSaveTreeState(kind, uri.toString())
        prefs.edit().putString(saveTreeKey(kind), uri.toString()).apply()
        SaveTree.releaseUnused(context, old, SaveKind.entries.map(::saveTree))
    }
    /** True when a picked folder no longer grants write access, so settings can warn early. */
    fun saveTreeLost(kind: SaveKind) = saveTree(kind).let { it.isNotBlank() && !SaveTree.hasWriteAccess(context.contentResolver, it) }
    var accentMode by mutableStateOf(AccentMode.WALLPAPER)
        private set
    fun changeAccentMode(mode: AccentMode) {
        accentMode = mode
        AccentPreference.mode = mode
        prefs.edit().putString(ACCENT_MODE, mode.name).apply()
    }
    val customAccent get() = AccentPreference.custom
    fun changeCustomAccent(color: Int) {
        val opaque = color or (0xFF shl 24)
        AccentPreference.custom = opaque
        changeAccentMode(AccentMode.CUSTOM)
        prefs.edit().putInt(ACCENT_CUSTOM, opaque).apply()
    }
    init { AccentPreference.load(prefs); accentMode = AccentPreference.mode }
    var fileNameTemplate by mutableStateOf(prefs.getString(FILE_NAME_TEMPLATE, DEFAULT_FILE_NAME_TEMPLATE) ?: DEFAULT_FILE_NAME_TEMPLATE)
        private set
    fun changeFileNameTemplate(template: String) {
        fileNameTemplate = template.trim().ifBlank { DEFAULT_FILE_NAME_TEMPLATE }
        prefs.edit().putString(FILE_NAME_TEMPLATE, fileNameTemplate).apply()
    }
    private val gate = PromptGate(prefs.getStringSet("handled", emptySet()).orEmpty().toMutableSet())
    private var selectedTab by mutableIntStateOf(0)
    var requestedTab by mutableIntStateOf(0)
        private set
    internal var consumedTabRequest = 0
    var tabRequest by mutableIntStateOf(0)
        private set
    var tab: Int
        get() = selectedTab
        set(value) {
            selectedTab = value
            requestedTab = value
            tabRequest++
        }
    // Gesture settlement updates selection without issuing another navigation command.
    fun onPageSettled(page: Int) { selectedTab = page }
    var revealTaskId by mutableStateOf<String?>(null)
        private set
    fun taskRevealed(id: String) { if(revealTaskId == id) revealTaskId = null }
    internal var deleteRequest by mutableStateOf<DeleteRequest?>(null)
        private set
    var deleting by mutableStateOf(false)
        private set
    internal fun requestDelete(ids: List<String>, all: Boolean = false, withFiles: Boolean? = null) {
        if(!deleting && ids.isNotEmpty()) deleteRequest = DeleteRequest(ids.toList(), all, withFiles)
    }
    internal fun chooseDeleteFiles() { deleteRequest = deleteRequest?.copy(withFiles = true) }
    internal fun dismissDeletion() { if(!deleting) deleteRequest = null }
    internal fun confirmDeletion(withFiles: Boolean) {
        val request = deleteRequest ?: return
        if(deleting) return
        deleting = true
        viewModelScope.launch {
            try {
                val result = deleteDownloadTasks(store, request.ids, withFiles, DownloadService::awaitCancellation) { value ->
                    deleteSavedOutput(context, value)
                }
                notice = if(result.failed == 0) "已删除 ${result.removed} 个任务" else "已删除 ${result.removed} 个任务，${result.failed} 个任务的文件无法删除，记录已保留"
                deleteRequest = null
            } catch(e: CancellationException) { throw e }
            catch(_: Exception) { notice = "删除未完成，请重试" }
            finally { deleting = false }
        }
    }
    internal val specificationEditor by lazy { SpecificationEditor(context,viewModelScope) { task ->
        revealTaskId=task.id; tab=TAB_DOWNLOADS; start(task.id)
    } }
    /** Settings is a bottom-bar tab now; this keeps older call sites working. */
    var settings: Boolean
        get() = selectedTab == TAB_SETTINGS
        set(value) { if(value) tab = TAB_SETTINGS else if(selectedTab == TAB_SETTINGS) tab = TAB_HOME }
    var inputVisible by mutableStateOf(false)
    var input by mutableStateOf("")
    /** Chosen before the download starts; for video works. */
    var videoMode by mutableStateOf(VideoDownloadMode.MERGE)
    /** Chosen before the download starts; only album works use it. */
    var albumContent by mutableStateOf(AlbumContent.ALL)
    /** Resolved ahead of the download so the quality can be picked on the home page. */
    var qualityCatalog by mutableStateOf<MediaSpecifications?>(null)
        private set
    var qualityChosen by mutableStateOf<String?>(null)
    /** The resolved work, shown on the home page before anything is queued. */
    var preview by mutableStateOf<MediaPreview?>(null)
        private set
    internal var resolvedInfo by mutableStateOf<VideoInfo?>(null)
        private set
    var selectedAlbumImages by mutableStateOf<Set<Int>>(emptySet())
        private set
    var albumOutputs by mutableStateOf<Map<Int, AlbumOutput>>(emptyMap())
        private set
    fun chooseAlbumOutput(index: Int, output: AlbumOutput) {
        if(resolvedInfo?.imageVideos?.getOrNull(index) != null) albumOutputs = albumOutputs + (index to output)
    }
    /** One switch for every live item in the album. */
    fun chooseAllAlbumOutputs(output: AlbumOutput) {
        val videos = resolvedInfo?.imageVideos ?: return
        albumOutputs = videos.indices.filter { videos[it] != null }.associateWith { output }
    }
    var resolveError by mutableStateOf<String?>(null)
        private set
    var qualityLoading by mutableStateOf(false)
        private set
    private var qualityJob: Job? = null
    private var qualityKey: String? = null

    fun prepareQuality(link: VideoLink?) {
        if(link?.key == qualityKey) return
        qualityJob?.cancel()
        qualityKey = link?.key
        qualityCatalog = null
        qualityChosen = null
        videoMode = VideoDownloadMode.MERGE
        preview = null
        resolvedInfo = null
        selectedAlbumImages = emptySet()
        albumOutputs = emptyMap()
        resolveError = null
        qualityLoading = link != null
        if(link == null) return
        val key = link.key
        qualityJob = viewModelScope.launch {
            try {
                val info = VideoResolver().resolve(link)
                val specs = info.specifications
                if(qualityKey == key) {
                    resolvedInfo = info
                    preview = info.toPreview()
                    selectedAlbumImages = info.images.indices.toSet()
                }
                if(info.images.isEmpty() && specs != null && specs.videos.size > 1) qualityCatalog = specs
            } catch(e: CancellationException) { throw e }
            catch(e: DouyinUserRedirect) {
                if(qualityKey == key) openAuthor(e.secUid)
            }
            catch(e: Exception) {
                if(qualityKey == key) resolveError = "解析失败：${e.message?.take(120)?.ifBlank { null } ?: "请检查链接后重试"}"
            }
            if(qualityKey == key) qualityLoading = false
        }
    }
    fun retryResolve(link: VideoLink?) {
        if(link == null) return
        qualityKey = null
        prepareQuality(link)
    }
    fun toggleAlbumImage(index: Int) {
        val count = resolvedInfo?.images?.size ?: return
        if(index !in 0 until count) return
        selectedAlbumImages = if(index in selectedAlbumImages) selectedAlbumImages - index else selectedAlbumImages + index
    }
    fun selectAllAlbumImages() {
        selectedAlbumImages = resolvedInfo?.images?.indices?.toSet().orEmpty()
    }
    fun clearAlbumImageSelection() { selectedAlbumImages = emptySet() }
    internal fun isResolved(link: VideoLink?) = link != null && qualityKey == link.key && resolvedInfo != null
    private fun chosenQuality(): TrackSelection? {
        val catalog = qualityCatalog ?: return null
        val chosen = qualityChosen ?: return null
        val option = catalog.videos.find { it.id == chosen } ?: return null
        if(option.id == catalog.selected.video) return null
        val selection = catalog.selectVideo(option.id)
        if(!option.hasAudio) return selection.copy(audio = catalog.audios.firstOrNull()?.id ?: selection.audio)
        return selection
    }
    var error by mutableStateOf<String?>(null)
    var notice by mutableStateOf<String?>(null)
    internal val clipboard = ClipboardSuggestions(viewModelScope, { VideoResolver().resolve(it) }, gate::shouldShow, ::rememberHandled)
    internal val clipboardPrompt get() = clipboard.state

    fun setClipboard(enabled: Boolean) {
        clipboardEnabled = enabled
        prefs.edit().putBoolean("clipboard", enabled).apply()
        if(!enabled) clipboard.clear()
    }
    internal fun inspectClipboard(content: ClipboardContent?) {
        if(clipboardEnabled && !inputVisible) clipboard.inspect(content?.text, content?.timestamp ?: 0)
    }
    fun dismissClipboard() = clipboard.dismiss()
    fun retryClipboard() = clipboard.retry()
    private fun rememberHandled(key: String) {
        gate.handled(key)
        prefs.edit().putStringSet("handled", gate.keys()).apply()
    }
    fun openInput(text: String = "") {
        clipboard.clear()
        // The input card is laid out on the home page, so opening it must land there.
        input = text.take(16000); error = null; inputVisible = true; author.close(); tab = TAB_HOME
    }
    fun onShare(text: String) {
        settings = false
        openInput(text)
        if(Links.detect(text) == null && DouyinUserLinks.detect(text) == null) error = "没有识别到 B 站、抖音、YouTube、微博或微信视频号视频"
    }
    fun submit(): Boolean {
        val link = Links.detect(input)
        if(link == null) { error = "请粘贴 B 站、抖音、YouTube、微博或微信视频号链接，也支持分享文案、BV / AV 号和 YouTube 视频 ID"; return false }
        val info = resolvedInfo?.takeIf { qualityKey == link.key }
        if(info == null) { error = resolveError ?: "请等待作品解析完成"; return false }
        val album = info.images.isNotEmpty()
        val selected = if(album && albumContent != AlbumContent.MUSIC) selectedAlbumImages.sorted() else null
        if(album && albumContent == AlbumContent.MUSIC && info.music.isNullOrBlank()) {
            error = "此图集没有可单独保存的配乐"; return false
        }
        if(album && albumContent != AlbumContent.MUSIC && selected.isNullOrEmpty()) {
            error = "请至少选择一项内容"; return false
        }
        val task = enqueue(link, info = info.takeIf { album }, selection = chosenQuality(), videoMode = videoMode, albumImageIndices = selected, albumOutputs = albumOutputs)
        if(album) context.prepareDownload(task.id, info)
        if(!start(task.id)) { error = store.get(task.id)?.error; return false }
        inputVisible = false; tab = TAB_DOWNLOADS
        prepareQuality(null)
        return true
    }
    fun downloadSuggestion(): Boolean {
        val prompt=clipboardPrompt ?: return false
        if(prompt is ClipboardPrompt.Failed) return false
        val info=(prompt as? ClipboardPrompt.Ready)?.info
        clipboard.dismiss()
        val task=enqueue(info?.source ?: prompt.link,info)
        if(info != null) context.prepareDownload(task.id,info)
        tab=TAB_DOWNLOADS
        return start(task.id)
    }
    /** Queue only this album item. Keeps the current selection and stays on the work page. */
    fun downloadAlbumItem(index: Int): Boolean {
        val link = Links.detect(input)
        val info = resolvedInfo?.takeIf { link != null && qualityKey == link.key }
        if(link == null || info == null) { error = resolveError ?: "请等待作品解析完成"; return false }
        val queued = albumItemDownload(index, info.images.size, albumOutputs)
        if(queued == null) { error = "没有这一张图"; return false }
        val (indices, outputs) = queued
        val task = enqueue(link, info = info, albumContent = AlbumContent.IMAGES,
            albumImageIndices = indices, albumOutputs = outputs)
        context.prepareDownload(task.id, info)
        if(!start(task.id)) { error = store.get(task.id)?.error; return false }
        notice = albumItemDownloadNotice(albumOutput(index, info.imageVideos, albumOutputs))
        return true
    }
    private fun enqueue(link: VideoLink, info: VideoInfo? = null, selection: TrackSelection? = null,
        videoMode: VideoDownloadMode = VideoDownloadMode.MERGE, albumContent: AlbumContent = this.albumContent,
        albumImageIndices: List<Int>? = null, albumOutputs: Map<Int, AlbumOutput> = emptyMap()): DownloadTask {
        val task = store.add(link, selection = selection, videoMode = videoMode, albumContent = albumContent, albumImageIndices = albumImageIndices, albumOutputs = albumOutputs)
        if(info != null) store.update(task.id) {
            it.copy(title = info.title, quality = info.quality, resolution = info.resolution)
        }
        revealTaskId = task.id
        return task
    }
    private fun start(id: String): Boolean = try {
        if(!store.paused.value) DownloadService.start(context)
        true
    } catch(e: Exception) {
        store.update(id) { it.copy(status = TaskStatus.FAILED, error = "无法启动后台下载，请回到应用后重试") }
        false
    }
    fun retry(id: String) {
        if(deleting) return
        if(store.retry(id)) start(id)
    }
    fun pauseDownloads() {
        runCatching { DownloadService.pause(context) }.onFailure { notice = "无法暂停下载，请重试" }
    }
    fun resumeDownloads() {
        if(deleting) return
        runCatching { DownloadService.resume(context) }.onFailure { notice = "无法继续下载，请重试" }
    }
    // Douyin authors and follows.
    internal val follows by lazy { FollowStore(java.io.File(context.filesDir, "follows.json")) }
    internal val author by lazy { AuthorController(viewModelScope, follows, ::downloadWorks) { notice = it } }
    internal val followUpdates by lazy { FollowUpdates(viewModelScope, follows) { notice = it } }
    private var routedAuthorInput: String? = null
    fun openAuthor(secUid: String) { author.open(secUid) }
    /** A pasted Douyin user homepage link goes straight to the author page, once per paste. */
    fun routeAuthorInput(text: String): Boolean {
        val sec = DouyinUserLinks.detect(text) ?: return false
        if(routedAuthorInput != text) { routedAuthorInput = text; prepareQuality(null); openAuthor(sec) }
        return true
    }
    /** A work tapped on the author page opens in the work page. */
    fun openWork(work: DouyinWork) {
        author.close()
        openInput(work.url)
    }
    /** Queues author works with default options: live items stay live photos, music saved separately. */
    fun downloadWorks(works: List<DouyinWork>): Int {
        var added = 0
        works.forEach { work ->
            val link = Links.fromUrl(work.url) ?: return@forEach
            val task = store.add(link)
            store.update(task.id) { it.copy(title = work.title, quality = if(work.isAlbum) "${work.imageCount} 张图片" else it.quality) }
            revealTaskId = task.id
            added++
        }
        if(added > 0) {
            val id = revealTaskId
            if(id != null) start(id)
            notice = "已加入 $added 个作品到下载队列"
        }
        return added
    }
    fun setParallelism(count: Int) { store.setParallelism(count) }
    fun cancel(id: String) {
        runCatching { DownloadService.cancel(context, id) }.onFailure {
            store.update(id) { it.copy(status = TaskStatus.CANCELLED, speed = 0) }
        }
    }
}
