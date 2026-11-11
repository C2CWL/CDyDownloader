package c2cw.cdydownload

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class HomePasteTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    @Test fun linkPanelStaysVisibleWithoutAutofocusAndPasteDoesNotStartDownload() {
        val vm = ViewModelProvider(rule.activity)[MainViewModel::class.java]
        val clipboard = rule.activity.getSystemService(ClipboardManager::class.java)
        val original = clipboard.primaryClip
        val enabled = vm.clipboardEnabled
        val text = "测试分享内容"
        val count = vm.store.tasks.value.size
        try {
            rule.runOnIdle {
                vm.setClipboard(false); vm.inputVisible = false; vm.settings = false; vm.tab = 0
                clipboard.clearPrimaryClip()
            }
            rule.onNodeWithTag("downloadLinkInput").assertIsDisplayed().assertIsNotFocused()
            rule.onNodeWithText("添加下载链接").assertDoesNotExist()
            rule.onNodeWithText("收起").assertDoesNotExist()
            rule.onNodeWithTag("resolveThenDownload").assertIsNotEnabled()
            rule.runOnIdle { clipboard.setPrimaryClip(ClipData.newPlainText("test", text)) }
            rule.onNodeWithTag("fillClipboardLink").performClick()
            rule.onNodeWithTag("downloadLinkInput").assertTextEquals(text)
            assertEquals(count, vm.store.tasks.value.size)
            rule.runOnIdle { vm.inputVisible = false; clipboard.clearPrimaryClip() }
            rule.onNodeWithTag("downloadLinkInput").assertIsDisplayed().assertTextEquals(text)
            rule.runOnIdle { vm.tab = TAB_DOWNLOADS }
            rule.waitForIdle()
            rule.runOnIdle { vm.tab = 0 }
            rule.onNodeWithTag("downloadLinkInput").assertIsDisplayed().assertTextEquals(text)
            rule.onNodeWithText("收起").assertDoesNotExist()
        } finally {
            rule.runOnIdle {
                vm.inputVisible = false
                if(original != null) clipboard.setPrimaryClip(original) else clipboard.clearPrimaryClip()
                vm.setClipboard(enabled)
            }
        }
    }
    private fun screenshot(name: String) {
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(rule.activity.getExternalFilesDir(null), name).outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
