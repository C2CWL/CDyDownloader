package c2cw.cdydownload

import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class InlineQualityPickerTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test fun qualityStartsCollapsedSelectsAndResetsForAnotherLink() {
        val chosen = mutableStateOf<String?>(null)
        val linkKey = mutableStateOf("first")
        val catalog = MediaSpecifications(
            listOf(TrackOption("high", "1080p"), TrackOption("low", "720p")),
            emptyList(), TrackSelection("high"))
        rule.runOnIdle { rule.activity.setContent {
            DownloaderTheme { InlineQualityPicker(catalog, chosen.value, linkKey.value) { chosen.value = it } }
        } }
        rule.onNodeWithTag("quality").assertTextContains("自动（推荐）")
        rule.onNodeWithTag("quality-high").assertDoesNotExist()
        rule.onNodeWithTag("quality").performClick()
        rule.onNodeWithTag("quality-high").assertIsDisplayed().performClick()
        rule.runOnIdle { assertEquals("high", chosen.value) }
        rule.onNodeWithTag("quality").assertTextContains("1080p")
        rule.onNodeWithTag("quality-low").assertDoesNotExist()
        rule.onNodeWithTag("quality").performClick()
        rule.onNodeWithTag("quality-auto").performClick()
        rule.runOnIdle { assertEquals(null, chosen.value) }
        rule.onNodeWithTag("quality").assertTextContains("自动（推荐）")
        rule.onNodeWithTag("quality").performClick()
        rule.onNodeWithTag("quality-low").assertIsDisplayed()
        rule.runOnIdle { linkKey.value = "second" }
        rule.onNodeWithTag("quality-low").assertDoesNotExist()
    }
}
