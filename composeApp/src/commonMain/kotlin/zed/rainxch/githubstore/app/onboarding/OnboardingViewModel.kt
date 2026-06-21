package zed.rainxch.githubstore.app.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import zed.rainxch.core.domain.isAndroid
import zed.rainxch.core.domain.repository.TweaksRepository

class OnboardingViewModel(
    private val tweaksRepository: TweaksRepository,
) : ViewModel() {
    private val isAndroid = isAndroid()

    private val _state =
        MutableStateFlow(
            OnboardingState(
                steps =
                    if (isAndroid) {
                        persistentListOf(OnboardingStep.THEME, OnboardingStep.PERMISSIONS)
                    } else {
                        persistentListOf(OnboardingStep.THEME)
                    },
                isAndroid = isAndroid,
            ),
        )
    val state = _state.asStateFlow()

    private val _events = Channel<OnboardingEvent>()
    val events = _events.receiveAsFlow()

    fun onAction(action: OnboardingAction) {
        when (action) {
            is OnboardingAction.OnPaletteSelected -> {
                _state.update { it.copy(selectedPalette = action.palette) }
                viewModelScope.launch { tweaksRepository.setThemeColor(action.palette) }
            }

            is OnboardingAction.OnModeSelected -> {
                _state.update { it.copy(selectedMode = action.mode) }
                viewModelScope.launch { tweaksRepository.setThemeMode(action.mode) }
            }

            OnboardingAction.OnNextClick -> {
                advance()
            }

            OnboardingAction.OnBackClick -> {
                retreat()
            }

            OnboardingAction.OnSkipStepClick -> {
                advance()
            }

            OnboardingAction.OnSignInClick -> {
                viewModelScope.launch {
                    _events.send(OnboardingEvent.NavigateToSignIn)
                }
            }

            OnboardingAction.OnFinishClick -> {
                viewModelScope.launch {
                    tweaksRepository.setOnboardingComplete(true)
                    _events.send(OnboardingEvent.NavigateToHome)
                }
            }
        }
    }

    private fun advance() {
        val s = _state.value
        if (s.isLast) {
            onAction(OnboardingAction.OnFinishClick)
        } else {
            _state.update { it.copy(currentIndex = it.currentIndex + 1) }
        }
    }

    private fun retreat() {
        _state.update { if (it.isFirst) it else it.copy(currentIndex = it.currentIndex - 1) }
    }
}
