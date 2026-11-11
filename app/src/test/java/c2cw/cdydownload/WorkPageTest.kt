package c2cw.cdydownload

import org.junit.Assert.assertEquals
import org.junit.Test

class WorkPageTest {
    private fun album(hasMusic: Boolean, imageVideos: List<String?>) = MediaPreview(
        platform = Platform.DOUYIN,
        title = "图集",
        author = "示例作者",
        cover = "https://example.com/cover.jpg",
        images = imageVideos.indices.map { "https://example.com/$it.jpg" },
        quality = "",
        hasMusic = hasMusic,
        likes = -1,
        comments = -1,
        shares = -1,
        publishedAt = 0,
        referer = "https://www.douyin.com/",
        userAgent = "test",
        imageVideos = imageVideos
    )

    @Test fun contentLabelsUseJinInsteadOfZhiYao() {
        assertEquals("图片和配乐", albumContentLabel(AlbumContent.ALL, hasMusic = true))
        assertEquals("下载选中", albumContentLabel(AlbumContent.ALL, hasMusic = false))
        assertEquals("仅图片", albumContentLabel(AlbumContent.IMAGES, hasMusic = true))
        assertEquals("仅配乐", albumContentLabel(AlbumContent.MUSIC, hasMusic = true))
    }

    @Test fun selectedDownloadButtonMentionsLivePhotosAndMusic() {
        val videos = listOf("https://example.com/0.mp4", "https://example.com/1.mp4")
        val preview = album(hasMusic = true, imageVideos = videos)
        assertEquals(videos, preview.imageVideos)
        assertEquals(true, preview.hasMusic)
        val outputs = mapOf(0 to AlbumOutput.MOTION, 1 to AlbumOutput.STATIC)
        val all = albumDownloadLabel(preview, AlbumContent.ALL, listOf(0, 1), outputs)
        val imagesOnly = albumDownloadLabel(preview, AlbumContent.IMAGES, listOf(1), outputs)
        val musicOnly = albumDownloadLabel(preview, AlbumContent.MUSIC, emptyList(), outputs)
        assertEquals("all was: $all", "下载选中（2 张，其中 1 张存实况）+ 配乐", all)
        assertEquals("images was: $imagesOnly", "下载选中（1 张）", imagesOnly)
        assertEquals("music was: $musicOnly", "只下载配乐", musicOnly)
    }

    @Test fun singleItemNoticeFollowsTheCurrentLiveOrStillChoice() {
        assertEquals("已开始下载这张实况", albumItemDownloadNotice(AlbumOutput.MOTION))
        assertEquals("已开始下载这张静图", albumItemDownloadNotice(AlbumOutput.STATIC))
        assertEquals("已开始下载这段视频", albumItemDownloadNotice(AlbumOutput.VIDEO))
    }
}
