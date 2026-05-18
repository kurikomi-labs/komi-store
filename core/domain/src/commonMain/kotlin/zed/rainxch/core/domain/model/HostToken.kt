package zed.rainxch.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class HostToken(
    val host: String,
    val token: String,
    val displayName: String? = null,
    val createdAtEpochMillis: Long,
)

object HostNames {
    const val GITHUB = "github.com"
    const val CODEBERG = "codeberg.org"

    fun normalize(raw: String): String {
        val trimmed = raw.trim().lowercase()
        val withoutScheme = trimmed.removePrefix("https://").removePrefix("http://")
        val hostOnly = withoutScheme.substringBefore('/')
        return hostOnly.removeSuffix(".")
    }
}
