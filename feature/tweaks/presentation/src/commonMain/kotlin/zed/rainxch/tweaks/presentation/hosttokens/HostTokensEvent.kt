package zed.rainxch.tweaks.presentation.hosttokens

import zed.rainxch.core.domain.model.HostToken

sealed interface HostTokensEvent {
    data class Message(val text: String) : HostTokensEvent

    /** Snackbar with undo when a token is removed. */
    data class TokenDeletedWithUndo(val deleted: HostToken) : HostTokensEvent

    /** Open URL in browser (token creation pages on each forge). */
    data class OpenUrl(val url: String) : HostTokensEvent
}
