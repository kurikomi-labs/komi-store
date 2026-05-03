package zed.rainxch.githubstore

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel
import zed.rainxch.core.presentation.components.announcements.CriticalAnnouncementModal
import zed.rainxch.core.presentation.components.whatsnew.WhatsNewSheet
import zed.rainxch.core.presentation.theme.GithubStoreTheme
import zed.rainxch.core.presentation.utils.ApplyAndroidSystemBars
import zed.rainxch.core.presentation.utils.ObserveAsEvents
import zed.rainxch.githubstore.app.announcements.AnnouncementsViewModel
import zed.rainxch.githubstore.app.components.RateLimitDialog
import zed.rainxch.githubstore.app.components.SessionExpiredDialog
import zed.rainxch.githubstore.app.deeplink.DeepLinkDestination
import zed.rainxch.githubstore.app.deeplink.DeepLinkParser
import zed.rainxch.githubstore.app.desktop.KeyboardNavigation
import zed.rainxch.githubstore.app.desktop.KeyboardNavigationEvent
import zed.rainxch.githubstore.app.navigation.AppNavigation
import zed.rainxch.githubstore.app.navigation.GithubStoreGraph
import zed.rainxch.githubstore.app.navigation.getCurrentScreen
import zed.rainxch.githubstore.app.whatsnew.WhatsNewViewModel

@Composable
fun App(deepLinkUri: String? = null) {
    val viewModel: MainViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val currentScreen = navController.currentBackStackEntryAsState().value.getCurrentScreen()

    LaunchedEffect(deepLinkUri) {
        deepLinkUri?.let { uri ->
            when (val destination = DeepLinkParser.parse(uri)) {
                is DeepLinkDestination.Repository -> {
                    navController.navigate(
                        GithubStoreGraph.DetailsScreen(
                            owner = destination.owner,
                            repo = destination.repo,
                        ),
                    )
                }

                DeepLinkDestination.Apps -> {
                    // Pending-install notification dropped us here.
                    // Navigate to the apps tab so the user can finish
                    // the deferred install from the row.
                    navController.navigate(GithubStoreGraph.AppsScreen) {
                        popUpTo(GithubStoreGraph.HomeScreen) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }

                DeepLinkDestination.None -> {
                    // ignore unrecognized deep links
                }
            }
        }
    }

    ObserveAsEvents(KeyboardNavigation.events) { event ->
        when (event) {
            KeyboardNavigationEvent.OnCtrlFClick -> {
                if (currentScreen !is GithubStoreGraph.SearchScreen) {
                    navController.navigate(GithubStoreGraph.SearchScreen) {
                        popUpTo(GithubStoreGraph.HomeScreen) {
                            saveState = true
                        }

                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        }
    }

    GithubStoreTheme(
        fontTheme = state.currentFontTheme,
        appTheme = state.currentColorTheme,
        isAmoledTheme = state.isAmoledTheme,
        isDarkTheme = state.isDarkTheme ?: isSystemInDarkTheme(),
    ) {
        ApplyAndroidSystemBars(state.isDarkTheme)

        // Suppress the rate-limit dialog while the user is on the auth
        // screen. They already accepted the prompt and are mid-sign-in;
        // re-emitting the same dialog over the auth UI is noise that
        // also blocks them from finishing the device-flow steps. Also
        // flush any pending flag set by background API calls during
        // auth, so it doesn't ghost back when the user returns home.
        val onAuthScreen = currentScreen is GithubStoreGraph.AuthenticationScreen
        LaunchedEffect(onAuthScreen, state.showRateLimitDialog) {
            if (onAuthScreen && state.showRateLimitDialog) {
                viewModel.onAction(MainAction.DismissRateLimitDialog)
            }
        }
        if (state.showRateLimitDialog && state.rateLimitInfo != null && !onAuthScreen) {
            RateLimitDialog(
                rateLimitInfo = state.rateLimitInfo!!,
                isAuthenticated = state.isLoggedIn,
                onDismiss = {
                    viewModel.onAction(MainAction.DismissRateLimitDialog)
                },
                onSignIn = {
                    viewModel.onAction(MainAction.DismissRateLimitDialog)

                    navController.navigate(GithubStoreGraph.AuthenticationScreen)
                },
            )
        }

        if (state.showSessionExpiredDialog) {
            SessionExpiredDialog(
                onDismiss = {
                    viewModel.onAction(MainAction.DismissSessionExpiredDialog)
                },
                onSignIn = {
                    viewModel.onAction(MainAction.DismissSessionExpiredDialog)
                    navController.navigate(GithubStoreGraph.AuthenticationScreen)
                },
            )
        }

        AppNavigation(
            navController = navController,
            isLiquidGlassEnabled = state.isLiquidGlassEnabled,
            isScrollbarEnabled = state.isScrollbarEnabled,
        )

        val whatsNewViewModel: WhatsNewViewModel = koinViewModel()
        val pendingEntry by whatsNewViewModel.pendingEntry.collectAsStateWithLifecycle()
        val hasHistory by whatsNewViewModel.hasHistory.collectAsStateWithLifecycle()
        val onHomeScreen = currentScreen is GithubStoreGraph.HomeScreen
        val authSettled = !state.showSessionExpiredDialog && !onAuthScreen
        val rateLimitCleared = !state.showRateLimitDialog
        val canShowWhatsNew = onHomeScreen && authSettled && rateLimitCleared

        var debouncedReady by remember { mutableStateOf(false) }
        LaunchedEffect(canShowWhatsNew) {
            if (canShowWhatsNew) {
                delay(600)
                debouncedReady = true
            } else {
                debouncedReady = false
            }
        }

        val entryToShow = pendingEntry
        if (entryToShow != null && canShowWhatsNew && debouncedReady) {
            WhatsNewSheet(
                entry = entryToShow,
                showHistoryAction = hasHistory,
                onDismiss = { whatsNewViewModel.markSeen() },
                onViewHistory = {
                    whatsNewViewModel.markSeen()
                    navController.navigate(GithubStoreGraph.WhatsNewHistoryScreen)
                },
            )
        }

        val announcementsViewModel: AnnouncementsViewModel = koinViewModel()
        val pendingCritical by announcementsViewModel.pendingCriticalAcknowledgment.collectAsStateWithLifecycle()
        val criticalToShow = pendingCritical
        if (criticalToShow != null && canShowWhatsNew && debouncedReady && entryToShow == null) {
            CriticalAnnouncementModal(
                announcement = criticalToShow,
                onAcknowledge = { announcementsViewModel.acknowledge(criticalToShow) },
                onOpenDetails = { announcementsViewModel.openCta(criticalToShow) },
            )
        }
    }
}
