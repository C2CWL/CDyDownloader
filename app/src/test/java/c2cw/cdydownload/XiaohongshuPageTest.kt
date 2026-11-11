package c2cw.cdydownload

import org.junit.Assert.*
import org.junit.Test

class XiaohongshuPageTest {
    private val id = "65abcdef0123456789abcdef"
    private val link = Links.detect("https://www.xiaohongshu.com/explore/$id?xsec_token=abc%2Bdef&xsec_source=pc_share")!!
    private fun page(body: String) = "<script>window.__INITIAL_STATE__={\"note\":{\"noteDetailMap\":{\"$id\":{\"note\":$body}}}};</script>"
    private fun note(images: String) = """{"noteId":"$id","type":"normal","title":"undefined","extra":undefined,"imageList":$images}"""

    @Test fun retainsSignedLinksAndRecognizesShortLinks() {
        assertTrue(link.url.contains("xsec_token=abc%2Bdef"))
        assertEquals("xhs-short:/a/Abc123", Links.detect("分享 http://xhslink.com/a/Abc123")?.key)
        assertEquals(link.key, Links.detect("https://www.xiaohongshu.com/discovery/item/$id?xsec_token=test")?.key)
        assertNull(Links.detect("https://www.xiaohongshu.com.evil.com/explore/$id"))
        assertNull(Links.detect("https://user@www.xiaohongshu.com/explore/$id"))
    }
    @Test fun pairsLiveResourcesWithoutMakingAlbumVideo() {
        val info = XiaohongshuPage.parse(page(note("""[
            {"urlDefault":"https://sns-img.xhscdn.com/still.webp"},
            {"urlDefault":"https://sns-img.xhscdn.com/live.jpg","livePhoto":true,
             "stream":{"h264":[{"masterUrl":"https://sns-video.xhscdn.com/live.mp4?sig=original"}]}}
        ]""")), link, "test")
        assertEquals("undefined", info.title)
        val preview = info.toPreview()
        assertEquals("https://www.xiaohongshu.com/", preview.referer)
        assertEquals("test", preview.userAgent)
        assertEquals(info.images.first(), preview.cover)
        assertEquals("", info.video)
        assertEquals(2, info.images.size)
        assertEquals(listOf(null, "https://sns-video.xhscdn.com/live.mp4?sig=original"), info.imageVideos)
    }
    @Test fun failsRatherThanSilentlyDiscardingLiveMotion() {
        val result = runCatching { XiaohongshuPage.parse(page(note("""[{"urlDefault":"https://sns-img.xhscdn.com/live.jpg","livePhoto":true}]""")), link, "test") }
        assertTrue(result.exceptionOrNull()?.message.orEmpty().contains("缺少动态资源"))
    }
    @Test fun retainsGifUrlAndRejectsForeignMedia() {
        val info = XiaohongshuPage.parse(page(note("""[{"urlDefault":"https://sns-img.xhscdn.com/animated.gif?sig=original"}]""")), link, "test")
        assertTrue(info.images.single().endsWith("animated.gif?sig=original"))
        assertTrue(runCatching { XiaohongshuPage.parse(page(note("""[{"urlDefault":"https://evil.com/image.jpg"}]""")), link, "test") }.isFailure)
    }
    @Test fun resolvesVideoSeparatelyAndRejectsDifferentWork() {
        val body = """{"noteId":"$id","type":"video","video":{"media":{"stream":{"h264":[{"masterUrl":"https://sns-video.xhscdn.com/video.mp4"}]}}}}"""
        val info = XiaohongshuPage.parse(page(body), link, "test")
        assertTrue(info.images.isEmpty())
        assertEquals("https://sns-video.xhscdn.com/video.mp4", info.video)
        assertTrue(runCatching { XiaohongshuPage.parse(page(body.replace(id, "000000000000000000000000")), link, "test") }.isFailure)
    }
    @Test fun recognizesCnShortLinksAndRednoteLandingPage() {
        assertEquals("xhs-short:/o/3Ye61YUiFNk", Links.detect("https://xhslink.cn/o/3Ye61YUiFNk")?.key)
        val actual = Links.detect("https://www.rednote.com/discovery/item/$id?app_platform=android&xsec_token=test")!!
        assertEquals(link.key, actual.key)
        assertTrue(actual.url.startsWith("https://www.rednote.com/"))
        assertTrue(actual.url.contains("xsec_token=test"))
        assertNull(Links.detect("https://rednote.com.evil.com/discovery/item/$id"))
    }
    @Test fun parsesMobileLivePhotoShapeReturnedBySharedSample() {
        val body = """{"noteId":"$id","type":"normal","title":"实况图集","imageList":[
          {"url":"http://sns-web-i10.rednotecdn.com/sample!rnh5jpg","livePhoto":true,
           "stream":{"h264":[{"width":1080,"height":1440,"audioChannels":0,
           "masterUrl":"http://sns-v27.rednotecdn.com/stream/sample.mp4"}]}}
        ]}"""
        val mobile = "<script>window.__INITIAL_STATE__={\"noteData\":{\"data\":{\"noteData\":$body}}};</script>"
        val info = XiaohongshuPage.parse(mobile, link, "test")
        assertEquals("", info.video)
        assertEquals("https://sns-web-i10.rednotecdn.com/sample!rnh5jpg", info.images.single())
        assertEquals("https://sns-v27.rednotecdn.com/stream/sample.mp4", info.imageVideos.single())
    }

}
