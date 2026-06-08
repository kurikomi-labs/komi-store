package zed.rainxch.auth.presentation.mapper

import zed.rainxch.auth.presentation.model.GithubDeviceStartUi
import zed.rainxch.core.domain.model.account.github.GithubDeviceStart

fun GithubDeviceStart.toUi(): GithubDeviceStartUi =
    GithubDeviceStartUi(
        deviceCode = deviceCode,
        userCode = userCode,
        verificationUri = verificationUri,
        verificationUriComplete = verificationUriComplete,
        intervalSec = intervalSec,
        expiresInSec = expiresInSec,
    )
