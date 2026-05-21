package zed.rainxch.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import zed.rainxch.core.domain.repository.UserSessionRepository

class ProfileViewModel(
    private val userSessionRepository: UserSessionRepository
) : ViewModel() {
    private var userProfileJob: Job? = null

    private var hasLoadedInitialData = false

    private val _state = MutableStateFlow(ProfileState())
    val state =
        _state
            .onStart {
                if (!hasLoadedInitialData) {
                    observeLoggedInStatus()

                    hasLoadedInitialData = true
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = ProfileState(),
            )

    private val _events = Channel<ProfileEvent>(capacity = Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private fun formatCacheSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        var size = bytes.toDouble()
        var unitIndex = 0
        while (size >= 1024 && unitIndex < units.lastIndex) {
            size /= 1024
            unitIndex++
        }
        return if (size == size.toLong().toDouble()) {
            "${size.toLong()} ${units[unitIndex]}"
        } else {
            "${"%.1f".format(size)} ${units[unitIndex]}"
        }
    }

    private fun observeLoggedInStatus() {
        viewModelScope.launch {
            userSessionRepository.isUserLoggedIn()
                .collect { isLoggedIn ->
                    _state.update { it.copy(isUserLoggedIn = isLoggedIn) }
                    if (isLoggedIn) {
                        loadUserProfile()
                    } else {
                        _state.update { it.copy(userProfile = null) }
                    }
                }
        }
    }

    private fun loadUserProfile() {
        userProfileJob?.cancel()

        userProfileJob =
            viewModelScope.launch {
                userSessionRepository.getUser().collect { profile ->
                    _state.update { it.copy(userProfile = profile) }
                }
            }
    }

    fun onAction(action: ProfileAction) {
        when (action) {
            ProfileAction.OnLogoutClick -> {
                _state.update {
                    it.copy(
                        isLogoutDialogVisible = true,
                    )
                }
            }

            ProfileAction.OnLogoutConfirmClick -> {
                viewModelScope.launch {
                    runCatching {
                        userSessionRepository.logout()
                    }.onSuccess {
                        _state.update { it.copy(isLogoutDialogVisible = false, userProfile = null) }
                        _events.send(ProfileEvent.OnLogoutSuccessful)
                    }.onFailure { error ->
                        _state.update { it.copy(isLogoutDialogVisible = false) }
                        error.message?.let {
                            _events.send(ProfileEvent.OnLogoutError(it))
                        }
                    }
                }
            }

            ProfileAction.OnLogoutDismiss -> {
                _state.update {
                    it.copy(
                        isLogoutDialogVisible = false,
                    )
                }
            }

            ProfileAction.OnLoginClick -> {

            }

            ProfileAction.OnFavouriteReposClick -> {

            }

            ProfileAction.OnStarredReposClick -> {

            }

            is ProfileAction.OnRepositoriesClick -> {

            }

            ProfileAction.OnRecentlyViewedClick -> {

            }

            ProfileAction.OnWhatsNewClick -> {

            }

            ProfileAction.OnWhatsNewLongClick -> {

            }

            ProfileAction.OnAnnouncementsClick -> {

            }

            ProfileAction.OnAnnouncementsLongClick -> {

            }
        }
    }
}
