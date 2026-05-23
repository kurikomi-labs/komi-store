package zed.rainxch.home.presentation.model

import kotlinx.collections.immutable.toImmutableList
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import zed.rainxch.core.domain.model.DiscoveryPlatform
import zed.rainxch.core.domain.model.GithubRepoSummary
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
    val days = daysSinceIso(repo.updatedAt)
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
        relativeAgoLabel = relativeAgo(repo.updatedAt),
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

@OptIn(ExperimentalTime::class)
private fun daysSinceIso(isoInstant: String): Int {
    val trimmed = isoInstant.trim()
    if (trimmed.isEmpty()) return Int.MAX_VALUE
    val parsed = runCatching { Instant.parse(trimmed) }.getOrNull() ?: return Int.MAX_VALUE
    val diffMs = Clock.System.now().toEpochMilliseconds() - parsed.toEpochMilliseconds()
    if (diffMs <= 0L) return 0
    return (diffMs / 86_400_000L).toInt()
}

@OptIn(ExperimentalTime::class)
private fun relativeAgo(isoInstant: String): String {
    val trimmed = isoInstant.trim()
    if (trimmed.isEmpty()) return ""
    val parsed = runCatching { Instant.parse(trimmed) }.getOrNull() ?: return ""
    val diffMs = Clock.System.now().toEpochMilliseconds() - parsed.toEpochMilliseconds()
    if (diffMs <= 0L) return "now"
    val minutes = diffMs / 60_000L
    if (minutes < 1L) return "now"
    if (minutes < 60L) return "${minutes}m"
    val hours = minutes / 60L
    if (hours < 24L) return "${hours}h"
    val days = hours / 24L
    if (days < 30L) return "${days}d"
    val months = days / 30L
    if (months < 12L) return "${months}mo"
    val years = days / 365L
    return "${years}y"
}
