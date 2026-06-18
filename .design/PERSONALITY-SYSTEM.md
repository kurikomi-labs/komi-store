# Personality Design System — full re-architecture plan

Rebuild `core:presentation` around **personalities**. One global `Personality` (sealed) owns the whole visual language; swapping it reskins the app. This supersedes the old Material-theme stack and the dropped-Cookie leftovers. Accepts a full app-theme rewrite.

## Locked decisions
- **Pure sealed** `Personality` (Manga, Classic). Token-only components read the common contract directly; structurally-divergent components do exhaustive `when(personality)`. Caller API stays neutral (`KomiHeadline(text)`, never `marker = Stamp`).
- **Personality owns the visual language**: colors, type, shape, shadow, motion, status colors, component recipes. **Spacing = one shared neutral scale** (not per-personality).
- **No global color package, no `vocabulary/` package.** Components live in `components/`, foldered by category. Personalities + accent + tweaks replace the concept of "themes".
- **Manga = the direction**; **Classic = clean Material 3 Expressive** (NOT the Cookie current-app look — no squiggles/wonky-squircle/wax-seal). Classic components are thin wrappers over `MaterialExpressiveTheme` styled by the classic palette; `is ClassicPersonality ->` branches mostly defer to M3 defaults. Cookie dropped entirely.

### vocabulary/ remnant fates (all consumer-coupled → execute on the Phase 6 sweep, NOT now)
- DELETE (Cookie decoration; neither personality wants it): `Squiggle` (16 uses — the blocker), `CookieShape` (3), `WaxSeal` (1), `VersionStack` (2), `AppAccent` + `AppAccentResolver` (per-app accent — personalities own accent), `Freshness` + `FreshnessState` (coupled to dying `Tokens`).
- KEEP + relocate: `PlatformGlyph` (functional OS icons, personality-agnostic) → `components/media/`.
- Already deleted (Phase 1, 0 uses): DownloadWeight, FreshnessRing, Heartbeat, LicensePosture, PermDot, SignalBars, TopicGlyph, VersionDelta.
- **Material bridge stays** as the migration lifeline (see Strategy).

## The mess, by the numbers (why this is needed)
Three generations of design code coexist:
1. **Old Material theme** — `theme/tokens/{Schemes,EmberPalette,GhsAccents,Tokens,Radii}`, `theme/{Theme(GithubStoreTheme),DynamicColorScheme,AnimateColorScheme,Type}`. Heavily coupled: `GithubStoreTheme` in 14 files, **`MaterialTheme.colorScheme` read directly in 122 files**, `WonkySquircleShape` in 13.
2. **Dropped-Cookie leftovers** — `vocabulary/` "silent vocabulary" (`FreshnessRing/Heartbeat/WaxSeal/PermDot/SignalBars/DownloadWeight/LicensePosture/TopicGlyph/Freshness*/VersionDelta/Squiggle`) = **0 external uses (dead)**; `VersionStack`(2)/`PlatformGlyph`(2)/`CookieShape`(3)/`AppAccent`(1) lightly used; `color/` per-app avatar accent; `cards/WaxSealTrustCard`.
3. **New Manga personality** — `personality/`, `KomiText`, `KomiHeadline`.

So: a lot of dead/cheap weight to delete, and one genuinely large rewrite — the 122 `colorScheme` readers.

## The Personality contract (target)
```kotlin
sealed interface Personality {
    val colors: PersonalityColors   // roles + success / warning (error already there)
    val type: PersonalityType
    val shape: PersonalityShape      // border widths (panel/button/chip), corner, skew
    val shadow: PersonalityShadow    // offsets, blur, pressTranslate
    val motion: PersonalityMotion    // expanded: durations + easings
}
// MangaPersonality / ClassicPersonality data-class subtypes carry their own structural extras
// (Manga: headlineMarker, screentone, starburst, inkedIcons, panelTilt).
```
**No per-component recipe bundle.** Per-component divergence = `when(personality)` inside the component (pure sealed) + reading the shared token groups above + the subtype's own fields. A `PersonalityComponents` field would re-introduce the dropped recipe pattern, balloon per component, and couple the contract to the component list.

Shared-neutral (NOT on Personality): `Spacing` scale, layout locals.

## Target `core/presentation/` structure
```
personality/        design-system SSOT
  Personality · MangaPersonality · ClassicPersonality · LocalPersonality · PersonalityThemeProvider
  model/   PersonalityColors · Type · Shape · Shadow · Motion · MotionLevel
  manga/   MangaColors · Fonts · Paper · Accent · HeadlineMarker · decoration/{InkModifiers,StarburstShape}
  classic/ ClassicColors · ClassicFonts
  utils/   PersonalityPreview
spacing/            shared neutral Spacing scale (the only non-personality token)
components/         EVERY component, foldered by category (see inventory)
  text/ surfaces/ buttons/ inputs/ chips/ loaders/ navigation/
  overlays/ cards/ media/ section/ markdown/ adaptive/ feedback/
locals/             keep — LocalContentWidth · LocalBottomNavigationHeight · LocalScrollbarEnabled · SharedTransitionLocals
model/              keep — GithubRepoSummaryUi · DiscoveryRepositoryUi · GithubUserUi
utils/              keep — mappers · formatters · ObserveAsEvents · ConstrainedContentWidth · system-bars · scroll helpers

DELETE  color/ (AvatarColorStore · DominantColorMath · RememberAvatarColor — per-app avatar accent)
DELETE  vocabulary/ (Komi → components/text/; dead Cookie primitives → gone)
RETIRE  theme/  (see fate table)
```

### theme/ fate
| file | fate |
|---|---|
| `tokens/{Schemes,EmberPalette,GhsAccents,Tokens,Radii}` | DELETE — personalities own color |
| `Theme.kt` (GithubStoreTheme), `DynamicColorScheme`, `AnimateColorScheme` | DELETE — replaced by `PersonalityThemeProvider` |
| `Type.kt` | DELETE — personalities own type |
| `shapes/{WonkySquircleShape,CornerRadii}` | DELETE after its 13 users migrate to personality shapes |
| `StatusColors` | FOLD → `PersonalityColors` (success/warning) |
| `MotionTokens`, `ThresholdSet` | FOLD → expanded `PersonalityMotion` |
| `SpacingTokens` | KEEP → move to `spacing/` (shared neutral) |
| `Locals.kt` | MERGE into `locals/` |

## Component inventory → new home
| New (foldered) | From (existing) | Action |
|---|---|---|
| `text/KomiText·KomiHeadline·KomiTextRole` | vocabulary/ | MOVE |
| `surfaces/KomiPanel` | ExpressiveCard, CompactCard, RowCard, RepoStripeCard | NEW + consolidate |
| `buttons/KomiButton` (+ size/variant) | buttons/{GhsButton,Primary,Outline,Tinted,Icon} | REWRITE → personality-driven |
| `inputs/KomiTextField` | inputs/GhsTextField | REWRITE |
| `chips/KomiChip` | chips/{Add,Filter,Platforms,Stat}, RepoRankChip, RepoReleasePill, FloatingPill | REWRITE + consolidate |
| `loaders/KomiLoading` + skeletons | — | NEW (manga starburst spinner / classic spinner) |
| `navigation/KomiTopBar·KomiBottomNav·KomiSectionHeader` | chrome/GhsHomeTopBar, section/SectionHeader, hub/GhsSectionHeader | REWRITE |
| `overlays/KomiSheet·KomiDialog·KomiDropdown·KomiToast·Banner` | overlays/{GhsBottomSheet,GhsFullScreenSheet,GhsConfirmDialog,GhsDropdownMenu,GhsToast}, section/Banner | REWRITE |
| `cards/KomiRepoCard·StatsSlab` | RepositoryCard, cards/{LeadHeroCard,VitalSignsGrid} | REWRITE (the big one) |
| `media/KomiImage·KomiAvatar` | GitHubStoreImage, RepoAvatarTile, RepoIdentity | REWRITE (inked-icon recipe) |
| `markdown/*` | markdown/ | KEEP, restyle later |
| `adaptive/*` | adaptive/ (list-detail layout) | KEEP (layout, not visual) |
| `feedback/*` | announcements/ | KEEP/restyle (or push to feature) |
| — | cards/WaxSealTrustCard, hub/GhsEntryRow | DROP / reassess (Cookie) |

## Strategy — app never breaks (Material bridge)
`PersonalityThemeProvider` seeds `MaterialTheme.colorScheme` from `personality.colors`. So all **122 `colorScheme` readers keep rendering** (now with personality colors) with zero edits. Migration is therefore incremental, never big-bang. Old `theme/` tokens are deleted only after their readers are gone.

## Phases
0. **Lock plan** (this doc).
1. **Demolition** — delete dead Cookie primitives (verify no internal use), kill `color/`, move Komi → `components/text/`, retire empty `vocabulary/`. App still builds.
2. **Contract** — expand `Personality`: status colors, `PersonalityComponents` recipes, expanded motion; add shared `spacing/`. Fill Manga + Classic.
3. **Component build** — foldered `components/`, one at a time, each `@Preview` (manga + classic) + reviewed: surfaces → buttons → chips → inputs → loaders → navigation → overlays → media → cards (RepoCard last/biggest).
4. **Go live** — `PersonalityThemeProvider` at `Main.kt` (replace `GithubStoreTheme`); personality + paper + accent + motion persisted in `TweaksRepository`; selection UI in Tweaks.
5. **Enforcement** — detekt `ForbiddenImport` banning raw `androidx.compose.material3.{Text,Button,Scaffold,…}` outside `components/` + `personality/`.
6. **Sweep** — migrate screens + old components off raw Material → new `components/`. Per feature module. `:composeApp` compile each step.
7. **Final retire** — delete `theme/` tokens, `WonkySquircleShape`, the Material bridge if going pure (decide at the end). Subset Zen Kaku fonts (4.6MB → ~150KB).

## THE RULE — personality-driven, never personality-hardcoded
Components derive ALL look from the active personality, default neutral; a manga-only flourish leaking into another personality is a bug. Token components read the contract (no `when`); divergent ones `when(personality)`. Caller API neutral.

## Contrast (WCAG AA, locked)
Accent = **fill only**, never small foreground text — paint `primary` as fill with `onPrimary` over it. Corrected values live in `MangaColors` (Day `onSurfaceVariant #695F50`; Nord `error/onError #E5818A`/`#20242E`).

## Risks / notes
- 122 `colorScheme` readers → the bridge is mandatory; don't remove Material until the sweep is done.
- `WonkySquircleShape` (13) + `GithubStoreTheme` (14) are live — delete only post-migration.
- Fonts: Zen Kaku 4.6MB bundled; subset before release.
- PR cadence per memory: large PRs, atomic commits, compile each step.
