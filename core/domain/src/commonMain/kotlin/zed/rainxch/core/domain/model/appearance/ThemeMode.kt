package zed.rainxch.core.domain.model.appearance
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
