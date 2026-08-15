package com.fsck.k9.activity

import android.hardware.display.DisplayManager
import android.view.Display

internal class DualScreenDisplaySelector(
    private val displayManager: DisplayManager,
    private val geometry: DualScreenSpanGeometry = DualScreenSpanGeometry(),
) {
    fun findEligibleSecondaryDisplay(activityDisplayId: Int?): Display? {
        if (activityDisplayId != Display.DEFAULT_DISPLAY) return null

        val eligibleDisplays = displayManager.getDisplays(
            DisplayManager.DISPLAY_CATEGORY_PRESENTATION,
        ).filter { display ->
            display.state == Display.STATE_ON &&
                display.displayId != Display.DEFAULT_DISPLAY &&
                geometry.matches(display.mode.physicalWidth, display.mode.physicalHeight)
        }

        return eligibleDisplays.firstOrNull { it.displayId == PREFERRED_SECONDARY_DISPLAY_ID }
            ?: eligibleDisplays.firstOrNull()
    }

    private companion object {
        const val PREFERRED_SECONDARY_DISPLAY_ID = 2
    }
}
