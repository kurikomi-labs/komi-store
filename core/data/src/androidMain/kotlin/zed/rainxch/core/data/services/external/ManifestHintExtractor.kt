package zed.rainxch.core.data.services.external

import android.os.Bundle
import zed.rainxch.core.domain.system.ManifestHint
import zed.rainxch.core.domain.system.ManifestHintSource

class ManifestHintExtractor {
    fun extract(metaData: Bundle?): ManifestHint? {
        if (metaData == null) return null

        metaData.getString(KEY_GITHUB_REPO)?.trim()?.takeIf { it.isNotEmpty() }?.let { raw ->
            parseOwnerRepoLiteral(raw)?.let { (owner, repo) ->
                return ManifestHint(
                    owner = owner,
                    repo = repo,
                    source = ManifestHintSource.META_GITHUB_REPO,
                    confidence = 0.95,
                )
            }
        }

        for (entry in URL_KEYS) {
            val raw = metaData.getString(entry.name)?.trim() ?: continue
            val parsed = parseGithubUrl(raw) ?: continue
            return ManifestHint(
                owner = parsed.first,
                repo = parsed.second,
                source = entry.source,
                confidence = entry.confidence,
            )
        }
        return null
    }

    fun parseOwnerRepoLiteral(raw: String): Pair<String, String>? {
        if (!OWNER_REPO_REGEX.matches(raw)) return null
        val parts = raw.split('/')
        if (parts.size != 2) return null
        return parts[0] to stripRepoSuffix(parts[1])
    }

    fun parseGithubUrl(raw: String): Pair<String, String>? {
        val cleaned = raw.trim().removeSurrounding("\"")
        val match = URL_REGEX.find(cleaned) ?: return null
        val owner = match.groupValues[1]
        val repoSegment = match.groupValues[2]
        if (owner.isEmpty() || repoSegment.isEmpty()) return null
        if (!OWNER_REGEX.matches(owner)) return null
        val repo = stripRepoSuffix(repoSegment)
        if (!REPO_NAME_REGEX.matches(repo)) return null
        return owner to repo
    }

    private fun stripRepoSuffix(repo: String): String {
        var trimmed = repo.trimEnd('/')
        if (trimmed.endsWith(".git", ignoreCase = true)) {
            trimmed = trimmed.removeSuffix(".git").removeSuffix(".GIT")
        }
        return trimmed
    }

    companion object {
        const val KEY_GITHUB_REPO = "github_repo"
        const val KEY_FDROID_SOURCE_CODE = "org.fdroid.fdroid.SourceCode"
        const val KEY_UPSTREAM_URL = "upstream_url"
        const val KEY_APP_REPO_URL = "app_repo_url"

        private data class UrlKey(
            val name: String,
            val source: ManifestHintSource,
            val confidence: Double,
        )

        // declaration order = priority order
        private val URL_KEYS =
            listOf(
                UrlKey(KEY_FDROID_SOURCE_CODE, ManifestHintSource.META_FDROID_SOURCE_CODE, 0.85),
                UrlKey(KEY_UPSTREAM_URL, ManifestHintSource.META_UPSTREAM_URL, 0.80),
                UrlKey(KEY_APP_REPO_URL, ManifestHintSource.META_APP_REPO_URL, 0.75),
            )

        private val OWNER_REPO_REGEX = Regex("^[\\w.-]{1,39}/[\\w.-]{1,100}$")
        private val OWNER_REGEX = Regex("^[\\w.-]{1,39}$")
        private val REPO_NAME_REGEX = Regex("^[\\w.-]{1,100}$")

        // Matches https?://github.com/<owner>/<repo>(/...)? — first two path segments only
        private val URL_REGEX =
            Regex(
                "(?i)^https?://(?:www\\.)?github\\.com/([\\w.-]+)/([\\w.-]+)(?:[/?#].*)?$",
            )
    }
}
