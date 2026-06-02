package zed.rainxch.repopages.domain.repository

import zed.rainxch.repopages.domain.model.IssueComment
import zed.rainxch.repopages.domain.model.IssueState
import zed.rainxch.repopages.domain.model.RepoIssue
import zed.rainxch.repopages.domain.model.RepoIssueDetail
import zed.rainxch.repopages.domain.model.RepoPullRequest
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

    suspend fun addIssueComment(
        owner: String,
        repo: String,
        number: Int,
        body: String,
    ): Result<IssueComment>

    suspend fun createIssue(
        owner: String,
        repo: String,
        title: String,
        body: String,
    ): Result<Int>

    suspend fun reactToIssue(
        owner: String,
        repo: String,
        number: Int,
    ): Result<Unit>

    suspend fun reactToComment(
        owner: String,
        repo: String,
        commentId: Long,
    ): Result<Unit>

    suspend fun getPullRequests(
        owner: String,
        repo: String,
        state: IssueState,
        page: Int,
    ): Result<List<RepoPullRequest>>
}
