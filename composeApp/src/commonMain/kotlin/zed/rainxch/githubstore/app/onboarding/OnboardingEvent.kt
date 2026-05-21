package zed.rainxch.githubstore.app.onboarding

sealed interface OnboardingEvent {
    data object NavigateToSignIn : OnboardingEvent

    data object NavigateToHome : OnboardingEvent
}
