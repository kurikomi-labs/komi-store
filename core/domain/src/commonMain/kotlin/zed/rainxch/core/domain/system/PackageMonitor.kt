package zed.rainxch.core.domain.system

import zed.rainxch.core.domain.model.installation.DeviceApp
import zed.rainxch.core.domain.model.installation.SystemPackageInfo

interface PackageMonitor {
    suspend fun isPackageInstalled(packageName: String): Boolean

    suspend fun getInstalledPackageInfo(packageName: String): SystemPackageInfo?

    suspend fun getAllInstalledPackageNames(): Set<String>

    suspend fun getAllInstalledApps(): List<DeviceApp>

    // Whether getAllInstalledPackageNames() returns a trustworthy full enumeration of installed
    // packages on this platform. False for stub monitors (e.g. desktop) where an empty result
    // means "unknown", not "nothing installed". Any reconciliation that deletes tracked rows on
    // absence MUST be gated on this being true.
    fun canEnumerateInstalledPackages(): Boolean
}
