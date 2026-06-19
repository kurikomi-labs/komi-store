# Backend Refinements — Design Overhaul

Hand this to your backend coding agent. Every item is a field/endpoint/storage gap surfaced during design handoff analysis. Frontend ships with fallbacks for unfulfilled items, but full design fidelity requires these.

Origin: `~/Downloads/handoff 4/DESIGN.md` §14 + agent specs (`.design/UX-AUDIT.md`, `UX-ARCHITECTURE.md`, `UI-SPEC.md`).

## Priority legend

- **P0** — Blocks a core primitive from rendering correctly. Frontend uses neutral fallback.
- **P1** — Enables a richer signal. Frontend renders partial state until backend lands.
- **P2** — Nice-to-have polish.

---

## R1 — `trendingScore` (P0)

**Field:** `Repository.trendingScore: Float?` (or `Int?` rank position)
**Where used:** Trending section on Home (DESIGN.md §9.3 / §10.1). Drives the `#1 / #2 / #3` rank chip.
**Today:** Sample API returned `null`. Frontend currently uses a local affinity-`score` proxy in `HomeRepositoryImpl.kt:571-604`.
**Need:** Server-side ranking (e.g. release count × stars × freshness × velocity in last 7d). Stable per request; cache TTL ≥ 1h. Position (1, 2, 3…) is preferred over percentage to honor DESIGN.md §11 "no invented percentages."

## R2 — `popularityScore` (P0)

**Field:** `Repository.popularityScore: Int?` (rank position)
**Where used:** "Most popular" section on Home. Drives the rank numbers (Fraunces italic, opacity 0.55).
**Today:** Sample returned `null`. Frontend sorts by `stargazersCount` locally.
**Need:** Server-side all-time rank. Same caching as trending. Independent of time window.

## R3 — Per-app accent color (P1)

**Field:** `Repository.accent: { hex: String, lightTint: String, darkTintAlpha: Float }?`
**Where used:** Lead card bg tint, FreshnessRing outer, install-panel bloom, Apps row tinted leading icon.
**Today:** No field. Frontend will resolve in this order (per UX-Architecture §7): backend → topic-derived → language-derived → blue fallback.
**Backend opportunity:** Run a color-thief / palette extraction on `avatarUrl` once at index time; persist `{ hex, lightTint, darkTintAlpha }`. Saves CPU on every device + stays consistent across devices. **Deferred per D5** — frontend ships topic/language fallback first. Re-evaluate after overhaul lands.

## R4 — Sub-day release recency (P2)

**Field:** `Release.publishedAtUtcSeconds: Long` (in addition to current `publishedAt: Instant`)
**Where used:** FreshnessRing for releases <24h. Currently rounds to "1 day ago"; design wants "5 hours ago" granularity for the Hot bucket.
**Today:** `publishedAt: Instant` exists — frontend can derive seconds locally. **No backend change required if backend already returns ISO-8601 with seconds precision.** Confirm.

## R5 — Maintenance state (commit recency) (P1)

**Field:** `Repository.lastCommitAt: Instant?` or `Repository.daysSinceLastCommit: Int?`
**Where used:** Heartbeat animation period (active 1.4s / recent 2.4s / quiet 4.2s / dormant none — per DESIGN.md §6.1).
**Today:** Repository has `updatedAt` (last metadata update) but that's not commit recency. GitHub API exposes `pushed_at` — pipe through.
**Need:** Backend persists + serves `pushed_at` (default branch HEAD timestamp) per repo. Refresh hourly or on release.

## R6 — Mirror health for `SignalBars` (P0)

**Field:** Per-mirror `health: { latencyMs: Int, lossPercent: Float, lastCheckedAt: Instant }`
**Where used:** Mirror picker — `SignalBars` (4 ascending bars, WiFi-style) shows mirror quality.
**Today:** GHS pings mirrors client-side (existing `AutoSuggestMirror` logic). Lives on device, not in `MirrorsRepository`.
**Need (backend):** Optional — backend-aggregated health metrics across users would improve cold-start UX. Not required if client-side latency probe stays.

## R7 — Expected APK signing fingerprint (P0)

**Field:** `Repository.signingFingerprint: { sha256: String, source: "first_install" | "publisher_declared" }?`
**Where used:** Wax-seal trust card (DESIGN.md §7.8). Cracked-seal red state requires comparing installed APK fingerprint to expected.
**Today:** No "expected" fingerprint stored anywhere. Cracked-seal state can never fire.
**Need:**

- On-device: persist fingerprint of first successful install per `InstalledApp` (Room schema addition). Subsequent installs compare against this.
- Optional backend: publisher-declared fingerprint (maintainer registers via dashboard) — out of scope for this overhaul unless backend wants it.

## R8 — Translation cache persistence (P2)

**Field:** Backend-side translation cache keyed by `(target_lang, version_tag, repo_id)`.
**Where used:** Inner Detail screen language toggle. Hot path: user clicks "Translate to es", waits for provider response.
**Today:** Client-side in-memory cache only (kills on app restart).
**Need:** Either (a) backend caches translations and serves them via `/v1/translate/cache` so cold-start re-opens are instant, or (b) client persists to Room (already aligned with KSafe stack — frontend can do this).
**Recommendation:** Client-side persistence to Room is simpler. Skip backend cache unless multi-device sync is desired.

## R9 — `permissionRisk` summary (P1)

**Field:** `Repository.permissionRisk: "low" | "moderate" | "high"?`
**Where used:** Vital signs grid (Permissions tile) on Detail. `PermDot` color (green/amber/red).
**Today:** Frontend computes locally from APK Inspect results (Android only). Desktop can't show — needs backend.
**Need (backend):** When scanning a release's APK assets, classify by protection-level groups (Android docs: normal/signature/dangerous). Cache per `(repoId, releaseTag)`. Required for Desktop parity.

## R10 — `licensePosture` from SPDX (P1)

**Field:** `Repository.licensePosture: "copyleft" | "permissive" | "unknown"`
**Where used:** `LicensePosture` glyph (Filled © tile vs dashed · tile).
**Today:** `Repository.license` is a free-text string today. Frontend has hardcoded mapping in tokens.json (`licenses.copyleft`, `licenses.permissive`).
**Need:** Backend normalizes to SPDX identifier on indexing. Frontend uses the tokens.json map. **No backend change needed if license already SPDX.**

## R11 — `downloads` field (P1)

**Field:** `Repository.totalDownloads: Long` (sum across all release assets)
**Where used:** `DownloadWeight` primitive (radius is log10(downloads)) + meta line "4.8M dl" on Detail.
**Today:** Existing Forgejo path aggregates client-side. GitHub doesn't expose easily — current GHS backend likely already proxies.
**Need:** Confirm `totalDownloads` populated for both GitHub + Forgejo paths.

## R12 — `topics` field (P1)

**Field:** `Repository.topics: List<String>`
**Where used:** TopicGlyph row (up to 3, mapped via `tokens.json.topicGlyphs.supported` + `topicAliases`).
**Today:** GitHub API returns topics; confirm backend forwards them. Forgejo `/repos/topics` endpoint exists.
**Need:** Both branches expose `topics` consistently. If topic count is high, prioritize ones in our supported map.

## R13 — `pushedAt` vs `updatedAt` clarity

**Field naming:** Distinguish "last metadata change" (`updatedAt`) from "last commit" (`pushedAt`).
**Today:** `Repository.updatedAt` ambiguous in our API.
**Need:** Rename or split — UX-Audit flagged this as confusing.

## R14 — Backend support for accent override on a per-repo basis (P2)

**Field:** Allow maintainer-published `accent: {hex, lightTint, darkTintAlpha}` overrides.
**Where:** Some apps have brand colors that don't match topic/language fallback. Maintainer should be able to opt in.
**Today:** No mechanism.
**Need:** Out of scope unless backend wants. Frontend's fallback chain (topic → language → blue) is acceptable for v1.

---

## Endpoint additions needed

| Endpoint | Purpose | Priority |
| :--- | :--- | :--- |
| `GET /v1/repo/{owner}/{repo}/trending-rank` | Returns `{ rank: Int, lastComputedAt: Instant }` if repo in top-100 trending. Else 404. | P0 |
| `GET /v1/repo/{owner}/{repo}/popularity-rank` | Same shape, all-time. | P0 |
| `GET /v1/repo/{owner}/{repo}/permissions-summary` | Returns `{ posture: low\|moderate\|high, dangerous: List\<String\>, sensitive: List\<String\> }` for latest release. | P1 |
| `GET /v1/translate/cache?lang=&version=&repo=` | Returns cached translation or 204. Skip if going Room-only. | P2 |

---

## Data model summary

Backend `Repository` payload after refinements should have these new/clarified fields:

```jsonc
{
  "id": 123,
  "owner": "...",
  "name": "...",
  // ... existing fields ...

  // NEW or clarified
  "trendingScore": 1,          // rank position in trending (nullable)
  "popularityScore": 47,       // rank position all-time (nullable)
  "pushedAt": "2026-05-20T...",// last commit (DISTINCT from updatedAt)
  "totalDownloads": 4843201,
  "topics": ["self-hosted", "photos"],
  "licensePosture": "copyleft",     // server-normalized from SPDX
  "permissionRisk": "moderate",     // optional pre-computed summary
  "accent": null                    // deferred — null until D5 reversed
}
```

---

## Frontend fallback behavior (what ships without backend changes)

Per UI-SPEC.md §5 Data Honesty Audit Hooks:

- `trendingScore` null → drop rank chip; sort by local affinity
- `popularityScore` null → sort by stars; drop rank number
- `accent` null → topic → language → blue fallback
- `pushedAt` null → use `updatedAt` (with caveat in Heartbeat label)
- `mirrorHealth` null → client-side ping
- `signingFingerprint` not stored locally → Wax-seal stays in "Unsigned" open state forever
- `permissionRisk` null → derive on Android via APK Inspect; show "—" on Desktop
- `licensePosture` SPDX missing → "unknown" → dashed neutral tile
- `topics` missing → no TopicGlyph row

Nothing fabricated. Missing primitives just don't render.
