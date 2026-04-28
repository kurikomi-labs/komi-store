package zed.rainxch.core.data.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import zed.rainxch.core.data.dto.ExternalMatchRequest
import zed.rainxch.core.data.dto.ExternalMatchResponse
import zed.rainxch.core.domain.repository.TweaksRepository

interface ExternalMatchApi {
    // NOTE: chunking lives in the repo; impls receive whatever the repo passes
    suspend fun match(request: ExternalMatchRequest): Result<ExternalMatchResponse>
}

class BackendExternalMatchApi(
    private val backendClient: BackendApiClient,
) : ExternalMatchApi {
    override suspend fun match(request: ExternalMatchRequest): Result<ExternalMatchResponse> {
        if (request.candidates.size <= MAX_BATCH_SIZE) {
            return backendClient.postExternalMatch(request)
        }
        val merged = mutableListOf<ExternalMatchResponse.MatchEntry>()
        for (batch in request.candidates.chunked(MAX_BATCH_SIZE)) {
            val sub = ExternalMatchRequest(platform = request.platform, candidates = batch)
            val result = backendClient.postExternalMatch(sub)
            result.onFailure { return Result.failure(it) }
            result.onSuccess { merged += it.matches }
        }
        return Result.success(ExternalMatchResponse(matches = merged))
    }

    companion object {
        private const val MAX_BATCH_SIZE = 25
    }
}

class MockExternalMatchApi : ExternalMatchApi {
    override suspend fun match(request: ExternalMatchRequest): Result<ExternalMatchResponse> =
        Result.success(
            ExternalMatchResponse(
                matches = request.candidates.map {
                    ExternalMatchResponse.MatchEntry(
                        packageName = it.packageName,
                        candidates = emptyList(),
                    )
                },
            ),
        )
}

class ExternalMatchApiSelector(
    private val real: BackendExternalMatchApi,
    private val mock: MockExternalMatchApi,
    tweaks: TweaksRepository,
    scope: CoroutineScope,
) : ExternalMatchApi {
    // Cache the flag in a hot StateFlow so `match()` can read it
    // synchronously instead of round-tripping DataStore on every call.
    private val flagState = tweaks.getExternalMatchSearchEnabled()
        .stateIn(scope, SharingStarted.Eagerly, initialValue = false)

    override suspend fun match(request: ExternalMatchRequest): Result<ExternalMatchResponse> =
        if (flagState.value) {
            real.match(request)
        } else {
            mock.match(request)
        }
}
