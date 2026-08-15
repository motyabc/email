package com.fsck.k9.activity.compose

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import assertk.assertThat
import assertk.assertions.containsExactly
import net.thunderbird.core.preference.DualScreenMode
import net.thunderbird.core.ui.compose.theme2.k9mail.K9MailTheme2
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class DualScreenModeEntryTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val selectedModes = mutableListOf<DualScreenMode>()

    @Test
    fun `entry should be hidden when the secondary display is unavailable`() {
        setContent(visible = false)

        composeTestRule.onNodeWithTag(DUAL_SCREEN_MODE_BUTTON_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun `dialog should show both modes descriptions and current selection`() {
        setContent(visible = true, currentMode = DualScreenMode.IMMERSIVE)

        composeTestRule.onNodeWithText("Immersive mode").assertIsDisplayed()
        composeTestRule.onNodeWithTag(DUAL_SCREEN_MODE_BUTTON_TEST_TAG).performClick()

        composeTestRule.onNodeWithTag(DUAL_SCREEN_MODE_DIALOG_TEST_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithText("Use both screens as one continuous canvas").assertIsDisplayed()
        composeTestRule.onNodeWithText("Smart mode").assertIsDisplayed()
        composeTestRule.onNodeWithText("Read above and manage mail below").assertIsDisplayed()
        composeTestRule.onNodeWithTag(dualScreenModeOptionTestTag(DualScreenMode.IMMERSIVE)).assertIsSelected()
    }

    @Test
    fun `selecting smart mode should report the selected mode`() {
        setContent(visible = true, currentMode = DualScreenMode.IMMERSIVE)
        composeTestRule.onNodeWithTag(DUAL_SCREEN_MODE_BUTTON_TEST_TAG).performClick()

        composeTestRule.onNodeWithTag(dualScreenModeOptionTestTag(DualScreenMode.SMART)).performClick()

        assertThat(selectedModes).containsExactly(DualScreenMode.SMART)
    }

    private fun setContent(
        visible: Boolean,
        currentMode: DualScreenMode = DualScreenMode.IMMERSIVE,
    ) {
        composeTestRule.setContent {
            K9MailTheme2 {
                DualScreenModeEntry(
                    visible = visible,
                    currentMode = currentMode,
                    onModeSelected = { mode -> selectedModes.add(mode) },
                )
            }
        }
    }
}
