package zed.rainxch.apps.presentation.import.model

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class CandidateUi(
    val packageName: String,
    val appLabel: String,
    val versionName: String?,
    val installerLabel: String,
    val suggestions: ImmutableList<RepoSuggestionUi> = persistentListOf(),
    val preselectedSuggestion: RepoSuggestionUi? = null,
)
