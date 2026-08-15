package com.fsck.k9.activity.smartassistant

import app.k9mail.legacy.message.controller.MessageReference
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test

class SmartAssistantContextTest {
    @Test
    fun `message context contains stable references only`() {
        val source = MessageReference(
            accountUuid = "account",
            folderId = 42L,
            uid = "uid-7",
        )

        val context = SmartAssistantContext(
            scene = SmartAssistantScene.MESSAGE_READING,
            account = SmartAssistantAccountReference(source.accountUuid),
            folder = SmartAssistantFolderReference(source.accountUuid, source.folderId),
            message = source.toSmartAssistantMessageReference(),
        )

        assertThat(context).isEqualTo(
            SmartAssistantContext(
                scene = SmartAssistantScene.MESSAGE_READING,
                account = SmartAssistantAccountReference("account"),
                folder = SmartAssistantFolderReference("account", 42L),
                message = SmartAssistantMessageReference("account", 42L, "uid-7"),
            ),
        )
    }

    @Test
    fun `draft mapping contains identity without message payload`() {
        val source = MessageReference("account", 8L, "draft-uid")

        val draftReference = source.toSmartAssistantDraftReference()

        assertThat(draftReference).isEqualTo(
            SmartAssistantDraftReference("account", 8L, "draft-uid"),
        )
    }
}
