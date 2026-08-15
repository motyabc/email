package com.fsck.k9.activity

import net.thunderbird.core.preference.DualScreenMode

internal enum class DualScreenRuntimeState {
    SINGLE_SCREEN,
    IMMERSIVE,
    SMART,
    ;

    val isDualScreenAvailable: Boolean
        get() = this != SINGLE_SCREEN

    val usesImmersiveCanvas: Boolean
        get() = this == IMMERSIVE

    companion object {
        fun resolve(
            savedMode: DualScreenMode,
            isEligibleSecondaryDisplayAvailable: Boolean,
        ): DualScreenRuntimeState {
            if (!isEligibleSecondaryDisplayAvailable) return SINGLE_SCREEN

            return when (savedMode) {
                DualScreenMode.IMMERSIVE -> IMMERSIVE
                DualScreenMode.SMART -> SMART
            }
        }
    }
}
