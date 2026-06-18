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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.core.domain.model.announcement.Announcement
import zed.rainxch.core.domain.model.announcement.AnnouncementCategory
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.announcements_empty
import zed.rainxch.githubstore.core.presentation.res.announcements_open_mute_settings
import zed.rainxch.githubstore.core.presentation.res.announcements_refresh_failed
import zed.rainxch.githubstore.core.presentation.res.announcements_title
import zed.rainxch.githubstore.core.presentation.res.navigate_back

@OptIn(ExperimentalMaterial3Api::class)
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
    var showMuteSheet by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    DisposableEffect(Unit) {
        onEnteringScreen()
        onDispose { onLeavingScreen() }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.announcements_title),
                        style =
                            MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                            ),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.navigate_back),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showMuteSheet = true }) {
                        Icon(
                            imageVector = Icons.Filled.Tune,
                            contentDescription = stringResource(Res.string.announcements_open_mute_settings),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        PullToRefreshBox(
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
                    Text(
                        text =
                            stringResource(
                                if (refreshFailed) {
                                    Res.string.announcements_refresh_failed
                                } else {
                                    Res.string.announcements_empty
                                },
                            ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                            Text(
                                text = stringResource(Res.string.announcements_refresh_failed),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
