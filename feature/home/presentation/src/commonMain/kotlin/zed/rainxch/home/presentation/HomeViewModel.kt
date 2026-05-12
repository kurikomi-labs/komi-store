package zed.rainxch.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import zed.rainxch.core.domain.logging.GitHubStoreLogger
import zed.rainxch.core.domain.model.DiscoveryPlatform
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
import zed.rainxch.home.domain.model.HomeCategory
import zed.rainxch.home.domain.model.TopicCategory
import zed.rainxch.home.domain.repository.HomeRepository
import zed.rainxch.home.presentation.HomeEvent.*
import zed.rainxch.profile.domain.repository.ProfileRepository

class HomeViewModel(
    private val homeRepository: HomeRepository,
    private val installedAppsRepository: InstalledAppsRepository,
    private val platform: Platform,
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
    private var currentJob: Job? = null
    private var switchCategoryJob: Job? = null
    private var topicSupplementJob: Job? = null
    private var nextPageIndex = 1

    // Cached so each repo mapping doesn't re-hit ProfileRepository (which
    // walks a suspend cache + DataStore on every call). Refreshed by the
    // observer below — login/logout flips badges without restarting the VM.
    @Volatile private var currentUserLogin: String? = null

    private val _state = MutableStateFlow(HomeState())
    val state =
        _state
            .onStart {
                if (!hasLoadedInitialData) {
                    observeCurrentUser()
                    syncSystemState()

                    loadPlatform()
                    loadRepos(isInitial = true)
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

    private val _events = Channel<HomeEvent>()
    val events = _events.receiveAsFlow()

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

    private fun loadPlatform() {
        _state.update {
            it.copy(isAppsSectionVisible = platform == Platform.ANDROID)
        }
    }

    private fun observeInstalledApps() {
        viewModelScope.launch {
            installedAppsRepository.getAllInstalledApps().collect { installedApps ->
                val installedMap = installedApps.groupBy { it.repoId }
                _state.update { current ->
                    current.copy(
                        repos =
                            current.repos
                                .map { homeRepo ->
                                    val apps = installedMap[homeRepo.repository.id].orEmpty()
                                    homeRepo.copy(
                                        isInstalled = apps.any { it.isReallyInstalled() },
                                        isUpdateAvailable = apps.any { it.hasActualUpdate() },
                                    )
                                }.toImmutableList(),
                        isUpdateAvailable = installedMap.values.flatten().any { it.hasActualUpdate() },
                    )
                }
            }
        }
    }

    private fun observeDiscoveryPlatforms() {
        viewModelScope.launch {
            tweaksRepository.getDiscoveryPlatforms().collect { platforms ->
                _state.update {
                    it.copy(
                        selectedPlatforms = platforms,
                    )
                }
            }
        }
    }

    private fun loadRepos(
        isInitial: Boolean = false,
        category: HomeCategory? = null,
        platforms: Set<DiscoveryPlatform>? = null,
        topics: Set<TopicCategory>? = null,
        topicsExplicitlySet: Boolean = false,
    ): Job? {
        currentJob?.cancel()
        topicSupplementJob?.cancel()

        if (_state.value.isLoading || _state.value.isLoadingMore) {
            logger.debug("Already loading, skipping...")
            return null
        }

        if (isInitial) {
            nextPageIndex = 1
        }

        val targetCategory = category ?: _state.value.currentCategory
        val targetPlatformsDeferred =
            viewModelScope.async {
                tweaksRepository.getDiscoveryPlatforms().first()
            }
        val targetTopics = if (topicsExplicitlySet) topics.orEmpty() else _state.value.selectedTopics

        logger.debug("Loading repos: category=$targetCategory, topics=$targetTopics, page=$nextPageIndex, isInitial=$isInitial")

        return viewModelScope
            .launch {
                val targetPlatforms = platforms ?: targetPlatformsDeferred.await()

                if (platforms != null) {
                    tweaksRepository.setDiscoveryPlatforms(targetPlatforms)
                }

                _state.update {
                    it.copy(
                        isLoading = isInitial,
                        isLoadingMore = !isInitial,
                        errorMessage = null,
                        selectedPlatforms = targetPlatforms,
                        currentCategory = targetCategory,
                        selectedTopics = targetTopics,
                        repos = if (isInitial) persistentListOf() else it.repos,
                    )
                }

                try {
                    val flow =
                        when (targetCategory) {
                            HomeCategory.TRENDING -> {
                                homeRepository.getTrendingRepositories(
                                    platforms = targetPlatforms,
                                    page = nextPageIndex,
                                )
                            }

                            HomeCategory.HOT_RELEASE -> {
                                homeRepository.getHotReleaseRepositories(
                                    platforms = targetPlatforms,
                                    page = nextPageIndex,
                                )
                            }

                            HomeCategory.MOST_POPULAR -> {
                                homeRepository.getMostPopular(
                                    platforms = targetPlatforms,
                                    page = nextPageIndex,
                                )
                            }
                        }

                    flow.collect { paginatedRepos ->
                        logger.debug(
                            "Received ${paginatedRepos.repos.size} repos, hasMore=${paginatedRepos.hasMore}, nextPage=${paginatedRepos.nextPageIndex}",
                        )

                        this@HomeViewModel.nextPageIndex = paginatedRepos.nextPageIndex

                        val repos =
                            if (targetTopics.isEmpty()) {
                                paginatedRepos.repos
                            } else {
                                paginatedRepos.repos.filter { repo ->
                                    targetTopics.any { topic ->
                                        topic.matchesRepo(repo.topics, repo.description, repo.name)
                                    }
                                }
                            }

                        val newReposWithStatus = mapReposToUi(repos)

                        _state.update { currentState ->
                            val rawList = currentState.repos + newReposWithStatus
                            val uniqueList = rawList.distinctBy { it.repository.fullName }

                            currentState.copy(
                                repos = uniqueList.toImmutableList(),
                                hasMorePages = paginatedRepos.hasMore,
                                errorMessage =
                                    if (uniqueList.isEmpty() && !paginatedRepos.hasMore) {
                                        getString(Res.string.no_repositories_found)
                                    } else {
                                        null
                                    },
                            )
                        }
                    }

                    logger.debug("Flow completed")
                    _state.update {
                        it.copy(isLoading = false, isLoadingMore = false)
                    }

                    if (targetTopics.isNotEmpty() && isInitial) {
                        loadTopicSupplement(targetTopics, targetPlatforms)
                    }
                } catch (t: Throwable) {
                    if (t is CancellationException) {
                        logger.debug("Load cancelled (expected)")
                        throw t
                    }

                    logger.error("Load failed: ${t.message}")
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isLoadingMore = false,
                            errorMessage =
                                t.message
                                    ?: getString(Res.string.home_failed_to_load_repositories),
                        )
                    }
                }
            }.also {
                currentJob = it
            }
    }

    private fun loadTopicSupplement(
        topics: Set<TopicCategory>,
        platforms: Set<DiscoveryPlatform>,
    ) {
        topicSupplementJob?.cancel()
        topicSupplementJob =
            viewModelScope.launch {
                _state.update { it.copy(isLoadingTopicSupplement = true) }

                try {
                    // Phase 1: Pre-fetched cached topic repos (instant, no API cost).
                    // Run mirror fetches per selected topic in parallel, merge once.
                    val cachedRepos =
                        coroutineScope {
                            topics.map { topic ->
                                async {
                                    homeRepository
                                        .getTopicRepositories(topic = topic, platforms = platforms)
                                        .firstOrNull()
                                        ?.repos
                                        .orEmpty()
                                }
                            }.awaitAll().flatten()
                        }
                    if (cachedRepos.isNotEmpty()) {
                        val cachedReposWithStatus = mapReposToUi(cachedRepos)
                        _state.update { currentState ->
                            val merged =
                                (currentState.repos + cachedReposWithStatus)
                                    .distinctBy { it.repository.fullName }
                            currentState.copy(repos = merged.toImmutableList())
                        }
                        logger.debug("Loaded ${cachedRepos.size} cached topic repos for $topics")
                    }

                    // Phase 2: Live GitHub search (fills gaps). One search per selected
                    // topic so each topic's keywords AND together correctly inside its
                    // own query, while results from different topics OR via merge.
                    topics.forEach { topic ->
                        homeRepository
                            .searchByTopic(
                                searchKeywords = topic.searchKeywords,
                                platforms = platforms,
                                page = 1,
                            ).collect { paginatedRepos ->
                                val newReposWithStatus = mapReposToUi(paginatedRepos.repos)

                                _state.update { currentState ->
                                    val merged =
                                        (currentState.repos + newReposWithStatus)
                                            .distinctBy { it.repository.fullName }

                                    currentState.copy(
                                        repos = merged.toImmutableList(),
                                        hasMorePages = currentState.hasMorePages || paginatedRepos.hasMore,
                                    )
                                }
                            }
                    }
                } catch (t: Throwable) {
                    if (t is CancellationException) throw t
                    logger.warn("Topic supplement search failed: ${t.message}")
                } finally {
                    _state.update { it.copy(isLoadingTopicSupplement = false) }
                }
            }
    }

    private suspend fun mapReposToUi(repos: List<zed.rainxch.core.domain.model.GithubRepoSummary>): List<DiscoveryRepositoryUi> {
        val installedAppsMap =
            installedAppsRepository
                .getAllInstalledApps()
                .first()
                .groupBy { it.repoId }

        val favoritesMap =
            favouritesRepository
                .getAllFavorites()
                .first()
                .associateBy { it.repoId }

        val starredReposMap =
            starredRepository
                .getAllStarred()
                .first()
                .associateBy { it.repoId }

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

    fun onAction(action: HomeAction) {
        when (action) {
            HomeAction.Refresh -> {
                viewModelScope.launch {
                    syncInstalledAppsUseCase()
                    nextPageIndex = 1
                    loadRepos(isInitial = true)
                }
            }

            HomeAction.Retry -> {
                nextPageIndex = 1
                loadRepos(isInitial = true)
            }

            HomeAction.LoadMore -> {
                logger.debug(
                    "LoadMore action: isLoading=${_state.value.isLoading}, isLoadingMore=${_state.value.isLoadingMore}, hasMore=${_state.value.hasMorePages}",
                )

                if (!_state.value.isLoadingMore && !_state.value.isLoading && _state.value.hasMorePages) {
                    loadRepos(isInitial = false)
                }
            }

            is HomeAction.SwitchTopic -> {
                val current = _state.value.selectedTopics
                val target =
                    if (action.topic in current) current - action.topic else current + action.topic
                if (target != current) {
                    nextPageIndex = 1
                    switchCategoryJob?.cancel()
                    switchCategoryJob =
                        viewModelScope.launch {
                            loadRepos(
                                isInitial = true,
                                topics = target,
                                topicsExplicitlySet = true,
                            )?.join() ?: return@launch
                            _events.send(HomeEvent.OnScrollToListTop)
                        }
                }
            }

            is HomeAction.SwitchCategory -> {
                if (_state.value.currentCategory != action.category) {
                    nextPageIndex = 1
                    switchCategoryJob?.cancel()
                    switchCategoryJob =
                        viewModelScope.launch {
                            loadRepos(isInitial = true, category = action.category)?.join()
                                ?: return@launch
                            _events.send(HomeEvent.OnScrollToListTop)
                        }
                }
            }

            is HomeAction.OnShareClick -> {
                viewModelScope.launch {
                    runCatching {
                        shareManager.shareText("https://github-store.org/app?repo=${action.repo.fullName}")
                    }.onFailure { t ->
                        logger.error("Failed to share link: ${t.message}")
                        _events.send(
                            OnMessage(getString(Res.string.failed_to_share_link)),
                        )
                        return@launch
                    }

                    if (platform != Platform.ANDROID) {
                        _events.send(OnMessage(getString(Res.string.link_copied_to_clipboard)))
                    }
                }
            }

            is HomeAction.TogglePlatform -> {
                val current = _state.value.selectedPlatforms
                val target = current.toggle(action.platform)
                if (target != current) {
                    nextPageIndex = 1
                    switchCategoryJob?.cancel()
                    switchCategoryJob =
                        viewModelScope.launch {
                            loadRepos(isInitial = true, platforms = target)?.join()
                                ?: return@launch
                            _events.send(OnScrollToListTop)
                        }
                }
            }

            HomeAction.OnSelectAllPlatforms -> {
                val target =
                    if (_state.value.selectedPlatforms.isEmpty()) {
                        setOf(devicePlatformAsDiscovery())
                    } else {
                        emptySet()
                    }
                if (target != _state.value.selectedPlatforms) {
                    nextPageIndex = 1
                    switchCategoryJob?.cancel()
                    switchCategoryJob =
                        viewModelScope.launch {
                            loadRepos(isInitial = true, platforms = target)?.join()
                                ?: return@launch
                            _events.send(OnScrollToListTop)
                        }
                }
            }

            HomeAction.OnTogglePlatformPopup -> {
                _state.update {
                    it.copy(
                        isPlatformPopupVisible = !it.isPlatformPopupVisible,
                    )
                }
            }

            is HomeAction.OnRepositoryClick -> {
                // Handled in composable
            }

            is HomeAction.OnRepositoryDeveloperClick -> {
                // Handled in composable
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

            HomeAction.OnSearchClick -> {
                // Handled in composable
            }

            HomeAction.OnSettingsClick -> {
                // Handled in composable
            }

            HomeAction.OnAppsClick -> {
                // Handled in composable
            }
        }
    }

    /**
     * Tap-from-`All` (empty selection) selects only the tapped platform —
     * not "every other platform" — which is what users intuit from the
     * popup. Tapping the only remaining platform deselects it and falls
     * back to the device's own platform so the home feed never ends up
     * empty. Reaching every selectable platform collapses to the `All`
     * representation (empty set) to keep the chip row tidy.
     */
    private fun Set<DiscoveryPlatform>.toggle(platform: DiscoveryPlatform): Set<DiscoveryPlatform> {
        if (platform == DiscoveryPlatform.All) return emptySet()

        if (isEmpty()) return setOf(platform)

        val mutated =
            if (platform in this) this - platform else this + platform

        return when {
            mutated.size == DiscoveryPlatform.selectablePlatforms.size -> emptySet()
            mutated.isEmpty() -> setOf(devicePlatformAsDiscovery())
            else -> mutated
        }
    }

    private fun devicePlatformAsDiscovery(): DiscoveryPlatform =
        when (platform) {
            Platform.ANDROID -> DiscoveryPlatform.Android
            Platform.WINDOWS -> DiscoveryPlatform.Windows
            Platform.MACOS -> DiscoveryPlatform.Macos
            Platform.LINUX -> DiscoveryPlatform.Linux
        }

    private fun observeSeenRepos() {
        viewModelScope.launch {
            seenReposRepository.getAllSeenRepoIds().collect { ids ->
                _state.update { current ->
                    current.copy(
                        seenRepoIds = ids,
                        repos =
                            current.repos
                                .map { repo ->
                                    repo.copy(isSeen = repo.repository.id in ids)
                                }.toImmutableList(),
                    )
                }
            }
        }
    }

    private fun observeHiddenRepos() {
        viewModelScope.launch {
            hiddenReposRepository.getAllHiddenRepoIds().collect { ids ->
                // Track IDs only — mirror the `seenRepoIds` pattern so
                // unhiding restores the repo to the visible list without
                // a refresh. `HomeRoot.visibleRepos` filters at render
                // time using these IDs.
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
                // Re-stamp `isCurrentUserOwner` on already-loaded repos so
                // logging in (or switching accounts) immediately flips
                // badges without forcing a full reload.
                val login = user?.username
                _state.update { current ->
                    current.copy(
                        repos =
                            current.repos
                                .map { repo ->
                                    repo.copy(
                                        isCurrentUserOwner =
                                            login != null &&
                                                repo.repository.owner.login.equals(login, ignoreCase = true),
                                    )
                                }.toImmutableList(),
                    )
                }
            }
        }
    }

    private fun observeFavourites() {
        viewModelScope.launch {
            favouritesRepository.getAllFavorites().collect { favourites ->
                val favouritesMap = favourites.associateBy { it.repoId }
                _state.update { current ->
                    current.copy(
                        repos =
                            current.repos
                                .map { homeRepo ->
                                    homeRepo.copy(
                                        isFavourite = favouritesMap.containsKey(homeRepo.repository.id),
                                    )
                                }.toImmutableList(),
                    )
                }
            }
        }
    }

    private fun observeStarredRepos() {
        viewModelScope.launch {
            starredRepository.getAllStarred().collect { starredRepos ->
                val starredReposById = starredRepos.associateBy { it.repoId }
                _state.update { current ->
                    current.copy(
                        repos =
                            current.repos
                                .map { homeRepo ->
                                    homeRepo.copy(
                                        isStarred = starredReposById.containsKey(homeRepo.repository.id),
                                    )
                                }.toImmutableList(),
                    )
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        currentJob?.cancel()
        topicSupplementJob?.cancel()
    }
}
