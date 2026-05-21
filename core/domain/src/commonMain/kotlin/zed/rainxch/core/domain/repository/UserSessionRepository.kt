package zed.rainxch.core.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import zed.rainxch.core.domain.model.UserProfile

interface UserSessionRepository {
    fun isUserLoggedIn(): Flow<Boolean>
    fun getUser(): Flow<UserProfile?>

    suspend fun isCurrentlyUserLoggedIn(): Boolean

    val sessionExpiredEvent: SharedFlow<Unit>

    suspend fun notifySessionExpired(tokenKey: String?)

    suspend fun notifyRequestSucceeded(tokenKey: String?)
    suspend fun logout()
}
