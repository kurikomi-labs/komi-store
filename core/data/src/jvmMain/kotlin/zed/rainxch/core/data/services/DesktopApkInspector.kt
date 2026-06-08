package zed.rainxch.core.data.services

import zed.rainxch.core.domain.model.apk.ApkInspection
import zed.rainxch.core.domain.system.ApkInspector

class DesktopApkInspector : ApkInspector {
    override suspend fun inspectFile(filePath: String): ApkInspection? = null

    override suspend fun inspectInstalled(packageName: String): ApkInspection? = null
}
