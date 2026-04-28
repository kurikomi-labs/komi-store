# E1 — backend handoff

Status: client-side complete on `feature/e1-external-imports` (PR #461). Two backend endpoints required before E1 reaches GA. One consolidation question with E6.

---

## What the client expects

### 1. `POST /v1/external-match` — required

Match a batch of installed-app fingerprints to GitHub repos. Anonymous (no `X-GitHub-Token`). Used by the wizard's match-resolution pipeline; today the client mocks this endpoint behind the `tweaks.externalMatchSearchEnabled` flag (default `false`).

**Request:**

```http
POST /v1/external-match
Content-Type: application/json

{
  "platform": "android",
  "candidates": [
    {
      "packageName": "com.example.foo",
      "appLabel": "Foo App",
      "signingFingerprint": "AB:CD:EF:...",
      "installerKind": "browser",
      "manifestHint": { "owner": null, "repo": null }
    }
  ]
}
```

| Field | Type | Required | Constraints |
| --- | --- | --- | --- |
| `platform` | string enum | yes | `android` for E1 |
| `candidates` | array | yes | 1..25 items per request (client batches) |
| `candidates[].packageName` | string | yes | `^[\w.-]{1,255}$` |
| `candidates[].appLabel` | string | yes | utf-8, 1..200 chars |
| `candidates[].signingFingerprint` | string\|null | optional | `^[0-9A-F]{2}(:[0-9A-F]{2}){31}$` SHA-256 hex |
| `candidates[].installerKind` | string enum\|null | optional | `obtainium`, `fdroid`, `play`, `aurora`, `galaxy`, `oem_other`, `browser`, `sideload`, `system`, `github_store_self`, `unknown` |
| `candidates[].manifestHint.owner` | string\|null | optional | `^[\w.-]{1,39}$` |
| `candidates[].manifestHint.repo` | string\|null | optional | `^[\w.-]{1,100}$` |

**`manifestHint` semantics.** The entire `candidates[].manifestHint` object MAY be omitted when the client found no in-APK hint; today's client always sends it as `{ "owner": null, "repo": null }` for symmetry, but backends should treat omission and a present-but-fully-null object identically (no hint provided). Validation runs on each non-null field independently — e.g., if `manifestHint.owner` is null and `manifestHint.repo` is non-null, only `manifestHint.repo` is regex-validated against `^[\w.-]{1,100}$`. A present `manifestHint` with both `owner` AND `repo` non-null is the only shape that should drive the manifest-hint scoring branch; partial values (one null, one not) should be treated as no hint and fall through to the search/fingerprint paths.

**Response (200):**

```json
{
  "matches": [
    {
      "packageName": "com.example.foo",
      "candidates": [
        {
          "owner": "octocat",
          "repo": "hello-world",
          "confidence": 0.78,
          "source": "search",
          "stars": 1240,
          "description": "Example application"
        }
      ]
    }
  ]
}
```

`source` enum: `manifest` | `search` | `fingerprint`. `candidates[]` sorted by confidence descending, capped at 5. `stars` and `description` may be null.

**Other status codes:**
- `400` — invalid body
- `429` — rate limited; include `Retry-After` (in seconds). **Current client behavior:** `BackendApiClient.postExternalMatch` throws `RateLimitedException` on 429 and `ExternalImportRepositoryImpl.resolveMatches` logs the failure per-batch and continues with the remaining batches; no automatic WorkManager-backed retry is scheduled today. The plan called for WorkManager retry on `Retry-After` but it isn't wired yet — a backend that hard-rate-limits aggressively will see partial-result wizard sessions until that retry path is implemented in `resolveMatches`.
- `503` — partial outage. **Current client behavior:** `ExternalImportRepositoryImpl.resolveMatches` runs three strategies in parallel — manifest hints (parsed locally from each candidate's `AndroidManifest.xml`), signing-cert seed (looked up locally from the cached `signing_fingerprints` table), and the backend match call. When `BackendApiClient.postExternalMatch` fails with 503, only the backend strategy drops out for that batch; manifest-derived suggestions and signing-cert hits still flow through unaffected. So the client degrades to "local-only matching" — *not* manifest-only — as long as the seed sync has run at least once. Newly-installed apps with no manifest hint and no fingerprint match will see no suggestions until the backend recovers.

**Server-side scoring (per plan §3.2):**
- If `manifestHint.owner` and `repo` present → validate via HEAD against GitHub → `manifest` match at confidence 1.0
- If `signingFingerprint` present → look up in `signing_fingerprint → (owner, repo)` table → `fingerprint` match at confidence 0.92
- Else → score top 5 search results: exact-name match +0.4, substring +0.2, owner login matches packageName author segment +0.2, star bucket +0.05/0.10/0.15, has APK assets in last 5 releases +0.10 (else **−0.20** — heavy penalty for no-APK repos), description contains "Android"/"APK" +0.05
- Cap search-only confidence at 0.85 (keeps out of auto-link tier)

**Confidence clamping.** Backend MUST clamp every emitted `confidence` to `[0.0, 1.0]` before serialising the response. The −0.20 no-APK penalty plus other negative signals can produce a negative pre-clamp score for very weak matches; clamp those to 0.0 rather than emitting negative values. Rationale: client tier logic uses ≥0.85 (auto-link), 0.5..0.85 (preselected wizard suggestion), <0.5 (wizard with no preselect), and `RepoCandidateRow` displays `confidence * 100` percent rounded to int — client also runs a defensive `coerceIn(0, 100)` on the percentage but treating that as the source of truth on the wire would let invalid backend payloads slip through analytics. Manifest (1.0) and fingerprint (0.92) paths are already fixed values and don't require clamping; only the search-scoring path needs it.

**Cache:** 24h server-side keyed on `(packageName, appLabel, signingFingerprint)`. Including `signingFingerprint` in the key means a returning user with a different fingerprint (e.g., reinstalled the app from a different source after a key rotation) bypasses the cache and gets a fresh look-up. If `signingFingerprint` is null, treat the null itself as part of the key — don't merge null-fingerprint hits with the same package's known-fingerprint hits. (Original plan §3.2 wording was inverted — please use this clarified version.)

**Rate limit:** 60 req/hour/IP. Include `Retry-After` on 429.

**DTO:** `core/data/src/commonMain/kotlin/zed/rainxch/core/data/dto/ExternalMatchRequest.kt` and `ExternalMatchResponse.kt` already match this shape.

---

### 2. `GET /v1/signing-seeds` — required

Paginated incremental dump of signing-cert → GitHub-repo mappings, seeded from F-Droid index. Anonymous.

**Request:**

```http
GET /v1/signing-seeds?since=1714521600000&platform=android&cursor=opaque-cursor-string
```

| Param | Type | Required | Notes |
| --- | --- | --- | --- |
| `since` | integer (epoch millis) | optional | Only return rows observed at or after this timestamp |
| `cursor` | string | optional | Opaque pagination token from prior response |
| `platform` | string enum | required | `android` for E1 |

**Response (200):**

```json
{
  "rows": [
    {
      "fingerprint": "AB:CD:EF:...",
      "owner": "octocat",
      "repo": "hello-world",
      "observedAt": 1714521600000
    }
  ],
  "nextCursor": "opaque-string-or-null"
}
```

**Important:** `observedAt` MUST be **epoch milliseconds** (not seconds). The client stores this and passes it as `since` on the next sync. Mixing units silently corrupts the incremental cursor — there's a unit-tagged comment on the client DTO calling this out.

**Page size:** 1000 rows recommended. Initial seed: 5–15k rows total (5–15 page calls). Daily delta: typically <200 rows.

**Source:** F-Droid index has the `(certificate, source-code-URL)` mapping for ~5k OSS apps. Backend extracts it into the seed table on a daily cron.

**DTO:** `core/data/src/commonMain/kotlin/zed/rainxch/core/data/dto/SigningFingerprintSeedResponse.kt`

---

### 3. Flag flip (no client release needed)

After both endpoints are in production, flip `tweaks.externalMatchSearchEnabled` to `true`. The client picks this up via the existing tweaks DataStore channel — no client release required.

If your tweaks infrastructure doesn't yet support remote-driven values for that flag, ship the default-on flip in the next client release.

---

## Optional (defer if needed)

- **Fingerprint-derived match in `POST /v1/external-match`** — frontend computes this locally from the seed table, so the backend's `source: "fingerprint"` path is a redundant safety net. Skip if it's complex.
- **Dynamic seed updates with full diff** — initial seed is enough for v1. Daily delta can land later.

---

## Cannot defer

- **`POST /v1/external-match`** — without it, Strategy 2 (search) is mocked and the medium-confidence tier produces no matches. Manifest hints + signing-cert seed still work, so the wizard is usable but coverage is narrower.

---

## Telemetry overlap with E6 (clarification needed)

E6's handoff document (`feature/e6-telemetry`) §3.4 "Import (E1 / E2)" says to wire `IMPORT_SCAN_STARTED`, `IMPORT_SCAN_COMPLETED`, `IMPORT_MATCH_ATTEMPTED`, `IMPORT_AUTO_LINKED`, `IMPORT_MANUALLY_LINKED`, `IMPORT_SKIPPED` from `LibraryImportViewModel.kt` via the new `ProductTelemetry` interface.

**Two issues:**

1. The class is named `ExternalImportViewModel.kt`, not `LibraryImportViewModel.kt`. Heads-up so the next person doesn't grep for the wrong name.
2. **E1 already fires those six events** via the existing `TelemetryRepository.import*` methods. The wiring is in:
   - `ExternalImportRepositoryImpl.runFullScan / runDeltaScan` (importScanStarted / importScanCompleted / importMatchAttempted / importAutoLinked)
   - `ExternalImportViewModel.skipPackage / pickSuggestion / submitSearchOverride` (importSkipped / importManuallyLinked / importSearchOverrideUsed / importSearchOverrideNoResults / importPermissionRequested / importPermissionOutcome)
   - `DetailsViewModel.confirmUnlinkExternalApp` (importUnlinkedFromDetails)

**Decision needed before E6 wires §3.4:** does `ProductTelemetry` replace `TelemetryRepository` for these events, or do they coexist? If "replace," E6's port is the right approach and the existing `TelemetryRepository.import*` calls get deleted. If "coexist," every import action fires *two* events on the wire — almost certainly wrong.

Please respond with which path you intend so the E6 work doesn't double-emit.

---

## Endpoint URLs

The client base URL is the existing `BACKEND_BASE_URL`. Both new endpoints are siblings of `events`, `categories`, `topics`, `repo`, `releases`, `readme`, `user`. No new auth/scope needed.

## Verification path

```sh
# 1. Build the client APK against staging
./gradlew :composeApp:assembleDebug

# 2. Flip the flag locally for testing
adb shell am start-foreground-service \
  -a zed.rainxch.tweak.SET \
  --es key external_match_search_enabled --ez value true

# 3. Open the wizard, observe match calls hit your endpoint
adb logcat -s OkHttp | grep external-match

# 4. Confirm match results render with `source: "search"` chip in the wizard UI
```
