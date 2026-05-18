package zed.rainxch.tweaks.presentation.hosttokens

import zed.rainxch.core.domain.model.HostToken

data class HostTokensState(
    val tokens: List<HostToken> = emptyList(),
    val draftHost: String = "",
    val draftToken: String = "",
    val draftDisplayName: String = "",
    val draftHostError: String? = null,
    val draftTokenError: String? = null,
    val isAddDialogVisible: Boolean = false,
    val isValidating: Boolean = false,
    val pendingValidationFor: String? = null,
    val validationMessage: String? = null,
    val isLoading: Boolean = false,
)
