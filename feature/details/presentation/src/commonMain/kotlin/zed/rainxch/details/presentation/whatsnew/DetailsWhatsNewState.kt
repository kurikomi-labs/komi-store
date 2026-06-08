package zed.rainxch.details.presentation.whatsnew

import zed.rainxch.core.domain.model.account.github.GithubRelease
import zed.rainxch.details.presentation.model.TranslationState

data class DetailsWhatsNewState(
    val isLoading: Boolean = true,
    val repoName: String = "",
    val releases: List<GithubRelease> = emptyList(),
    val errorMessage: String? = null,
    val deviceLanguageCode: String = "en",
    val translation: TranslationState = TranslationState(),
    val isLanguagePickerVisible: Boolean = false,
)
