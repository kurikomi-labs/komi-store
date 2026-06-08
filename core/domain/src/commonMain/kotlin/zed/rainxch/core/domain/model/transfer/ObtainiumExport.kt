package zed.rainxch.core.domain.model.transfer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ObtainiumExport(
    val apps: List<ObtainiumApp> = emptyList(),
    val settings: JsonElement? = null,
    val overrideExportFormatVersion: Int? = null,
)
