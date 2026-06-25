package zed.rainxch.core.domain.model.appearance

enum class MangaPaperId {
    DAY,
    NIGHT,
    NORD,
    ;

    companion object {
        fun fromName(name: String?): MangaPaperId = entries.firstOrNull { it.name == name } ?: DAY
    }
}
