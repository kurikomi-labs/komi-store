package zed.rainxch.core.data.network

import zed.rainxch.core.data.dto.ExternalMatchRequest
import zed.rainxch.core.data.dto.ExternalMatchResponse

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
