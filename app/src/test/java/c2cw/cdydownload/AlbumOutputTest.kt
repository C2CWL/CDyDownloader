package c2cw.cdydownload

import org.junit.Assert.assertEquals
import org.junit.Test

class AlbumOutputTest {
    private val videos = listOf(null, "https://example.com/1.mp4", "https://example.com/2.mp4")

    @Test fun staticImagesNeverBecomeVideoAndLegacyLivePhotosKeepTheirDefault() {
        assertEquals(AlbumOutput.STATIC, albumOutput(0, videos, mapOf(0 to AlbumOutput.VIDEO)))
        assertEquals(AlbumOutput.STATIC, albumOutput(4, videos, emptyMap()))
        assertEquals(AlbumOutput.MOTION, albumOutput(1, videos, emptyMap()))
        assertEquals(AlbumOutput.VIDEO, albumOutput(1, videos, mapOf(1 to AlbumOutput.VIDEO)))
        assertEquals(AlbumOutput.STATIC, albumOutput(1, videos, mapOf(1 to AlbumOutput.STATIC)))
    }

    @Test fun mixedSelectionIsCountedByOutputRatherThanCalledImages() {
        assertEquals("1 张静图 · 1 段动态视频 · 1 张实况照片",
            albumSummary(listOf(0, 1, 2), videos, mapOf(1 to AlbumOutput.VIDEO)))
        assertEquals("1 段动态视频", albumSummary(listOf(2), videos, mapOf(2 to AlbumOutput.VIDEO)))
        assertEquals("0 张静图", albumSummary(emptyList(), videos, emptyMap()))
    }
}
