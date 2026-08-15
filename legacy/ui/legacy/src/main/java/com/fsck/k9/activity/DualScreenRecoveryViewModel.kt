package com.fsck.k9.activity

import androidx.lifecycle.ViewModel

/** Retains hot-plug recovery only across Activity recreation, not across process death or a new task. */
internal class DualScreenRecoveryViewModel : ViewModel() {
    var recoveryPending: Boolean = false
}
