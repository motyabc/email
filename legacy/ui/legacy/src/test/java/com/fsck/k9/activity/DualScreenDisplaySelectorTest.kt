package com.fsck.k9.activity

import android.content.Context
import android.hardware.display.DisplayManager
import android.view.Display
import androidx.test.core.app.ApplicationProvider
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import java.util.function.Consumer
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.internal.DisplayConfig
import org.robolectric.shadows.ShadowDisplayManager

@RunWith(RobolectricTestRunner::class)
class DualScreenDisplaySelectorTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val displayManager = context.getSystemService(DisplayManager::class.java)
    private val selector = DualScreenDisplaySelector(displayManager)

    @Test
    fun `secondary display is not selected when Activity is not on default display`() {
        addPresentationDisplay()

        assertThat(selector.findEligibleSecondaryDisplay(activityDisplayId = 1)).isNull()
    }

    @Test
    fun `display without presentation capability is ignored`() {
        ShadowDisplayManager.addDisplay(KEMI_DISPLAY_QUALIFIERS, DISPLAY_TYPE_HDMI)

        assertThat(selector.findEligibleSecondaryDisplay(Display.DEFAULT_DISPLAY)).isNull()
    }

    @Test
    fun `display with unsupported physical size is ignored`() {
        addPresentationDisplay(UNSUPPORTED_DISPLAY_QUALIFIERS)

        assertThat(selector.findEligibleSecondaryDisplay(Display.DEFAULT_DISPLAY)).isNull()
    }

    @Test
    fun `eligible KEMI presentation display is selected`() {
        val displayId = addPresentationDisplay()

        assertThat(selector.findEligibleSecondaryDisplay(Display.DEFAULT_DISPLAY)?.displayId).isEqualTo(displayId)
    }

    @Test
    fun `preferred display two wins when multiple displays are eligible`() {
        addPresentationDisplay()
        val preferredDisplayId = addPresentationDisplay()

        assertThat(preferredDisplayId).isEqualTo(2)
        assertThat(selector.findEligibleSecondaryDisplay(Display.DEFAULT_DISPLAY)?.displayId).isEqualTo(2)
    }

    private fun addPresentationDisplay(qualifiers: String = KEMI_DISPLAY_QUALIFIERS): Int {
        val displayId = ShadowDisplayManager.addDisplay(qualifiers, DISPLAY_TYPE_HDMI)
        val changeDisplay = ShadowDisplayManager::class.java.getDeclaredMethod(
            "changeDisplay",
            Int::class.javaPrimitiveType,
            Consumer::class.java,
        ).apply { isAccessible = true }
        changeDisplay.invoke(
            null,
            displayId,
            Consumer<DisplayConfig> { config -> config.flags = config.flags or Display.FLAG_PRESENTATION },
        )
        return displayId
    }

    private companion object {
        const val KEMI_DISPLAY_QUALIFIERS = "w1920dp-h1280dp-land-mdpi"
        const val UNSUPPORTED_DISPLAY_QUALIFIERS = "w1280dp-h720dp-land-mdpi"
        const val DISPLAY_TYPE_HDMI = 2
    }
}
