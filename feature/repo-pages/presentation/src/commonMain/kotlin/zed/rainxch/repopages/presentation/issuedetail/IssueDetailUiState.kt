package zed.rainxch.repopages.presentation.issuedetail

import zed.rainxch.repopages.domain.model.RepoIssueDetail

data class IssueDetailUiState(
    val isLoading: Boolean = false,
    val detail: RepoIssueDetail? = null,
    val errorMessage: String? = null,
)
