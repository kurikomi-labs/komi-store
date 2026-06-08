package zed.rainxch.core.domain.model

data class WhatsNewEntry(
    val versionCode: Int,
    val versionName: String,
    val releaseDate: String,
    val sections: List<WhatsNewSection>,
    val showAsSheet: Boolean = true,
)

data class WhatsNewSection(
    val type: WhatsNewSectionType,
    val bullets: List<String>,
)

enum class WhatsNewSectionType {
    NEW,
    IMPROVED,
    FIXED,
    HEADS_UP,
}
