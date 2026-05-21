package zed.rainxch.githubstore.app.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State

/**
 * Two-permission controller used by the Onboarding Permissions step (Android only).
 * Wraps the platform's permission APIs so the composable layer stays expect-free.
 *
 *   - [notificationsGranted]: `POST_NOTIFICATIONS` (Android 13+). On older API levels
 *     resolves to `true` immediately — the permission was install-time before T.
 *   - [installSourcesGranted]: `REQUEST_INSTALL_PACKAGES` settings toggle. There is
 *     no runtime prompt on Android — the user has to flip a Settings switch; we
 *     deep-link them there via [requestInstallSources] and re-read on resume.
 *
 * Desktop's actual is a no-op stub: both states report `true`, request methods do
 * nothing. The Permissions step is skipped on Desktop anyway (D17) but the type
 * exists in common so the screen composable can be platform-agnostic.
 */
expect class OnboardingPermissionsController {
    val notificationsGranted: State<Boolean>
    val installSourcesGranted: State<Boolean>

    fun requestNotifications()

    fun requestInstallSources()
}

@Composable
expect fun rememberOnboardingPermissionsController(): OnboardingPermissionsController
