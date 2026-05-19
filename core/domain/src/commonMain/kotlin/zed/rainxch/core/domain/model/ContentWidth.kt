package zed.rainxch.core.domain.model

enum class ContentWidth(
    val displayName: String,
) {
    COMPACT("Compact"),
    WIDE("Wide"),
    EXTRA_WIDE("Extra wide"),
    ;

    companion object {
        fun fromName(name: String?): ContentWidth = entries.find { it.name == name } ?: WIDE
    }
}
