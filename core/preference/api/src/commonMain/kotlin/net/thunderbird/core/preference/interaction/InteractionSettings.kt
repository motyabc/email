package net.thunderbird.core.preference.interaction

import net.thunderbird.core.common.action.SwipeAction
import net.thunderbird.core.common.action.SwipeActions

const val INTERACTION_SETTINGS_DEFAULT_USE_VOLUME_KEYS_NAVIGATION = false
val INTERACTION_SETTINGS_DEFAULT_MESSAGE_VIEW_POST_REMOVE_NAVIGATION = PostRemoveNavigation.ReturnToMessageList.name
val INTERACTION_SETTINGS_DEFAULT_MESSAGE_VIEW_POST_MARK_AS_UNREAD_NAVIGATION =
    PostMarkAsUnreadNavigation.ReturnToMessageList
val INTERACTION_SETTINGS_DEFAULT_SWIPE_ACTION = SwipeActions(
    leftAction = SwipeAction.ToggleRead,
    rightAction = SwipeAction.ToggleSelection,
)
const val INTERACTION_SETTINGS_DEFAULT_CONFIRM_DELETE = false
const val INTERACTION_SETTINGS_DEFAULT_CONFIRM_DELETE_STARRED = false
const val INTERACTION_SETTINGS_DEFAULT_CONFIRM_DELETE_FROM_NOTIFICATION = true
const val INTERACTION_SETTINGS_DEFAULT_CONFIRM_SPAM = false
const val INTERACTION_SETTINGS_DEFAULT_CONFIRM_DISCARD_MESSAGE = true
const val INTERACTION_SETTINGS_DEFAULT_CONFIRM_MARK_ALL_READ = true
const val INTERACTION_SETTINGS_DEFAULT_DUAL_SCREEN_KEY_CODE = 0
val INTERACTION_SETTINGS_DEFAULT_DUAL_SCREEN_KEY_BINDING = DualScreenKeyBinding()

data class InteractionSettings(
    val useVolumeKeysForNavigation: Boolean = INTERACTION_SETTINGS_DEFAULT_USE_VOLUME_KEYS_NAVIGATION,
    val dualScreenKeyBinding: DualScreenKeyBinding = INTERACTION_SETTINGS_DEFAULT_DUAL_SCREEN_KEY_BINDING,
    val messageViewPostRemoveNavigation: String = INTERACTION_SETTINGS_DEFAULT_MESSAGE_VIEW_POST_REMOVE_NAVIGATION,
    var messageViewPostMarkAsUnreadNavigation: PostMarkAsUnreadNavigation =
        INTERACTION_SETTINGS_DEFAULT_MESSAGE_VIEW_POST_MARK_AS_UNREAD_NAVIGATION,
    val swipeActions: SwipeActions = INTERACTION_SETTINGS_DEFAULT_SWIPE_ACTION,
    val isConfirmDelete: Boolean = INTERACTION_SETTINGS_DEFAULT_CONFIRM_DELETE,
    val isConfirmDeleteStarred: Boolean = INTERACTION_SETTINGS_DEFAULT_CONFIRM_DELETE_STARRED,
    val isConfirmDeleteFromNotification: Boolean = INTERACTION_SETTINGS_DEFAULT_CONFIRM_DELETE_FROM_NOTIFICATION,
    val isConfirmSpam: Boolean = INTERACTION_SETTINGS_DEFAULT_CONFIRM_SPAM,
    val isConfirmDiscardMessage: Boolean = INTERACTION_SETTINGS_DEFAULT_CONFIRM_DISCARD_MESSAGE,
    val isConfirmMarkAllRead: Boolean = INTERACTION_SETTINGS_DEFAULT_CONFIRM_MARK_ALL_READ,
)

/**
 * A user-learned, app-local hardware key binding for the KEMI dual-screen workspace.
 *
 * The Android-specific input policy decides which key codes can be learned. Keeping the raw code here avoids
 * guessing vendor mappings and lets the binding remain disabled until the user explicitly configures it.
 */
data class DualScreenKeyBinding(
    val keyCode: Int = INTERACTION_SETTINGS_DEFAULT_DUAL_SCREEN_KEY_CODE,
    val action: DualScreenKeyAction = DualScreenKeyAction.DISABLED,
) {
    val isConfigured: Boolean
        get() = keyCode > INTERACTION_SETTINGS_DEFAULT_DUAL_SCREEN_KEY_CODE && action != DualScreenKeyAction.DISABLED
}

enum class DualScreenKeyAction {
    DISABLED,
    OPEN_MODE_SELECTOR,
    TOGGLE_MODE,
    PREVIOUS_MESSAGE,
    NEXT_MESSAGE,
}

/**
 * The navigation actions that can be to performed after the user has deleted or moved a message from the message
 * view screen.
 */
enum class PostRemoveNavigation {
    ReturnToMessageList,
    ShowPreviousMessage,
    ShowNextMessage,
}

/**
 * The navigation actions that can be to performed after the user has marked a
 * message as unread from the message view screen.
 */
enum class PostMarkAsUnreadNavigation {
    StayOnCurrentMessage,
    ReturnToMessageList,
}
