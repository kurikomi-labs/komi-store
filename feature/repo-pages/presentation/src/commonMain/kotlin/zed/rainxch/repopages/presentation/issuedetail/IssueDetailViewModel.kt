package zed.rainxch.repopages.presentation.issuedetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.failed_to_load
import zed.rainxch.repopages.domain.repository.RepoPagesRepository

class IssueDetailViewModel(
    private val owner: String,
    private val repo: String,
    private val number: Int,
    private val repository: RepoPagesRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(IssueDetailUiState())
    val state = _state.asStateFlow()

    init {
        load()
    }

    fun retry() = load()

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            repository.getIssueDetail(owner, repo, number)
                .onSuccess { detail ->
                    _state.update { it.copy(isLoading = false, detail = detail, errorMessage = null) }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = e.message ?: getString(Res.string.failed_to_load),
                        )
                    }
                }
        }
    }
}
