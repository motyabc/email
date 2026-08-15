package com.fsck.k9.activity.smartassistant

import android.view.ViewGroup

internal interface SmartAssistantPanelHost {
    val isAvailable: Boolean

    fun attach(container: ViewGroup)

    fun updateContext(context: SmartAssistantContext)

    fun detach()
}

internal object NoOpSmartAssistantPanelHost : SmartAssistantPanelHost {
    override val isAvailable: Boolean = false

    override fun attach(container: ViewGroup) = Unit

    override fun updateContext(context: SmartAssistantContext) = Unit

    override fun detach() = Unit
}
