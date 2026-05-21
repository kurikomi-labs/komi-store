# UX-ARCHITECTURE — Design System v2 refresh

> Author: ArchitectUX. Companion: `UX-RESEARCH.md` (written by UX-Researcher in parallel — do not delete).
> Scope: technical plan only. **No Kotlin written yet.** Scaffold + sequencing.
> Source-of-truth handoff: `~/Downloads/handoff 4/` (DESIGN.md, tokens.json, MIGRATION.md, patterns.md, themes.md, design-system.md, silent-vocab.jsx, store-themed.jsx).

---

## 0 · Current-state snapshot

Findings from a read pass over the codebase. All paths root at repo.

| Concern | File | Note |
|---|---|---|
| Material 3 theme entrypoint | `core/presentation/.../theme/Theme.kt:418` | `GithubStoreTheme(isDarkTheme, appTheme, fontTheme, isAmoledTheme)` → `MaterialExpressiveTheme`. |
| Color tokens | `core/presentation/.../theme/Color.kt`, `Theme.kt:17-414` | Five M3 schemes hand-rolled: Ocean, Purple, Forest, Slate, Amber. Single-axis (palette ⇆ scheme), dark-light selected by boolean. |
| Type | `core/presentation/.../theme/Type.kt` | Inter (regular sans) + JetBrains Mono already shipped under Compose Resources. **Fraunces is missing.** Custom-vs-system toggle exists. |
| Dynamic Material You | `Theme.android.kt`, `Theme.jvm.kt` | `expect/actual` `getDynamicColorScheme(dark)`. JVM = null. |
| Persistence | `core/data/.../repository/TweaksRepositoryImpl.kt:66-91` | KSafe-backed. Keys: `K_THEME` (`app_theme`), `K_IS_DARK` (`is_dark_theme`, tri-state nullable), `K_AMOLED`, `K_FONT`. |
| Domain enums | `core/domain/.../model/AppTheme.kt`, `FontTheme.kt` | `AppTheme = { DYNAMIC, OCEAN, PURPLE, FOREST, SLATE, AMBER }`. No mode enum — `null` Boolean encodes "system". |
| Repository "Themes" interface | none | The handoff calls it `ThemesRepository`; in this repo theme prefs live on `TweaksRepository`. We keep that name. |
| Font assets | `core/presentation/src/commonMain/composeResources/font/` | `inter_*.ttf`, `jetbrains_mono_*.ttf`. Loaded via `org.jetbrains.compose.resources.Font(Res.font.X)`. |
| Vector drawables | `core/presentation/.../composeResources/drawable/ic_platform_*.xml` | `.xml` vectors — Compose Resources `painterResource(Res.drawable.X)` already in use. |
| Local providers (existing) | `core/presentation/.../locals/Local{BottomNavigationHeight,ContentWidth,ScrollbarEnabled}.kt` | Pattern in place: `staticCompositionLocalOf` with `noLocalProvidedFor` defaults. We extend it, don't reinvent. |
| Sample card | `core/presentation/.../components/ExpressiveCard.kt:15` | Symmetric `RoundedCornerShape(32.dp)`. Needs asymmetric replacement. |

**Implication.** We have to do two structural changes besides the visual refresh:

1. **Two-axis theme model.** Today: 6 palettes × `Boolean?` dark. New: 4 palettes × 3-mode (LIGHT/DARK/SYSTEM). Old palettes (OCEAN, PURPLE, SLATE, AMBER, DYNAMIC) are decommissioned. A migration is required.
2. **Asymmetric shapes.** Compose's `RoundedCornerShape` accepts four corners but each corner is a single radius (x == y). The handoff uses elliptical radii for the wonky squircle (`20px 14px 22px 16px / 16px 22px 14px 20px`). Requires a custom `Shape`.

---

## 1 · Token system

### 1.1 Delivery: hand-written Kotlin objects (not codegen)

**Decision: ship `Tokens.kt` as hand-written Kotlin in `core/presentation`.** Do **not** wire a Gradle task to generate from `tokens.json`.

Justification:

- `tokens.json` is ~270 lines, four palettes, frozen on v1. The "build step" amortises over churn, and we have none.
- Codegen breaks Kotlin's compile-time tools — `Color(0xFF...)` constants resolve to integers cleanly only when authored as Kotlin. Generated string-to-hex loses the `@Stable` guarantees and bloats DI.
- The handoff's `silent-vocab.jsx` already encodes thresholds (`freshnessOf`, `freshnessFraction`, `starTier`) and shape paths. Hand-porting forces the implementer to read the rule, not skim it.
- A future regen story: keep `tokens.json` checked in at `.design/tokens.json`. Add a one-off `./gradlew :core:presentation:checkTokens` Konsist/regex sanity that grep-asserts every hex in the JSON appears in `PaletteTokens.kt`. Cheap, no codegen.
- Build-step alternative considered: `build-logic/convention/.../TokensGenerationConventionPlugin.kt` that runs a `KotlinScript` reading the JSON. Rejected: extra build cost (Kotlin daemon spin), brittle to schema drift, and no consumer outside this one module.

**Rule:** if a 5th palette ever ships, write it by hand. We never invent palettes (themes.md §"Disallowed combinations").

### 1.2 File layout

Under `core/presentation/src/commonMain/kotlin/zed/rainxch/core/presentation/theme/`:

```
theme/
├── tokens/
│   ├── PaletteTokens.kt        // data class PaletteColors + 8 instances
│   ├── StatusTokens.kt         // freshness/wax/perm/trend (palette-independent)
│   ├── ShapeTokens.kt          // asymmetric radius constants + WonkySquircleShape + CookieShape + Squiggle path
│   ├── SpacingTokens.kt        // xxs..xxxl Dp
│   ├── TypeTokens.kt           // TypeScale data class + roles
│   ├── MotionTokens.kt         // durations + easings + Heartbeat periods
│   └── ThresholdTokens.kt      // freshness/maintenance/stars lookup
├── locals/
│   ├── LocalPalette.kt
│   ├── LocalStatusColors.kt
│   ├── LocalTypeScale.kt
│   ├── LocalShapes.kt
│   ├── LocalSpacing.kt
│   └── LocalMotion.kt
├── GhsTheme.kt                 // new entrypoint (replaces GithubStoreTheme)
├── Color.kt                    // KEEP — re-export for any non-migrated screen; mark @Deprecated
└── Theme.kt                    // KEEP wrapper that delegates GithubStoreTheme → GhsTheme during migration
```

`Color.kt`, `Theme.kt`, `Type.kt` stay alongside as a deprecation shim until phase 3+. Calls into the deprecated API compile but warn; new code consumes `GhsTheme` + `LocalPalette.current`.

### 1.3 Data classes (sketch — not code yet, only contract)

```
@Immutable
data class PaletteColors(
    val bg, surface, surface2, ink, ink2, outline, primary, tintP,
    success, successT, danger, dangerT, shadow,
) // all androidx.compose.ui.graphics.Color

@Immutable
data class StatusColors(
    val freshness: FreshnessColors,    // hot, fresh, warm, cool, dormant
    val wax: WaxColors,                // intact, cracked, open
    val perm: PermColors,              // low, moderate, high
    val trend: TrendColors,            // rising, flat, falling
)

@Immutable
data class TypeScale(
    val display: TextStyle, displaySm: TextStyle, headline: TextStyle,
    val title: TextStyle, titleSm: TextStyle,
    val body: TextStyle, bodySm: TextStyle,
    val caption: TextStyle, label: TextStyle, mono: TextStyle,
    val h3Warm: TextStyle, h3Meta: TextStyle, // editorial vs metadata variants
)

@Immutable
data class GhsShapes(
    val xs: Shape, sm: Shape, md: Shape, lg: Shape, xl: Shape,
    val wonkySquircle: Shape, wonkySquircleAlt: Shape, wonkySquircleSearch: Shape,
    val cookie: Shape, full: Shape,
)

@Immutable
data class Spacing(val xxs, xs, s, sm, m, l, xl, xxl, xxxl: Dp)

@Immutable
data class GhsMotion(
    val quick: AnimationSpec<Float>, val medium: AnimationSpec<Float>, val slow: AnimationSpec<Float>,
    val springSoft: SpringSpec<Float>, val springBouncy: SpringSpec<Float>,
    val heartbeat: HeartbeatSpec, // scaleFrom 1.0, scaleTo 1.25, haloTo opacity 0
)

data class Thresholds(
    val freshness: List<FreshnessBucket>,     // {maxDaysInclusive, state, ringFraction, color}
    val stars: List<StarBucket>,              // {minStars, tier}
    val maintenance: List<MaintenanceBucket>, // {maxDaysInclusive, state, heartbeatPeriodMs}
)
```

All marked `@Immutable` so Compose can skip recomposition when a CompositionLocal carries them unchanged. Status / Thresholds / Shapes are **constants** — only `PaletteColors` and (downstream) `TypeScale.color` change per state.

---

## 2 · Composition locals + `GhsTheme`

### 2.1 Provider entrypoint

```
@Composable
fun GhsTheme(
    palette: Palette = Palette.NORD,
    mode: ResolvedMode = ResolvedMode.LIGHT,   // already-resolved (system → light/dark done upstream)
    fontTheme: FontTheme = FontTheme.CUSTOM,
    content: @Composable () -> Unit,
)
```

Inside, it:

1. Resolves the `PaletteColors` for `(palette, mode)` from a frozen 4×2 map (8 instances).
2. Builds a Material 3 `ColorScheme` from those tokens (see §2.2 mapping).
3. Resolves the `TypeScale` (Fraunces + Inter Tight + JetBrains Mono, see §5) with current ink color baked into each `TextStyle`.
4. Provides composition locals + calls `MaterialExpressiveTheme(...)` so existing M3 widgets keep working.

```
CompositionLocalProvider(
    LocalPalette provides paletteColors,
    LocalStatusColors provides StatusColors.Constant,   // palette-independent
    LocalTypeScale provides typeScale,
    LocalShapes provides GhsShapes.Default,
    LocalSpacing provides Spacing.Default,
    LocalMotion provides GhsMotion.Default,
) {
    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        typography = MaterialBridge.toM3Typography(typeScale),
        shapes = MaterialBridge.toM3Shapes(GhsShapes.Default),
        motionScheme = MotionScheme.expressive(),
        content = content,
    )
}
```

### 2.2 Material 3 mapping

Material widgets (sliders, dialogs, snackbar, ripple) read `MaterialTheme.colorScheme`. We map our tokens so they don't drift:

| Our token | M3 slot |
|---|---|
| `bg` | `background`, `surface` (M3 conflates these in M3 Expressive), `surfaceContainerLowest` |
| `surface` | `surfaceContainer`, `surfaceContainerLow` |
| `surface2` | `surfaceContainerHigh`, `surfaceVariant` |
| `ink` | `onBackground`, `onSurface` |
| `ink2` | `onSurfaceVariant` |
| `outline` | `outline`, `outlineVariant` |
| `primary` | `primary`, `inversePrimary` (the latter via tonal flip per mode) |
| `tintP` | `primaryContainer`, `secondaryContainer` |
| `surface` (white fg) | `onPrimary` — but Cream `primary` (`#B8542C`) on white = 4.8:1 ✓. Forest light primary (`#6B8E5A`) on white = 3.0:1, fails AA. **Action:** for Cream + Forest light, use the palette `ink` as `onPrimary` (it's `#2B1F14` / `#2D3A2C` respectively — 7:1 against the primary). Document per-palette in PaletteTokens.kt. |
| `success` | `tertiary` (closest semantic match in M3) |
| `successT` | `tertiaryContainer` |
| `danger` | `error` |
| `dangerT` | `errorContainer` |

**Why both layers?** M3 components don't know about our tokens. If a third-party Compose lib (Landscapist placeholders, Markdown renderer, navigation animations) reads `MaterialTheme.colorScheme.surface`, it should still get the right surface. Composition locals are for *our* code; M3 colorScheme is for *everyone else*.

### 2.3 Locals API (read site)

```
object Ghs {
    val palette  @Composable get() = LocalPalette.current
    val status   @Composable get() = LocalStatusColors.current
    val type     @Composable get() = LocalTypeScale.current
    val shapes   @Composable get() = LocalShapes.current
    val spacing  @Composable get() = LocalSpacing.current
    val motion   @Composable get() = LocalMotion.current
}

// Call site:
Text("Updated", style = Ghs.type.label, color = Ghs.palette.ink2)
Box(Modifier.background(Ghs.palette.tintP, Ghs.shapes.lg))
```

`staticCompositionLocalOf` for everything (we never animate between palettes by interpolating — palette switches are a recomposition event, not an interpolation; see themes.md §"Mode-switching transitions" which crossfades the whole tree, not individual color values).

---

## 3 · Asymmetric "wonky squircle" `Shape`

### 3.1 Problem

CSS: `border-radius: 20px 14px 22px 16px / 16px 22px 14px 20px;` — four corners, each with **different x and y radii** (elliptical). Compose:

- `RoundedCornerShape` — same per-corner radius, x == y.
- `AbsoluteRoundedCornerShape` — also x == y per corner.
- `CutCornerShape` — wrong shape.

No built-in elliptical-per-corner shape exists. We write `WonkySquircleShape : Shape`.

### 3.2 Algorithm

```
class WonkySquircleShape(
    val topLeftX: Dp, val topLeftY: Dp,
    val topRightX: Dp, val topRightY: Dp,
    val bottomRightX: Dp, val bottomRightY: Dp,
    val bottomLeftX: Dp, val bottomLeftY: Dp,
) : Shape {

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val (tlx, tly, trx, try_, brx, bry, blx, bly) = density.toPxAll(...)
        val w = size.width; val h = size.height
        val path = Path().apply {
            moveTo(tlx, 0f)
            lineTo(w - trx, 0f)
            // top-right corner: ellipse arc from (w-trx, 0) → (w, try_)
            arcTo(
                rect = Rect(w - 2*trx, 0f, w, 2*try_),
                startAngleDegrees = -90f, sweepAngleDegrees = 90f, forceMoveTo = false,
            )
            lineTo(w, h - bry)
            arcTo(rect = Rect(w - 2*brx, h - 2*bry, w, h), 0f, 90f, false)
            lineTo(blx, h)
            arcTo(rect = Rect(0f, h - 2*bly, 2*blx, h), 90f, 90f, false)
            lineTo(0f, tly)
            arcTo(rect = Rect(0f, 0f, 2*tlx, 2*tly), 180f, 90f, false)
            close()
        }
        return Outline.Generic(path)
    }
}
```

`Path.arcTo` with an elliptical bounding `Rect` (width ≠ height) draws an elliptical arc. This is the trick that closes the gap.

Mirror for `LayoutDirection.Rtl`: swap (TL, TR) and (BL, BR) corners.

### 3.3 Token wiring

`ShapeTokens.kt` exposes named instances using the JSON values:

```
val WonkySquircleCard    = WonkySquircleShape(tl=20.dp/16.dp, tr=14.dp/22.dp, br=22.dp/14.dp, bl=16.dp/20.dp)
val WonkySquircleCardAlt = WonkySquircleShape(...22/18, 16/24, 24/16, 18/22)
val WonkySquircleSearch  = WonkySquircleShape(...24/18, 18/24, 26/20, 20/26)
```

**Notation:** `tl=20.dp/16.dp` = "top-left x-radius 20dp, y-radius 16dp" in CSS shorthand order.

### 3.4 Fallback (graceful degradation)

If `WonkySquircleShape` ever causes outline-clip artifacts on a target platform (e.g. Skia rasterisation glitch on JVM Linux), provide:

```
object Shapes {
    val wonkySquircle: Shape = when {
        DegradedShapes.enabled -> AbsoluteRoundedCornerShape(
            topStart=18.dp, topEnd=14.dp, bottomEnd=20.dp, bottomStart=16.dp,
        )  // symmetric per corner — loses ~5% of the wonky feel
        else -> WonkySquircleShape(...)
    }
}
```

Gated by a build flag. Default = full shape. Use the fallback only if we hit a rasterisation bug in QA.

### 3.5 The diagonal asymmetry rule (design-system.md §6.2)

For all *non-wonky* asymmetric corners (rows, chips, buttons) the rule is symmetric-per-corner with two values diagonally:

```
RoundedCornerShape(topStart=L, topEnd=S, bottomEnd=L, bottomStart=S)
```

Built-in `RoundedCornerShape` handles this. No custom shape needed for `xs/sm/md/lg/xl`. Wonky squircle is **only** for primary CTAs and lead/hero cards (CSS comment: *"feels hand-shaped"*).

---

## 4 · `CookieShape` + `Squiggle`

### 4.1 CookieShape (clippable)

`tokens.json` ships:
- `viewBox: "0 0 100 100"`
- `path: "M50 4 C 62 4 66 12 76 12 C 86 12 91 22 91 32 C 95 40 100 50 94 58 C 96 70 90 82 80 86 C 72 90 64 96 54 96 C 44 96 36 95 26 92 C 16 90 10 80 8 70 C 4 62 0 54 6 46 C 6 34 12 22 22 18 C 32 12 38 4 50 4 Z"`

This is ~12 cubic Bezier segments. Two options:

**Option A — hand-translate.** Map each `C x1 y1, x2 y2, x y` to `path.cubicTo(x1*sx, y1*sy, x2*sx, y2*sy, x*sx, y*sy)` where `sx = size.width / 100f`, `sy = size.height / 100f`. ~12 lines of code. Done once, frozen.

**Option B — SVG path parser.** Tokenize the `d` string, dispatch by command (`M`, `C`, `T`, `Q`, `L`, `Z`). Useful if more SVG paths arrive. Squiggle uses `Q` and `T` (quadratic + smooth quadratic), so the parser must handle those if we want one parser for both.

**Decision: Option A.** Two paths, frozen tokens, no need for a parser. Hand-translate both into `Path` builders. Document the source `d` string in a KDoc/comment so the next maintainer knows where it came from.

```
object CookieShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val sx = size.width / 100f
        val sy = size.height / 100f
        val p = Path().apply {
            moveTo(50f*sx, 4f*sy)
            cubicTo(62f*sx, 4f*sy,  66f*sx, 12f*sy, 76f*sx, 12f*sy)
            cubicTo(86f*sx, 12f*sy, 91f*sx, 22f*sy, 91f*sx, 32f*sy)
            // ... 10 more
            close()
        }
        return Outline.Generic(p)
    }
}
```

Then `Modifier.clip(CookieShape)` works on any `Box` / `Image` / brand mark.

Sizes specified (design-system.md): brand mark, profile avatar, active bottom-nav tab. All three accept `Modifier.size(40.dp)` / `64.dp` / `28.dp` and the shape scales because of `size.width / 100f`.

### 4.2 Squiggle (decorative, not clippable)

```
viewBox: "0 0 40 5"
path:    "M1 3 Q 5 0.5, 9 3 T 17 3 T 25 3 T 33 3 T 39 3"
stroke:  "1.6px", opacity: 0.6, color: "primary"
```

Not a clip shape (it's an underline). Render with `Canvas`:

```
@Composable
fun Squiggle(modifier: Modifier = Modifier, color: Color = Ghs.palette.primary) {
    Canvas(modifier.size(width=40.dp, height=5.dp)) {
        val p = Path().apply {
            moveTo(1f*sx, 3f*sy)
            quadraticBezierTo(5f*sx, 0.5f*sy, 9f*sx, 3f*sy)
            // smooth-quadratic: reflect previous ctrl point. T x,y == Q (2*prevEnd - prevCtrl), x, y
            quadraticBezierTo(13f*sx, 5.5f*sy, 17f*sx, 3f*sy)
            quadraticBezierTo(21f*sx, 0.5f*sy, 25f*sx, 3f*sy)
            quadraticBezierTo(29f*sx, 5.5f*sy, 33f*sx, 3f*sy)
            quadraticBezierTo(37f*sx, 0.5f*sy, 39f*sx, 3f*sy)
        }
        drawPath(p, color.copy(alpha=0.6f),
                 style = Stroke(width=1.6.dp.toPx(), cap=StrokeCap.Round))
    }
}
```

(The `T` reflection is pre-computed above — Compose `Path` has no smooth-quadratic primitive.)

---

## 5 · Typography

### 5.1 Three fonts, one resource bundle

Already on disk:
- `inter_{light,regular,medium,semi_bold,bold,black}.ttf`
- `jetbrains_mono_{light,regular,medium,semi_bold,bold}.ttf`

Missing:
- **Fraunces** — italic 500/600/700, with optical-size axis 9..144 (the JSON imports it variable). We will ship **static italic TTFs** (3 weights × italic) for app-size correctness; variable TTF parses on Android API 26+ and JVM (Skia ≥ M85) but is heavier and Compose's `Font(...)` doesn't expose axis controls in the Resources API today. Static is safe.
- **Inter Tight** — currently we ship "Inter" (the wider one). DESIGN.md §3.4 forbids it ("Inter Tight reads tighter and pairs better with Fraunces"). Replace `inter_*.ttf` with `inter_tight_*.ttf` (400/500/600/700 + a 800 variant for the rare display-weight case).

Drop into `core/presentation/src/commonMain/composeResources/font/` (same place as today). Compose Resources generates `Res.font.X` accessors for `commonMain` consumption — **no `expect/actual` needed**. The existing `Type.kt:12-30` pattern (using `org.jetbrains.compose.resources.Font(...)`) is the right pattern; we extend it.

### 5.2 Multi-script policy

From `MIGRATION.md`: Inter Tight covers Latin + Cyrillic only. Fraunces is Latin/Cyrillic only.

**For Latin/Cyrillic:** Fraunces (display) + Inter Tight (body) + JetBrains Mono (code).

**For other scripts:** fall back via `FontFamily` composition. Compose `FontFamily` accepts multiple `Font` entries with the same weight; if a glyph isn't in the first font, the platform falls back through the list. We will:

1. Ship Noto Sans variants in `composeResources/font/`: `noto_sans_jp`, `noto_sans_sc`, `noto_sans_tc`, `noto_sans_kr`, `noto_sans_devanagari`, `noto_sans_arabic`, `noto_sans_hebrew`. Regular + Bold only — that's enough for headlines + body.
2. Build family with platform-aware ordering. **On Android** the platform's `Typeface` does its own script fallback against system fonts; just provide the Latin fonts and let Android do the rest (Android system fonts cover everything). **On JVM** the situation is worse — JDK font fallback is best-effort, headless Linux may have no CJK. Ship the Noto TTFs as `Font(Res.font.noto_sans_jp_regular)` and **embed them in `FontFamily` after the Latin fonts**. This costs ~5MB per Noto family but guarantees no tofu on any platform.

`FontFamily` declaration:
```
val SansBody = FontFamily(
    Font(Res.font.inter_tight_regular, FontWeight.Normal),
    Font(Res.font.inter_tight_medium,  FontWeight.Medium),
    Font(Res.font.inter_tight_semi_bold, FontWeight.SemiBold),
    Font(Res.font.inter_tight_bold,    FontWeight.Bold),
    // Fallbacks for non-Latin scripts (Compose picks based on glyph coverage)
    Font(Res.font.noto_sans_jp_regular, FontWeight.Normal),
    Font(Res.font.noto_sans_sc_regular, FontWeight.Normal),
    Font(Res.font.noto_sans_kr_regular, FontWeight.Normal),
    Font(Res.font.noto_sans_devanagari_regular, FontWeight.Normal),
    Font(Res.font.noto_sans_arabic_regular, FontWeight.Normal),
    Font(Res.font.noto_sans_hebrew_regular, FontWeight.Normal),
    // (matching bolds...)
)
```

**Open question (§12-Q3):** is the ~30MB APK size hit (Noto × 7 scripts × 2 weights) acceptable, or do we prefer Android-only system fallback + accept JVM tofu on minority scripts?

### 5.3 Italic Fraunces — "App identity" rule

DESIGN.md §3.3: "Italic Fraunces only for app/screen identity. Don't italicize body or buttons." Practical implication: the `TypeScale` only puts `FontStyle.Italic` on `display`, `displaySm`, `headline`, and `h3Warm`. Body/title/caption use upright.

### 5.4 Tabular numbers

DESIGN.md §5.3: "Numbers use Inter Tight tabular (`fontVariantNumeric: 'tabular-nums'`)". In Compose: `TextStyle(fontFeatureSettings = "tnum")`. Bake this into `TypeScale.caption`, `body`, `bodySm`, `mono` — anywhere a star count, version, or download number renders.

---

## 6 · Theme persistence — schema migration

### 6.1 New domain model

`core/domain/.../model/Palette.kt`:
```
enum class Palette { NORD, CREAM, FOREST, PLUM ;
    companion object { fun fromName(n: String?) = entries.find { it.name == n } ?: NORD }
}
```

`core/domain/.../model/ThemeMode.kt`:
```
enum class ThemeMode { LIGHT, DARK, SYSTEM ;
    companion object { fun fromName(n: String?) = entries.find { it.name == n } ?: SYSTEM }
}
```

`AppTheme.kt`: **deprecated** with `@Deprecated("Replaced by Palette + ThemeMode")`. Keep the enum compiled so existing code on `feature/tweaks` still resolves until phase 5. Provide an extension `AppTheme.toPalette(): Palette` for the migration path (table below).

### 6.2 TweaksRepository surface change

```
// new — added alongside the old methods
fun getPalette(): Flow<Palette>
suspend fun setPalette(palette: Palette)
fun getThemeMode(): Flow<ThemeMode>
suspend fun setThemeMode(mode: ThemeMode)

// old — kept until phase 5, then removed in one commit
@Deprecated fun getThemeColor(): Flow<AppTheme>
@Deprecated suspend fun setThemeColor(theme: AppTheme)
@Deprecated fun getIsDarkTheme(): Flow<Boolean?>
@Deprecated suspend fun setDarkTheme(isDarkTheme: Boolean?)
```

`getAmoledTheme`/`setAmoledTheme` survives — AMOLED is a sub-mode of DARK and we keep that knob (§12-Q5).

### 6.3 Persistence keys

Add new KSafe keys in `TweaksRepositoryImpl.kt:447`:
```
private const val K_PALETTE = "palette_v2"
private const val K_THEME_MODE = "theme_mode_v2"
```

Old keys (`K_THEME = "app_theme"`, `K_IS_DARK = "is_dark_theme"`) stay on disk. On first read of `getPalette()`/`getThemeMode()`:

```
override fun getPalette(): Flow<Palette> = flow {
    migrationDeferred.await()
    val raw: String = ksafe.safeGet(K_PALETTE, "")
    if (raw.isEmpty()) {
        // one-shot migration from legacy K_THEME
        val legacy: String = ksafe.safeGet(K_THEME, "")
        val migrated = mapLegacyAppThemeToPalette(legacy)   // see table below
        ksafe.safePut(K_PALETTE, migrated.name)
        emit(migrated)
    }
    emitAll(ksafe.safeGetFlow(K_PALETTE, Palette.NORD.name).map { Palette.fromName(it) })
}
```

### 6.4 Legacy → new palette map

| Old `AppTheme` | New `Palette` | Notes |
|---|---|---|
| `OCEAN` | `NORD` | Nord is the cool-blue default. |
| `DYNAMIC` | `NORD` | Material You removed — themes.md §"Don't let dynamic color override". |
| `PURPLE` | `PLUM` | Closest mood. |
| `FOREST` | `FOREST` | Direct map. |
| `SLATE` | `NORD` | Cool grey → nord (no muted-grey palette in v2). |
| `AMBER` | `CREAM` | Warm orange → cream. |

`getIsDarkTheme(): Flow<Boolean?>` → `getThemeMode(): Flow<ThemeMode>`:
- `null` → `SYSTEM`
- `false` → `LIGHT`
- `true` → `DARK`

Migrated once on first read, then `K_IS_DARK` is left in place (read-only, no harm).

### 6.5 Resolving "system" mode

`GhsTheme` takes `ResolvedMode = LIGHT | DARK` (already resolved). The resolution happens once near the root of the tree:

```
// inside MainViewModel or AppNavigation
val mode by tweaks.getThemeMode().collectAsState(ThemeMode.SYSTEM)
val systemDark = isSystemInDarkTheme()  // Compose primitive
val resolved = when (mode) {
    ThemeMode.LIGHT -> ResolvedMode.LIGHT
    ThemeMode.DARK -> ResolvedMode.DARK
    ThemeMode.SYSTEM -> if (systemDark) ResolvedMode.DARK else ResolvedMode.LIGHT
}
GhsTheme(palette, resolved, fontTheme) { AppNavigation() }
```

`isSystemInDarkTheme()` is multiplatform (commonMain). Single source of resolution, no scattered branching.

---

## 7 · Per-app accent storage

### 7.1 Domain model

`core/domain/.../model/AppAccent.kt`:
```
@Immutable
data class AppAccent(
    val c: Color,            // saturated accent (text, icon, recommended-pill foreground)
    val lightTint: Color,    // light-mode tint surface
    val darkTintAlpha: Float = 0.20f,  // dark-mode = c.copy(alpha=darkTintAlpha) over dark surface
) {
    fun tintFor(mode: ResolvedMode): Color =
        if (mode == ResolvedMode.DARK) c.copy(alpha = darkTintAlpha) else lightTint
}
```

### 7.2 Resolution chain (`core/data/util/AppAccents.kt`)

```
object AppAccents {
    fun forRepo(
        backendAccent: String?,  // hex from backend (future — not wired yet)
        topics: List<String>,
        primaryLanguage: String?,
    ): AppAccent {
        backendAccent?.let { return AppAccent.fromHex(it) }
        topics.firstNotNullOfOrNull { TOPIC_ACCENTS[it] }?.let { return it }
        primaryLanguage?.let { LANGUAGE_ACCENTS[it.lowercase()] }?.let { return it }
        return FALLBACK_BLUE
    }
}
```

**Resolution order** (themes.md):
1. Backend-supplied hex
2. Topic match (table below)
3. Language match (Kotlin→purple, Rust→amber, Go→sage, Swift→orange, Python→amber, JS/TS→sage, …)
4. Fallback `FALLBACK_BLUE = AppAccent(c=#5E81AC, lightTint=#D8E1EC)`

Tables live as `val TOPIC_ACCENTS: Map<String, AppAccent>` and `val LANGUAGE_ACCENTS: Map<String, AppAccent>` in the same file. ~30 entries total.

### 7.3 Where it lives on the data path

**Recommendation: derive at the UI mapper layer, not at the repo data class.**

Reasoning:
- `GithubRepoSummary` (`core/domain/.../model/GithubRepoSummary.kt`) and `DiscoveryRepositoryUi` are domain models. Accent is **presentation-only data** — it depends on the palette mode (light/dark tint) and on visual tokens.
- The existing pattern (`GithubRepoSummaryMappers.kt`, `GithubUserMappers.kt`) maps domain → UI in `core/presentation/.../utils/`. Add a `repoAccent: AppAccent` field to `GithubRepoSummaryUi` and resolve it in the mapper.
- **Not persisted.** Same `(topics, language)` deterministically resolves to the same accent — recomputation is free, caching adds invalidation bugs.

**Exception:** if the backend ever returns a per-repo accent (DESIGN.md §2.4 mentions "color-thief on avatar"), that hex needs persistence to avoid recomputing on every fetch. Add a single nullable column to the existing repos table at that point — **not in this overhaul.** Defer to a future phase (§12-Q6).

### 7.4 UI-side cache

`Modifier.background(accent.tintFor(mode), shape)` recomputes per recomposition but the math is `Color.copy(alpha=…)` — single allocation, irrelevant. No memoisation needed.

---

## 8 · Module layout for silent vocabulary

**Decision: single module — `core/presentation/.../vocabulary/`.**

Reasons:
- Every feature consumes these primitives (Home feed card uses FreshnessRing; Library row uses Heartbeat; Detail screen uses WaxSeal + VersionStack). A new module forces every feature to add a dependency; we already pay that cost on `core/presentation`.
- Primitives are stateless, take `Modifier` + theme tokens. They don't need their own data/domain layer.
- ~14 files, ~1500 lines total. Below the threshold where a new module pays off.

Files:

```
core/presentation/src/commonMain/kotlin/zed/rainxch/core/presentation/vocabulary/
├── FreshnessRing.kt       // squircle icon tile + draining ring; takes (daysSinceRelease, accent, avatarUrl?)
├── Heartbeat.kt           // breathing dot, period from maintenance.heartbeat_period_s
├── StarTier.kt            // 1–5 Michelin stars from log buckets
├── WaxSeal.kt             // intact/cracked/open — three SVG glyphs via Path
├── VersionDelta.kt        // patch dot / minor 2-dots / major bar+slash
├── VersionStack.kt        // 1..7 stacked bars for skipped versions
├── PermDot.kt             // green/amber/red dot + optional halo ring
├── PlatformGlyph.kt       // android/windows/macos/linux — uses existing ic_platform_*.xml as Vector + dashed-outline variant
├── TopicGlyph.kt          // 12 topic pictograms — Canvas-drawn from silent-vocab.jsx paths
├── SignalBars.kt          // mirror strength, 0–4 bars
├── DownloadWeight.kt      // log10 sized dot
├── LicensePosture.kt      // copyleft © tile vs permissive · tile
├── Squiggle.kt            // (lives here, not in theme/, since it's vocabulary-decorative)
└── CookieBrand.kt         // brand mark composable using CookieShape from theme/
```

**`Heartbeat.kt` perf note:** infinite animation, common in lists. Use `rememberInfiniteTransition` + `animateFloat`. Pause when not visible via `isInLazyListVisibleRange()` helper or `LocalLifecycleOwner`-aware composable. See §11 Risk-4.

**Platform glyphs:** we already have `ic_platform_{android,windows,macos,linux}.xml`. Convert PlatformGlyph to a wrapper that picks between `painterResource(...)` for `on=true` and a Canvas-drawn dashed variant for `on=false`. Don't ship new platform SVGs.

---

## 9 · Resource pipeline

Compose Multiplatform 1.10.3 (per `gradle/libs.versions.toml`) supports two viable paths for vector content:

**Path A — Compose Resources `painterResource(Res.drawable.foo)` on Android-XML vector drawables.**
- We already use this for `ic_platform_*.xml`. Works on Android (native vector drawable) + JVM (parsed by Compose Resources at compile time into ImageVector).
- Limitation: only Android-XML vector format. SVG-native is not supported.
- Good for: icons we'd otherwise ship as Material Symbols / custom platform glyphs.

**Path B — `ImageVector.Builder` Kotlin DSL.**
- Build vectors at runtime from path data.
- Good for: small, dynamic vectors driven by data (e.g. WaxSeal — three states, ~5 path segments each; TopicGlyph — 12 distinct sub-paths driven by `kind: String`).

**Decision:** **Path B for vocabulary primitives** (because the JSX uses tiny inline SVGs that compose better as `Canvas` or `ImageVector.Builder` than as 12 separate XML files), **Path A for everything else** (top-bar icons, platform glyphs we already ship, etc.).

**No raster icons.** Ship no new PNGs. The existing `app_icon.png` stays, but topic glyphs / vocabulary primitives are all vector-only.

For Material Symbols Rounded (design-system.md §12) — we don't bundle Material Symbols today. Either add the `androidx.compose.material:material-icons-extended` dependency (`Icons.Rounded.Home`, etc.) or ship Material Symbols as a font file (`MaterialSymbolsRounded.ttf`, ~600KB, all icons addressable via codepoint). **Prefer the font file** — `icons-extended` is ~10MB on Android baseline APK, the font is 600KB and addressed by `Text(text="", fontFamily=MaterialSymbolsRounded)`. Decided downstream during phase 1.

---

## 10 · Sequencing + acceptance criteria

Mapped to MIGRATION.md but adapted for our actual modules (no "Library" module → we have `feature/apps`, `feature/favourites`, `feature/starred`).

### Phase 0 — Tokens + Theme scaffolding

**Deliverable:** Every token (palette × mode, type, shape, spacing, motion, thresholds, status) reachable via `Ghs.X`/`MaterialTheme.X` from anywhere. Both theme entrypoints work side-by-side.

**Files touched (new):**
- `core/presentation/.../theme/tokens/{Palette,Status,Shape,Spacing,Type,Motion,Threshold}Tokens.kt`
- `core/presentation/.../theme/locals/Local{Palette,StatusColors,TypeScale,Shapes,Spacing,Motion}.kt`
- `core/presentation/.../theme/GhsTheme.kt`
- `core/presentation/src/commonMain/composeResources/font/fraunces_{500,600,700}_italic.ttf`
- `core/presentation/src/commonMain/composeResources/font/inter_tight_{400,500,600,700}.ttf`
- `core/presentation/src/commonMain/composeResources/font/noto_sans_{jp,sc,tc,kr,devanagari,arabic,hebrew}_{regular,bold}.ttf` (open question §12-Q3)
- `core/domain/.../model/Palette.kt`, `ThemeMode.kt`, `AppAccent.kt`, `ResolvedMode.kt`
- `core/data/util/AppAccents.kt`

**Files touched (modified):**
- `core/domain/.../repository/TweaksRepository.kt` — add `getPalette()`, `setPalette()`, `getThemeMode()`, `setThemeMode()`. Deprecate old four.
- `core/data/.../repository/TweaksRepositoryImpl.kt` — add `K_PALETTE`, `K_THEME_MODE`, migration on first read.
- `composeApp/.../app/Main.kt` (or wherever `GithubStoreTheme {}` is called) — branch to `GhsTheme` once palette is wired.

**Definition of done:**
- `./gradlew :core:presentation:compileCommonMainKotlinMetadata` green.
- `./gradlew :composeApp:assembleDebug` green.
- Launching the app on Nord light + Nord dark + Cream light renders the existing UI without colour regressions (because we map M3 slots faithfully).
- Toggling Palette via a temporary debug menu cycles through all 4 palettes and persists across app restart.
- Fraunces renders in a temporary `Text(...)` call site without tofu on Android + JVM.
- Migration: a user with `K_THEME = "AMBER"` + `K_IS_DARK = true` on disk launches and lands on `Palette.CREAM` + `ThemeMode.DARK`.

### Phase 1 — Silent vocabulary primitives

**Deliverable:** All 14 primitives from §8 callable, each renders correctly in all 8 (palette × mode) combinations.

**Files touched (new):**
- `core/presentation/.../vocabulary/*.kt` (14 files, see §8)
- One Compose Preview composable per primitive (Android only — Previews work there) under `core/presentation/src/androidMain/...`.

**Definition of done:**
- Visual diff against `silent-vocab.jsx` reference. Each primitive matches CSS reference within ~5% tolerance.
- `Heartbeat` does not run animations when off-screen (LazyList scroll verification).
- Build green; lint green.

### Phase 2 — Reusable cards, chips, buttons, rows

**Deliverable:** `RepoCard`, `AppRow`, `SetRow`, `Chip`, `GhsButton`, `Section`, `BottomNav`, `TopBar`, `IconShell`, `InstallPanel`, `UpdateBanner`, `IntegrityCard`, `IdentityCard`, `ConnectCard` per design-system.md §10.

**Files touched:**
- `core/presentation/.../components/v2/{RepoCard,AppRow,SetRow,Chip,GhsButton,Section,BottomNav,TopBar,IconShell,InstallPanel,UpdateBanner,IntegrityCard,IdentityCard,ConnectCard}.kt`

Existing `ExpressiveCard`, `GithubStoreButton`, `RepositoryCard` stay; new components live in `components/v2/`. Phase 3+ swaps consumers; old files removed in a final cleanup commit per migration playbook §"Behaviour parity over visual parity".

**Definition of done:**
- Sample screen wires every new component into a single scrollable demo (under `feature/tweaks` as a hidden debug entry, or under a new `feature/dev-profile` debug screen).
- Each component takes only tokens + content slots — zero hex/dp literals (Konsist check is overkill; eyeball + grep).

### Phase 3 — Home feed migration

**Deliverable:** `feature/home` swap to v2 components. Behaviour parity, visual replacement.

**Files touched:**
- `feature/home/presentation/src/commonMain/kotlin/zed/rainxch/home/presentation/HomeRoot.kt` and child composables
- ViewModel left alone unless data fields are missing (per-app accent resolution moves to the UI mapper — see §7.3).

**DoD:** Home renders cards in Nord light + dark identically to handoff `home.jsx`. Existing acceptance test in `composeApp/.../HomeScreen*Test.kt` (if any) passes.

### Phase 4 — Apps / Favourites / Starred (the "Library" trio)

We don't have a single Library module. The handoff "Library" collapses three of ours into one screen logically — but **don't merge the modules**. Migration plan:

- `feature/apps` is the primary visual surface; takes the "Library" treatment (sections: Updates, Installed, Recently used).
- `feature/favourites` and `feature/starred` get re-skinned with the same `AppRow` component but keep their own routes (the IA collapse from MIGRATION.md is a UX choice; the module collapse is not — too risky for one PR).
- The bottom-nav 4→3 reduction (per MIGRATION.md §"Recommended order #6") is **deferred to phase 7**.

**Files touched:** `feature/apps/presentation`, `feature/favourites/presentation`, `feature/starred/presentation`.

**DoD:** All three render with v2 components. Updates badge count visible on the Apps tab.

### Phase 5 — Profile + Settings (Tweaks)

**Deliverable:** `feature/profile`, `feature/tweaks` migrated. Tweaks gets the new **Palette + ThemeMode** two-axis picker UI. AMOLED toggle remains under "Dark mode" group.

**Files touched:** `feature/profile/presentation/*`, `feature/tweaks/presentation/*`.

**DoD:** Old `AppTheme` enum unused in UI code; remaining references are in the deprecated repo methods + the migration table.

### Phase 6 — Details screen

**Deliverable:** `feature/details` — the highest-fidelity screen, per MIGRATION.md.

**Files touched:** `feature/details/presentation/*`.

**DoD:** Hero block uses Fraunces displaySm + FreshnessRing on the app icon; install panel uses WonkySquircle; Integrity card uses `successT` background.

### Phase 7 — Bottom nav reduction + remaining screens

**Deliverable:**
- Bottom nav 4→3 (Home, Library-as-Apps, Profile) + detached search FAB.
- `feature/search`, `feature/auth`, `feature/dev-profile`, "What's new" sheet, mirror picker, external import wizard.
- Onboarding tooltip ("Settings moved to Profile") on first launch post-upgrade (`tweaksRepo.firstLaunchAfterV2`).

**DoD:** All screens consume `GhsTheme`. Zero references to `GithubStoreTheme()` outside the deprecation shim.

### Phase 8 — Deprecation cleanup

**Deliverable:** Delete `AppTheme.kt`, deprecated `TweaksRepository` methods, old `Theme.kt` schemes, `Color.kt` ocean-blue constants, `ExpressiveCard.kt`, `GithubStoreButton.kt`, `RepositoryCard.kt`. The deprecation shim `Theme.kt → GhsTheme` is removed.

**DoD:** `grep -rn 'AppTheme\|GithubStoreTheme\|isDarkTheme: Boolean' core feature` returns zero hits. `:composeApp:assembleDebug` green.

---

## 11 · Risk register

Likelihood × impact rated 1–5. Score = L × I.

| # | Risk | L | I | Score | Mitigation |
|---|---|---|---|---|---|
| 1 | `WonkySquircleShape` arc math wrong → visible artifacts (over/under-shoots, mis-aligned corners) | 3 | 3 | 9 | Implement with Skia debug overlay turned on in a dev preview screen. Diff against a CSS reference rendered at 2x in a screenshot. Have the `AbsoluteRoundedCornerShape` fallback (§3.4) flag-ready. |
| 2 | Fraunces italic doesn't render on JVM Linux (system font fallback to bold-not-italic) | 2 | 4 | 8 | Ship the TTF in `composeResources/font/` (bundled). Compose Resources packages fonts into the jar; Skia loads from the bundle, not the system. Same path as `inter_*.ttf` today. |
| 3 | Theme migration loses user setting → user re-themes app on first v2 launch | 2 | 4 | 8 | Migration is idempotent (only runs when `K_PALETTE` is empty). Add a Logcat/Kermit log line on each migration so support can verify. Keep `K_THEME`/`K_IS_DARK` on disk untouched in case we ever need to rollback. |
| 4 | `Heartbeat` infinite animation in a list of 50 cards eats CPU + battery | 4 | 3 | 12 | Always-on `InfiniteTransition` is per-composable: it pauses when the composable leaves composition. **In a `LazyColumn`, recycled items leave composition automatically.** Bigger concern: 8 visible cards on screen × continuous repaint at 60 Hz. Mitigations: (a) tie the transition's `targetValue` keyframes to discrete steps so the GPU has few invalidations; (b) gate behind `LocalLifecycleOwner` paused → no animation; (c) provide a `Tweaks → Reduce motion` switch that returns a static dot (§12-Q4). |
| 5 | M3 colorScheme mapping mismatch — third-party widget (Markdown renderer, Landscapist placeholder) renders wrong colour on a dark Cream theme | 3 | 2 | 6 | Audit each consumer (`markdown-renderer`, `landscapist-coil`, navigation transition) for which `colorScheme.X` slots they read. Document in `PaletteTokens.kt` next to each mapping. Snapshot test the markdown renderer pass on all 8 (palette × mode) variants. |
| 6 | Per-app accent resolution is non-deterministic (topic list arrives in different order from backend) | 2 | 3 | 6 | `topics.firstNotNullOfOrNull { TOPIC_ACCENTS[it] }` — if backend reorders, accent changes. Mitigation: sort topics alphabetically before resolution. Or use the explicit GitHub API ordering (which is creation order — stable across calls). Note in `AppAccents.kt` docstring. |
| 7 | Noto fallback adds ~30 MB to APK | 4 | 2 | 8 | Open question §12-Q3 — decision needed. If we drop Noto, Android system fallback covers it (Android has CJK + Devanagari fonts in /system/fonts); JVM gets tofu on user-installed-only systems. Compromise: ship Noto on JVM only (split sourceSet), Android relies on system. |
| 8 | Two-axis picker UI breaks muscle memory — existing users hit `Tweaks → Theme` and find new layout | 2 | 2 | 4 | Tooltip on first launch per MIGRATION.md §"Risky areas". Keep the section title as "Theme" not "Appearance" so search-by-label still finds it. |

Top three by score: #4 Heartbeat perf, #1 wonky-squircle math, #7 Noto size.

---

## 12 · Open questions for the user

These need a decision before phase 0 begins (or, marked clearly, during the phase). One answer per number.

**Q1 — Fraunces font axis.** Ship static italic TTFs (3 weights × italic, ~600KB) or the variable TTF (~250KB but no axis API in Compose Resources)? Recommend static.

**Q2 — Inter Tight replacement of Inter.** DESIGN.md §3.4 forbids "Inter" (the wider one). Today we ship Inter. Confirm: replace `inter_*.ttf` files with `inter_tight_*.ttf` (same `Res.font.inter_X` keys, different file)? Or rename keys to `inter_tight_X` and migrate `Type.kt:21-30` references? Recommend rename — keeps git history clear.

**Q3 — Noto Sans bundling.** Ship 7 Noto Sans families (JP/SC/TC/KR/Devanagari/Arabic/Hebrew) × 2 weights = 14 TTFs, ~30 MB APK. Or rely on Android system fallback (covers everything) + accept JVM tofu on minority scripts? Or split sourceSet: Android = no Noto, JVM = bundle Noto? Recommend the split approach (Q3-C).

**Q4 — "Reduce motion" Tweak.** Add a new boolean to `TweaksRepository` for users who want `Heartbeat` to render as a static dot? Or rely on the OS-level reduce-motion setting (Android `Settings.Global.TRANSITION_ANIMATION_SCALE`, JVM has none)? Recommend new in-app toggle — OS signal is unreliable and the JVM has no equivalent.

**Q5 — AMOLED black mode in v2.** Today `getAmoledTheme()` forces surfaces to true black when dark. Keep this as a sub-mode of `ThemeMode.DARK` (rendered as a toggle in Tweaks, not a third mode)? Recommend yes — three top-level modes (LIGHT/DARK/SYSTEM) + a "Pure black surfaces when dark" sub-toggle.

**Q6 — Backend per-repo accent.** DESIGN.md §2.4 says "When backend doesn't supply one, derive from the dominant color of the avatar (color-thief style) and store it." Today the backend doesn't supply one and we don't color-thief. Confirm: in this overhaul, **don't** add color-thief. Resolve accent client-side from topics → language → fallback only. Backend-supplied accent is a future feature.

**Q7 — Material You / dynamic colour.** themes.md §"Disallowed combinations" forbids Material You on Android 12+ overriding our palettes. Today we ship `AppTheme.DYNAMIC`. Migration table (§6.4) folds `DYNAMIC → NORD`. Confirm: drop `AppTheme.DYNAMIC` entirely, no opt-in to dynamic colour anywhere? (Recommend yes.)

**Q8 — Two-axis Tweaks UI breakage.** The new "Palette" + "Theme mode" picker is a different layout than the current single-list theme selector. Acceptable to break the current Tweaks visual on the day we ship phase 5? Or do we need a one-release transition where the old screen still exists behind a debug flag? Recommend: ship clean break + add the one-time tooltip (§11 Risk-8).

**Q9 — Module for vocabulary.** I recommended single module (`core/presentation/vocabulary/`) over a new `core/vocabulary/` module. Confirm? A new module would force `core/presentation` to depend on it and every feature already depends on `core/presentation`, so no consumer benefit.

**Q10 — Where does the palette+mode resolution live?** Recommended (§6.5) at the root of `AppNavigation` via `MainViewModel`. Alternative: introduce a tiny `ThemeViewModel` that exposes a `StateFlow<ResolvedTheme>` derived from palette + mode + system-dark + amoled. Recommend `MainViewModel` extension — it already exists, less DI churn.

---

## Summary — what ArchitectUX is on the hook for in phase 0

Read this section before opening a new chat to start phase 0.

1. Write `core/presentation/.../theme/tokens/{Palette,Status,Shape,Spacing,Type,Motion,Threshold}Tokens.kt` from `tokens.json`.
2. Write `WonkySquircleShape : Shape` + `CookieShape : Shape` from §3.2 and §4.1.
3. Add Fraunces + Inter Tight (+ Noto per Q3) to `composeResources/font/`.
4. Add `Palette`, `ThemeMode`, `ResolvedMode`, `AppAccent` enums + data classes.
5. Add `getPalette`/`setPalette`/`getThemeMode`/`setThemeMode` to `TweaksRepository` + impl + migration.
6. Write `GhsTheme.kt` + 6 composition locals.
7. Wire `GhsTheme` into `composeApp/.../app/Main.kt` (or the top-level theming call site) behind a runtime flag so we can flip between old + new during phases 1–7.
8. One screenshot per (4 palettes × 2 modes) of a temporary debug screen showing a card, a button, an FAB, and a status row — to verify the colour mapping landed.

End.
