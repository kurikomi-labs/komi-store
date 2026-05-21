package zed.rainxch.githubstore.app.onboarding

import zed.rainxch.core.domain.model.AppTheme
import zed.rainxch.core.domain.model.ThemeMode

sealed interface OnboardingAction {
    data class OnPaletteSelected(
        val palette: AppTheme,
    ) : OnboardingAction

    data class OnModeSelected(
        val mode: ThemeMode,
    ) : OnboardingAction

    data object OnNextClick : OnboardingAction

    data object OnBackClick : OnboardingAction

    data object OnSkipStepClick : OnboardingAction

    data object OnSignInClick : OnboardingAction

    data object OnFinishClick : OnboardingAction
}
