package com.fsck.k9.activity.attachment

import android.content.Context
import android.net.Uri
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.test.core.app.ApplicationProvider
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.fsck.k9.mailstore.AttachmentViewInfo
import kotlin.test.Test
import net.thunderbird.core.ui.theme.api.FeatureThemeProvider
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DualScreenAttachmentWorkspaceCoordinatorTest {
    @Test
    fun `supported attachment shows both workspace hosts and dismiss clears them`() {
        val fixture = createFixture()

        assertThat(fixture.testSubject.showIfSupported(createAttachment())).isTrue()
        assertThat(fixture.upperHost.visibility).isEqualTo(View.VISIBLE)
        assertThat(fixture.lowerHost.visibility).isEqualTo(View.VISIBLE)
        assertThat(fixture.upperHost.childCount).isEqualTo(1)
        assertThat(fixture.lowerHost.childCount).isEqualTo(1)

        fixture.testSubject.dismiss()

        assertHostsCleared(fixture)
    }

    @Test
    fun `unsupported request clears a previous preview before falling back`() {
        val fixture = createFixture()
        fixture.testSubject.showIfSupported(createAttachment())

        val handled = fixture.testSubject.showIfSupported(createAttachment(mimeType = "application/pdf"))

        assertThat(handled).isFalse()
        assertHostsCleared(fixture)
    }

    @Test
    fun `destroy clears an active workspace`() {
        val fixture = createFixture()
        fixture.testSubject.showIfSupported(createAttachment())

        fixture.testSubject.destroy()

        assertHostsCleared(fixture)
    }

    private fun assertHostsCleared(fixture: Fixture) {
        assertThat(fixture.upperHost.visibility).isEqualTo(View.GONE)
        assertThat(fixture.lowerHost.visibility).isEqualTo(View.GONE)
        assertThat(fixture.upperHost.childCount).isEqualTo(0)
        assertThat(fixture.lowerHost.childCount).isEqualTo(0)
    }

    private fun createFixture(
        onOpenExternally: (AttachmentViewInfo) -> Unit = {},
        onSave: (AttachmentViewInfo) -> Unit = {},
    ): Fixture {
        val applicationContext = ApplicationProvider.getApplicationContext<Context>()
        val themedContext = ContextThemeWrapper(
            applicationContext,
            com.google.android.material.R.style.Theme_Material3_Light_NoActionBar,
        )
        val upperHost = FrameLayout(themedContext)
        val lowerHost = FrameLayout(themedContext)
        val coordinator = DualScreenAttachmentWorkspaceCoordinator(
            upperHost = upperHost,
            lowerHost = lowerHost,
            themeProvider = FakeThemeProvider,
            onOpenExternally = onOpenExternally,
            onSave = onSave,
        )
        return Fixture(coordinator, upperHost, lowerHost)
    }

    private fun createAttachment(mimeType: String = "image/png"): AttachmentViewInfo {
        return AttachmentViewInfo(
            mimeType,
            "attachment.png",
            1024,
            Uri.parse("content://com.example.attachments/1"),
            false,
            null,
            true,
        )
    }

    private data class Fixture(
        val testSubject: DualScreenAttachmentWorkspaceCoordinator,
        val upperHost: FrameLayout,
        val lowerHost: FrameLayout,
    )

    private object FakeThemeProvider : FeatureThemeProvider {
        @Composable
        override fun WithTheme(content: @Composable () -> Unit) {
            content()
        }

        @Composable
        override fun WithTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
            content()
        }
    }
}
