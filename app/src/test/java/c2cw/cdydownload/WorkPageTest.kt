package c2cw.cdydownload

import org.junit.Assert.assertEquals
import org.junit.Test

class WorkPageTest {
    private fun album(hasMusic: Boolean, videos: List<String?> = emptyList()) = MediaPreview(
        platform = Platform.DOUYIN,
        title = "图集",
        author = "示例作者",
        cover = "https://example.com/cover.jpg",
        images = List(videos.size.coerceAtLeast(2)) { "https://example.com/$it.jpg" },
        quality = "",
        hasMusic = hasMusic,
        imageVideos = videos.ifEmpty { listOf("https://example.com/0.mp4", null) }
    )

    @Test fun contentLabelsUseJinInsteadOfZhiYao() {
        assertEquals("图片和配乐", albumContentLabel(AlbumContent.ALL, hasMusic = true))
        assertEquals("下载选中", albumContentLabel(AlbumContent.ALL, hasMusic = false))
        assertEquals("仅图片", albumContentLabel(AlbumContent.IMAGES, hasMusic = true))
        assertEquals("仅配乐", albumContentLabel(AlbumContent.MUSIC, hasMusic = true))
    }

    @Test fun selectedDownloadButtonMentionsLivePhotosAndMusic() {
        val preview = album(hasMusic = true, videos = listOf("https://example.com/0.mp4", "https://example.com/1.mp4"))
        val outputs = mapOf(0 to AlbumOutput.MOTION, 1 to AlbumOutput.STATIC)
        assertEquals("下载选中（2 张，其中 1 张存实况）+ 配乐",
            albumDownloadLabel(preview, AlbumContent.ALL, listOf(0, 1), outputs))
        assertEquals("下载选中（1 张）",
            albumDownloadLabel(preview, AlbumContent.IMAGES, listOf(1), outputs))
        assertEquals("只下载配乐", albumDownloadLabel(preview, AlbumContent.MUSIC, emptyList(), outputs))
    }

    @Test fun singleItemNoticeFollowsTheCurrentLiveOrStillChoice() {
        assertEquals("已开始下载这张实况", albumItemDownloadNotice(AlbumOutput.MOTION))
        assertEquals("已开始下载这张静图", albumItemDownloadNotice(AlbumOutput.STATIC))
        assertEquals("已开始下载这段视频", albumItemDownloadNotice(AlbumOutput.VIDEO))
    }
}
