package c2cw.cdydownload

import org.junit.Assert.*
import org.junit.Test

class MediaPreviewTest {
    private fun page(loader: String, item: String) = "<script>window._ROUTER_DATA = " +
        """{"loaderData":{"$loader":{"videoInfoRes":{"item_list":[$item]}}}}""" + ";</script>"

    @Test fun videoPreviewCarriesAuthorCoverAndEngagement() {
        val link = Links.detect("https://www.douyin.com/video/7689856421856364794")!!
        val info = DouyinPage.parse(page("video_(id)/page", """{"aweme_id":"7689856421856364794","desc":"秋天的第一杯奶茶",
            "author":{"nickname":"饼干小羊"},"statistics":{"digg_count":11000,"comment_count":186,"share_count":1108},
            "create_time":1794000000,
            "video":{"play_addr":{"url_list":["https://cdn.example.com/play.mp4"]},"cover":{"url_list":["https://cdn.example.com/cover.jpg"]}}}"""), link)
        val preview = info.toPreview()
        assertEquals(Platform.DOUYIN, preview.platform)
        assertEquals("饼干小羊", preview.author)
        assertEquals("https://cdn.example.com/cover.jpg", preview.cover)
        assertEquals(11000, preview.likes)
        assertEquals(186, preview.comments)
        assertEquals(1108, preview.shares)
        assertEquals(1794000000000L, preview.publishedAt)
        assertFalse(preview.isAlbum)
    }

    @Test fun albumPreviewFallsBackToTheFirstImageAsCover() {
        val link = Links.detect("https://www.douyin.com/note/7689856421856364794")!!
        val info = DouyinPage.parse(page("note_(id)/page", """{"aweme_id":"7689856421856364794","desc":"图集",
            "author":{"nickname":"饼干小羊"},
            "images":[{"url_list":["https://images.example.com/1.webp"]},{"url_list":["https://images.example.com/2.webp"]}],
            "video":{"play_addr":{"uri":"https://music.example.com/music.mp3"},"cover":{"url_list":["https://images.example.com/cover.jpg"]}}}"""), link)
        val preview = info.toPreview()
        assertTrue(preview.isAlbum)
        assertEquals(2, preview.images.size)
        assertEquals("https://images.example.com/1.webp", preview.cover)
        assertTrue(preview.hasMusic)
        assertEquals(-1, preview.likes)
        assertEquals(0L, preview.publishedAt)
    }

    @Test fun albumCountSurvivesTheDesktopLivePhotoSupplement() {
        val link = Links.detect("https://www.douyin.com/note/7689856421856364794")!!
        val mobile = """{"status_code":0,"aweme_details":[{"aweme_id":"7689856421856364794","desc":"图集","author":{"nickname":"饼干小羊"},
            "images":[{"uri":"a","url_list":["https://images.example.com/1.webp"]}],"statistics":{"digg_count":42},
            "video":{"play_addr":{"uri":"https://music.example.com/music.mp3"}}}]}"""
        val desktop = """{"awemeId":"7689856421856364794","images":[{"uri":"a","livePhotoType":0,"clipType":0,"urlList":["https://images.example.com/1.webp"]}]}"""
        val preview = DouyinPage.supplementDesktop(mobile, desktop, link).toPreview()
        assertEquals("饼干小羊", preview.author)
        assertEquals(42, preview.likes)
        assertEquals("https://images.example.com/1.webp", preview.cover)
    }
}
