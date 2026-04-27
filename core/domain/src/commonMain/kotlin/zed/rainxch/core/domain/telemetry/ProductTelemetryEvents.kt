package zed.rainxch.core.domain.telemetry

// Locked schema mirror of the backend's TelemetryAllowlist
// (github-store-backend, src/main/kotlin/.../telemetry/TelemetryEvent.kt).
// Names are referenced by callers as constants — never typed as inline
// strings — so a typo is a compile error, not a silently-dropped event.
//
// PrivacyAuditTest pins this set against the backend's; any drift fails CI.
object ProductTelemetryEvents {

    // session
    const val APP_LAUNCHED = "app_launched"
    const val SESSION_DURATION = "session_duration"

    // import (E1 / E2)
    const val IMPORT_SCAN_STARTED = "import_scan_started"
    const val IMPORT_SCAN_COMPLETED = "import_scan_completed"
    const val IMPORT_MATCH_ATTEMPTED = "import_match_attempted"
    const val IMPORT_AUTO_LINKED = "import_auto_linked"
    const val IMPORT_MANUALLY_LINKED = "import_manually_linked"
    const val IMPORT_SKIPPED = "import_skipped"

    // reliability (E3)
    const val CRASH = "crash"
    const val OPERATION_FAILED = "operation_failed"

    // performance (E4)
    const val COLD_START_MS = "cold_start_ms"
    const val FIRST_PAINT_MS = "first_paint_ms"
    const val CACHE_HIT = "cache_hit"
    const val CACHE_MISS = "cache_miss"

    // proxy (E5)
    const val PROXY_CONFIGURED = "proxy_configured"
    const val PROXY_USED = "proxy_used"
    const val MIRROR_USED = "mirror_used"

    // discovery / engagement
    const val UPDATE_INSTALLED = "update_installed"
    const val SEARCH_EXECUTED = "search_executed"
    const val DETAILS_VIEWED = "details_viewed"

    val ALL: Set<String> = setOf(
        APP_LAUNCHED, SESSION_DURATION,
        IMPORT_SCAN_STARTED, IMPORT_SCAN_COMPLETED, IMPORT_MATCH_ATTEMPTED,
        IMPORT_AUTO_LINKED, IMPORT_MANUALLY_LINKED, IMPORT_SKIPPED,
        CRASH, OPERATION_FAILED,
        COLD_START_MS, FIRST_PAINT_MS, CACHE_HIT, CACHE_MISS,
        PROXY_CONFIGURED, PROXY_USED, MIRROR_USED,
        UPDATE_INSTALLED, SEARCH_EXECUTED, DETAILS_VIEWED,
    )
}

// Allowed prop keys for telemetry events. Anything outside this set must not
// appear as a key in props. Categorical / bucketed values only — no IDs,
// names, queries, paths.
object ProductTelemetryProps {

    const val PLATFORM = "platform"
    const val VERSION = "version"
    const val SECONDS = "seconds"
    const val CANDIDATE_COUNT = "candidate_count"
    const val DURATION_MS = "duration_ms"
    const val STRATEGY = "strategy"
    const val CONFIDENCE_BUCKET = "confidence_bucket"
    const val COUNT = "count"
    const val CATEGORY = "category"
    const val OP = "op"
    const val ERROR_CODE = "error_code"
    const val BUCKET = "bucket"
    const val SCREEN = "screen"
    const val CACHE_NAME = "cache_name"
    const val TYPE = "type"
    const val SUCCESS = "success"
    const val PRESET = "preset"
    const val RESULT_COUNT_BUCKET = "result_count_bucket"
    const val FROM = "from"

    val ALLOWED: Set<String> = setOf(
        PLATFORM, VERSION, SECONDS, CANDIDATE_COUNT, DURATION_MS,
        STRATEGY, CONFIDENCE_BUCKET, COUNT, CATEGORY, OP, ERROR_CODE,
        BUCKET, SCREEN, CACHE_NAME, TYPE, SUCCESS, PRESET,
        RESULT_COUNT_BUCKET, FROM,
    )
}
