package zed.rainxch.core.domain.model

enum class ThemeMode {
    LIGHT,
    DARK,
    AMOLED,
    SYSTEM,
    ;

    companion object {
        fun fromName(name: String?): ThemeMode {
            if (name.isNullOrEmpty()) return SYSTEM
            return entries.firstOrNull { it.name == name } ?: SYSTEM
        }
    }
}
