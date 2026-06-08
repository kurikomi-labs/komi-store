package zed.rainxch.core.domain.model

enum class AnnouncementCategory {
    NEWS,
    PRIVACY,
    SURVEY,
    SECURITY,
    STATUS,
    ;

    val isMutable: Boolean
        get() = this != PRIVACY && this != SECURITY
}
