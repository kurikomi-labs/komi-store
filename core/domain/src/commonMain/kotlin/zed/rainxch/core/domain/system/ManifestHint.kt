package zed.rainxch.core.domain.system

enum class ManifestHintSource {
    META_GITHUB_REPO,
    META_FDROID_SOURCE_CODE,
    META_UPSTREAM_URL,
    META_APP_REPO_URL,
}

data class ManifestHint(
    val owner: String,
    val repo: String,
    val source: ManifestHintSource,
    val confidence: Double,
)
