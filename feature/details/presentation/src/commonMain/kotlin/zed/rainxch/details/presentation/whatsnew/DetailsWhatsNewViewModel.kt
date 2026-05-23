package zed.rainxch.details.presentation.whatsnew

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import zed.rainxch.details.domain.repository.DetailsRepository

class DetailsWhatsNewViewModel(
    private val repositoryId: Long,
    private val owner: String,
    private val repo: String,
    private val sourceHost: String?,
    private val detailsRepository: DetailsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(DetailsWhatsNewState())
    val state = _state.asStateFlow()

    init {
        load()
    }

    fun retry() {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                val resolved = if (owner.isNotBlank() && repo.isNotBlank()) {
                    detailsRepository.getRepositoryByOwnerAndName(owner, repo, sourceHost)
                } else {
                    detailsRepository.getRepositoryById(repositoryId)
                }
                val releases = detailsRepository.getAllReleases(
                    owner = resolved.owner.login,
                    repo = resolved.name,
                    defaultBranch = resolved.defaultBranch,
                    sourceHost = sourceHost,
                )
                resolved to releases
            }.onSuccess { (resolved, releases) ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        repoName = resolved.name,
                        releases = releases,
                    )
                }
            }.onFailure { e ->
                _state.update { it.copy(isLoading = false, errorMessage = e.message ?: "Failed to load") }
            }
        }
    }
}
