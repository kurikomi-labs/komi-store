package zed.rainxch.githubstore.app.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.toRoute
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import zed.rainxch.apps.presentation.AppsRoot
import zed.rainxch.apps.presentation.AppsViewModel
import zed.rainxch.apps.presentation.import.ExternalImportRoot
import zed.rainxch.auth.presentation.AuthenticationRoot
import zed.rainxch.core.domain.getPlatform
import zed.rainxch.core.domain.model.ContentWidth
import zed.rainxch.core.domain.model.Platform
import zed.rainxch.core.presentation.components.announcements.AnnouncementsRoot
import zed.rainxch.core.presentation.components.whatsnew.WhatsNewHistoryScreen
import zed.rainxch.core.presentation.locals.LocalBottomNavigationHeight
import zed.rainxch.core.presentation.locals.LocalContentWidth
import zed.rainxch.core.presentation.locals.LocalScrollbarEnabled
import zed.rainxch.details.presentation.DetailsRoot
import zed.rainxch.devprofile.presentation.DeveloperProfileRoot
import zed.rainxch.favourites.presentation.FavouritesRoot
import zed.rainxch.githubstore.app.announcements.AnnouncementsViewModel
import zed.rainxch.githubstore.app.whatsnew.WhatsNewViewModel
import zed.rainxch.home.presentation.HomeRoot
import zed.rainxch.profile.presentation.ProfileRoot
import zed.rainxch.recentlyviewed.presentation.RecentlyViewedRoot
import zed.rainxch.search.presentation.SearchRoot
import zed.rainxch.search.presentation.mappers.toSearchPlatformUi
import zed.rainxch.starred.presentation.StarredReposRoot
import zed.rainxch.tweaks.presentation.TweaksRoot
import zed.rainxch.tweaks.presentation.hidden.HiddenRepositoriesRoot
import zed.rainxch.tweaks.presentation.hosttokens.HostTokensRoot
import zed.rainxch.tweaks.presentation.mirror.AutoSuggestMirrorViewModel
import zed.rainxch.tweaks.presentation.mirror.MirrorPickerRoot
import zed.rainxch.tweaks.presentation.mirror.components.AutoSuggestMirrorSheet
import zed.rainxch.tweaks.presentation.skipped.SkippedUpdatesRoot

// Cross-screen "return result" key: set by the external-import wizard's
// "Add manually" path before navigateUp(), read once by the Apps screen.
private const val EXTERNAL_IMPORT_OPEN_LINK_SHEET_KEY = "external_import_open_link_sheet"

@Composable
fun AppNavigation(
    navController: NavHostController,
    isScrollbarEnabled: Boolean = false,
    contentWidth: ContentWidth = ContentWidth.COMPACT,
) {
    var bottomNavigationHeight by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current

    val appsViewModel = koinViewModel<AppsViewModel>()
    val appsState by appsViewModel.state.collectAsStateWithLifecycle()

    val whatsNewViewModel = koinViewModel<WhatsNewViewModel>()
    val announcementsViewModel = koinViewModel<AnnouncementsViewModel>()
    val announcementsUnreadCount by announcementsViewModel.unreadCount.collectAsStateWithLifecycle()

    val isDesktop = getPlatform() != Platform.ANDROID

    CompositionLocalProvider(
        LocalBottomNavigationHeight provides bottomNavigationHeight,
        LocalScrollbarEnabled provides isScrollbarEnabled,
        LocalContentWidth provides contentWidth,
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            val desktopDrawerCurrent =
                navController.currentBackStackEntryAsState().value.getCurrentScreen()
            if (isDesktop && desktopDrawerCurrent != null) {
                DesktopDrawer(
                    currentScreen = desktopDrawerCurrent,
                    onNavigate = { target ->
                        navController.navigate(target) {
                            popUpTo(GithubStoreGraph.HomeScreen) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    isUpdateAvailable =
                        appsState.apps.any { it.installedApp.isUpdateAvailable } ||
                            appsState.showImportProposalBanner,
                    hasUnreadAnnouncements = announcementsUnreadCount > 0,
                )
            }
            Box(
                modifier = if (isDesktop) Modifier.weight(1f).fillMaxHeight() else Modifier.fillMaxSize(),
            ) {
                NavHost(
                    navController = navController,
                    startDestination = GithubStoreGraph.HomeScreen,
                    modifier = Modifier.background(MaterialTheme.colorScheme.background),
                ) {
                    composable<GithubStoreGraph.HomeScreen> {
                        HomeRoot(
                            onNavigateToSearch = { _ ->
                                navController.navigate(GithubStoreGraph.SearchScreen())
                            },
                            onNavigateToSettings = {
                                navController.navigate(GithubStoreGraph.ProfileScreen)
                            },
                            onNavigateToApps = {
                                navController.navigate(GithubStoreGraph.AppsScreen)
                            },
                            onNavigateToDetails = { repoId ->
                                navController.navigate(
                                    GithubStoreGraph.DetailsScreen(
                                        repositoryId = repoId,
                                    ),
                                )
                            },
                            onNavigateToDeveloperProfile = { username ->
                                navController.navigate(
                                    GithubStoreGraph.DeveloperProfileScreen(
                                        username = username,
                                    ),
                                )
                            },
                        )
                    }

                    composable<GithubStoreGraph.SearchScreen> { backStackEntry ->
                        val args = backStackEntry.toRoute<GithubStoreGraph.SearchScreen>()
                        val initialPlatform =
                            args.initialPlatform?.let { name ->
                                runCatching {
                                    zed.rainxch.search.presentation.model.SearchPlatformUi
                                        .valueOf(name)
                                }.getOrNull()
                            }
                        SearchRoot(
                            onNavigateBack = {
                                navController.navigateUp()
                            },
                            onNavigateToDetails = { repoId, sourceHost ->
                                navController.navigate(
                                    GithubStoreGraph.DetailsScreen(
                                        repositoryId = repoId,
                                        sourceHost = sourceHost,
                                    ),
                                )
                            },
                            onNavigateToDetailsFromLink = { owner, repo ->
                                navController.navigate(
                                    GithubStoreGraph.DetailsScreen(
                                        owner = owner,
                                        repo = repo,
                                    ),
                                )
                            },
                            onNavigateToDeveloperProfile = { username ->
                                navController.navigate(
                                    GithubStoreGraph.DeveloperProfileScreen(
                                        username = username,
                                    ),
                                )
                            },
                            viewModel =
                                koinViewModel {
                                    parametersOf(initialPlatform)
                                },
                        )
                    }

                    composable<GithubStoreGraph.DetailsScreen> { backStackEntry ->
                        val args = backStackEntry.toRoute<GithubStoreGraph.DetailsScreen>()
                        DetailsRoot(
                            onNavigateBack = {
                                navController.navigateUp()
                            },
                            onOpenRepositoryInApp = { repoId ->
                                navController.navigate(
                                    GithubStoreGraph.DetailsScreen(
                                        repositoryId = repoId,
                                    ),
                                )
                            },
                            onNavigateToDeveloperProfile = { username ->
                                navController.navigate(
                                    GithubStoreGraph.DeveloperProfileScreen(
                                        username = username,
                                    ),
                                )
                            },
                            onNavigateToSearchByPlatform = { platform ->
                                navController.navigate(
                                    GithubStoreGraph.SearchScreen(
                                        initialPlatform = platform.toSearchPlatformUi().name,
                                    ),
                                )
                            },
                            viewModel =
                                koinViewModel {
                                    parametersOf(
                                        args.repositoryId,
                                        args.owner,
                                        args.repo,
                                        args.isComingFromUpdate,
                                        args.sourceHost,
                                    )
                                },
                        )
                    }

                    composable<GithubStoreGraph.DeveloperProfileScreen> { backStackEntry ->
                        val args = backStackEntry.toRoute<GithubStoreGraph.DeveloperProfileScreen>()
                        DeveloperProfileRoot(
                            onNavigateBack = {
                                navController.navigateUp()
                            },
                            onNavigateToDetails = { repoId ->
                                navController.navigate(
                                    GithubStoreGraph.DetailsScreen(
                                        repositoryId = repoId,
                                    ),
                                )
                            },
                            viewModel =
                                koinViewModel {
                                    parametersOf(args.username)
                                },
                        )
                    }

                    composable<GithubStoreGraph.AuthenticationScreen> {
                        AuthenticationRoot(
                            onNavigateToHome = {
                                navController.navigate(GithubStoreGraph.HomeScreen) {
                                    popUpTo(0) {
                                        inclusive = true
                                    }
                                }
                            },
                        )
                    }

                    composable<GithubStoreGraph.OnboardingScreen> {
                        zed.rainxch.githubstore.app.onboarding.OnboardingRoot(
                            onNavigateToSignIn = {
                                navController.navigate(GithubStoreGraph.AuthenticationScreen)
                            },
                            onNavigateToHome = {
                                navController.navigate(GithubStoreGraph.HomeScreen) {
                                    popUpTo(0) { inclusive = true }
                                }
                            },
                        )
                    }

                    composable<GithubStoreGraph.FavouritesScreen> {
                        FavouritesRoot(
                            onNavigateBack = {
                                navController.navigateUp()
                            },
                            onNavigateToDetails = {
                                navController.navigate(GithubStoreGraph.DetailsScreen(it))
                            },
                            onNavigateToDeveloperProfile = { username ->
                                navController.navigate(
                                    GithubStoreGraph.DeveloperProfileScreen(
                                        username = username,
                                    ),
                                )
                            },
                        )
                    }

                    composable<GithubStoreGraph.StarredReposScreen> {
                        StarredReposRoot(
                            onNavigateBack = {
                                navController.navigateUp()
                            },
                            onNavigateToDetails = { repoId ->
                                navController.navigate(
                                    GithubStoreGraph.DetailsScreen(
                                        repositoryId = repoId,
                                    ),
                                )
                            },
                            onNavigateToAuthentication = {
                                navController.navigate(
                                    GithubStoreGraph.AuthenticationScreen,
                                )
                            },
                            onNavigateToDeveloperProfile = { username ->
                                navController.navigate(
                                    GithubStoreGraph.DeveloperProfileScreen(
                                        username = username,
                                    ),
                                )
                            },
                        )
                    }

                    composable<GithubStoreGraph.StarredPickerScreen> {
                        zed.rainxch.apps.presentation.starred.StarredPickerRoot(
                            onNavigateBack = { navController.navigateUp() },
                            onNavigateToDetails = { repoId, owner, repo ->
                                navController.navigate(
                                    GithubStoreGraph.DetailsScreen(
                                        repositoryId = repoId,
                                        owner = owner,
                                        repo = repo,
                                    ),
                                )
                            },
                        )
                    }

                    composable<GithubStoreGraph.ProfileScreen> {
                        ProfileRoot(
                            onNavigateBack = {
                                navController.navigateUp()
                            },
                            onNavigateToAuthentication = {
                                navController.navigate(GithubStoreGraph.AuthenticationScreen)
                            },
                            onNavigateToStarredRepos = {
                                navController.navigate(GithubStoreGraph.StarredReposScreen)
                            },
                            onNavigateToFavouriteRepos = {
                                navController.navigate(GithubStoreGraph.FavouritesScreen)
                            },
                            onNavigateToRecentlyViewed = {
                                navController.navigate(GithubStoreGraph.RecentlyViewedScreen)
                            },
                            onNavigateToDevProfile = { username ->
                                navController.navigate(GithubStoreGraph.DeveloperProfileScreen(username))
                            },
                            onNavigateToWhatsNew = {
                                navController.navigate(GithubStoreGraph.WhatsNewHistoryScreen)
                            },
                            onPreviewWhatsNewSheet = {
                                whatsNewViewModel.forceShowLatest()
                                navController.navigateUp()
                            },
                            onNavigateToAnnouncements = {
                                navController.navigate(GithubStoreGraph.AnnouncementsScreen)
                            },
                            onPreviewAnnouncements = {
                                announcementsViewModel.previewSampleAnnouncements()
                                navController.navigate(GithubStoreGraph.AnnouncementsScreen)
                            },
                            hasUnreadAnnouncements = announcementsUnreadCount > 0,
                        )
                    }

                    composable<GithubStoreGraph.RecentlyViewedScreen> {
                        RecentlyViewedRoot(
                            onNavigateBack = {
                                navController.navigateUp()
                            },
                            onNavigateToDetails = { repoId ->
                                navController.navigate(
                                    GithubStoreGraph.DetailsScreen(
                                        repositoryId = repoId,
                                    ),
                                )
                            },
                            onNavigateToDeveloperProfile = { username ->
                                navController.navigate(
                                    GithubStoreGraph.DeveloperProfileScreen(
                                        username = username,
                                    ),
                                )
                            },
                        )
                    }

                    composable<GithubStoreGraph.MirrorPickerScreen> {
                        MirrorPickerRoot(
                            onNavigateBack = { navController.popBackStack() },
                        )
                    }

                    composable<GithubStoreGraph.WhatsNewHistoryScreen> {
                        val historyEntries by whatsNewViewModel.historyEntries.collectAsStateWithLifecycle()
                        WhatsNewHistoryScreen(
                            entries = historyEntries,
                            onNavigateBack = { navController.navigateUp() },
                        )
                    }

                    composable<GithubStoreGraph.AnnouncementsScreen> {
                        val feed by announcementsViewModel.feed.collectAsStateWithLifecycle()
                        val displayed by announcementsViewModel.displayedItems.collectAsStateWithLifecycle()
                        AnnouncementsRoot(
                            items = displayed,
                            acknowledgedIds = feed.acknowledgedIds,
                            mutedCategories = feed.mutedCategories,
                            refreshFailed = feed.lastRefreshFailed,
                            onNavigateBack = { navController.navigateUp() },
                            onRefresh = { announcementsViewModel.refresh() },
                            onCtaClick = { announcementsViewModel.openCta(it) },
                            onDismissClick = { announcementsViewModel.dismiss(it) },
                            onAcknowledgeClick = { announcementsViewModel.acknowledge(it) },
                            onToggleMute = { category, muted ->
                                announcementsViewModel.setMuted(category, muted)
                            },
                            onLeavingScreen = { announcementsViewModel.clearPreview() },
                            onEnteringScreen = { announcementsViewModel.markRoutineItemsSeen() },
                        )
                    }

                    composable<GithubStoreGraph.TweaksScreen> {
                        TweaksRoot(
                            onNavigateToMirrorPicker = {
                                navController.navigate(GithubStoreGraph.MirrorPickerScreen) {
                                    launchSingleTop = true
                                }
                            },
                            onNavigateToSkippedUpdates = {
                                navController.navigate(GithubStoreGraph.SkippedUpdatesScreen) {
                                    launchSingleTop = true
                                }
                            },
                            onNavigateToHiddenRepositories = {
                                navController.navigate(GithubStoreGraph.HiddenRepositoriesScreen) {
                                    launchSingleTop = true
                                }
                            },
                            onNavigateToHostTokens = {
                                navController.navigate(GithubStoreGraph.HostTokensScreen) {
                                    launchSingleTop = true
                                }
                            },
                        )
                    }

                    composable<GithubStoreGraph.SkippedUpdatesScreen> {
                        SkippedUpdatesRoot(
                            onNavigateBack = { navController.popBackStack() },
                        )
                    }

                    composable<GithubStoreGraph.HiddenRepositoriesScreen> {
                        HiddenRepositoriesRoot(
                            onNavigateBack = { navController.popBackStack() },
                        )
                    }

                    composable<GithubStoreGraph.HostTokensScreen> {
                        HostTokensRoot(
                            onNavigateBack = { navController.popBackStack() },
                        )
                    }

                    composable<GithubStoreGraph.AppsScreen> { backStackEntry ->
                        // Pick up the "open link sheet" flag set by ExternalImportRoot's
                        // "Add manually" path. We consume the flag once on entry so a
                        // later config change or back-stack rewind doesn't reopen the sheet.
                        LaunchedEffect(backStackEntry) {
                            val handle = backStackEntry.savedStateHandle
                            val openLinkSheet = handle.get<Boolean>(EXTERNAL_IMPORT_OPEN_LINK_SHEET_KEY)
                            if (openLinkSheet == true) {
                                handle.remove<Boolean>(EXTERNAL_IMPORT_OPEN_LINK_SHEET_KEY)
                                appsViewModel.onAction(zed.rainxch.apps.presentation.AppsAction.OnAddByLinkClick)
                            }
                        }
                        AppsRoot(
                            onNavigateBack = {
                                navController.navigateUp()
                            },
                            onNavigateToRepo = { repoId, sourceHost, owner, repo ->
                                navController.navigate(
                                    GithubStoreGraph.DetailsScreen(
                                        repositoryId = repoId,
                                        isComingFromUpdate = true,
                                        sourceHost = sourceHost,
                                        owner = owner.orEmpty(),
                                        repo = repo.orEmpty(),
                                    ),
                                )
                            },
                            onNavigateToExternalImport = {
                                navController.navigate(GithubStoreGraph.ExternalImportScreen)
                            },
                            onNavigateToStarredPicker = {
                                navController.navigate(GithubStoreGraph.StarredPickerScreen)
                            },
                            viewModel = appsViewModel,
                            state = appsState,
                        )
                    }

                    composable<GithubStoreGraph.ExternalImportScreen> {
                        ExternalImportRoot(
                            onNavigateBack = {
                                navController.navigateUp()
                            },
                            onNavigateToDetails = { repoId ->
                                navController.navigate(
                                    GithubStoreGraph.DetailsScreen(
                                        repositoryId = repoId,
                                        isComingFromUpdate = true,
                                    ),
                                )
                            },
                            onAddManually = {
                                navController.previousBackStackEntry
                                    ?.savedStateHandle
                                    ?.set(EXTERNAL_IMPORT_OPEN_LINK_SHEET_KEY, true)
                                navController.navigateUp()
                            },
                        )
                    }
                }

                val currentScreen =
                    navController.currentBackStackEntryAsState().value.getCurrentScreen()

                currentScreen?.takeIf { !isDesktop }?.let {
                    BottomNavigation(
                        currentScreen = currentScreen,
                        onNavigate = {
                            navController.navigate(it) {
                                popUpTo(GithubStoreGraph.HomeScreen) {
                                    saveState = true
                                }

                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        // Badge fires when either an update is waiting OR pending
                        // import candidates need review. The badge is a single dot
                        // — a union of the two conditions is honest "you have
                        // something to look at on this tab".
                        isUpdateAvailable =
                            appsState.apps.any { it.installedApp.isUpdateAvailable } ||
                                appsState.showImportProposalBanner,
                        hasUnreadAnnouncements = announcementsUnreadCount > 0,
                        modifier =
                            Modifier
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                                .padding(bottom = 24.dp)
                                .onGloballyPositioned { coordinates ->
                                    bottomNavigationHeight =
                                        with(density) { coordinates.size.height.toDp() }
                                },
                    )
                }

                val autoSuggestVm: AutoSuggestMirrorViewModel = koinViewModel()
                val isAutoSuggestVisible by autoSuggestVm.isVisible.collectAsStateWithLifecycle()
                if (isAutoSuggestVisible) {
                    AutoSuggestMirrorSheet(
                        onDismiss = autoSuggestVm::dismiss,
                        onPickOne = {
                            autoSuggestVm.onPickOneClicked()
                            navController.navigate(GithubStoreGraph.MirrorPickerScreen) {
                                launchSingleTop = true
                            }
                        },
                        onMaybeLater = autoSuggestVm::onMaybeLater,
                        onDontAskAgain = autoSuggestVm::onDontAskAgain,
                    )
                }
            }
        }
    }
}
