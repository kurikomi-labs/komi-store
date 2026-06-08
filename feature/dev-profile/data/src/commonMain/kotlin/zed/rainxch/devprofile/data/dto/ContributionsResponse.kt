package zed.rainxch.devprofile.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class ContributionsResponse(
    val total: Map<String, Int> = emptyMap(),
    val contributions: List<ContributionDayResponse> = emptyList(),
)
