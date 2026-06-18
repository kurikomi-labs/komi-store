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
import androidx.core.net.toUri
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            launchNotifications()
        } else {
            notifications.value = true
        }
    }

    actual fun requestInstallSources() {
        val intent =
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = "package:${context.packageName}".toUri()
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

private fun readInstallSourcesGranted(context: android.content.Context): Boolean = context.packageManager.canRequestPackageInstalls()
