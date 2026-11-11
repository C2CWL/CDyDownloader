package c2cw.cdydownload

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DouyinAuthorTest {
    private val sec = "MS4wLjABAAAA8U_l6rBzmy7bcy6xOJel4v0RzoR_wfAubGPeJimN__4"

    @Test fun userLinksAreRecognized() {
        assertEquals(sec, DouyinUserLinks.secUidFromUrl("https://www.douyin.com/user/$sec"))
        assertEquals(sec, DouyinUserLinks.secUidFromUrl("https://www.douyin.com/user/$sec?from_tab_name=main"))
        assertEquals(sec, DouyinUserLinks.secUidFromUrl("https://www.iesdouyin.com/share/user/1234567?sec_uid=$sec&u_code=x"))
        assertEquals(sec, DouyinUserLinks.detect("长按复制此条消息，打开抖音搜索，查看TA的更多作品。 https://www.douyin.com/user/$sec"))
        assertNull(DouyinUserLinks.secUidFromUrl("https://www.douyin.com/user/self"))
        assertNull(DouyinUserLinks.secUidFromUrl("https://www.douyin.com/user/$sec?modal_id=7689856421856364794"))
        assertNull(DouyinUserLinks.secUidFromUrl("https://www.douyin.com/video/7689856421856364794"))
        assertNull(DouyinUserLinks.secUidFromUrl("https://example.com/user/$sec"))
        assertNull(DouyinUserLinks.detect("https://v.douyin.com/abcdEFG/"))
        // Work links are still handled by the normal resolver.
        assertNull(Links.detect("https://www.douyin.com/user/$sec"))
    }

    @Test fun shareProfileParses() {
        val raw = """{"status_code":0,"user_info":{"short_id":"0","nickname":"人民日报","signature":"参与、沟通、记录时代。",
            "avatar_thumb":{"url_list":["https://p11.douyinpic.com/aweme/100x100/a.jpeg"]},"aweme_count":12674,"following_count":30,
            "total_favorited":"19313864808","unique_id":"rmrbxmt","mplatform_followers_count":203668000,"sec_uid":"$sec"}}"""
        val author = DouyinAuthorParser.profile(raw, sec)
        assertEquals("人民日报", author.nickname)
        assertEquals("rmrbxmt", author.douyinId)
        assertEquals(203668000L, author.followers)
        assertEquals(19313864808L, author.likes)
        assertEquals(12674, author.works)
        assertEquals(30, author.following)
        assertEquals("https://p11.douyinpic.com/aweme/100x100/a.jpeg", author.avatar)
        assertEquals("2亿", compactCount(author.followers))
        assertEquals("1.5万", compactCount(15000))
        assertEquals("424", compactCount(424))
    }

    @Test fun webProfileParsesAndErrorsAreFriendly() {
        val raw = """{"status_code":0,"user":{"nickname":"示例","short_id":"123456789","follower_count":15,"total_favorited":424,"aweme_count":3,"following_count":5}}"""
        val author = DouyinAuthorParser.profile(raw, sec)
        assertEquals("123456789", author.douyinId)
        assertEquals(sec, author.secUid)
        val error = runCatching { DouyinAuthorParser.profile("""{"status_code":2053,"status_msg":"用户不存在"}""", sec) }.exceptionOrNull()
        assertEquals("抖音提示：用户不存在", error?.message)
        assertTrue(runCatching { DouyinAuthorParser.profile("", sec) }.isFailure)
    }

    @Test fun postsParse() {
        val raw = """{"status_code":0,"max_cursor":1700000000000,"has_more":1,"aweme_list":[
            {"aweme_id":"7689856421856364794","desc":"图集标题","create_time":1794000000,"is_top":1,"statistics":{"digg_count":14},
             "images":[{"url_list":["https://p3.douyinpic.com/1.webp"],"video":{"play_addr":{}}},{"url_list":["https://p3.douyinpic.com/2.webp"]}]},
            {"aweme_id":"7689856421856364795","desc":"","video":{"cover":{"url_list":["http://p3.douyinpic.com/c.jpeg"]}},"images":null},
            {"aweme_id":"bad"}]}"""
        val page = DouyinAuthorParser.posts(raw)
        assertEquals(2, page.works.size)
        assertTrue(page.hasMore)
        assertEquals("1700000000000", page.cursor)
        val album = page.works[0]
        assertTrue(album.isAlbum); assertTrue(album.pinned)
        assertEquals(2, album.imageCount); assertEquals(1, album.liveCount); assertEquals(14L, album.likes)
        assertEquals("https://www.douyin.com/note/7689856421856364794", album.url)
        val video = page.works[1]
        assertFalse(video.isAlbum)
        assertEquals("抖音作品 7689856421856364795", video.title)
        assertEquals("https://p3.douyinpic.com/c.jpeg", video.cover)
        assertEquals("https://www.douyin.com/video/7689856421856364795", video.url)
        assertEquals("dy:7689856421856364794", Links.fromUrl(album.url)?.key)
    }

    @Test fun emptyPostsStopPaging() {
        val page = DouyinAuthorParser.posts("""{"status_code":0,"aweme_list":[],"has_more":true,"max_cursor":0}""")
        assertFalse(page.hasMore)
        assertTrue(runCatching { DouyinAuthorParser.posts("") }.isFailure)
    }
}
