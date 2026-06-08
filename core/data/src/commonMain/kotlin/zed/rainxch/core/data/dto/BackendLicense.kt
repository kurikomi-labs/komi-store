package zed.rainxch.core.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class BackendLicense(
    val spdxId: String? = null,
    val name: String? = null,
)
