package com.fsck.k9.activity

internal data class DualScreenSpanGeometry(
    val displayWidth: Int = 1920,
    val viewportHeight: Int = 1280,
) {
    val logicalHeight: Int = viewportHeight * 2
    val primaryTranslationY: Float = -viewportHeight.toFloat()

    fun matches(width: Int, height: Int): Boolean {
        return width == displayWidth && height == viewportHeight
    }

    fun secondaryLogicalY(physicalY: Float): Float = physicalY
}
