package zed.rainxch.core.domain.model.account.github

import zed.rainxch.core.domain.utils.VersionMath

// Only the API flag and the version TAG decide pre-release status. The release `name` is free
// prose ("Stability and beta fixes") and was producing false positives that filtered legitimate
// stable releases out of the update window — i.e. real updates silently never appeared. The tag
// is the canonical version identifier, so it is the reliable secondary signal when a maintainer
// forgets to set the pre-release flag.
fun GithubRelease.isEffectivelyPreRelease(): Boolean =
    isPrerelease ||
        VersionMath.isPreReleaseTag(tagName)

fun GithubRelease.preReleaseLabel(): String? =
    VersionMath.preReleaseMarkerLabel(tagName)
        ?: VersionMath.preReleaseMarkerLabel(name)
