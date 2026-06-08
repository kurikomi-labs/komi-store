package zed.rainxch.home.presentation.model

import kotlinx.collections.immutable.toImmutableList
import zed.rainxch.core.domain.model.repository.DiscoveryPlatform
import zed.rainxch.core.domain.model.account.github.GithubRepoSummary
import zed.rainxch.core.presentation.utils.daysSinceIso
import zed.rainxch.core.presentation.utils.formatRelativeShort
import zed.rainxch.core.presentation.utils.toUi
import zed.rainxch.core.presentation.vocabulary.AppAccentResolver
import zed.rainxch.core.presentation.vocabulary.PlatformKind
import zed.rainxch.core.presentation.vocabulary.freshnessOf

fun toHomeRepoCardUi(
    repo: GithubRepoSummary,
    isInstalled: Boolean,
    isUpdateAvailable: Boolean,
    isFavourite: Boolean,
    isStarred: Boolean,
    isSeen: Boolean,
    isCurrentUserOwner: Boolean,
): HomeRepoCardUi {
    val ui = repo.toUi()
    val days = daysSinceIso(repo.updatedAt)?.coerceAtLeast(0) ?: Int.MAX_VALUE
    val freshness = freshnessOf(days)
    val accent = AppAccentResolver.resolve(
        backendHex = null,
        topics = repo.topics.orEmpty(),
        primaryLanguage = repo.language,
    )
    return HomeRepoCardUi(
        id = ui.id,
        name = ui.name,
        ownerLogin = ui.owner.login,
        ownerAvatarUrl = ui.owner.avatarUrl,
        description = ui.description.orEmpty(),
        starsCount = ui.stargazersCount,
        downloadsCount = repo.downloadCount,
        language = repo.language,
        daysSinceUpdate = days,
        relativeAgoLabel = formatRelativeShort(repo.updatedAt),
        freshnessState = freshness.state,
        freshnessFraction = freshness.ringFraction,
        freshnessColor = freshness.color,
        accentSaturated = accent.c,
        accentLightTint = accent.lt,
        accentDarkAlpha = accent.dtAlpha,
        topics = ui.topics.orEmpty().toImmutableList(),
        platforms = ui.availablePlatforms.mapNotNull { platform ->
            when (platform) {
                DiscoveryPlatform.Android -> PlatformKind.ANDROID
                DiscoveryPlatform.Windows -> PlatformKind.WINDOWS
                DiscoveryPlatform.Macos -> PlatformKind.MACOS
                DiscoveryPlatform.Linux -> PlatformKind.LINUX
                else -> null
            }
        }.toImmutableList(),
        isInstalled = isInstalled,
        isUpdateAvailable = isUpdateAvailable,
        isFavourite = isFavourite,
        isStarred = isStarred,
        isSeen = isSeen,
        isCurrentUserOwner = isCurrentUserOwner,
        rawRepository = ui,
    )
}
