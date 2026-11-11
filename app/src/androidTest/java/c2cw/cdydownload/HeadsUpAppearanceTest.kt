package c2cw.cdydownload

import android.app.NotificationManager
import android.content.ClipboardManager
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HeadsUpAppearanceTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test fun clipboardResolutionStaysInlineAndRequiresPreviewBeforeDownload() {
        val vm = ViewModelProvider(rule.activity)[MainViewModel::class.java]
        val clipboard = rule.activity.getSystemService(ClipboardManager::class.java)
        val original = clipboard.primaryClip
        val enabled = vm.clipboardEnabled
        val link = Links.detect("https://www.douyin.com/video/7690882615418703138")!!
        val count = vm.store.tasks.value.size
        try {
            rule.runOnIdle {
                vm.setClipboard(false); clipboard.clearPrimaryClip()
                vm.settings = false; vm.inputVisible = false; vm.tab = 0
            }
            rule.runOnIdle {
                vm.setClipboard(true)
                vm.clipboard.state = ClipboardPrompt.Resolving(link)
            }
            rule.onNodeWithTag("inlineClipboardSuggestion").assertIsDisplayed()
            rule.onNodeWithText("正在解析链接，请稍候…").assertIsDisplayed()
            rule.onNodeWithTag("downloadHeadsUp").assertDoesNotExist()
            rule.runOnIdle { vm.settings = true }
            rule.onNodeWithTag("inlineClipboardSuggestion").assertDoesNotExist()
            rule.runOnIdle {
                vm.settings = false
                vm.clipboard.state = ClipboardPrompt.Ready(link,
                    VideoInfo(link, link.key, "作品解析完成，可以预览", "", referer = "", userAgent = ""))
            }
            rule.onNodeWithText("作品解析完成，可以预览").assertIsDisplayed()
            rule.onNodeWithTag("openClipboardPreview").assertIsEnabled()
            assertEquals(count, vm.store.tasks.value.size)
            assertFalse(rule.activity.getSystemService(NotificationManager::class.java)
                .activeNotifications.any { it.id == 9502 })
            rule.runOnIdle { vm.clipboard.state = ClipboardPrompt.Failed(link) }
            rule.onNodeWithText("解析失败，请检查网络后重试。").assertIsDisplayed()
            rule.onNodeWithText("重试").assertIsDisplayed()
            rule.onNodeWithTag("openClipboardPreview").performClick()
            rule.onNodeWithTag("downloadLinkInput").assertTextEquals(link.url)
            rule.onNodeWithTag("inlineClipboardSuggestion").assertDoesNotExist()
            rule.onNodeWithTag("downloadHeadsUp").assertDoesNotExist()
            assertEquals(count, vm.store.tasks.value.size)
        } finally {
            rule.runOnIdle {
                vm.setClipboard(false); vm.settings = false; vm.inputVisible = false
                if(original != null) clipboard.setPrimaryClip(original) else clipboard.clearPrimaryClip()
                vm.setClipboard(enabled)
            }
        }
    }
}
