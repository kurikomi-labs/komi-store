package zed.rainxch.core.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class SigningFingerprintSeedResponse(
    val rows: List<Row>,
    val nextCursor: String? = null,
) {
    @Serializable
    data class Row(
        val fingerprint: String,
        val owner: String,
        val repo: String,
        // Epoch milliseconds. Backend contract (E1 plan §7.4); the same value is
        // forwarded as `since` on the next sync — any unit drift between client
        // and backend would show up as either re-fetched pages or a hard skip,
        // which the seed-sync telemetry will surface.
        val observedAt: Long,
    )
}
