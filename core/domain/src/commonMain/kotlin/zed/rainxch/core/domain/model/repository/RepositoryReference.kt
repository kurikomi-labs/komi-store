package zed.rainxch.core.domain.model.repository

data class RepositoryReference(
    val source: RepositorySource,
    val owner: String,
    val repo: String,
)
