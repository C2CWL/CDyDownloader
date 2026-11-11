package c2cw.cdydownload

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** A followed Douyin author, stored on this device only. */
data class FollowedAuthor(
    val secUid: String,
    val nickname: String,
    val avatar: String = "",
    val douyinId: String = "",
    val works: Int = -1,
    val addedAt: Long = 0,
    val lastChecked: Long = 0,
    /** Works already known when the user last looked; newest first, bounded. */
    val seenIds: List<String> = emptyList(),
    /** Works found by update checks that the user has not opened the author page for yet. */
    val newIds: List<String> = emptyList()
) {
    val newCount get() = newIds.size
}

internal const val SEEN_LIMIT = 80

/** Works in [latest] that were not known before. With no baseline yet, nothing counts as new. */
internal fun diffNewWorks(seen: Collection<String>, latest: List<String>): List<String> =
    if(seen.isEmpty()) emptyList() else latest.filter { it !in seen }.distinct()

/** Result of an update check: newly found works are added to the pending list. */
internal fun FollowedAuthor.checked(latest: List<String>, now: Long, works: Int = this.works,
    nickname: String = this.nickname, avatar: String = this.avatar): FollowedAuthor {
    if(seenIds.isEmpty()) return copy(seenIds = latest.distinct().take(SEEN_LIMIT), lastChecked = now,
        works = works, nickname = nickname, avatar = avatar)
    val fresh = diffNewWorks(seenIds + newIds, latest)
    return copy(newIds = (fresh + newIds).distinct(), lastChecked = now, works = works,
        nickname = nickname.ifBlank { this.nickname }, avatar = avatar.ifBlank { this.avatar })
}

/** The user opened the author page: everything pending and currently listed becomes seen. */
internal fun FollowedAuthor.viewed(latest: List<String> = emptyList()): FollowedAuthor =
    copy(seenIds = (newIds + latest + seenIds).distinct().take(SEEN_LIMIT), newIds = emptyList())

internal object FollowCodec {
    fun encode(list: List<FollowedAuthor>): String = JSONObject().put("version", 1).put("authors", JSONArray().apply {
        list.forEach { a -> put(JSONObject()
            .put("secUid", a.secUid).put("nickname", a.nickname).put("avatar", a.avatar)
            .put("douyinId", a.douyinId).put("works", a.works).put("addedAt", a.addedAt)
            .put("lastChecked", a.lastChecked)
            .put("seenIds", JSONArray(a.seenIds)).put("newIds", JSONArray(a.newIds))) }
    }).toString()

    fun decode(raw: String?): List<FollowedAuthor> {
        if(raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONObject(raw).optJSONArray("authors") ?: return emptyList()
            fun strings(a: JSONArray?) = (0 until (a?.length() ?: 0)).mapNotNull { a!!.optString(it).takeIf(String::isNotBlank) }
            (0 until array.length()).mapNotNull { array.optJSONObject(it) }.mapNotNull { o ->
                val sec = o.optString("secUid").takeIf { it.isNotBlank() } ?: return@mapNotNull null
                FollowedAuthor(sec, o.optString("nickname").ifBlank { "抖音作者" }, o.optString("avatar"),
                    o.optString("douyinId"), o.optInt("works", -1), o.optLong("addedAt"), o.optLong("lastChecked"),
                    strings(o.optJSONArray("seenIds")), strings(o.optJSONArray("newIds")))
            }.distinctBy { it.secUid }
        }.getOrDefault(emptyList())
    }
}

/** Followed authors as a small JSON file; newest follow first. */
internal class FollowStore(private val file: File) {
    private val _authors = MutableStateFlow(FollowCodec.decode(runCatching { file.readText() }.getOrNull()))
    val authors: StateFlow<List<FollowedAuthor>> = _authors

    fun get(secUid: String) = _authors.value.find { it.secUid == secUid }
    fun isFollowed(secUid: String) = get(secUid) != null

    @Synchronized fun follow(author: DouyinAuthor, latest: List<String>, now: Long = System.currentTimeMillis()) {
        val existing = get(author.secUid)
        val entry = existing?.copy(nickname = author.nickname, avatar = author.avatar, douyinId = author.douyinId,
            works = author.works) ?: FollowedAuthor(author.secUid, author.nickname, author.avatar, author.douyinId,
            author.works, addedAt = now, lastChecked = now, seenIds = latest.distinct().take(SEEN_LIMIT))
        save(listOf(entry) + _authors.value.filter { it.secUid != author.secUid })
    }

    @Synchronized fun unfollow(secUid: String) = save(_authors.value.filter { it.secUid != secUid })

    @Synchronized fun update(secUid: String, transform: (FollowedAuthor) -> FollowedAuthor) =
        save(_authors.value.map { if(it.secUid == secUid) transform(it) else it })

    private fun save(list: List<FollowedAuthor>) {
        _authors.value = list
        runCatching {
            val temp = File(file.parentFile, file.name + ".tmp")
            temp.writeText(FollowCodec.encode(list))
            if(!temp.renameTo(file)) { file.writeText(temp.readText()); temp.delete() }
        }
    }
}
