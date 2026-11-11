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

class SaveTreeLabelTest {
    @Test fun treeDocumentIdsBecomeReadableLabels() {
        assertEquals("内部存储 / Pictures / 旅行", treeDocumentLabel("primary:Pictures/旅行"))
        assertEquals("内部存储", treeDocumentLabel("primary:"))
        assertEquals("存储卡 / DCIM", treeDocumentLabel("1A2B-3C4D:DCIM"))
        assertEquals(null, treeDocumentLabel("raw"))
    }

    @Test fun pickedFolderWinsOverTypedName() {
        assertEquals("内部存储 / Movies", saveLocationLabel(SaveKind.VIDEO, "旧名称", "content://tree") { "内部存储 / Movies" })
        assertEquals("所选文件夹", saveLocationLabel(SaveKind.VIDEO, "旧名称", "content://tree") { null })
        assertEquals("Pictures / 旧名称", saveLocationLabel(SaveKind.IMAGE, "旧名称", "") { "unused" })
        assertEquals("Music", saveLocationLabel(SaveKind.MUSIC, "", "") { "unused" })
    }
}
