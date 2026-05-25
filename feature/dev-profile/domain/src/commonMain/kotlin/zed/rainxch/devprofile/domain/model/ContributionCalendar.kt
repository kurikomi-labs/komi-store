package zed.rainxch.devprofile.domain.model

data class ContributionDay(
    val date: String,
    val count: Int,
    val level: Int,
)

data class ContributionCalendar(
    val totalLastYear: Int,
    val days: List<ContributionDay>,
)
