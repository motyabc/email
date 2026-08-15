package com.fsck.k9.activity.compose

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
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
import app.k9mail.core.ui.compose.designsystem.atom.button.RadioButton
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyMedium
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleMedium
import app.k9mail.core.ui.compose.designsystem.organism.AlertDialog
import com.fsck.k9.ui.R
import net.thunderbird.core.preference.DualScreenMode
import net.thunderbird.core.ui.compose.theme2.MainTheme

internal const val DUAL_SCREEN_MODE_BUTTON_TEST_TAG = "dual_screen_mode_button"
internal const val DUAL_SCREEN_MODE_DIALOG_TEST_TAG = "dual_screen_mode_dialog"
internal fun dualScreenModeOptionTestTag(mode: DualScreenMode): String = "dual_screen_mode_option_${mode.name}"

@Composable
internal fun DualScreenModeEntry(
    visible: Boolean,
    currentMode: DualScreenMode,
    onModeSelected: (DualScreenMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDialog by rememberSaveable(visible) { mutableStateOf(false) }
    if (!visible) return

    val currentModeName = stringResource(currentMode.titleResource())
    val buttonDescription = stringResource(R.string.dual_screen_mode_button_description, currentModeName)

    ButtonFilled(
        text = currentModeName,
        icon = DualScreenModeIcon,
        onClick = { showDialog = true },
        modifier = modifier
            .padding(MainTheme.spacings.double)
            .wrapContentWidth()
            .semantics { contentDescription = buttonDescription }
            .testTag(DUAL_SCREEN_MODE_BUTTON_TEST_TAG),
    )

    if (showDialog) {
        DualScreenModeDialog(
            currentMode = currentMode,
            onModeSelected = onModeSelected,
            onDismissRequest = { showDialog = false },
        )
    }
}

@Composable
private fun DualScreenModeDialog(
    currentMode: DualScreenMode,
    onModeSelected: (DualScreenMode) -> Unit,
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
        }
    }
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
