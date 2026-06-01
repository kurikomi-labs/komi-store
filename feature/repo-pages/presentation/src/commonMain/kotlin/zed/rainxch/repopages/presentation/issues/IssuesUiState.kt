package zed.rainxch.repopages.presentation.issues

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import zed.rainxch.repopages.domain.model.IssueState
import zed.rainxch.repopages.domain.model.RepoIssue

data class IssuesUiState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val filter: IssueState = IssueState.OPEN,
    val issues: ImmutableList<RepoIssue> = persistentListOf(),
    val page: Int = 1,
    val endReached: Boolean = false,
    val errorMessage: String? = null,
)
