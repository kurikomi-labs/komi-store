package zed.rainxch.profile.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import zed.rainxch.core.presentation.components.chrome.GhsHomeTopBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import zed.rainxch.core.presentation.locals.LocalBottomNavigationHeight
import zed.rainxch.core.presentation.theme.GithubStoreTheme
import zed.rainxch.core.presentation.utils.ObserveAsEvents
import zed.rainxch.core.presentation.utils.arrowKeyScroll
import zed.rainxch.core.presentation.utils.constrainedContentWidth
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.downloads_cleared
import zed.rainxch.githubstore.core.presentation.res.logout_success
import zed.rainxch.githubstore.core.presentation.res.profile_title
import zed.rainxch.githubstore.core.presentation.res.proxy_saved
import zed.rainxch.githubstore.core.presentation.res.seen_history_cleared
import zed.rainxch.profile.presentation.components.LogoutDialog
import zed.rainxch.profile.presentation.components.profileSections

@Composable
fun ProfileRoot(
    onNavigateBack: () -> Unit,
    onNavigateToDevProfile: (username: String) -> Unit,
    onNavigateToAuthentication: () -> Unit,
    onNavigateToStarredRepos: () -> Unit,
    onNavigateToFavouriteRepos: () -> Unit,
    onNavigateToRecentlyViewed: () -> Unit,
    onNavigateToWhatsNew: () -> Unit,
    onPreviewWhatsNewSheet: () -> Unit,
    onNavigateToAnnouncements: () -> Unit,
    onPreviewAnnouncements: () -> Unit,
    onNavigateToTweaks: () -> Unit,
    onNavigateToAbout: () -> Unit,
    hasUnreadAnnouncements: Boolean,
    viewModel: ProfileViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            ProfileEvent.OnLogoutSuccessful -> {
                coroutineScope.launch {
                    snackbarState.showSnackbar(getString(Res.string.logout_success))

                    onNavigateBack()
                }
            }

            is ProfileEvent.OnLogoutError -> {
                coroutineScope.launch {
                    snackbarState.showSnackbar(event.message)
                }
            }

            ProfileEvent.OnProxySaved -> {
                coroutineScope.launch {
                    snackbarState.showSnackbar(getString(Res.string.proxy_saved))
                }
            }

            is ProfileEvent.OnProxySaveError -> {
                coroutineScope.launch {
                    snackbarState.showSnackbar(event.message)
                }
            }

            ProfileEvent.OnCacheCleared -> {
                coroutineScope.launch {
                    snackbarState.showSnackbar(getString(Res.string.downloads_cleared))
                }
            }

            is ProfileEvent.OnCacheClearError -> {
                coroutineScope.launch {
                    snackbarState.showSnackbar(event.message)
                }
            }

            ProfileEvent.OnSeenHistoryCleared -> {
                coroutineScope.launch {
                    snackbarState.showSnackbar(getString(Res.string.seen_history_cleared))
                }
            }
        }
    }

    ProfileScreen(
        state = state,
        hasUnreadAnnouncements = hasUnreadAnnouncements,
        onAction = { action ->
            when (action) {
                ProfileAction.OnLoginClick -> {
                    onNavigateToAuthentication()
                }

                ProfileAction.OnFavouriteReposClick -> {
                    onNavigateToFavouriteRepos()
                }

                ProfileAction.OnStarredReposClick -> {
                    onNavigateToStarredRepos()
                }

                is ProfileAction.OnRepositoriesClick -> {
                    onNavigateToDevProfile(action.username)
                }

                ProfileAction.OnRecentlyViewedClick -> {
                    onNavigateToRecentlyViewed()
                }

                ProfileAction.OnWhatsNewClick -> {
                    onNavigateToWhatsNew()
                }

                ProfileAction.OnWhatsNewLongClick -> {
                    onPreviewWhatsNewSheet()
                }

                ProfileAction.OnAnnouncementsClick -> {
                    onNavigateToAnnouncements()
                }

                ProfileAction.OnAnnouncementsLongClick -> {
                    onPreviewAnnouncements()
                }

                ProfileAction.OnTweaksClick -> {
                    onNavigateToTweaks()
                }

                ProfileAction.OnAboutClick -> {
                    onNavigateToAbout()
                }

                else -> {
                    viewModel.onAction(action)
                }
            }
        },
        snackbarState = snackbarState,
    )

    if (state.isLogoutDialogVisible) {
        LogoutDialog(
            onDismissRequest = {
                viewModel.onAction(ProfileAction.OnLogoutDismiss)
            },
            onLogout = {
                viewModel.onAction(ProfileAction.OnLogoutConfirmClick)
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ProfileScreen(
    state: ProfileState,
    onAction: (ProfileAction) -> Unit,
    snackbarState: SnackbarHostState,
    hasUnreadAnnouncements: Boolean = false,
) {
    val bottomNavHeight = LocalBottomNavigationHeight.current
    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarState,
                modifier = Modifier.padding(bottom = bottomNavHeight + 16.dp),
            )
        },
        topBar = {
            GhsHomeTopBar(title = stringResource(Res.string.profile_title))
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        val listState = rememberLazyListState()
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                state = listState,
                modifier =
                    Modifier
                        .constrainedContentWidth()
                        .fillMaxHeight()
                        .padding(16.dp)
                        .arrowKeyScroll(listState, autoFocus = true),
            ) {
                profileSections(
                    state = state,
                    hasUnreadAnnouncements = hasUnreadAnnouncements,
                    onAction = onAction,
                )

                item {
                    Spacer(Modifier.height(bottomNavHeight + 32.dp))
                }
            }
        }
    }
}


@Preview
@Composable
private fun Preview() {
    GithubStoreTheme {
        ProfileScreen(
            state = ProfileState(),
            onAction = {},
            snackbarState = SnackbarHostState(),
        )
    }
}
