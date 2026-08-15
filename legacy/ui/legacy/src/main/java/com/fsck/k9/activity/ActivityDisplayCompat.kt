package com.fsck.k9.activity

import android.app.Activity

/** Returns the display hosting this Activity without requiring API 30 on supported legacy devices. */
@Suppress("DEPRECATION")
internal fun Activity.getDisplayIdCompat(): Int = windowManager.defaultDisplay.displayId
