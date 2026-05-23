package zed.rainxch.home.presentation.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import kotlinx.collections.immutable.ImmutableList
import zed.rainxch.core.presentation.model.GithubRepoSummaryUi
import zed.rainxch.core.presentation.vocabulary.FreshnessState
import zed.rainxch.core.presentation.vocabulary.PlatformKind

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
    val daysSinceUpdate: Int,
    val relativeAgoLabel: String,
    val freshnessState: FreshnessState,
    val freshnessFraction: Float,
    val freshnessColor: Color,
    val accentSaturated: Color,
    val accentLightTint: Color,
    val accentDarkAlpha: Float,
    val topics: ImmutableList<String>,
    val platforms: ImmutableList<PlatformKind>,
    val isInstalled: Boolean,
    val isUpdateAvailable: Boolean,
    val isFavourite: Boolean,
    val isStarred: Boolean,
    val isSeen: Boolean,
    val isCurrentUserOwner: Boolean,
    val rawRepository: GithubRepoSummaryUi,
)
