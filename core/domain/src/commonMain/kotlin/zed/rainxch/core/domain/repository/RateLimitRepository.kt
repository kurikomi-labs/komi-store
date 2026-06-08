package zed.rainxch.core.domain.repository

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import zed.rainxch.core.domain.model.error.RateLimitInfo

interface RateLimitRepository {
    val rateLimitState: StateFlow<RateLimitInfo?>
    val rateLimitExhaustedEvent: SharedFlow<RateLimitInfo>

    fun updateRateLimit(
        rateLimitInfo: RateLimitInfo?,
        notifyExhausted: Boolean = rateLimitInfo?.isExhausted == true,
    )

    fun getCurrentRateLimit(): RateLimitInfo?

    fun isCurrentlyLimited(): Boolean

    fun clear()
}
