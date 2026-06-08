package zed.rainxch.devprofile.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class ContributionDayResponse(
    val date: String,
    val count: Int,
    val level: Int,
)
