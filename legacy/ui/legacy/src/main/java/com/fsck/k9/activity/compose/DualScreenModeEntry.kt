package com.fsck.k9.activity.compose

import android.view.KeyEvent
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonFilled
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonOutlined
import app.k9mail.core.ui.compose.designsystem.atom.button.RadioButton
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyLarge
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyMedium
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleMedium
import app.k9mail.core.ui.compose.designsystem.organism.AlertDialog
import com.fsck.k9.activity.DualScreenKeyCaptureState
import com.fsck.k9.ui.R
import net.thunderbird.core.preference.DualScreenMode
import net.thunderbird.core.preference.interaction.DualScreenKeyAction
import net.thunderbird.core.preference.interaction.DualScreenKeyBinding
import net.thunderbird.core.ui.compose.theme2.MainTheme

internal const val DUAL_SCREEN_MODE_BUTTON_TEST_TAG = "dual_screen_mode_button"
internal const val DUAL_SCREEN_MODE_DIALOG_TEST_TAG = "dual_screen_mode_dialog"
internal const val DUAL_SCREEN_KEY_CONFIGURE_TEST_TAG = "dual_screen_key_configure"
internal const val DUAL_SCREEN_KEY_DIALOG_TEST_TAG = "dual_screen_key_dialog"
internal const val DUAL_SCREEN_KEY_LEARN_TEST_TAG = "dual_screen_key_learn"
internal const val DUAL_SCREEN_KEY_DISABLE_TEST_TAG = "dual_screen_key_disable"
internal fun dualScreenModeOptionTestTag(mode: DualScreenMode): String = "dual_screen_mode_option_${mode.name}"
internal fun dualScreenKeyActionTestTag(action: DualScreenKeyAction): String = "dual_screen_key_action_${action.name}"

internal data class DualScreenModeEntryState(
    val currentMode: DualScreenMode,
    val keyBinding: DualScreenKeyBinding,
    val keyCaptureState: DualScreenKeyCaptureState,
    val dialogVisible: Boolean,
)

internal data class DualScreenModeEntryCallbacks(
    val onModeSelected: (DualScreenMode) -> Unit,
    val onDialogVisibilityChanged: (Boolean) -> Unit,
    val onKeyActionSelected: (DualScreenKeyAction) -> Unit,
    val onStartKeyCapture: () -> Unit,
    val onCancelKeyCapture: () -> Unit,
    val onDisableKeyBinding: () -> Unit,
)

@Composable
internal fun DualScreenModeEntry(
    visible: Boolean,
    state: DualScreenModeEntryState,
    callbacks: DualScreenModeEntryCallbacks,
    modifier: Modifier = Modifier,
) {
    var showKeySettings by rememberSaveable(state.dialogVisible) { mutableStateOf(false) }
    if (!visible) return

    val currentModeName = stringResource(state.currentMode.titleResource())
    val buttonDescription = stringResource(R.string.dual_screen_mode_button_description, currentModeName)

    ButtonFilled(
        text = currentModeName,
        icon = DualScreenModeIcon,
        onClick = { callbacks.onDialogVisibilityChanged(true) },
        modifier = modifier
            .padding(MainTheme.spacings.double)
            .wrapContentWidth()
            .semantics { contentDescription = buttonDescription }
            .testTag(DUAL_SCREEN_MODE_BUTTON_TEST_TAG),
    )

    if (state.dialogVisible && showKeySettings) {
        DualScreenKeySettingsDialog(
            keyBinding = state.keyBinding,
            captureState = state.keyCaptureState,
            onActionSelected = callbacks.onKeyActionSelected,
            onStartCapture = callbacks.onStartKeyCapture,
            onDisable = callbacks.onDisableKeyBinding,
            onDismissRequest = {
                callbacks.onCancelKeyCapture()
                showKeySettings = false
            },
        )
    } else if (state.dialogVisible) {
        DualScreenModeDialog(
            currentMode = state.currentMode,
            onModeSelected = callbacks.onModeSelected,
            onConfigureKey = { showKeySettings = true },
            onDismissRequest = { callbacks.onDialogVisibilityChanged(false) },
        )
    }
}

@Composable
private fun DualScreenModeDialog(
    currentMode: DualScreenMode,
    onModeSelected: (DualScreenMode) -> Unit,
    onConfigureKey: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    AlertDialog(
        title = stringResource(R.string.dual_screen_mode_dialog_title),
        confirmText = stringResource(R.string.dual_screen_mode_close),
        onConfirmClick = onDismissRequest,
        onDismissRequest = onDismissRequest,
        modifier = Modifier.testTag(DUAL_SCREEN_MODE_DIALOG_TEST_TAG),
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.default),
        ) {
            DualScreenMode.entries.forEach { mode ->
                RadioButton(
                    selected = mode == currentMode,
                    onClick = {
                        onDismissRequest()
                        onModeSelected(mode)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(dualScreenModeOptionTestTag(mode)),
                    label = {
                        Column(
                            modifier = Modifier.padding(vertical = MainTheme.spacings.default),
                        ) {
                            TextTitleMedium(text = stringResource(mode.titleResource()))
                            TextBodyMedium(text = stringResource(mode.descriptionResource()))
                        }
                    },
                )
            }
            ButtonOutlined(
                text = stringResource(R.string.dual_screen_key_configure),
                onClick = onConfigureKey,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(DUAL_SCREEN_KEY_CONFIGURE_TEST_TAG),
            )
        }
    }
}

@Composable
private fun DualScreenKeySettingsDialog(
    keyBinding: DualScreenKeyBinding,
    captureState: DualScreenKeyCaptureState,
    onActionSelected: (DualScreenKeyAction) -> Unit,
    onStartCapture: () -> Unit,
    onDisable: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    AlertDialog(
        title = stringResource(R.string.dual_screen_key_dialog_title),
        confirmText = stringResource(R.string.dual_screen_key_done),
        onConfirmClick = onDismissRequest,
        onDismissRequest = onDismissRequest,
        modifier = Modifier.testTag(DUAL_SCREEN_KEY_DIALOG_TEST_TAG),
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.default),
        ) {
            TextBodyMedium(text = stringResource(R.string.dual_screen_key_description))
            TextBodyLarge(text = keyBinding.summary())
            TextBodyMedium(text = captureState.description())
            ButtonFilled(
                text = stringResource(R.string.dual_screen_key_learn),
                onClick = onStartCapture,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(DUAL_SCREEN_KEY_LEARN_TEST_TAG),
            )
            if (keyBinding.keyCode > KeyEvent.KEYCODE_UNKNOWN) {
                DualScreenKeyAction.entries.filterNot { it == DualScreenKeyAction.DISABLED }.forEach { action ->
                    RadioButton(
                        selected = keyBinding.action == action,
                        onClick = { onActionSelected(action) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(dualScreenKeyActionTestTag(action)),
                        label = {
                            TextTitleMedium(text = stringResource(action.titleResource()))
                        },
                    )
                }
            }
            ButtonOutlined(
                text = stringResource(R.string.dual_screen_key_disable),
                onClick = onDisable,
                enabled = keyBinding.keyCode > KeyEvent.KEYCODE_UNKNOWN,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(DUAL_SCREEN_KEY_DISABLE_TEST_TAG),
            )
        }
    }
}

@Composable
private fun DualScreenKeyBinding.summary(): String {
    if (keyCode <= KeyEvent.KEYCODE_UNKNOWN) return stringResource(R.string.dual_screen_key_not_configured)
    val keyName = KeyEvent.keyCodeToString(keyCode).removePrefix("KEYCODE_").replace('_', ' ')
    val actionName = stringResource(action.titleResource())
    return stringResource(R.string.dual_screen_key_summary, keyName, actionName)
}

@Composable
private fun DualScreenKeyCaptureState.description(): String = when (this) {
    DualScreenKeyCaptureState.IDLE -> stringResource(R.string.dual_screen_key_capture_idle)
    DualScreenKeyCaptureState.WAITING -> stringResource(R.string.dual_screen_key_capture_waiting)
    DualScreenKeyCaptureState.REJECTED -> stringResource(R.string.dual_screen_key_capture_rejected)
}

@StringRes
private fun DualScreenMode.titleResource(): Int = when (this) {
    DualScreenMode.IMMERSIVE -> R.string.dual_screen_mode_immersive
    DualScreenMode.SMART -> R.string.dual_screen_mode_smart
}

@StringRes
private fun DualScreenMode.descriptionResource(): Int = when (this) {
    DualScreenMode.IMMERSIVE -> R.string.dual_screen_mode_immersive_description
    DualScreenMode.SMART -> R.string.dual_screen_mode_smart_description
}

@StringRes
private fun DualScreenKeyAction.titleResource(): Int = when (this) {
    DualScreenKeyAction.DISABLED -> R.string.dual_screen_key_action_disabled
    DualScreenKeyAction.OPEN_MODE_SELECTOR -> R.string.dual_screen_key_action_open_selector
    DualScreenKeyAction.TOGGLE_MODE -> R.string.dual_screen_key_action_toggle_mode
    DualScreenKeyAction.PREVIOUS_MESSAGE -> R.string.dual_screen_key_action_previous_message
    DualScreenKeyAction.NEXT_MESSAGE -> R.string.dual_screen_key_action_next_message
}

@Suppress("MagicNumber")
private val DualScreenModeIcon: ImageVector = ImageVector.Builder(
    name = "DualScreenMode",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).apply {
    path(fill = SolidColor(Color.Black)) {
        moveTo(3f, 3f)
        lineTo(21f, 3f)
        lineTo(21f, 10f)
        lineTo(3f, 10f)
        close()
        moveTo(3f, 14f)
        lineTo(21f, 14f)
        lineTo(21f, 21f)
        lineTo(3f, 21f)
        close()
    }
}.build()
