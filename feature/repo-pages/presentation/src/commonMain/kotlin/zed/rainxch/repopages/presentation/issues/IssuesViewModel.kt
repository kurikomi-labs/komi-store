package zed.rainxch.repopages.presentation.issues

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.failed_to_load
import zed.rainxch.repopages.domain.model.IssueState
import zed.rainxch.repopages.domain.repository.RepoPagesRepository

class IssuesViewModel(
    private val owner: String,
    private val repo: String,
    private val repository: RepoPagesRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(IssuesUiState())
    val state = _state.asStateFlow()

    init {
        load(IssueState.OPEN)
    }

    fun retry() = load(_state.value.filter)

    fun setFilter(filter: IssueState) {
        if (_state.value.filter == filter && _state.value.errorMessage == null) return
        load(filter)
    }

    fun loadNextPage() {
        val s = _state.value
        if (s.isLoading || s.isLoadingMore || s.endReached || s.errorMessage != null) return
        loadPage(s.filter, s.page + 1, append = true)
    }

    private fun load(filter: IssueState) {
        _state.update {
            it.copy(
                filter = filter,
                issues = persistentListOf(),
                page = 1,
                endReached = false,
                errorMessage = null,
            )
        }
        loadPage(filter, page = 1, append = false)
    }

    private fun loadPage(
        filter: IssueState,
        page: Int,
        append: Boolean,
    ) {
        viewModelScope.launch {
            _state.update {
                if (append) it.copy(isLoadingMore = true) else it.copy(isLoading = true, errorMessage = null)
            }
            repository.getIssues(owner, repo, filter, page)
                .onSuccess { newIssues ->
                    _state.update { st ->
                        val merged = if (append) st.issues + newIssues else newIssues
                        st.copy(
                            isLoading = false,
                            isLoadingMore = false,
                            issues = merged.toPersistentList(),
                            page = page,
                            endReached = newIssues.size < PER_PAGE,
                            errorMessage = null,
                        )
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isLoadingMore = false,
                            errorMessage = e.message ?: getString(Res.string.failed_to_load),
                        )
                    }
                }
        }
    }

    companion object {
        private const val PER_PAGE = 30
    }
}
