package zed.rainxch.details.presentation.about

import zed.rainxch.details.presentation.model.TranslationState

data class DetailsAboutState(
    val isLoading: Boolean = true,
    val repoName: String = "",
    val readmeMarkdown: String = "",
    val readmeLanguage: String? = null,
    val errorMessage: String? = null,
    val deviceLanguageCode: String = "en",
    val translation: TranslationState = TranslationState(),
    val isLanguagePickerVisible: Boolean = false,
)
