package com.fsck.k9.activity

import android.hardware.display.DisplayManager
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import com.fsck.k9.mailstore.MessageViewInfo
import com.fsck.k9.message.SimpleMessageFormat
import com.fsck.k9.message.extractors.BodyTextExtractor
import com.fsck.k9.ui.R
import com.fsck.k9.ui.base.getDisplayIdCompat
import com.google.android.material.snackbar.Snackbar
import net.thunderbird.core.preference.DualScreenMode

/**
 * Adds dual-screen presentation to the existing single-authority compose Activity.
 *
 * The lower workspace contains the only editable recipients, body, attachments, draft, and send path. The upper
 * smart workspace is a transient, read-only rendering of the source message and never persists its own mail state.
 */
internal class DualScreenComposeController private constructor(
    private val activity: MessageCompose,
    private val savedMode: DualScreenMode,
    private val displaySelector: DualScreenDisplaySelector,
    private val initialDisplayTarget: DualScreenDisplayTarget?,
    private val workspace: DualScreenComposeWorkspace,
    private val recoveryViewModel: DualScreenRecoveryViewModel,
) {
    @get:LayoutRes
    val layoutResource: Int = workspace.layoutResource

    private var spanCoordinator: DualScreenSpanCoordinator? = null
    private var referenceView: DualScreenComposeReferenceView? = null
    private var recoverySnackbar: Snackbar? = null
    private var workspaceRecreationRequested = false

    fun attach(activityContent: ViewGroup, authoritativeRootView: View) {
        check(spanCoordinator == null)

        workspace.deviceProfile?.takeIf { workspace.runtimeState.usesSmartWorkspace }?.let { deviceProfile ->
            SmartDualScreenComposeLayoutConfigurator.apply(authoritativeRootView, deviceProfile)
            referenceView = DualScreenComposeReferenceView.create(authoritativeRootView)
        }

        spanCoordinator = DualScreenSpanCoordinator(
            activity = activity,
            sourceView = authoritativeRootView,
            dualScreenMode = savedMode,
            preparedRuntimeState = workspace.runtimeState,
            preparedDeviceProfile = initialDisplayTarget?.deviceProfile,
            displaySelector = displaySelector,
            secondaryViewportContentDescription =
            activity.getText(R.string.dual_screen_compose_reference_description),
            initialRecoveryPending = recoveryViewModel.recoveryPending,
            onRecoveryPendingChanged = { recoveryPending ->
                recoveryViewModel.recoveryPending = recoveryPending
            },
            onRecoveryAvailabilityChanged = { recoveryAvailable ->
                updateRecoveryPrompt(activityContent, recoveryAvailable)
            },
            onSmartWorkspaceLost = ::recreateWorkspaceOnce,
        )
    }

    fun showInitialReference(action: MessageCompose.Action) {
        referenceView?.showInitial(action)
    }

    fun showSourceReference(messageViewInfo: MessageViewInfo, action: MessageCompose.Action) {
        if (!action.usesSourceReference()) return

        referenceView?.showSource(DualScreenComposeReferenceContent.from(messageViewInfo))
    }

    fun showReferenceUnavailable(action: MessageCompose.Action) {
        if (action.usesSourceReference()) referenceView?.showUnavailable()
    }

    fun start() {
        spanCoordinator?.start()
    }

    fun stop() {
        spanCoordinator?.stop()
    }

    fun destroy() {
        recoverySnackbar?.dismiss()
        recoverySnackbar = null
        referenceView = null
        spanCoordinator?.destroy()
        spanCoordinator = null
    }

    private fun updateRecoveryPrompt(anchor: View, recoveryAvailable: Boolean) {
        if (!recoveryAvailable) {
            recoverySnackbar?.dismiss()
            recoverySnackbar = null
            return
        }
        if (recoverySnackbar?.isShown == true) return

        recoverySnackbar = Snackbar.make(
            anchor,
            R.string.dual_screen_recovery_available,
            Snackbar.LENGTH_INDEFINITE,
        ).setAction(R.string.dual_screen_recovery_action) {
            val recreateWorkspace = spanCoordinator?.resumeAfterDisplayReconnect() == true
            if (recreateWorkspace) recreateWorkspaceOnce()
        }.also(Snackbar::show)
    }

    private fun recreateWorkspaceOnce() {
        if (workspaceRecreationRequested) return

        workspaceRecreationRequested = true
        activity.recreate()
    }

    companion object {
        @JvmStatic
        fun create(activity: MessageCompose, savedMode: DualScreenMode): DualScreenComposeController {
            val displaySelector = DualScreenDisplaySelector(activity.getSystemService(DisplayManager::class.java))
            val initialDisplayTarget = displaySelector.findEligibleSecondaryDisplay(activity.getDisplayIdCompat())
            val recoveryViewModel = ViewModelProvider(activity)[DualScreenRecoveryViewModel::class.java]
            val workspace = DualScreenComposeWorkspace.resolve(
                savedMode = savedMode,
                deviceProfile = initialDisplayTarget?.deviceProfile,
                recoveryPending = recoveryViewModel.recoveryPending,
            )

            return DualScreenComposeController(
                activity = activity,
                savedMode = savedMode,
                displaySelector = displaySelector,
                initialDisplayTarget = initialDisplayTarget,
                workspace = workspace,
                recoveryViewModel = recoveryViewModel,
            )
        }
    }
}

internal data class DualScreenComposeWorkspace(
    val runtimeState: DualScreenRuntimeState,
    val deviceProfile: DualScreenDeviceProfile?,
) {
    @get:LayoutRes
    val layoutResource: Int = if (runtimeState.usesSmartWorkspace) {
        R.layout.smart_message_compose
    } else {
        R.layout.message_compose
    }

    companion object {
        fun resolve(
            savedMode: DualScreenMode,
            deviceProfile: DualScreenDeviceProfile?,
            recoveryPending: Boolean,
        ): DualScreenComposeWorkspace {
            val resolvedState = DualScreenRuntimeState.resolve(
                savedMode = savedMode,
                isEligibleSecondaryDisplayAvailable = deviceProfile != null,
            )
            val initialState = if (recoveryPending) DualScreenRuntimeState.SINGLE_SCREEN else resolvedState

            return DualScreenComposeWorkspace(initialState, deviceProfile)
        }
    }
}

internal object SmartDualScreenComposeLayoutConfigurator {
    fun apply(rootView: View, deviceProfile: DualScreenDeviceProfile) {
        val referencePanel = checkNotNull(rootView.findViewById<View>(R.id.dual_screen_compose_reference_panel))
        val editorPanel = checkNotNull(rootView.findViewById<View>(R.id.dual_screen_compose_editor_panel))

        setViewportHeight(referencePanel, deviceProfile.logicalViewportHeight)
        setViewportHeight(editorPanel, deviceProfile.logicalViewportHeight)
    }

    private fun setViewportHeight(view: View, viewportHeight: Int) {
        view.layoutParams = (view.layoutParams as LinearLayout.LayoutParams).apply {
            height = viewportHeight
            weight = 0f
        }
    }
}

internal data class DualScreenComposeReferenceContent(
    val subject: String,
    val body: String,
    val truncated: Boolean,
) {
    companion object {
        private const val MAX_BODY_CHARACTERS = 100_000

        fun from(messageViewInfo: MessageViewInfo): DualScreenComposeReferenceContent {
            val body = messageViewInfo.rootPart?.let { rootPart ->
                BodyTextExtractor.getBodyTextFromMessage(rootPart, SimpleMessageFormat.TEXT)
            }.orEmpty()

            return fromText(messageViewInfo.subject, body)
        }

        fun fromText(subject: String?, body: String): DualScreenComposeReferenceContent {
            val truncated = body.length > MAX_BODY_CHARACTERS
            return DualScreenComposeReferenceContent(
                subject = subject.orEmpty(),
                body = if (truncated) body.take(MAX_BODY_CHARACTERS) else body,
                truncated = truncated,
            )
        }
    }
}

private class DualScreenComposeReferenceView private constructor(rootView: View) {
    private val guidanceView = rootView.findViewById<TextView>(R.id.dual_screen_compose_reference_guidance)
    private val subjectView = rootView.findViewById<TextView>(R.id.dual_screen_compose_reference_subject)
    private val bodyView = rootView.findViewById<TextView>(R.id.dual_screen_compose_reference_body)
    private val truncatedView = rootView.findViewById<TextView>(R.id.dual_screen_compose_reference_truncated)

    fun showInitial(action: MessageCompose.Action) {
        guidanceView.setText(action.initialReferenceText())
        subjectView.isVisible = false
        bodyView.isVisible = false
        truncatedView.isVisible = false
    }

    fun showSource(content: DualScreenComposeReferenceContent) {
        guidanceView.setText(R.string.dual_screen_compose_reference_source_hint)
        if (content.subject.isBlank()) {
            subjectView.setText(R.string.dual_screen_compose_reference_no_subject)
        } else {
            subjectView.text = content.subject
        }
        if (content.body.isBlank()) {
            bodyView.setText(R.string.dual_screen_compose_reference_no_body)
        } else {
            bodyView.text = content.body
        }
        subjectView.isVisible = true
        bodyView.isVisible = true
        truncatedView.isVisible = content.truncated
    }

    fun showUnavailable() {
        guidanceView.setText(R.string.dual_screen_compose_reference_unavailable)
        subjectView.isVisible = false
        bodyView.isVisible = false
        truncatedView.isVisible = false
    }

    companion object {
        fun create(rootView: View): DualScreenComposeReferenceView {
            return DualScreenComposeReferenceView(rootView)
        }
    }
}

internal fun MessageCompose.Action.usesSourceReference(): Boolean {
    return this == MessageCompose.Action.REPLY ||
        this == MessageCompose.Action.REPLY_ALL ||
        this == MessageCompose.Action.FORWARD ||
        this == MessageCompose.Action.FORWARD_AS_ATTACHMENT
}

internal fun MessageCompose.Action.initialReferenceText(): Int = when (this) {
    MessageCompose.Action.COMPOSE -> R.string.dual_screen_compose_reference_compose_hint
    MessageCompose.Action.EDIT_DRAFT -> R.string.dual_screen_compose_reference_draft_hint
    MessageCompose.Action.REPLY,
    MessageCompose.Action.REPLY_ALL,
    MessageCompose.Action.FORWARD,
    MessageCompose.Action.FORWARD_AS_ATTACHMENT,
    -> R.string.dual_screen_compose_reference_loading_hint
}
