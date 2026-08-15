package com.fsck.k9.activity.attachment

import android.app.Application
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import assertk.assertThat
import assertk.assertions.containsExactly
import net.thunderbird.core.ui.compose.theme2.k9mail.K9MailTheme2
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class DualScreenAttachmentWorkspaceTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val invokedActions = mutableListOf<String>()

    @Test
    fun `lower workspace presents attachment information and unique actions`() {
        setContent()

        composeTestRule.onNodeWithTag(ATTACHMENT_ACTIONS_TEST_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithText("Attachment workspace").assertIsDisplayed()
        composeTestRule.onNodeWithText("quarterly-report.png").assertIsDisplayed()
        composeTestRule.onNodeWithText("image/png · 2.1 MB").assertIsDisplayed()
        composeTestRule.onNodeWithText("Open with system app").assertIsDisplayed()
        composeTestRule.onNodeWithText("Save attachment").assertIsDisplayed()
        composeTestRule.onNodeWithText("Return to message list").assertIsDisplayed()
    }

    @Test
    fun `lower workspace delegates open save and close actions once`() {
        setContent()

        composeTestRule.onNodeWithTag(ATTACHMENT_OPEN_TEST_TAG).performClick()
        composeTestRule.onNodeWithTag(ATTACHMENT_SAVE_TEST_TAG).performClick()
        composeTestRule.onNodeWithTag(ATTACHMENT_CLOSE_TEST_TAG).performClick()

        assertThat(invokedActions).containsExactly("open", "save", "close")
    }

    @Test
    @Config(qualifiers = "zh-rCN")
    fun `lower workspace exposes simplified Chinese labels`() {
        setContent()

        composeTestRule.onNodeWithText("附件工作台").assertIsDisplayed()
        composeTestRule.onNodeWithText("使用系统应用打开").assertIsDisplayed()
        composeTestRule.onNodeWithText("保存附件").assertIsDisplayed()
        composeTestRule.onNodeWithText("返回邮件列表").assertIsDisplayed()
    }

    @Test
    fun `lower workspace remains reachable at two hundred percent font scale`() {
        setContent(fontScale = 2f)

        composeTestRule.onNodeWithTag(ATTACHMENT_ACTIONS_TEST_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(ATTACHMENT_OPEN_TEST_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(ATTACHMENT_SAVE_TEST_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(ATTACHMENT_CLOSE_TEST_TAG).assertIsDisplayed()
    }

    private fun setContent(fontScale: Float = 1f) {
        composeTestRule.setContent {
            val currentDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(currentDensity.density, fontScale),
            ) {
                K9MailTheme2 {
                    DualScreenAttachmentActions(
                        attachmentName = "quarterly-report.png",
                        attachmentDetails = "image/png · 2.1 MB",
                        onOpenExternally = { invokedActions.add("open") },
                        onSave = { invokedActions.add("save") },
                        onClose = { invokedActions.add("close") },
                    )
                }
            }
        }
    }
}
