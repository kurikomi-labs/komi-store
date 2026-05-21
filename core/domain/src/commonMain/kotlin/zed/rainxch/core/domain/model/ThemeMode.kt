package zed.rainxch.core.domain.model

/**
 * Light / Dark / Amoled / System — orthogonal to [AppTheme] palette. AMOLED is a Dark
 * sub-mode (pure-black background); falls back to DARK rendering when SYSTEM resolves
 * to dark and AMOLED preference is enabled separately.
 */
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
