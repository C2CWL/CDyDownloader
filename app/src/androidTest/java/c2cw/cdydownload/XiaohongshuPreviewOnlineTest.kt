package c2cw.cdydownload

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

class XiaohongshuPreviewOnlineTest {
    @Test fun sharedAlbumDecodesCoverAndThumbnail() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("xhsPreviewOnline") == "true")
        val link = Links.detect("https://xhslink.cn/o/6d7k1zFbr66")!!
        val preview = VideoResolver().resolve(link).toPreview()
        assertEquals(18, preview.images.size)
        assertEquals("https://www.xiaohongshu.com/", preview.referer)
        listOf(preview.cover, preview.images[1]).forEach { url ->
            val bitmap = PreviewImages.load(url, preview.referer, preview.userAgent)
            assertNotNull("Preview decode failed", bitmap)
            assertTrue(bitmap!!.width > 0 && bitmap.height > 0)
        }
    }
}
