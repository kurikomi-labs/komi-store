package zed.rainxch.core.data.mirror

import zed.rainxch.core.domain.model.MirrorConfig
import zed.rainxch.core.domain.model.MirrorStatus
import zed.rainxch.core.domain.model.MirrorType
import zed.rainxch.core.domain.model.TrafficKind

// Offline fallback when the backend's /v1/mirrors/list is unreachable.
// Order + identity must match MirrorPresets.ALL on the backend so the
// client renders the same picker either way. Add an entry here whenever a
// mirror is added/removed on the backend.
object BundledMirrors {
    private val FULL_PROXY_KINDS = setOf(TrafficKind.RELEASE_ASSET, TrafficKind.RAW_FILE)
    private val RAW_FILE_ONLY = setOf(TrafficKind.RAW_FILE)

    val ALL: List<MirrorConfig> =
        listOf(
            entry("direct", "Direct GitHub", null, MirrorType.OFFICIAL),
            entry("ghfast_top", "ghfast.top", "https://ghfast.top/{url}", MirrorType.COMMUNITY),
            entry("moeyy_xyz", "github.moeyy.xyz", "https://github.moeyy.xyz/{url}", MirrorType.COMMUNITY),
            entry("gh_proxy_com", "gh-proxy.com", "https://gh-proxy.com/{url}", MirrorType.COMMUNITY),
            entry("ghps_cc", "ghps.cc", "https://ghps.cc/{url}", MirrorType.COMMUNITY),
            entry(
                "gh_99988866_xyz",
                "gh.api.99988866.xyz",
                "https://gh.api.99988866.xyz/{url}",
                MirrorType.COMMUNITY,
            ),
            // jsDelivr Fastly endpoint — raw_file only. Release tarballs are
            // not served under /gh/, so a client that ignores trafficKinds and
            // sends a release-asset URL here will get a 404. Clients on
            // 1.8.3+ are expected to consult trafficKinds before routing.
            entry(
                "fastly_jsdelivr",
                "fastly.jsdelivr.net",
                "https://fastly.jsdelivr.net/gh/{owner}/{repo}@{ref}/{path}",
                MirrorType.COMMUNITY,
                trafficKinds = RAW_FILE_ONLY,
            ),
        )

    private fun entry(
        id: String,
        name: String,
        template: String?,
        type: MirrorType,
        trafficKinds: Set<TrafficKind> = FULL_PROXY_KINDS,
    ) = MirrorConfig(
        id = id,
        name = name,
        urlTemplate = template,
        type = type,
        status = MirrorStatus.UNKNOWN,
        latencyMs = null,
        lastCheckedAt = null,
        trafficKinds = trafficKinds,
    )
}
