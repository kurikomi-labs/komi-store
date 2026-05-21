package zed.rainxch.githubstore.app.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.core.presentation.theme.fraunces
import zed.rainxch.core.presentation.theme.tokens.Radii
import zed.rainxch.core.presentation.vocabulary.CookieShape
import zed.rainxch.core.presentation.vocabulary.VersionStack
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.app_name

/**
 * Desktop sidebar drawer (DESIGN.md §8.1) — 240dp wide, persistent navigation.
 * Cookie brand mark at top, nav items in middle, user card at bottom. Active item:
 * `tintP` background + `primary` foreground.
 *
 * Wired by [AppNavigation] when the platform is non-Android. The Android path
 * keeps the [BottomNavigation] capsule.
 */
@Composable
fun DesktopDrawer(
    currentScreen: GithubStoreGraph?,
    onNavigate: (GithubStoreGraph) -> Unit,
    isUpdateAvailable: Boolean,
    hasUnreadAnnouncements: Boolean,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    // Library (AppsScreen) is Android-only: Installer + PackageMonitor + Shizuku
    // don't exist on Desktop, so the screen has nothing to manage. Filter it out
    // of the drawer entirely rather than show an empty stub.
    val items = BottomNavigationUtils.items().filterNot { it.screen == GithubStoreGraph.AppsScreen }
    Column(
        modifier =
            modifier
                .fillMaxHeight()
                .width(240.dp)
                .background(cs.surface)
                .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.Top,
    ) {
        // Brand: Cookie + name
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(28.dp)
                        .clip(CookieShape)
                        .background(cs.primary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "G",
                    color = Color.White,
                    style =
                        MaterialTheme.typography.titleMedium.copy(
                            fontFamily = fraunces,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                        ),
                )
            }
            Text(
                text = stringResource(Res.string.app_name),
                color = cs.onSurface,
                style =
                    MaterialTheme.typography.titleMedium.copy(
                        fontFamily = fraunces,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        fontWeight = FontWeight.SemiBold,
                    ),
            )
        }
        Spacer(Modifier.size(16.dp))
        items.forEach { item ->
            val isSelected = item.screen::class == currentScreen?.let { it::class }
            val badge: (@Composable () -> Unit)? =
                when {
                    item.screen == GithubStoreGraph.AppsScreen && isUpdateAvailable -> {
                        { VersionStack(count = 1, widthDp = 8) }
                    }

                    item.screen == GithubStoreGraph.ProfileScreen && hasUnreadAnnouncements -> {
                        { UnreadDot(cs.error) }
                    }

                    else -> {
                        null
                    }
                }
            DrawerNavItem(
                label = stringResource(item.titleRes),
                iconImage = if (isSelected) item.iconFilled else item.iconOutlined,
                isSelected = isSelected,
                onClick = { onNavigate(item.screen) },
                trailing = badge,
            )
        }
    }
}

@Composable
private fun DrawerNavItem(
    label: String,
    iconImage: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
) {
    val cs = MaterialTheme.colorScheme
    val bg = if (isSelected) cs.primaryContainer else Color.Transparent
    val fg = if (isSelected) cs.primary else cs.onSurface
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 2.dp)
                .clip(Radii.row)
                .background(bg)
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = iconImage,
            contentDescription = label,
            modifier = Modifier.size(20.dp),
            tint = fg,
        )
        Text(
            text = label,
            color = fg,
            style =
                MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                ),
            modifier = Modifier.weight(1f),
        )
        trailing?.invoke()
    }
}

@Composable
private fun UnreadDot(color: Color) {
    Box(
        modifier =
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color),
    )
}
