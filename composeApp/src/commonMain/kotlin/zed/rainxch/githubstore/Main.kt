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
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel
import zed.rainxch.auth.presentation.AuthDeepLinkBus
import zed.rainxch.auth.presentation.AuthDeepLinkEvent
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
    setSingletonImageLoaderFactory { context ->
        ImageLoader
            .Builder(context)
            .components { add(coil3.svg.SvgDecoder.Factory()) }
            .build()
    }

    val viewModel: MainViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val currentScreen = navController.currentBackStackEntryAsState().value.getCurrentScreen()

    LaunchedEffect(state.onboardingComplete) {
        if (state.onboardingComplete == false &&
            currentScreen !is GithubStoreGraph.OnboardingScreen
        ) {
            navController.navigate(GithubStoreGraph.OnboardingScreen) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

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
                    navController.navigate(GithubStoreGraph.AppsScreen) {
                        popUpTo(GithubStoreGraph.HomeScreen) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }

                is DeepLinkDestination.AuthHandoff -> {
                    AuthDeepLinkBus.publish(
                        AuthDeepLinkEvent.Handoff(destination.handoffId, destination.state),
                    )
                    if (currentScreen !is GithubStoreGraph.AuthenticationScreen) {
                        navController.navigate(GithubStoreGraph.AuthenticationScreen) {
                            launchSingleTop = true
                        }
                    }
                }

                is DeepLinkDestination.AuthError -> {
                    AuthDeepLinkBus.publish(
                        AuthDeepLinkEvent.Error(destination.reason, destination.state),
                    )
                    if (currentScreen !is GithubStoreGraph.AuthenticationScreen) {
                        navController.navigate(GithubStoreGraph.AuthenticationScreen) {
                            launchSingleTop = true
                        }
                    }
                }

                DeepLinkDestination.Tweaks -> {
                    navController.navigate(GithubStoreGraph.TweaksScreen) {
                        launchSingleTop = true
                    }
                }

                DeepLinkDestination.About -> {
                    navController.navigate(GithubStoreGraph.AboutScreen) {
                        launchSingleTop = true
                    }
                }

                DeepLinkDestination.TweaksLicenses -> {
                    navController.navigate(GithubStoreGraph.LicensesScreen) {
                        launchSingleTop = true
                    }
                }

                DeepLinkDestination.None -> {
                }
            }
        }
    }

    ObserveAsEvents(KeyboardNavigation.events) { event ->
        when (event) {
            KeyboardNavigationEvent.OnCtrlFClick -> {
                if (currentScreen !is GithubStoreGraph.SearchScreen) {
                    navController.navigate(GithubStoreGraph.SearchScreen()) {
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
