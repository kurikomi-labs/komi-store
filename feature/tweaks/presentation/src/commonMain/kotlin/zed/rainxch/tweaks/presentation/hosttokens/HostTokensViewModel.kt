package zed.rainxch.tweaks.presentation.hosttokens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import zed.rainxch.core.domain.model.HostNames
import zed.rainxch.core.domain.repository.HostTokenRepository
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.host_tokens_removed
import zed.rainxch.githubstore.core.presentation.res.host_tokens_saved
import zed.rainxch.githubstore.core.presentation.res.host_tokens_validation_failed
import zed.rainxch.githubstore.core.presentation.res.host_tokens_validation_host_invalid
import zed.rainxch.githubstore.core.presentation.res.host_tokens_validation_host_required
import zed.rainxch.githubstore.core.presentation.res.host_tokens_validation_success
import zed.rainxch.githubstore.core.presentation.res.host_tokens_validation_token_required
import zed.rainxch.githubstore.core.presentation.res.host_tokens_validation_token_short

class HostTokensViewModel(
    private val repository: HostTokenRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(HostTokensState(isLoading = true))
    val state = _state.asStateFlow()

    private val _events = Channel<HostTokensEvent>()
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            repository.observeAll().collect { tokens ->
                _state.update { it.copy(tokens = tokens.sortedBy { t -> t.host }, isLoading = false) }
            }
        }
    }

    fun onAction(action: HostTokensAction) {
        when (action) {
            HostTokensAction.OnNavigateBack -> { /* host handles */ }
            HostTokensAction.OnAddClicked ->
                _state.update {
                    it.copy(
                        isAddDialogVisible = true,
                        draftHost = "",
                        draftToken = "",
                        draftDisplayName = "",
                        draftHostError = null,
                        draftTokenError = null,
                        validationMessage = null,
                    )
                }
            HostTokensAction.OnAddDismiss ->
                _state.update { it.copy(isAddDialogVisible = false) }
            is HostTokensAction.OnDraftHostChanged ->
                _state.update {
                    it.copy(
                        draftHost = action.value,
                        draftHostError = validateHost(action.value),
                    )
                }
            is HostTokensAction.OnDraftTokenChanged ->
                _state.update {
                    it.copy(
                        draftToken = action.value,
                        draftTokenError = validateToken(action.value),
                    )
                }
            is HostTokensAction.OnDraftDisplayNameChanged ->
                _state.update { it.copy(draftDisplayName = action.value) }
            HostTokensAction.OnAddConfirm -> persistDraft()
            is HostTokensAction.OnDelete -> deleteHost(action.host)
            is HostTokensAction.OnValidate -> validateExisting(action.host)
        }
    }

    private fun validateHost(value: String): org.jetbrains.compose.resources.StringResource? {
        val normalized = HostNames.normalize(value)
        if (normalized.isEmpty()) return Res.string.host_tokens_validation_host_required
        if (!normalized.contains('.')) return Res.string.host_tokens_validation_host_invalid
        return null
    }

    private fun validateToken(value: String): org.jetbrains.compose.resources.StringResource? {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return Res.string.host_tokens_validation_token_required
        if (trimmed.length < 8) return Res.string.host_tokens_validation_token_short
        return null
    }

    private fun persistDraft() {
        val current = state.value
        val hostError = validateHost(current.draftHost)
        val tokenError = validateToken(current.draftToken)
        if (hostError != null || tokenError != null) {
            _state.update { it.copy(draftHostError = hostError, draftTokenError = tokenError) }
            return
        }
        val host = HostNames.normalize(current.draftHost)
        val token = current.draftToken.trim()
        val displayName = current.draftDisplayName.trim().takeIf { it.isNotEmpty() }
        viewModelScope.launch {
            repository.set(host, token, displayName)
            _state.update {
                it.copy(
                    isAddDialogVisible = false,
                    draftHost = "",
                    draftToken = "",
                    draftDisplayName = "",
                )
            }
            _events.send(HostTokensEvent.Message(getString(Res.string.host_tokens_saved, host)))
        }
    }

    private fun deleteHost(host: String) {
        viewModelScope.launch {
            repository.delete(host)
            _events.send(HostTokensEvent.Message(getString(Res.string.host_tokens_removed, host)))
        }
    }

    private fun validateExisting(host: String) {
        viewModelScope.launch {
            val token = repository.get(host)?.token ?: return@launch
            _state.update { it.copy(isValidating = true, pendingValidationFor = host, validationMessage = null) }
            val result = repository.validate(host, token)
            val message = result.fold(
                onSuccess = { v ->
                    val login = v.login ?: "?"
                    val scopes = if (v.scopes.isEmpty()) "-" else v.scopes.joinToString(",")
                    getString(Res.string.host_tokens_validation_success, login, scopes)
                },
                onFailure = { t ->
                    getString(Res.string.host_tokens_validation_failed, t.message ?: "")
                },
            )
            _state.update { it.copy(isValidating = false, pendingValidationFor = null, validationMessage = message) }
        }
    }
}
