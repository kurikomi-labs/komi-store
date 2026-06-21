package zed.rainxch.details.presentation.components.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import zed.rainxch.core.presentation.components.GitHubStoreImage
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.core.domain.model.account.github.GithubUserProfile
import zed.rainxch.core.presentation.components.icon.KomiIcon
import zed.rainxch.core.presentation.components.text.KomiText
import zed.rainxch.core.presentation.components.text.KomiTextRole
import zed.rainxch.core.presentation.locals.LocalPersonality
import zed.rainxch.details.presentation.DetailsAction
import zed.rainxch.githubstore.core.presentation.res.*

fun LazyListScope.author(
    author: GithubUserProfile?,
    onAction: (DetailsAction) -> Unit,
) {
    item {
        val colors = LocalPersonality.current.colors
        val shape = LocalPersonality.current.shape
        val developerCardShape = RoundedCornerShape(shape.corner)
        Spacer(Modifier.height(20.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            KomiText(
                text = stringResource(Res.string.details_developer_section),
                role = KomiTextRole.Title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp,
                color = colors.onBackground,
                uppercase = false,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(developerCardShape)
                .border(
                    width = 1.dp,
                    color = colors.outline,
                    shape = developerCardShape,
                )
                .background(colors.surface)
                .clickable(enabled = author?.login != null) {
                    author?.login?.let { login ->
                        onAction(DetailsAction.OpenDeveloperProfile(login))
                    }
                }
                .padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GitHubStoreImage(
                imageModel = { author?.avatarUrl },
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(shape.cornerSmall))
                    .border(2.dp, colors.primary, RoundedCornerShape(shape.cornerSmall)),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                val displayName = author?.name?.takeIf { it.isNotBlank() } ?: author?.login
                val handle = author?.login?.takeIf { it != author.name }
                displayName?.let {
                    androidx.compose.foundation.text.BasicText(
                        text = it,
                        style = LocalPersonality.current.type.title.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp,
                            letterSpacing = (-0.3).sp,
                            color = colors.onSurface,
                        ),
                        maxLines = 1,
                        softWrap = false,
                        autoSize = androidx.compose.foundation.text.TextAutoSize.StepBased(
                            minFontSize = 15.sp,
                            maxFontSize = 22.sp,
                            stepSize = 1.sp,
                        ),
                    )
                }
                handle?.let { login ->
                    KomiText(
                        text = "@$login",
                        role = KomiTextRole.Label,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        uppercase = false,
                    )
                }
                author?.bio?.let { bio ->
                    KomiText(
                        text = bio,
                        role = KomiTextRole.Body,
                        fontSize = 13.sp,
                        color = colors.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (!author?.htmlUrl.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Row(
                        modifier = Modifier.clickable {
                            onAction(DetailsAction.OpenAuthorInBrowser)
                        },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        KomiIcon(
                            painter = painterResource(Res.drawable.ic_github),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = colors.primary,
                        )
                        KomiText(
                            text = stringResource(Res.string.profile),
                            role = KomiTextRole.Label,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.primary,
                            uppercase = false,
                        )
                    }
                }
            }
            if (author?.login != null) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(shape.cornerSmall))
                        .background(colors.onSurface)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    KomiText(
                        text = stringResource(Res.string.details_view_developer_profile),
                        role = KomiTextRole.Label,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.surface,
                        uppercase = false,
                    )
                    KomiIcon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = colors.surface,
                    )
                }
            }
        }
    }
}
