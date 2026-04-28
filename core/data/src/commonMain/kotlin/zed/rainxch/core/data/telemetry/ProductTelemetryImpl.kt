package zed.rainxch.core.data.telemetry

import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Clock
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import zed.rainxch.core.data.BuildKonfig
import zed.rainxch.core.data.dto.ProductTelemetryEventBody
import zed.rainxch.core.data.network.BackendApiClient
import zed.rainxch.core.domain.logging.GitHubStoreLogger
import zed.rainxch.core.domain.model.Platform
import zed.rainxch.core.domain.repository.TweaksRepository
import zed.rainxch.core.domain.telemetry.ProductTelemetry
import zed.rainxch.core.domain.telemetry.ProductTelemetryConsent
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.random.Random

@OptIn(ExperimentalEncodingApi::class)
class ProductTelemetryImpl(
    // Lazy-resolved to break the BackendApiClient ↔ ProductTelemetry cycle
    // in Koin: BackendApiClient injects ProductTelemetry for PROXY_USED
    // emission, and we need BackendApiClient to ship batches. Both are
    // singletons, so deferring resolution to first use lets Koin construct
    // either one first.
    private val backendApiClientProvider: () -> BackendApiClient,
    private val tweaksRepository: TweaksRepository,
    private val platform: Platform,
    private val appScope: CoroutineScope,
    private val logger: GitHubStoreLogger,
) : ProductTelemetry {

    private val backendApiClient: BackendApiClient by lazy(backendApiClientProvider)

    // Fresh per process. The contract from E6 is "ephemeral, reset per app
    // launch" — the JVM/Android process lifecycle scopes that for us.
    private val sessionId: String = Base64.UrlSafe
        .encode(Random.nextBytes(16))
        .trimEnd('=')

    private val bufferMutex = Mutex()
    private val buffer = ArrayDeque<ProductTelemetryEventBody>()

    @Volatile
    private var backoffMs: Long = INITIAL_BACKOFF_MS

    init {
        appScope.launch {
            while (true) {
                delay(FLUSH_INTERVAL_MS)
                runCatching { flushInternal() }
                    .onFailure {
                        if (it is CancellationException) throw it
                        logger.debug("Product telemetry flush error: ${it.message}")
                    }
            }
        }
    }

    override fun fire(name: String, props: Map<String, Any?>) {
        appScope.launch {
            // Consent check happens here, BEFORE serialization or queue insert.
            // PrivacyAuditTest leans on this — a test that mocks the queue and
            // asserts zero enqueue calls when consent is not Granted.
            if (consent() != ProductTelemetryConsent.Granted) return@launch

            val body = ProductTelemetryEventBody(
                name = name,
                sessionId = sessionId,
                timestamp = nowMs(),
                platform = platformSlug(),
                appVersion = BuildKonfig.VERSION_NAME,
                props = props.toJsonObject(),
            )
            val shouldFlush = bufferMutex.withLock {
                if (buffer.size >= MAX_BUFFER_SIZE) buffer.removeFirst()
                buffer.add(body)
                buffer.size >= FLUSH_BATCH_THRESHOLD
            }
            if (shouldFlush) {
                appScope.launch { flushInternal() }
            }
        }
    }

    override suspend fun flush() {
        // Best-effort with a short timeout — used by app shutdown hooks.
        withTimeoutOrNull(SHUTDOWN_FLUSH_TIMEOUT_MS) {
            flushInternal()
        }
    }

    private suspend fun flushInternal() {
        if (consent() != ProductTelemetryConsent.Granted) {
            // Consent withdrawn between enqueue and flush. Drop everything in
            // flight; never let a stale-consent batch leave the device.
            bufferMutex.withLock { buffer.clear() }
            return
        }

        val pending = bufferMutex.withLock {
            if (buffer.isEmpty()) return
            val take = minOf(buffer.size, MAX_BATCH_SIZE)
            (0 until take).map { buffer.removeFirst() }
        }

        val result = withContext(Dispatchers.IO) {
            backendApiClient.postProductTelemetryEvents(pending)
        }

        if (result.isSuccess) {
            backoffMs = INITIAL_BACKOFF_MS
        } else {
            // Network or 5xx. Re-add at the front for retry next tick, but only
            // if consent still holds. Bound the buffer so a long offline session
            // doesn't grow unbounded.
            if (consent() == ProductTelemetryConsent.Granted) {
                bufferMutex.withLock {
                    for (i in pending.indices.reversed()) {
                        if (buffer.size < MAX_BUFFER_SIZE) buffer.addFirst(pending[i])
                    }
                }
                // Exponential backoff up to MAX_BACKOFF_MS. The fixed-cadence
                // flush loop still runs every FLUSH_INTERVAL_MS; backoff just
                // makes us skip flushes while it's elevated.
                backoffMs = (backoffMs * 2).coerceAtMost(MAX_BACKOFF_MS)
                delay(backoffMs)
            } else {
                bufferMutex.withLock { buffer.clear() }
            }
            logger.debug("Product telemetry batch failed: ${result.exceptionOrNull()?.message}")
        }
    }

    private suspend fun consent(): ProductTelemetryConsent =
        runCatching { tweaksRepository.getProductTelemetryConsent().first() }
            .getOrDefault(ProductTelemetryConsent.NotYetAsked)

    private fun platformSlug(): String = when (platform) {
        Platform.ANDROID -> "android"
        Platform.MACOS -> "macos"
        Platform.WINDOWS -> "windows"
        Platform.LINUX -> "linux"
    }

    private fun Map<String, Any?>.toJsonObject(): JsonObject? {
        if (isEmpty()) return null
        // Only categorical / bucketed values survive — anything that isn't a
        // String, Number, or Boolean is dropped. Defense against accidentally
        // attaching a repo object, an exception, or a query string.
        val entries = mutableMapOf<String, kotlinx.serialization.json.JsonElement>()
        for ((k, v) in this) {
            when (v) {
                is String -> entries[k] = JsonPrimitive(v)
                is Number -> entries[k] = JsonPrimitive(v)
                is Boolean -> entries[k] = JsonPrimitive(v)
                null -> {} // skip nulls
                else -> logger.debug("Product telemetry: dropping non-primitive prop '$k' (${v::class.simpleName})")
            }
        }
        return if (entries.isEmpty()) null else JsonObject(entries)
    }

    private fun nowMs(): Long = Clock.System.now().toEpochMilliseconds()

    private companion object {
        // Flush every 30s on a timer, OR when the buffer reaches the batch
        // threshold. Whichever first.
        private const val FLUSH_INTERVAL_MS = 30_000L
        private const val FLUSH_BATCH_THRESHOLD = 20

        // Backend caps batches at 100 — we cap at 50 for safety margin.
        private const val MAX_BATCH_SIZE = 50

        // Bounded ring buffer. Long offline sessions can exceed this; oldest
        // events get dropped first. 500 is enough for a multi-hour session at
        // typical event rates without becoming a memory liability.
        private const val MAX_BUFFER_SIZE = 500

        private const val INITIAL_BACKOFF_MS = 5_000L
        private const val MAX_BACKOFF_MS = 5 * 60_000L
        private const val SHUTDOWN_FLUSH_TIMEOUT_MS = 2_000L
    }
}
