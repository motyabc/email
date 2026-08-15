package com.fsck.k9.activity

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.view.ContextThemeWrapper
import android.view.View
import androidx.test.core.app.ApplicationProvider
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import assertk.assertions.isTrue
import com.fsck.k9.ui.R
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DualScreenSecondaryViewportTest {
    @Test
    fun `viewport uses theme background in light and dark modes`() {
        val lightContext = themedContext(com.google.android.material.R.style.Theme_Material3_Light_NoActionBar)
        val darkContext = themedContext(com.google.android.material.R.style.Theme_Material3_Dark_NoActionBar)

        val lightColor = createViewport(lightContext).backgroundColor
        val darkColor = createViewport(darkContext).backgroundColor

        assertThat(lightColor).isEqualTo(lightContext.themeBackgroundColor)
        assertThat(darkColor).isEqualTo(darkContext.themeBackgroundColor)
        assertThat(lightColor).isNotEqualTo(darkColor)
    }

    @Test
    fun `viewport exposes a basic accessibility explanation`() {
        val context = themedContext(com.google.android.material.R.style.Theme_Material3_Light_NoActionBar)
        val viewport = createViewport(context)

        assertThat(viewport.contentDescription).isEqualTo(
            context.getString(R.string.dual_screen_secondary_viewport_description),
        )
        assertThat(viewport.isClickable).isTrue()
        assertThat(viewport.isFocusable).isTrue()
        assertThat(viewport.importantForAccessibility).isEqualTo(View.IMPORTANT_FOR_ACCESSIBILITY_YES)
    }

    private fun createViewport(context: Context): DualScreenSecondaryViewport {
        return DualScreenSecondaryViewport(
            context = context,
            sourceView = View(context),
            geometry = DualScreenSpanGeometry(),
        )
    }

    private fun themedContext(theme: Int): Context {
        val applicationContext = ApplicationProvider.getApplicationContext<Context>()
        return ContextThemeWrapper(applicationContext, theme)
    }

    private val View.backgroundColor: Int
        get() = (background as ColorDrawable).color

    private val Context.themeBackgroundColor: Int
        get() {
            val attributes = obtainStyledAttributes(intArrayOf(android.R.attr.colorBackground))
            return try {
                attributes.getColor(0, android.graphics.Color.TRANSPARENT)
            } finally {
                attributes.recycle()
            }
        }
}
