package zed.rainxch.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import zed.rainxch.core.domain.getPlatform
import zed.rainxch.core.domain.logging.GitHubStoreLogger
import zed.rainxch.core.domain.model.DiscoveryPlatform
import zed.rainxch.core.domain.model.GithubRepoSummary
import zed.rainxch.core.domain.model.InstalledApp
import zed.rainxch.core.domain.model.Platform
import zed.rainxch.core.domain.model.hasActualUpdate
import zed.rainxch.core.domain.model.isReallyInstalled
import zed.rainxch.core.domain.repository.FavouritesRepository
import zed.rainxch.core.domain.repository.HiddenReposRepository
import zed.rainxch.core.domain.repository.InstalledAppsRepository
import zed.rainxch.core.domain.repository.SeenReposRepository
import zed.rainxch.core.domain.repository.StarredRepository
import zed.rainxch.core.domain.repository.TweaksRepository
import zed.rainxch.core.domain.repository.UserSessionRepository
import zed.rainxch.core.domain.use_cases.SyncInstalledAppsUseCase
import zed.rainxch.core.domain.utils.ShareManager
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.failed_to_share_link
import zed.rainxch.githubstore.core.presentation.res.home_failed_to_load_repositories
import zed.rainxch.githubstore.core.presentation.res.link_copied_to_clipboard
import zed.rainxch.home.domain.repository.HomeRepository
import zed.rainxch.home.presentation.model.HomeRepoCardUi
import zed.rainxch.home.presentation.model.toHomeRepoCardUi

private data class RawRepo(
    val raw: GithubRepoSummary,
    val isInstalled: Boolean,
    val isUpdateAvailable: Boolean,
    val isFavourite: Boolean,
    val isStarred: Boolean,
)

private data class RawHomeState(
    val hot: List<RawRepo> = emptyList(),
    val trending: List<RawRepo> = emptyList(),
    val popular: List<RawRepo> = emptyList(),
    val starred: List<RawRepo> = emptyList(),
    val installedById: Map<Long, List<InstalledApp>> = emptyMap(),
    val favouriteIds: Set<Long> = emptySet(),
    val starredIds: Set<Long> = emptySet(),
    val seenIds: Set<Long> = emptySet(),
    val hiddenIds: Set<Long> = emptySet(),
    val isHideSeenEnabled: Boolean = false,
    val isHotLoading: Boolean = false,
    val isTrendingLoading: Boolean = false,
    val isPopularLoading: Boolean = false,
    val isStarredLoading: Boolean = false,
    val errorMessage: String? = null,
    val selectedPlatforms: Set<DiscoveryPlatform> = emptySet(),
    val isPlatformPopupVisible: Boolean = false,
    val isUpdateAvailable: Boolean = false,
    val isUserSignedIn: Boolean = false,
    val currentUserLogin: String? = null,
    val actionSheetRepoId: Long? = null,
)

class HomeViewModel(
    private val homeRepository: HomeRepository,
    private val installedAppsRepository: InstalledAppsRepository,
    private val syncInstalledAppsUseCase: SyncInstalledAppsUseCase,
    private val favouritesRepository: FavouritesRepository,
    private val starredRepository: StarredRepository,
    private val tweaksRepository: TweaksRepository,
    private val seenReposRepository: SeenReposRepository,
    private val hiddenReposRepository: HiddenReposRepository,
    private val userSessionRepository: UserSessionRepository,
    private val logger: GitHubStoreLogger,
    private val shareManager: ShareManager,
) : ViewModel() {

    private var hasLoadedInitialData = false
    private var loadJob: Job? = null

    private val rawState = MutableStateFlow(RawHomeState())

    val state: StateFlow<HomeState> = rawState
        .onStart {
            if (!hasLoadedInitialData) {
                observeCurrentUser()
                syncSystemState()
                refreshAllSections(isInitial = true)
                observeInstalledApps()
                observeFavourites()
                observeStarredRepos()
                observeSeenRepos()
                observeHiddenRepos()
                observeDiscoveryPlatforms()
                observeHideSeenEnabled()
                hasLoadedInitialData = true
            }
        }
        .map { it.toView() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = HomeState(),
        )

    private val _events = Channel<HomeEvent>(capacity = Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onAction(action: HomeAction) {
        when (action) {
            HomeAction.OnRefreshClick -> viewModelScope.launch {
                syncInstalledAppsUseCase()
                refreshAllSections(isInitial = false)
            }

            HomeAction.OnRetry -> refreshAllSections(isInitial = true)

            HomeAction.OnPlatformPopupOpen ->
                rawState.update { it.copy(isPlatformPopupVisible = true) }

            HomeAction.OnPlatformPopupDismiss ->
                rawState.update { it.copy(isPlatformPopupVisible = false) }

            is HomeAction.OnRepoLongClick ->
                rawState.update { it.copy(actionSheetRepoId = action.repoId) }

            HomeAction.OnActionSheetDismiss ->
                rawState.update { it.copy(actionSheetRepoId = null) }

            is HomeAction.OnShareClick -> viewModelScope.launch {
                runCatching {
                    shareManager.shareText("https://github-store.org/app?repo=${action.repo.fullName}")
                }.onFailure { t ->
                    logger.error("Failed to share link: ${t.message}")
                    _events.send(HomeEvent.OnMessage(getString(Res.string.failed_to_share_link)))
                    return@launch
                }
                if (getPlatform() != Platform.ANDROID) {
                    _events.send(HomeEvent.OnMessage(getString(Res.string.link_copied_to_clipboard)))
                }
            }

            is HomeAction.OnHideRepository -> viewModelScope.launch {
                val repo = action.repo
                try {
                    hiddenReposRepository.hide(
                        repoId = repo.id,
                        repoName = repo.name,
                        repoOwner = repo.owner.login,
                        repoOwnerAvatarUrl = repo.owner.avatarUrl,
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    logger.warn("Hide repository failed for ${repo.id}: ${e.message}")
                }
            }

            is HomeAction.OnUndoHideRepository -> viewModelScope.launch {
                try {
                    hiddenReposRepository.unhide(action.repoId)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    logger.warn("Unhide repository failed for ${action.repoId}: ${e.message}")
                }
            }

            is HomeAction.OnMarkAsSeen -> viewModelScope.launch {
                val repo = action.repo
                try {
                    seenReposRepository.markAsSeen(
                        repoId = repo.id,
                        repoName = repo.name,
                        repoOwner = repo.owner.login,
                        repoOwnerAvatarUrl = repo.owner.avatarUrl,
                        repoDescription = repo.description,
                        primaryLanguage = repo.language,
                        repoUrl = repo.htmlUrl,
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    logger.warn("Mark as seen failed for ${repo.id}: ${e.message}")
                }
            }

            is HomeAction.OnMarkAsUnseen -> viewModelScope.launch {
                try {
                    seenReposRepository.removeFromHistory(action.repoId)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    logger.warn("Mark as unseen failed for ${action.repoId}: ${e.message}")
                }
            }

            HomeAction.OnSearchClick,
            HomeAction.OnSettingsClick,
            HomeAction.OnAppsClick,
            is HomeAction.OnRepoClick,
            is HomeAction.OnDeveloperClick,
            HomeAction.OnSeeAllHot,
            HomeAction.OnSeeAllTrending,
            HomeAction.OnSeeAllPopular,
            HomeAction.OnSeeAllStarred -> Unit // Handled in composable
        }
    }

    private fun refreshAllSections(isInitial: Boolean) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val platforms = tweaksRepository.getDiscoveryPlatforms().first()
            rawState.update {
                it.copy(
                    selectedPlatforms = platforms,
                    isHotLoading = true,
                    isTrendingLoading = true,
                    isPopularLoading = true,
                    isStarredLoading = it.isUserSignedIn,
                    errorMessage = null,
                    hot = if (isInitial) emptyList() else it.hot,
                    trending = if (isInitial) emptyList() else it.trending,
                    popular = if (isInitial) emptyList() else it.popular,
                    starred = if (isInitial) emptyList() else it.starred,
                )
            }
            coroutineScope {
                launch { loadHot(platforms) }
                launch { loadTrending(platforms) }
                launch { loadPopular(platforms) }
                launch { loadStarred() }
            }
        }
    }

    private suspend fun loadHot(platforms: Set<DiscoveryPlatform>) {
        try {
            val page = homeRepository.getHotReleaseRepositories(platforms, page = 1).first()
            val mapped = wrapAll(page.repos)
            rawState.update { it.copy(hot = mapped, isHotLoading = false) }
        } catch (t: CancellationException) {
            throw t
        } catch (t: Throwable) {
            logger.error("Hot section load failed: ${t.message}")
            rawState.update {
                it.copy(
                    isHotLoading = false,
                    errorMessage = it.errorMessage ?: t.message
                        ?: getString(Res.string.home_failed_to_load_repositories),
                )
            }
        }
    }

    private suspend fun loadTrending(platforms: Set<DiscoveryPlatform>) {
        try {
            val page = homeRepository.getTrendingRepositories(platforms, page = 1).first()
            rawState.update { it.copy(trending = wrapAll(page.repos), isTrendingLoading = false) }
        } catch (t: CancellationException) {
            throw t
        } catch (t: Throwable) {
            logger.error("Trending section load failed: ${t.message}")
            rawState.update { it.copy(isTrendingLoading = false) }
        }
    }

    private suspend fun loadPopular(platforms: Set<DiscoveryPlatform>) {
        try {
            val page = homeRepository.getMostPopular(platforms, page = 1).first()
            rawState.update { it.copy(popular = wrapAll(page.repos), isPopularLoading = false) }
        } catch (t: CancellationException) {
            throw t
        } catch (t: Throwable) {
            logger.error("Popular section load failed: ${t.message}")
            rawState.update { it.copy(isPopularLoading = false) }
        }
    }

    private suspend fun loadStarred() {
        if (!rawState.value.isUserSignedIn) {
            rawState.update { it.copy(starred = emptyList(), isStarredLoading = false) }
            return
        }
        try {
            runCatching { starredRepository.syncStarredRepos(forceRefresh = false) }
            val topIds = starredRepository
                .getAllStarred()
                .first()
                .sortedByDescending { it.stargazersCount }
                .take(5)
                .map { it.repoId }
            val fetched = coroutineScope {
                topIds.map { id ->
                    async { runCatching { homeRepository.getRepositoryById(id) }.getOrNull() }
                }.awaitAll().filterNotNull()
            }
            rawState.update { it.copy(starred = wrapAll(fetched), isStarredLoading = false) }
        } catch (t: CancellationException) {
            throw t
        } catch (t: Throwable) {
            logger.error("Starred section load failed: ${t.message}")
            rawState.update { it.copy(isStarredLoading = false) }
        }
    }

    private suspend fun wrapAll(repos: List<GithubRepoSummary>): List<RawRepo> {
        val installed = installedAppsRepository.getAllInstalledApps().first().groupBy { it.repoId }
        val favourites = favouritesRepository.getAllFavorites().first().map { it.repoId }.toSet()
        val starred = starredRepository.getAllStarred().first().map { it.repoId }.toSet()
        rawState.update {
            it.copy(
                installedById = installed,
                favouriteIds = favourites,
                starredIds = starred,
            )
        }
        return repos.map { repo ->
            val apps = installed[repo.id].orEmpty()
            RawRepo(
                raw = repo,
                isInstalled = apps.any { it.isReallyInstalled() },
                isUpdateAvailable = apps.any { it.hasActualUpdate() },
                isFavourite = repo.id in favourites,
                isStarred = repo.id in starred,
            )
        }
    }

    private fun syncSystemState() {
        viewModelScope.launch {
            try {
                val result = syncInstalledAppsUseCase()
                if (result.isFailure) {
                    logger.warn("Initial sync had issues: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                logger.error("Initial sync failed: ${e.message}")
            }
        }
    }

    private fun observeInstalledApps() {
        viewModelScope.launch {
            installedAppsRepository.getAllInstalledApps().collect { apps ->
                val byRepo = apps.groupBy { it.repoId }
                rawState.update { snapshot ->
                    snapshot.copy(
                        installedById = byRepo,
                        isUpdateAvailable = apps.any { it.hasActualUpdate() },
                        hot = snapshot.hot.restampInstall(byRepo),
                        trending = snapshot.trending.restampInstall(byRepo),
                        popular = snapshot.popular.restampInstall(byRepo),
                        starred = snapshot.starred.restampInstall(byRepo),
                    )
                }
            }
        }
    }

    private fun observeDiscoveryPlatforms() {
        viewModelScope.launch {
            tweaksRepository.getDiscoveryPlatforms().collect { platforms ->
                rawState.update { it.copy(selectedPlatforms = platforms) }
            }
        }
    }

    private fun observeSeenRepos() {
        viewModelScope.launch {
            seenReposRepository.getAllSeenRepoIds().collect { ids ->
                rawState.update { it.copy(seenIds = ids) }
            }
        }
    }

    private fun observeHiddenRepos() {
        viewModelScope.launch {
            hiddenReposRepository.getAllHiddenRepoIds().collect { ids ->
                rawState.update { it.copy(hiddenIds = ids) }
            }
        }
    }

    private fun observeHideSeenEnabled() {
        viewModelScope.launch {
            tweaksRepository.getHideSeenEnabled().collect { enabled ->
                rawState.update { it.copy(isHideSeenEnabled = enabled) }
            }
        }
    }

    private fun observeCurrentUser() {
        viewModelScope.launch {
            userSessionRepository.getUser().collect { user ->
                val signedIn = user != null
                val previouslySignedIn = rawState.value.isUserSignedIn
                rawState.update {
                    it.copy(
                        isUserSignedIn = signedIn,
                        currentUserLogin = user?.username,
                    )
                }
                if (signedIn != previouslySignedIn) {
                    if (signedIn) loadStarred() else rawState.update {
                        it.copy(starred = emptyList(), isStarredLoading = false)
                    }
                }
            }
        }
    }

    private fun observeFavourites() {
        viewModelScope.launch {
            favouritesRepository.getAllFavorites().collect { favourites ->
                val ids = favourites.map { it.repoId }.toSet()
                rawState.update { snapshot ->
                    snapshot.copy(
                        favouriteIds = ids,
                        hot = snapshot.hot.restampFavourite(ids),
                        trending = snapshot.trending.restampFavourite(ids),
                        popular = snapshot.popular.restampFavourite(ids),
                        starred = snapshot.starred.restampFavourite(ids),
                    )
                }
            }
        }
    }

    private fun observeStarredRepos() {
        viewModelScope.launch {
            starredRepository.getAllStarred().collect { starredRepos ->
                val ids = starredRepos.map { it.repoId }.toSet()
                rawState.update { snapshot ->
                    snapshot.copy(
                        starredIds = ids,
                        hot = snapshot.hot.restampStarred(ids),
                        trending = snapshot.trending.restampStarred(ids),
                        popular = snapshot.popular.restampStarred(ids),
                        starred = snapshot.starred.restampStarred(ids),
                    )
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        loadJob?.cancel()
    }
}

private fun List<RawRepo>.restampInstall(byRepo: Map<Long, List<InstalledApp>>) = map { item ->
    val apps = byRepo[item.raw.id].orEmpty()
    item.copy(
        isInstalled = apps.any { it.isReallyInstalled() },
        isUpdateAvailable = apps.any { it.hasActualUpdate() },
    )
}

private fun List<RawRepo>.restampFavourite(ids: Set<Long>) = map {
    it.copy(isFavourite = it.raw.id in ids)
}

private fun List<RawRepo>.restampStarred(ids: Set<Long>) = map {
    it.copy(isStarred = it.raw.id in ids)
}

private fun RawHomeState.toView(): HomeState {
    val hotVisible = hot.filterVisible(hiddenIds, seenIds, isHideSeenEnabled)
    val leadRaw = hotVisible.firstOrNull()
    val leadCard = leadRaw?.toCard(currentUserLogin, seenIds)
    val hotCards = hotVisible.drop(1).take(6).map { it.toCard(currentUserLogin, seenIds) }.toImmutableList()
    val trendingCards = trending.filterVisible(hiddenIds, seenIds, isHideSeenEnabled).take(6)
        .map { it.toCard(currentUserLogin, seenIds) }.toImmutableList()
    val popularCards = popular.filterVisible(hiddenIds, seenIds, isHideSeenEnabled).take(6)
        .map { it.toCard(currentUserLogin, seenIds) }.toImmutableList()
    val starredCards = starred.filterVisible(hiddenIds, seenIds, isHideSeenEnabled).take(5)
        .map { it.toCard(currentUserLogin, seenIds) }.toImmutableList()
    val actionSheetCard = actionSheetRepoId?.let { id ->
        (hot + trending + popular + starred).firstOrNull { it.raw.id == id }?.toCard(currentUserLogin, seenIds)
    }
    return HomeState(
        lead = leadCard,
        hot = hotCards,
        trending = trendingCards,
        popular = popularCards,
        starred = starredCards,
        isHotLoading = isHotLoading,
        isTrendingLoading = isTrendingLoading,
        isPopularLoading = isPopularLoading,
        isStarredLoading = isStarredLoading,
        errorMessage = errorMessage,
        selectedPlatforms = selectedPlatforms,
        isPlatformPopupVisible = isPlatformPopupVisible,
        isUpdateAvailable = isUpdateAvailable,
        isHideSeenEnabled = isHideSeenEnabled,
        isUserSignedIn = isUserSignedIn,
        actionSheetCard = actionSheetCard,
    )
}

private fun List<RawRepo>.filterVisible(
    hiddenIds: Set<Long>,
    seenIds: Set<Long>,
    hideSeenEnabled: Boolean,
): List<RawRepo> = filter { item ->
    item.raw.id !in hiddenIds && (!hideSeenEnabled || item.raw.id !in seenIds)
}

private fun RawRepo.toCard(currentUserLogin: String?, seenIds: Set<Long>): HomeRepoCardUi =
    toHomeRepoCardUi(
        repo = raw,
        isInstalled = isInstalled,
        isUpdateAvailable = isUpdateAvailable,
        isFavourite = isFavourite,
        isStarred = isStarred,
        isSeen = raw.id in seenIds,
        isCurrentUserOwner = currentUserLogin != null &&
            raw.owner.login.equals(currentUserLogin, ignoreCase = true),
    )
