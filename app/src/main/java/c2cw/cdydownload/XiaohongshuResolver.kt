package c2cw.cdydownload

import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

internal class XiaohongshuResolver(private val trackCall: (Call) -> Unit) {
    private val client = OkHttpClient.Builder().cookieJar(PlatformCookieJar())
        .connectTimeout(12, TimeUnit.SECONDS).readTimeout(25, TimeUnit.SECONDS).callTimeout(40, TimeUnit.SECONDS)
        .addNetworkInterceptor { chain ->
            val url = chain.request().url
            check(url.isHttps && url.username.isEmpty() && url.password.isEmpty() && url.port == 443 &&
                (url.host in setOf("xhslink.com", "www.xhslink.com", "xhslink.cn", "www.xhslink.cn", "rednote.com", "www.rednote.com", "xiaohongshu.com", "www.xiaohongshu.com", "www.xhs.cn"))) {
                "小红书链接跳转到了不支持的网站"
            }
            chain.proceed(chain.request())
        }.build()

    fun resolve(link: VideoLink, selection: TrackSelection?): VideoInfo {
        val ua = if(link.key.startsWith("xhs-short:") || link.url.contains("app_platform=")) VideoResolver.MOBILE else VideoResolver.DESKTOP
        val request = Request.Builder().url(link.url).header("User-Agent", ua)
            .header("Referer", "https://www.xiaohongshu.com/").build()
        return client.newCall(request).also(trackCall).execute().use { response ->
            check(response.isSuccessful) { "小红书返回 ${response.code}，请重新复制完整分享链接或在设置中登录后重试" }
            val actual = Links.fromUrl(response.request.url.toString())
                ?.takeIf { it.platform == Platform.XIAOHONGSHU && it.key.startsWith("xhs:") }
                ?: error("小红书要求登录或验证，请在设置中登录后重新复制分享链接")
            check(link.key.startsWith("xhs-short:") || actual.key == link.key) { "小红书返回的作品与分享链接不一致" }
            val bytes = response.body?.byteStream()?.readNBytes(6 * 1024 * 1024 + 1) ?: error("小红书未返回页面")
            check(bytes.size <= 6 * 1024 * 1024) { "小红书返回内容过大" }
            XiaohongshuPage.parse(bytes.toString(Charsets.UTF_8), actual, ua, selection)
        }
    }
}
