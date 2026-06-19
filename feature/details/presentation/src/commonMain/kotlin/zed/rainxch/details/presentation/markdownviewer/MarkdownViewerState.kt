package zed.rainxch.details.presentation.markdownviewer

import zed.rainxch.details.presentation.model.TranslationState

data class MarkdownViewerState(
    val url: String = "",
    val isLoading: Boolean = false,
    val markdown: String = "",
    val errorMessage: String? = null,
    val translation: TranslationState = TranslationState(),
    val isLanguagePickerVisible: Boolean = false,
    val deviceLanguageCode: String = "en",
)
