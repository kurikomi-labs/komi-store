package zed.rainxch.auth.domain.repository

data class PollOutcome(
    val result: DevicePollResult,
    val path: AuthPath,
)
