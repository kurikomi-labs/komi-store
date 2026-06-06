package zed.rainxch.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    private var hotRepos: List<GithubRepoSummary> = emptyList()
    private var trendingRepos: List<GithubRepoSummary> = emptyList()
    private var popularRepos: List<GithubRepoSummary> = emptyList()
    private var starredRepos: List<GithubRepoSummary> = emptyList()

    private var installedById: Map<Long, List<InstalledApp>> = emptyMap()
    private var favouriteIds: Set<Long> = emptySet()
    private var starredIds: Set<Long> = emptySet()
    private var seenIds: Set<Long> = emptySet()
    private var hiddenIds: Set<Long> = emptySet()
    private var isHideSeenEnabled = false
    private var currentUserLogin: String? = null
    private var isUserSignedIn = false
    private var actionSheetRepoId: Long? = null

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state
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
                _state.update { it.copy(isPlatformPopupVisible = true) }

            HomeAction.OnPlatformPopupDismiss ->
                _state.update { it.copy(isPlatformPopupVisible = false) }

            is HomeAction.OnRepoLongClick -> {
                actionSheetRepoId = action.repoId
                rebuild()
            }

            HomeAction.OnActionSheetDismiss -> {
                actionSheetRepoId = null
                rebuild()
            }

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
            HomeAction.OnSeeAllStarred -> Unit
        }
    }

    private fun rebuild() {
        val hotVisible = hotRepos.filterVisible()
        val lead = hotVisible.firstOrNull()?.toCard()
        val hotCards = hotVisible.drop(1).take(6).map { it.toCard() }.toImmutableList()
        val trendingCards = trendingRepos.filterVisible().take(6).map { it.toCard() }.toImmutableList()
        val popularCards = popularRepos.filterVisible().take(6).map { it.toCard() }.toImmutableList()
        val starredCards = starredRepos.filterVisible().take(5).map { it.toCard() }.toImmutableList()
        val sheetCard = actionSheetRepoId?.let { id ->
            (hotRepos + trendingRepos + popularRepos + starredRepos)
                .firstOrNull { it.id == id }
                ?.toCard()
        }

        _state.update {
            it.copy(
                lead = lead,
                hot = hotCards,
                trending = trendingCards,
                popular = popularCards,
                starred = starredCards,
                actionSheetCard = sheetCard,
                isUserSignedIn = isUserSignedIn,
            )
        }
    }

    private fun List<GithubRepoSummary>.filterVisible(): List<GithubRepoSummary> =
        filter { repo ->
            repo.id !in hiddenIds && (!isHideSeenEnabled || repo.id !in seenIds)
        }

    private fun GithubRepoSummary.toCard(): HomeRepoCardUi {
        val apps = installedById[id].orEmpty()
        return toHomeRepoCardUi(
            repo = this,
            isInstalled = apps.any { it.isReallyInstalled() },
            isUpdateAvailable = apps.any { it.hasActualUpdate() },
            isFavourite = id in favouriteIds,
            isStarred = id in starredIds,
            isSeen = id in seenIds,
            isCurrentUserOwner = currentUserLogin != null &&
                owner.login.equals(currentUserLogin, ignoreCase = true),
        )
    }

    private fun refreshAllSections(isInitial: Boolean) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val platforms = tweaksRepository.getDiscoveryPlatforms().first()
            _state.update {
                it.copy(
                    selectedPlatforms = platforms,
                    isHotLoading = true,
                    isTrendingLoading = true,
                    isPopularLoading = true,
                    isStarredLoading = isUserSignedIn,
                    errorMessage = null,
                )
            }

            if (isInitial) {
                hotRepos = emptyList()
                trendingRepos = emptyList()
                popularRepos = emptyList()
                starredRepos = emptyList()
                rebuild()
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
            hotRepos = page.repos
            _state.update { it.copy(isHotLoading = false) }
            rebuild()
        } catch (t: CancellationException) {
            throw t
        } catch (t: Throwable) {
            logger.error("Hot section load failed: ${t.message}")
            _state.update {
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
            trendingRepos = page.repos
            _state.update { it.copy(isTrendingLoading = false) }
            rebuild()
        } catch (t: CancellationException) {
            throw t
        } catch (t: Throwable) {
            logger.error("Trending section load failed: ${t.message}")
            _state.update { it.copy(isTrendingLoading = false) }
        }
    }

    private suspend fun loadPopular(platforms: Set<DiscoveryPlatform>) {
        try {
            val page = homeRepository.getMostPopular(platforms, page = 1).first()
            popularRepos = page.repos
            _state.update { it.copy(isPopularLoading = false) }
            rebuild()
        } catch (t: CancellationException) {
            throw t
        } catch (t: Throwable) {
            logger.error("Popular section load failed: ${t.message}")
            _state.update { it.copy(isPopularLoading = false) }
        }
    }

    private suspend fun loadStarred() {
        if (!isUserSignedIn) {
            starredRepos = emptyList()
            _state.update { it.copy(isStarredLoading = false) }
            rebuild()
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
            starredRepos = fetched
            _state.update { it.copy(isStarredLoading = false) }
            rebuild()
        } catch (t: CancellationException) {
            throw t
        } catch (t: Throwable) {
            logger.error("Starred section load failed: ${t.message}")
            _state.update { it.copy(isStarredLoading = false) }
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
                installedById = apps.groupBy { it.repoId }
                _state.update { it.copy(isUpdateAvailable = apps.any { app -> app.hasActualUpdate() }) }
                rebuild()
            }
        }
    }

    private fun observeDiscoveryPlatforms() {
        viewModelScope.launch {
            tweaksRepository.getDiscoveryPlatforms().collect { platforms ->
                _state.update { it.copy(selectedPlatforms = platforms) }
            }
        }
    }

    private fun observeSeenRepos() {
        viewModelScope.launch {
            seenReposRepository.getAllSeenRepoIds().collect { ids ->
                seenIds = ids
                rebuild()
            }
        }
    }

    private fun observeHiddenRepos() {
        viewModelScope.launch {
            hiddenReposRepository.getAllHiddenRepoIds().collect { ids ->
                hiddenIds = ids
                rebuild()
            }
        }
    }

    private fun observeHideSeenEnabled() {
        viewModelScope.launch {
            tweaksRepository.getHideSeenEnabled().collect { enabled ->
                isHideSeenEnabled = enabled
                rebuild()
            }
        }
    }

    private fun observeCurrentUser() {
        viewModelScope.launch {
            userSessionRepository.getUser().collect { user ->
                val signedIn = user != null
                val previouslySignedIn = isUserSignedIn
                isUserSignedIn = signedIn
                currentUserLogin = user?.username

                if (signedIn != previouslySignedIn) {
                    if (signedIn) {
                        loadStarred()
                    } else {
                        starredRepos = emptyList()
                        _state.update { it.copy(isStarredLoading = false) }
                    }
                }

                rebuild()
            }
        }
    }

    private fun observeFavourites() {
        viewModelScope.launch {
            favouritesRepository.getAllFavorites().collect { favourites ->
                favouriteIds = favourites.map { it.repoId }.toSet()
                rebuild()
            }
        }
    }

    private fun observeStarredRepos() {
        viewModelScope.launch {
            starredRepository.getAllStarred().collect { starredRepos ->
                starredIds = starredRepos.map { it.repoId }.toSet()
                rebuild()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        loadJob?.cancel()
    }
}
