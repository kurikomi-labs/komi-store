package zed.rainxch.devprofile.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class ContributionsResponse(
    val total: Map<String, Int> = emptyMap(),
    val contributions: List<ContributionDayResponse> = emptyList(),
)

@Serializable
data class ContributionDayResponse(
    val date: String,
    val count: Int,
    val level: Int,
)
