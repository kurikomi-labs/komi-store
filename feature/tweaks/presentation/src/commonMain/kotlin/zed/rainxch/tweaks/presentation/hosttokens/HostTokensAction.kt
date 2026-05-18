package zed.rainxch.tweaks.presentation.hosttokens

sealed interface HostTokensAction {
    data object OnNavigateBack : HostTokensAction
    data object OnAddClicked : HostTokensAction
    data object OnAddDismiss : HostTokensAction
    data class OnDraftHostChanged(val value: String) : HostTokensAction
    data class OnDraftTokenChanged(val value: String) : HostTokensAction
    data class OnDraftDisplayNameChanged(val value: String) : HostTokensAction
    data object OnAddConfirm : HostTokensAction
    data class OnDelete(val host: String) : HostTokensAction
    data class OnValidate(val host: String) : HostTokensAction
}
