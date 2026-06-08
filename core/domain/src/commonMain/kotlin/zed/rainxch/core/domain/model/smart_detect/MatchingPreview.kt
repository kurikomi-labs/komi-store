package zed.rainxch.core.domain.model

data class MatchingPreview(
    val release: GithubRelease?,
    val matchedAssets: List<GithubAsset>,
    val regexError: String? = null,
)
