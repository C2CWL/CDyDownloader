package c2cw.cdydownload

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

data class DouyinAuthor(
    val secUid: String, val nickname: String, val douyinId: String = "", val signature: String = "",
    val avatar: String = "", val followers: Long = -1, val likes: Long = -1, val works: Int = -1, val following: Int = -1
)

data class DouyinWork(
    val id: String, val title: String, val cover: String, val imageCount: Int = 0, val liveCount: Int = 0,
    val likes: Long = -1, val createTime: Long = 0, val pinned: Boolean = false
) {
    val isAlbum get() = imageCount > 0
    val url get() = "https://www.douyin.com/${if(isAlbum) "note" else "video"}/$id"
}

data class DouyinWorkPage(val works: List<DouyinWork>, val cursor: String, val hasMore: Boolean)

/** Pure parsing of Douyin profile and post responses. */
internal object DouyinAuthorParser {
    private fun firstUrl(obj: JSONObject?): String {
        val list = obj?.optJSONArray("url_list") ?: return ""
        for(i in 0 until list.length()) {
            val url = list.optString(i).replaceFirst(Regex("^http://"), "https://")
            if(url.startsWith("https://")) return url
        }
        return ""
    }
    private fun longOf(obj: JSONObject, vararg keys: String): Long {
        for(key in keys) {
            if(!obj.has(key) || obj.isNull(key)) continue
            val value = obj.opt(key)
            val parsed = when(value) {
                is Number -> value.toLong()
                is String -> value.toLongOrNull()
                else -> null
            }
            if(parsed != null && parsed >= 0) return parsed
        }
        return -1
    }

    private fun checkStatus(json: JSONObject) {
        val code = json.optInt("status_code", 0)
        check(code == 0) {
            json.optString("status_msg").takeIf { it.isNotBlank() && it.length < 60 }?.let { "抖音提示：$it" }
                ?: "抖音暂时无法访问该作者，请稍后重试"
        }
    }

    /** iesdouyin `web/api/v2/user/info` (`user_info`) or web `user/profile/other` (`user`). */
    fun profile(raw: String, secUid: String): DouyinAuthor {
        check(raw.isNotBlank()) { "抖音没有返回作者资料，请稍后重试" }
        val json = JSONObject(raw)
        checkStatus(json)
        val user = json.optJSONObject("user") ?: json.optJSONObject("user_info")
            ?: error("没有找到这个抖音作者，可能已注销或主页不公开")
        val nickname = user.optString("nickname")
        check(nickname.isNotBlank()) { "没有找到这个抖音作者，可能已注销或主页不公开" }
        val shortId = user.optString("short_id").takeIf { it.isNotBlank() && it != "0" }.orEmpty()
        return DouyinAuthor(
            secUid = user.optString("sec_uid").ifBlank { secUid },
            nickname = nickname,
            douyinId = user.optString("unique_id").ifBlank { shortId },
            signature = user.optString("signature"),
            avatar = firstUrl(user.optJSONObject("avatar_medium")).ifBlank { firstUrl(user.optJSONObject("avatar_thumb")) }
                .ifBlank { firstUrl(user.optJSONObject("avatar_larger")) },
            followers = longOf(user, "follower_count", "mplatform_followers_count"),
            likes = longOf(user, "total_favorited"),
            works = longOf(user, "aweme_count").toInt(),
            following = longOf(user, "following_count").toInt()
        )
    }

    fun work(item: JSONObject): DouyinWork? {
        val id = item.optString("aweme_id").takeIf { it.matches(Regex("[0-9]{8,22}")) } ?: return null
        val images = item.optJSONArray("images")?.takeIf { it.length() > 0 }
        val lives = images?.let { array -> (0 until array.length()).count { index ->
            val image = array.optJSONObject(index)
            image?.optJSONObject("video") != null || (image?.optInt("clip_type") ?: 0) in setOf(3, 5)
        } } ?: 0
        val video = item.optJSONObject("video")
        val cover = images?.optJSONObject(0)?.let { firstUrl(it) }.orEmpty()
            .ifBlank { firstUrl(video?.optJSONObject("cover")) }
            .ifBlank { firstUrl(video?.optJSONObject("origin_cover")) }
        return DouyinWork(
            id = id, title = item.optString("desc").trim().ifBlank { "抖音作品 $id" }, cover = cover,
            imageCount = images?.length() ?: 0, liveCount = lives,
            likes = item.optJSONObject("statistics")?.let { longOf(it, "digg_count") } ?: -1,
            createTime = item.optLong("create_time") * 1000, pinned = item.optInt("is_top") == 1
        )
    }

    fun posts(raw: String): DouyinWorkPage {
        check(raw.isNotBlank()) { "抖音暂时没有返回作品列表，可能需要验证，请稍后重试" }
        val json = JSONObject(raw)
        checkStatus(json)
        val list: JSONArray = json.optJSONArray("aweme_list") ?: JSONArray()
        val works = (0 until list.length()).mapNotNull { list.optJSONObject(it)?.let { item -> work(item) } }
        val more = when(val value = json.opt("has_more")) {
            is Boolean -> value
            is Number -> value.toInt() != 0
            else -> false
        }
        return DouyinWorkPage(works, json.opt("max_cursor")?.toString()?.takeIf { it != "null" } ?: "0", more && works.isNotEmpty())
    }
}

/** Fetches profiles over plain HTTP and posts through the signed-in web page. */
internal object DouyinAuthors {
    private const val LOGIN_HINT = "可以在「设置 › 账号」登录抖音后重试"

    suspend fun profile(secUid: String): DouyinAuthor {
        val first = runCatching { withContext(Dispatchers.IO) { DouyinAuthorParser.profile(shareProfile(secUid), secUid) } }
        first.getOrNull()?.let { return it }
        (first.exceptionOrNull() as? CancellationException)?.let { throw it }
        return try {
            DouyinAuthorParser.profile(DouyinAuthorWeb.get(
                "/aweme/v1/web/user/profile/other/?device_platform=webapp&aid=6383&channel=channel_pc_web" +
                    "&publish_video_strategy_type=2&source=channel_pc_web&sec_user_id=$secUid" +
                    "&personal_center_strategy=1&pc_client_type=1&version_code=170400&version_name=17.4.0", secUid), secUid)
        } catch(e: CancellationException) { throw e }
        catch(e: Exception) { throw IllegalStateException(friendly(e, "作者资料加载失败"), e) }
    }

    suspend fun works(secUid: String, cursor: String = "0", count: Int = 18): DouyinWorkPage = try {
        DouyinAuthorParser.posts(DouyinAuthorWeb.get(
            "/aweme/v1/web/aweme/post/?device_platform=webapp&aid=6383&channel=channel_pc_web" +
                "&sec_user_id=$secUid&max_cursor=$cursor&locate_query=false&show_live_replay_strategy=1" +
                "&need_time_list=${if(cursor == "0") 1 else 0}&time_list_query=0&whale_cut_token=&cut_version=1" +
                "&count=$count&publish_video_strategy_type=2&pc_client_type=1&version_code=170400&version_name=17.4.0", secUid))
    } catch(e: CancellationException) { throw e }
    catch(e: Exception) { throw IllegalStateException(friendly(e, "作品列表加载失败"), e) }

    private fun shareProfile(secUid: String): String {
        val request = Request.Builder().url("https://www.iesdouyin.com/web/api/v2/user/info/?sec_uid=$secUid")
            .header("User-Agent", VideoResolver.MOBILE).header("Referer", "https://www.iesdouyin.com/share/user/$secUid").build()
        return VideoResolver.client.newCall(request).execute().use {
            check(it.isSuccessful) { "服务器返回 ${it.code}" }
            it.body?.string().orEmpty()
        }
    }

    private fun friendly(e: Exception, prefix: String): String = when(e) {
        is java.net.UnknownHostException -> "网络不可用，请联网后重试"
        is java.net.SocketTimeoutException -> "$prefix：连接超时，请稍后重试"
        is org.json.JSONException -> "$prefix：抖音返回的数据格式有变化，请稍后重试"
        else -> e.message?.takeIf { it.isNotBlank() && it.any { c -> c.code > 0x2E80 } }?.let {
            if(it.contains("验证")) "$it，$LOGIN_HINT" else it
        } ?: "$prefix，请稍后重试"
    }
}
