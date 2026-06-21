package zed.rainxch.profile.presentation.announcements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.core.presentation.components.bars.KomiTopBar
import zed.rainxch.core.presentation.components.bars.KomiTopBarSize
import zed.rainxch.core.presentation.components.buttons.KomiButtonVariant
import zed.rainxch.core.presentation.components.buttons.KomiIconButton
import zed.rainxch.core.presentation.components.refresh.KomiPullToRefresh
import zed.rainxch.core.presentation.components.scaffold.KomiScaffold
import zed.rainxch.core.presentation.components.text.KomiText
import zed.rainxch.core.presentation.components.text.KomiTextRole
import zed.rainxch.core.presentation.locals.LocalPersonality
import zed.rainxch.core.domain.model.announcement.Announcement
import zed.rainxch.core.domain.model.announcement.AnnouncementCategory
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.announcements_empty
import zed.rainxch.githubstore.core.presentation.res.announcements_open_mute_settings
import zed.rainxch.githubstore.core.presentation.res.announcements_refresh_failed
import zed.rainxch.githubstore.core.presentation.res.announcements_title
import zed.rainxch.githubstore.core.presentation.res.navigate_back

@Composable
fun AnnouncementsRoot(
    items: List<Announcement>,
    acknowledgedIds: Set<String>,
    mutedCategories: Set<AnnouncementCategory>,
    refreshFailed: Boolean,
    onNavigateBack: () -> Unit,
    onRefresh: suspend () -> Unit,
    onCtaClick: (Announcement) -> Unit,
    onDismissClick: (Announcement) -> Unit,
    onAcknowledgeClick: (Announcement) -> Unit,
    onToggleMute: (AnnouncementCategory, Boolean) -> Unit,
    onLeavingScreen: () -> Unit = {},
    onEnteringScreen: () -> Unit = {},
) {
    val colors = LocalPersonality.current.colors
    var showMuteSheet by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    DisposableEffect(Unit) {
        onEnteringScreen()
        onDispose { onLeavingScreen() }
    }

    KomiScaffold(
        topBar = {
            KomiTopBar(
                title = stringResource(Res.string.announcements_title),
                size = KomiTopBarSize.Compact,
                leading = {
                    KomiIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.navigate_back),
                        onClick = onNavigateBack,
                        variant = KomiButtonVariant.Text,
                    )
                },
                actions = {
                    KomiIconButton(
                        icon = Icons.Filled.Tune,
                        contentDescription = stringResource(Res.string.announcements_open_mute_settings),
                        onClick = { showMuteSheet = true },
                        variant = KomiButtonVariant.Text,
                    )
                },
            )
        },
    ) { innerPadding ->
        KomiPullToRefresh(
            isRefreshing = isRefreshing,
            onRefresh = {
                coroutineScope.launch {
                    isRefreshing = true
                    try {
                        onRefresh()
                    } finally {
                        isRefreshing = false
                    }
                }
            },
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            if (items.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    KomiText(
                        text =
                            stringResource(
                                if (refreshFailed) {
                                    Res.string.announcements_refresh_failed
                                } else {
                                    Res.string.announcements_empty
                                },
                            ),
                        role = KomiTextRole.Body,
                        color = colors.onSurfaceVariant,
                        uppercase = false,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (refreshFailed) {
                        item {
                            KomiText(
                                text = stringResource(Res.string.announcements_refresh_failed),
                                role = KomiTextRole.Label,
                                fontSize = 11.sp,
                                color = colors.onSurfaceVariant,
                                uppercase = false,
                            )
                        }
                    }
                    items(items, key = { it.id }) { item ->
                        AnnouncementCard(
                            announcement = item,
                            isAcknowledged = item.id in acknowledgedIds,
                            onCtaClick = { onCtaClick(item) },
                            onDismissClick = { onDismissClick(item) },
                            onAcknowledgeClick = { onAcknowledgeClick(item) },
                        )
                    }
                }
            }
        }
    }

    if (showMuteSheet) {
        MuteSettingsBottomSheet(
            mutedCategories = mutedCategories,
            onToggle = onToggleMute,
            onDismiss = { showMuteSheet = false },
        )
    }
}
