package zed.rainxch.core.domain.model.smart_detect
import zed.rainxch.core.domain.model.account.github.GithubAsset
import zed.rainxch.core.domain.model.account.github.GithubRelease

data class MatchingPreview(
    val release: GithubRelease?,
    val matchedAssets: List<GithubAsset>,
    val regexError: String? = null,
)
