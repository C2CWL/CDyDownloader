package c2cw.cdydownload

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** State of the Douyin author page. Only one author is open at a time. */
internal class AuthorController(
    private val scope: CoroutineScope,
    private val follows: FollowStore,
    private val download: (List<DouyinWork>) -> Int,
    private val notice: (String) -> Unit
) {
    var secUid by mutableStateOf<String?>(null)
        private set
    var profile by mutableStateOf<DouyinAuthor?>(null)
        private set
    var works by mutableStateOf<List<DouyinWork>>(emptyList())
        private set
    var selected by mutableStateOf<Set<String>>(emptySet())
        private set
    var loading by mutableStateOf(false)
        private set
    var loadingMore by mutableStateOf(false)
        private set
    var downloadingAll by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var worksError by mutableStateOf<String?>(null)
        private set
    var hasMore by mutableStateOf(false)
        private set
    /** Works that were new since the last visit, highlighted once on this visit. */
    var freshIds by mutableStateOf<Set<String>>(emptySet())
        private set
    private var cursor = "0"
    private var job: Job? = null

    val visible get() = secUid != null

    fun open(sec: String) {
        if(secUid == sec && profile != null) return
        job?.cancel()
        secUid = sec; profile = null; works = emptyList(); selected = emptySet(); error = null; worksError = null
        hasMore = false; cursor = "0"; loading = true; loadingMore = false
        freshIds = follows.get(sec)?.newIds?.toSet().orEmpty()
        job = scope.launch {
            try {
                val author = DouyinAuthors.profile(sec)
                if(secUid != sec) return@launch
                profile = author
                loading = false
                loadPage(sec)
            } catch(e: CancellationException) { throw e }
            catch(e: Exception) {
                if(secUid == sec) { error = e.message ?: "作者主页加载失败，请稍后重试"; loading = false }
            }
        }
    }

    fun retry() { val sec = secUid ?: return; secUid = null; open(sec) }

    fun close() {
        job?.cancel()
        secUid = null; profile = null; works = emptyList(); selected = emptySet(); loading = false; loadingMore = false
        downloadingAll = false
    }

    private suspend fun loadPage(sec: String) {
        loadingMore = true
        worksError = null
        try {
            val page = DouyinAuthors.works(sec, cursor)
            if(secUid != sec) return
            val known = works.map { it.id }.toSet()
            works = works + page.works.filter { it.id !in known }
            cursor = page.cursor
            hasMore = page.hasMore
            if(works.isNotEmpty() && follows.isFollowed(sec)) follows.update(sec) {
                it.viewed(works.map(DouyinWork::id)).copy(works = profile?.works ?: it.works)
            }
        } catch(e: CancellationException) { throw e }
        catch(e: Exception) { if(secUid == sec) worksError = e.message ?: "作品列表加载失败，请稍后重试" }
        finally { if(secUid == sec) loadingMore = false }
    }

    fun loadMore() {
        val sec = secUid ?: return
        if(loadingMore || loading || (!hasMore && works.isNotEmpty() && worksError == null)) return
        job = scope.launch { loadPage(sec) }
    }

    fun toggle(id: String) { selected = if(id in selected) selected - id else selected + id }
    fun selectAll() { selected = works.map { it.id }.toSet() }
    fun clearSelection() { selected = emptySet() }

    fun isFollowed(followed: List<FollowedAuthor>) = secUid?.let { sec -> followed.any { it.secUid == sec } } == true

    fun toggleFollow() {
        val author = profile ?: return
        if(follows.isFollowed(author.secUid)) {
            follows.unfollow(author.secUid); notice("已取消关注")
        } else {
            follows.follow(author, works.map { it.id })
            notice("已关注，可在「关注」里检查更新")
        }
    }

    fun downloadSelected() {
        val chosen = works.filter { it.id in selected }
        if(chosen.isEmpty()) { notice("请先选择作品"); return }
        if(download(chosen) > 0) selected = emptySet()
    }

    /** Loads the remaining pages (bounded) and queues everything. */
    fun downloadAll() {
        val sec = secUid ?: return
        if(downloadingAll) return
        downloadingAll = true
        scope.launch {
            try {
                var pages = 0
                while(hasMore && secUid == sec && pages < 20 && worksError == null) { loadPage(sec); pages++ }
                if(secUid == sec) {
                    if(works.isEmpty()) notice(worksError ?: "没有可以下载的作品")
                    else download(works)
                }
            } finally { downloadingAll = false }
        }
    }
}

/** 检查更新 on the follow tab: one author at a time, so the shared web page is reused. */
internal class FollowUpdates(
    private val scope: CoroutineScope,
    private val follows: FollowStore,
    private val notice: (String) -> Unit
) {
    var running by mutableStateOf(false)
        private set
    var done by mutableIntStateOf(0)
        private set
    var total by mutableIntStateOf(0)
        private set

    fun check() {
        if(running) return
        val authors = follows.authors.value
        if(authors.isEmpty()) { notice("还没有关注的作者"); return }
        running = true; done = 0; total = authors.size
        scope.launch {
            var failed = 0
            var lastError: String? = null
            try {
                for(author in authors) {
                    try {
                        val page = DouyinAuthors.works(author.secUid)
                        val profile = runCatching { DouyinAuthors.profile(author.secUid) }.getOrNull()
                        follows.update(author.secUid) {
                            it.checked(page.works.map(DouyinWork::id), System.currentTimeMillis(),
                                works = profile?.works?.takeIf { n -> n >= 0 } ?: it.works,
                                nickname = profile?.nickname.orEmpty(), avatar = profile?.avatar.orEmpty())
                        }
                    } catch(e: CancellationException) { throw e }
                    catch(e: Exception) { failed++; lastError = e.message }
                    done++
                }
                val fresh = follows.authors.value.count { it.newCount > 0 }
                notice(when {
                    failed == authors.size -> lastError ?: "检查失败，请稍后重试"
                    failed > 0 -> "$failed 位作者检查失败；${if(fresh > 0) "$fresh 位有新作品" else "其余没有新作品"}"
                    fresh > 0 -> "$fresh 位作者有新作品"
                    else -> "都没有新作品"
                })
            } finally { running = false }
        }
    }
}
