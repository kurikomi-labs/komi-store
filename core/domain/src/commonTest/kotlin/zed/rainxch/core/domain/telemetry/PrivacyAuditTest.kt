package zed.rainxch.core.domain.telemetry

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class PrivacyAuditTest {

    // Hardcoded mirror of the backend's TelemetryAllowlist. Drift between
    // client and server here means the server silently drops events the
    // client thinks it's sending — fail loudly on mismatch.
    @Test
    fun `event allowlist matches the backend's locked schema`() {
        val backendAllowlist = setOf(
            "app_launched", "session_duration",
            "import_scan_started", "import_scan_completed", "import_match_attempted",
            "import_auto_linked", "import_manually_linked", "import_skipped",
            "crash", "operation_failed",
            "cold_start_ms", "first_paint_ms", "cache_hit", "cache_miss",
            "proxy_configured", "proxy_used", "mirror_used",
            "update_installed", "search_executed", "details_viewed",
        )
        assertEquals(backendAllowlist, ProductTelemetryEvents.ALL)
    }

    @Test
    fun `no PII-shaped names slip into the allowlist`() {
        val forbidden = listOf(
            "user_id", "email", "search_query", "repo_name", "github_username",
            "device_id", "ip_address", "owner", "name", "query", "username",
        )
        forbidden.forEach { name ->
            assertFalse(name in ProductTelemetryEvents.ALL, "$name leaked into ALL")
            assertFalse(name in ProductTelemetryProps.ALLOWED, "$name leaked into ALLOWED props")
        }
    }

    @Test
    fun `every prop key is short and snake_case`() {
        ProductTelemetryProps.ALLOWED.forEach { key ->
            assertFalse(key.contains(" "), "prop key '$key' has whitespace")
            assertFalse(key.any { it.isUpperCase() }, "prop key '$key' has uppercase")
            check(key.length <= 32) { "prop key '$key' exceeds 32-char cap" }
        }
    }
}
