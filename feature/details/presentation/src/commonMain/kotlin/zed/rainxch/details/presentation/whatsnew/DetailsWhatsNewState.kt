package zed.rainxch.details.presentation.whatsnew

import zed.rainxch.core.domain.model.GithubRelease

data class DetailsWhatsNewState(
    val isLoading: Boolean = true,
    val repoName: String = "",
    val releases: List<GithubRelease> = emptyList(),
    val errorMessage: String? = null,
)
