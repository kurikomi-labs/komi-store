package zed.rainxch.feed.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
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
import zed.rainxch.core.domain.logging.GitHubStoreLogger
import zed.rainxch.core.domain.helpers.ShareManager
import zed.rainxch.core.domain.model.account.github.GithubRepoSummary
import zed.rainxch.core.domain.model.installation.InstalledApp
import zed.rainxch.core.domain.model.installation.hasActualUpdate
import zed.rainxch.core.domain.model.installation.isReallyInstalled
import zed.rainxch.core.domain.model.repository.DiscoveryPlatform
import zed.rainxch.core.domain.repository.FavouritesRepository
import zed.rainxch.core.domain.repository.HiddenReposRepository
import zed.rainxch.core.domain.repository.InstalledAppsRepository
import zed.rainxch.core.domain.repository.SeenReposRepository
import zed.rainxch.core.domain.repository.StarredRepository
import zed.rainxch.core.domain.repository.TweaksRepository
import zed.rainxch.core.domain.repository.UserSessionRepository
import zed.rainxch.core.presentation.model.DiscoveryRepositoryUi
import zed.rainxch.core.presentation.model.GithubRepoSummaryUi
import zed.rainxch.core.presentation.utils.toUi
import zed.rainxch.feed.domain.AffinityProfileBuilder
import zed.rainxch.feed.domain.FeedAffinityScorer
import zed.rainxch.feed.domain.model.AffinityProfile
import zed.rainxch.feed.domain.repository.FeedRepository
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.failed_to_share_link
import zed.rainxch.githubstore.core.presentation.res.for_you_failed_to_load
import zed.rainxch.githubstore.core.presentation.res.link_copied_to_clipboard
import zed.rainxch.core.domain.getPlatform
import zed.rainxch.core.domain.model.system.Platform

class FeedViewModel(
    private val feedRepository: FeedRepository,
    private val affinityProfileBuilder: AffinityProfileBuilder,
    private val installedAppsRepository: InstalledAppsRepository,
    private val favouritesRepository: FavouritesRepository,
    private val starredRepository: StarredRepository,
    private val seenReposRepository: SeenReposRepository,
    private val hiddenReposRepository: HiddenReposRepository,
    private val userSessionRepository: UserSessionRepository,
    private val tweaksRepository: TweaksRepository,
    private val logger: GitHubStoreLogger,
    private val shareManager: ShareManager,
) : ViewModel() {

    private var hasLoadedInitialData = false
    private var loadJob: Job? = null
    private var pageJob: Job? = null

    private var profile: AffinityProfile = AffinityProfile.EMPTY
    private var scoredRepos: List<GithubRepoSummary> = emptyList()
    private var nextPage = 1
    private var hasMore = false
    private var isOffline = false
    private var selectedPlatform = DiscoveryPlatform.All

    private var installedById: Map<Long, List<InstalledApp>> = emptyMap()
    private var favouriteIds: Set<Long> = emptySet()
    private var starredIds: Set<Long> = emptySet()
    private var seenIds: Set<Long> = emptySet()
    private var hiddenIds: Set<Long> = emptySet()
    private var isHideSeenEnabled = false
    private var currentUserLogin: String? = null

    private val _state = MutableStateFlow(FeedState())
    val state: StateFlow<FeedState> = _state
        .onStart {
            if (!hasLoadedInitialData) {
                observeCurrentUser()
                observeInstalledApps()
                observeFavourites()
                observeStarredRepos()
                observeSeenRepos()
                observeHiddenRepos()
                observeHideSeenEnabled()
                reload(isRefresh = false)
                hasLoadedInitialData = true
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = FeedState(),
        )

    private val _events = Channel<FeedEvent>(capacity = Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onAction(action: FeedAction) {
        when (action) {
            FeedAction.OnRefresh -> reload(isRefresh = true)

            FeedAction.OnRetry -> reload(isRefresh = false)

            FeedAction.OnLoadMore -> loadMore()

            is FeedAction.OnPlatformSelected -> {
                if (action.platform == selectedPlatform) return
                selectedPlatform = action.platform
                _state.update { it.copy(selectedPlatform = action.platform) }
                reload(isRefresh = false)
            }

            is FeedAction.OnShareClick -> viewModelScope.launch {
                runCatching {
                    shareManager.shareText("https://github-store.org/app?repo=${action.repo.fullName}")
                }.onFailure { t ->
                    logger.error("Failed to share link: ${t.message}")
                    _events.send(FeedEvent.OnMessage(getString(Res.string.failed_to_share_link)))
                    return@launch
                }
                if (getPlatform() != Platform.ANDROID) {
                    _events.send(FeedEvent.OnMessage(getString(Res.string.link_copied_to_clipboard)))
                }
            }

            is FeedAction.OnHideRepository -> viewModelScope.launch {
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

            is FeedAction.OnMarkAsSeen -> viewModelScope.launch {
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

            is FeedAction.OnMarkAsUnseen -> viewModelScope.launch {
                try {
                    seenReposRepository.removeFromHistory(action.repoId)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    logger.warn("Mark as unseen failed for ${action.repoId}: ${e.message}")
                }
            }

            FeedAction.OnSearchClick,
            FeedAction.OnSettingsClick,
            is FeedAction.OnRepoClick,
            is FeedAction.OnDeveloperClick -> Unit
        }
    }

    private fun reload(isRefresh: Boolean) {
        loadJob?.cancel()
        pageJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    isRefreshing = isRefresh,
                    isLoading = !isRefresh && scoredRepos.isEmpty(),
                    errorMessage = null,
                )
            }
            profile = runCatching { affinityProfileBuilder.build() }.getOrDefault(AffinityProfile.EMPTY)
            loadPage(page = 1, replace = true, forceRefresh = isRefresh)
        }
    }

    private fun loadMore() {
        if (!hasMore || _state.value.isLoadingMore || _state.value.isLoading) return
        pageJob?.cancel()
        pageJob = viewModelScope.launch {
            loadPage(page = nextPage, replace = false, forceRefresh = false)
        }
    }

    private suspend fun loadPage(page: Int, replace: Boolean, forceRefresh: Boolean) {
        if (!replace) _state.update { it.copy(isLoadingMore = true) }

        feedRepository.getFeed(selectedPlatform, page, forceRefresh).fold(
            onSuccess = { feedPage ->
                val seenSnapshot = snapshotSeen()
                val installedSnapshot = snapshotInstalled()
                val scored = FeedAffinityScorer.rank(
                    items = feedPage.items,
                    profile = profile,
                    seenIds = seenSnapshot,
                    installedRepoIds = installedSnapshot,
                )
                scoredRepos = if (replace) {
                    scored
                } else {
                    val existing = scoredRepos.mapTo(HashSet()) { it.id }
                    scoredRepos + scored.filter { it.id !in existing }
                }
                nextPage = feedPage.page + 1
                hasMore = feedPage.hasMore
                isOffline = feedPage.isOffline
                _state.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        errorMessage = null,
                    )
                }
                rebuild()
            },
            onFailure = { error ->
                if (error is CancellationException) throw error
                logger.error("Feed load failed (page $page): ${error.message}")
                _state.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        errorMessage = if (scoredRepos.isEmpty()) {
                            error.message ?: getString(Res.string.for_you_failed_to_load)
                        } else {
                            it.errorMessage
                        },
                    )
                }
            },
        )
    }

    private suspend fun snapshotSeen(): Set<Long> =
        runCatching { seenReposRepository.getAllSeenRepoIds().first() }.getOrDefault(emptySet())

    private suspend fun snapshotInstalled(): Set<Long> =
        runCatching {
            installedAppsRepository.getAllInstalledApps().first()
                .filter { it.isReallyInstalled() }
                .mapTo(HashSet()) { it.repoId }
        }.getOrDefault(emptySet())

    private fun rebuild() {
        val visible = scoredRepos.filter { repo ->
            repo.id !in hiddenIds && (!isHideSeenEnabled || repo.id !in seenIds)
        }
        val cards = visible.map { it.toDiscoveryUi() }.toImmutableList()
        _state.update {
            it.copy(
                repos = cards,
                hasMore = hasMore,
                isOffline = isOffline,
            )
        }
    }

    private fun GithubRepoSummary.toDiscoveryUi(): DiscoveryRepositoryUi {
        val apps = installedById[id].orEmpty()
        return DiscoveryRepositoryUi(
            isInstalled = apps.any { it.isReallyInstalled() },
            isUpdateAvailable = apps.any { it.hasActualUpdate() },
            isFavourite = id in favouriteIds,
            isStarred = id in starredIds,
            isSeen = id in seenIds,
            isCurrentUserOwner = currentUserLogin != null &&
                owner.login.equals(currentUserLogin, ignoreCase = true),
            repository = toUi(),
        )
    }

    private fun observeInstalledApps() {
        viewModelScope.launch {
            installedAppsRepository.getAllInstalledApps().collect { apps ->
                installedById = apps.groupBy { it.repoId }
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
            starredRepository.getAllStarred().collect { starred ->
                starredIds = starred.map { it.repoId }.toSet()
                rebuild()
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
                currentUserLogin = user?.username
                rebuild()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        loadJob?.cancel()
        pageJob?.cancel()
    }
}
