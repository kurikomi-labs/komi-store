# Personality migration sweep — execution plan

Branch: `feat/personality-design-system`. Goal: get `:composeApp:compileDebugKotlinAndroid` (and `:composeApp:run`) **green** again by migrating every file still importing the deleted `theme.*` package onto the new personality system + Komi components.

## End state
- Zero imports of `zed.rainxch.core.presentation.theme.*` anywhere (package is deleted).
- Legacy `Ghs*`/`Expressive*`/`RepositoryCard` components removed; consumers on Komi components.
- `:composeApp:compileDebugKotlinAndroid` green; desktop `:composeApp:run` launches.

## Scope (measured, not estimated)
- **72 files** import deleted `theme.*`. By module: tweaks 20 · details 15 · core/presentation 12 · dev-profile 5 · composeApp 5 · apps 4 · starred 2 · profile 2 · favourites 2 · search/repo-pages/recently-viewed/home/auth 1 each.
- `vocabulary`/`color` packages already clean (0). Old cards `RepoStripeCard`/`CompactCard`/`LeadHeroCard`/`RowCard` **do not exist** (0) — ignore the earlier note about them.

## Deleted-symbol → replacement (swap table)
| Deleted symbol | Uses | Replace with |
|---|---|---|
| `tokens.Radii.*` | 47 | `LocalPersonality.current.shape.corner` / `.cornerSmall` (respects personality: manga 0, classic rounded). If the element is a styled container → migrate it to `KomiSurface` and drop the manual shape. |
| `GithubStoreTheme { }` | 14 | **Remove** the wrapper — root `Main.kt` already provides `PersonalityTheme(...)`. In `@Preview`, use `PersonalityPreview { }` / `PersonalityTheme(classicPersonality()) { }`. |
| `shapes.WonkySquircleShape` | 12 | `RoundedCornerShape(LocalPersonality.current.shape.corner)`; on cards → `KomiSurface`. |
| `tokens.GhsAccents.*` | 5 | `LocalPersonality.current.colors.primary` (+ `onPrimary`) or `LocalStatusColors.current` for semantic accents. |
| `shapes.CornerRadii` | 3 | `LocalPersonality.current.shape.corner` / `.cornerSmall`. |
| `geist` / `geistMono` | 4 | Drop — fonts come from `personality.type` (theme-resolved). Use `KomiText` roles; raw `Text` → `personality.type.body`/`.mono`. |
| `isDynamicColorAvailable` / `dynamicColorScheme` | 3 | **Delete the dynamic-color path** — personalities are fixed palettes. Remove the `if (dynamic) … else …` branch. |
| `tokens.colorSchemeFor` / `tokens.Tokens` | 2 | `LocalPersonality.current.colors` (or just `MaterialTheme.colorScheme`, fed by the bridge). |

**Do NOT touch** `MaterialTheme.colorScheme.*` reads — the `PersonalityTheme` provider seeds `colorScheme`, so all ~122 readers still resolve. Leave them; only fix `theme.*` imports.

## Phase 0 — Demolish old vocabulary FIRST (clean slate, before ANY migration)
Delete every old-approach component up front (user-directed) so nothing half-migrated survives and the migration can only target Komi. **Do NOT rebuild anything in core** — rows etc. get rebuilt inline at the feature call-site. After Phase 0, core's component layer = `Komi*` + the KEEP list below; everything else broken downstream is migrated in later batches.

**KEEP (functional/infra, NOT design vocab — never delete):** `GitHubStoreImage`, `ScrollbarContainer`(+`.jvm`), `media/PlatformGlyph`, `adaptive/*` (already on FormFactor SSOT), `markdown/*`, `buttons/MangaButtonRoles` (NEW, part of KomiButton), `status/StatusColors`+`StatusPalette` (color SSOT). `announcements/*` + `whatsnew/*` = live feature UI → MIGRATE (a later batch), not delete.

**DELETE now (core/presentation):**
- `inputs/GhsTextField.kt`, `GithubStoreButton.kt`
- `overlays/GhsBottomSheet.kt`, `GhsFullScreenSheet.kt`, `GhsConfirmDialog.kt`, `GhsDropdownMenu.kt`, `GhsToast.kt`
- `hub/GhsSectionHeader.kt`, `hub/GhsEntryRow.kt`, `section/SectionHeader.kt`, `section/Banner.kt`, `chrome/GhsHomeTopBar.kt`
- `RepositoryCard.kt`, `ExpressiveCard.kt`, `RepoRankChip.kt`, `RepoReleasePill.kt`, `RepoAvatarTile.kt`, `RepoIdentity.kt`, `FloatingPill.kt`
- `chips/AddChip.kt`, `chips/FilterChip.kt`, `chips/PlatformsChip.kt`, `chips/StatChip.kt`
- `grep -rn GhsPasswordVisibilityIcon` → fold into `KomiTextField` (trailing icon) or delete. Remove now-empty dirs (`chrome/`, `section/`, `hub/` if emptied).

**Successor map** (apply when migrating each deleted thing's call-sites in later batches):
| Deleted | Successor (all BUILT + compiling) |
|---|---|
| `GhsTextField` (+`GhsPasswordVisibilityIcon`) | `KomiTextField` |
| `GithubStoreButton` | `KomiButton` |
| `GhsBottomSheet`, `GhsFullScreenSheet` | `KomiSheet(placement=Bottom)` |
| `GhsConfirmDialog` | `KomiSheet(placement=Center)` + footer `KomiButton`s |
| `GhsDropdownMenu`(+`Item`/`Divider`) | `KomiDropdown` + `KomiMenuEntry`/`KomiMenuItem`/`KomiMenuDivider` |
| `GhsToast` | `KomiToast` + `KomiToastHost`(`rememberKomiToastState`) mounted in scaffold overlay |
| `GhsSectionHeader`, `section/SectionHeader` | `KomiText(role = Title)` + `Spacing` |
| `GhsHomeTopBar` | `KomiTopBar` (+ `KomiScaffold`/`KomiBottomBar` for the app shell) |
| `GhsEntryRow`, `GhsEntryBadge` | `KomiSurface` + `KomiText` row, rebuilt **at the feature call-site** (Tweaks settings rows) — NOT a new core component |
| `Banner` | `KomiSurface` |
| `RepositoryCard`/`ExpressiveCard`/`RepoRankChip`/`RepoReleasePill`/`RepoAvatarTile`/`RepoIdentity`/`FloatingPill` | `KomiRepoCard` (+ `KomiChip`/`KomiSurface`) |
| `AddChip`/`FilterChip`/`PlatformsChip`/`StatChip` | `KomiChip` (Info/Filter/Input; platform glyph via `leadingContent`) |
| status badges (repo-pages Issues/PR/Security) | `KomiChip(Info, leadingContent={Icon})` + `LocalStatusColors.current.<field>` at call-site — **NO core status component** (overbuild; see [[feedback_no_overbuild_feature_ui]]) |

## Batch order (dependency-first; compile-checkpoint each)
Migrate the lowest layer first so feature modules have green deps.

**Batch 0 = Phase 0 demolition (above).** Pure deletion — do NOT rebuild rows/banners in core (rebuild at the feature call-site). Then migrate the core-resident LIVE UI onto Komi: `announcements/*` (AnnouncementCard/CriticalAnnouncementModal/MuteSettingsBottomSheet/AnnouncementsRoot → `KomiSurface`/`KomiSheet`/`KomiText`) + `whatsnew/*` (`WhatsNewHistoryScreen`/`SectionBlock`). Checkpoint: `./gradlew :core:presentation:compileDebugKotlinAndroid` green (downstream feature breakage still expected).

**Batch 1 — leaf features (low count): home, search, auth, favourites, starred, recently-viewed, profile, repo-pages (1–2 files each).** Mechanical swaps per the table + delete `Ghs*`→`Komi*`. Checkpoint per module: `:feature:<name>:presentation:compileDebugKotlinAndroid`.

**Batch 2 — dev-profile (5), apps (4).** Same. Checkpoint each.

**Batch 3 — details (15).** Biggest feature; cards → `KomiRepoCard`/`KomiSurface`, sheets → `KomiSheet`, chips → `KomiChip`. Checkpoint `:feature:details:presentation:…`.

**Batch 4 — tweaks (20).** Settings rows rebuilt inline on `KomiSurface`+`KomiText` (no `GhsEntryRow`), `GhsSectionHeader`→`KomiText(Title)`, `GhsTextField`→`KomiTextField`, dialogs/sheets → `KomiSheet`, dropdowns → `KomiDropdown`, feedback sheet → `KomiSheet`+`KomiTextField`+`KomiToast`. Checkpoint `:feature:tweaks:presentation:…`.

**Batch 5 — composeApp (5).** Nav + DI + any remaining wrappers. Remove leftover `GithubStoreTheme` wrappers.

**Batch 6 — integration.** `./gradlew :composeApp:compileDebugKotlinAndroid` then `:composeApp:compileKotlinJvm`; fix stragglers. Run `./gradlew :composeApp:run` (desktop) for a visual smoke test.

## Per-file checklist
1. Open file; list its `theme.*` imports.
2. Apply swap table (symbol → replacement). Swap legacy `Ghs*`/`Expressive*`/`RepositoryCard` usages → Komi.
3. Remove now-unused imports; add Komi/personality/`Spacing`/`LocalPersonality` imports.
4. Leave `MaterialTheme.colorScheme.*` alone.
5. Module compiles before moving on.

## Verify / guardrails
- After each batch: the batch's module compiles (`:…:compileDebugKotlinAndroid`).
- Final: `:composeApp` compiles (Android + JVM) + desktop run.
- Then (optional, separate PR): a detekt rule banning raw `androidx.compose.material3.*` outside `core/presentation/components/` to stop regressions — NOT part of this sweep.

## Notes
- One commit per module batch (small-mid commits, ≤10-word msgs). Do NOT auto-commit; leave for user review.
- Compile-verify before any push (prior broken-main incident).
- Update the upcoming-release what's-new JSON only if any user-visible change ships from this (pure refactor → skip).
