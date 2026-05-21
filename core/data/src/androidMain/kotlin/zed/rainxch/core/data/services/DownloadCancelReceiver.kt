package zed.rainxch.core.data.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext
import zed.rainxch.core.domain.system.DownloadOrchestrator

class DownloadCancelReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_CANCEL) return
        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME).orEmpty()
        if (packageName.isBlank()) {
            Logger.w { "DownloadCancelReceiver: missing package name extra" }
            return
        }

        val pending = goAsync()
        val koin = GlobalContext.getOrNull()
        if (koin == null) {
            Logger.w { "DownloadCancelReceiver: Koin not initialized, ignoring cancel for $packageName" }
            pending.finish()
            return
        }

        val orchestrator = koin.get<DownloadOrchestrator>()
        val scope = koin.get<CoroutineScope>()
        scope.launch {
            try {
                orchestrator.cancel(packageName)
            } catch (t: Throwable) {
                Logger.e(t) { "DownloadCancelReceiver: cancel failed for $packageName" }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_CANCEL = "zed.rainxch.githubstore.action.CANCEL_DOWNLOAD"
        const val EXTRA_PACKAGE_NAME = "zed.rainxch.githubstore.extra.PACKAGE_NAME"
    }
}
