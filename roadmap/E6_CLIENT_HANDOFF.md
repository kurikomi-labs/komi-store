# E6 — client handoff

Status: foundation in place on `feature/e6-telemetry`. Backend endpoint live (`POST /v1/telemetry/events`). Roughly 30% of the user-visible work shipped: one event type fires (`app_launched`), no consent UI exists yet, 19 events unwired.

This document is the punch list to take the client to GA. Read top to bottom.

---

## 0. What already exists on this branch (do NOT redo)

```text
core/domain/src/commonMain/kotlin/zed/rainxch/core/domain/telemetry/
  ProductTelemetry.kt              // interface: fire(name, props) + flush()
  ProductTelemetryConsent.kt       // enum: NotYetAsked / Granted / Denied
  ProductTelemetryEvents.kt        // 20 event-name constants + ALL set
                                   // 19 prop-key constants + ALLOWED set

core/data/src/commonMain/kotlin/zed/rainxch/core/data/
  telemetry/ProductTelemetryImpl.kt
      // Bounded ring buffer (500), 30s timer + 20-event threshold flush,
      // exponential backoff 5s→5min, ephemeral session_id (16 random bytes,
      // base64url, fresh per process), consent re-check at fire+flush+post-net
  dto/ProductTelemetryEventBody.kt
  network/BackendApiClient.kt          // postProductTelemetryEvents added
  repository/TweaksRepositoryImpl.kt   // get/setProductTelemetryConsent (DataStore)
  di/SharedModule.kt                   // ProductTelemetry registered as Koin single

core/domain/src/commonMain/kotlin/zed/rainxch/core/domain/repository/
  TweaksRepository.kt                  // get/setProductTelemetryConsent declared

feature/tweaks/presentation/src/commonMain/kotlin/zed/rainxch/tweaks/presentation/
  TweaksState.kt                       // productTelemetryConsent: ProductTelemetryConsent
  TweaksAction.kt                      // OnProductTelemetryToggled(enabled: Boolean)
  TweaksViewModel.kt                   // observes consent flow, handles toggle action

composeApp/src/androidMain/kotlin/zed/rainxch/githubstore/app/GithubStoreApp.kt
  // fires ProductTelemetryEvents.APP_LAUNCHED from onCreate

composeApp/src/jvmMain/kotlin/zed/rainxch/githubstore/DesktopApp.kt
  // fires ProductTelemetryEvents.APP_LAUNCHED from main()
```

`fire()` is a no-op when consent is not `Granted`. Currently nothing can grant consent because there's no UI. Wire that first, then events.

---

## 1. Compose UI — Switch row in privacy section

**Goal:** user can manually toggle product telemetry from the existing tweaks/privacy screen.

**Find the existing legacy-telemetry Switch:**
```bash
grep -rn 'OnTelemetryToggled\|isTelemetryEnabled' feature/tweaks/presentation/src/commonMain/kotlin/zed/rainxch/tweaks/presentation/components/
```

**Add a sibling `ListItem` directly below it:**
```kotlin
ListItem(
    headlineContent = { Text(stringResource(Res.string.settings_product_telemetry_title)) },
    supportingContent = {
        Text(stringResource(Res.string.settings_product_telemetry_description))
    },
    trailingContent = {
        Switch(
            checked = state.productTelemetryConsent == ProductTelemetryConsent.Granted,
            onCheckedChange = { onAction(TweaksAction.OnProductTelemetryToggled(it)) },
        )
    },
)
```

**Strings** (find the existing `settings_telemetry_*` keys and add neighbors):
```xml
<string name="settings_product_telemetry_title">Anonymous usage data</string>
<string name="settings_product_telemetry_description">Help improve GitHub Store with anonymous, aggregate metrics. We never collect search queries, repo names, or any identifying information. Schema is open source.</string>
```

**Acceptance:** toggling the Switch persists across app restart.

---

## 2. First-launch consent bottom sheet

Show when `productTelemetryConsent == NotYetAsked` and the user has reached the home screen. One screen, three buttons.

### 2a. New file

`feature/profile/presentation/src/commonMain/kotlin/zed/rainxch/profile/presentation/privacy/ProductTelemetryConsentSheet.kt`

```kotlin
package zed.rainxch.profile.presentation.privacy

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductTelemetryConsentSheet(
    onGrant: () -> Unit,
    onDeny: () -> Unit,
    onViewSchema: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Help improve GitHub Store", style = MaterialTheme.typography.headlineSmall)
            Text(
                "We collect anonymous, aggregate usage data to find bugs and improve performance. We never collect your repos, search queries, or any identifying information. The full schema and collection code are open source.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(onClick = onGrant, modifier = Modifier.fillMaxWidth()) { Text("Got it") }
            TextButton(onClick = onDeny, modifier = Modifier.fillMaxWidth()) { Text("No thanks") }
            TextButton(onClick = onViewSchema, modifier = Modifier.fillMaxWidth()) { Text("View what we collect") }
            Spacer(Modifier.height(8.dp))
        }
    }
}
```

### 2b. Wire from home

Add a small `HomeConsentGateViewModel` that exposes `consent: StateFlow<ProductTelemetryConsent>` and `grant() / deny()` (writes to `TweaksRepository`). In `HomeRoot`, observe and render the sheet conditionally. `onViewSchema` opens `https://github.com/OpenHub-Store/backend/blob/main/src/main/kotlin/zed/rainxch/githubstore/telemetry/TelemetryEvent.kt` (or, when the schema page exists, `https://github-store.org/telemetry-schema`) via `LocalUriHandler.current`.

Dismissing without choosing keeps state at `NotYetAsked` so the sheet reappears next launch — intentional, no answer ≠ silent opt-in.

**Acceptance:** fresh install → sheet appears once. "Got it"/"No thanks" persists, sheet doesn't reappear. Dismiss → sheet reappears next launch.

---

## 3. Wire the 19 remaining events

**Hard rules:**
- ALL event names from `ProductTelemetryEvents.*` constants.
- ALL prop keys from `ProductTelemetryProps.*` constants.
- NO inline strings on either side. A typo at a `fire()` call should be a compile error.
- NEVER `repo_name`, `query`, `username`, file paths, stack traces, exception messages, IDs, or anything user-identifying as prop values. Categorical / bucketed only.

### 3.1 Reliability

| Event | Where | Props |
|---|---|---|
| `CRASH` | `Thread.setDefaultUncaughtExceptionHandler` in `GithubStoreApp.onCreate` (Android) AND `CrashReporter.install` (Desktop) | `CATEGORY` ∈ `"data_loss" / "version_detect" / "install_fail" / "other"`, `PLATFORM` |
| `OPERATION_FAILED` | every `Result.failure(...)` branch in `core/data/.../services/` and `core/data/.../repository/` | `OP` ∈ `"download" / "install" / "update" / "fetch"`, `ERROR_CODE` (existing error enum's `name` — never raw exception messages) |

Helper file `composeApp/src/commonMain/kotlin/zed/rainxch/githubstore/app/CrashCategory.kt`:
```kotlin
fun categorizeCrash(throwable: Throwable): String = when {
    throwable is OutOfMemoryError -> "other"
    throwable.message?.contains("DataStore", ignoreCase = true) == true -> "data_loss"
    throwable.message?.contains("install", ignoreCase = true) == true -> "install_fail"
    throwable.message?.contains("version", ignoreCase = true) == true -> "version_detect"
    else -> "other"
}
```

Crash handler: chain to the existing `Thread.getDefaultUncaughtExceptionHandler()` after firing + 500ms flush. Don't suppress the original handler.

### 3.2 Performance

| Event | Where | Props |
|---|---|---|
| `COLD_START_MS` | `Application.onCreate` start → first frame of `App` Composable; once per launch | `PLATFORM`, `BUCKET` |
| `FIRST_PAINT_MS` | per-screen first non-skeleton render | `SCREEN` ∈ `"home" / "details" / "library" / "settings" / "search"`, `BUCKET` |
| `CACHE_HIT` / `CACHE_MISS` | each repo / readme / icon cache lookup | `CACHE_NAME` ∈ `"details" / "icons" / "readmes" / "search_results"` |

Bucketing helper `core/domain/src/commonMain/kotlin/zed/rainxch/core/domain/telemetry/Buckets.kt`:
```kotlin
package zed.rainxch.core.domain.telemetry

object TelemetryBuckets {
    fun durationMs(ms: Long): String = when {
        ms < 500 -> "<500"
        ms < 1000 -> "500-1000"
        ms < 3000 -> "1000-3000"
        else -> ">3000"
    }

    fun resultCount(n: Int): String = when {
        n == 0 -> "0"
        n <= 5 -> "1-5"
        n <= 20 -> "6-20"
        else -> ">20"
    }

    fun confidence(score: Float): String = when {
        score >= 0.85f -> "high"
        score >= 0.5f -> "medium"
        else -> "low"
    }
}
```

### 3.3 Discovery / engagement

| Event | Where | Props |
|---|---|---|
| `SEARCH_EXECUTED` | `SearchViewModel` on submit completion | `RESULT_COUNT_BUCKET` (via `TelemetryBuckets.resultCount`). **NEVER the query string.** |
| `DETAILS_VIEWED` | `DetailsViewModel` first-render | `FROM` ∈ `"search" / "category" / "library" / "link"` (derive from nav arg). **NEVER the repo name.** |
| `UPDATE_INSTALLED` | `DefaultDownloadOrchestrator` on install completion when `lastUpdatedAt` was bumped (i.e., update path, not first install) | (no required props) |

> **Coexist, not replace, for `SEARCH_EXECUTED`.** The legacy `TelemetryRepository.recordSearchPerformed` carries `query_hash + result_count + repo_id` per click for ranking-miss tracking. The new `SEARCH_EXECUTED` is a count-only aggregate. Both fire. Different purposes, different backends. Same is true for the rest of §3.6 below.

### 3.4 Import (E1) — REPLACE the legacy calls

**The six import events currently fire via `TelemetryRepository.recordImport*` calls. Move them to `ProductTelemetry`. Delete the legacy calls in the same commit so the migration is atomic — no double-emission on the wire.**

Sites (per E1's handoff doc):
- `core/data/.../repository/ExternalImportRepositoryImpl.kt` — `runFullScan` / `runDeltaScan` for `IMPORT_SCAN_STARTED`, `IMPORT_SCAN_COMPLETED`, `IMPORT_MATCH_ATTEMPTED`, `IMPORT_AUTO_LINKED`
- `feature/apps/.../ExternalImportViewModel.kt` — `skipPackage` / `pickSuggestion` / `submitSearchOverride` for `IMPORT_SKIPPED`, `IMPORT_MANUALLY_LINKED`

| Event | Props |
|---|---|
| `IMPORT_SCAN_STARTED` | `PLATFORM` |
| `IMPORT_SCAN_COMPLETED` | `CANDIDATE_COUNT`, `DURATION_MS` |
| `IMPORT_MATCH_ATTEMPTED` | `STRATEGY` ∈ `"manifest" / "search" / "fingerprint"`, `CONFIDENCE_BUCKET` (via `TelemetryBuckets.confidence`) |
| `IMPORT_AUTO_LINKED` | `COUNT` |
| `IMPORT_MANUALLY_LINKED` | `COUNT` |
| `IMPORT_SKIPPED` | `COUNT` |

**Out of scope — the following legacy import-related calls are NOT in the E6 allowlist and should be deleted in the same migration commit:**

- `recordImportSearchOverrideUsed`
- `recordImportSearchOverrideNoResults`
- `recordImportPermissionRequested`
- `recordImportPermissionOutcome`
- `recordImportUnlinkedFromDetails` (in `DetailsViewModel.confirmUnlinkExternalApp`)

These were finer-grained extensions the legacy pipeline took on. Either propose adding them to the E6 schema (requires backend allowlist update + migration coordination) OR delete them. **Recommended: delete.** They're ranking-signal-shaped but never used for ranking.

### 3.5 Proxy (E5)

| Event | Where | Props |
|---|---|---|
| `PROXY_CONFIGURED` | proxy ViewModel save handler | `TYPE` ∈ `"http" / "socks5" / "none"` |
| `PROXY_USED` | every outbound HTTP through the configured proxy (`BackendApiClient` request-completion site) | `SUCCESS: Bool` (true if 2xx) |
| `MIRROR_USED` | when a request used a mirror endpoint (search for `ghproxy`) | `PRESET` ∈ `"ghproxy" / "custom" / "none"`, `SUCCESS` |

### 3.6 Session

| Event | Where | Props |
|---|---|---|
| `SESSION_DURATION` | `ProcessLifecycleOwner.lifecycle` `ON_STOP` (Android) / `Window.onCloseRequest` (Desktop) | `SECONDS: Int` (compute from `coldStartAt` to now) |

---

## 4. Pipeline boundary — what stays in `TelemetryRepository`

**Do not migrate these. They have `repo_id` and feed `SignalAggregationWorker`'s ranking computation:**

- `recordSearchPerformed` — drives ranking-miss tracking and result-freshness signals
- `recordSearchResultClicked` — drives CTR
- `recordRepoViewed` — drives view-count ranking signal
- `recordReleaseDownloaded`, `recordInstallStarted`, `recordInstallSucceeded`, `recordInstallFailed` — drive install-success-rate signal
- `recordAppOpenedAfterInstall` — drives engagement signal
- `recordUninstalled`, `recordFavorited`, `recordUnfavorited` — drive long-term retention signals

These continue to flow through `/v1/events` and the `events` table. The two pipelines coexist by design — different jobs, different identity (hashed `device_id` vs ephemeral `session_id`), different consent toggles.

---

## 5. PrivacyAuditTest

### 5.1 Add `commonTest` to `core/domain`

Most KMP modules in this repo don't have `commonTest`. If `core/data` or any other module already has it, copy that pattern into `core/domain/build.gradle.kts`. Otherwise:

```kotlin
sourceSets {
    val commonTest by getting {
        dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
```

Ensure `libs.versions.toml` has:
```toml
kotlin-test = { module = "org.jetbrains.kotlin:kotlin-test", version.ref = "kotlin" }
```

### 5.2 Test file

`core/domain/src/commonTest/kotlin/zed/rainxch/core/domain/telemetry/PrivacyAuditTest.kt`:

```kotlin
package zed.rainxch.core.domain.telemetry

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class PrivacyAuditTest {

    @Test
    fun `event allowlist matches the backend's locked schema`() {
        // Hardcoded copy of the backend's TelemetryAllowlist. Drift between
        // client and server here means the server silently drops events the
        // client thinks it's sending — fail loudly on mismatch.
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
```

A source-tree static scan that asserts every `productTelemetry.fire(...)` call uses only `ProductTelemetryProps.*` keys is doable but requires a build task — skip unless you want CI-level enforcement.

**Acceptance:** `./gradlew :core:domain:jvmTest` passes.

---

## 6. Flush on app shutdown

### Android
`GithubStoreApp.onCreate` — register a `ProcessLifecycleOwner` observer on `Lifecycle.Event.ON_STOP`:
```kotlin
ProcessLifecycleOwner.get().lifecycle.addObserver(LifecycleEventObserver { _, event ->
    if (event == Lifecycle.Event.ON_STOP) {
        runBlocking { withTimeoutOrNull(2_000) { get<ProductTelemetry>().flush() } }
    }
})
```
(`onTerminate` only fires in emulators — don't rely on it.)

### Desktop
Before `application { … }` in `DesktopApp.kt`:
```kotlin
Runtime.getRuntime().addShutdownHook(
    Thread {
        runBlocking {
            withTimeoutOrNull(2_000) { GlobalContext.get().get<ProductTelemetry>().flush() }
        }
    },
)
```

---

## 7. Verification

```bash
# Both targets compile
./gradlew :composeApp:compileKotlinJvm :composeApp:compileDebugKotlinAndroid

# Tests pass (after §5.1 commonTest infra)
./gradlew :core:domain:jvmTest

# Manual verification against staging backend
# 1. Fresh-install device, open app → consent sheet appears
# 2. "Got it" → use the app for 30s → kill the app
# 3. SSH to VPS:
#    docker exec github-store-backend-postgres-1 psql -U githubstore -d githubstore \
#      -c "SELECT name, count(*) FROM telemetry_events GROUP BY name ORDER BY count DESC;"
#    Should show app_launched + cold_start_ms + first_paint_ms + (whatever else got wired)
# 4. Toggle off in Settings → use the app → re-query → no new rows
```

---

## 8. Commit conventions

One logical change per commit. Suggested order:

1. Render product telemetry consent toggle in privacy settings
2. Show first-launch consent sheet on home screen
3. Add TelemetryBuckets helper for duration / count / confidence bucketing
4. Wire CRASH and OPERATION_FAILED events
5. Wire COLD_START_MS and FIRST_PAINT_MS events
6. Wire CACHE_HIT and CACHE_MISS events
7. Wire SEARCH_EXECUTED and DETAILS_VIEWED events (coexist with legacy)
8. Migrate import events from TelemetryRepository to ProductTelemetry (E1 handoff §)
9. Wire PROXY_CONFIGURED / PROXY_USED / MIRROR_USED events
10. Wire SESSION_DURATION event on app stop
11. Add PrivacyAuditTest for telemetry allowlist
12. Flush telemetry buffer on app shutdown

After all merge: open PR `feature/e6-telemetry → main`, ship a new app release. Schema URL (`github-store.org/telemetry-schema`) can land separately — the consent sheet handles the missing URL gracefully (link goes to the GitHub source of the backend's `TelemetryEvent.kt`).

---

## 9. Backend contract recap

```text
POST https://api.github-store.org/v1/telemetry/events
Body:
{
  "events": [
    {
      "name": "...",            // must be in ProductTelemetryEvents.ALL
      "sessionId": "...",       // ≤128 chars, ephemeral, reset per launch
      "timestamp": 1745678901234,
      "platform": "android" | "macos" | "windows" | "linux",
      "appVersion": "1.7.0",
      "props": { ... }          // optional; keys from ProductTelemetryProps.ALLOWED
    }
  ]
}
Returns: 204 always (drops non-allowlisted server-side; partial success counts as success)
Rate limit: 600 / min / IP
Batch cap: 100 events; over → 400
Field caps: name 64, sessionId 128, platform 32, appVersion 32; over → 400
```

Status codes other than 204 / 400 are transient — the impl already retries with exponential backoff.
