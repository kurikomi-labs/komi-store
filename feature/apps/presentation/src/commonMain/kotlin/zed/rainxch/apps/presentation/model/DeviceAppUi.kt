package zed.rainxch.apps.presentation.model

import zed.rainxch.core.domain.model.installation.InstallerCategory

data class DeviceAppUi(
    val packageName: String,
    val appName: String,
    val versionName: String?,
    val versionCode: Long,
    val signingFingerprint: String?,
    val installerPackageName: String? = null,
    val isUpdatedSystemApp: Boolean = false,
) {
    val installerCategory: InstallerCategory
        get() = InstallerCategory.classify(installerPackageName, isUpdatedSystemApp, packageName)
}
