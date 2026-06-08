package zed.rainxch.auth.domain.repository

import zed.rainxch.core.domain.model.account.github.GithubDeviceStart

data class DeviceFlowStart(
    val start: GithubDeviceStart,
    val path: AuthPath,
)
