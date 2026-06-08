package zed.rainxch.core.domain.system

data class RepoMatchResult(
    val packageName: String,
    val suggestions: List<RepoMatchSuggestion>,
) {
    val topSuggestion: RepoMatchSuggestion?
        get() = suggestions.maxByOrNull { it.confidence }
}
