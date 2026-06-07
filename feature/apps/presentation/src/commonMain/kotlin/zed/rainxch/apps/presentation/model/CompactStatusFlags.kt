package zed.rainxch.apps.presentation.model

data class CompactStatusFlags(
    val filterActive: Boolean = false,
    val variantPinned: Boolean = false,
    val variantStale: Boolean = false,
    val preReleaseOn: Boolean = false,
    val pendingInstall: Boolean = false,
    val readyToInstall: Boolean = false,
    val updatesIgnored: Boolean = false,
)
