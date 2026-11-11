package c2cw.cdydownload

import org.junit.Assert.assertEquals
import org.junit.Test

class SaveFolderTest {
    @Test fun eachFolderIsIndependentAndBlankMeansNoSubfolder() {
        assertEquals("", optionalSaveFolder("  "))
        assertEquals("旅行下一级", optionalSaveFolder(" 旅行/下一级\\ "))
        assertEquals("Download", mediaRelativePath("Download", ""))
        assertEquals("Pictures/相册", mediaRelativePath("Pictures", "相册"))
        assertEquals("Music", mediaLocationLabel("Music", ""))
        assertEquals("Download / 视频", mediaLocationLabel("Download", "视频"))
    }

    @Test fun unsetFolderKeepsTheDefaultUntilAChoiceIsStored() {
        assertEquals("CDy下载器", resolveSaveFolder(hasNew = false, stored = null, hasLegacy = false, legacy = null))
        assertEquals("CDy下载器", resolveSaveFolder(hasNew = false, stored = null, hasLegacy = true, legacy = "CDy下载器"))
        assertEquals("", resolveSaveFolder(hasNew = true, stored = "", hasLegacy = true, legacy = "CDy下载器"))
        assertEquals("仅视频", resolveSaveFolder(hasNew = true, stored = "仅视频", hasLegacy = true, legacy = "旧目录"))
    }
}
