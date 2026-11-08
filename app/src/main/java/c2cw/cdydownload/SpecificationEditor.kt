package c2cw.cdydownload

import android.net.Uri
import androidx.compose.runtime.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal data class SpecificationEditorState(val taskId: String, val catalog: MediaSpecifications? = null,
    val selection: TrackSelection? = null, val loading: Boolean = true, val applying: Boolean = false,
    val error: String? = null, val confirmFiles: Boolean = false,
    /** Non-null marks an album task; the dialog then edits what the task saves, not video tracks. */
    val albumContent: AlbumContent? = null, val albumHasMusic: Boolean = false)

internal class SpecificationEditor(private val app: DownloaderApp, private val scope: CoroutineScope,
    private val replaced: (DownloadTask) -> Unit) {
    var state by mutableStateOf<SpecificationEditorState?>(null)
        private set
    private var job: Job? = null
    fun open(id: String) {
        if(state?.applying == true) return
        job?.cancel()
        state = SpecificationEditorState(id)
        job = scope.launch {
            try {
                val task = app.store.get(id) ?: error("任务已删除")
                val link = Links.detect(task.source) ?: error("视频链接无效")
                val info = VideoResolver().resolve(link)
                if(info.images.isNotEmpty()) {
                    val content = if(info.music == null && task.albumContent == AlbumContent.MUSIC) AlbumContent.IMAGES else task.albumContent
                    state = SpecificationEditorState(id,albumContent=content,albumHasMusic=info.music != null,loading=false)
                    return@launch
                }
                val catalog = info.specifications ?: error("平台没有返回可选规格，请稍后重试")
                check(catalog.videos.isNotEmpty()) { "没有可用的视频规格，请稍后重试" }
                val video=catalog.videos.find { it.id == task.selection?.video }
                    ?: catalog.videos.find { it.id == catalog.selected.video } ?: catalog.videos.first()
                state = SpecificationEditorState(id,catalog,catalog.selectVideo(video.id),loading=false)
            } catch(e: CancellationException) { throw e }
            catch(e: Exception) { state = state?.copy(loading=false,error=e.message ?: "无法获取规格，请重试") }
        }
    }
    fun dismiss() { if(state?.applying != true) { job?.cancel(); state = null } }
    fun video(id: String) {
        val current = state ?: return
        val catalog = current.catalog ?: return
        if(catalog.videos.none { it.id == id }) return
        state = current.copy(selection=catalog.selectVideo(id,current.selection),error=null)
    }
    fun audio(id: String) {
        val current = state ?: return
        val catalog = current.catalog ?: return
        val selection = current.selection ?: return
        if(catalog.videos.find { it.id == selection.video }?.hasAudio != false) return
        if(id.isNotEmpty() && catalog.audios.none { it.id == id }) return
        state=current.copy(selection=selection.copy(audio=id.takeIf(String::isNotEmpty)),error=null)
    }
    fun album(content: AlbumContent) {
        val current = state ?: return
        if(current.albumContent == null || current.applying) return
        if(content == AlbumContent.MUSIC && !current.albumHasMusic) return
        state=current.copy(albumContent=content,error=null)
    }
    fun cancelConfirmation() { state=state?.copy(confirmFiles=false) }
    fun confirm(deleteFiles: Boolean = false) {
        val current = state ?: return
        val selection = current.selection
        if(current.albumContent == null && selection == null) return
        if(current.loading || current.applying) return
        val task = app.store.get(current.taskId) ?: run { state=current.copy(error="任务已删除"); return }
        if(!deleteFiles && (task.status == TaskStatus.COMPLETED || task.uri.isNotBlank() || task.outputUris.isNotEmpty())) {
            state=current.copy(confirmFiles=true); return
        }
        state=current.copy(applying=true,error=null,confirmFiles=false)
        job=scope.launch {
            try {
                val link = Links.detect(task.source) ?: error("链接无效")
                val info = if(current.albumContent != null) VideoResolver().resolve(link)
                    else VideoResolver().resolve(link, selection)
                val done = replaceTaskSpecification(app.store,task.id,info,
                    if(current.albumContent != null) task.selection else selection,
                    current.albumContent ?: AlbumContent.ALL,deleteFiles,
                    DownloadService::awaitCancellation) { uri -> app.contentResolver.delete(Uri.parse(uri),null,null) }
                app.prepareDownload(done.id,info)
                state=null
                replaced(done)
            } catch(e: CancellationException) { throw e }
            catch(_: FileReplacementConfirmation) { state=state?.copy(applying=false,confirmFiles=true) }
            catch(e: Exception) { state=state?.copy(applying=false,error=e.message ?: "更换规格失败，原任务已保留") }
        }
    }
}
