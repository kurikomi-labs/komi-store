package zed.rainxch.favourites.presentation.import

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class ImportStarsState(
    val phase: Phase = Phase.UsernameInput,
    val usernameQuery: String = "",
    val importedUsername: String? = null,
    val isImporting: Boolean = false,
    val candidates: ImmutableList<ImportCandidateUi> = persistentListOf(),
    val filteredCandidates: ImmutableList<ImportCandidateUi> = persistentListOf(),
    val pendingCount: Int = 0,
    val searchQuery: String = "",
    val errorMessage: String? = null,
    val isBulkAdding: Boolean = false,
) {
    enum class Phase {
        UsernameInput,
        Results,
    }
}
