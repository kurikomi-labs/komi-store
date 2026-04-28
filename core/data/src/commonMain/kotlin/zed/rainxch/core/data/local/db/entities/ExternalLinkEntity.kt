package zed.rainxch.core.data.local.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "external_links",
    indices = [Index(value = ["repoOwner", "repoName"])],
)
data class ExternalLinkEntity(
    @PrimaryKey val packageName: String,
    val state: String,
    val repoOwner: String?,
    val repoName: String?,
    val matchSource: String?,
    val matchConfidence: Double?,
    val signingFingerprint: String?,
    val installerKind: String?,
    val firstSeenAt: Long,
    val lastReviewedAt: Long,
    val skipExpiresAt: Long?,
)
