package zed.rainxch.githubstore.app.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.core.presentation.theme.GithubStoreTheme
import zed.rainxch.core.presentation.theme.geist
import zed.rainxch.core.presentation.vocabulary.CookieShape
import zed.rainxch.core.presentation.vocabulary.VersionStack

@Composable
fun BottomNavigation(
    currentScreen: GithubStoreGraph,
    onNavigate: (GithubStoreGraph) -> Unit,
    isUpdateAvailable: Boolean,
    hasUnreadAnnouncements: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val allowedScreens = BottomNavigationUtils.allowedScreens()
    if (allowedScreens.none { it.screen::class == currentScreen::class }) return

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Row(
            modifier =
                Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = CircleShape,
                    ).padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            allowedScreens.forEach { item ->
                CookieTabItem(
                    item = item,
                    isSelected = item.screen::class == currentScreen::class,
                    onSelect = { onNavigate(item.screen) },
                    showUpdateBadge = item.screen == GithubStoreGraph.AppsScreen && isUpdateAvailable,
                    hasUnreadDot = item.screen == GithubStoreGraph.ProfileScreen && hasUnreadAnnouncements,
                )
            }
        }
    }
}

@Composable
private fun CookieTabItem(
    item: BottomNavigationItem,
    isSelected: Boolean,
    onSelect: () -> Unit,
    showUpdateBadge: Boolean = false,
    hasUnreadDot: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec =
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium,
            ),
        label = "tab-press-scale",
    )
    val activeAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "tab-active-alpha",
    )
    val cs = MaterialTheme.colorScheme
    val cookieFill = cs.primary
    val activeFg = cs.onPrimary
    val inactiveFg = cs.onSurface.copy(alpha = 0.7f)

    Box(
        modifier =
            Modifier
                .clip(CircleShape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onSelect,
                ).scale(pressScale)
                .padding(horizontal = if (isSelected) 14.dp else 10.dp, vertical = 6.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Box(
                modifier = Modifier.size(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (activeAlpha > 0f) {
                    Box(
                        modifier =
                            Modifier
                                .size(32.dp)
                                .graphicsLayer { alpha = activeAlpha }
                                .clip(CookieShape)
                                .background(cookieFill),
                    )
                }
                Icon(
                    imageVector = if (isSelected) item.iconFilled else item.iconOutlined,
                    contentDescription = stringResource(item.titleRes),
                    modifier = Modifier.size(20.dp),
                    tint = if (isSelected) activeFg else inactiveFg,
                )
            }

            AnimatedVisibility(
                visible = isSelected,
                enter = fadeIn() + scaleIn(initialScale = 0.6f),
                exit = fadeOut() + scaleOut(targetScale = 0.6f),
            ) {
                Text(
                    text = stringResource(item.titleRes),
                    color = cs.primary,
                    style =
                        MaterialTheme.typography.labelSmall.copy(
                            fontFamily = geist,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                        ),
                    maxLines = 1,
                )
            }
        }

        if (showUpdateBadge) {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 2.dp),
            ) {
                VersionStack(count = 1, widthDp = 8)
            }
        }
        if (hasUnreadDot) {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 4.dp, end = 2.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(cs.error),
            )
        }
    }
}

@Preview
@Composable
fun BottomNavigationPreview() {
    GithubStoreTheme {
        BottomNavigation(
            currentScreen = GithubStoreGraph.HomeScreen,
            onNavigate = {},
            isUpdateAvailable = true,
        )
    }
}
