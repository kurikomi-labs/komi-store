package zed.rainxch.core.data.services

import zed.rainxch.core.domain.system.PendingInstallNotifier

class DesktopPendingInstallNotifier : PendingInstallNotifier {
    override fun notifyPending(
        packageName: String,
        repoOwner: String,
        repoName: String,
        appName: String,
        versionTag: String,
    ) = Unit

    override fun clearPending(packageName: String) = Unit
}
