package zed.rainxch.auth.presentation

sealed interface AuthDeepLinkEvent {
    data class Handoff(
        val handoffId: String,
        val state: String,
    ) : AuthDeepLinkEvent

    data class Error(
        val reason: String,
        val state: String,
    ) : AuthDeepLinkEvent
}
