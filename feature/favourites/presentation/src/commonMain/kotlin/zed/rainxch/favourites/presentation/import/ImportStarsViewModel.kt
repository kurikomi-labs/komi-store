@file:OptIn(ExperimentalTime::class)

package zed.rainxch.favourites.presentation.import

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import zed.rainxch.core.domain.model.FavoriteRepo
import zed.rainxch.core.domain.repository.FavouritesRepository
import zed.rainxch.core.domain.repository.StarredRepository
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class ImportStarsViewModel(
    private val starredRepository: StarredRepository,
    private val favouritesRepository: FavouritesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ImportStarsState())
    val state = _state.asStateFlow()

    private val _events = Channel<ImportStarsEvent>(capacity = Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var importJob: Job? = null

    fun onAction(action: ImportStarsAction) {
        when (action) {
            ImportStarsAction.OnNavigateBack -> {
                viewModelScope.launch { _events.send(ImportStarsEvent.NavigateBack) }
            }
            is ImportStarsAction.OnUsernameQueryChange -> {
                _state.update { it.copy(usernameQuery = action.query, errorMessage = null) }
            }
            ImportStarsAction.OnImportClick -> importByUsername()
            ImportStarsAction.OnResetImport -> {
                importJob?.cancel()
                _state.update {
                    it.copy(
                        phase = ImportStarsState.Phase.UsernameInput,
                        candidates = persistentListOf(),
                        importedUsername = null,
                        searchQuery = "",
                        errorMessage = null,
                        isImporting = false,
                        isBulkAdding = false,
                    )
                }
            }
            is ImportStarsAction.OnSearchChange -> {
                _state.update { it.copy(searchQuery = action.query) }
            }
            is ImportStarsAction.OnToggleFavourite -> toggleFavourite(action.candidate)
            ImportStarsAction.OnAddAll -> addAll()
            is ImportStarsAction.OnCandidateClick -> {
                viewModelScope.launch {
                    _events.send(
                        ImportStarsEvent.NavigateToDetails(
                            repoId = action.candidate.repoId,
                            owner = action.candidate.owner,
                            repo = action.candidate.name,
                        ),
                    )
                }
            }
        }
    }

    private fun importByUsername() {
        val username = _state.value.usernameQuery.trim().trimStart('@')
        if (username.isEmpty()) return

        importJob?.cancel()
        importJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    isImporting = true,
                    errorMessage = null,
                )
            }

            val favouritedIds = favouritesRepository.getAllFavorites().first()
                .map { it.repoId }
                .toSet()

            val result = runCatching {
                starredRepository.fetchStarredForUsername(username)
            }.getOrElse { Result.failure(it) }

            result.fold(
                onSuccess = { repos ->
                    val candidates = repos.map { repo ->
                        ImportCandidateUi(
                            repoId = repo.repoId,
                            owner = repo.repoOwner,
                            name = repo.repoName,
                            ownerAvatarUrl = repo.repoOwnerAvatarUrl,
                            description = repo.repoDescription,
                            primaryLanguage = repo.primaryLanguage,
                            repoUrl = repo.repoUrl,
                            stargazersCount = repo.stargazersCount,
                            isAlreadyFavourited = repo.repoId in favouritedIds,
                        )
                    }
                    _state.update {
                        it.copy(
                            isImporting = false,
                            importedUsername = username,
                            candidates = candidates.toImmutableList(),
                            phase = ImportStarsState.Phase.Results,
                        )
                    }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isImporting = false,
                            errorMessage = error.message ?: "Failed to import stars",
                        )
                    }
                },
            )
        }
    }

    private fun toggleFavourite(candidate: ImportCandidateUi) {
        viewModelScope.launch {
            val now = Clock.System.now().toEpochMilliseconds()
            val favourite = FavoriteRepo(
                repoId = candidate.repoId,
                repoName = candidate.name,
                repoOwner = candidate.owner,
                repoOwnerAvatarUrl = candidate.ownerAvatarUrl,
                repoDescription = candidate.description,
                primaryLanguage = candidate.primaryLanguage,
                repoUrl = candidate.repoUrl,
                latestVersion = null,
                latestReleaseUrl = null,
                addedAt = now,
                lastSyncedAt = now,
            )
            favouritesRepository.toggleFavorite(favourite)
            _state.update { current ->
                val updated = current.candidates.map { c ->
                    if (c.repoId == candidate.repoId) {
                        c.copy(isAlreadyFavourited = !candidate.isAlreadyFavourited)
                    } else {
                        c
                    }
                }
                current.copy(candidates = updated.toImmutableList())
            }
        }
    }

    private fun addAll() {
        viewModelScope.launch {
            val pending = _state.value.candidates.filter { !it.isAlreadyFavourited }
            if (pending.isEmpty()) return@launch
            _state.update { it.copy(isBulkAdding = true) }
            val now = Clock.System.now().toEpochMilliseconds()
            for (candidate in pending) {
                val favourite = FavoriteRepo(
                    repoId = candidate.repoId,
                    repoName = candidate.name,
                    repoOwner = candidate.owner,
                    repoOwnerAvatarUrl = candidate.ownerAvatarUrl,
                    repoDescription = candidate.description,
                    primaryLanguage = candidate.primaryLanguage,
                    repoUrl = candidate.repoUrl,
                    latestVersion = null,
                    latestReleaseUrl = null,
                    addedAt = now,
                    lastSyncedAt = now,
                )
                runCatching { favouritesRepository.toggleFavorite(favourite) }
            }
            _state.update { current ->
                val updated = current.candidates.map { c ->
                    if (!c.isAlreadyFavourited) c.copy(isAlreadyFavourited = true) else c
                }
                current.copy(
                    candidates = updated.toImmutableList(),
                    isBulkAdding = false,
                )
            }
        }
    }
}
