package zed.rainxch.core.domain.model.error

enum class RefreshError {
    COOLDOWN,
    BUDGET_EXHAUSTED,
    ARCHIVED,
    NOT_FOUND,
    UPSTREAM,
    GENERIC,
}
