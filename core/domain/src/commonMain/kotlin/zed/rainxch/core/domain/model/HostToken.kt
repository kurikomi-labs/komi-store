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

    /**
     * Maps a request URL host to the canonical token storage host.
     *
     * GitHub stores the PAT under [GITHUB] (`github.com`), but real API
     * requests go to `api.github.com` — without this collapse step the
     * interceptor's `repo.get(request.url.host)` returns null on every
     * GitHub REST call.
     *
     * Forgejo/Codeberg/Gitea instances expose their REST API at
     * `https://<host>/api/v1/...` (same host as the storage key), so
     * they need no mapping.
     */
    fun apiHostToTokenHost(apiHost: String): String {
        val normalized = normalize(apiHost)
        return when (normalized) {
            "api.github.com", "uploads.github.com", "raw.githubusercontent.com" -> GITHUB
            else -> normalized
        }
    }
}
