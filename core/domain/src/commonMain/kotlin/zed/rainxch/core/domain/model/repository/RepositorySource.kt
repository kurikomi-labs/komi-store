package zed.rainxch.core.domain.model

sealed interface RepositorySource {
    val host: String

    data object GitHub : RepositorySource {
        override val host: String = "github.com"
    }

    data class Forgejo(override val host: String) : RepositorySource

    companion object {
        fun fromHost(host: String?): RepositorySource =
            when {
                host.isNullOrBlank() -> GitHub
                host.equals("github.com", ignoreCase = true) -> GitHub
                else -> Forgejo(host.lowercase())
            }
    }
}

data class RepositoryReference(
    val source: RepositorySource,
    val owner: String,
    val repo: String,
)
