package zed.rainxch.githubstore.app.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State

expect class OnboardingPermissionsController {
    val notificationsGranted: State<Boolean>
    val installSourcesGranted: State<Boolean>

    fun requestNotifications()

    fun requestInstallSources()
}

@Composable
expect fun rememberOnboardingPermissionsController(): OnboardingPermissionsController
