package zed.rainxch.core.domain.system

data class ExternalDecisionSnapshot(
    val packageName: String,
    val state: ExternalLinkState?,
    val repoOwner: String?,
    val repoName: String?,
    val matchSource: String?,
    val matchConfidence: Double?,
    val skipExpiresAt: Long?,
)
