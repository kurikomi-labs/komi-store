package zed.rainxch.apps.presentation.import.util

import android.content.Context
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// keep in sync with AndroidExternalAppScanner.GRANT_THRESHOLD
private const val GRANT_THRESHOLD = 30

@Composable
actual fun rememberPackageVisibilityRequester(): PackageVisibilityRequester {
    val context = LocalContext.current.applicationContext
    return remember(context) { AndroidPackageVisibilityRequester(context) }
}

private class AndroidPackageVisibilityRequester(
    private val context: Context,
) : PackageVisibilityRequester {
    // pm.getInstalledPackages is a binder IPC and can take noticeable time on devices
    // with many packages — keep it off the main thread.
    override suspend fun isGranted(): Boolean =
        withContext(Dispatchers.IO) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return@withContext true
            val pm = context.packageManager
            val visible = runCatching { pm.getInstalledPackages(0) }.getOrElse { emptyList() }
            visible.size >= GRANT_THRESHOLD
        }

    override suspend fun requestOrOpenSettings(): Boolean {
        // No-op by contract. QUERY_ALL_PACKAGES is granted at install time
        // via the manifest declaration — there is no user-grantable runtime
        // toggle on stock Android, and `ACTION_APPLICATION_DETAILS_SETTINGS`
        // does not surface the (non-existent) toggle either. Some OEMs
        // (Samsung One UI 4+) expose a "Special access → Allow access to all
        // apps" setting, but no public Intent reliably deep-links to it.
        // Callers must rely on `isGranted()` and gracefully degrade when
        // false; the scanner's heuristic-based degraded path handles this.
        return false
    }
}
