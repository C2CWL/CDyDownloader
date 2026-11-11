package c2cw.cdydownload

import org.junit.Assert.*
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FileNameTemplateTest {

    @Test
    fun defaultTemplateFormatsCorrectly() {
        val dateMillis = 1700000000000L
        val dateStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(dateMillis))
        val result = formatFileName(
            template = null,
            title = "精彩视频",
            id = "123456789012345",
            author = "测试作者",
            platform = Platform.DOUYIN,
            dateMillis = dateMillis,
            extension = "mp4"
        )
        assertEquals("$dateStr-123456789012.mp4", result)
        assertTrue(NAMING_TEMPLATE_PRESETS.none { it.first.contains("{title}") })
    }

    @Test
    fun customTemplatesSupportAllVariables() {
        val dateMillis = 1700000000000L
        val dateStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(dateMillis))

        // Author and title template
        val authorTitle = formatFileName(
            template = "{author} - {title}",
            title = "风景美图",
            id = "note123",
            author = "摄影师小明",
            platform = Platform.XIAOHONGSHU,
            dateMillis = dateMillis,
            extension = "jpg"
        )
        assertEquals("摄影师小明 - 风景美图.jpg", authorTitle)

        // Platform and date template
        val platDate = formatFileName(
            template = "[{platform}] {date}_{title}",
            title = "热门视频",
            id = "vid456",
            author = "up主",
            platform = Platform.BILI,
            dateMillis = dateMillis,
            extension = "mp4"
        )
        assertEquals("[B 站] ${dateStr}_热门视频.mp4", platDate)
    }

    @Test
    fun motionAndIndexSuffixesArePreserved() {
        val livePhoto = formatFileName(
            template = "{title}",
            title = "实况图集",
            id = "img001",
            author = "用户",
            platform = Platform.XIAOHONGSHU,
            motion = true,
            indexSuffix = "1",
            extension = "mp4"
        )
        assertEquals("实况图集-1_MP.mp4", livePhoto)
    }

    @Test
    fun illegalCharactersAndByteLimitsAreSanitized() {
        val dirty = formatFileName(
            template = "{title}",
            title = "非法字符 /:*?\"<>| 标题",
            id = "cleanId",
            extension = "mp4"
        )
        assertFalse(dirty.contains("/"))
        assertFalse(dirty.contains(":"))
        assertFalse(dirty.contains("?"))
        assertFalse(dirty.contains("\""))
        assertTrue(dirty.endsWith(".mp4"))

        val veryLong = formatFileName(
            template = "{title}",
            title = "长标题".repeat(100),
            id = "id",
            extension = "mp4"
        )
        assertTrue(veryLong.toByteArray(Charsets.UTF_8).size <= 255)
    }
}
