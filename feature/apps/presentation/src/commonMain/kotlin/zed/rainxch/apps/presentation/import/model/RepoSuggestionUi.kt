package zed.rainxch.apps.presentation.import.model

data class RepoSuggestionUi(
    val owner: String,
    val repo: String,
    val confidence: Double,
    val source: SuggestionSource,
    val stars: Int? = null,
    val description: String? = null,
    // Non-null when the suggestion lives on a non-GitHub forge.
    // Carries through from import paste / smart-match to the linker
    // so the row is actually stored against the right host.
    val sourceHost: String? = null,
) {
    val ownerSlashRepo: String get() = "$owner/$repo"
}

enum class SuggestionSource {
    MANIFEST,
    SEARCH,
    FINGERPRINT,
    MANUAL,
}
