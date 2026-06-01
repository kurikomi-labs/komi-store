package zed.rainxch.repopages.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SecurityAdvisoryDto(
    @SerialName("ghsa_id") val ghsaId: String = "",
    val summary: String = "",
    val description: String? = null,
    val severity: String? = null,
    @SerialName("cve_id") val cveId: String? = null,
    @SerialName("published_at") val publishedAt: String? = null,
    @SerialName("html_url") val htmlUrl: String? = null,
)

@Serializable
data class RepoContentDto(
    val content: String? = null,
    val encoding: String? = null,
)
