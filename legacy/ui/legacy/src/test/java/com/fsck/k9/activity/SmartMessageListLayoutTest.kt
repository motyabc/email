package com.fsck.k9.activity

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.drawerlayout.widget.DrawerLayout
import androidx.test.core.app.ApplicationProvider
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import com.fsck.k9.ui.R
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SmartMessageListLayoutTest {
    @Test
    fun `smart workspace places message above lower list workspace`() {
        val root = inflateSmartWorkspace()
        val workspace = root.findViewById<LinearLayout>(R.id.smart_workspace)

        assertThat(workspace.getChildAt(0).id).isEqualTo(R.id.message_view_container)
        assertThat(workspace.getChildAt(2).id).isEqualTo(R.id.coordinator_layout)
        assertThat(workspace.getChildAt(2).findViewById<ViewGroup>(R.id.message_list_container)).isNotNull()
    }

    @Test
    fun `navigation drawer is constrained to physical lower viewport`() {
        val root = inflateSmartWorkspace()
        val drawer = root.getChildAt(1) as ViewGroup
        val layoutParams = drawer.layoutParams as DrawerLayout.LayoutParams

        assertThat(layoutParams.height).isEqualTo(LOWER_VIEWPORT_HEIGHT)
        assertThat(layoutParams.gravity and Gravity.BOTTOM == Gravity.BOTTOM).isTrue()
    }

    private fun inflateSmartWorkspace(): DrawerLayout {
        val applicationContext = ApplicationProvider.getApplicationContext<Context>()
        val themedContext = ContextThemeWrapper(
            applicationContext,
            com.google.android.material.R.style.Theme_Material3_Light_NoActionBar,
        )
        return LayoutInflater.from(themedContext)
            .inflate(R.layout.smart_message_list, null, false) as DrawerLayout
    }

    private companion object {
        const val LOWER_VIEWPORT_HEIGHT = 1280
    }
}
