package zed.rainxch.apps.presentation.import.model

data class RepoSuggestionUi(
    val owner: String,
    val repo: String,
    val confidence: Double,
    val source: SuggestionSource,
    val stars: Int? = null,
    val description: String? = null,
    val sourceHost: String? = null,
)
