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
            val saveResult = runCatching { repository.set(host, token, displayName) }
            if (saveResult.isSuccess) {
                _state.update {
                    it.copy(
                        isAddDialogVisible = false,
                        draftHost = "",
                        draftToken = "",
                        draftDisplayName = "",
                    )
                }
                _events.send(HostTokensEvent.Message(getString(Res.string.host_tokens_saved, host)))
            } else {
                // Keep the dialog open with the draft intact so the user can
                // retry — the persistence failure (Keystore unavailable,
                // device locked, etc.) should not look like a silent success.
                val msg = saveResult.exceptionOrNull()?.message.orEmpty()
                _events.send(
                    HostTokensEvent.Message(
                        getString(Res.string.host_tokens_validation_failed, msg),
                    ),
                )
            }
        }
    }

    private fun deleteHost(host: String) {
        viewModelScope.launch {
            val result = runCatching { repository.delete(host) }
            if (result.isSuccess) {
                _events.send(
                    HostTokensEvent.Message(getString(Res.string.host_tokens_removed, host)),
                )
            } else {
                val msg = result.exceptionOrNull()?.message.orEmpty()
                _events.send(
                    HostTokensEvent.Message(
                        getString(Res.string.host_tokens_validation_failed, msg),
                    ),
                )
            }
        }
    }

    private fun validateExisting(host: String) {
        // Refuse to start another validation while one is in flight against
        // the same host — concurrent calls would race shared UI flags
        // (`isValidating`, `pendingValidationFor`) and emit stale messages.
        if (state.value.isValidating && state.value.pendingValidationFor == host) return
        viewModelScope.launch {
            val token = runCatching { repository.get(host)?.token }.getOrNull() ?: return@launch
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
            _state.update {
                // Only clear the in-flight flags if THIS host is still the
                // one being shown as pending — defends against a second
                // validation racing in past the guard above (e.g. for a
                // different host).
                if (it.pendingValidationFor == host) {
                    it.copy(isValidating = false, pendingValidationFor = null, validationMessage = message)
                } else {
                    it.copy(validationMessage = message)
                }
            }
        }
    }
}
