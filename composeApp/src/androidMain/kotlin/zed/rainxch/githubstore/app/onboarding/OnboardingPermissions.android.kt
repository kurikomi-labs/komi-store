package zed.rainxch.githubstore.app.onboarding

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

actual class OnboardingPermissionsController internal constructor(
    private val context: android.content.Context,
    private val notifications: MutableState<Boolean>,
    private val installSources: MutableState<Boolean>,
    private val launchNotifications: () -> Unit,
) {
    actual val notificationsGranted: State<Boolean> = notifications
    actual val installSourcesGranted: State<Boolean> = installSources

    actual fun requestNotifications() {
        // POST_NOTIFICATIONS only requestable runtime on Android 13+. On older
        // releases the permission was install-time, so callers should already
        // see `granted = true` and never hit this branch.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            launchNotifications()
        } else {
            notifications.value = true
        }
    }

    actual fun requestInstallSources() {
        // REQUEST_INSTALL_PACKAGES is a Settings toggle, not a runtime prompt.
        // Deep-link to the per-app screen; the on-resume observer below re-reads
        // `canRequestPackageInstalls()` once the user returns.
        val intent =
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        runCatching { context.startActivity(intent) }
    }

    fun refresh() {
        notifications.value = readNotificationsGranted(context)
        installSources.value = readInstallSourcesGranted(context)
    }
}

@Composable
actual fun rememberOnboardingPermissionsController(): OnboardingPermissionsController {
    val context = LocalContext.current
    val notifications = remember { mutableStateOf(readNotificationsGranted(context)) }
    val installSources = remember { mutableStateOf(readInstallSourcesGranted(context)) }

    val launcher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { granted ->
            notifications.value = granted
        }

    val controller =
        remember {
            OnboardingPermissionsController(
                context = context,
                notifications = notifications,
                installSources = installSources,
                launchNotifications = {
                    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                },
            )
        }

    // Re-read on resume so the install-sources Settings toggle reflects back
    // when the user returns from the OS Settings screen.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) controller.refresh()
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    return controller
}

private fun readNotificationsGranted(context: android.content.Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS,
    ) == PackageManager.PERMISSION_GRANTED
}

private fun readInstallSourcesGranted(context: android.content.Context): Boolean {
    // canRequestPackageInstalls() exists since API 26 (Oreo). GHS minSdk is 26
    // (per top CLAUDE.md), so we don't need to gate on Build.VERSION_CODES.O.
    return context.packageManager.canRequestPackageInstalls()
}
