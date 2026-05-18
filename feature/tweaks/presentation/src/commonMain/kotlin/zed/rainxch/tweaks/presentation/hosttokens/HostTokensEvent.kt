package zed.rainxch.tweaks.presentation.hosttokens

sealed interface HostTokensEvent {
    data class Message(val text: String) : HostTokensEvent
}
