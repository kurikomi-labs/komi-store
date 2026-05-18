package zed.rainxch.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class HostToken(
    val host: String,
    val token: String,
    val displayName: String? = null,
    val createdAtEpochMillis: Long,
)

enum class ForgeKind(
    val tokenHost: String,
    val displayName: String,
    val tokenCreationUrl: String,
    val tokenTermNoun: String,
) {
    GITHUB(
        tokenHost = "github.com",
        displayName = "GitHub",
        tokenCreationUrl =
            "https://github.com/settings/tokens/new" +
                "?scopes=public_repo,read:user&description=GitHub%20Store",
        tokenTermNoun = "Personal access token",
    ),
    CODEBERG(
        tokenHost = "codeberg.org",
        displayName = "Codeberg",
        tokenCreationUrl = "https://codeberg.org/user/settings/applications",
        tokenTermNoun = "Access token",
    ),
    ;

    companion object {
        fun fromHost(host: String): ForgeKind? = entries.firstOrNull { it.tokenHost == host }
    }
}

object HostNames {
    const val GITHUB = "github.com"
    const val CODEBERG = "codeberg.org"

    fun normalize(raw: String): String {
        val trimmed = raw.trim().lowercase()
        val withoutScheme = trimmed.removePrefix("https://").removePrefix("http://")
        val hostOnly = withoutScheme.substringBefore('/')
        val noAuth = hostOnly.substringAfterLast('@')
        return noAuth.removeSuffix(".").removePrefix("www.").removePrefix("api.")
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
    fun sanitizePastedToken(raw: String): String {
        return raw.trim()
            .removePrefix("Bearer ")
            .removePrefix("bearer ")
            .removePrefix("Token ")
            .removePrefix("token ")
            .removePrefix("Authorization: token ")
            .removePrefix("Authorization: Bearer ")
            .trim()
    }

    fun detectPatKind(token: String): String? = when {
        token.startsWith("ghp_") -> "GitHub classic PAT"
        token.startsWith("github_pat_") -> "GitHub fine-grained PAT"
        token.startsWith("gho_") -> "GitHub OAuth token"
        token.startsWith("ghs_") -> "GitHub server token"
        else -> null
    }

    fun apiHostToTokenHost(apiHost: String): String {
        val normalized = normalize(apiHost)
        return when (normalized) {
            "api.github.com", "uploads.github.com", "raw.githubusercontent.com" -> GITHUB
            else -> normalized
        }
    }
}
