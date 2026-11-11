package c2cw.cdydownload

import java.net.URI
import java.net.URLDecoder

/** Douyin user homepages. Only the stable sec_uid is kept; the numeric uid is not usable on the web. */
internal object DouyinUserLinks {
    private val urls = Regex("""(?i)(?:https?://)?(?:[a-z0-9-]+\.)+(?:com|cn)/[^\s<>"\[\](){}，。！？、【】（）]+""")
    private val secUid = Regex("""[A-Za-z0-9_\-]{16,120}""")
    private val hosts = setOf("douyin.com", "www.douyin.com", "m.douyin.com", "iesdouyin.com", "www.iesdouyin.com")

    fun profileUrl(sec: String) = "https://www.douyin.com/user/$sec"

    /** sec_uid of a full user homepage URL, or null for anything else (including short links). */
    fun secUidFromUrl(raw: String): String? = runCatching {
        val uri = URI(if(raw.startsWith("http", true)) raw else "https://$raw")
        val host = uri.host?.lowercase() ?: return null
        if(host !in hosts) return null
        val path = uri.path.orEmpty().trimEnd('/')
        val query = uri.rawQuery.orEmpty().split("&").mapNotNull {
            val pair = it.split("=", limit = 2)
            if(pair.size == 2) URLDecoder.decode(pair[0], "UTF-8") to URLDecoder.decode(pair[1], "UTF-8") else null
        }.toMap()
        val fromQuery = query["sec_uid"]?.takeIf { secUid.matches(it) }
        val user = Regex("""^/(?:share/)?user/([^/]+)$""").matchEntire(path)?.groupValues?.get(1) ?: return null
        // A work opened from a profile (modal_id) is a work link, not a profile.
        if(query.containsKey("modal_id")) return null
        fromQuery ?: user.takeIf { secUid.matches(it) && it != "self" }
    }.getOrNull()

    /** A pasted text that contains a full Douyin user homepage link. */
    fun detect(text: String): String? = urls.findAll(text.take(16000)).firstNotNullOfOrNull {
        secUidFromUrl(it.value.trimEnd('.', ',', ';', '!', '?', '。', '，'))
    }
}

/** Thrown while expanding a share short link that turns out to be a user homepage. */
internal class DouyinUserRedirect(val secUid: String) : Exception("这是抖音作者主页链接")
