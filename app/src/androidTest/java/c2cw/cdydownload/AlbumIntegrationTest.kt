package c2cw.cdydownload

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test

class AlbumIntegrationTest {
    @Test fun perItemOutputSurvivesReloadAndUpdatesDownloadClassification() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "album-output-${System.nanoTime()}"
        try {
            val store = DownloadStore(context, name)
            val link = Links.detect("https://www.douyin.com/note/7689856421856364997")!!
            val outputs = mapOf(0 to AlbumOutput.STATIC, 1 to AlbumOutput.VIDEO, 2 to AlbumOutput.MOTION)
            val task = store.add(link, albumContent = AlbumContent.IMAGES, albumOutputs = outputs)
            val restored = DownloadStore(context, name).get(task.id)!!
            assertEquals(outputs, restored.albumOutputs)
            val info = VideoInfo(link, link.key, "Mixed album", "", referer = "", userAgent = "",
                images = List(3) { "https://example.com/$it.jpg" },
                imageVideos = List(3) { "https://example.com/$it.mp4" })
            assertTrue(store.markResolved(task.id, info))
            assertEquals("1 张静图 · 1 段动态视频 · 1 张实况照片", store.get(task.id)!!.quality)
        } finally { context.getSharedPreferences(name, 0).edit().clear().commit() }
    }

    @Test fun legacyVideoRequestResolvesToOriginalImagesAndMusic() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "album-originals-${System.nanoTime()}"
        try {
            val store = DownloadStore(context, name)
            val link = Links.detect("https://www.douyin.com/note/7689856421856364997")!!
            val task = store.add(link, AlbumMode.VIDEO)
            val info = VideoInfo(link, link.key, "Original album", "", referer = "", userAgent = "",
                images = listOf("https://example.com/original.webp"), music = "https://example.com/music.mp3")
            assertTrue(store.markResolved(task.id, info))
            assertEquals(AlbumMode.IMAGES, store.get(task.id)!!.albumMode)
            assertTrue(store.get(task.id)!!.quality.contains("静图 · 配乐"))
            assertEquals(AlbumMode.IMAGES, DownloadStore(context, name).get(task.id)!!.albumMode)
        } finally { context.getSharedPreferences(name, 0).edit().clear().commit() }
    }
}
