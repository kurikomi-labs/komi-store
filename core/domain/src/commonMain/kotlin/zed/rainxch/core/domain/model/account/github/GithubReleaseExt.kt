package zed.rainxch.core.domain.model

import zed.rainxch.core.domain.util.VersionMath

fun GithubRelease.isEffectivelyPreRelease(): Boolean =
    isPrerelease ||
        VersionMath.isPreReleaseTag(tagName) ||
        VersionMath.isPreReleaseTag(name)

fun GithubRelease.preReleaseLabel(): String? =
    VersionMath.preReleaseMarkerLabel(tagName)
        ?: VersionMath.preReleaseMarkerLabel(name)
