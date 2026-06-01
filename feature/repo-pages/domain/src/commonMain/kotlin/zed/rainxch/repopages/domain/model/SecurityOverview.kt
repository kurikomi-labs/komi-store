package zed.rainxch.repopages.domain.model

data class SecurityOverview(
    val advisories: List<SecurityAdvisory>,
    val securityPolicyMarkdown: String?,
)

data class SecurityAdvisory(
    val ghsaId: String,
    val summary: String,
    val description: String?,
    val severity: AdvisorySeverity,
    val cveId: String?,
    val publishedAt: String?,
    val htmlUrl: String?,
)

enum class AdvisorySeverity {
    CRITICAL,
    HIGH,
    MEDIUM,
    LOW,
    UNKNOWN,
    ;

    companion object {
        fun fromApi(value: String?): AdvisorySeverity = when (value?.lowercase()) {
            "critical" -> CRITICAL
            "high" -> HIGH
            "medium" -> MEDIUM
            "low" -> LOW
            else -> UNKNOWN
        }
    }
}
