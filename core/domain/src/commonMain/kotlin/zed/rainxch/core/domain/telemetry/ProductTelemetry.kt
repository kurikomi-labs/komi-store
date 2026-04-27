package zed.rainxch.core.domain.telemetry

// E6 product-metric telemetry. Distinct from the existing TelemetryRepository
// (which feeds the backend's ranking-signal pipeline at /v1/events). This
// interface targets POST /v1/telemetry/events with anonymous, opt-out
// product-metric events for measuring crash rate, performance buckets,
// import success, etc. Schema is locked — see TelemetryAllowlist.
interface ProductTelemetry {

    // Fire an event. Synchronous from the caller's POV — never throws,
    // never blocks. If consent is denied or not yet asked, the call is
    // a no-op. Props keys must use snake_case and contain no PII.
    fun fire(name: String, props: Map<String, Any?> = emptyMap())

    // Drain the buffer if there's anything pending. Best-effort — fails
    // silently. Used by app shutdown hooks to send a final batch before
    // the process exits.
    suspend fun flush()
}
