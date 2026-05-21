# GitHub Store — UI Build Spec (Design System Refresh)

> Branch: `feat/design-system-refresh`. Anchored against `/Users/rainxchzed/Downloads/handoff 4/` (DESIGN.md, tokens.json, patterns.md, design-system.md, prototype `.jsx` files). All Kotlin/Compose specs target Compose Multiplatform — `commonMain` unless noted. No code yet; this is the build contract.
>
> Voice: Silent Vocabulary (80%) + Expressive moments (20%). Friendly editorial. Honest data.
>
> Token references use the literal keys from `handoff 4/tokens.json` (e.g. `palettes.nord.light.primary`, `shape.radii.card-lg`, `status.freshness.hot`). DESIGN.md citations look like `DESIGN.md §7.3`.

---

## 0. Glossary & conventions

- **T** — current palette+mode token bundle (`bg`, `surface`, `surface2`, `ink`, `ink2`, `outline`, `primary`, `tintP`, `success`, `successT`, `danger`, `dangerT`). Defined per palette × {light, dark} in `tokens.json#palettes`.
- **Status** — palette-independent semantic colors (`tokens.json#status.{freshness,wax,perm,trend}`). Same hex in every palette.
- **Accent** — per-repo brand color triplet `{c, lt, dt}` (DESIGN.md §2.4). Travels with the repo; never mutates with palette.
- **dp** — Compose `Dp` units. Compose tokens consume the integer "primary"/"secondary" pair from `tokens.json#shape.radii.*` (the `css` strings are reference only).
- **Wonky** — fully asymmetric `RoundedCornerShape` with four distinct corner radii (DESIGN.md §5.2). Implemented as `AbsoluteRoundedCornerShape(topStart=…, topEnd=…, bottomEnd=…, bottomStart=…)`.
- **Mono** — JetBrains Mono. Used **only** for technical artifacts (version tags, hashes, file sizes, package names). Never for prose.
- **Primary surface** — the most important interactive thing on the screen. One per surface (DESIGN.md §3, §7.1, design-system.md §11.1).
- **`GhsTheme`** — new theme composable (the wrapper that replaces today's `GithubStoreTheme`). Wraps `MaterialExpressiveTheme` and publishes `LocalGhsTokens`, `LocalGhsStatus`, `LocalGhsTypography`, `LocalGhsShapes`, `LocalGhsAccent`.

Compose Multiplatform deltas vs the JSX prototypes:
- No CSS `box-shadow` strings — translate to `Modifier.shadow(elevation, shape, …)`.
- No `linear-gradient` for chrome — only for the Lead-hero radial bloom (DESIGN.md §2.5 explicitly allows it) and the Profile identity card (patterns.md §"Hero card with gradient", `tintP → surface` only).
- No `borderRadius: 'A B C D / E F G H'` 8-value strings. Compose can express that with `GenericShape`; for the wonky squircle we use a 4-corner `AbsoluteRoundedCornerShape` and accept that the second axis is symmetric. The visual delta is negligible at the sizes we use.

---

## 1. Primitive catalogue (Silent Vocabulary + Expressive)

Every primitive lives in `core/presentation/src/commonMain/.../theme/primitives/`. Public composables, no business state, no Koin. Inputs are typed pure data.

Cross-platform rule for all primitives that handle pointer input: the **touch target** is min 48dp on Android; Desktop uses `Modifier.pointerHoverIcon(PointerIcon.Hand)` for hover-actionable primitives only (rings/dots are pure decoration). Where a primitive accepts a `tooltip: String?` Desktop uses `TooltipBox`; Android ignores it.

### 1.1 FreshnessRing

| Field | Value |
|---|---|
| Answers | "How recent is this release?" (DESIGN.md §4.1) |
| Inputs | `daysSinceRelease: Int?`, `size: Dp = 56.dp`, `strokeWidth: Dp = 2.5.dp`, `accent: Color? = null`, `content: @Composable BoxScope.() -> Unit` |
| Geometry | Outer arc starts at -90°, sweep = `360f * fraction`. Inner ring (full circle) drawn at `outline @ 0.35 alpha` as the unfilled track. Inset 1.5dp between ring and content. |
| State buckets | From `tokens.json#thresholds.freshness`: `hot 0–3d`, `fresh 4–30d`, `warm 31–90d`, `cool 91–365d`, `dormant >365d`. Fractions `1.00 / 0.78 / 0.55 / 0.30 / 0.12`. Colors from `status.freshness.{state}`. |
| Accent rule | When `accent != null`, the *outer* arc uses `accent.c`; the bucket color appears as a small chord segment at the tail to disambiguate. Default (no accent) uses bucket color for the whole arc. |
| States | default; loading (full ring `outline @ 0.5`, slow pulse 1.6s); null-input (no ring, content alone, no halo). |
| Behavioral rules | Always paired with an avatar inside (DESIGN.md §7.4). Never used as a button. **Never** stacked with Heartbeat in the same row (DESIGN.md §6.1). |
| Cross-platform | Compose `Canvas` draws the arc; the content slot is a `Box` accepting an image or letter avatar. |

### 1.2 Heartbeat

| Field | Value |
|---|---|
| Answers | "Is this project alive?" |
| Inputs | `daysSinceUpdate: Int?`, `size: Dp = 8.dp`, `showHalo: Boolean = true`, `tint: Color? = null` |
| Geometry | Center dot `size`. Halo circle scales 1.0 → 2.4 with opacity 0.45 → 0 (`tokens.json#motion.heartbeat`). |
| State buckets | From `tokens.json#thresholds.maintenance`: `active ≤1d → 1.4s`, `recent ≤7d → 2.4s`, `quiet ≤30d → 4.2s`, `dormant >30d → no animation`. |
| Colors | `active/recent → status.freshness.fresh`; `quiet → status.freshness.warm`; `dormant → status.freshness.dormant` (static, no halo). |
| States | default; dormant (no animation); reduced-motion (use static dot, regardless of bucket — read `LocalConfiguration` on Android, `Toolkit.getDefaultToolkit().getDesktopProperty("awt.dynamicLayoutSupported")` is not reliable; expose a `GhsMotionPreference` Local instead, defaulting to "full"). |
| Behavioral rules | **Never** in dense list rows alongside FreshnessRing (DESIGN.md §6.1, §15). Allowed: detail vital signs, library row when ring is suppressed, polling indicator in OAuth full-screen sheet (§16.5 — repurposed as "this flow is alive"). |
| Cross-platform | `rememberInfiniteTransition` for both desktop & android. |

### 1.3 StarTier

| Field | Value |
|---|---|
| Answers | "How big a deal is this?" |
| Inputs | `stars: Int`, `size: Dp = 11.dp`, `tint: Color? = null` (defaults to `T.ink`) |
| Geometry | 5 stars in a row, 1.5dp gap. Filled `tier` stars; the rest outlined at `0.35 alpha`. |
| Buckets | `tokens.json#thresholds.stars`: 0→1, 1k→2, 10k→3, 50k→4, 100k→5. |
| Numeric tail | Optional `showCount: Boolean = true` → appends count in `mono 12` (Inter Tight tabular numerals — design-system.md §5.3). Uses `CountFormatter` (already exists at `core/presentation/.../utils/CountFormatter.kt`). |
| States | default; disabled (50% alpha, used in hidden-repos screen). |

### 1.4 WaxSeal

| Field | Value |
|---|---|
| Answers | "Can I trust this binary?" |
| Inputs | `state: WaxState` = `Intact | Cracked | Open`, `size: Dp = 36.dp`, `fingerprint: String? = null` |
| Geometry | Octagonal stamp shape (DESIGN.md §7.8). Cracked state has a jagged centre fissure (drawn as `Path` with two angled lines + small spalls). Open state uses a `1.5dp dashed` stroke on the outer outline only. |
| Colors | `Intact → status.wax.intact (#8B4A2B)` fill, ink2 outline; `Cracked → status.wax.cracked (#B83A2C)` fill, white check that becomes a red cross; `Open → transparent fill, status.wax.open (#8E8E8E)` dashed outline. |
| States | The cracked state is the **only** place the app uses red aggressively (DESIGN.md §7.8). Must visibly scream — full `T.danger` border on the containing card and a wax-seal toast (§toast). |
| Behavioral rules | Anchored top-right of the install panel on Desktop, slightly rotated -6° for "stamped" feel (DESIGN.md §8.2/§7.8). Top of the card on Android (full width). Always paired with the signing fingerprint in `mono 11`. |
| Fallbacks | No `signingFingerprint` on `InstalledApp` → render `Open` state with caption "Unsigned" (`T.ink2`). |

### 1.5 VersionDelta

| Field | Value |
|---|---|
| Answers | "How risky is this update?" (`patch | minor | major`) |
| Inputs | `delta: SemverDelta = Patch | Minor | Major | Unknown`, `tint: Color? = null` |
| Geometry | `Patch` = one dot (4dp). `Minor` = two dots (4dp, 3dp gap). `Major` = filled bar 14×4 with a 1dp slash at 60° through it. |
| Colors | tint defaults to `T.primary` for patch/minor; `T.danger` for major. Unknown → grey dot at `T.ink2`. |
| Behavioral rules | Computed from `installedVersion` vs `latestVersion` via `VersionMath` (exists). When parse fails → `Unknown`. |
| Cross-platform | Pure Canvas. |

### 1.6 VersionStack

| Field | Value |
|---|---|
| Answers | "How far behind am I?" |
| Inputs | `skippedCount: Int`, `maxBars: Int = 6`, `barHeightStep: Dp = 1.5.dp`, `accent: Color? = null` |
| Geometry | A row of N vertical bars, each `width 2.5dp`, growing in height by `barHeightStep` per bar (4dp, 5.5dp, 7dp, …). Bars beyond `maxBars` collapse into a single bar with `+` suffix. |
| Colors | Bars use `accent.c` if provided, else `T.primary`. |
| Use sites | Apps tab badge (Android bottom nav, DESIGN.md §9.1), update banner inline glyph (DESIGN.md §11.2 / patterns.md §"Update banner"), Library row trailing indicator. |
| Fallbacks | `skippedCount == 0` → composable returns nothing (no empty render). When the data layer doesn't expose "skipped" history, count = `if (isUpdateAvailable) 1 else 0`. |

### 1.7 PermDot

| Field | Value |
|---|---|
| Answers | "How dangerous are the permissions?" |
| Inputs | `risk: PermRisk = Low | Moderate | High`, `size: Dp = 10.dp`, `withHalo: Boolean = false` |
| Geometry | Single dot. Halo = 1px ring `1.5dp` outside the dot at 35% alpha. |
| Colors | `tokens.json#status.perm.{low,moderate,high}`: `#6BA068 / #C49652 / #B83A2C`. |
| Use sites | APK Inspect screen permission groups (Android only); vital signs grid "PERMISSIONS" tile on Detail. |
| Fallback | Not an APK or no permission breakdown → tile shows "—" instead of a dot. |

### 1.8 PlatformGlyph

| Field | Value |
|---|---|
| Answers | "Will it run on my OS?" |
| Inputs | `platform: DiscoveryPlatform`, `supported: Boolean`, `size: Dp = 18.dp` |
| Geometry | Monochrome silhouettes (DESIGN.md §4.1) — phone, window, apple, penguin. Filled when `supported`, `1.2dp` dashed stroke when not. |
| Source enum | `core/domain/.../DiscoveryPlatform` (`Android, Macos, Windows, Linux`). |
| Colors | Always `LocalContentColor` (defaults to `T.ink`). Never carries the per-app accent. |

### 1.9 TopicGlyph

| Field | Value |
|---|---|
| Answers | "What kind of app is this?" |
| Inputs | `topic: String`, `size: Dp = 14.dp` |
| Geometry | Micro-pictograms from `tokens.json#topicGlyphs.supported`: `self-hosted, mobile, photo, video, book, manga, key, audio, backup, reader, cross-platform, cloud`. |
| Aliasing | `tokens.json#topicGlyphs.topicAliases` handles `password-manager → key`, etc. If neither match → return null Composable (do not render — DESIGN.md §4.2 "or omit"). |
| Behavioral rules | At most **3 per card** (DESIGN.md §4.2). Always monochrome `T.ink2`. Never colored. |

### 1.10 SignalBars

| Field | Value |
|---|---|
| Answers | "How fast is this mirror?" |
| Inputs | `tier: Int (0..4)`, `size: Dp = 14.dp` |
| Geometry | 4 ascending bars (3dp wide, gap 1.5dp, heights 4/7/10/13dp). Filled = `T.primary`. Unfilled = `outline @ 0.5`. |
| Use sites | `MirrorPickerScreen` rows; download speed indicator in toast. |
| Fallbacks | Mirror health not measured yet → tier 0 (all unfilled). Add caption "Untested" in `caption` text. |

### 1.11 DownloadWeight

| Field | Value |
|---|---|
| Answers | "How widely adopted?" |
| Inputs | `downloads: Long`, `maxSize: Dp = 14.dp`, `tint: Color? = null` |
| Geometry | Filled circle whose radius scales `log10(downloads + 1)` mapped to `[4.dp, maxSize/2]`. Caps at maxSize. |
| Colors | `tint ?: T.ink2`. |
| Behavioral rules | Pair with `mono` count text. Never use alone in dense rows — `StarTier` already does adoption. Reserve for Detail "vital signs" and Developer profile. |
| Fallbacks | `downloads == 0` → render only the caption "—". (Forgejo repos may have 0 because the asset aggregation is best-effort.) |

### 1.12 LicensePosture

| Field | Value |
|---|---|
| Answers | "Is this restrictive?" |
| Inputs | `spdxId: String?`, `size: Dp = 16.dp` |
| Geometry | A small tile (square, radius 3dp): filled `©` glyph for copyleft, dashed-border `·` glyph for permissive, no glyph for unknown. |
| Buckets | `tokens.json#licenses.copyleft` and `licenses.permissive`. |
| Colors | Foreground `T.ink2`. Filled bg uses `T.surface2` for copyleft (visual weight), transparent for permissive (visual lightness). |
| Fallbacks | Not in either bucket → return null. |

### 1.13 CookieShape (Expressive)

| Field | Value |
|---|---|
| Use sites (3 total) | Brand "G" mark (top-left on every primary surface); user identity tile (Profile); active bottom-nav tab on Android. DESIGN.md §4.3, §15. |
| Path | `tokens.json#shape.cookie.path` (9-petal organic). ViewBox `0 0 100 100`. Implement as a `Shape` derived from `androidx.compose.ui.graphics.Path` so it can be used as `clip` + as the path of a `BorderStroke` (Compose: `GenericShape` constructor returning a `Path` scaled to size). |
| Inputs | `letter: String? = null`, `tint: Color = T.primary`, `size: Dp = 32.dp`, `contentColor: Color = Color.White` |
| States | Default (filled `tint`); pressed (`tint @ 0.85`); disabled (replaced with circle, not Cookie — Cookie is identity-only). |
| Behavioral rules | **Never multiple Cookies adjacent** (DESIGN.md §4.4). Desktop drawer's brand mark + user tile sit at opposite ends of the drawer — that satisfies the rule. |

### 1.14 Squiggle (Expressive)

| Field | Value |
|---|---|
| Use sites | Section headings; bottom-sheet headings; confirm-dialog headings; diagnostics-card separators. **One per heading** (DESIGN.md §15). |
| Path | `tokens.json#shape.squiggle.path`. Aspect 40×5, stroke 1.6px. |
| Inputs | `width: Dp = 40.dp`, `color: Color = T.primary`, `opacity: Float = 0.6f` |
| Cross-platform | Pure Canvas. |

---

## 2. Component catalogue

All components live in `core/presentation/.../components/`. Shapes referenced as `GhsShapes.X` where X is one of `chip, row, cardSm, card, cardLg, hero, heroLg, wonky, wonkyAlt, wonkySearch` — these mirror `tokens.json#shape.radii.*` and `shape.wonkySquircle.*`.

### 2.1 Buttons

| Variant | Use | Visual spec |
|---|---|---|
| `GhsButtonPrimary` | Install, Update, Open, Sign in | Wonky squircle `GhsShapes.wonky`. Height `48.dp` (Android touch), `40.dp` desktop. Padding `horizontal 18.dp, vertical 10.dp`. Background `T.primary`. Content color `Color.White`. Elevation: `Modifier.shadow(8.dp, GhsShapes.wonky, ambientColor = T.primary.copy(alpha=0.4f), spotColor = T.primary.copy(alpha=0.6f))`. Press: scale 0.97, 100ms. Disabled: `T.primary @ 0.4`, no shadow. |
| `GhsButtonAccent` | Update inside an app-context card | Same as Primary but `background = accent.c`, shadow uses `accent.c`. Used for the in-card Update CTA (patterns.md §"Update banner"). |
| `GhsButtonTinted` | Get, Read more, See all | `GhsShapes.card`. Background `T.tintP`. Content `T.primary`. No shadow. Height `40.dp` / `36.dp`. |
| `GhsButtonOutline` | Inspect, Refresh, Cancel | `GhsShapes.card` or `RoundedCornerShape(50%)` (pill) for nav-row "Cancel". 1.dp border `T.outline`. Background transparent. Content `T.ink`. |
| `GhsButtonDanger` | Destructive confirm | Wonky. Background `T.danger`. Content white. Same shadow recipe but with `T.danger`. |
| `GhsIconButton` | Back, share, favourite, more, dismiss | 36×36.dp transparent box (Desktop) / 48×48.dp (Android). Centered glyph `20.dp`. Tap ripple `T.ink @ 0.08` (design-system.md §10.1). |

State matrix for every button:

| State | Bg delta | Content delta | Border / shadow |
|---|---|---|---|
| default | base | base | as defined |
| hover (desktop) | `+5%` lighten / `−5%` darken | unchanged | shadow `+2.dp` for Primary only |
| pressed | `-8%` lightness | unchanged | shadow `-2.dp`, scale `0.97f` 100ms |
| focused (keyboard) | base | base | 2.dp ring `T.primary @ 0.6`, offset `2.dp` |
| disabled | `α 0.4` | `α 0.6` | no shadow |

### 2.2 Chips

| Variant | Visual spec |
|---|---|
| `GhsChipFilter` (on) | `GhsShapes.chip`, padding `H 12.dp V 5.dp`, font `Inter Tight 12 / 600`. Bg `T.tintP`, content `T.primary`, 1.dp border `T.primary @ 0.33`. Trailing `×` 14.dp when removable. |
| `GhsChipFilter` (off) | Same shape. Bg transparent. Content `T.ink`. 1.dp border `T.outline`. |
| `GhsChipAdd` | Dashed 1.dp border `T.outline`. `+ Add filter`. Same height & font as filter chip. |
| `GhsChipPill` | `RoundedCornerShape(50%)`. Used only for the search-suggestion recent-query chips. |

Touch target: chip rows on Android pad an extra 6.dp vertically (so visual 26.dp height → 38.dp touch row). Hit testing extends to the gap between chips.

### 2.3 Cards

| Variant | Visual spec |
|---|---|
| `GhsCardLead` (hero with bloom) | Shape `GhsShapes.wonkyAlt`. Padding `20.dp 22.dp 18.dp`. Background `T.surface` with a `RadialGradient` overlay (center 30% from top-left, radius `card.maxWidth`, color stops `[accent.lt @ 0.6 → Color.Transparent]`). Border `1.dp T.outline @ 0.4`. Soft shadow `0 12.dp 32.dp -12.dp T.ink @ 0.18`. Cap width on Desktop at `LocalContentWidth` from `core/presentation/.../locals/LocalContentWidth.kt`. |
| `GhsCardCompact` | Shape `GhsShapes.card`. Padding `14.dp`. Background `T.surface`. 1.dp border `T.outline`. Soft shadow `0 1.dp 0 ink@0.04, 0 8.dp 22.dp -16.dp T.ink @ 0.18` (design-system.md §8). Internal layout per DESIGN.md §7.3 (avatar+ring | name+secondary; description 2-line clamp; topic glyphs row; dashed divider; StarTier + count, platform silhouettes). |
| `GhsCardListRow` | Shape `GhsShapes.row`. Padding `H 12.dp V 10.dp`. Bg `T.surface`. No border (rely on row separators). |
| `GhsCardInstall` (Detail) | Shape `GhsShapes.heroLg`. Padding `18.dp`. Bg `T.surface`. Holds: Primary CTA full-width, secondary asset selector, wax seal anchored top-right (`offset(x=8.dp, y=-8.dp)` + `rotate(-6f)`). |
| `GhsCardIntegrity` | `T.successT` bg, 1.dp `T.success` border, white-on-success check badge. Used as inline alternative for wax-intact in compact contexts. |
| `GhsCardHeroGradient` (Profile) | `linear-gradient(135deg, T.tintP 0%, T.surface 60%)`. Shape `GhsShapes.heroLg`. (patterns.md §"Hero card with gradient" — `tintP → surface` only.) |

### 2.4 Section header

```
[Glyph 20dp]  [Fraunces italic 22/600  -0.02em]  [· sub-count caption ink2]      [See all ›]
              [~~~ Squiggle 36–42dp wide, 1.6dp stroke, primary @ 0.6 ~~~]
```

| Spec | Value |
|---|---|
| Height | Title row 28.dp + squiggle 8.dp + bottom pad 12.dp |
| Glyph + title gap | 8.dp |
| Squiggle | Aligned under the title's first 36–42dp |
| `See all ›` | `GhsButtonTinted` size-small, height 28.dp, padding `H 10.dp V 4.dp` |
| Meta variant | If section is meta-label not editorial: drop italic + squiggle, use `H3-meta` style from `tokens.json#typography.scale.h3-meta` (Inter Tight 700, uppercase, tracking 0.06em). |

### 2.5 Banner

| Spec | Value |
|---|---|
| Shape | `GhsShapes.card` |
| Bg / border (4 variants) | `info: T.tintP / T.primary@0.33`, `success: T.successT / T.success@0.33`, `warn: accent.lt / accent.c@0.33`, `error: T.dangerT / T.danger@0.33` |
| Padding | `H 12.dp V 10.dp` |
| Layout | `[Glyph 18.dp] [Text Inter Tight 13/500 + optional mono 12 detail] [Action button optional] [× dismiss 18.dp]` |
| Use sites | clipboard banner (Home), update banner (Apps, Detail), integrity warnings (Detail), rate-limit notice (any screen) |

### 2.6 Vital signs 2×2 grid (Detail)

| Spec | Value |
|---|---|
| Container | 2 cols × 2 rows, gap `12.dp`, each tile `GhsShapes.cardSm`, bg `T.surface2`, padding `12.dp` |
| Tile order (fixed) | `RELEASED · MAINTAINED · STARS · PERMISSIONS` (DESIGN.md §7.7) |
| Tile internal | Glyph (22.dp height) → value `Fraunces italic 600 / 13.sp` colored to the signal → label `caption uppercase 9.5.sp`. |
| Signal colors | RELEASED uses `status.freshness.*`. MAINTAINED uses heartbeat color. STARS uses `T.ink`. PERMISSIONS uses `status.perm.*`. |
| Empty data | Permissions on a non-APK repo → render "—" centered, no glyph; label stays. |

### 2.7 Wax-seal trust card

Detail screen (DESIGN.md §7.8). Spec already covered in §1.4 visual; container is `GhsCardInstall`'s top-right anchor (Desktop) or a separate `GhsCardCompact` with the wax seal as the leading 44.dp glyph (Android).

### 2.8 Bottom sheet

Compose's `ModalBottomSheet`. DESIGN.md §16.1.

| Spec | Value |
|---|---|
| Shape | `RoundedCornerShape(topStart=24.dp, topEnd=18.dp, bottomEnd=0.dp, bottomStart=0.dp)` (wonky top corners only) |
| Bg | `T.surface`. Optional 1.dp top border `T.outline`. |
| Drag handle | 36×4.dp pill, `T.ink2 @ 0.3`, top margin 8.dp |
| Heading | Fraunces italic 20 + Squiggle below. Top pad 12.dp. |
| Action row | Sticky bottom, padding 16.dp. Cancel outline (left), primary wonky (right). LTR. |
| Scrim | `T.ink @ 0.5` |
| Animation | Slide-up 240ms ease-out; scrim 180ms fade |
| Dismiss | tap scrim **unless** the sheet hosts an irreversible action |

Use sites: asset picker (Details), mirror picker, language picker, library import wizard, asset filter chooser.

### 2.9 Confirm dialog

DESIGN.md §16.2 / patterns.md §"Confirmation dialog". Use `BasicAlertDialog`.

| Spec | Value |
|---|---|
| Shape | Wonky `GhsShapes.wonkyAlt`. Max-width 320.dp mobile / 400.dp desktop. |
| Bg | `T.surface`. Scrim `T.ink @ 0.55`. |
| Optional context glyph | Centered top, 36.dp. Picks from §1: WaxSeal cracked, PermDot red ring, CookieShape, VersionStack. |
| Heading | Fraunces italic 18, centered, weight 600. **Specific question form** ("Uninstall immich?", not "Are you sure?"). |
| Body | Inter Tight 13, `T.ink2`, max 3 lines. Explains *consequence*. |
| Actions | Right-aligned. Cancel (outline) left, Confirm (wonky primary or wonky danger) right. |
| Touch | Min 48.dp button height. |

### 2.10 Toast

DESIGN.md §16.3. Use Compose's `SnackbarHost` with a custom Snackbar composable that uses our wonky squircle.

| Spec | Value |
|---|---|
| Shape | `GhsShapes.wonky` |
| Bg / border | by variant (info/success/error/warn) same matrix as Banner §2.5 |
| Leading glyph | Mandatory. From silent vocabulary (DESIGN.md §16.3). |
| Body | Inter Tight 13/600 + optional mono in `T.ink2` |
| Position Android | Bottom-center, 84.dp from bottom (above gesture-nav). Width = `screen − 32.dp`. |
| Position Desktop | Bottom-right, 24.dp inset, max-width 380.dp. |
| Duration | info 3s, action 4s with "Undo", error 6s, **cracked-seal sticky until tap**. |
| Stack | Max 3 visible. Newer push older up. |

### 2.11 Full-screen sheet

DESIGN.md §16.5. Used for OAuth device flow, PAT entry, Library Imports wizard, Web-OAuth handoff "waiting" screen.

| Spec | Value |
|---|---|
| Layout Android | Full-screen Composable host inside the existing nav graph (`AuthenticationScreen`, `ExternalImportScreen`). |
| Layout Desktop | Modal-style dialog 480×640, wonky `GhsShapes.heroLg`. |
| Top bar | Back arrow (24.dp). No title. |
| Identity mark | 64–96.dp CookieShape `letter = "G"` (sign-in) or topic glyph (imports). |
| Heading | Fraunces italic 24 + Squiggle. |
| Numbered steps | Primary-tinted circle markers (`size 18.dp`, bg `T.tintP`, text `T.primary`). Step text Inter Tight 14/500. |
| Code reveal box | Mono 28–32, wonky border 1.5.dp `T.primary`, padding 16.dp. Tap → copy → toast. |
| Polling indicator | Heartbeat glyph (re-using §1.2) + caption "waiting" |
| Fallback CTA | Always offer PAT path as `GhsButtonOutline` at bottom. |

### 2.12 Dropdown menus

Used by: translation language picker (already in `details/components/LanguagePicker.kt`), sort order menus, palette picker in Tweaks, "more" `⋯` actions.

| Spec | Value |
|---|---|
| Shape | `GhsShapes.card` |
| Bg | `T.surface`, 1.dp border `T.outline`, shadow `0 10.dp 24.dp -12.dp T.ink @ 0.35` |
| Item | Height 40.dp. Padding `H 12.dp`. Optional leading glyph 16.dp, label Inter Tight 13, optional trailing mono detail or checkmark `T.primary`. |
| Hover (desktop) | bg `T.tintP @ 0.5` |
| Pressed | bg `T.tintP` |
| Selected | trailing `✓ T.primary` |

### 2.13 Bottom nav (Android only)

DESIGN.md §9.1 + MIGRATION.md (4 → 3 tabs + detached search).

| Spec | Value |
|---|---|
| Tabs (final order) | `Home`, `Search`, `Apps` (a.k.a. Library), `Profile` |
| Height | 64.dp + system gesture inset |
| Bg | `T.surface` with shadow `0 8.dp 24.dp -12.dp T.ink @ 0.32` |
| Inactive | Outline glyph 20.dp `T.ink2`, label Inter Tight 11/500 `T.ink2` below |
| Active | CookieShape (size 40.dp) bg `T.primary` behind a knocked-out white 20.dp glyph; label Fraunces italic 12/600 `T.primary` below |
| Apps tab badge | VersionStack at top-right (replaces M3 numeric badge) when `pendingUpdates > 0` |
| Tap state | Tap layer `T.ink @ 0.08`, 120ms |

(Desktop has no bottom nav — drawer §3 below.)

### 2.14 Desktop drawer (Desktop only)

Replaces the current "side rail / drawer hybrid" with the spec from DESIGN.md §8.1.

| Spec | Value |
|---|---|
| Width | 240.dp |
| Bg | `T.bg` (sits flush against window chrome) |
| Brand row | CookieShape (28.dp, "G", `T.primary`) + "GitHub Store" Inter Tight 14/600 |
| Search input | Wonky `GhsShapes.wonkySearch`, 1.dp `T.outline`, leading glyph, trailing `⌘K` mono caption. Tap → `SearchScreen`. |
| Nav item | Height 40.dp. Padding H 12.dp. Glyph 20.dp + label Inter Tight 14. Active: bg `T.tintP`, content `T.primary`, weight 600, shape `radD(13,10)` (`GhsShapes.row`). Inactive: transparent, content `T.ink`, weight 500. |
| User card (bottom) | CookieShape with user initial; primary fill. Subtitle in caption ink2 ("3 updates"). |
| Sticky bottom group | `Settings ⌘,`, `Shortcuts ?`. |

---

## 3. Screen-by-screen visual spec

The screens below are scoped to the existing modules. Files cited are `commonMain` unless noted. The "Data fields" column references actual domain models — `GithubRepoSummary`, `GithubRelease`, `InstalledApp`, `GithubUser`, `GithubUserProfile`, `Announcement`, `HostToken`, `MirrorConfig`, `ApkInspection`.

### 3.1 `feature/home/` — Discovery feed

Files: `HomeRoot.kt`, `HomeViewModel.kt`, `HomeState.kt`, `components/HomeFilterChips.kt`.

**Layout (vertical scroll, Android + Desktop):**

```
[Brand row Android: G-cookie · "GitHub Store" · User-cookie]   (Desktop: drawer handles brand)
[Search input (wonky) — tap → SearchScreen]
[Clipboard banner — if clipboard URL parses to a repo, conditional]
[Time-window chip row: Today · Week · Month · All]                (HomeFilterChips, keep)
[Lead release card · GhsCardLead with accent radial bloom]
[Section: Hot releases             squiggle]
[ → Horizontal scroll Android / 2-col grid Desktop of GhsCardCompact ]
[Section: Trending now             squiggle]
[ → Vertical list with #N rank chips on the left ]
[Section: Most popular             squiggle]
[ → Vertical list, rank in Fraunces italic 0.55 opacity ]
[Section: From your stars (auth required)   squiggle  ]
[ → Vertical list, APK-shipping starred repos]
```

| Component | Data |
|---|---|
| Lead release | `GithubRepoSummary` + freshest `GithubRelease.publishedAt`. Accent from §6. |
| Hot releases | `GithubRepoSummary[]` filtered by time window. |
| Trending now | Backend rank → `position #N`. If backend missing (DESIGN.md §11), use local sort proxy and drop the rank chip (don't fake). |
| Most popular | Sorted by `stargazersCount`. |
| From your stars | `StarredRepository`, scope: APK-shipping. Empty state if signed-out. |

Android vs Desktop deltas:
- Android: top bar (52.dp) with G-cookie left, U-cookie right.
- Desktop: drawer is persistent; right pane shows the same vertical list at `LocalContentWidth` (COMPACT 720 / WIDE 960 / EXTRA_WIDE 1200). The "Most popular" and "Trending" lists become 2-column grids when content width ≥ WIDE.

### 3.2 `feature/search/` — Search

Files: `SearchRoot.kt`, `SearchViewModel.kt`, `SearchState.kt`.

```
[Top bar: ← back · search input (wonky, autofocus) · ⋯ filters]
[Source toggle: GitHub | Codeberg | Custom forge]                 (NEW chip row)
[Filter chips: Platform (Android/Win/macOS/Linux/All) · Sort · Language]
[Recent queries (when empty) — chip cloud]
[Results list — GhsCardListRow]
```

| Component | Data |
|---|---|
| Source toggle | `RepositorySource` enum already exists. GitHub default. Codeberg defaults to `codeberg.org`. Custom forge opens dropdown picker of user-added hosts. |
| Result row | Avatar+FreshnessRing left, name (Fraunces italic) + owner caption, StarTier+count, PlatformGlyphs trailing. Tap → `DetailsScreen(sourceHost=…)`. |

Android vs Desktop:
- Android: input is the only thing in the top bar. Filters via a `⋯` menu opening a bottom sheet.
- Desktop: filters expand inline as a chips row under the input. Source toggle is a segmented control on the right.

### 3.3 `feature/details/` — Repo detail

Files: `DetailsRoot.kt`, `DetailsViewModel.kt`, `components/sections/{Header,About,Stats,WhatsNew,Owner,ReleaseChannel,ReportIssue,Logs}.kt`.

**Android layout (centered hero, stacked):**

```
[Top bar: ←   ↗ open external · ♡ favourite · ⋯]
[Hero: 92.dp avatar in FreshnessRing
       repo name (Fraunces italic 28)
       owner · TopicGlyphs (max 3)
       StarTier ★★★★★ + count · DownloadWeight mono]
[Install panel (GhsCardInstall):
   [Primary wonky CTA full-width — Install · 48 MB]
   meta row: PermDot · PlatformGlyph(arch) · SignalBars (mirror)
   wax seal card (top of section, full width on Android)]
[Vital signs 2×2: RELEASED · MAINTAINED · STARS · PERMISSIONS]
[About section preview (3 lines clamp) → "Read more"]
[What's new preview → "Show all versions"]
```

**Desktop layout (two-column):**

```
[Top bar: ← (when entered from Home/Search) breadcrumb · share]
[Hero block left-aligned 65% width]              [Right column 35% — wax seal card rotated -6°,
[Install panel — same as Android]                 vital signs 2×2, platform silhouettes]
[About + What's new tabs underneath]
```

| Component | Data |
|---|---|
| Hero | `GithubRepoSummary` + chosen `GithubRelease`. Accent from §6. |
| Install panel | Asset picker bottom-sheet, primary CTA varies per `SmartInstallButton` (already exists). Wax seal uses `InstalledApp.signingFingerprint` and an expected fingerprint (when previously installed) — currently the data layer doesn't store expected fingerprint for non-installed repos; **fallback: Open state with "Signed by maintainer" caption.** |
| Vital signs | RELEASED uses `releaseRecency` days; MAINTAINED uses `updatedAt` delta; STARS uses `stargazersCount`; PERMISSIONS uses `ApkInspection` if available (Android only), else "—". |
| About preview | First 3 lines of README. "Read more" opens inner About screen (next sub-section). |
| What's new preview | Latest `GithubRelease.description` first 3 lines + `JetBrains Mono` tag. |

**Detail inner — About / What's new (DESIGN.md §8.4):**

```
[Top bar: ← repo-name · [About | What's new] · 🌐 EN▾ (TranslationControls existing)]
[About: rendered README, multiplatform-markdown-renderer]
   OR
[What's new: split view]
   [Version rail (left)]  v2.7.5 · 2d ago   ← current
                          v2.7.4 · 1w ago
                          v2.7.3 · 2w ago
                          v2.7.0 [YOU]      ← installed badge
   [Selected version notes (right)]  Fraunces italic title, Inter Tight body, mono version tag
```

Version rail on Android collapses to a vertical list above the notes (single column). Tap a row → notes update inline.

### 3.4 `feature/apps/` — Library (installed apps)

Files: `AppsRoot.kt`, `AppsViewModel.kt`. Also covers `ExternalImportScreen`, `StarredPickerScreen` (both lives in same module).

**Android layout (single pane):**

```
[Top bar: Library Fraunces 28 · "5 apps · 1 update available"   [filter ⚙]]
[Update banner if any: VersionStack · vOLD → vNEW · primary wonky Update CTA — patterns.md §"Update banner"]
[Segmented chip row: Installed · Updates [1] · Pending]
[List of GhsCardListRow per installed app]
   [Avatar+FreshnessRing] [name (Fraunces italic) · mono version · heartbeat] [trailing: Open / Update / Inspect]
```

**Desktop layout (TWO-PANE — NEW for GHS):**

```
[Drawer 240]   [List pane 380]                       [Detail pane 660]
               [Header: "Library · 5 apps · 1 upd"]
               [Filter tabs: Installed/Updates/Stars/Recent]
               [Update banner inline (sticky top)]
               [Rows of installed apps]
                                                     [Hero (avatar+ring, name, meta)]
                                                     [Update/Open CTA]
                                                     [Install panel]
                                                     [About preview "Read more" → inner]
                                                     [Right column: wax seal, vitals]
```

The two-pane is **new** — propose adding a `LibraryDetailHost` composable that hosts either the empty state ("Select an app") or the Detail screen (reuse the existing `DetailsRoot` with a `embedded=true` prop that hides its own top bar). Selection state lives in `AppsViewModel` so deep links from Home/Search still route through the existing `DetailsScreen` graph entry.

| Data fields | Source |
|---|---|
| Row avatar+ring | `InstalledApp.repoOwnerAvatarUrl` + `latestReleasePublishedAt` (days delta) |
| Heartbeat | `lastUpdatedAt` for freshness, gated by row density — only show heartbeat when ring is suppressed (e.g. updates-only filter mode) |
| Trailing CTA | `hasActualUpdate()` extension already in `InstalledApp.kt` |
| Update banner | `pendingUpdates = installedApps.count { it.hasActualUpdate() }` |
| Inspect button | Android only (`isAndroid()`) |

ExternalImportScreen and StarredPickerScreen reuse `GhsCardListRow` with a leading checkbox (icon shell shape `GhsShapes.cardSm`, 24.dp, bg `T.tintP`, primary check).

### 3.5 `feature/profile/` — User profile

Files: `ProfileRoot.kt`, `ProfileViewModel.kt`, `SponsorScreen.kt` (kept as Sponsor route but **rebranded** — see don't-build list).

```
[Top bar: Profile Fraunces 28 · settings ⚙ → TweaksScreen]
[Identity card (GhsCardHeroGradient — patterns.md §"Hero card with gradient")]
   [Avatar in Cookie shape 64.dp · primary fill]
   [@username Fraunces italic 22] [Inter Tight 13 ink2 bio]
   [stats row: followers · following · public repos]   ← from GithubUserProfile
[Section: Activity (squiggle)]
   [Recently viewed → RecentlyViewedScreen]
   [Favourites → FavouritesScreen]
   [Starred (auth) → StarredReposScreen]
[Section: Connect (squiggle, MIGRATION.md "no donations")]
   [6-cell grid: GitHub · Mastodon · Bluesky · Discord · email · website]
   [Business inquiries row → opens default mail client]
[Section: App (squiggle)]
   [Tweaks → settings] [What's new history → WhatsNewHistoryScreen]
[Sign out → confirm dialog]
```

Android & Desktop: identical layout. Desktop respects `LocalContentWidth`.

### 3.6 `feature/dev-profile/` — Developer profile of an owner

Files: `DeveloperProfileRoot.kt`, `DeveloperProfileViewModel.kt`.

```
[Top bar: ← @owner]
[Identity card: avatar (NO cookie — owner is not "the user") + name + bio + follower count]
[Section: Repositories that ship binaries (squiggle)]
   [List of GhsCardListRow per repo]
[Section: Their stats (squiggle)]
   [DownloadWeight summed · total stars StarTier · repo count]
```

Data: `GithubUserProfile`.

### 3.7 `feature/tweaks/` — Settings

Files: `TweaksRoot.kt`, `TweaksViewModel.kt` (note: 57KB — large state), `components/{ToggleSettingCard, SectionText, CustomForgesDialog, ClearDownloadsDialog}.kt`, plus subfolders `hosttokens/`, `mirror/`, `feedback/`, `hidden/`, `skipped/`.

Section order (top to bottom):

1. **Appearance** (squiggle):
   - Two-axis theme picker (§7 below) — palette row + mode segmented control
   - Content width: `COMPACT / WIDE / EXTRA_WIDE` (existing) — segmented control on Desktop, dropdown on Android
   - Font theme (keep existing `FontTheme.CUSTOM/SYSTEM`)
2. **Sources** (squiggle):
   - Custom forges (`CustomForgesDialog` — keep) — list of `{host, label}` with Add/Edit/Delete
   - Per-host tokens → `HostTokensScreen` (existing route)
   - Mirror picker → `MirrorPickerScreen`
3. **Translations** (squiggle):
   - Provider segmented: Google · Youdao · LibreTranslate · DeepL · Microsoft (`TranslationProvider` enum existing)
   - Per-provider config card (sub-fields like API key, mirror URL) — gated by selection
4. **Library** (squiggle):
   - Installer preference (Android only) — System / Shizuku / Root
   - Skipped updates → `SkippedUpdatesScreen`
   - Hidden repositories → `HiddenRepositoriesScreen`
5. **Privacy** (squiggle):
   - Telemetry toggle
   - Proxy config
6. **About** (squiggle):
   - Version (mono) `1.8.3 (18)`
   - Announcements (existing) — route to `AnnouncementsScreen`
   - What's new history → `WhatsNewHistoryScreen`
   - Send feedback → diagnostics card (DESIGN.md §16.4) with "Email" + "GitHub issue" dual CTA

Each setting row uses `SetRow` (patterns.md §"Form / Settings group"): 36.dp tinted icon shell (bg `T.tintP`, content `T.primary`, shape `GhsShapes.cardSm`) + name + trailing toggle/value/chevron.

### 3.8 `feature/favourites/`, `feature/starred/`, `feature/recently-viewed/`

Presentation-only modules. All three:

```
[Top bar: ← Section title Fraunces 28 · count caption]
[Sort/filter chips (optional)]
[List of GhsCardListRow]
[Empty state: Squiggle illustration absent (DESIGN.md §"empty"), Fraunces italic headline, body, primary outline CTA "Browse home" / "Sign in" / "Search apps"]
```

Differences:
- **Favourites**: data from `FavouritesRepository.favouriteRepos`. Tap → DetailsScreen.
- **Starred**: data from `StarredRepository`, gated by auth. Empty state changes when signed-out → CTA "Sign in" → AuthenticationScreen.
- **Recently viewed**: data from `SeenReposRepository`. Sort by `lastSeenAt` desc. Swipe-to-clear on Android; clear-all button on Desktop.

### 3.9 `feature/auth/` — Sign in flow

Files: `AuthenticationRoot.kt`, `AuthenticationViewModel.kt`.

Renders as **Full-screen sheet** (§2.11). Three states:

| State | Visuals |
|---|---|
| Web OAuth handoff (default) | CookieShape "G" 80.dp; heading "Sign in with GitHub"; primary wonky CTA "Open in browser"; small caption "We'll redirect you back via githubstore://auth". Below: outline "Use device code instead" + outline "Use Personal Access Token". |
| Device flow (fallback) | DESIGN.md §16.5 layout: numbered steps, code reveal (mono 28), heartbeat polling indicator + "Polling github.com…" caption. Cancel button (back arrow). |
| PAT entry (last resort) | Single text field (mono), info banner about scopes, primary "Verify token" CTA. |

`AuthPath` (`Backend`|`Direct`) state already in `SavedStateHandle`; the UI just renders different bodies for the same shell.

---

## 4. Build order (phases)

Each phase ends with a runnable, testable artifact. Branch `feat/design-system-refresh` should accumulate atomic commits per the user's PR/commit sizing memory.

### Phase 0 — Tokens + theme + fonts

**Scope:**
- Add `core/presentation/.../theme/tokens/` directory:
  - `GhsTokens.kt` (data class with `bg`, `surface`, …)
  - `GhsStatus.kt` (palette-independent — wax, freshness, perm, trend)
  - `GhsPalette.kt` (enum NORD/CREAM/FOREST/PLUM) + `GhsPalettes` provider mapping each palette × {light,dark} from `tokens.json`
  - `GhsShapes.kt` (composable shapes: `chip, row, cardSm, card, cardLg, hero, heroLg, wonky, wonkyAlt, wonkySearch`)
  - `GhsTypography.kt` (Material `Typography` populated from `tokens.json#typography.scale`)
- Add fonts: Fraunces (italic 600), Inter Tight (400–700), JetBrains Mono (500–700). Use `composeApp/.../res/font/`. Configure variable-font axis when possible.
- Add Noto fallback families for CJK/Devanagari/Arabic/Hebrew (design-system.md §5.4) via `FontFamily.Default` extensions.
- Add `GhsTheme(palette, isDark, fontTheme, isAmoled, content)` composable that wraps `MaterialExpressiveTheme` and exposes Locals.
- Migrate `TweaksRepository` palette key — replace the existing `AppTheme.{DYNAMIC,OCEAN,…}` with `GhsPalette.{NORD,CREAM,FOREST,PLUM}` (+ keep DYNAMIC under an "if Android & supported" branch — see don't-build §8).
- Tweaks → Appearance: render the two-axis picker (§7).

**Success criteria:**
- App launches in all 8 combinations (4 palettes × {light, dark}).
- Palette switch in Tweaks updates every screen live within 250ms (medium motion token).
- No hardcoded hex outside `Color.kt` / `GhsPalettes.kt`.
- Existing screens still render (visually unchanged or roughly compatible — they read tokens through compat shims).

### Phase 1 — Silent vocabulary primitives + preview screen

**Scope:** all 12 silent primitives (§1.1–§1.12) + a dev-only preview screen behind a Tweaks → "Developer → Show primitives gallery" toggle. Screen renders every primitive in every relevant state (default, hover/pressed where applicable, disabled).

**Success criteria:**
- Each primitive composable has a `@Preview` (commonMain previews supported in IDE).
- Heartbeat animations honor the `GhsMotionPreference` Local.
- All primitives render correctly under all 4 palettes × {light, dark}.

### Phase 2 — Expressive primitives + base components

**Scope:**
- `CookieShape` (§1.13), `Squiggle` (§1.14)
- All buttons (§2.1)
- All chips (§2.2)
- All cards (§2.3) — including `GhsCardLead` with radial-bloom modifier
- Section header (§2.4)
- Banner (§2.5)
- Vital signs grid (§2.6)
- Wax-seal trust card (§2.7)
- Bottom sheet, confirm dialog, toast, dropdown (§§2.8–2.10, 2.12)
- Bottom nav (Android) + Drawer (Desktop) shells, not yet wired to features

Add to the primitives preview screen: a "Components" tab.

**Success criteria:**
- Component tab in preview shows every component in every state.
- Cookie shape passes path equality vs `tokens.json#shape.cookie.path` (write a unit test that compares the constructed `Path` against the expected SVG path command stream).

### Phase 3 — Home migration

**Scope:** Migrate `feature/home/` to new spec (§3.1). Reuse existing `HomeViewModel`, swap composables. Keep existing data model.

Visual deltas to verify:
- Lead release card uses `GhsCardLead` with the repo's accent radial bloom.
- Hot releases: horizontal scroll on Android; 2-col grid on Desktop when content width ≥ WIDE.
- Section headers use Squiggle.
- Clipboard banner uses `GhsBanner info` variant.

**Success criteria:** Home renders correctly in all palette × mode combos; lead-card accent travels from the API-supplied accent or topic-derived fallback (§6).

### Phase 4 — Apps (Library) migration + bottom nav switch

**Scope:** Migrate `feature/apps/` to new spec (§3.4). Switch Android bottom nav from current set to `[Home, Search, Apps, Profile]` (4 tabs — see assumption Q1 below; if the spec mandates 3 tabs we'll drop Search to a detached FAB later). Implement two-pane Desktop Library.

**Success criteria:**
- Library shows Update banner with VersionStack glyph when `pendingUpdates > 0`.
- Bottom nav active tab uses CookieShape behind the glyph.
- Desktop two-pane: selecting a row replaces the detail pane in <250ms; deep-link to a repo via `DetailsScreen` still works.

### Phase 5 — Details migration

**Scope:** Migrate `feature/details/` (§3.3). Re-skin `Header`, `About`, `Stats`, `WhatsNew`, `ReleaseChannel`, `Owner`, `ReportIssue`. Replace `StatItem` with vital signs 2×2.

Anchor the wax seal to the install panel; verify rotated -6° on Desktop only.

**Success criteria:**
- Detail screen renders all per-app accent surfaces (lead hero bloom, freshness ring outer, install panel bloom).
- Wax seal Cracked state forces card border to `T.danger` and surfaces a sticky toast.

### Phase 6 — Search migration

**Scope:** Migrate `feature/search/` (§3.2). Add source toggle (GitHub / Codeberg / Custom forge), preserve existing `SearchViewModel` logic, swap composables for chip/row/banner.

**Success criteria:** Source toggle drives the existing `sourceHost` plumbing through `ForgejoClientRegistry`. Recent queries chip cloud appears when input is empty.

### Phase 7 — Auth migration

**Scope:** Migrate `feature/auth/` to full-screen sheet (§3.9). Re-render web-OAuth handoff, device flow, PAT entry as three bodies of the same shell. Reuse `AuthPath` state.

**Success criteria:** All three auth paths render with CookieShape identity, Squiggle heading, Heartbeat polling indicator. Tap-to-copy device code emits "Code copied" toast.

### Phase 8 — Tweaks migration

**Scope:** Migrate `feature/tweaks/` (§3.7). Build the two-axis theme picker (§7). Wire all settings rows to `ToggleSettingCard` (rename to `GhsSetRow`).

**Success criteria:** Every section uses Squiggle headers. Two-axis picker swatches show live preview when hovered (Desktop).

### Phase 9 — Profile + DevProfile migration

**Scope:** Migrate `feature/profile/` (§3.5) and `feature/dev-profile/` (§3.6). Implement Connect grid + Business inquiries row. **Remove** SponsorScreen donations content; repurpose route to a "Support the project" page that links out to the GitHub Sponsors page externally (browser intent), nothing inline.

**Success criteria:** Profile renders identity card with `tintP → surface` gradient (only allowed gradient besides Lead bloom).

### Phase 10 — Favourites / Starred / Recently-viewed migration

**Scope:** Migrate the three list-only modules to `GhsCardListRow` + empty states.

**Success criteria:** Empty states have no stock illustration (DESIGN.md §"empty"), use Squiggle + Fraunces italic headline + outline CTA.

### Phase 11 — APK Inspect (Android only)

**Scope:** Build the APK Inspect screen (DESIGN.md §9.6). Lives under `feature/apps/presentation/` (Android-only file using `expect/actual` or `androidMain` source set). Uses `ApkInspector` already in `core/domain/`.

**Success criteria:** Shows wax seal, min/target/compile SDK tiles, activity/service/receiver counts, permissions grouped by `PermRisk`. Permission chips are color-coded inline.

---

## 5. Data honesty audit hooks

Per primitive — what falls back if the backend doesn't supply the input:

| Primitive | Required field | Backend nullable? | Fallback |
|---|---|---|---|
| FreshnessRing | `GithubRelease.publishedAt` → days | Yes when no releases ever | Render avatar without ring; caption "No releases" |
| Heartbeat | `GithubRepoSummary.updatedAt` → days | No (always provided) | n/a |
| StarTier | `stargazersCount` | No | n/a |
| WaxSeal | `InstalledApp.signingFingerprint` + expected | Both nullable | If installed but expected missing → `Intact` w/ caption "Signed by maintainer" (truthful: we know it's signed because Android won't install unsigned). If not installed → `Open` w/ caption "Unsigned by us yet". |
| VersionDelta | `installedVersion` + `latestVersion` | Sometimes one null | `Unknown` grey dot |
| VersionStack | history of skipped tags | Not tracked today | `skippedCount = if (isUpdateAvailable) 1 else 0` |
| PermDot | `ApkInspection.permissions` | Non-APK / no inspection | Render "—" in vital signs tile, no glyph in card |
| PlatformGlyph | `availablePlatforms` | Empty list legal | Hide the row entirely (don't show all-dashed) |
| TopicGlyph | `topics` | Often empty on Codeberg | Drop the row (do not invent topics) |
| SignalBars | mirror health | Untested mirror | tier 0 + caption "Untested" |
| DownloadWeight | aggregated `downloadCount` | Forgejo sums may be 0 | Render "—" in the cap text, skip the dot |
| LicensePosture | SPDX id | Often missing | Render nothing |
| CookieShape | identity letter | n/a | Use `?` glyph as fallback (signed-out user tile) |
| Squiggle | n/a | n/a | always renders |

**Trending and Most-popular sections** (DESIGN.md §11): when the backend's `trendingScore` / `popularityScore` is null, the rank chip `#N` is suppressed — section still renders, the order is the backend list's natural order, but **no pretending**.

**Per-release "HOT · Nd ago" pill on the Lead card:** derived from `releaseRecency` — when `recency > 30d`, downgrade to "FRESH · Nw ago" / "WARM" per `tokens.json#thresholds.freshness`. Never label something HOT it isn't.

---

## 6. Per-app accent algorithm

Each repo carries `accent = { c, lt, dt }` (DESIGN.md §2.4). Resolution order (top wins):

1. **Backend-supplied** — when API returns an `accent` object (not present today; propose adding via `GithubRepoSummary.accentHex: String?` field, server-side derived from avatar dominant color).
2. **Topic-derived** — first matching topic in repo's `topics` against the table below.
3. **Language-derived** — `primaryLanguage` against the language table.
4. **Blue fallback** — `#5E81AC` (Nord primary).

### 6.1 Topic → accent

| Topic | `c` | Use case |
|---|---|---|
| `photo`, `photos`, `gallery` | `#5E81AC` | Cool blue (immich-like) |
| `manga`, `comic`, `reader` | `#7E6BA8` | Plum |
| `password-manager`, `security`, `vault` | `#4C6E96` | Navy |
| `podcast`, `audio`, `music` | `#6B8E5A` | Sage |
| `book`, `ebook`, `koreader` | `#9B6B3C` | Amber |
| `messaging`, `chat`, `signal` | `#A35365` | Muted rose |
| `vpn`, `network`, `proxy` | `#5C7A8E` | Slate-blue |
| `note`, `notes`, `markdown` | `#7A6549` | Cream-ink |
| `backup`, `sync` | `#5A6A57` | Forest-ink |
| `self-hosted`, `home-server` | `#356859` | Forest-deep |
| `video`, `media` | `#B8542C` | Cream-primary |

### 6.2 Language → accent

| Language | `c` |
|---|---|
| `Kotlin` | `#7E6BA8` |
| `Java` | `#B8542C` |
| `TypeScript`, `JavaScript` | `#5E81AC` |
| `Python` | `#356859` |
| `Rust` | `#A35346` |
| `Go` | `#5C7A8E` |
| `C`, `C++` | `#7A6549` |
| `Swift` | `#B8542C` |
| `Dart` | `#5E81AC` |
| `Ruby` | `#B83A2C` |
| `Shell`, `Bash` | `#6B8E5A` |
| anything else | fallback blue |

### 6.3 Tint derivation

```
lt = mix(c, white, 0.78)   // light-mode soft bg fill
dt = c.copy(alpha = 0.22)  // dark-mode bg fill
```

Stored once per repo (cache in `SeenReposRepository` or a new in-memory map keyed by `repoId`).

### 6.4 Where the accent appears

| Surface | Accent role |
|---|---|
| Home Lead card | Radial bloom (`lt @ 0.6`) center-top |
| Hot release compact card | Faint top stripe (4.dp) `lt @ 0.4` |
| Trending / Popular rows | None (rank is the answer) |
| Detail hero | Avatar ring outer arc tint |
| Detail install panel | Primary CTA stays `T.primary`; the small "Update" banner uses `accent.c` |
| Library row | None (apps share visual weight; per-app tint would compete with VersionStack) |
| Favourites / Starred / Recently-viewed rows | None |
| Update banner inside Apps detail pane | Banner bg `accent.lt`, CTA bg `accent.c` (patterns.md §"Update banner") |

**Never**: bottom nav (accent doesn't follow user, palette does), toast (status colors do the work), confirm dialog, drawer.

---

## 7. Two-axis theme picker (Tweaks → Appearance)

The picker has two independent axes:

```
APPEARANCE
~~ squiggle ~~

Palette
[Nord]  [Cream]  [Forest]  [Plum]            ← 4 swatches, each 72.dp square, GhsShapes.cardSm
  ◉ active (1.5.dp T.primary border + corner check)

Mode
[ ☀ Light  ◐ System  🌙 Dark ]               ← segmented control, RoundedCornerShape(50%) outer, each segment GhsShapes.chip
```

| Spec | Value |
|---|---|
| Palette swatch | 72×72.dp `GhsShapes.cardSm`. Internally split: top half = light-mode preview (mini bg+surface+primary blocks), bottom half = dark-mode preview. Label below in caption: "Nord" / "Cream" / etc. |
| Active swatch | 1.5.dp `T.primary` border + small `✓` in top-right `T.primary` |
| Mode segmented | Three icon+label segments. Selected segment has `T.tintP` bg + `T.primary` content; others transparent + `T.ink2`. |
| Live preview | On hover (Desktop) the whole app re-themes for 1.5s; on tap, commits. Android: tap commits immediately. |
| Persistence | Two keys in `TweaksRepository`: `palette: GhsPalette`, `themeMode: AppearanceMode = Light | Dark | System`. |
| Migration | The existing `AppTheme` enum (`DYNAMIC, OCEAN, PURPLE, …`) maps once on first launch post-update: OCEAN/SLATE → NORD; PURPLE/AMBER → PLUM/CREAM (closest hue); FOREST → FOREST; DYNAMIC → user explicitly opted into Material You — see don't-build §8. |

Mobile layout: palette swatches in a 2×2 grid (each 72.dp), mode segmented full-width below.
Desktop layout: palette swatches in a horizontal row of 4, mode segmented to the right.

---

## 8. Don't-build list

Things explicitly NOT to bring forward, with rationale:

1. **Donations / Sponsor inline UI** (MIGRATION.md §risky areas). Profile gets Connect + Business inquiries. Keep the `SponsorScreen` route but repurpose: it now just opens GitHub Sponsors externally via `BrowserHelper`. No inline donation cards, no rewards, no tier list.
2. **Material You / Dynamic color override** (themes.md, design-system.md §3). Dynamic color is removed. The current `AppTheme.DYNAMIC` is gone — users who had it get migrated to `NORD` + their existing mode. Reasoning: dynamic colors don't survive the Silent Vocabulary's accent rules — they fight the per-app accent. Add a Tweaks line "We removed dynamic color — pick Nord, Cream, Forest, or Plum instead. The accent now comes from each app's logo."
3. **"Featured" curation** (DESIGN.md §11). The lead release on Home is always "top of filtered Hot list", labeled honestly with `HOT · Nd ago`. Don't invent any "Editor's pick" labels.
4. **Trending percentage chips** ("+15%"). Backend doesn't supply rate of change. Use position `#N` only when backend provides rank; otherwise no rank chip.
5. **Translate-the-app feature** (MIGRATION.md §risky areas). The 13 locale strings stay (`core/presentation` already has them), but no "Translate" UI surface. The Translate provider plumbing in Tweaks is **only** for translating README/release-notes content (existing TranslationControls).
6. **Stock illustrations in empty states**. Use Squiggle + Fraunces italic headline only. RoseFourLoader covers loading.
7. **Card hover-lift animations / scale on hover**. DESIGN.md §6.2 forbids. Use only background tint state-layer at 8% (`T.ink @ 0.08`).
8. **Decorative gradients** anywhere except (a) Lead card accent bloom and (b) Profile identity card `tintP → surface`. No backgrounds gradients on regular cards (DESIGN.md §2.5, design-system.md §2).
9. **Emoji in UI chrome** — replaced by silent primitives or Material Symbols. Allowed only in user-generated content (README, release notes).
10. **Long descriptive sentences** ("Released 37 days ago") when a primitive already says it (the FreshnessRing's fraction is the answer).
11. **Multiple modal dialogs stacked** (DESIGN.md §16.8). Replace with full-screen sheet.

### 8.1 GHS-specific surfaces the handoff doesn't address (we add them under the new system)

The handoff was written for a single-source (github.com) app. GHS extends to Codeberg / Forgejo / custom forges + per-host PATs + mirror picker — all foreign to the prototypes. They belong under the new vocabulary as follows:

| Surface | Where | Visual approach |
|---|---|---|
| **Source toggle (GitHub / Codeberg / Custom)** | Search top, Details top | Segmented control. Each segment is a small platform mark (GitHub octocat → outlined, Codeberg → outlined, Custom forge → dashed plus). |
| **Per-host tokens** | Tweaks → Sources → "Access tokens" | List rows. Each row: host name in mono, label, "Edit / Remove" trailing menu. Add row uses dashed-border chip pattern (§2.2). |
| **Custom forges** | Tweaks → Sources → "Custom forges" | Same row pattern. Adds: live validation chip (SignalBars-style) after entry test. |
| **Mirror picker** | `MirrorPickerScreen` (sub-screen) | Bottom sheet on Android, side sheet on Desktop. Rows: mirror name in mono + SignalBars + last-checked relative time. |
| **Translation provider config** | Tweaks → Translations | Segmented provider picker + per-provider sub-form (mirror URL for LibreTranslate, API key for DeepL, etc.). Existing `KSafe` pattern. |
| **Repo-id-codec foreign host marker** | Anywhere a repo from a non-GitHub source appears | Small platform mark (16.dp) to the right of the owner caption; PlatformGlyph-style outlined silhouette. |

---

## 9. Open questions and assumptions

10 items, prioritized. Marked **A** (Assumption — I'm picking, flag if wrong) or **B** (Blocker — needs user input before that phase starts).

1. **[A] Bottom nav tabs = 4 (Home, Search, Apps, Profile).** DESIGN.md §9.1 shows 4. MIGRATION.md §risky says "3-tab + detached search FAB". The handoff is internally inconsistent. I'm picking 4 because (a) Search is heavily used in this app and (b) detached FABs collide with our Shizuku install affordances on Android. Flag if you want 3.
2. **[A] Cookie active indicator on Desktop drawer.** I'm rendering Cookie only on Android bottom-nav active tab; Desktop drawer uses the simpler `T.tintP` background + 13/10 squircle (DESIGN.md §8.1). The rule "Cookie only at 3 touchpoints" survives because Desktop has CookieShape on brand mark + user tile (and no active tab Cookie).
3. **[A] Profile sub-screens (Favourites / Starred / Recently-viewed) keep their own top-level routes.** I'm not collapsing them into a single "Activity" screen. MIGRATION.md §risky implies collapse but the existing nav graph names them separately and the current UI keeps them as distinct routes — easier to migrate one-screen-at-a-time.
4. **[B] Two-pane Library on Desktop — confirm scope.** The handoff §8.3 mandates two-pane. We don't have it today. Phase 4 needs a green light to introduce `LibraryDetailHost` and embed `DetailsRoot` with an `embedded=true` prop (which I have to add).
5. **[B] Repo accent — server-side or client-side derivation?** DESIGN.md §2.4 says "When backend doesn't supply one, derive from avatar dominant color (color-thief)". We don't have a JVM/Android color-thief in `commonMain`. Phase 3 falls back to topic→accent and language→accent (§6) — confirm that's enough until backend ships the field.
6. **[A] WaxSeal "expected fingerprint" storage.** Current `InstalledApp.signingFingerprint` is the *current* one; we don't store an expected baseline. I'm fallback-rendering `Intact` for any installed signed app (since Android refuses to install unsigned). The Cracked state activates **only** when a new install attempt's fingerprint differs from the stored one — needs a new DB column `expectedSigningFingerprint`. Flag for Phase 5.
7. **[A] Wonky squircle in Compose** — I'm using `AbsoluteRoundedCornerShape` with 4 distinct corner radii (single-axis). DESIGN.md's CSS uses 8-value border-radius (two axes per corner). The visible delta at our sizes (≤28.dp radius) is sub-pixel on @2x — accepting it.
8. **[A] Fonts at build time.** Fraunces variable + Inter Tight variable + JetBrains Mono — adding three fonts inflates APK ~600KB. Acceptable; Compose Resources supports font subsetting if we hit the wall.
9. **[B] Translate-the-app removal.** MIGRATION.md says no Translate UI but we ship 13 locales today (CLAUDE.md `core/presentation` + 13-locale strings). The translate plumbing in Tweaks is content-only. Confirm we keep the 13 user-facing UI locales (Settings > Language stays) and only forbid showing a "Translate this app" CTA.
10. **[A] Dynamic color migration.** Existing users with `AppTheme.DYNAMIC` get mapped to `NORD` on next launch and shown the one-time tooltip "We removed dynamic color — pick Nord, Cream, Forest, or Plum instead." First-launch dismissal stored in `TweaksRepository`. Flag if you want a different default.

---

## 10. Quick-reference card (one-screen)

- **Tokens** live in `tokens.json#palettes.{nord,cream,forest,plum}.{light,dark}` → `GhsTokens`.
- **Status colors** (palette-independent) live in `tokens.json#status.{freshness,wax,perm,trend}` → `GhsStatus`.
- **Shapes**: `chip 11/8`, `row 13/10`, `cardSm 15/11`, `card 18/14`, `cardLg 20/15`, `hero 24/18`, `heroLg 28/22`. Wonky variants for Primary CTA + Lead card + Search input.
- **Fonts**: Fraunces italic (names/headings), Inter Tight (body), JetBrains Mono (versions/hashes only).
- **Cookie at 3 places**: brand "G", user identity, Android active tab.
- **Squiggle**: one per section heading. No more.
- **Heartbeat**: only where it has space to label itself. Not in dense rows.
- **Wax-seal red**: the only place red is allowed to scream.
- **Per-app accent**: backend → topic → language → blue.
- **One filled button per surface.**
- **No emoji in UI chrome. No stock illustrations. No fake data.**

End of UI-SPEC.
