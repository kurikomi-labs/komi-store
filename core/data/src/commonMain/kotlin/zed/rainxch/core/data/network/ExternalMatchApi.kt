package zed.rainxch.core.data.network

import zed.rainxch.core.data.dto.ExternalMatchRequest
import zed.rainxch.core.data.dto.ExternalMatchResponse

interface ExternalMatchApi {
    suspend fun match(request: ExternalMatchRequest): Result<ExternalMatchResponse>
}
