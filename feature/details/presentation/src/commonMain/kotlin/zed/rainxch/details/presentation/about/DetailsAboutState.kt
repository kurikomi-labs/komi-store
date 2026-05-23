package zed.rainxch.details.presentation.about

data class DetailsAboutState(
    val isLoading: Boolean = true,
    val repoName: String = "",
    val readmeMarkdown: String = "",
    val readmeLanguage: String? = null,
    val errorMessage: String? = null,
)
