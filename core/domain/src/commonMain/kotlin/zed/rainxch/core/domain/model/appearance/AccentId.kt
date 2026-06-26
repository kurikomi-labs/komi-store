package zed.rainxch.core.domain.model.appearance

enum class AccentId {
    MONO,
    CRIMSON,
    COBALT,
    SUN,
    FROST,
    ;

    companion object {
        fun fromName(name: String?): AccentId = entries.firstOrNull { it.name == name } ?: CRIMSON
    }
}
