package zed.rainxch.auth.domain.repository

import kotlinx.coroutines.flow.Flow
import zed.rainxch.core.domain.model.GithubDeviceStart
import zed.rainxch.core.domain.model.GithubDeviceTokenSuccess

interface AuthenticationRepository {
    val accessTokenFlow: Flow<String?>

    suspend fun startDeviceFlow(): DeviceFlowStart

    suspend fun awaitDeviceToken(start: GithubDeviceStart): GithubDeviceTokenSuccess

    suspend fun pollDeviceTokenOnce(
        deviceCode: String,
        path: AuthPath,
    ): PollOutcome

    suspend fun signInWithPat(token: String): Result<Unit>

    suspend fun registerWebAuth(): Result<WebAuthRegistration>

    suspend fun exchangeWebAuthHandoff(handoffId: String): Result<String>
}

data class WebAuthRegistration(
    val state: String,
    val authUrl: String,
)

enum class AuthPath { Backend, Direct }

data class DeviceFlowStart(
    val start: GithubDeviceStart,
    val path: AuthPath,
)

data class PollOutcome(
    val result: DevicePollResult,
    val path: AuthPath,
)

sealed interface DevicePollResult {
    data class Success(val token: GithubDeviceTokenSuccess) : DevicePollResult

    data object Pending : DevicePollResult

    data object SlowDown : DevicePollResult

    data class Failed(val error: Throwable) : DevicePollResult
}

sealed interface RejectedKind {

    data object BadCredentials : RejectedKind

    data object InsufficientScope : RejectedKind

    data class Other(val statusCode: Int) : RejectedKind
}

class PatRejectedException(val kind: RejectedKind) : Exception("PAT rejected: $kind")
