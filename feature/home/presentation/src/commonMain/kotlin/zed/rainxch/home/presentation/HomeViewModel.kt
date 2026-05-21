package zed.rainxch.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
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
import zed.rainxch.core.domain.use_cases.SyncInstalledAppsUseCase
import zed.rainxch.core.domain.utils.ShareManager
import zed.rainxch.core.presentation.model.DiscoveryRepositoryUi
import zed.rainxch.core.presentation.utils.toUi
import zed.rainxch.githubstore.core.presentation.res.*
import zed.rainxch.home.domain.repository.HomeRepository
import zed.rainxch.profile.domain.repository.ProfileRepository

class HomeViewModel(
    private val homeRepository: HomeRepository,
    private val installedAppsRepository: InstalledAppsRepository,
    private val syncInstalledAppsUseCase: SyncInstalledAppsUseCase,
    private val favouritesRepository: FavouritesRepository,
    private val starredRepository: StarredRepository,
    private val logger: GitHubStoreLogger,
    private val shareManager: ShareManager,
    private val tweaksRepository: TweaksRepository,
    private val seenReposRepository: SeenReposRepository,
    private val hiddenReposRepository: HiddenReposRepository,
    private val profileRepository: ProfileRepository,
) : ViewModel() {
    private var hasLoadedInitialData = false
    private var loadJob: Job? = null

    @Volatile private var currentUserLogin: String? = null

    private val _state = MutableStateFlow(HomeState())
    val state =
        _state
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
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = HomeState(),
            )

    private val _events = Channel<HomeEvent>(capacity = Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onAction(action: HomeAction) {
        when (action) {
            HomeAction.OnRefreshClick -> {
                viewModelScope.launch {
                    syncInstalledAppsUseCase()
                    refreshAllSections(isInitial = false)
                }
            }

            HomeAction.OnRetry -> refreshAllSections(isInitial = true)

            HomeAction.OnPlatformPopupOpen -> {
                _state.update { it.copy(isPlatformPopupVisible = true) }
            }

            HomeAction.OnPlatformPopupDismiss -> {
                _state.update { it.copy(isPlatformPopupVisible = false) }
            }

            is HomeAction.OnRepoLongClick -> {
                _state.update { it.copy(actionSheetRepoId = action.repoId) }
            }

            HomeAction.OnActionSheetDismiss -> {
                _state.update { it.copy(actionSheetRepoId = null) }
            }

            is HomeAction.OnShareClick -> {
                viewModelScope.launch {
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
            }

            is HomeAction.OnHideRepository -> {
                val repo = action.repo
                viewModelScope.launch {
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
            }

            is HomeAction.OnUndoHideRepository -> {
                viewModelScope.launch {
                    try {
                        hiddenReposRepository.unhide(action.repoId)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Throwable) {
                        logger.warn("Unhide repository failed for ${action.repoId}: ${e.message}")
                    }
                }
            }

            is HomeAction.OnMarkAsSeen -> {
                val repo = action.repo
                viewModelScope.launch {
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
            }

            is HomeAction.OnMarkAsUnseen -> {
                viewModelScope.launch {
                    try {
                        seenReposRepository.removeFromHistory(action.repoId)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Throwable) {
                        logger.warn("Mark as unseen failed for ${action.repoId}: ${e.message}")
                    }
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
        loadJob =
            viewModelScope.launch {
                val platforms = tweaksRepository.getDiscoveryPlatforms().first()
                _state.update {
                    it.copy(
                        selectedPlatforms = platforms,
                        isHotLoading = true,
                        isTrendingLoading = true,
                        isPopularLoading = true,
                        isStarredLoading = _state.value.isUserSignedIn,
                        errorMessage = null,
                        hot = if (isInitial) persistentListOf() else _state.value.hot,
                        trending = if (isInitial) persistentListOf() else _state.value.trending,
                        popular = if (isInitial) persistentListOf() else _state.value.popular,
                        starred = if (isInitial) persistentListOf() else _state.value.starred,
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
            val mapped = mapReposToUi(page.repos)
            _state.update {
                it.copy(
                    hot = mapped.toImmutableList(),
                    isHotLoading = false,
                )
            }
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
            val mapped = mapReposToUi(page.repos)
            _state.update {
                it.copy(
                    trending = mapped.toImmutableList(),
                    isTrendingLoading = false,
                )
            }
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
            val mapped = mapReposToUi(page.repos)
            _state.update {
                it.copy(
                    popular = mapped.toImmutableList(),
                    isPopularLoading = false,
                )
            }
        } catch (t: CancellationException) {
            throw t
        } catch (t: Throwable) {
            logger.error("Popular section load failed: ${t.message}")
            _state.update { it.copy(isPopularLoading = false) }
        }
    }

    private suspend fun loadStarred() {
        if (!_state.value.isUserSignedIn) {
            _state.update { it.copy(starred = persistentListOf(), isStarredLoading = false) }
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
                topIds
                    .map { id ->
                        async {
                            runCatching { homeRepository.getRepositoryById(id) }.getOrNull()
                        }
                    }
                    .awaitAll()
                    .filterNotNull()
            }
            val mapped = mapReposToUi(fetched)
            _state.update {
                it.copy(
                    starred = mapped.toImmutableList(),
                    isStarredLoading = false,
                )
            }
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
            installedAppsRepository.getAllInstalledApps().collect { installedApps ->
                val installedMap = installedApps.groupBy { it.repoId }
                _state.update { current ->
                    current.copy(
                        hot = current.hot.restamp(installedMap),
                        trending = current.trending.restamp(installedMap),
                        popular = current.popular.restamp(installedMap),
                        starred = current.starred.restamp(installedMap),
                        isUpdateAvailable = installedMap.values.flatten().any { it.hasActualUpdate() },
                    )
                }
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
                _state.update { current ->
                    current.copy(
                        seenRepoIds = ids,
                        hot = current.hot.restampSeen(ids),
                        trending = current.trending.restampSeen(ids),
                        popular = current.popular.restampSeen(ids),
                        starred = current.starred.restampSeen(ids),
                    )
                }
            }
        }
    }

    private fun observeHiddenRepos() {
        viewModelScope.launch {
            hiddenReposRepository.getAllHiddenRepoIds().collect { ids ->
                _state.update { it.copy(hiddenRepoIds = ids) }
            }
        }
    }

    private fun observeHideSeenEnabled() {
        viewModelScope.launch {
            tweaksRepository.getHideSeenEnabled().collect { enabled ->
                _state.update { it.copy(isHideSeenEnabled = enabled) }
            }
        }
    }

    private fun observeCurrentUser() {
        viewModelScope.launch {
            profileRepository.getUser().collect { user ->
                currentUserLogin = user?.username
                val signedIn = user != null
                val previouslySignedIn = _state.value.isUserSignedIn
                val login = user?.username
                _state.update { current ->
                    current.copy(
                        isUserSignedIn = signedIn,
                        hot = current.hot.restampOwner(login),
                        trending = current.trending.restampOwner(login),
                        popular = current.popular.restampOwner(login),
                        starred = current.starred.restampOwner(login),
                    )
                }
                if (signedIn != previouslySignedIn) {
                    if (signedIn) loadStarred() else _state.update {
                        it.copy(starred = persistentListOf(), isStarredLoading = false)
                    }
                }
            }
        }
    }

    private fun observeFavourites() {
        viewModelScope.launch {
            favouritesRepository.getAllFavorites().collect { favourites ->
                val keys = favourites.map { it.repoId }.toSet()
                _state.update { current ->
                    current.copy(
                        hot = current.hot.restampFavourite(keys),
                        trending = current.trending.restampFavourite(keys),
                        popular = current.popular.restampFavourite(keys),
                        starred = current.starred.restampFavourite(keys),
                    )
                }
            }
        }
    }

    private fun observeStarredRepos() {
        viewModelScope.launch {
            starredRepository.getAllStarred().collect { starredRepos ->
                val keys = starredRepos.map { it.repoId }.toSet()
                _state.update { current ->
                    current.copy(
                        hot = current.hot.restampStarred(keys),
                        trending = current.trending.restampStarred(keys),
                        popular = current.popular.restampStarred(keys),
                        starred = current.starred.restampStarred(keys),
                    )
                }
            }
        }
    }

    private suspend fun mapReposToUi(repos: List<GithubRepoSummary>): List<DiscoveryRepositoryUi> {
        val installedAppsMap =
            installedAppsRepository.getAllInstalledApps().first().groupBy { it.repoId }
        val favoritesMap =
            favouritesRepository.getAllFavorites().first().associateBy { it.repoId }
        val starredReposMap =
            starredRepository.getAllStarred().first().associateBy { it.repoId }
        val seenIds = _state.value.seenRepoIds
        val currentLogin = currentUserLogin

        return repos.map { repo ->
            val apps = installedAppsMap[repo.id].orEmpty()
            val favourite = favoritesMap[repo.id]
            val starred = starredReposMap[repo.id]

            DiscoveryRepositoryUi(
                isInstalled = apps.any { it.isReallyInstalled() },
                isFavourite = favourite != null,
                isStarred = starred != null,
                isSeen = repo.id in seenIds,
                isCurrentUserOwner =
                    currentLogin != null &&
                        repo.owner.login.equals(currentLogin, ignoreCase = true),
                isUpdateAvailable = apps.any { it.hasActualUpdate() },
                repository = repo.toUi(),
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        loadJob?.cancel()
    }
}

private fun ImmutableList<DiscoveryRepositoryUi>.restamp(
    installedMap: Map<Long, List<InstalledApp>>,
) = map { repo ->
    val apps = installedMap[repo.repository.id].orEmpty()
    repo.copy(
        isInstalled = apps.any { it.isReallyInstalled() },
        isUpdateAvailable = apps.any { it.hasActualUpdate() },
    )
}.toImmutableList()

private fun ImmutableList<DiscoveryRepositoryUi>.restampSeen(
    ids: Set<Long>,
) = map { it.copy(isSeen = it.repository.id in ids) }.toImmutableList()

private fun ImmutableList<DiscoveryRepositoryUi>.restampFavourite(
    ids: Set<Long>,
) = map { it.copy(isFavourite = it.repository.id in ids) }.toImmutableList()

private fun ImmutableList<DiscoveryRepositoryUi>.restampStarred(
    ids: Set<Long>,
) = map { it.copy(isStarred = it.repository.id in ids) }.toImmutableList()

private fun ImmutableList<DiscoveryRepositoryUi>.restampOwner(
    login: String?,
) = map { repo ->
    repo.copy(
        isCurrentUserOwner =
            login != null && repo.repository.owner.login.equals(login, ignoreCase = true),
    )
}.toImmutableList()

