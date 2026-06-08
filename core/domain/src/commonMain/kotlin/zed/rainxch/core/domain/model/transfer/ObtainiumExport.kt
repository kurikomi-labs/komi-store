package zed.rainxch.core.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ObtainiumExport(
    val apps: List<ObtainiumApp> = emptyList(),
    val settings: JsonElement? = null,
    val overrideExportFormatVersion: Int? = null,
)

@Serializable
data class ObtainiumApp(
    val id: String = "",
    val url: String = "",
    val author: String? = null,
    val name: String? = null,
    val installedVersion: String? = null,
    val latestVersion: String? = null,
    val preferredApkIndex: Int? = null,
    val pinned: Boolean? = null,
    val categories: List<String>? = null,
    @SerialName("additionalSettings")
    val additionalSettingsRaw: JsonElement? = null,
    val overrideSource: String? = null,
)
