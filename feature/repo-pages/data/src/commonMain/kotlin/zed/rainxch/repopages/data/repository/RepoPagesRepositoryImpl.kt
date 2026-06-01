package zed.rainxch.repopages.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import zed.rainxch.core.data.network.GitHubClientProvider
import zed.rainxch.core.domain.logging.GitHubStoreLogger
import zed.rainxch.core.domain.model.RateLimitException
import zed.rainxch.repopages.data.dto.IssueCommentDto
import zed.rainxch.repopages.data.dto.IssueDto
import zed.rainxch.repopages.data.dto.RepoContentDto
import zed.rainxch.repopages.data.dto.SecurityAdvisoryDto
import zed.rainxch.repopages.data.mappers.toApiValue
import zed.rainxch.repopages.data.mappers.toIssueComment
import zed.rainxch.repopages.data.mappers.toIssueLabel
import zed.rainxch.repopages.data.mappers.toIssueState
import zed.rainxch.repopages.data.mappers.toRepoIssue
import zed.rainxch.repopages.data.mappers.toSecurityAdvisory
import zed.rainxch.repopages.domain.model.IssueState
import zed.rainxch.repopages.domain.model.RepoIssue
import zed.rainxch.repopages.domain.model.RepoIssueDetail
import zed.rainxch.repopages.domain.model.SecurityAdvisory
import zed.rainxch.repopages.domain.model.SecurityOverview
import zed.rainxch.repopages.domain.repository.RepoPagesRepository

class RepoPagesRepositoryImpl(
    private val clientProvider: GitHubClientProvider,
    private val logger: GitHubStoreLogger,
) : RepoPagesRepository {
    private val httpClient: HttpClient get() = clientProvider.client

    override suspend fun getIssues(
        owner: String,
        repo: String,
        state: IssueState,
        page: Int,
    ): Result<List<RepoIssue>> = withContext(Dispatchers.IO) {
        try {
            val response = httpClient.get("/repos/$owner/$repo/issues") {
                parameter("state", state.toApiValue())
                parameter("per_page", PER_PAGE)
                parameter("page", page)
                parameter("sort", "created")
                parameter("direction", "desc")
            }
            if (!response.status.isSuccess()) {
                return@withContext Result.failure(
                    Exception("Failed to fetch issues: ${response.status.description}"),
                )
            }
            val dtos: List<IssueDto> = response.body()
            val issues = dtos.filter { it.pullRequest == null }.map { it.toRepoIssue() }
            Result.success(issues)
        } catch (e: RateLimitException) {
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.error("Failed to fetch issues for $owner/$repo: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun getIssueDetail(
        owner: String,
        repo: String,
        number: Int,
    ): Result<RepoIssueDetail> = withContext(Dispatchers.IO) {
        try {
            coroutineScope {
                val issueDeferred = async {
                    httpClient.get("/repos/$owner/$repo/issues/$number")
                }
                val commentsDeferred = async {
                    httpClient.get("/repos/$owner/$repo/issues/$number/comments") {
                        parameter("per_page", COMMENTS_PER_PAGE)
                    }
                }

                val issueResponse = issueDeferred.await()
                if (!issueResponse.status.isSuccess()) {
                    return@coroutineScope Result.failure(
                        Exception("Failed to fetch issue: ${issueResponse.status.description}"),
                    )
                }
                val issueDto: IssueDto = issueResponse.body()

                val commentsResponse = commentsDeferred.await()
                val comments = if (commentsResponse.status.isSuccess()) {
                    val commentDtos: List<IssueCommentDto> = commentsResponse.body()
                    commentDtos.map { it.toIssueComment() }
                } else {
                    emptyList()
                }

                Result.success(
                    RepoIssueDetail(
                        number = issueDto.number,
                        title = issueDto.title,
                        state = issueDto.state.toIssueState(),
                        authorLogin = issueDto.user?.login.orEmpty(),
                        authorAvatarUrl = issueDto.user?.avatarUrl,
                        bodyMarkdown = issueDto.body.orEmpty(),
                        createdAt = issueDto.createdAt,
                        labels = issueDto.labels.map { it.toIssueLabel() },
                        comments = comments,
                    ),
                )
            }
        } catch (e: RateLimitException) {
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.error("Failed to fetch issue $owner/$repo#$number: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun getSecurityOverview(
        owner: String,
        repo: String,
    ): Result<SecurityOverview> = withContext(Dispatchers.IO) {
        try {
            coroutineScope {
                val advisoriesDeferred = async { fetchAdvisories(owner, repo) }
                val policyDeferred = async { fetchSecurityPolicy(owner, repo) }
                Result.success(
                    SecurityOverview(
                        advisories = advisoriesDeferred.await(),
                        securityPolicyMarkdown = policyDeferred.await(),
                    ),
                )
            }
        } catch (e: RateLimitException) {
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.error("Failed to fetch security overview for $owner/$repo: ${e.message}")
            Result.failure(e)
        }
    }

    private suspend fun fetchAdvisories(
        owner: String,
        repo: String,
    ): List<SecurityAdvisory> {
        return try {
            val response = httpClient.get("/repos/$owner/$repo/security-advisories") {
                parameter("per_page", PER_PAGE)
                parameter("state", "published")
            }
            if (!response.status.isSuccess()) return emptyList()
            val dtos: List<SecurityAdvisoryDto> = response.body()
            dtos.map { it.toSecurityAdvisory() }
        } catch (e: RateLimitException) {
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.warn("Failed to fetch advisories for $owner/$repo: ${e.message}")
            emptyList()
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    private suspend fun fetchSecurityPolicy(
        owner: String,
        repo: String,
    ): String? {
        for (path in SECURITY_PATHS) {
            try {
                val response = httpClient.get("/repos/$owner/$repo/contents/$path")
                if (response.status == HttpStatusCode.NotFound) continue
                if (!response.status.isSuccess()) continue
                val dto: RepoContentDto = response.body()
                val raw = dto.content ?: continue
                val decoded = Base64.Mime.decode(raw).decodeToString()
                if (decoded.isNotBlank()) return decoded
            } catch (e: RateLimitException) {
                throw e
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.warn("Failed to read $path for $owner/$repo: ${e.message}")
            }
        }
        return null
    }

    companion object {
        private const val PER_PAGE = 30
        private const val COMMENTS_PER_PAGE = 100
        private val SECURITY_PATHS = listOf(
            "SECURITY.md",
            ".github/SECURITY.md",
            "docs/SECURITY.md",
        )
    }
}
