package zed.rainxch.repopages.domain.repository

import zed.rainxch.repopages.domain.model.IssueState
import zed.rainxch.repopages.domain.model.RepoIssue
import zed.rainxch.repopages.domain.model.RepoIssueDetail
import zed.rainxch.repopages.domain.model.SecurityOverview

interface RepoPagesRepository {
    suspend fun getIssues(
        owner: String,
        repo: String,
        state: IssueState,
        page: Int,
    ): Result<List<RepoIssue>>

    suspend fun getIssueDetail(
        owner: String,
        repo: String,
        number: Int,
    ): Result<RepoIssueDetail>

    suspend fun getSecurityOverview(
        owner: String,
        repo: String,
    ): Result<SecurityOverview>
}
