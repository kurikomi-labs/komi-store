package zed.rainxch.core.domain.model

import kotlin.time.Instant

data class Announcement(
    val id: String,
    val publishedAt: Instant,
    val expiresAt: Instant?,
    val severity: AnnouncementSeverity,
    val category: AnnouncementCategory,
    val title: String,
    val body: String,
    val ctaUrl: String?,
    val ctaLabel: String?,
    val dismissible: Boolean,
    val requiresAcknowledgment: Boolean,
    val minVersionCode: Int?,
    val maxVersionCode: Int?,
    val platforms: Set<String>?,
    val installerTypes: Set<String>?,
    val iconHint: AnnouncementIconHint?,
)

enum class AnnouncementIconHint {
    INFO,
    WARNING,
    SECURITY,
    CELEBRATION,
    CHANGE,
}
