package zed.rainxch.tweaks.presentation.hosttokens

import org.jetbrains.compose.resources.StringResource
import zed.rainxch.core.domain.model.ForgeKind
import zed.rainxch.core.domain.model.HostToken

data class HostTokensState(
    val tokens: List<HostToken> = emptyList(),

    val validationByHost: Map<String, ValidationLine> = emptyMap(),

    val validatingHosts: Set<String> = emptySet(),

    val draftMode: DraftMode = DraftMode.Closed,
    val draftForge: ForgeKind? = null,
    val draftHost: String = "",
    val draftHostNormalized: String = "",
    val draftToken: String = "",
    val draftDisplayName: String = "",
    val draftHostError: StringResource? = null,
    val draftTokenError: StringResource? = null,
    val draftDetectedTokenKind: String? = null,
    val isLoading: Boolean = false,
    val isOAuthSignedInToGithub: Boolean = false,
    val pendingUndoDelete: HostToken? = null,
)

sealed interface DraftMode {
    data object Closed : DraftMode

    data object Picker : DraftMode

    data class Compose(val replacingExisting: HostToken? = null) : DraftMode
}

data class ValidationLine(
    val login: String?,
    val scopes: List<String>,
    val rateLimitRemaining: Int?,
    val errorMessage: String?,
) {
    val isSuccess: Boolean get() = errorMessage == null
}
