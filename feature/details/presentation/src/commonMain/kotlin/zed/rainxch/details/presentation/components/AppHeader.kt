package zed.rainxch.details.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CallSplit
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.core.domain.model.repository.DiscoveryPlatform
import zed.rainxch.core.domain.model.account.github.GithubRelease
import zed.rainxch.core.domain.model.account.github.GithubRepoSummary
import zed.rainxch.core.domain.model.account.github.GithubUserProfile
import zed.rainxch.core.domain.model.installation.InstalledApp
import zed.rainxch.core.presentation.components.GitHubStoreImage
import zed.rainxch.core.presentation.components.chips.KomiChip
import zed.rainxch.core.presentation.components.chips.KomiChipKind
import zed.rainxch.core.presentation.components.chips.KomiChipSize
import zed.rainxch.core.presentation.components.icon.KomiIcon
import zed.rainxch.core.presentation.components.progress.KomiCircularProgress
import zed.rainxch.core.presentation.components.text.KomiText
import zed.rainxch.core.presentation.components.text.KomiTextRole
import zed.rainxch.core.presentation.locals.LocalPersonality
import zed.rainxch.core.presentation.utils.formatCount
import zed.rainxch.core.presentation.utils.toIcons
import zed.rainxch.core.presentation.utils.toLabel
import zed.rainxch.details.domain.model.RepoStats
import zed.rainxch.details.presentation.model.DownloadStage
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.forked_repository
import zed.rainxch.githubstore.core.presentation.res.installed
import zed.rainxch.githubstore.core.presentation.res.no_description
import zed.rainxch.githubstore.core.presentation.res.pending_install
import zed.rainxch.githubstore.core.presentation.res.self_owned_badge
import zed.rainxch.githubstore.core.presentation.res.update_available

private fun Color.normalizedForStripe(isDark: Boolean): Color {
    val r = (red * 255f).toInt().coerceIn(0, 255)
    val g = (green * 255f).toInt().coerceIn(0, 255)
    val b = (blue * 255f).toInt().coerceIn(0, 255)
    val lum = (r + g + b) / 3
    val target = if (isDark) 170 else 140
    val minLum = if (isDark) 90 else 70
    if (lum >= minLum) return this
    val factor = target.toFloat() / lum.coerceAtLeast(1).toFloat()
    return Color(
        red = (r * factor).toInt().coerceIn(0, 255),
        green = (g * factor).toInt().coerceIn(0, 255),
        blue = (b * factor).toInt().coerceIn(0, 255),
        alpha = 255,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppHeader(
    author: GithubUserProfile?,
    repository: GithubRepoSummary,
    release: GithubRelease?,
    installedApp: InstalledApp?,
    stats: RepoStats?,
    modifier: Modifier = Modifier,
    downloadStage: DownloadStage = DownloadStage.IDLE,
    downloadProgress: Int? = null,
    isCurrentUserOwner: Boolean = false,
    onPlatformClick: ((DiscoveryPlatform) -> Unit)? = null,
    onOwnerClick: () -> Unit = {},
) {
    val isDark = isSystemInDarkTheme()
    val colors = LocalPersonality.current.colors
    val surface = colors.surface
    val avatarUrl = author?.avatarUrl ?: repository.owner.avatarUrl
    val rawAccent = colors.primary
    val shape = LocalPersonality.current.shape
    val headerShape = RoundedCornerShape(shape.corner)
    val normalizedAccent = remember(rawAccent, isDark) { rawAccent.normalizedForStripe(isDark) }
    val tintFraction = if (isDark) 0.10f else 0.06f
    val animatedAccent by animateColorAsState(
        targetValue = normalizedAccent,
        animationSpec = tween(durationMillis = 1800, easing = LinearOutSlowInEasing),
        label = "details-hero-accent",
    )
    val animatedSurface by animateColorAsState(
        targetValue = lerp(surface, normalizedAccent, tintFraction),
        animationSpec = tween(durationMillis = 1800, easing = LinearOutSlowInEasing),
        label = "details-hero-surface",
    )
    val stripeBase = if (isDark) animatedAccent.copy(alpha = 0.18f) else animatedAccent.copy(alpha = 0.12f)
    val stripeLineThick = if (isDark) animatedAccent.copy(alpha = 0.45f) else animatedAccent.copy(alpha = 0.55f)
    val stripeLineThin = if (isDark) animatedAccent.copy(alpha = 0.22f) else animatedAccent.copy(alpha = 0.30f)
    val avatarBg = if (isDark) animatedAccent.copy(alpha = 0.20f) else animatedAccent.copy(alpha = 0.14f)
    val borderColor = colors.outline

    val animatedProgress by animateFloatAsState(
        targetValue = (downloadProgress ?: 0) / 100f,
        animationSpec = tween(durationMillis = 500),
        label = "avatar-progress",
    )

    val supportedPlatforms = remember(release?.assets) {
        val names = release?.assets?.map { it.name.lowercase() }.orEmpty()
        buildList {
            if (names.any { it.endsWith(".apk") }) add(DiscoveryPlatform.Android)
            if (names.any { it.endsWith(".exe") || it.endsWith(".msi") }) add(DiscoveryPlatform.Windows)
            if (names.any { it.endsWith(".dmg") || it.endsWith(".pkg") }) add(DiscoveryPlatform.Macos)
            if (names.any {
                    it.endsWith(".appimage") ||
                        it.endsWith(".deb") ||
                        it.endsWith(".rpm") ||
                        it.endsWith(".pkg.tar.zst")
                }
            ) add(DiscoveryPlatform.Linux)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(headerShape)
            .background(animatedSurface)
            .border(1.5.dp, borderColor, headerShape),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(76.dp)
                    .clipToBounds()
                    .drawBehind {
                        drawRect(color = stripeBase)
                        val thick = 9.dp.toPx()
                        val thin = 2.5.dp.toPx()
                        val gapAfterThick = 10.dp.toPx()
                        val gapBetweenThin = 6.dp.toPx()
                        val cycle = thick + gapAfterThick + thin + gapBetweenThin + thin + gapAfterThick
                        var x = -size.height
                        while (x < size.width + size.height) {
                            drawLine(
                                color = stripeLineThick,
                                start = Offset(x, size.height),
                                end = Offset(x + size.height, 0f),
                                strokeWidth = thick,
                                cap = StrokeCap.Round,
                            )
                            var xt = x + thick + gapAfterThick
                            drawLine(
                                color = stripeLineThin,
                                start = Offset(xt, size.height),
                                end = Offset(xt + size.height, 0f),
                                strokeWidth = thin,
                                cap = StrokeCap.Round,
                            )
                            xt += thin + gapBetweenThin
                            drawLine(
                                color = stripeLineThin,
                                start = Offset(xt, size.height),
                                end = Offset(xt + size.height, 0f),
                                strokeWidth = thin,
                                cap = StrokeCap.Round,
                            )
                            x += cycle
                        }
                    },
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 130.dp, end = 20.dp, top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                KomiText(
                    text = author?.login ?: repository.owner.login,
                    role = KomiTextRole.Label,
                    fontSize = 13.sp,
                    color = colors.onSurface.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .clickable(onClick = onOwnerClick),
                    uppercase = false,
                )
                if (isCurrentUserOwner) {
                    KomiIcon(
                        imageVector = Icons.Filled.Verified,
                        contentDescription = stringResource(Res.string.self_owned_badge),
                        modifier = Modifier.size(16.dp),
                        tint = colors.primary,
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 130.dp, end = 20.dp, top = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                androidx.compose.foundation.text.BasicText(
                    text = repository.name,
                    style = LocalPersonality.current.type.display.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 30.sp,
                        letterSpacing = (-0.4).sp,
                        color = colors.onSurface,
                    ),
                    maxLines = 1,
                    softWrap = false,
                    autoSize = androidx.compose.foundation.text.TextAutoSize.StepBased(
                        minFontSize = 18.sp,
                        maxFontSize = 30.sp,
                        stepSize = 1.sp,
                    ),
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (repository.isFork) {
                    KomiChip(
                        label = stringResource(Res.string.forked_repository),
                        kind = KomiChipKind.Info,
                        size = KomiChipSize.Sm,
                        leadingIcon = Icons.AutoMirrored.Outlined.CallSplit,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                val statColor = colors.onSurface.copy(alpha = 0.88f)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    KomiIcon(
                        imageVector = Icons.Outlined.Star,
                        contentDescription = null,
                        tint = statColor,
                        modifier = Modifier.size(15.dp),
                    )
                    KomiText(
                        text = formatCount(stats?.stars ?: repository.stargazersCount),
                        role = KomiTextRole.Label,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = statColor,
                        uppercase = false,
                    )
                }
                val forksValue = stats?.forks ?: repository.forksCount
                if (forksValue > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        KomiIcon(
                            imageVector = Icons.Outlined.AccountTree,
                            contentDescription = null,
                            tint = statColor,
                            modifier = Modifier.size(15.dp),
                        )
                        KomiText(
                            text = formatCount(forksValue),
                            role = KomiTextRole.Label,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = statColor,
                            uppercase = false,
                        )
                    }
                }
                val downloadsValue = stats?.totalDownloads ?: repository.downloadCount
                if (downloadsValue > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        KomiIcon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = statColor,
                            modifier = Modifier.size(15.dp),
                        )
                        KomiText(
                            text = formatCount(downloadsValue),
                            role = KomiTextRole.Label,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = statColor,
                            uppercase = false,
                        )
                    }
                }
                val licenseValue = stats?.license
                if (!licenseValue.isNullOrBlank()) {
                    KomiText(
                        text = licenseValue,
                        role = KomiTextRole.Label,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = statColor,
                        uppercase = false,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            KomiText(
                text = repository.description ?: stringResource(Res.string.no_description),
                role = KomiTextRole.Body,
                fontSize = 14.sp,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            if (installedApp != null) {
                Spacer(Modifier.height(12.dp))
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    val statusColor = when {
                        installedApp.isPendingInstall -> colors.primary
                        installedApp.isUpdateAvailable -> colors.primary
                        else -> colors.primary
                    }
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(shape.cornerSmall))
                            .border(
                                width = 1.dp,
                                color = statusColor,
                                shape = RoundedCornerShape(shape.cornerSmall),
                            )
                            .padding(horizontal = 12.dp, vertical = 5.dp),
                    ) {
                        KomiText(
                            text = stringResource(
                                when {
                                    installedApp.isPendingInstall -> Res.string.pending_install
                                    installedApp.isUpdateAvailable -> Res.string.update_available
                                    else -> Res.string.installed
                                },
                            ),
                            role = KomiTextRole.Label,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = statusColor,
                            uppercase = false,
                        )
                    }
                }
            }
            if (supportedPlatforms.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(horizontal = 20.dp),
                ) {
                    supportedPlatforms.forEach { platform ->
                        KomiChip(
                            label = platform.toLabel(),
                            kind = KomiChipKind.Info,
                            size = KomiChipSize.Sm,
                            leadingContent = {
                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    platform.toIcons().forEach { icon ->
                                        KomiIcon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = colors.onSurface,
                                        )
                                    }
                                }
                            },
                            onClick = onPlatformClick?.let { handler -> { handler(platform) } },
                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 20.dp, y = 30.dp)
                .size(100.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(shape.cornerSmall))
                    .background(avatarBg)
                    .border(2.5.dp, animatedAccent, RoundedCornerShape(shape.cornerSmall)),
                contentAlignment = Alignment.Center,
            ) {
                GitHubStoreImage(
                    imageModel = { avatarUrl },
                    modifier = Modifier.size(92.dp).clip(RoundedCornerShape(shape.cornerSmall)),
                )
            }
            if (downloadStage != DownloadStage.IDLE) {
                when (downloadStage) {
                    DownloadStage.DOWNLOADING -> {
                        KomiCircularProgress(
                            progress = { 1f },
                            modifier = Modifier.fillMaxSize(),
                            color = animatedAccent.copy(alpha = 0.2f),
                        )
                        KomiCircularProgress(
                            progress = { animatedProgress },
                            modifier = Modifier.fillMaxSize(),
                            color = animatedAccent,
                        )
                    }
                    DownloadStage.VERIFYING, DownloadStage.INSTALLING -> {
                        KomiCircularProgress(
                            modifier = Modifier.fillMaxSize(),
                            color = animatedAccent,
                        )
                    }
                }
            }
        }
    }
}
