package zed.rainxch.githubstore.app.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

actual class OnboardingPermissionsController {
    actual val notificationsGranted: State<Boolean> = mutableStateOf(true)
    actual val installSourcesGranted: State<Boolean> = mutableStateOf(true)

    actual fun requestNotifications() = Unit

    actual fun requestInstallSources() = Unit
}

@Composable
actual fun rememberOnboardingPermissionsController(): OnboardingPermissionsController = remember { OnboardingPermissionsController() }
