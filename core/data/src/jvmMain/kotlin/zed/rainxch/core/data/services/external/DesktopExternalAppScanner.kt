package zed.rainxch.core.data.services.external

import zed.rainxch.core.domain.system.ExternalAppCandidate
import zed.rainxch.core.domain.system.ExternalAppScanner
import zed.rainxch.core.domain.system.VisiblePackageEstimate

class DesktopExternalAppScanner : ExternalAppScanner {
    override suspend fun isPermissionGranted(): Boolean = true

    override suspend fun visiblePackageCountEstimate(): VisiblePackageEstimate =
        VisiblePackageEstimate(
            visibleCount = 0,
            invisibleEstimate = 0,
            permissionGranted = true,
        )

    override suspend fun snapshot(): List<ExternalAppCandidate> = emptyList()

    override suspend fun snapshotSingle(packageName: String): ExternalAppCandidate? = null
}
