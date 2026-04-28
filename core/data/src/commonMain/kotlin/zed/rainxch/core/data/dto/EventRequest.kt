package zed.rainxch.core.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class EventRequest(
    val deviceId: String,
    val platform: String,
    val appVersion: String? = null,
    val eventType: String,
    val repoId: Long? = null,
    val resultCount: Int? = null,
    val success: Boolean? = null,
    val errorCode: String? = null,
    // ── E1 external-import props (all bucketed enums or counts) ──
    val trigger: String? = null,
    val strategy: String? = null,
    val confidenceBucket: String? = null,
    val countBucket: String? = null,
    val candidateCountBucket: String? = null,
    val durationMsBucket: String? = null,
    val rowsAddedBucket: String? = null,
    val statusCodeBucket: String? = null,
    val sdkIntBucket: String? = null,
    val source: String? = null,
    val persisted: String? = null,
    val granted: Boolean? = null,
    val retried: Boolean? = null,
)
