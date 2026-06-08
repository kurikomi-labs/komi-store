package zed.rainxch.core.domain.system

interface ExternalAppScanner {
    suspend fun isPermissionGranted(): Boolean

    suspend fun visiblePackageCountEstimate(): VisiblePackageEstimate

    suspend fun snapshot(): List<ExternalAppCandidate>

    suspend fun snapshotSingle(packageName: String): ExternalAppCandidate?
}
