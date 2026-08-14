package com.fsck.k9.activity

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlin.test.Test

class DualScreenSpanGeometryTest {
    private val testSubject = DualScreenSpanGeometry()

    @Test
    fun `logical canvas should contain two vertical viewports`() {
        assertThat(testSubject.logicalHeight).isEqualTo(2560)
        assertThat(testSubject.primaryTranslationY).isEqualTo(-1280f)
    }

    @Test
    fun `only target display geometry should be eligible`() {
        assertThat(testSubject.matches(width = 1920, height = 1280)).isTrue()
        assertThat(testSubject.matches(width = 1280, height = 1920)).isFalse()
        assertThat(testSubject.matches(width = 1920, height = 1080)).isFalse()
    }

    @Test
    fun `upper display touch should keep the first viewport coordinate`() {
        assertThat(testSubject.secondaryLogicalY(640f)).isEqualTo(640f)
    }
}
