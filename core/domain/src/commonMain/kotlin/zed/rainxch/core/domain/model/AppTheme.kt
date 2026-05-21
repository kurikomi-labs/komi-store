package zed.rainxch.core.domain.model

/**
 * Palette identity. Each entry maps 1:1 to a [zed.rainxch.core.presentation.theme.tokens.Tokens.Palette]
 * at the presentation layer. Light / Dark / Amoled are orthogonal — see [ThemeMode].
 *
 * Legacy values from the pre-overhaul enum (DYNAMIC / OCEAN / PURPLE / SLATE / AMBER) are
 * migrated on first read by [fromName] — see [LEGACY_MIGRATION] for the explicit map.
 * Material You dynamic color is intentionally dropped (themes.md "Disallowed combinations").
 */
enum class AppTheme {
    NORD,
    CREAM,
    FOREST,
    PLUM,
    ;

    companion object {
        /** Legacy → new palette mapping. Locked in `.design/DECISIONS.md` D3. */
        private val LEGACY_MIGRATION = mapOf(
            "DYNAMIC" to NORD,
            "OCEAN" to NORD,
            "SLATE" to NORD,
            "PURPLE" to PLUM,
            "AMBER" to CREAM,
        )

        fun fromName(name: String?): AppTheme {
            if (name.isNullOrEmpty()) return NORD
            entries.firstOrNull { it.name == name }?.let { return it }
            LEGACY_MIGRATION[name]?.let { return it }
            return NORD
        }
    }
}
