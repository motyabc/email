package com.fsck.k9.activity.smartassistant

import app.k9mail.legacy.message.controller.MessageReference

internal enum class SmartAssistantScene {
    MESSAGE_LIST,
    MESSAGE_READING,
    DRAFT_COMPOSING,
    MESSAGE_SELECTION,
}

internal data class SmartAssistantAccountReference(
    val accountUuid: String,
)

internal data class SmartAssistantFolderReference(
    val accountUuid: String,
    val folderId: Long,
)

internal data class SmartAssistantMessageReference(
    val accountUuid: String,
    val folderId: Long,
    val messageUid: String,
)

internal data class SmartAssistantDraftReference(
    val accountUuid: String,
    val folderId: Long,
    val messageUid: String,
)

internal data class SmartAssistantSelectionReference(
    val messages: List<SmartAssistantMessageReference>,
)

internal data class SmartAssistantContext(
    val scene: SmartAssistantScene,
    val account: SmartAssistantAccountReference? = null,
    val folder: SmartAssistantFolderReference? = null,
    val message: SmartAssistantMessageReference? = null,
    val draft: SmartAssistantDraftReference? = null,
    val selection: SmartAssistantSelectionReference? = null,
)

internal fun MessageReference.toSmartAssistantMessageReference(): SmartAssistantMessageReference {
    return SmartAssistantMessageReference(
        accountUuid = accountUuid,
        folderId = folderId,
        messageUid = uid,
    )
}

internal fun MessageReference.toSmartAssistantDraftReference(): SmartAssistantDraftReference {
    return SmartAssistantDraftReference(
        accountUuid = accountUuid,
        folderId = folderId,
        messageUid = uid,
    )
}
