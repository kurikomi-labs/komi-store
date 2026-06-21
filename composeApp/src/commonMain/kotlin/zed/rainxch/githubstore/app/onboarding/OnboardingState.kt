package zed.rainxch.githubstore.app.onboarding

import androidx.compose.runtime.Stable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import zed.rainxch.core.domain.model.appearance.AppTheme
import zed.rainxch.core.domain.model.appearance.ThemeMode

@Stable
data class OnboardingState(
    val steps: ImmutableList<OnboardingStep> = persistentListOf(),
    val currentIndex: Int = 0,
    val selectedPalette: AppTheme = AppTheme.NORD,
    val selectedMode: ThemeMode = ThemeMode.SYSTEM,
    val isAndroid: Boolean = false,
) {
    val currentStep: OnboardingStep get() = steps[currentIndex]
    val isFirst: Boolean get() = currentIndex == 0
    val isLast: Boolean get() = currentIndex == steps.lastIndex
}
