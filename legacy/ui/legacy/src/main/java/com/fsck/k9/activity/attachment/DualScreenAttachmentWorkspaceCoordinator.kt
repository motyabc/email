package com.fsck.k9.activity.attachment

import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.fsck.k9.mailstore.AttachmentViewInfo
import com.fsck.k9.ui.R
import com.fsck.k9.ui.helper.SizeFormatter
import net.thunderbird.core.ui.theme.api.FeatureThemeProvider

internal fun canBeginDualScreenAttachmentDrag(attachment: AttachmentViewInfo): Boolean {
    return DualScreenAttachmentPreviewPolicy.canPreview(attachment)
}

internal class DualScreenAttachmentWorkspaceCoordinator(
    private val upperHost: ViewGroup,
    private val lowerHost: ViewGroup,
    private val themeProvider: FeatureThemeProvider,
    private val onOpenExternally: (AttachmentViewInfo) -> Unit,
    private val onSave: (AttachmentViewInfo) -> Unit,
) {
    private var pendingDragAttachment: AttachmentViewInfo? = null

    init {
        hideAndClearHosts()
    }

    fun showIfSupported(attachment: AttachmentViewInfo): Boolean {
        if (!DualScreenAttachmentPreviewPolicy.canPreview(attachment)) {
            dismiss()
            return false
        }

        dismiss()
        bindUpperWorkspace(attachment)
        bindLowerWorkspace(attachment)
        upperHost.isVisible = true
        lowerHost.isVisible = true
        return true
    }

    fun beginDrag(attachment: AttachmentViewInfo): Boolean {
        if (!canBeginDualScreenAttachmentDrag(attachment)) return false

        dismiss()
        pendingDragAttachment = attachment
        bindLowerDropTarget(attachment)
        lowerHost.isVisible = true
        return true
    }

    fun completePendingDrag(): Boolean {
        val attachment = pendingDragAttachment ?: return false
        return showIfSupported(attachment)
    }

    fun cancelPendingDrag(): Boolean {
        if (pendingDragAttachment == null) return false

        dismiss()
        return true
    }

    fun dismiss() {
        hideAndClearHosts()
    }

    private fun bindUpperWorkspace(attachment: AttachmentViewInfo) {
        val preview = ComposeView(upperHost.context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                themeProvider.WithTheme {
                    DualScreenAttachmentPreview(
                        attachmentUri = attachment.internalUri,
                        attachmentName = attachment.displayName,
                        onClose = ::dismiss,
                    )
                }
            }
        }
        upperHost.addView(preview, matchParentLayoutParams())
    }

    private fun bindLowerWorkspace(attachment: AttachmentViewInfo) {
        val details = lowerHost.context.getString(
            R.string.dual_screen_attachment_details,
            attachment.mimeType,
            SizeFormatter(lowerHost.resources).formatSize(attachment.size),
        )
        val actions = ComposeView(lowerHost.context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                themeProvider.WithTheme {
                    DualScreenAttachmentActions(
                        attachmentName = attachment.displayName,
                        attachmentDetails = details,
                        onOpenExternally = {
                            dismiss()
                            onOpenExternally(attachment)
                        },
                        onSave = {
                            dismiss()
                            onSave(attachment)
                        },
                        onClose = ::dismiss,
                    )
                }
            }
        }
        lowerHost.addView(actions, matchParentLayoutParams())
    }

    private fun bindLowerDropTarget(attachment: AttachmentViewInfo) {
        val dropTarget = ComposeView(lowerHost.context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                themeProvider.WithTheme {
                    DualScreenAttachmentDropTarget(
                        attachmentName = attachment.displayName,
                        onDrop = { completePendingDrag() },
                        onCancel = ::dismiss,
                    )
                }
            }
        }
        lowerHost.addView(dropTarget, matchParentLayoutParams())
    }

    private fun matchParentLayoutParams(): ViewGroup.LayoutParams {
        return ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
    }

    private fun hideAndClearHosts() {
        pendingDragAttachment = null
        upperHost.removeAllViews()
        upperHost.isGone = true
        lowerHost.removeAllViews()
        lowerHost.isGone = true
    }
}
