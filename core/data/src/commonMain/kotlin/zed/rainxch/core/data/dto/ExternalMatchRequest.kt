package zed.rainxch.core.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class ExternalMatchRequest(
    val platform: String,
    val candidates: List<RequestItem>,
) {
    @Serializable
    data class RequestItem(
        val packageName: String,
        val appLabel: String,
        val signingFingerprint: String? = null,
        val installerKind: String? = null,
        val manifestHint: ManifestHintDto? = null,
    )

    @Serializable
    data class ManifestHintDto(
        val owner: String,
        val repo: String,
    )
}
