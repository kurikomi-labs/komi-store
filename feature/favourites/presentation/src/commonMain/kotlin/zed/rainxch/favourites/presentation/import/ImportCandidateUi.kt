package zed.rainxch.favourites.presentation.import

data class ImportCandidateUi(
    val repoId: Long,
    val owner: String,
    val name: String,
    val ownerAvatarUrl: String,
    val description: String?,
    val primaryLanguage: String?,
    val repoUrl: String,
    val stargazersCount: Int,
    val isAlreadyFavourited: Boolean,
)
