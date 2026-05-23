package zed.rainxch.home.presentation.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.core.presentation.components.GitHubStoreImage
import zed.rainxch.core.presentation.theme.shapes.CornerRadii
import zed.rainxch.core.presentation.theme.shapes.WonkySquircleShape
import zed.rainxch.core.presentation.theme.tokens.EmberPalette
import zed.rainxch.core.presentation.utils.formatCount
import zed.rainxch.core.presentation.vocabulary.PlatformGlyph
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.home_action_get
import zed.rainxch.githubstore.core.presentation.res.open
import zed.rainxch.githubstore.core.presentation.res.update
import zed.rainxch.home.presentation.model.HomeRepoCardUi

private val LeadShape = WonkySquircleShape(
    topStart = CornerRadii(32.dp, 26.dp),
    topEnd = CornerRadii(26.dp, 32.dp),
    bottomEnd = CornerRadii(32.dp, 26.dp),
    bottomStart = CornerRadii(26.dp, 32.dp),
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LeadCard(
    card: HomeRepoCardUi,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = isSystemInDarkTheme()
    val surface = MaterialTheme.colorScheme.surface
    val borderColor = if (isDark) EmberPalette.Hot.copy(alpha = 0.42f) else EmberPalette.Deep.copy(alpha = 0.5f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(248.dp)
            .clip(LeadShape)
            .background(if (isDark) EmberPalette.Ash else surface)
            .drawBehind {
                val warmth = Brush.linearGradient(
                    colorStops = arrayOf(
                        0f to EmberPalette.Hot.copy(alpha = if (isDark) 0.32f else 0.16f),
                        0.6f to EmberPalette.Warm.copy(alpha = if (isDark) 0.18f else 0.08f),
                        1f to Color.Transparent,
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(size.width * 0.85f, size.height),
                )
                drawRect(brush = warmth)
                val sun = Brush.radialGradient(
                    colors = listOf(
                        EmberPalette.Amber.copy(alpha = if (isDark) 0.18f else 0.12f),
                        Color.Transparent,
                    ),
                    center = Offset(size.width * 0.18f, size.height * 0.25f),
                    radius = size.minDimension * 0.7f,
                )
                drawRect(brush = sun)
            }
            .border(width = 1.5.dp, color = borderColor, shape = LeadShape)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 18.dp, vertical = 18.dp),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(if (isDark) EmberPalette.Ash else surface)
                        .border(2.5.dp, EmberPalette.Deep, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    GitHubStoreImage(
                        imageModel = { card.ownerAvatarUrl },
                        modifier = Modifier.size(68.dp).clip(CircleShape),
                        extractDominantFor = card.ownerAvatarUrl,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = card.ownerLogin,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 12.sp,
                            ),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.size(6.dp))
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(EmberPalette.Deep)
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = "HOT",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 9.5.sp,
                                    letterSpacing = 0.8.sp,
                                ),
                            )
                            Text(
                                text = "·",
                                color = Color.White.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            )
                            Text(
                                text = card.relativeAgoLabel,
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                ),
                            )
                        }
                    }
                    Text(
                        text = card.name,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 28.sp,
                            letterSpacing = (-0.3).sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            if (card.description.isNotBlank()) {
                Text(
                    text = card.description,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(10.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f),
                        modifier = Modifier.size(15.dp),
                    )
                    Text(
                        text = formatCount(card.starsCount),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f),
                    )
                }
                if (card.downloadsCount > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f),
                            modifier = Modifier.size(15.dp),
                        )
                        Text(
                            text = formatCount(card.downloadsCount),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                            ),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f),
                        )
                    }
                }
                Box(modifier = Modifier.weight(1f))
                if (card.platforms.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        card.platforms.forEach { kind ->
                            PlatformGlyph(kind = kind, supported = true, sizeDp = 15)
                        }
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .background(EmberPalette.Deep)
                    .padding(vertical = 13.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(
                        when {
                            card.isUpdateAvailable -> Res.string.update
                            card.isInstalled -> Res.string.open
                            else -> Res.string.home_action_get
                        },
                    ),
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                    ),
                )
            }
        }
    }
}
