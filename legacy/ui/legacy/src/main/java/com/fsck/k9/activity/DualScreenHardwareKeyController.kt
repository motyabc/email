package com.fsck.k9.activity

import android.view.KeyEvent
import net.thunderbird.core.preference.interaction.DualScreenKeyAction
import net.thunderbird.core.preference.interaction.DualScreenKeyBinding

internal class DualScreenHardwareKeyController(
    private val bindingProvider: () -> DualScreenKeyBinding,
    private val isDualScreenAvailable: () -> Boolean,
    private val isInteractionBlocked: () -> Boolean,
    private val onBindingCaptured: (Int) -> Unit,
    private val onCaptureStateChanged: (DualScreenKeyCaptureState) -> Unit,
    private val performAction: (DualScreenKeyAction) -> Unit,
) {
    private var captureActive = false
    private var consumedKeyCode = KeyEvent.KEYCODE_UNKNOWN

    fun startCapture() {
        captureActive = true
        onCaptureStateChanged(DualScreenKeyCaptureState.WAITING)
    }

    fun cancelCapture() {
        captureActive = false
        onCaptureStateChanged(DualScreenKeyCaptureState.IDLE)
    }

    fun handle(event: KeyEvent): Boolean = when {
        hasPendingConsumedKey(event) -> consumePendingKey(event)
        captureActive -> handleCapture(event)
        canHandleBinding(event) -> handleBinding(event)
        else -> false
    }

    private fun hasPendingConsumedKey(event: KeyEvent): Boolean {
        return consumedKeyCode != KeyEvent.KEYCODE_UNKNOWN && event.keyCode == consumedKeyCode
    }

    private fun consumePendingKey(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_UP) consumedKeyCode = KeyEvent.KEYCODE_UNKNOWN
        return true
    }

    private fun canHandleBinding(event: KeyEvent): Boolean {
        val binding = bindingProvider()
        val isContextReady = binding.isConfigured && isDualScreenAvailable() && !isInteractionBlocked()
        val isMatchingInitialDown =
            event.keyCode == binding.keyCode &&
                event.action == KeyEvent.ACTION_DOWN &&
                event.hasNoModifiers()
        return isContextReady && isMatchingInitialDown
    }

    private fun handleBinding(event: KeyEvent): Boolean {
        val binding = bindingProvider()
        consumedKeyCode = event.keyCode
        if (event.repeatCount == 0) performAction(binding.action)
        return true
    }

    private fun handleCapture(event: KeyEvent): Boolean = when {
        event.action != KeyEvent.ACTION_DOWN -> true
        event.repeatCount > 0 -> true
        else -> captureInitialDown(event)
    }

    private fun captureInitialDown(event: KeyEvent): Boolean {
        consumedKeyCode = event.keyCode
        when {
            event.keyCode == KeyEvent.KEYCODE_BACK -> cancelCapture()
            !DualScreenHardwareKeyPolicy.canLearn(event) -> {
                onCaptureStateChanged(DualScreenKeyCaptureState.REJECTED)
            }

            else -> {
                captureActive = false
                onBindingCaptured(event.keyCode)
                onCaptureStateChanged(DualScreenKeyCaptureState.IDLE)
            }
        }
        return true
    }
}

internal object DualScreenHardwareKeyPolicy {
    private val blockedNavigationKeys = setOf(
        KeyEvent.KEYCODE_DPAD_UP,
        KeyEvent.KEYCODE_DPAD_DOWN,
        KeyEvent.KEYCODE_DPAD_LEFT,
        KeyEvent.KEYCODE_DPAD_RIGHT,
        KeyEvent.KEYCODE_DPAD_CENTER,
        KeyEvent.KEYCODE_ENTER,
        KeyEvent.KEYCODE_NUMPAD_ENTER,
        KeyEvent.KEYCODE_TAB,
        KeyEvent.KEYCODE_SPACE,
        KeyEvent.KEYCODE_DEL,
        KeyEvent.KEYCODE_FORWARD_DEL,
        KeyEvent.KEYCODE_ESCAPE,
        KeyEvent.KEYCODE_MOVE_HOME,
        KeyEvent.KEYCODE_MOVE_END,
        KeyEvent.KEYCODE_PAGE_UP,
        KeyEvent.KEYCODE_PAGE_DOWN,
        KeyEvent.KEYCODE_INSERT,
    )

    fun canLearn(event: KeyEvent): Boolean {
        return event.action == KeyEvent.ACTION_DOWN &&
            event.repeatCount == 0 &&
            event.keyCode != KeyEvent.KEYCODE_UNKNOWN &&
            event.hasNoModifiers() &&
            !event.isSystem &&
            !KeyEvent.isModifierKey(event.keyCode) &&
            event.unicodeChar == 0 &&
            !isTextEntryKey(event.keyCode) &&
            event.keyCode !in blockedNavigationKeys
    }

    private fun isTextEntryKey(keyCode: Int): Boolean {
        return keyCode in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 ||
            keyCode in KeyEvent.KEYCODE_A..KeyEvent.KEYCODE_Z ||
            keyCode in KeyEvent.KEYCODE_NUMPAD_0..KeyEvent.KEYCODE_NUMPAD_9 ||
            keyCode in setOf(
                KeyEvent.KEYCODE_COMMA,
                KeyEvent.KEYCODE_PERIOD,
                KeyEvent.KEYCODE_GRAVE,
                KeyEvent.KEYCODE_MINUS,
                KeyEvent.KEYCODE_EQUALS,
                KeyEvent.KEYCODE_LEFT_BRACKET,
                KeyEvent.KEYCODE_RIGHT_BRACKET,
                KeyEvent.KEYCODE_BACKSLASH,
                KeyEvent.KEYCODE_SEMICOLON,
                KeyEvent.KEYCODE_APOSTROPHE,
                KeyEvent.KEYCODE_SLASH,
                KeyEvent.KEYCODE_AT,
                KeyEvent.KEYCODE_PLUS,
            )
    }
}

internal enum class DualScreenKeyCaptureState {
    IDLE,
    WAITING,
    REJECTED,
}
