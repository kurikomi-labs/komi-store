package zed.rainxch.details.presentation

import zed.rainxch.core.domain.model.error.RefreshError

sealed interface DetailsEvent {
    data class OnOpenRepositoryInApp(
        val repositoryId: Long,
    ) : DetailsEvent

    data class OnMessage(
        val message: String,
    ) : DetailsEvent

    data class OnRefreshError(
        val kind: RefreshError,
        val retryAfterSeconds: Long? = null,
    ) : DetailsEvent
}
