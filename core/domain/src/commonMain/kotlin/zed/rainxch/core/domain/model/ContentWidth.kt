package zed.rainxch.core.domain.model

enum class ContentWidth {
    COMPACT,
    WIDE,
    EXTRA_WIDE,
    ;

    companion object {
        fun fromName(name: String?): ContentWidth = entries.find { it.name == name } ?: WIDE
    }
}
