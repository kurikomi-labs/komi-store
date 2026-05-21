package zed.rainxch.core.domain.model

enum class AppTheme {
    NORD,
    CREAM,
    FOREST,
    PLUM,
    ;

    companion object {

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
