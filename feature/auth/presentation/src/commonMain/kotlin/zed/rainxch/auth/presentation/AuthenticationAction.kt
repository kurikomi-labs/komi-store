package zed.rainxch.auth.presentation

import zed.rainxch.auth.presentation.model.GithubDeviceStartUi

sealed interface AuthenticationAction {
    data object StartLogin : AuthenticationAction

    data class CopyCode(
        val start: GithubDeviceStartUi,
    ) : AuthenticationAction

    data class OpenGitHub(
        val start: GithubDeviceStartUi,
    ) : AuthenticationAction

    data object MarkLoggedOut : AuthenticationAction

    data object MarkLoggedIn : AuthenticationAction

    data class OnInfo(
        val message: String,
    ) : AuthenticationAction

    data object SkipLogin : AuthenticationAction

    data object PollNow : AuthenticationAction

    data object OnResumed : AuthenticationAction

    data object OpenPatSheet : AuthenticationAction

    data object DismissPatSheet : AuthenticationAction

    data class OnPatInputChanged(
        val input: String,
    ) : AuthenticationAction

    data object SubmitPat : AuthenticationAction

    data object OpenPatSettingsPage : AuthenticationAction

    data object StartWebAuth : AuthenticationAction

    data class ConsumeAuthHandoff(
        val handoffId: String,
        val state: String,
    ) : AuthenticationAction

    data class ConsumeAuthError(
        val reason: String,
        val state: String,
    ) : AuthenticationAction

    data object DismissAdvancedAuth : AuthenticationAction

    data object OpenAdvancedAuth : AuthenticationAction
}
