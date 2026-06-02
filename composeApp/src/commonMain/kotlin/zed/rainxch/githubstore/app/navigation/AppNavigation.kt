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
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import zed.rainxch.apps.presentation.AppsRoot
import zed.rainxch.apps.presentation.AppsViewModel
import zed.rainxch.apps.presentation.import.EXTERNAL_IMPORT_OPEN_LINK_SHEET_KEY
import zed.rainxch.apps.presentation.import.ExternalImportRoot
import zed.rainxch.auth.presentation.AuthenticationRoot
import zed.rainxch.core.domain.getPlatform
import zed.rainxch.core.domain.model.ContentWidth
import zed.rainxch.core.domain.model.Platform
import zed.rainxch.core.presentation.components.adaptive.AdaptiveDetailArgs
import zed.rainxch.core.presentation.components.adaptive.AdaptiveListDetailScaffold
import zed.rainxch.core.presentation.components.adaptive.rememberAdaptiveListDetailState
import zed.rainxch.core.presentation.components.announcements.AnnouncementsRoot
import zed.rainxch.core.presentation.components.whatsnew.WhatsNewHistoryScreen
import zed.rainxch.core.presentation.locals.LocalBottomNavigationHeight
import zed.rainxch.core.presentation.locals.LocalContentWidth
import zed.rainxch.core.presentation.locals.LocalScrollbarEnabled
import zed.rainxch.details.presentation.DetailsRoot
import zed.rainxch.details.presentation.about.AboutRoot
import zed.rainxch.details.presentation.whatsnew.WhatsNewRoot
import zed.rainxch.devprofile.presentation.DeveloperProfileRoot
import zed.rainxch.favourites.presentation.FavouritesRoot
import zed.rainxch.githubstore.app.announcements.AnnouncementsViewModel
import zed.rainxch.githubstore.app.whatsnew.WhatsNewViewModel
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.adaptive_pick_repo_subtitle
import zed.rainxch.githubstore.core.presentation.res.adaptive_pick_repo_title
import zed.rainxch.home.presentation.HomeRoot
import zed.rainxch.profile.presentation.ProfileRoot
import zed.rainxch.recentlyviewed.presentation.RecentlyViewedRoot
import zed.rainxch.repopages.presentation.issuedetail.IssueDetailRoot
import zed.rainxch.repopages.presentation.issues.IssuesRoot
import zed.rainxch.repopages.presentation.pulls.PullsRoot
import zed.rainxch.repopages.presentation.security.SecurityRoot
import zed.rainxch.search.presentation.SearchRoot
import zed.rainxch.search.presentation.mappers.toSearchPlatformUi
import zed.rainxch.search.presentation.model.SearchPlatformUi
import zed.rainxch.starred.presentation.StarredReposRoot
import zed.rainxch.tweaks.presentation.TweaksRoot
import zed.rainxch.tweaks.presentation.appearance.TweaksAppearanceRoot
import zed.rainxch.tweaks.presentation.appinfo.TweaksAppInfoRoot
import zed.rainxch.tweaks.presentation.connection.TweaksConnectionRoot
import zed.rainxch.tweaks.presentation.hidden.HiddenRepositoriesRoot
import zed.rainxch.tweaks.presentation.hosttokens.HostTokensRoot
import zed.rainxch.tweaks.presentation.install.TweaksInstallRoot
import zed.rainxch.tweaks.presentation.language.TweaksLanguageRoot
import zed.rainxch.tweaks.presentation.licenses.LicensesRoot
import zed.rainxch.tweaks.presentation.mirror.MirrorPickerRoot
import zed.rainxch.tweaks.presentation.privacy.TweaksPrivacyRoot
import zed.rainxch.tweaks.presentation.skipped.SkippedUpdatesRoot
import zed.rainxch.tweaks.presentation.sources.TweaksSourcesRoot
import zed.rainxch.tweaks.presentation.storage.TweaksStorageRoot
import zed.rainxch.tweaks.presentation.translation.TweaksTranslationRoot
import zed.rainxch.tweaks.presentation.updates.TweaksUpdatesRoot

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
                navController
                    .currentBackStackEntryAsState()
                    .value
                    .getCurrentScreen()
            if (isDesktop && desktopDrawerCurrent != null) {
                DesktopDrawer(
                    currentScreen = desktopDrawerCurrent,
                    onNavigate = { target ->
                        navController.navigate(target) {
                            popUpTo(GithubStoreGraph.HomeScreen) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    isUpdateAvailable =
                        appsState.apps.any {
                            it.installedApp.isUpdateAvailable
                        } || appsState.showImportProposalBanner,
                    hasUnreadAnnouncements = announcementsUnreadCount > 0,
                )
            }

            Box(
                modifier =
                    if (isDesktop) {
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    } else {
                        Modifier.fillMaxSize()
                    },
            ) {
                androidx.compose.animation.SharedTransitionLayout {
                    val sharedScope = this
                    NavHost(
                        navController = navController,
                        startDestination = GithubStoreGraph.HomeScreen,
                        modifier = Modifier.background(MaterialTheme.colorScheme.background),
                        enterTransition = {
                            val from = initialState.bottomNavIndex()
                            val to = targetState.bottomNavIndex()
                            if (from != null && to != null && from != to) {
                                val sign = if (to > from) 1 else -1
                                androidx.compose.animation.slideInHorizontally(
                                    initialOffsetX = { it * sign },
                                    animationSpec =
                                        androidx.compose.animation.core
                                            .tween(280),
                                ) +
                                    androidx.compose.animation.fadeIn(
                                        animationSpec =
                                            androidx.compose.animation.core
                                                .tween(220),
                                    )
                            } else {
                                androidx.compose.animation.slideInHorizontally(
                                    initialOffsetX = { it / 6 },
                                    animationSpec =
                                        androidx.compose.animation.core
                                            .tween(280),
                                ) +
                                    androidx.compose.animation.fadeIn(
                                        animationSpec =
                                            androidx.compose.animation.core
                                                .tween(220),
                                    )
                            }
                        },
                        exitTransition = {
                            val from = initialState.bottomNavIndex()
                            val to = targetState.bottomNavIndex()
                            if (from != null && to != null && from != to) {
                                val sign = if (to > from) -1 else 1
                                androidx.compose.animation.slideOutHorizontally(
                                    targetOffsetX = { it * sign },
                                    animationSpec =
                                        androidx.compose.animation.core
                                            .tween(280),
                                ) +
                                    androidx.compose.animation.fadeOut(
                                        animationSpec =
                                            androidx.compose.animation.core
                                                .tween(220),
                                    )
                            } else {
                                androidx.compose.animation.fadeOut(
                                    animationSpec =
                                        androidx.compose.animation.core
                                            .tween(180),
                                )
                            }
                        },
                        popEnterTransition = {
                            androidx.compose.animation.fadeIn(
                                animationSpec =
                                    androidx.compose.animation.core
                                        .tween(220),
                            )
                        },
                        popExitTransition = {
                            androidx.compose.animation.slideOutHorizontally(
                                targetOffsetX = { it / 6 },
                                animationSpec =
                                    androidx.compose.animation.core
                                        .tween(280),
                            ) +
                                androidx.compose.animation.fadeOut(
                                    animationSpec =
                                        androidx.compose.animation.core
                                            .tween(220),
                                )
                        },
                    ) {
                        composable<GithubStoreGraph.HomeScreen> {
                            val animatedScope = this
                            CompositionLocalProvider(
                                zed.rainxch.core.presentation.locals.LocalSharedTransitionScope provides sharedScope,
                                zed.rainxch.core.presentation.locals.LocalAnimatedVisibilityScope provides animatedScope,
                            ) {
                                val listDetailState = rememberAdaptiveListDetailState()
                                val pickRepoTitle =
                                    stringResource(Res.string.adaptive_pick_repo_title)
                                val pickRepoSubtitle =
                                    stringResource(Res.string.adaptive_pick_repo_subtitle)
                                AdaptiveListDetailScaffold(
                                    state = listDetailState,
                                    emptyPaneTitle = pickRepoTitle,
                                    emptyPaneSubtitle = pickRepoSubtitle,
                                    list = { isExpanded ->
                                        HomeRoot(
                                            onNavigateToSearch = {
                                                navController.navigate(GithubStoreGraph.SearchScreen())
                                            },
                                            onNavigateToSettings = {
                                                navController.navigate(GithubStoreGraph.ProfileScreen)
                                            },
                                            onNavigateToApps = {
                                                navController.navigate(GithubStoreGraph.AppsScreen)
                                            },
                                            onNavigateToDetails = { repoId ->
                                                if (isExpanded) {
                                                    listDetailState.select(
                                                        AdaptiveDetailArgs(repositoryId = repoId),
                                                    )
                                                } else {
                                                    navController.navigate(
                                                        GithubStoreGraph.DetailsScreen(repositoryId = repoId),
                                                    )
                                                }
                                            },
                                            onNavigateToDeveloperProfile = { username ->
                                                navController.navigate(
                                                    GithubStoreGraph.DeveloperProfileScreen(username = username),
                                                )
                                            },
                                            onNavigateToCategoryList = { category ->
                                                navController.navigate(
                                                    GithubStoreGraph.CategoryListScreen(category.name),
                                                )
                                            },
                                            onNavigateToStarredRepos = {
                                                navController.navigate(GithubStoreGraph.StarredReposScreen)
                                            },
                                        )
                                    },
                                    detail = { args ->
                                        AdaptiveDetailPaneContent(
                                            args = args,
                                            navController = navController,
                                            onCrossNavToRepo = { newArgs ->
                                                listDetailState.select(
                                                    newArgs,
                                                )
                                            },
                                            onClearPane = { listDetailState.clear() },
                                        )
                                    },
                                )
                            }
                        }

                        composable<GithubStoreGraph.CategoryListScreen> { backStackEntry ->
                            val args = backStackEntry.toRoute<GithubStoreGraph.CategoryListScreen>()
                            val category =
                                runCatching {
                                    zed.rainxch.home.domain.model.HomeCategory
                                        .valueOf(args.category)
                                }.getOrDefault(
                                    zed.rainxch.home.domain.model.HomeCategory.HOT_RELEASE,
                                )
                            zed.rainxch.home.presentation.categorylist.CategoryListRoot(
                                category = category,
                                onNavigateBack = { navController.navigateUp() },
                                onNavigateToDetails = { repoId ->
                                    navController.navigate(
                                        GithubStoreGraph.DetailsScreen(repositoryId = repoId),
                                    )
                                },
                            )
                        }

                        composable<GithubStoreGraph.SearchScreen> { backStackEntry ->
                            val args = backStackEntry.toRoute<GithubStoreGraph.SearchScreen>()
                            val initialPlatform =
                                args.initialPlatform?.let { name ->
                                    runCatching {
                                        SearchPlatformUi.valueOf(name)
                                    }.getOrNull()
                                }
                            val listDetailState = rememberAdaptiveListDetailState()
                            val pickRepoTitle = stringResource(Res.string.adaptive_pick_repo_title)
                            val pickRepoSubtitle =
                                stringResource(Res.string.adaptive_pick_repo_subtitle)
                            val searchViewModel: zed.rainxch.search.presentation.SearchViewModel =
                                koinViewModel {
                                    parametersOf(initialPlatform)
                                }
                            AdaptiveListDetailScaffold(
                                state = listDetailState,
                                emptyPaneTitle = pickRepoTitle,
                                emptyPaneSubtitle = pickRepoSubtitle,
                                list = { isExpanded ->
                                    SearchRoot(
                                        onNavigateBack = {
                                            navController.navigateUp()
                                        },
                                        onNavigateToDetails = { repoId, sourceHost ->
                                            if (isExpanded) {
                                                listDetailState.select(
                                                    AdaptiveDetailArgs(
                                                        repositoryId = repoId,
                                                        sourceHost = sourceHost,
                                                    ),
                                                )
                                            } else {
                                                navController.navigate(
                                                    GithubStoreGraph.DetailsScreen(
                                                        repositoryId = repoId,
                                                        sourceHost = sourceHost,
                                                    ),
                                                )
                                            }
                                        },
                                        onNavigateToDetailsFromLink = { owner, repo ->
                                            if (isExpanded) {
                                                listDetailState.select(
                                                    AdaptiveDetailArgs(
                                                        owner = owner,
                                                        repo = repo,
                                                    ),
                                                )
                                            } else {
                                                navController.navigate(
                                                    GithubStoreGraph.DetailsScreen(
                                                        owner = owner,
                                                        repo = repo,
                                                    ),
                                                )
                                            }
                                        },
                                        onNavigateToDeveloperProfile = { username ->
                                            navController.navigate(
                                                GithubStoreGraph.DeveloperProfileScreen(
                                                    username = username,
                                                ),
                                            )
                                        },
                                        viewModel = searchViewModel,
                                    )
                                },
                                detail = { detailArgs ->
                                    AdaptiveDetailPaneContent(
                                        args = detailArgs,
                                        navController = navController,
                                        onCrossNavToRepo = { newArgs ->
                                            listDetailState.select(
                                                newArgs,
                                            )
                                        },
                                        onClearPane = { listDetailState.clear() },
                                    )
                                },
                            )
                        }

                        composable<GithubStoreGraph.DetailsScreen> { backStackEntry ->
                            val animatedScope = this
                            CompositionLocalProvider(
                                zed.rainxch.core.presentation.locals.LocalSharedTransitionScope provides sharedScope,
                                zed.rainxch.core.presentation.locals.LocalAnimatedVisibilityScope provides animatedScope,
                            ) {
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
                                    onNavigateToAbout = { repoId, owner, repo, sourceHost ->
                                        navController.navigate(
                                            GithubStoreGraph.DetailsAboutScreen(
                                                repositoryId = repoId,
                                                owner = owner,
                                                repo = repo,
                                                sourceHost = sourceHost,
                                            ),
                                        )
                                    },
                                    onNavigateToWhatsNew = { repoId, owner, repo, sourceHost ->
                                        navController.navigate(
                                            GithubStoreGraph.DetailsWhatsNewScreen(
                                                repositoryId = repoId,
                                                owner = owner,
                                                repo = repo,
                                                sourceHost = sourceHost,
                                            ),
                                        )
                                    },
                                    onNavigateToIssues = { owner, repo ->
                                        navController.navigate(
                                            GithubStoreGraph.RepoIssuesScreen(
                                                owner = owner,
                                                repo = repo,
                                            ),
                                        )
                                    },
                                    onNavigateToSecurity = { owner, repo ->
                                        navController.navigate(
                                            GithubStoreGraph.RepoSecurityScreen(
                                                owner = owner,
                                                repo = repo,
                                            ),
                                        )
                                    },
                                    onNavigateToPulls = { owner, repo ->
                                        navController.navigate(
                                            GithubStoreGraph.RepoPullsScreen(
                                                owner = owner,
                                                repo = repo,
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
                        }

                        composable<GithubStoreGraph.DetailsAboutScreen>(
                            enterTransition = {
                                androidx.compose.animation.slideInHorizontally(
                                    initialOffsetX = { fullWidth -> fullWidth },
                                    animationSpec =
                                        androidx.compose.animation.core
                                            .tween(durationMillis = 280),
                                )
                            },
                            exitTransition = {
                                androidx.compose.animation.slideOutHorizontally(
                                    targetOffsetX = { fullWidth -> -fullWidth / 4 },
                                    animationSpec =
                                        androidx.compose.animation.core
                                            .tween(durationMillis = 280),
                                )
                            },
                            popEnterTransition = {
                                androidx.compose.animation.slideInHorizontally(
                                    initialOffsetX = { fullWidth -> -fullWidth / 4 },
                                    animationSpec =
                                        androidx.compose.animation.core
                                            .tween(durationMillis = 280),
                                )
                            },
                            popExitTransition = {
                                androidx.compose.animation.slideOutHorizontally(
                                    targetOffsetX = { fullWidth -> fullWidth },
                                    animationSpec =
                                        androidx.compose.animation.core
                                            .tween(durationMillis = 280),
                                )
                            },
                        ) { backStackEntry ->
                            val args = backStackEntry.toRoute<GithubStoreGraph.DetailsAboutScreen>()
                            AboutRoot(
                                repositoryId = args.repositoryId,
                                owner = args.owner,
                                repo = args.repo,
                                sourceHost = args.sourceHost,
                                onNavigateBack = { navController.navigateUp() },
                            )
                        }

                        composable<GithubStoreGraph.DetailsWhatsNewScreen>(
                            enterTransition = {
                                androidx.compose.animation.slideInHorizontally(
                                    initialOffsetX = { fullWidth -> fullWidth },
                                    animationSpec =
                                        androidx.compose.animation.core
                                            .tween(durationMillis = 280),
                                )
                            },
                            exitTransition = {
                                androidx.compose.animation.slideOutHorizontally(
                                    targetOffsetX = { fullWidth -> -fullWidth / 4 },
                                    animationSpec =
                                        androidx.compose.animation.core
                                            .tween(durationMillis = 280),
                                )
                            },
                            popEnterTransition = {
                                androidx.compose.animation.slideInHorizontally(
                                    initialOffsetX = { fullWidth -> -fullWidth / 4 },
                                    animationSpec =
                                        androidx.compose.animation.core
                                            .tween(durationMillis = 280),
                                )
                            },
                            popExitTransition = {
                                androidx.compose.animation.slideOutHorizontally(
                                    targetOffsetX = { fullWidth -> fullWidth },
                                    animationSpec =
                                        androidx.compose.animation.core
                                            .tween(durationMillis = 280),
                                )
                            },
                        ) { backStackEntry ->
                            val args =
                                backStackEntry.toRoute<GithubStoreGraph.DetailsWhatsNewScreen>()
                            WhatsNewRoot(
                                repositoryId = args.repositoryId,
                                owner = args.owner,
                                repo = args.repo,
                                sourceHost = args.sourceHost,
                                onNavigateBack = { navController.navigateUp() },
                            )
                        }

                        composable<GithubStoreGraph.RepoIssuesScreen> { backStackEntry ->
                            val args = backStackEntry.toRoute<GithubStoreGraph.RepoIssuesScreen>()
                            IssuesRoot(
                                onNavigateBack = { navController.navigateUp() },
                                onOpenIssue = { issueNumber ->
                                    navController.navigate(
                                        GithubStoreGraph.RepoIssueDetailScreen(
                                            owner = args.owner,
                                            repo = args.repo,
                                            issueNumber = issueNumber,
                                        ),
                                    )
                                },
                                viewModel = koinViewModel { parametersOf(args.owner, args.repo) },
                            )
                        }

                        composable<GithubStoreGraph.RepoIssueDetailScreen> { backStackEntry ->
                            val args =
                                backStackEntry.toRoute<GithubStoreGraph.RepoIssueDetailScreen>()
                            IssueDetailRoot(
                                onNavigateBack = { navController.navigateUp() },
                                viewModel =
                                    koinViewModel {
                                        parametersOf(
                                            args.owner,
                                            args.repo,
                                            args.issueNumber,
                                        )
                                    },
                            )
                        }

                        composable<GithubStoreGraph.RepoSecurityScreen> { backStackEntry ->
                            val args = backStackEntry.toRoute<GithubStoreGraph.RepoSecurityScreen>()
                            SecurityRoot(
                                onNavigateBack = { navController.navigateUp() },
                                viewModel = koinViewModel { parametersOf(args.owner, args.repo) },
                            )
                        }

                        composable<GithubStoreGraph.RepoPullsScreen> { backStackEntry ->
                            val args = backStackEntry.toRoute<GithubStoreGraph.RepoPullsScreen>()
                            PullsRoot(
                                onNavigateBack = { navController.navigateUp() },
                                onOpenPull = { number ->
                                    navController.navigate(
                                        GithubStoreGraph.RepoIssueDetailScreen(
                                            owner = args.owner,
                                            repo = args.repo,
                                            issueNumber = number,
                                        ),
                                    )
                                },
                                viewModel = koinViewModel { parametersOf(args.owner, args.repo) },
                            )
                        }

                        composable<GithubStoreGraph.DeveloperProfileScreen> { backStackEntry ->
                            val args =
                                backStackEntry.toRoute<GithubStoreGraph.DeveloperProfileScreen>()
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
                                onNavigateToUser = { username ->
                                    navController.navigate(
                                        GithubStoreGraph.DeveloperProfileScreen(username = username),
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
                                    navController.navigate(
                                        GithubStoreGraph.DeveloperProfileScreen(
                                            username,
                                        ),
                                    )
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
                                onNavigateToTweaks = {
                                    navController.navigate(GithubStoreGraph.TweaksScreen)
                                },
                                onNavigateToAbout = {
                                    navController.navigate(GithubStoreGraph.AboutScreen)
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
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToAppearance = {
                                    navController.navigate(GithubStoreGraph.TweaksAppearanceScreen) {
                                        launchSingleTop = true
                                    }
                                },
                                onNavigateToLanguage = {
                                    navController.navigate(GithubStoreGraph.TweaksLanguageScreen) {
                                        launchSingleTop = true
                                    }
                                },
                                onNavigateToConnection = {
                                    navController.navigate(GithubStoreGraph.TweaksConnectionScreen) {
                                        launchSingleTop = true
                                    }
                                },
                                onNavigateToSources = {
                                    navController.navigate(GithubStoreGraph.TweaksSourcesScreen) {
                                        launchSingleTop = true
                                    }
                                },
                                onNavigateToTranslation = {
                                    navController.navigate(GithubStoreGraph.TweaksTranslationScreen) {
                                        launchSingleTop = true
                                    }
                                },
                                onNavigateToInstallMethod = {
                                    navController.navigate(GithubStoreGraph.TweaksInstallScreen) {
                                        launchSingleTop = true
                                    }
                                },
                                onNavigateToUpdates = {
                                    navController.navigate(GithubStoreGraph.TweaksUpdatesScreen) {
                                        launchSingleTop = true
                                    }
                                },
                                onNavigateToStorage = {
                                    navController.navigate(GithubStoreGraph.TweaksStorageScreen) {
                                        launchSingleTop = true
                                    }
                                },
                                onNavigateToPrivacy = {
                                    navController.navigate(GithubStoreGraph.TweaksPrivacyScreen) {
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

                        composable<GithubStoreGraph.TweaksAppearanceScreen> {
                            TweaksAppearanceRoot(
                                onNavigateBack = { navController.popBackStack() },
                            )
                        }

                        composable<GithubStoreGraph.TweaksLanguageScreen> {
                            TweaksLanguageRoot(
                                onNavigateBack = { navController.popBackStack() },
                            )
                        }

                        composable<GithubStoreGraph.TweaksConnectionScreen> {
                            TweaksConnectionRoot(
                                onNavigateBack = { navController.popBackStack() },
                            )
                        }

                        composable<GithubStoreGraph.TweaksSourcesScreen> {
                            TweaksSourcesRoot(
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToMirrorPicker = {
                                    navController.navigate(GithubStoreGraph.MirrorPickerScreen) {
                                        launchSingleTop = true
                                    }
                                },
                            )
                        }

                        composable<GithubStoreGraph.TweaksTranslationScreen> {
                            TweaksTranslationRoot(
                                onNavigateBack = { navController.popBackStack() },
                            )
                        }

                        composable<GithubStoreGraph.TweaksInstallScreen> {
                            TweaksInstallRoot(
                                onNavigateBack = { navController.popBackStack() },
                            )
                        }

                        composable<GithubStoreGraph.TweaksUpdatesScreen> {
                            TweaksUpdatesRoot(
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToSkippedUpdates = {
                                    navController.navigate(GithubStoreGraph.SkippedUpdatesScreen) {
                                        launchSingleTop = true
                                    }
                                },
                            )
                        }

                        composable<GithubStoreGraph.TweaksStorageScreen> {
                            TweaksStorageRoot(
                                onNavigateBack = { navController.popBackStack() },
                            )
                        }

                        composable<GithubStoreGraph.TweaksPrivacyScreen> {
                            TweaksPrivacyRoot(
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToHiddenRepositories = {
                                    navController.navigate(GithubStoreGraph.HiddenRepositoriesScreen) {
                                        launchSingleTop = true
                                    }
                                },
                            )
                        }

                        composable<GithubStoreGraph.AboutScreen> {
                            TweaksAppInfoRoot(
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToLicenses = {
                                    navController.navigate(GithubStoreGraph.LicensesScreen) {
                                        launchSingleTop = true
                                    }
                                },
                            )
                        }

                        composable<GithubStoreGraph.LicensesScreen> {
                            LicensesRoot(
                                onNavigateBack = { navController.popBackStack() },
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
                            LaunchedEffect(backStackEntry) {
                                val handle = backStackEntry.savedStateHandle
                                val openLinkSheet =
                                    handle.get<Boolean>(EXTERNAL_IMPORT_OPEN_LINK_SHEET_KEY)
                                if (openLinkSheet == true) {
                                    handle.remove<Boolean>(EXTERNAL_IMPORT_OPEN_LINK_SHEET_KEY)
                                    appsViewModel.onAction(zed.rainxch.apps.presentation.AppsAction.OnAddByLinkClick)
                                }
                            }
                            val listDetailState = rememberAdaptiveListDetailState()
                            val pickRepoTitle = stringResource(Res.string.adaptive_pick_repo_title)
                            val pickRepoSubtitle =
                                stringResource(Res.string.adaptive_pick_repo_subtitle)
                            AdaptiveListDetailScaffold(
                                state = listDetailState,
                                emptyPaneTitle = pickRepoTitle,
                                emptyPaneSubtitle = pickRepoSubtitle,
                                list = { isExpanded ->
                                    AppsRoot(
                                        onNavigateBack = {
                                            navController.navigateUp()
                                        },
                                        onNavigateToRepo = { repoId, sourceHost, owner, repo ->
                                            if (isExpanded) {
                                                listDetailState.select(
                                                    AdaptiveDetailArgs(
                                                        repositoryId = repoId,
                                                        isComingFromUpdate = true,
                                                        sourceHost = sourceHost,
                                                        owner = owner,
                                                        repo = repo,
                                                    ),
                                                )
                                            } else {
                                                navController.navigate(
                                                    GithubStoreGraph.DetailsScreen(
                                                        repositoryId = repoId,
                                                        isComingFromUpdate = true,
                                                        sourceHost = sourceHost,
                                                        owner = owner.orEmpty(),
                                                        repo = repo.orEmpty(),
                                                    ),
                                                )
                                            }
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
                                },
                                detail = { detailArgs ->
                                    AdaptiveDetailPaneContent(
                                        args = detailArgs,
                                        navController = navController,
                                        onCrossNavToRepo = { newArgs ->
                                            listDetailState.select(
                                                newArgs,
                                            )
                                        },
                                        onClearPane = { listDetailState.clear() },
                                    )
                                },
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
            }
        }
    }
}
