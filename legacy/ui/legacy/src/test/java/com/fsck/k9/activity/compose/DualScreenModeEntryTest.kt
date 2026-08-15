package com.fsck.k9.activity.compose

import android.app.Application
import android.view.KeyEvent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import com.fsck.k9.activity.DualScreenKeyCaptureState
import net.thunderbird.core.preference.DualScreenMode
import net.thunderbird.core.preference.interaction.DualScreenKeyAction
import net.thunderbird.core.preference.interaction.DualScreenKeyBinding
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
    private val selectedKeyActions = mutableListOf<DualScreenKeyAction>()
    private var captureRequests = 0

    @Test
    fun `entry should be hidden when the secondary display is unavailable`() {
        setContent(visible = false)

        composeTestRule.onNodeWithTag(DUAL_SCREEN_MODE_BUTTON_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun `dialog should show both modes descriptions and current selection`() {
        setContent(visible = true, currentMode = DualScreenMode.IMMERSIVE)

        composeTestRule.onNodeWithText("Immersive mode").assertIsDisplayed()
        composeTestRule.onNodeWithTag(DUAL_SCREEN_MODE_BUTTON_TEST_TAG)
            .assertContentDescriptionEquals("Dual-screen mode, currently Immersive mode")
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
        composeTestRule.onNodeWithTag(DUAL_SCREEN_MODE_DIALOG_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun `physical key settings should be disabled until a key is learned`() {
        setContent(visible = true)
        composeTestRule.onNodeWithTag(DUAL_SCREEN_MODE_BUTTON_TEST_TAG).performClick()
        composeTestRule.onNodeWithTag(DUAL_SCREEN_KEY_CONFIGURE_TEST_TAG).performClick()

        composeTestRule.onNodeWithTag(DUAL_SCREEN_KEY_DIALOG_TEST_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithText("No physical key configured").assertIsDisplayed()
        composeTestRule.onNodeWithTag(dualScreenKeyActionTestTag(DualScreenKeyAction.TOGGLE_MODE))
            .assertDoesNotExist()

        composeTestRule.onNodeWithTag(DUAL_SCREEN_KEY_LEARN_TEST_TAG).performClick()
        assertThat(captureRequests).isEqualTo(1)
    }

    @Test
    fun `configured physical key should show action choices and report selection`() {
        setContent(
            visible = true,
            keyBinding = DualScreenKeyBinding(KeyEvent.KEYCODE_F8, DualScreenKeyAction.OPEN_MODE_SELECTOR),
        )
        composeTestRule.onNodeWithTag(DUAL_SCREEN_MODE_BUTTON_TEST_TAG).performClick()
        composeTestRule.onNodeWithTag(DUAL_SCREEN_KEY_CONFIGURE_TEST_TAG).performClick()

        composeTestRule.onNodeWithText("Key:", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(dualScreenKeyActionTestTag(DualScreenKeyAction.NEXT_MESSAGE))
            .performScrollTo()
            .performClick()

        assertThat(selectedKeyActions).containsExactly(DualScreenKeyAction.NEXT_MESSAGE)
    }

    @Test
    @Config(qualifiers = "zh-rCN")
    fun `entry and dialog expose simplified Chinese labels`() {
        setContent(visible = true, currentMode = DualScreenMode.IMMERSIVE)

        composeTestRule.onNodeWithText("沉浸模式").assertIsDisplayed()
        composeTestRule.onNodeWithTag(DUAL_SCREEN_MODE_BUTTON_TEST_TAG)
            .assertContentDescriptionEquals("双屏模式，当前为沉浸模式")
        composeTestRule.onNodeWithTag(DUAL_SCREEN_MODE_BUTTON_TEST_TAG).performClick()

        composeTestRule.onNodeWithText("智慧模式").assertIsDisplayed()
        composeTestRule.onNodeWithText("上屏阅读邮件，下屏便捷处理").assertIsDisplayed()
        composeTestRule.onNodeWithText("设置物理双屏键").assertIsDisplayed()
    }

    @Test
    fun `dialog remains usable with two hundred percent font scale`() {
        setContent(visible = true, fontScale = 2f)
        composeTestRule.onNodeWithTag(DUAL_SCREEN_MODE_BUTTON_TEST_TAG).performClick()

        composeTestRule.onNodeWithTag(DUAL_SCREEN_MODE_DIALOG_TEST_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(dualScreenModeOptionTestTag(DualScreenMode.IMMERSIVE)).assertIsDisplayed()
        composeTestRule.onNodeWithTag(dualScreenModeOptionTestTag(DualScreenMode.SMART)).assertIsDisplayed()
        composeTestRule.onNodeWithTag(DUAL_SCREEN_KEY_CONFIGURE_TEST_TAG).assertIsDisplayed()
    }

    private fun setContent(
        visible: Boolean,
        currentMode: DualScreenMode = DualScreenMode.IMMERSIVE,
        keyBinding: DualScreenKeyBinding = DualScreenKeyBinding(),
        fontScale: Float = 1f,
    ) {
        composeTestRule.setContent {
            val currentDensity = LocalDensity.current
            var dialogVisible by remember { mutableStateOf(false) }
            CompositionLocalProvider(
                LocalDensity provides Density(currentDensity.density, fontScale),
            ) {
                K9MailTheme2 {
                    DualScreenModeEntry(
                        visible = visible,
                        state = DualScreenModeEntryState(
                            currentMode = currentMode,
                            keyBinding = keyBinding,
                            keyCaptureState = DualScreenKeyCaptureState.IDLE,
                            dialogVisible = dialogVisible,
                        ),
                        callbacks = DualScreenModeEntryCallbacks(
                            onModeSelected = { mode -> selectedModes.add(mode) },
                            onDialogVisibilityChanged = { dialogVisible = it },
                            onKeyActionSelected = selectedKeyActions::add,
                            onStartKeyCapture = { captureRequests++ },
                            onCancelKeyCapture = {},
                            onDisableKeyBinding = {},
                        ),
                    )
                }
            }
        }
    }
}
