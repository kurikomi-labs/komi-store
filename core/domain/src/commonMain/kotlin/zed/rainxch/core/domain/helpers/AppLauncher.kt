package zed.rainxch.core.domain.helpers

import zed.rainxch.core.domain.model.installation.InstalledApp

interface AppLauncher {
    suspend fun launchApp(installedApp: InstalledApp): Result<Unit>

    suspend fun canLaunchApp(installedApp: InstalledApp): Boolean
}
