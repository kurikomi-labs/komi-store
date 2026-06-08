package zed.rainxch.core.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LicenseNetwork(
    @SerialName("spdx_id") val spdxId: String? = null,
    val name: String? = null,
)
