# Design Overhaul Decisions

Locked decisions before Phase 0. Source: maintainer answers during kickoff session (2026-05-21).

## D1 — Bottom nav 5 → 4

Drop **Tweaks** tab from bottom nav. Reachable via Profile → Settings (already wired).
Keep Home / Search / Apps / Favourites.
Override: handoff's 3-tab + Search-FAB rejected as too disruptive to muscle memory.

## D2 — Noto fonts

Bundle **Latin-only** Fraunces, Inter Tight, JetBrains Mono with the app.
Non-Latin scripts (CJK / Devanagari / Arabic / Hebrew / Thai etc.) fall back to system fonts.
Modern Android / macOS / Windows ship Noto-like coverage; Linux may show tofu (acceptable risk).
Total bundled font weight target: ≤ 4 MB across all three families.

## D3 — AppTheme migration map

Legacy `AppTheme` enum → new `Palette`:

- `OCEAN` → `NORD`
- `SLATE` → `NORD`
- `PURPLE` → `PLUM`
- `FOREST` → `FOREST`
- `AMBER` → `CREAM`

One-time migration on first launch after upgrade. Surface a non-blocking banner ("Themes refreshed — try the new palettes in Tweaks") via a new one-shot flag in `TweaksRepository`.

## D4 — Wonky-squircle scope

Full DESIGN.md coverage: hero CTAs, lead cards, search input, bottom sheets, confirm dialogs, toasts, device-code box, and any "wonky" call-out in the handoff. Implement as custom `WonkySquircleShape : Shape` using `Path.arcTo` with elliptical bounding rects. No fallback to `RoundedCornerShape`; the asymmetric character is brand-defining.

## D5 — Sponsor cut

Cut `SponsorScreen` entirely. Remove:

- Route `GithubStoreGraph.SponsorScreen`
- Profile sponsor row (`feature/profile/.../Options.kt:114`)
- Nav wiring in `AppNavigation.kt`
- Any deep links / strings

Aligns with MIGRATION.md "no donations in UI."

## D6 — Desktop two-pane Library

Build during **Phase 4**. Desktop `feature/apps/` adopts list (380dp) + detail (660dp) two-pane per DESIGN.md §8.3. Android remains single-pane → full-screen detail.
Existing single-pane code path retained behind the same composable signature; platform branch picks layout.

## D7 — AMOLED mode

`ThemeMode` enum = `LIGHT` / `DARK` / `AMOLED` / `SYSTEM`. AMOLED is a Dark sub-mode (resolves to Dark when `SYSTEM = dark`). All 4 palettes ship an AMOLED variant — pure-black `bg` (#000) + dimmed `surface` (lift one notch up). Adds ~30% token surface but contained to `Tokens.kt`.

## D8 — Visual reference

Open `desktop-app.html` / `mobile-app.html` / `design-system.html` **per phase** as side-by-side reference. Not opened upfront en-masse.

---

## Carry-over from agent specs (no decision needed — accepted as written)

- Tokens delivered as hand-written `Tokens.kt`, not Gradle codegen (UX-Architect §1).
- Single `core/presentation/.../vocabulary/` module hosts all primitives (UX-Architect §8).
- Per-app accent at UI mapper layer, not domain model (UX-Architect §7).
- `GhsTheme` composable wraps `MaterialExpressiveTheme` + 6 composition locals (UX-Architect §2).
- Build order: Phase 0 → 8 per UX-Architecture sequencing.
- Vocabulary uses `ImageVector.Builder` + `Canvas`; Material Symbols painters where applicable (UX-Architect §9).

## D9 — Coverage policy

Design handoff covers only some screens (Home, Library, Detail incl. Inner About/What's-new, Search, APK Inspect, overlay surfaces, Tweaks fragments). Many screens are uncovered. Policy: **plan each uncovered screen before implementing**, extrapolating from DESIGN.md primitives + patterns + tokens. Ask clarification questions before building.

## D10 — Motion scope (rich)

Beyond DESIGN.md §6.2 baseline (Heartbeat + 120ms tap + palette/mode crossfade):

- Shared-element transitions (Compose `SharedTransitionLayout`) on avatar → Detail hero
- Spring physics on press / release (`spring(dampingRatio = MediumBouncy)`)
- List item enter/exit (slide 200ms)
- Parallax on Detail hero scroll
- Skeleton loaders during async fetches

Quick, never blocking user. Spring stiffness defaults: high (300+) so transitions feel snappy.

## D11 — Backend refinements doc

`.design/BACKEND-REFINEMENTS.md` drafted upfront. Hand to backend coding agent in parallel while frontend builds. Frontend ships with fallbacks for any field not yet populated.

## D12 — PR cadence

**One mega-PR** at the end of overhaul. Commits per feature / milestone (atomic). Memory rule (commit msgs ≤10 words) applies. Each commit compiles + runs.

## D13 — Build order

User-directed: **Root + Navigation first**, then **core module fully**, then **feature-by-feature**. Translated to 17 phases — see `MEMORY.md` `project_design_overhaul` and the task list.

## D15 — SponsorScreen full delete

Delete `feature/profile/.../SponsorScreen.kt` composable, its route in `GithubStoreGraph`, nav wire in `AppNavigation`, sponsor row in `feature/profile/.../Options.kt`, and all related strings across 13 locales. No flag, no shim.

## D16 — Apps tab renamed "Library"

Label change only. Route name `AppsScreen` stays (no breaking change for deep links). BottomNav + Drawer label → "Library". String resource updated across 13 locales.

## D17 — Onboarding (3 steps)

New first-launch flow:

1. **Palette pick** — 4 Cookie swatches (Nord/Cream/Forest/Plum) + System/Light/Dark mode default
2. **Sign in (optional)** — entry point to web-OAuth or device-flow or skip
3. **Permissions (Android)** — notifications + install-from-unknown-sources prompts (skip-able)

One-shot. Persisted via `TweaksRepository.onboardingComplete: Boolean`. Skipped on subsequent launches. Lives in `composeApp/` as app-level orchestration (no new feature module — too small).

## D18 — Shared elements: Android only

Use `SharedTransitionLayout` + `sharedElement` modifier (`@OptIn(ExperimentalSharedTransitionApi::class)`) on Android only. Desktop uses standard slide/fade nav transitions via Compose Navigation's `enterTransition` / `exitTransition`. Platform branch via `expect/actual` or runtime platform check.

## D14 — Architecture skills

Source of truth for ViewModel/State/Action/Event/Root/Screen structure, navigation, DI, error handling, testing: `~/.claude/skills/android/*`. Applied to KMP/CMP common code (most patterns platform-agnostic).

---

## Deferred / explicitly out of scope for this overhaul

- Color-thief / server-side accent derivation from avatar (UX-Architect Q6) — use topic + language fallbacks only.
- Translate-the-app (Crowdin pipeline) (UI-Designer §8).
- Material You dynamic color override (themes.md §Disallowed).
- New 5th palette (themes.md §Disallowed).
- Fake trending percentages / "Featured" curation / invented data (DESIGN.md §11).
