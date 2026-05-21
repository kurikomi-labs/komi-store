# Screen Inventory

Master list of every screen in GHS + its coverage by the handoff. Drives Phase 6+ work order. Uncovered screens get pre-build design pass + maintainer approval per decision D9.

## Coverage legend

- **FULL** — handoff has ASCII layout + spec + JSX reference for this screen
- **PARTIAL** — handoff mentions the screen but only fragments (e.g. component used, but no full layout)
- **NONE** — handoff doesn't cover; extrapolate from primitives/patterns + ask user

## Primary routes (from `GithubStoreGraph.kt`)

| # | Route | Module | Coverage | Handoff ref | Notes |
|---|-------|--------|----------|-------------|-------|
| 1 | `HomeScreen` | `feature/home/` | **FULL** | DESIGN.md §9.3 / §10.1 + `home.jsx` + `mobile.jsx` | Lead + Hot + Trending + Popular + From-your-stars |
| 2 | `SearchScreen(initialPlatform)` | `feature/search/` | **PARTIAL** | DESIGN.md §10.3 + `desktop.jsx` `DesktopSearchScreen` | Source toggle (GitHub/Codeberg/Custom) NOT in handoff |
| 3 | `AuthenticationScreen` | `feature/auth/` | **PARTIAL** | DESIGN.md §16.5 (full-screen sheet pattern w/ device-flow code) | Web-OAuth path + PAT path need extrapolation |
| 4 | `DetailsScreen(repoId, owner, repo, isComingFromUpdate, sourceHost)` | `feature/details/` | **FULL** | DESIGN.md §8.4 + §9.4 + `desktop.jsx` + `mobile.jsx` + `detail-inner.jsx` | Foreign-source variant needs handling |
| 5 | `DeveloperProfileScreen(username)` | `feature/dev-profile/` | **NONE** | — | Pre-build design pass |
| 6 | `ProfileScreen` | `feature/profile/` | **PARTIAL** | DESIGN.md §8.1 mentions user card + MIGRATION.md §risky areas (Connect + Business inquiries vs donate) | Need full layout pass |
| 7 | `TweaksScreen` | `feature/tweaks/` | **PARTIAL** | DESIGN.md §8.1 mentions Settings entry, `tweaks-panel.jsx` shows palette picker only | Full Tweaks UI needs design — many sub-sections (Network, Translation, Installation, Updates, etc.) |
| 8 | `FavouritesScreen` | `feature/favourites/` | **NONE** | — | Reuse list-row pattern but pre-build design pass |
| 9 | `StarredReposScreen` | `feature/starred/` | **NONE** | — | Reuse list-row pattern but pre-build design pass |
| 10 | `RecentlyViewedScreen` | `feature/recently-viewed/` | **NONE** | — | Reuse list-row pattern but pre-build design pass |
| 11 | `AppsScreen` (= Library) | `feature/apps/` | **FULL** | DESIGN.md §8.3 / §9.5 + `desktop.jsx` (two-pane) + `mobile.jsx` | Desktop two-pane is net-new for GHS |
| 12 | `ExternalImportScreen` | `feature/apps/` (import wizard) | **PARTIAL** | DESIGN.md §16.5 full-screen sheet pattern | Multi-step Obtainium import flow needs full design |
| 13 | `MirrorPickerScreen` | `feature/tweaks/` (mirror sub-screen) | **NONE** | tokens.json mentions `SignalBars` for mirror speed | Pre-build design pass |
| 14 | `SkippedUpdatesScreen` | `feature/tweaks/` (updates sub-screen) | **NONE** | — | Pre-build design pass |
| 15 | `HiddenRepositoriesScreen` | `feature/tweaks/` (updates sub-screen) | **NONE** | — | Pre-build design pass |
| 16 | `WhatsNewHistoryScreen` | `feature/profile/` or standalone | **NONE** | DESIGN.md §16.6 mentions What's-new sheet (per-version), not history page | Pre-build design pass |
| 17 | `AnnouncementsScreen` | `feature/profile/` or standalone | **NONE** | — | Pre-build design pass |
| 18 | `StarredPickerScreen` | `feature/apps/` (link-app wizard) | **NONE** | — | Pre-build design pass |
| 19 | `HostTokensScreen` | `feature/tweaks/` (access tokens) | **NONE** | — | Pre-build design pass (per-host PAT manager — GHS-specific) |

## Cross-screen overlay surfaces (DESIGN.md §16)

| Surface | Coverage | Where it shows up |
|---------|----------|-------------------|
| Bottom sheet (asset picker, install variant, source) | **FULL** §16.1 | Details install panel, link-app sheet |
| Confirm dialog (sign out, uninstall, clear cache) | **FULL** §16.2 | Profile sign-out, Apps uninstall, Tweaks clear |
| Toast (retry-after, install complete, network changed) | **FULL** §16.3 | Cross-app |
| Diagnostics card (Send feedback) | **FULL** §16.4 | Tweaks → Feedback (current `FeedbackBottomSheet`) |
| Full-screen sheet (OAuth, PAT, Imports wizard) | **FULL** §16.5 | Auth, ExternalImport |
| What's-new sheet (per-version one-shot) | **FULL** §16.6 | First launch after upgrade (existing `WhatsNewHistory` ≠ this sheet) |
| Dropdown menu (translate lang, sort, palette) | **PARTIAL** §16.7 | TranslateButton in Details, Apps sort menu |

## Sub-components / dialogs that need design (GHS-specific, no handoff coverage)

- **CustomForgesDialog** (Tweaks → Network → Custom forges) — list + add/remove forge hosts
- **AdvancedAppSettingsBottomSheet** (Apps → row long-press) — asset filter regex, monorepo fallback, pin variant, include pre-releases
- **LinkAppBottomSheet** (Apps → unlinked device app) — search GitHub repo + pick + link
- **ApkInspectSheet** (Details → Android only) — partial coverage in DESIGN.md §9.6 standalone APK Inspect screen
- **ReleaseAssetsPicker / VersionPicker / VersionTypePicker** (Details install panel) — pickers for asset / version / channel
- **AutoSuggestMirrorSheet** (Tweaks → Mirror auto-suggest) — locale-gated suggestion
- **FeedbackBottomSheet** (Tweaks → Feedback) — already aligned with §16.4 pattern
- **DownloadProgress / InstallProgress UI** (Details install flow) — DESIGN.md mentions inline progress but doesn't detail
- **LanguagePicker** (Details → translate) — DESIGN.md §16.7 pattern
- **TranslationControls** (Details → translate bar) — provider-aware toggle
- **PlatformSectionCard** (Details → cross-platform assets) — group APK/EXE/DMG by platform

## Sub-screens not in `GithubStoreGraph.kt` but composed inside features

- **APK Inspect** (full-screen, Android only) — handoff DESIGN.md §9.6 covers FULL
- **Inner Detail (About / What's-new tabs + version rail)** — handoff DESIGN.md §8.4 covers FULL
- **Onboarding** (first launch) — not currently a screen in GHS but worth considering. MIGRATION.md mentions "first-launch surfaces"
- **Empty states** (Library before scan, Search before query, Updates when none, Favourites empty) — `patterns.md` §Pattern: Empty state

## What stays purely behavioural (no UI redesign)

- Install flow (Shizuku / Dhizuku / Root / Default) — pickers re-styled, logic untouched
- Background update check (WorkManager) — no UI surface
- KSafe encryption layer — no UI
- Crash reporter (Desktop) — no UI
- WinGet publish workflow — CI only
- SignPath Windows signing — CI only

## Open per-screen questions (resolve in their respective phases)

1. **Onboarding** — build it? (Currently no formal onboarding; first launch goes straight to Home.)
2. **Profile Connect rows** — what platforms? (Mastodon, GitHub Discussions, Reddit, Discord? Maintainer pick.)
3. **Profile Business inquiries** — what data? (Email + GitHub Issues link?)
4. **WhatsNewHistory** — page or per-version sheet only? (Current GHS has a route — keep as history list.)
5. **Announcements** — backend-driven or static? (Current is backend-driven.)
6. **MirrorPickerScreen vs MirrorPickerSheet** — is full-screen still right, or move to bottom sheet?
7. **HostTokensScreen** — table-style or card-style rows? (Sensitive data; mask PAT by default.)
8. **CustomForgesDialog** — keep as dialog or promote to sub-screen?
9. **Desktop two-pane Apps** — does it apply to Favourites/Starred/Recently-viewed too?
10. **Search source toggle** — chip row above results or in filter sheet?

These get re-asked in the relevant phase.
