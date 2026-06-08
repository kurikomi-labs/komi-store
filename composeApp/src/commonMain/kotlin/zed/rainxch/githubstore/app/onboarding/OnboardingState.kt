package zed.rainxch.githubstore.app.onboarding

import androidx.compose.runtime.Stable
import zed.rainxch.core.domain.model.appearance.AppTheme
import zed.rainxch.core.domain.model.appearance.ThemeMode

@Stable
data class OnboardingState(
    val steps: List<OnboardingStep> = listOf(OnboardingStep.PALETTE, OnboardingStep.SIGN_IN),
    val currentIndex: Int = 0,
    val selectedPalette: AppTheme = AppTheme.NORD,
    val selectedMode: ThemeMode = ThemeMode.SYSTEM,
    val isAndroid: Boolean = false,
) {
    val currentStep: OnboardingStep get() = steps[currentIndex]
    val isFirst: Boolean get() = currentIndex == 0
    val isLast: Boolean get() = currentIndex == steps.lastIndex
}
