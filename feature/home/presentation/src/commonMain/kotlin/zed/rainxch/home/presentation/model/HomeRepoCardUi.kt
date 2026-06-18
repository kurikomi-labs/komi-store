package zed.rainxch.home.presentation.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import zed.rainxch.core.presentation.model.GithubRepoSummaryUi

@Immutable
data class HomeRepoCardUi(
    val id: Long,
    val name: String,
    val ownerLogin: String,
    val ownerAvatarUrl: String,
    val description: String,
    val starsCount: Int,
    val downloadsCount: Long,
    val language: String?,
    val topics: ImmutableList<String>,
    val isInstalled: Boolean,
    val isUpdateAvailable: Boolean,
    val isFavourite: Boolean,
    val isStarred: Boolean,
    val isSeen: Boolean,
    val isCurrentUserOwner: Boolean,
    val rawRepository: GithubRepoSummaryUi,
)
