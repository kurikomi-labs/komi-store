package zed.rainxch.core.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class ExternalMatchResponse(
    val matches: List<MatchEntry>,
) {
    @Serializable
    data class MatchEntry(
        val packageName: String,
        val candidates: List<MatchCandidate>,
    )

    @Serializable
    data class MatchCandidate(
        val owner: String,
        val repo: String,
        val confidence: Double,
        val source: String,
        val stars: Int? = null,
        val description: String? = null,
    )
}
