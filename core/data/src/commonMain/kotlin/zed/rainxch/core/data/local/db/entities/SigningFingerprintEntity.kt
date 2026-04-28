package zed.rainxch.core.data.local.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "signing_fingerprints",
    indices = [Index(value = ["repoOwner", "repoName"])],
)
data class SigningFingerprintEntity(
    @PrimaryKey val fingerprint: String,
    val repoOwner: String,
    val repoName: String,
    val source: String,
    val observedAt: Long,
)
