package zed.rainxch.core.domain.system

data class VisiblePackageEstimate(
    val visibleCount: Int,
    val invisibleEstimate: Int,
    val permissionGranted: Boolean,
)
