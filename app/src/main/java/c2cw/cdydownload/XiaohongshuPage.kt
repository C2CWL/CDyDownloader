package c2cw.cdydownload

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONObject

internal object XiaohongshuPage {
    fun parse(page: String, link: VideoLink, ua: String, selection: TrackSelection? = null): VideoInfo {
        val raw = Regex("""window\.__INITIAL_STATE__\s*=\s*(.*?)</script>""", RegexOption.DOT_MATCHES_ALL)
            .find(page)?.groupValues?.get(1)?.trim()?.removeSuffix(";")
            ?: error("小红书未返回笔记内容，请复制完整分享链接或在设置中登录后重试")
        // Replace undefined only outside JSON strings; captions and signed URLs remain untouched.
        val normalized = Regex(""""(?:\\.|[^"\\])*"|\bundefined\b""").replace(raw) {
            if(it.value == "undefined") "null" else it.value
        }
        val state = JSONObject(normalized)
        val id = link.key.removePrefix("xhs:")
        val note = state.optJSONObject("note")?.optJSONObject("noteDetailMap")?.optJSONObject(id)?.optJSONObject("note")
            ?: state.optJSONObject("noteData")?.optJSONObject("data")?.optJSONObject("noteData")
            ?: error("笔记不存在、已删除或当前不可访问")
        check(note.optString("noteId").ifBlank { note.optString("id") } == id) { "小红书返回的作品与分享链接不一致" }
        val title = note.optString("title").ifBlank { note.optString("desc") }.ifBlank { "小红书笔记 $id" }
        val user = note.optJSONObject("user")
        val common = VideoInfo(link, link.key, title, "", referer = "https://www.xiaohongshu.com/", userAgent = ua,
            author = user?.optString("nickname").orEmpty().ifBlank { user?.optString("nickName").orEmpty() },
            publishedAt = note.optLong("time"))
        if(note.optString("type") == "video") {
            val video = note.optJSONObject("video") ?: error("笔记缺少视频资源")
            val media = video.optJSONObject("media") ?: video
            val urls = streams(media).flatMap(::addresses).distinct()
            check(urls.isNotEmpty()) { "小红书没有返回可下载的视频" }
            check(selection == null || selection == TrackSelection("xhs:combined")) { "所选规格已不可用，请重新选择" }
            return common.copy(video = urls.first(), videoFallbacks = urls.drop(1), quality = "原始视频",
                specifications = MediaSpecifications(listOf(TrackOption("xhs:combined", "原始视频", hasAudio = true)), emptyList(), TrackSelection("xhs:combined")))
        }
        val images = note.optJSONArray("imageList") ?: error("笔记没有图片资源")
        check(images.length() > 0) { "笔记没有图片资源" }
        val originals = (0 until images.length()).map { images.getJSONObject(it) }
        val urls = originals.mapIndexed { index, image ->
            mediaUrl(image.optString("urlDefault")) ?: mediaUrl(image.optString("url"))
                ?: error("第 ${index + 1} 张图片缺少资源")
        }
        val motion = originals.mapIndexed { index, image ->
            val video = streams(image).flatMap(::addresses).firstOrNull()
            check(!image.optBoolean("livePhoto") || video != null) { "第 ${index + 1} 张实况图缺少动态资源，不会降级保存为静态图" }
            video
        }
        return common.copy(images = urls, imageVideos = motion, separateAlbumMusic = true,
            cover = urls.first(), quality = "${urls.size} 张图片")
    }

    private fun streams(media: JSONObject): List<JSONObject> {
        val stream = media.optJSONObject("stream") ?: return emptyList()
        return listOf("h264", "h265", "av1").flatMap { codec ->
            val array = stream.optJSONArray(codec)
            (0 until (array?.length() ?: 0)).mapNotNull { array!!.optJSONObject(it) }
        }.sortedByDescending { it.optLong("width") * it.optLong("height") }
    }

    private fun addresses(stream: JSONObject): List<String> = buildList {
        mediaUrl(stream.optString("masterUrl"))?.let { add(it) }
        val backup = stream.optJSONArray("backupUrls")
        for(i in 0 until (backup?.length() ?: 0)) mediaUrl(backup!!.optString(i))?.let { add(it) }
    }

    private fun mediaUrl(raw: String): String? {
        val url = (if(raw.startsWith("//")) "https:$raw" else raw).toHttpUrlOrNull() ?: return null
        if(url.username.isNotEmpty() || url.password.isNotEmpty() || url.port !in setOf(80, 443) ||
            !listOf("xhscdn.com", "rednotecdn.com", "xiaohongshu.com").any { url.host == it || url.host.endsWith(".$it") }) return null
        return url.newBuilder().scheme("https").build().toString()
    }
}
