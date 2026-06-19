package zed.rainxch.githubstore.app.navigation

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.core.presentation.components.badge.KomiBadge
import zed.rainxch.core.presentation.locals.LocalPersonality
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.app_icon
import zed.rainxch.githubstore.core.presentation.res.app_name

@Composable
fun DesktopDrawer(
    currentScreen: GithubStoreGraph?,
    onNavigate: (GithubStoreGraph) -> Unit,
    unreadAnnouncementsCount: Int,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme

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
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(Res.drawable.app_icon),
                contentDescription = null,
                modifier =
                    Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(LocalPersonality.current.shape.corner)),
                contentScale = ContentScale.Crop,
            )
            Text(
                text = stringResource(Res.string.app_name),
                color = cs.onSurface,
                style =
                    MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
            )
        }
        Spacer(Modifier.size(16.dp))
        items.forEach { item ->
            val isSelected = item.screen::class == currentScreen?.let { it::class }
            val badge: (@Composable () -> Unit)? =
                when {
                    item.screen == GithubStoreGraph.ProfileGraph.ProfileScreen -> {
                        {
                            KomiBadge(
                                count =
                                    if (unreadAnnouncementsCount > 0) {
                                        unreadAnnouncementsCount
                                    } else {
                                        null
                                    },
                                dot = unreadAnnouncementsCount > 0,
                            )
                        }
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
                .clip(RoundedCornerShape(LocalPersonality.current.shape.corner))
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
