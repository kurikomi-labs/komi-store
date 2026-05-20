package zed.rainxch.core.domain.system

data class RepoMatchSuggestion(
    val owner: String,
    val repo: String,
    val confidence: Double,
    val source: RepoMatchSource,
    val stars: Int? = null,
    val description: String? = null,
    // Non-null when the suggestion lives on a non-GitHub forge
    // (Codeberg / Forgejo / custom). Drives the URL builder in the
    // "select suggestion" path so we don't navigate to github.com for
    // a Forgejo repo.
    val sourceHost: String? = null,
)

data class RepoMatchResult(
    val packageName: String,
    val suggestions: List<RepoMatchSuggestion>,
) {
    val topConfidence: Double
        get() = suggestions.maxOfOrNull { it.confidence } ?: 0.0

    val topSuggestion: RepoMatchSuggestion?
        get() = suggestions.maxByOrNull { it.confidence }
}
