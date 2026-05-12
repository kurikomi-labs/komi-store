package zed.rainxch.tweaks.presentation.hidden

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class HiddenRepositoriesState(
    val isLoading: Boolean = true,
    val items: ImmutableList<HiddenRepoUi> = persistentListOf(),
)

data class HiddenRepoUi(
    val repoId: Long,
    val repoName: String,
    val repoOwner: String,
    val repoOwnerAvatarUrl: String,
) {
    val fullName: String get() = "$repoOwner/$repoName"
}
