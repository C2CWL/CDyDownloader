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
    internal var homeMessages by mutableStateOf(HomeMessages.Empty)
        private set
    internal suspend fun refreshHomeMessages() {
        try {
            homeMessages = c2cw.cdydownload.update.UpdateSource(BuildConfig.UPDATE_REPOSITORY, BuildConfig.UPDATE_BRANCH)
                .repositoryJson("config/home-messages.json", HomeMessages::parse)
        } catch(e: CancellationException) { throw e }
        catch(_: Exception) { homeMessages = HomeMessages.Empty }
    }
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
        val key = when(kind) {
            SaveKind.VIDEO -> SAVE_FOLDER_VIDEO
            SaveKind.IMAGE -> SAVE_FOLDER_IMAGE
            SaveKind.MUSIC -> SAVE_FOLDER_MUSIC
        }
        prefs.edit().putString(key, value).apply()
    }
    var accentMode by mutableStateOf(AccentMode.WALLPAPER)
        private set
    fun changeAccentMode(mode: AccentMode) {
        accentMode = mode
        AccentPreference.mode = mode
        prefs.edit().putString(ACCENT_MODE, mode.name).apply()
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
                    context.contentResolver.delete(android.net.Uri.parse(value), null, null)
                }
                notice = if(result.failed == 0) "已删除 ${result.removed} 个任务" else "已删除 ${result.removed} 个任务，${result.failed} 个任务的文件无法删除，记录已保留"
                deleteRequest = null
            } catch(e: CancellationException) { throw e }
            catch(_: Exception) { notice = "删除未完成，请重试" }
            finally { deleting = false }
        }
    }
    internal val specificationEditor by lazy { SpecificationEditor(context,viewModelScope) { task ->
        revealTaskId=task.id; tab=1; settings=false; start(task.id)
    } }
    var settings by mutableStateOf(false)
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
        input = text.take(16000); error = null; inputVisible = true; if(!settings) tab = 0
    }
    fun onShare(text: String) {
        settings = false
        openInput(text)
        if(Links.detect(text) == null) error = "没有识别到 B 站、抖音、YouTube、微博或微信视频号视频"
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
        inputVisible = false; tab = 1; settings = false
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
        tab=1; settings=false
        return start(task.id)
    }
    private fun enqueue(link: VideoLink, info: VideoInfo? = null, selection: TrackSelection? = null,
        videoMode: VideoDownloadMode = VideoDownloadMode.MERGE, albumImageIndices: List<Int>? = null, albumOutputs: Map<Int, AlbumOutput> = emptyMap()): DownloadTask {
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
    fun setParallelism(count: Int) { store.setParallelism(count) }
    fun cancel(id: String) {
        runCatching { DownloadService.cancel(context, id) }.onFailure {
            store.update(id) { it.copy(status = TaskStatus.CANCELLED, speed = 0) }
        }
    }
}
