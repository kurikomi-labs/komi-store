package zed.rainxch.core.domain.model.appearance

enum class AppPersonality {
    MANGA,
    CLASSIC,
    ;

    companion object {
        fun fromName(name: String?): AppPersonality = entries.firstOrNull { it.name == name } ?: MANGA
    }
}
