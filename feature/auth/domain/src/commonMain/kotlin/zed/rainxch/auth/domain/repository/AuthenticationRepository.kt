package zed.rainxch.auth.domain.repository

import kotlinx.coroutines.flow.Flow
import zed.rainxch.core.domain.model.account.github.GithubDeviceStart
import zed.rainxch.core.domain.model.account.github.GithubDeviceTokenSuccess

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
