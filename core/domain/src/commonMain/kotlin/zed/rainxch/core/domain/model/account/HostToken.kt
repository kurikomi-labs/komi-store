package zed.rainxch.core.domain.model.account
import kotlinx.serialization.Serializable

@Serializable
data class HostToken(
    val host: String,
    val token: String,
    val displayName: String? = null,
    val createdAtEpochMillis: Long,
)
