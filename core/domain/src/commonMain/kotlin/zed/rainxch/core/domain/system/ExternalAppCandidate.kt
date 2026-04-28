package zed.rainxch.core.domain.system

data class ExternalAppCandidate(
    val packageName: String,
    val appLabel: String,
    val versionName: String?,
    val versionCode: Long,
    val signingFingerprint: String?,
    val installerKind: InstallerKind,
    val manifestHint: ManifestHint?,
    val firstSeenAt: Long,
)
