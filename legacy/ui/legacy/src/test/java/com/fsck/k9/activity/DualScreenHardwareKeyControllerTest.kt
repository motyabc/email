package com.fsck.k9.activity

import android.view.KeyEvent
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import net.thunderbird.core.preference.interaction.DualScreenKeyAction
import net.thunderbird.core.preference.interaction.DualScreenKeyBinding
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class DualScreenHardwareKeyControllerTest {
    private var binding = DualScreenKeyBinding()
    private var dualScreenAvailable = true
    private var interactionBlocked = false
    private val capturedKeyCodes = mutableListOf<Int>()
    private val captureStates = mutableListOf<DualScreenKeyCaptureState>()
    private val actions = mutableListOf<DualScreenKeyAction>()
    private val testSubject = DualScreenHardwareKeyController(
        bindingProvider = { binding },
        isDualScreenAvailable = { dualScreenAvailable },
        isInteractionBlocked = { interactionBlocked },
        onBindingCaptured = capturedKeyCodes::add,
        onCaptureStateChanged = captureStates::add,
        performAction = actions::add,
    )

    @Test
    fun `disabled binding should leave event untouched`() {
        assertThat(testSubject.handle(keyDown(KeyEvent.KEYCODE_F8))).isFalse()
        assertThat(testSubject.handle(keyDown(KeyEvent.KEYCODE_UNKNOWN))).isFalse()
        assertThat(actions).isEmpty()
    }

    @Test
    fun `capture should learn a non system non text key and consume its key up`() {
        testSubject.startCapture()

        assertThat(testSubject.handle(keyDown(KeyEvent.KEYCODE_F8))).isTrue()
        assertThat(testSubject.handle(keyUp(KeyEvent.KEYCODE_F8))).isTrue()

        assertThat(capturedKeyCodes).containsExactly(KeyEvent.KEYCODE_F8)
        assertThat(captureStates).containsExactly(
            DualScreenKeyCaptureState.WAITING,
            DualScreenKeyCaptureState.IDLE,
        )
    }

    @Test
    fun `capture should reject printable navigation and system keys`() {
        testSubject.startCapture()

        assertThat(testSubject.handle(keyDown(KeyEvent.KEYCODE_A))).isTrue()
        assertThat(testSubject.handle(keyUp(KeyEvent.KEYCODE_A))).isTrue()
        assertThat(testSubject.handle(keyDown(KeyEvent.KEYCODE_DPAD_LEFT))).isTrue()
        assertThat(testSubject.handle(keyUp(KeyEvent.KEYCODE_DPAD_LEFT))).isTrue()
        assertThat(testSubject.handle(keyDown(KeyEvent.KEYCODE_VOLUME_UP))).isTrue()

        assertThat(capturedKeyCodes).isEmpty()
        assertThat(captureStates).containsExactly(
            DualScreenKeyCaptureState.WAITING,
            DualScreenKeyCaptureState.REJECTED,
            DualScreenKeyCaptureState.REJECTED,
            DualScreenKeyCaptureState.REJECTED,
        )
    }

    @Test
    fun `back should cancel capture without learning a binding`() {
        testSubject.startCapture()

        assertThat(testSubject.handle(keyDown(KeyEvent.KEYCODE_BACK))).isTrue()

        assertThat(capturedKeyCodes).isEmpty()
        assertThat(captureStates).containsExactly(
            DualScreenKeyCaptureState.WAITING,
            DualScreenKeyCaptureState.IDLE,
        )
    }

    @Test
    fun `configured binding should run once and consume repeat and key up`() {
        binding = DualScreenKeyBinding(KeyEvent.KEYCODE_F8, DualScreenKeyAction.TOGGLE_MODE)

        assertThat(testSubject.handle(keyDown(KeyEvent.KEYCODE_F8))).isTrue()
        assertThat(testSubject.handle(keyDown(KeyEvent.KEYCODE_F8, repeatCount = 1))).isTrue()
        assertThat(testSubject.handle(keyUp(KeyEvent.KEYCODE_F8))).isTrue()

        assertThat(actions).containsExactly(DualScreenKeyAction.TOGGLE_MODE)
    }

    @Test
    fun `binding should be inactive without dual screen or while interaction is blocked`() {
        binding = DualScreenKeyBinding(KeyEvent.KEYCODE_F8, DualScreenKeyAction.NEXT_MESSAGE)
        dualScreenAvailable = false

        assertThat(testSubject.handle(keyDown(KeyEvent.KEYCODE_F8))).isFalse()

        dualScreenAvailable = true
        interactionBlocked = true
        assertThat(testSubject.handle(keyDown(KeyEvent.KEYCODE_F8))).isFalse()
        assertThat(actions).isEmpty()
    }

    @Test
    fun `wrong key and modified key should stay on original event path`() {
        binding = DualScreenKeyBinding(KeyEvent.KEYCODE_F8, DualScreenKeyAction.OPEN_MODE_SELECTOR)

        assertThat(testSubject.handle(keyDown(KeyEvent.KEYCODE_F7))).isFalse()
        assertThat(
            testSubject.handle(
                KeyEvent(
                    0L,
                    0L,
                    KeyEvent.ACTION_DOWN,
                    KeyEvent.KEYCODE_F8,
                    0,
                    KeyEvent.META_SHIFT_ON,
                ),
            ),
        ).isFalse()
        assertThat(actions).isEmpty()
    }

    private fun keyDown(keyCode: Int, repeatCount: Int = 0): KeyEvent {
        return KeyEvent(0L, 0L, KeyEvent.ACTION_DOWN, keyCode, repeatCount)
    }

    private fun keyUp(keyCode: Int): KeyEvent {
        return KeyEvent(0L, 0L, KeyEvent.ACTION_UP, keyCode, 0)
    }
}
