# P12.5 — Tweaks Redesign (V2)

> Hub-and-spoke IA for the Tweaks feature. Replaces the single-scroll mega-list in `feature/tweaks/presentation/TweaksRoot.kt` with a category hub + dedicated drill-in screens. Reuses the existing design-system vocabulary (`Radii.row`, `WonkySquircleShape.*`, outlined Surface row, `FloatingPill`, `SectionText`/`Squiggle`, `ToggleSettingCard`, `GhsBottomSheet`, `GhsConfirmDialog`, `GhsDropdownMenu`, `PlatformGlyph`). No new fonts. No square corners. No KDoc.
>
> Author: ArchitectUX. Status: V2, post UX-Research critique + product-owner decisions. Supersedes V1 in full.

---

## 0. Change log — V1 → V2

Citations point to `.design/P12_5_TWEAKS_RESEARCH_REVIEW.md` (review) and `.design/P12_5_ABOUT_PLACEMENT_RESEARCH.md` (placement).

| # | Area | V1 said | V2 says | Driver |
|---|---|---|---|---|
| 1 | Telemetry UI | absent | first-class opt-out, lives in new **Privacy** sub-screen | review C1 |
| 2 | Library cleanup | one screen mixing cache + clipboard + history + telemetry-shaped concerns | **split** into **Storage** (APK cache) + **Privacy** (telemetry, clipboard, hide-seen, viewed history) | review M1 |
| 3 | About leaf | "About" sub-screen with feedback nested inside | **App info** sub-screen (About + Licenses + Privacy policy + version) **plus** dedicated **Send feedback** hub row at bottom; desktop also gains `MenuBar` | placement Option E + review M7 |
| 4 | Master proxy | derived from per-scope equality at load | **persisted** as its own record + `useMaster: Boolean` per scope; one-time migration | review C2 |
| 5 | "Direct" rename | `proxy_none` → "Direct" | `proxy_none` → **"No proxy"** (mode pill + body copy updated everywhere) | review C3 |
| 6 | Search in hub | rejected | **shipped**, `WonkySquircleShape.Search` field under topbar, filters by title + subtitle; deep-link routes spec'd as a follow-up | review C5 |
| 7 | Empty / loading / error | partial per screen | every sub-screen carries a standardized states sub-section; reversible → snackbar+Undo, destructive → `GhsConfirmDialog`, scope-changing → snackbar confirming scope | review C4 |
| 8 | Override toggle | "Use main connection" switch | 2-segment chooser per scope: `[ Use main ] [ Custom… ]` | review M2 |
| 9 | Proxy modes | Direct / System / HTTP / SOCKS | **No proxy** / System / **HTTP/HTTPS** / **SOCKS5** + "Paste full URL" affordance; PAC explicit v1 non-goal | review M3 |
| 10 | Border contrast | `outlineVariant.copy(alpha = 0.55f)` | **`outline`** at full opacity (with documented per-palette contrast audit gate before merge) | review M4 |
| 11 | Tap targets / a11y | unspec'd for nested icon buttons | every interactive 48dp min; explicit nested-click pattern; row semantics + status pill `liveRegion`; chevron `contentDescription = null` | review M5 |
| 12 | i18n | English-only rewrite | mandate `pluralStringResource` for counted labels; 32-char subtitle cap with rightmost-token drop on overflow; English-first ship policy + translator CSV handoff | review M6 |
| 13 | Cross-platform rows | hidden on inapplicable platform | shown with `tertiaryContainer` "Android only" / "Desktop only" subtitle badge; clicking routes to centered empty state | review M8 |
| 14 | Hub blocking | 4 blocks for 10 rows | **5 blocks** for 11 rows (Look & feel, Connectivity, Installs & updates, Privacy & data, App); Translation moves into Connectivity; Access tokens into Privacy & data | review M9 (trimmed) |
| 15 | Restart UX | snackbar local to Language screen | **persistent banner** sourced from `TweaksRepository.needsRestartReasons`; shows on hub + every sub-screen until restart | review M10 |
| 16 | Nits | various | applied N1–N12 (see §6 + per-screen specs) | review nits |
| 17 | Connection mode label | "Direct" | "No proxy" | review C3 |
| 18 | Update interval | 3h / 6h / 12h / 24h | "Every 6 hours / Every 12 hours / Daily / Manual only" (drops 3h per rate-limit risk) | review N3 |
| 19 | Installer rename | "System installer (Default)" | "System installer" | review N7 |
| 20 | Licenses row | placeholder, hideable | **shipped**, sourced from `gradle-license-plugin` JSON; row in App info | review N8 |
| 21 | Icon tile tinting | unstated | explicitly **uniform** `surfaceContainerHigh` tile with `onSurfaceVariant` icon tint across all rows | review N9 |
| 22 | "Follow system" subtitle | static "Follow system" | "Follow system · en-US" (resolved tag in parens) | review N12 |

V1 strengths preserved: outlined `Radii.row` vocabulary across all sub-screens, sentence-case section headers (no more `.uppercase()`), dynamic-state hub subtitles, §3.5 Translation provider radio redesign, empty-state framing for custom forges, §7.4 explicit "did not change" discipline (now §7.6).

---

## 1. Information architecture

### 1.1 Final category list (the Tweaks hub)

The hub is grouped into **5 visual blocks**, 11 entry rows total. Each block has its own `SectionText` + `Squiggle` underline, then a stack of entry rows. No top-level "Settings" section header — the topbar already says "Tweaks".

| Block | Order | Entry row | Icon (Material outlined) | Subtitle (dynamic state) | Drill-in | Platforms |
|---|---|---|---|---|---|---|
| **Look & feel** | 1 | Appearance | `Palette` | Palette + mode, e.g. "Nord · Dark" | `TweaksAppearanceScreen` | All |
| | 2 | Language | `Translate` | Language name, e.g. "English (US)" or "Follow system · en-US" | `TweaksLanguageScreen` | All |
| **Connectivity** | 3 | Connection | `Wifi` | "No proxy" / "HTTP 127.0.0.1:1080" / "System proxy" / "127.0.0.1:1080 · 1 override" | `TweaksConnectionScreen` | All |
| | 4 | Sources | `Hub` | "GitHub + N forges" | `TweaksSourcesScreen` | All |
| | 5 | Translation | `GTranslate` | Provider name + auto state, e.g. "DeepL · auto on" | `TweaksTranslationScreen` | All |
| **Installs & updates** | 6 | Install method | `InstallMobile` | Installer + Ready / Needs permission badge | `TweaksInstallScreen` | All (Android-only behavior; desktop shows badge) |
| | 7 | Update behavior | `Update` | "Every 6 hours" / "Manual only" / "Check failed" badge | `TweaksUpdatesScreen` | All |
| **Privacy & data** | 8 | Storage | `Inventory2` | Live cache size, e.g. "Downloads: 124 MB" | `TweaksStorageScreen` | All |
| | 9 | Privacy | `PrivacyTip` | Compact state, e.g. "Telemetry off · clipboard on" | `TweaksPrivacyScreen` | All |
| | 10 | Access tokens | `VpnKey` | "N tokens" (plural-aware) / "No tokens yet" | `HostTokensScreen` (existing) | All |
| **App** | 11 | App info | `Info` | App version, e.g. "1.8.3" | `TweaksAppInfoScreen` | All |
| | 12 | Send feedback | `Feedback` | "We read every report." | (opens `FeedbackBottomSheet` directly) | All |

Notes on the IA:

- **Translation moves to Connectivity.** It has network credentials + auto-translate state — tighter fit than "Look & feel." Review M9 picked at this; we agree with the move, but trimmed to 5 blocks.
- **Access tokens lives under Privacy & data.** They're sensitive credentials, not network plumbing. Reads "Privacy & data → Access tokens" much more naturally than "Network & data → Access tokens."
- **Install method shows on both platforms** with platform badging (M8 fix). Desktop click → centered empty state explaining the constraint.
- **Send feedback is a hub row, not nested.** Per placement Option E + review M7. One tap from the hub.
- **Search-in-hub** sits under the topbar, above the first block (see §2.5).

### 1.2 Platform conditional rules

- **Install method**: row visible everywhere. On desktop, subtitle badge "Android only" (`tertiaryContainer` pill). Clicking routes to a centered empty state.
- **Update behavior**: cross-platform. On desktop, the WorkManager interval picker is replaced by a single "Check on launch" toggle (see §3.7).
- **Storage**: fully cross-platform (per-platform downloads dir).
- **Appearance**: shows scrollbar + content width only on desktop. AMOLED only when dark mode is resolved on either platform.
- **Privacy**: cross-platform. Telemetry opt-out shows on both platforms.
- **App info**: cross-platform. Identical content; desktop additionally exposes via `MenuBar` (see §8).
- **Send feedback**: cross-platform. Same `FeedbackBottomSheet` on both.

### 1.3 What stays where

| Existing thing | New home |
|---|---|
| `MirrorPickerScreen` | Reached from **Sources** (existing nav route preserved) |
| `HostTokensScreen` | Reached from **Access tokens** hub row (existing nav route preserved) |
| `SkippedUpdatesScreen` | Reached from **Update behavior** |
| `HiddenRepositoriesScreen` | Reached from **Update behavior** |
| `WhatsNewHistoryScreen` | Reached from **App info** |
| `FeedbackBottomSheet` | Opens **directly** from the Send feedback hub row (no intermediate screen) |
| `CustomForgesDialog` | Replaced by `CustomForgesSheet` opened from **Sources** |
| `ClearDownloadsDialog` | Opened from **Storage** |
| Clipboard / hide-seen / viewed-history controls | Moved into **Privacy** sub-screen |
| Telemetry opt-out (new UI) | **Privacy** sub-screen |
| About / Licenses / Privacy policy link | Folded into **App info** sub-screen |

---

## 2. Hub screen — `TweaksScreen` (the new root)

### 2.1 Topbar

**Pattern**: plain large title, no FloatingPill.

Rationale: FloatingPill is the "in-content overlay" pattern used in Search + Auth where the topbar floats over scrolling hero content. Tweaks has no hero. Tweaks is a deep settings hub — it wants a steady, anchored title.

Spec:

- Material 3 `LargeTopAppBar` with collapsing behavior (`TopAppBarDefaults.exitUntilCollapsedScrollBehavior`).
- Title: "Tweaks" (`Res.string.tweaks_title`).
- Title font: Geist SemiBold, 28sp expanded → 20sp collapsed.
- Leading: back arrow, contentDescription "Back".
- Trailing: none. Search lives below the topbar (§2.5), not in it.
- Container color: `colorScheme.background`.

### 2.2 Category entry row — `TweaksEntryRow`

The hub is a list of 11 identical rows grouped into 5 sub-sections. The row is the shared primitive, lives at `feature/tweaks/presentation/components/TweaksEntryRow.kt`.

**Shape & color**:

- Outer `Surface(shape = Radii.row, color = colorScheme.surfaceContainerLow, border = BorderStroke(1.dp, colorScheme.outline))`.
- **Border**: `colorScheme.outline` at **full opacity**. Drops V1's `outlineVariant.copy(alpha = 0.55f)` per review M4 (failed WCAG 1.4.11 on Cream light). Implementer must screenshot Nord/Cream/Forest/Plum × Light/Dark/AMOLED matrix and confirm border passes 3:1 contrast before merge. Fallback if `outline` reads too heavy on dark: `outline.copy(alpha = 0.55f)` but only after measured contrast > 3:1 on Cream light.
- Inner `Row`, `Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 14.dp)`.

**Visual zones**, left → right:

1. **Icon tile**: 40.dp square clipped to `Radii.chip`, background **uniformly** `colorScheme.surfaceContainerHigh`, padded 8.dp, icon tinted `onSurfaceVariant`. Never per-row colored. (review N9)
2. **Two-line text column** (`Modifier.weight(1f)`):
   - Title: `titleMedium`, `onSurface`, `FontWeight.SemiBold`.
   - Subtitle: `bodySmall`, `onSurfaceVariant`, max 1 line, ellipsize end. **Dynamic** — reflects current domain state.
3. **Optional trailing badge slot** (status pill): `tertiaryContainer` background, `Radii.chip` shape, `labelSmall`, `FontWeight.Medium`. Used by Install method ("Ready" / "Needs permission" / "Android only"), Update behavior ("Check failed"). When present, the row's `Row` is `semantics { liveRegion = LiveRegionMode.Polite }` so TalkBack announces re-evaluation when state changes.
4. **Trailing chevron**: `Icons.AutoMirrored.Filled.ChevronRight`, 24.dp, tinted `onSurfaceVariant`, `contentDescription = null` (decorative).

**Tap targets & nested clicks** (review M5):

- Entire row is 48dp+ min height (icon tile 40.dp + 14.dp padding × 2 = 68dp).
- Any inner `IconButton` (e.g. delete on Custom forges row) wraps in `Modifier.size(48.dp).clip(Radii.chip)`.
- Compose 1.5+: outer `Modifier.clickable` and inner `IconButton.onClick` correctly dispatch to the nearest handler — no manual `consumeWindowInsets` needed. Implementer must verify in a smoke test (tap the inner icon, confirm only its handler fires).

**A11y semantics** (review M5):

```
Modifier.semantics {
    role = Role.Button
    contentDescription = "$title. $subtitle. Double-tap to open."
}
```

Chevron `contentDescription = null`. Status pill participates via `liveRegion = Polite`. Override segment (in Connection sub-screen) announces "Discovery override on, custom proxy settings for Discovery" when expanding.

**Press feedback**: standard ripple inside the squircle border. Spring scale-on-press: 0.97× on Android, 0.985× on desktop (review N1), `MediumBouncy` per D10.

**Component sketch**:

```
@Composable
fun TweaksEntryRow(
    title: String,
    icon: ImageVector,
    subtitle: String? = null,
    badge: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
)
```

### 2.3 Restart-required banner (new — review M10)

`TweaksRepository` exposes `needsRestartReasons: StateFlow<Set<RestartReason>>` and `clearRestartReasons()`. Reasons enum: `LANGUAGE`, `THEME_MIGRATION`, `TELEMETRY_TOGGLE`.

Banner placement:

- **Hub**: top of the `LazyColumn`, above the search field. Sticky during collapsing topbar scroll.
- **Sub-screens**: top of each sub-screen's `LazyColumn`, above the first card.

Banner visual:

- `Surface(shape = Radii.row, color = colorScheme.tertiaryContainer, border = BorderStroke(1.dp, colorScheme.outline))`.
- Body: "Some changes need a restart to apply." If multiple reasons, append clarifier line: "Affected: language, theme."
- Two trailing buttons: "Restart now" (`WonkySquircleShape.CtaPrimary`, `FilledTonalButton`) + "Later" (text button). "Later" dismisses for the current session only — banner returns next launch until restart.
- "Restart now" routes to the existing app-restart codepath the Language flow already uses.

Banner disappears only when `needsRestartReasons` is empty (i.e. app process restarted).

### 2.4 Section header

Reuses `SectionHeader` from `feature/tweaks/presentation/components/SectionText.kt` (`titleLarge` + `Squiggle`). Section labels (sentence-case, **never** uppercase):

- "Look & feel"
- "Connectivity"
- "Installs & updates"
- "Privacy & data"
- "App"

### 2.5 Search-within-hub (new — review C5)

A single inline filter field, lives between the restart banner (if visible) and the first section header.

- `OutlinedTextField`, shape **`WonkySquircleShape.Search`**, leading icon `Icons.Default.Search`, trailing clear icon (when non-empty), placeholder "Search settings" (sentence-case, no period).
- ~20 lines of Compose. No new VM logic — pure presentational `remember { mutableStateOf("") }`. Filters the static row list in `TweaksScreen` by `title.contains(query, ignoreCase = true) || subtitle?.contains(query, ignoreCase = true) == true`.
- When query is non-empty, section headers hide and rows render in a flat list.
- When query yields zero matches: empty-state card (outlined `Radii.row`), `Icons.Outlined.SearchOff` icon tile + "No settings match '<query>'."

Deep-link routes (`githubstore://tweaks/<category>`) are **spec'd here as a follow-up ticket** (P12.5.1, post-launch). Not in v1 scope. Route names when implemented: `appearance`, `language`, `connection`, `sources`, `translation`, `install-method`, `updates`, `storage`, `privacy`, `access-tokens`, `app-info`, `send-feedback`.

### 2.6 Vertical rhythm

```
LazyColumn(contentPadding = 16.dp horizontal, top = 8.dp, bottom = bottomNavHeight + 32.dp)
  restart banner (if reasons non-empty)
  Spacer(12.dp)
  search field
  Spacer(16.dp)
  section header 1
  Spacer(8.dp)
  entry rows (separated by Spacer(8.dp))
  Spacer(24.dp)
  section header 2
  ...
```

### 2.7 Empty / loading / error states (hub)

- **Empty**: not possible — every row is always present.
- **Loading**: hub renders synchronously from `TweaksState`. The only field that can lag is App info's `versionName`; placeholder `"—"` until populated.
- **Search empty result**: see §2.5.
- **Error**: hub has no async sources of its own; per-row dynamic subtitles read from already-loaded `TweaksState`. If a subtitle's underlying flow errors (e.g. cache size read fails), the row still renders with a graceful fallback subtitle ("Tap to manage").

---

## 3. Per-sub-screen specs

Conventions shared by every sub-screen:

- **Topbar**: Material 3 `MediumTopAppBar` + back arrow + title. No subtitle line in the bar.
- **Container**: `Scaffold(containerColor = colorScheme.background)` + `LazyColumn`, `contentPadding = 16.dp` horizontal, 8.dp top, `bottomNavHeight + 32.dp` bottom.
- **Restart banner**: §2.3 banner renders at the top of every sub-screen when `needsRestartReasons` non-empty.
- **Cards**: all use `Radii.row` outlined Surface (`surfaceContainerLow` + full-opacity `outline` border per M4).
- **Section sub-headers inside a sub-screen**: `titleSmall` + `FontWeight.SemiBold`, `onSurface`, padded `start = 4.dp, top = 16.dp, bottom = 8.dp`. No `Squiggle` inside sub-screens.
- **Save state**: per-screen settings persist immediately. Explicit Save buttons only on Translation credentials + Connection proxy editor (validation-gated).
- **Snackbar**: each sub-screen has its own `SnackbarHostState`, subscribed to `TweaksViewModel.events` filtered to events applicable to it.
- **Standardized empty / loading / error pattern**:
  - Reversible action → snackbar with Undo.
  - Irreversible destructive (clear cache, clear history, remove token, remove forge) → `GhsConfirmDialog`.
  - Scope-changing mode switch (proxy override on/off, telemetry off→on) → snackbar confirming scope of change, e.g. "Downloads now uses the main connection."

### 3.1 Appearance — `TweaksAppearanceScreen`

**Title**: "Appearance"

**Layout** (top → bottom):

1. **Theme card** — outlined `Radii.row`.
   - Sub-header: "Theme".
   - **Mode segment** (3-way): Light / Dark / Follow system. Uses `ModePillSegment` (§5.1).
   - 16.dp spacer.
   - **Palette grid** (`FlowRow`): 4 `PaletteSwatch`es — Nord, Cream, Forest, Plum.
   - `AnimatedVisibility(resolvedDark)` AMOLED inline toggle.
2. **Text & layout card** — outlined `Radii.row`, inner `Column`.
   - Sub-header: "Text & layout".
   - Row: System font toggle. Title "Use system font", subtitle "Use your device's default typeface instead of Geist."
   - Row (desktop only): Show scrollbar toggle.
   - Row (desktop only): Content width segment (`Compact` / `Wide` / `Extra wide`).

**Empty / loading / error**: no async loads; state is synchronous from `TweaksState.appearance`. Palette change → crossfade (D10) + snackbar "Theme applied." Theme migrations that need restart (rare, only on major palette schema change) push `RestartReason.THEME_MIGRATION` into `needsRestartReasons`.

**Gotchas**: AMOLED visibility binds to `resolvedDark` (System + dark OS still shows). When mode segment switches to Light explicitly, AMOLED collapses with `AnimatedVisibility(false)`. State machine: AMOLED visible ⇔ `(mode == Dark) || (mode == FollowSystem && systemIsDark)`. (review N11)

### 3.2 Language — `TweaksLanguageScreen`

**Title**: "Language"

**Layout**:

1. Top intro card (outlined `Radii.row`):
   - Title "App language".
   - Body "The app restarts when you change this so all screens reload."
2. Search field — `OutlinedTextField`, `WonkySquircleShape.Search` shape, leading search icon, placeholder "Search languages".
3. Language list — each language a `Radii.row` outlined Surface row:
   - Language name in its own locale (e.g. "Deutsch", "Polski").
   - Subtitle: English name in parentheses if different + tag (`pl-PL`) in `Geist Mono labelSmall`.
   - Trailing `Icons.Default.Check` (24.dp, `colorScheme.primary`) when selected.
4. First row is always "Follow system" with `Icons.Outlined.PhoneAndroid` (Android) / `Icons.Outlined.Computer` (desktop). Subtitle includes the resolved tag, e.g. "Follow system · en-US" (review N12).

**Validation / events**: selecting a language fires `OnAppLanguageSelected(tag)` → repository sets language **and** pushes `RestartReason.LANGUAGE` into `needsRestartReasons`. The §2.3 restart banner takes over; no snackbar from this screen.

**Empty / loading / error**:
- Empty list: never (35+ languages baked in).
- Search empty: same pattern as §2.5 ("No languages match '<query>'").
- Error: not possible (static list).

### 3.3 Connection — `TweaksConnectionScreen` (proxy redesign)

This screen is the centerpiece. Full deep-dive in §4.

### 3.4 Sources — `TweaksSourcesScreen`

**Title**: "Sources"

**Layout**:

1. Top intro card (outlined `Radii.row`):
   - Title "Where the app looks for repositories".
   - Body "The app searches GitHub by default. You can route it through a regional mirror or add custom Forgejo / Gitea hosts."
2. **Mirror row** (outlined `Radii.row`) → `MirrorPickerScreen`.
   - Title "GitHub mirror". Subtitle: "Default (github.com)" or selected mirror display name.
3. Sub-header "Custom forges".
   - If `customForgeHosts.isEmpty()`: empty-state outlined row, title "Add a Forgejo or Gitea host", subtitle "We already know about codeberg.org and gitea.com. Add others here.", trailing `Icons.Default.Add`. Opens `CustomForgesSheet`.
   - If non-empty: list of host rows.
     - Title in `Geist Mono labelLarge`, +1 weight bump for visual parity with proportional text (review N5).
     - Subtitle "Added manually" or "Suggested by URL parse".
     - Trailing `Icons.Outlined.DeleteOutline` inside a 48dp `IconButton.size(48.dp).clip(Radii.chip)`. On click → `GhsConfirmDialog` ("Remove `git.disroot.org`? Repos hosted there will stop appearing in search.").
   - "Add another" row below the list — outlined `Radii.row`, `Icons.Default.Add` icon tile, title "Add another host". Opens `CustomForgesSheet`.

**`CustomForgesSheet`** (new, replaces `CustomForgesDialog`):

- `GhsBottomSheet` with `WonkySquircleShape.Sheet`.
- Single `OutlinedTextField` "Hostname", placeholder "code.example.org", shape `WonkySquircleShape.Search`.
- Helper text: "Enter the bare hostname. We'll add https:// and validate it's a Forgejo or Gitea instance."
- Inline error states (existing `customForgeError`).
- CTA: "Add host", full-width `WonkySquircleShape.CtaPrimary`, disabled until non-blank + passes validation.

**Empty / loading / error**:
- Empty (no forges added): the empty-state row above is the empty state.
- Loading (validating new host): inline progress indicator in the sheet's CTA, button text "Validating…"
- Error: inline `supportingText` on the field; failures during validation surface as a snackbar on the Sources screen after the sheet dismisses, e.g. "Couldn't reach `code.example.org`."
- Reversible host removal: snackbar with Undo (5s window) after `GhsConfirmDialog` confirms.

### 3.5 Translation — `TweaksTranslationScreen`

**Title**: "Translation"

**Layout**:

1. **Provider card** (outlined `Radii.row`):
   - Sub-header: "Provider".
   - 5 stacked **provider radio rows**:
     - Outlined `Radii.chip` Surface each. Radio button + icon tile + title + 1-line description.
     - Google: "Free, no setup."
     - DeepL: "High quality. Requires API key."
     - Microsoft: "Free tier. No-Trace mode by default."
     - LibreTranslate: "Open source. Self-hostable."
     - Youdao: "Best for Chinese."
   - Selecting a row persists provider choice immediately and reveals credentials card.
2. **Credentials card** (outlined `Radii.row`) — only when provider needs credentials (i.e. not Google).
   - Sub-header: "{Provider name} credentials".
   - Body: provider-specific help + "Get a free API key →" link.
   - Form fields: `OutlinedTextField`s with `Radii.chip` shape.
   - Visibility toggles on secret fields.
   - Save button: `WonkySquircleShape.CtaPrimary` `FilledTonalButton`, full-width, disabled until form valid.
3. **Auto-translate card** (outlined `Radii.row`):
   - Sub-header: "Auto-translate READMEs".
   - Toggle: title "Translate READMEs automatically", subtitle "When opening a repo, translate the README into your target language."
   - When enabled: target language picker drill-row that **reuses §3.2 list** filtered to `SupportedTranslationLanguages.all` (review N2 — same picker, not a parallel one).

**Empty / loading / error**:
- No provider selected: Google is always the implicit default; never empty.
- Credentials missing for non-Google: one-line warning inside credentials card "No key yet — using Google as fallback." (`onSurfaceVariant`, no destructive coloring).
- Save error: snackbar with retry, e.g. "Couldn't validate DeepL key. [Retry]"
- Save success: snackbar "DeepL credentials saved."

### 3.6 Install method — `TweaksInstallScreen`

**Title**: "Install method"

**Platform visibility**: hub row shows on both platforms; on desktop the row carries a "Desktop only" → actually "Android only" badge subtitle (review M8). Clicking on desktop routes to a centered empty state (see below). All other content is Android-only.

**Layout (Android)**:

1. Top intro card (outlined `Radii.row`):
   - Title "How the app installs APKs".
   - Body "Choose between the system installer or a silent installer that doesn't ask each time."
2. **Installer picker card** (outlined `Radii.row`):
   - Sub-header: "Method".
   - 4 vertical radio rows (System / Shizuku / Dhizuku / Root), each with icon tile, title, description, status badge (Ready / Needs permission / Not installed / Not running). Selected state: 2.dp `colorScheme.primary` border on the row + left-edge 4.dp wide primary-tinted bar.
   - Title rename per review N7: "System installer" (no "(Default)" suffix).
   - When selected installer needs permission, "Grant permission" button: `WonkySquircleShape.CtaPrimary`.
3. **Auto-update card** (outlined `Radii.row`) — only when silent install ready:
   - Toggle: "Install updates in the background", subtitle "Apply downloaded updates without notifying you each time."
4. **Attribution card** (outlined `Radii.row`) — only when silent install ready:
   - Sub-header: "Pretend the installer is…".
   - Body + radio rows + custom field — unchanged from V1.

**Desktop empty state**:

- Centered `Icons.Outlined.Computer` (48dp) + title "Install method is Android-only" + body "Silent installers and installer attribution are Android features. Desktop installs go through the OS package manager." + back CTA `WonkySquircleShape.CtaAlt` "Back to Tweaks".

**Empty / loading / error**:
- Loading installer states: each radio row's status badge defaults to "Checking…" until `InstallerStatusProvider` returns. Badge has `liveRegion = Polite`.
- Permission grant errors: snackbar.
- Attribution custom field error: inline `supportingText`.

### 3.7 Update behavior — `TweaksUpdatesScreen`

**Title**: "Update behavior"

**Layout**:

1. Battery optimization banner (Android only, when `state.showBatteryOptimizationCard`): outlined `Radii.row` `tertiaryContainer` tinted, Open / Dismiss buttons.
2. **Check for updates card** (outlined `Radii.row`):
   - Sub-header: "Checking".
   - Toggle: "Check automatically", subtitle "Look for new releases on a schedule."
   - When enabled, reveal interval segment. **4 pills** (review N3): "Every 6 hours" / "Every 12 hours" / "Daily" / "Manual only". Drops the 3h option (GitHub rate limits). Disabled-state styling when toggle off.
3. **Pre-releases toggle row**: "Include pre-releases", subtitle "Show alpha and beta tags as available updates."
4. Sub-header "Manage":
   - Drill: "Skipped updates" → `SkippedUpdatesScreen`. Subtitle: plural-aware "%d app" / "%d apps" or "Nothing skipped" when 0.
   - Drill: "Hidden repositories" → `HiddenRepositoriesScreen`. Subtitle: plural-aware count or "No hidden repos."

**Desktop**: auto-update card simplifies to a single toggle "Check on launch" (no interval picker, no WorkManager). Skipped + Hidden drill rows still apply. What's-new history moves to **App info** (V1 had it here; V2 moves it because it's reference content about the app, not update behavior).

**Empty / loading / error**:
- "Manual only" interval = no scheduled check; subtitle on hub reads "Manual checks only."
- Last-check error: hub subtitle pill "Check failed" (`tertiaryContainer`), in-screen banner "Last check failed at 14:02 — [Retry]."
- Plural-aware count subtitles use `pluralStringResource`.

### 3.8 Storage — `TweaksStorageScreen` (new, split from V1's Library cleanup)

**Title**: "Storage"

**Scope**: downloaded APK cache only. Everything else (clipboard, hide-seen, viewed history, telemetry) moves to §3.9 Privacy per review M1.

**Layout**:

1. **Downloaded packages card** (outlined `Radii.row`):
   - Icon tile: `Icons.Outlined.Inventory2`.
   - Title: "Downloaded APKs".
   - Body: "We keep installers around so updates resume fast. Clear them anytime."
   - Status line in `Geist Mono labelMedium`: "Using: 124 MB" / "Using: 0 B" when empty.
   - Trailing primary action: `WonkySquircleShape.CtaAlt` `FilledTonalButton` "Clear", `errorContainer` tinted. **Disabled when size = 0 B**. Opens `ClearDownloadsDialog` (existing).

**Empty / loading / error**:
- Size = 0 B: status reads "Using: 0 B"; Clear button disabled with `disabledContainerColor` per Material 3 disabled-tonal pattern. No empty-state card needed — "0 B" is itself the empty state.
- Loading size: status reads "Calculating…" with subtle indeterminate text shimmer (existing `TweaksViewModel.OnRefreshCacheSize` initialization period).
- Clear success: snackbar "Cleared 124 MB."
- Clear error: snackbar with retry.

### 3.9 Privacy — `TweaksPrivacyScreen` (new — review C1 + M1)

**Title**: "Privacy"

**Scope** (per product-owner decisions): telemetry opt-out, clipboard detection, hide-seen, viewed-history clear.

This screen is **not** the home of the *legal* "Privacy policy" link — that lives in §3.11 App info per placement Option E. The naming overlap is intentional: this sub-screen surfaces in-app behavior toggles; App info surfaces the legal document.

**Layout**:

1. **Telemetry card** (outlined `Radii.row`, inner Column):
   - Sub-header: "Usage data".
   - Toggle row: title "Share anonymous usage data", subtitle "Help us understand which features get used."
   - Below the toggle, an **expandable "What we collect"** row (chevron icon, `Icons.Default.ExpandMore` rotates 180°). Expanded body is bulleted plain text — app version, OS + platform, feature counts (no repo names, no tokens, no identifiers). Compose `AnimatedVisibility`.
   - When toggled off→on, snackbar "Sharing usage data starting next launch." When toggled on→off, snackbar "Usage data sharing stopped. Existing data is dropped." Push `RestartReason.TELEMETRY_TOGGLE` into `needsRestartReasons` so the banner appears (telemetry init runs at app start).
2. **Clipboard card** (outlined `Radii.row`):
   - Toggle: title "Detect repo links in clipboard", subtitle "When you copy a github.com or codeberg.org link, we'll prompt to open it."
3. **Browsing history card** (outlined `Radii.row`, inner Column):
   - Sub-header: "Browsing history".
   - Toggle: title "Hide repos I've already viewed", subtitle "Skip seen repos in feeds and search."
   - Drill row (destructive): title "Clear viewed history". Click → `GhsConfirmDialog` ("Clear all viewed history? This won't unstar or unfavorite anything."). On confirm, snackbar with **Undo** for 5s window, then commits irreversibly. (Treats this as reversible-with-Undo rather than purely irreversible — viewed history is non-critical data.)

**Empty / loading / error**:
- Telemetry expandable: static content; never loading.
- Clipboard / hide-seen toggles: synchronous state; never loading.
- Clear viewed history error: snackbar with retry.

**Hub subtitle compact state** (for §1.1 row 9): `pluralStringResource`-driven, capped at 32 chars per review M6. Examples:
- "Telemetry off · clipboard on" (28 chars)
- "Telemetry on · 3 toggles on" — drop "clipboard" / "hide-seen" specifics on overflow.

### 3.10 Access tokens — `HostTokensScreen` (existing)

No visual changes to the screen contents. Audit pending — confirm `HostTokensScreen` rows use `Radii.row` outlined Surface + new full-opacity `outline` border (review M4). If they don't, refactor as part of P12.5.

Hub subtitle: plural-aware "%d token" / "%d tokens" (review M6), or "No tokens yet" when empty.

### 3.11 App info — `TweaksAppInfoScreen` (new — replaces V1 §3.10 About)

**Title**: "App info"

**Scope** (per placement Option E): about + licenses + privacy policy link + version metadata. **Does not** include feedback (separate hub row §3.12). **Does not** include in-app privacy toggles (those are §3.9 Privacy).

**Layout**:

1. **App identity card** (outlined `Radii.row`, single hero-style card):
   - Top-left: 56.dp app icon (load actual app icon, fallback `Icons.Outlined.Store`).
   - App name "GitHub Store" (`titleLarge`).
   - Version `versionName` in `Geist Mono labelLarge`. Long-press copies to clipboard with snackbar "Version copied."
   - Tagline: "Cross-platform app store for GitHub, Codeberg, and Forgejo releases." (xliff-style placeholders for the three product names in translator handoff per review N4.)
2. **Action rows** (each outlined `Radii.row` drill-in row):
   - **What's new** — icon `Icons.Outlined.NewReleases`, subtitle "Past release notes." Opens `WhatsNewHistoryScreen`.
   - **Open source licenses** — icon `Icons.Outlined.Code`, subtitle "Libraries used in the app." Opens new `LicensesScreen` fed by `gradle-license-plugin` JSON (ship as part of P12.5, not a placeholder per review N8). If `gradle-license-plugin` isn't configured yet, add it in this phase.
   - **Privacy policy** — icon `Icons.Outlined.Description` (or `PrivacyTip` if the §3.9 row uses `Shield`), subtitle "View on github-store.org." Opens `github-store.org/privacy` in external browser via `LocalUriHandler`. Distinct from §3.9 Privacy by icon + subtitle copy.
   - **Source code on GitHub** — icon GitHub glyph (reuse `PlatformGlyph` GitHub variant; fallback `Icons.Outlined.Code`), subtitle "View this app's source." Opens the repo URL.

**Empty / loading / error**:
- Version loading: placeholder "—" until populated.
- Licenses JSON unreachable (shouldn't happen since shipped as asset): empty-state card "Couldn't load licenses. [Retry]."
- External URL handlers can fail on desktop without a default browser: snackbar "Couldn't open privacy policy. URL copied to clipboard." with the URL pre-copied as a fallback.

**Distinction from §3.9 Privacy**: §3.9 = in-app behavior toggles ("what the app does with your data"). §3.11 = static reference content ("what the app is, what its docs say"). They share neither title nor icon set. The hub orders them in different blocks (Privacy & data vs App) so users encounter them with different intent.

### 3.12 Send feedback — hub row, not a sub-screen

Per product-owner decision + placement Option E. **Single feedback path** (not duplicated inside App info per researcher's open question).

- Hub row (§1.1 row 12) tapping opens `FeedbackBottomSheet` directly. No intermediate screen.
- `FeedbackBottomSheet` uses `GhsBottomSheet` with `WonkySquircleShape.Sheet`.
- Behavior unchanged from today — pre-fills user-agent + version metadata.

**Empty / loading / error**:
- Submit success: sheet dismisses, hub snackbar "Feedback sent. Thanks."
- Submit error: inline error in sheet, retry button.

### 3.13 (reserved — was §3.13 Privacy in product-owner brief, merged into §3.9 above)

### 3.14 (reserved — was §3.14 Storage in product-owner brief, merged into §3.8 above)

---

## 4. Proxy redesign (deep dive)

### 4.1 Pattern — master + per-scope overrides (Option B, persisted)

Master proxy applies to all 3 scopes by default. Each scope has a `[ Use main ] [ Custom… ]` segment (review M2). "Custom…" reveals a mini-editor; "Use main" hides it.

Persistence change (review C2): the master config is its **own** `ProxyConfig?` record in `ProxyRepository`, plus a `useMaster: Boolean` per scope. The V1 promise to derive master from per-scope equality is reversed — equality-derivation is racy across DataStore writes and the user can't see "which scope am I testing." Schema migration in §4.5.

### 4.2 Why master + overrides

1. ~95% of users want one proxy everywhere. V1's 3-card design forces them to fill the same form 3 times.
2. Per-scope overrides (Tor SOCKS5 for Translation, corporate HTTP for Downloads) are real power-user scenarios.
3. Persisted master = glance-readable hub subtitle: "No proxy" / "HTTP 127.0.0.1:1080" / "HTTP 127.0.0.1:1080 · 1 override".
4. Persisted master = unambiguous "which scope am I testing" — test buttons name the scope.

### 4.3 The Connection screen, step by step

**Title**: "Connection"

**Top intro card** (outlined `Radii.row`):
- Title "How the app reaches the internet".
- Body "Pick a connection mode below. Most people leave this on No proxy."

**Card 1 — Main connection** (outlined `Radii.row`, inner Column):

1. Sub-header: "Main connection".
2. **Mode segment** — 4-way `ModePillSegment`: **No proxy** / **System** / **HTTP/HTTPS** / **SOCKS5** (review M3 + C3).
   - "No proxy" / "System" → form collapses (no fields to edit).
   - "HTTP/HTTPS" / "SOCKS5" → form expands `AnimatedVisibility`.
   - Small caption under HTTP/HTTPS pill: "Most corporate proxies." Small caption under SOCKS5: "Tor, SSH tunnels." (review M3)
3. **Form fields** (when expanded):
   - Row: Host (weight 2) + Port (weight 1) `OutlinedTextField`s, shape `Radii.chip`, with existing `isLikelyValidProxyHost` validation.
   - Username (optional).
   - Password (optional) with eye toggle.
   - Inline helper: "Applies to all traffic unless overridden below." (review N10 — compressed.)
4. **Paste full URL** affordance (review M3) — text-button below the form fields, `Icons.Outlined.ContentPaste` leading icon, label "Paste full URL". Opens a modal sheet (`GhsBottomSheet`, `WonkySquircleShape.Sheet`) with a single paste field. Parser accepts `scheme://user:pass@host:port` (`scheme ∈ {http, https, socks5}`). On parse success: form fields populate, sheet dismisses, snackbar "Pasted from URL." On parse fail: inline error "Couldn't read that URL."
5. **Test & save row** (HTTP/HTTPS / SOCKS5 only):
   - **Test** — `OutlinedButton`, `Radii.chip`, leading `Icons.Default.NetworkCheck`. Label: **"Test main connection"** (review C2 — announce scope). On click: tests against 3 endpoints (search API, download CDN, configured translation provider). Results snackbar is 3-line: "Search ✓ 184 ms · Downloads ✓ 92 ms · Translation ✓ 220 ms" or per-endpoint failures.
   - **Save** — `FilledTonalButton`, `WonkySquircleShape.CtaPrimary`, leading `Icons.Default.Save`. Disabled until form valid. Writes to the persisted **master** record.

Test request honors `HostTokenInterceptor` per review N6 — for private GH Enterprise + PAT users, the test uses the user's PAT.

**Card 2 — Per-scope overrides** (outlined `Radii.row`, inner Column):

1. Sub-header: "Per-scope overrides".
2. Body: "Each scope uses the main connection by default. Choose 'Custom' to give a scope its own settings."
3. Three sub-rows, one per scope:
   - **Discovery** — title "Search & metadata", subtitle "GitHub API, search results, repo details."
   - **Downloads** — title "Downloads", subtitle "APK and asset downloads."
   - **Translation** — title "Translation", subtitle "DeepL, Microsoft, LibreTranslate calls."
   - Each row has a trailing **2-segment chooser** `[ Use main ] [ Custom… ]` (review M2).
   - "Custom…" expands `AnimatedVisibility` into a mini-editor matching Card 1 structure (mode segment + form + test + save). Test button label: **"Test for Downloads"** (review C2). Save here writes only to that scope.
   - Closed (Use main selected) sub-rows show a compact status: green pill "Using main" or — if "Custom…" persists a non-default config but the user just toggled back to "Use main" — the saved override config is preserved in state but not applied. Toggle back to "Custom…" to restore.
4. Live state pill on every override sub-row (review C2 #4):
   - "Use main" → small green-ish pill "Using main" (`tertiaryContainer`, `labelSmall`).
   - "Custom…" with config → amber-ish pill "HTTP 127.0.0.1:1080" (`tertiaryContainer`, `labelSmall`, mono font).

**Empty / loading / error states**:

- **Empty**: never — "No proxy" mode is always available.
- **Loading test**: Test button shows inline `CircularProgressIndicator` (16.dp). Disable both Test and Save while testing.
- **Test success**: snackbar with 3-line breakdown (master) or 1-line scope-specific (override).
- **Test failure (one endpoint)**: snackbar "Search ✓ · Downloads ✗ connection refused · Translation ✓". Don't fail the whole test — show partial.
- **Test failure (all endpoints)**: snackbar with retry button.
- **Save success**: snackbar "Main connection saved." or "Downloads connection saved."
- **Save error**: snackbar with retry.
- **Mode switch with unsaved form draft**: switching the master mode from HTTP/HTTPS to SOCKS5 (or vice versa) with dirty form: confirmation snackbar "Switch mode? Unsaved settings will be cleared. [Switch] [Cancel]." (covers review C4 scope-changing pattern.)
- **Override on→off toggle ("Custom…" → "Use main")**: snackbar "Downloads now uses the main connection." Reversible — toggle back within 5s to restore the override config.

**Direct vs No-proxy clarity** (review C3): "No proxy" replaces "Direct" / "None" everywhere. Body copy updated. `proxy_none_description` rewritten to "The app connects to the internet without a proxy." (See §6.)

### 4.4 Validation rules (unchanged from V1)

- Host: existing `isLikelyValidProxyHost(raw)`. Inline error in `supportingText`.
- Port: integer in `1..65535`.
- Username + password: optional.
- Save button disabled until host + port pass.
- Test button enabled even when fields invalid; surfaces friendly snackbar "Enter a host and port first."

### 4.5 Master proxy persistence migration

`ProxyRepository` schema additions:

- New record key: `proxy_master` → `ProxyConfig?` (nullable for "no master configured").
- New per-scope key: `proxy_<scope>_use_master` → `Boolean` (default true).
- Existing per-scope `proxy_<scope>` records remain; semantics now mean "the scope's override config" (only consulted when `proxy_<scope>_use_master == false`).

**One-time migration on first launch of V2**:

1. Read existing 3 per-scope records.
2. If all 3 are equal (same `ProxyConfig`): write that config to `proxy_master`, set all 3 `proxy_<scope>_use_master = true`.
3. If any diverge: write the **most common** config to `proxy_master` (ties broken by scope order: Discovery > Downloads > Translation). For each scope whose config differs from the chosen master, set `proxy_<scope>_use_master = false` (existing record stays as the override).
4. Bump DataStore version key from N → N+1 to gate the migration (`tweaksDataStore.version`). Run once.

This is a pure-presentation migration — no network calls, no async beyond DataStore reads. Failures roll back (don't bump version key) so retry on next launch is safe.

`TweaksAction` additions:

- `OnMasterProxySave(config: ProxyConfig)` — writes master + all scopes' `useMaster = true`.
- `OnScopeUseMainToggled(scope: ProxyScope, useMain: Boolean)` — toggles override; preserves the scope's override config in state when toggling to main.
- `OnScopeProxySave(scope: ProxyScope, config: ProxyConfig)` — writes scope override + sets `useMaster = false`.
- Existing `OnProxyTest(scope: ProxyScope?)` — `null` scope = master (tests 3 endpoints).

---

## 5. Visual primitive proposals

### 5.1 New primitives

**`TweaksEntryRow`** — described in §2.2. Lives at `feature/tweaks/presentation/components/TweaksEntryRow.kt`.

**`ModePillSegment`** — promote existing `ModeSegment` from `sections/Others.kt` (private) into `core/presentation/components/ModePillSegment.kt`. Generic over a value type:

```
data class ModePillItem<T>(val value: T, val label: String, val icon: ImageVector? = null, val caption: String? = null)
@Composable fun <T> ModePillSegment(items: List<ModePillItem<T>>, selected: T, onSelect: (T) -> Unit, modifier: Modifier = Modifier)
```

Used by:
- Appearance theme mode (Light / Dark / Follow system).
- Connection master mode (No proxy / System / HTTP/HTTPS / SOCKS5) — with `caption` for the bottom-of-pill hint.
- Content width segment (Compact / Wide / Extra wide).

**`UseMainSegment`** — 2-segment `[ Use main ] [ Custom… ]`. Lives at `feature/tweaks/presentation/components/UseMainSegment.kt`. Two `ToggleButton`-style chips, `Radii.chip` shape, primary fill on selected. (Could be implemented as a thin wrapper around `ModePillSegment<Boolean>` — implementer's call.)

**`RestartBanner`** — `feature/tweaks/presentation/components/RestartBanner.kt`. Outlined `Radii.row` `tertiaryContainer`-tinted Surface. Props: `reasons: Set<RestartReason>`, `onRestartNow: () -> Unit`, `onLater: () -> Unit`.

**`CustomForgesSheet`** — `GhsBottomSheet` instance, no new shape primitive. New composable at `feature/tweaks/presentation/components/CustomForgesSheet.kt`. Replaces `CustomForgesDialog.kt`.

**`PasteProxyUrlSheet`** — `GhsBottomSheet`. Single paste field + parser. Lives at `feature/tweaks/presentation/components/PasteProxyUrlSheet.kt`.

**`LicensesScreen`** — `feature/tweaks/presentation/LicensesScreen.kt`. Static markdown view fed by `gradle-license-plugin` JSON committed to assets. Outlined `Radii.row` row per library, expandable for full license text.

### 5.2 No new shapes

All squircles reuse `Radii.row`, `Radii.chip`, `WonkySquircleShape.{CtaPrimary, CtaAlt, Search, Sheet, Dialog, Toast}`.

### 5.3 Components to retire

- `ExpressiveCard` usages inside Tweaks sections (`Others.kt`, `Installation.kt`) — replace with outlined `Radii.row` Surface.
- `ElevatedCard(shape = RoundedCornerShape(32.dp))` in `Translation.kt`, `Language.kt`, `About.kt`, `Network.kt` — same replacement.
- `RoundedCornerShape(12.dp)` `OutlinedTextField` inside forms — switch to `Radii.chip`.
- 3× `ProxyScopeCard` — deleted, replaced by §4 design.
- `CustomForgesDialog.kt` — deleted, replaced by `CustomForgesSheet`.

---

## 6. String rename table

Resource keys + English values. Across-13-locale translation queued via §9 CSV; English ships first per project policy.

| Resource key | Current text | Proposed text |
|---|---|---|
| `Res.string.tweaks_title` | "Tweaks" | "Tweaks" (keep) |
| `Res.string.section_appearance` | "Appearance" | "Appearance" (keep) |
| `Res.string.theme_color` | "Theme color" | "Palette" |
| `Res.string.theme_light` | "Light" | "Light" (keep) |
| `Res.string.theme_dark` | "Dark" | "Dark" (keep) |
| `Res.string.theme_system` | "System" | "Follow system" |
| `Res.string.amoled_black_theme` | "AMOLED black" | "True black (AMOLED)" |
| `Res.string.amoled_black_description` | "Use pure black for OLED screens." | "Pure-black background — saves power on OLED screens." |
| `Res.string.system_font` | "System font" | "Use system font" |
| `Res.string.system_font_description` | "Use the system font for the app." | "Use your device's default typeface instead of Geist." |
| `Res.string.scrollbar_option_title` | "Show scrollbar" | "Show scrollbar" (keep) |
| `Res.string.scrollbar_option_description` | "Show scrollbar on the right side." | "Always show the scrollbar on long pages." |
| `Res.string.content_width_title` | "Content width" | "Content width" (keep) |
| `Res.string.content_width_description` | "Adjust max content width." | "How wide content should stretch on big windows." |
| `Res.string.section_language` | "Language" | "Language" (keep) |
| `Res.string.language_intro` | "Choose your app language." | "Pick the language used across the app." |
| `Res.string.language_picker_title` | "App language" | "App language" (keep) |
| `Res.string.language_picker_description` | "Restart required after change." | "The app restarts when you switch language." |
| `Res.string.language_follow_system` | "Follow system" | "Follow system" (keep) |
| `Res.string.language_follow_system_subtitle` | (new) | "Follow system · %1$s" (resolved tag interpolated) |
| `Res.string.section_network` | "Network" | (retired — block becomes "Connectivity" §1.1) |
| `Res.string.section_connectivity` | (new) | "Connectivity" |
| `Res.string.section_privacy_and_data` | (new) | "Privacy & data" |
| `Res.string.section_app_block` | (new) | "App" |
| `Res.string.section_installs_and_updates` | (new) | "Installs & updates" |
| `Res.string.connection_entry_title` | (new) | "Connection" |
| `Res.string.connection_entry_subtitle_no_proxy` | (new) | "No proxy" |
| `Res.string.connection_entry_subtitle_system` | (new) | "System proxy" |
| `Res.string.connection_entry_subtitle_proxy_with_overrides` | (new) | "%1$s · %2$d override" / "%1$s · %2$d overrides" (plural-aware) |
| `Res.string.sources_entry_title` | (new) | "Sources" |
| `Res.string.proxy_scope_intro` | "Configure proxies per scope." | (retired) |
| `Res.string.proxy_scope_discovery_title` | "Discovery proxy" | "Search & metadata" |
| `Res.string.proxy_scope_download_title` | "Download proxy" | "Downloads" |
| `Res.string.proxy_scope_translation_title` | "Translation proxy" | "Translation" |
| `Res.string.proxy_scope_discovery_description` | "Used for API and search." | "GitHub API, search results, repo details." |
| `Res.string.proxy_scope_download_description` | "Used for APK downloads." | "APK and asset downloads." |
| `Res.string.proxy_scope_translation_description` | "Used for translation providers." | "DeepL, Microsoft, LibreTranslate calls." |
| `Res.string.proxy_none` | "None" | "No proxy" |
| `Res.string.proxy_none_description` | "No proxy used." | "The app connects to the internet without a proxy." |
| `Res.string.proxy_system` | "System" | "System" (keep) |
| `Res.string.proxy_system_description` | "Use system proxy." | "Use the proxy configured in your OS." |
| `Res.string.proxy_http` | "HTTP" | "HTTP/HTTPS" |
| `Res.string.proxy_http_caption` | (new) | "Most corporate proxies." |
| `Res.string.proxy_socks` | "SOCKS" | "SOCKS5" |
| `Res.string.proxy_socks_caption` | (new) | "Tor, SSH tunnels." |
| `Res.string.proxy_test` | "Test" | (retired — replaced by scope-named buttons) |
| `Res.string.proxy_test_main` | (new) | "Test main connection" |
| `Res.string.proxy_test_scope` | (new) | "Test for %1$s" (scope name interpolated) |
| `Res.string.proxy_save` | "Save" | "Save" (keep) |
| `Res.string.proxy_test_success` | "Proxy reachable (%d ms)" | "Connected in %1$d ms" |
| `Res.string.proxy_test_main_success` | (new) | "Search ✓ %1$d ms · Downloads ✓ %2$d ms · Translation ✓ %3$d ms" |
| `Res.string.proxy_use_main` | (new) | "Use main" |
| `Res.string.proxy_use_custom` | (new) | "Custom…" |
| `Res.string.proxy_using_main_pill` | (new) | "Using main" |
| `Res.string.proxy_paste_full_url` | (new) | "Paste full URL" |
| `Res.string.proxy_paste_url_placeholder` | (new) | "scheme://user:pass@host:port" |
| `Res.string.proxy_paste_url_error` | (new) | "Couldn't read that URL." |
| `Res.string.connection_intro_title` | (new) | "How the app reaches the internet" |
| `Res.string.connection_intro_body` | (new) | "Pick a connection mode below. Most people leave this on No proxy." |
| `Res.string.connection_inline_helper` | (new) | "Applies to all traffic unless overridden below." |
| `Res.string.connection_overrides_body` | (new) | "Each scope uses the main connection by default. Choose 'Custom' to give a scope its own settings." |
| `Res.string.mirror_tweaks_entry_label` | "Mirror" | "GitHub mirror" |
| `Res.string.custom_forges_entry_label` | "Custom forges" | "Custom forges" (keep) |
| `Res.string.custom_forges_entry_subtitle` | "Add your own Forgejo/Gitea hosts." | "Add a Forgejo or Gitea host" |
| `Res.plurals.custom_forges_count` | (new) | "%d host" / "%d hosts" (plural-aware) |
| `Res.string.section_translation` | "Translation" | "Translation" (keep) |
| `Res.string.translation_intro` | "Configure translation provider and auto-translate." | "Pick the engine the app uses to translate READMEs." |
| `Res.string.translation_provider_title` | "Provider" | "Provider" (keep) |
| `Res.string.translation_provider_description` | "Pick a translation engine." | (retired) |
| `Res.string.translation_auto_title` | "Auto-translate" | "Translate READMEs automatically" |
| `Res.string.translation_auto_subtitle` | "Translate READMEs when opening repos." | "When opening a repo, translate the README into your target language." |
| `Res.string.section_installation` | "INSTALLATION" | "Install method" |
| `Res.string.install_method_android_only_badge` | (new) | "Android only" |
| `Res.string.install_method_desktop_empty_title` | (new) | "Install method is Android-only" |
| `Res.string.install_method_desktop_empty_body` | (new) | "Silent installers and installer attribution are Android features. Desktop installs go through the OS package manager." |
| `Res.string.installer_type_default` | "Default" | "System installer" |
| `Res.string.installer_type_default_description` | "Uses Android's default installer." | "Asks each time. Works on every device." |
| `Res.string.installer_type_shizuku` | "Shizuku" | "Shizuku" (keep) |
| `Res.string.installer_type_shizuku_description` | "Silent install via Shizuku." | "Silent install. Needs Shizuku app running." |
| `Res.string.installer_type_dhizuku` | "Dhizuku" | "Dhizuku" (keep) |
| `Res.string.installer_type_dhizuku_description` | "Silent install via Dhizuku." | "Silent install. No root needed." |
| `Res.string.installer_type_root` | "Root" | "Root" (keep) |
| `Res.string.installer_type_root_description` | "Silent install with root." | "Silent install via root. Power-user only." |
| `Res.string.installer_attribution_title` | "Installer attribution" | "Pretend the installer is…" |
| `Res.string.installer_attribution_description` | "Some apps reject silent installs unless installer claims to be Google Play." | "Some apps reject silent installs unless the installer claims to be Google Play. This setting controls what we claim." |
| `Res.string.section_updates` | "UPDATES" | "Update behavior" |
| `Res.string.auto_update_title` | "Auto-update" | "Install updates in the background" |
| `Res.string.auto_update_description` | "Apply downloaded updates automatically." | "Apply downloaded updates without notifying you each time." |
| `Res.string.update_check_enabled_title` | "Background update check" | "Check automatically" |
| `Res.string.update_check_enabled_description` | "Periodically check for new releases." | "Look for new releases on a schedule." |
| `Res.string.update_check_interval_title` | "Check interval" | "Check every" |
| `Res.string.update_check_interval_description` | "How often to check for updates." | (retired) |
| `Res.string.update_interval_6h` | (new / rename) | "Every 6 hours" |
| `Res.string.update_interval_12h` | (new / rename) | "Every 12 hours" |
| `Res.string.update_interval_24h` | (new / rename) | "Daily" |
| `Res.string.update_interval_manual` | (new) | "Manual only" |
| `Res.string.update_desktop_check_on_launch_title` | (new) | "Check on launch" |
| `Res.string.update_desktop_check_on_launch_subtitle` | (new) | "Check for updates each time the app starts." |
| `Res.string.include_pre_releases_title` | "Include pre-releases" | "Include pre-releases" (keep) |
| `Res.string.include_pre_releases_description` | "Treat alpha/beta as available updates." | "Show alpha and beta tags as available updates." |
| `Res.string.skipped_updates_entry_title` | "Skipped updates" | "Skipped updates" (keep) |
| `Res.plurals.skipped_updates_count` | (new) | "%d app" / "%d apps" (plural-aware) |
| `Res.string.skipped_updates_empty_subtitle` | (new) | "Nothing skipped" |
| `Res.string.hidden_repositories_title` | "Hidden repositories" | "Hidden repositories" (keep) |
| `Res.plurals.hidden_repositories_count` | (new) | "%d repo" / "%d repos" (plural-aware) |
| `Res.string.hidden_repositories_empty_subtitle` | (new) | "No hidden repos" |
| `Res.string.storage` | "Storage" | "Storage" (keep — now narrower scope) |
| `Res.string.downloaded_packages` | "Downloaded packages" | "Downloaded APKs" |
| `Res.string.downloaded_packages_description` | "Installer files kept for resumed updates." | "We keep installers around so updates resume fast." |
| `Res.string.current_size` | "Current size:" | "Using:" |
| `Res.string.section_privacy_screen_title` | (new) | "Privacy" |
| `Res.string.privacy_entry_subtitle` | (new) | "Compact state (see §3.9 hub subtitle rules)" |
| `Res.string.privacy_usage_data_subheader` | (new) | "Usage data" |
| `Res.string.privacy_telemetry_title` | (new) | "Share anonymous usage data" |
| `Res.string.privacy_telemetry_subtitle` | (new) | "Help us understand which features get used." |
| `Res.string.privacy_telemetry_collect_expand` | (new) | "What we collect" |
| `Res.string.privacy_telemetry_collect_body` | (new) | "App version. OS and platform. Feature usage counts. No repo names. No tokens. No identifiers." |
| `Res.string.privacy_telemetry_on_snackbar` | (new) | "Sharing usage data starting next launch." |
| `Res.string.privacy_telemetry_off_snackbar` | (new) | "Usage data sharing stopped. Existing data is dropped." |
| `Res.string.auto_detect_clipboard_links` | "Auto-detect clipboard links" | "Detect repo links in clipboard" |
| `Res.string.auto_detect_clipboard_description` | "Detect copied repo URLs and offer to open them." | "When you copy a github.com or codeberg.org link, we'll prompt to open it." |
| `Res.string.privacy_history_subheader` | (new) | "Browsing history" |
| `Res.string.hide_seen_title` | "Hide seen" | "Hide repos I've already viewed" |
| `Res.string.hide_seen_description` | "Skip already-viewed repos in feeds." | "Skip seen repos in feeds and search." |
| `Res.string.clear_seen_history` | "Clear seen history" | "Clear viewed history" |
| `Res.string.clear_seen_history_description` | "Reset the seen-repo list." | "Forget which repos you've already opened." |
| `Res.string.clear_seen_history_confirm` | (new) | "Clear all viewed history? This won't unstar or unfavorite anything." |
| `Res.plurals.host_tokens_count` | (new) | "%d token" / "%d tokens" (plural-aware) |
| `Res.string.host_tokens_empty_subtitle` | (new) | "No tokens yet" |
| `Res.string.section_about` | "About" | "App info" (sub-screen title) |
| `Res.string.app_info_tagline` | (new) | "Cross-platform app store for GitHub, Codeberg, and Forgejo releases." |
| `Res.string.app_info_action_whats_new` | (new) | "What's new" |
| `Res.string.app_info_action_whats_new_subtitle` | (new) | "Past release notes." |
| `Res.string.app_info_action_licenses` | (new) | "Open source licenses" |
| `Res.string.app_info_action_licenses_subtitle` | (new) | "Libraries used in the app." |
| `Res.string.app_info_action_privacy_policy` | (new) | "Privacy policy" |
| `Res.string.app_info_action_privacy_policy_subtitle` | (new) | "View on github-store.org." |
| `Res.string.app_info_action_source_code` | (new) | "Source code on GitHub" |
| `Res.string.app_info_action_source_code_subtitle` | (new) | "View this app's source." |
| `Res.string.app_info_version_copied` | (new) | "Version copied." |
| `Res.string.version` | "Version" | "Version" (keep) |
| `Res.string.feedback_send` | "Send feedback" | "Send feedback" (keep) |
| `Res.string.feedback_hub_subtitle` | (new) | "We read every report." |
| `Res.string.tweaks_search_placeholder` | (new) | "Search settings" |
| `Res.string.tweaks_search_empty` | (new) | "No settings match '%1$s'." |
| `Res.string.restart_banner_body` | (new) | "Some changes need a restart to apply." |
| `Res.string.restart_banner_reasons_prefix` | (new) | "Affected: %1$s" (comma-joined reasons) |
| `Res.string.restart_banner_reason_language` | (new) | "language" |
| `Res.string.restart_banner_reason_theme` | (new) | "theme" |
| `Res.string.restart_banner_reason_telemetry` | (new) | "usage data" |
| `Res.string.restart_banner_restart_now` | (new) | "Restart now" |
| `Res.string.restart_banner_later` | (new) | "Later" |
| `Res.string.menubar_help_menu` | (new, desktop) | "Help" |
| `Res.string.menubar_help_about` | (new, desktop) | "About GitHub Store" |
| `Res.string.menubar_help_feedback` | (new, desktop) | "Send feedback…" |
| `Res.string.menubar_help_licenses` | (new, desktop) | "Open source licenses" |
| `Res.string.menubar_help_privacy` | (new, desktop) | "Privacy policy" |

i18n constraints (review M6):

- Every counted label uses `pluralStringResource` (Compose Multiplatform Resources, `org.jetbrains.compose.resources`). Plural categories per locale follow CLDR (Russian 3 forms, Polish 3, Arabic 6, English 2).
- Subtitle ~32 chars; if subtitle has multiple dot-separated tokens, drop rightmost on overflow.
- Locale-aware separator (`·` for Latin-script locales, ` / ` for CJK in narrow contexts) — pragmatic compromise: use `·` everywhere in v1 since the existing `Squiggle`-adjacent text already does, queue CJK separator audit for P13.
- Translation handoff CSV: §9.

---

## 7. Risks / open questions / non-goals

### 7.1 Migration concerns

- **Deep links** — `githubstore://tweaks/<category>` deferred to follow-up ticket P12.5.1 (review C5 partially adopted: inline search ships, deep links queued). Route name list in §2.5.
- **Screenshots in docs** — README + Play Store + Homebrew tap screenshots that show today's Tweaks layout will be stale. Action: list and regen after merge.
- **Discoverability loss** — mitigated by (a) dynamic hub subtitles, (b) inline search (§2.5), (c) badged cross-platform rows (review M8).
- **ProxyRepository schema bump** — see §4.5. Migration tested via "all 3 equal," "2 equal + 1 different," "all 3 different" + clean install. Implementer must add a unit test for the migration's plurality-vote rule.

### 7.2 Implementation risks

- **`HostTokensScreen` row vocabulary audit** — confirm `Radii.row` + full-opacity `outline` border. Refactor in P12.5 if not.
- **`MirrorPickerScreen` row vocabulary audit** — same.
- **`FeedbackBottomSheet`** — confirm `WonkySquircleShape.Sheet`.
- **String resource churn** — touches ~80 keys (V1 had ~50; V2 added telemetry, restart banner, paste URL, menubar, search, plurals). Across 13 locales = ~1040 string updates. Ship strategy per project policy: English first, translator handoff CSV (§9) queued.
- **Action sealed-interface drift** — `TweaksAction` will balloon. Recommendation (not blocking v1): split into `TweaksHubAction`, `TweaksConnectionAction`, `TweaksPrivacyAction`, etc., each handled by either the same VM with grouped `when` or per-sub-screen sub-VMs. Refactor pairs naturally with this redesign.
- **`gradle-license-plugin`** — verify it's configured; if not, add in this phase. Output JSON goes to `composeApp/src/commonMain/composeResources/files/licenses.json`. Parsed at runtime by `LicensesScreen`.
- **Border contrast audit gate** — before merge, screenshot all 4 palettes × 3 modes (Light/Dark/AMOLED) and visually confirm `TweaksEntryRow` border passes 3:1 contrast on Cream light. If `outline` reads too heavy on dark themes, fall back to `outline.copy(alpha = 0.55f)` but only after measured contrast > 3:1 on Cream light.

### 7.3 v1 non-goals (explicit)

- **PAC files / proxy auto-config URL.** Documented non-goal per review M3. May add as a 5th mode pill in P13 if user demand surfaces. Today's Mode segment is **No proxy / System / HTTP/HTTPS / SOCKS5**, 4 pills.
- **SOCKS4 / SOCKS4a.** `ProxyConfig.Socks` is SOCKS5 only. Mode pill labeled "SOCKS5" to set expectation.
- **Multiple proxy presets / proxy profiles.** Out of scope.
- **In-screen telemetry data export.** Out of scope. Privacy expandable only describes; doesn't export.
- **Cross-screen settings search (system-level).** v1 search is hub-only (§2.5). Filtering through every sub-screen's controls is P13+.

### 7.4 UX research follow-ups (queued, not blocking v1)

1. Hub feels sparse on tall display — accept; revisit if heatmap shows abandoned scrolls.
2. "Connection" vs "Network" naming — locked to "Connection" + "Sources" per IA.
3. Provider radios vs chips — locked to radios per §3.5.
4. Install method "System installer" naming — locked.
5. About card hero vs 3 rows — locked to single hero card.
6. "Use main" vs "Inherit" wording — locked to "Use main" / "Custom…" segment per review M2.

### 7.5 Open items the architect can't resolve alone

- **Telemetry backend.** Spec defines the UI for opt-out. Whether `TelemetryRepository` already has a backend wired (per `feature/tweaks/CLAUDE.md` it's injected but UI is absent today) needs product confirmation. If backend isn't ready, the toggle persists locally + no-ops until backend ships. Worst case, it's a UX-correct placebo for one release; review C1 still satisfied because the user-facing control exists.
- **Crash reporter desktop opt-out.** `CrashReporter` (desktop) writes local logs; spec doesn't add a UI toggle for it because it's local-only (no network exfil). If a future release adds remote crash upload, gate it on the same telemetry toggle.

### 7.6 Things deliberately not changed

- The `AppLanguages.ALL` list and tag-based persistence.
- The `ProxyConfig` sealed class (only `ProxyRepository` schema bumps — see §4.5).
- The `TweaksRepository` persisted prefs keys for non-renamed settings (`RestartReason` + `needsRestartReasons` is **new**).
- The `MirrorPickerScreen`, `SkippedUpdatesScreen`, `HiddenRepositoriesScreen`, `HostTokensScreen`, `WhatsNewHistoryScreen` routes and nav wiring.
- The `FeedbackBottomSheet` flow.
- Cross-cutting tokens (`Tokens.kt`, `Radii.kt`, `WonkySquircleShape.kt`).

---

## 8. Desktop MenuBar spec (new — placement Option E)

Today's `composeApp/src/jvmMain/kotlin/zed/rainxch/githubstore/DesktopApp.kt` `Window { … }` has no `MenuBar` content. This is **additive** — no current menu is being overridden.

**Spec**:

```
Window(...) {
    MenuBar {
        Menu("Help") {
            Item("About GitHub Store", onClick = { /* navigate to App info */ })
            Item("Send feedback…", onClick = { /* open FeedbackBottomSheet */ })
            Item("Open source licenses", onClick = { /* navigate to App info → Licenses */ })
            Item("Privacy policy", onClick = { /* open github-store.org/privacy */ })
        }
    }
    // ... existing window content
}
```

**macOS specifics**:

- The JVM injects a default "About GitHub-Store" item under the macOS app menu. Override the no-op default via `Desktop.getDesktop().setAboutHandler { /* navigate to App info */ }` in `DesktopApp.main`. The Apple-menu About item then routes to our in-app App info screen.
- The Help menu still renders on macOS (per JVM `MenuBar` semantics), giving feedback + licenses + privacy a discoverable home.

**Windows/Linux specifics**:

- `MenuBar` renders in the title-bar area. Help menu is the entry point. No system About menu to override.

**i18n**: menu strings are added to `strings.xml` per §6 (`menubar_help_menu`, `menubar_help_about`, etc.). At runtime, the JVM `Menu` and `Item` labels are resolved via the same string catalog used by Compose UI.

**Why this matters**: every long-tail desktop app — VS Code, Slack, Discord, 1Password — exposes About via the native menu bar. Today our Compose Multiplatform `Window` has no menu, so desktop users hunt for About in the in-app UI. Wiring this is ~25 lines + zero changes to mobile.

---

## 9. Translator handoff CSV

Generated as **`/Users/rainxchzed/Documents/development/kmp/GitHub-Store/.design/P12_5_TWEAKS_STRINGS.csv`** alongside this spec. Three-column shape: `Resource key | Old English | New English`. New keys list as old="(new)".

CSV is the source of truth for the translator queue. English ships first per project policy; translations land in subsequent commits as translators turn each locale around.

---

## 10. Implementation order suggestion

Suggested wave-based parallelization for `gsd-execute-phase`:

- **Wave 1** — Foundation
  - Schema migration scaffolding (`ProxyRepository` master + per-scope `useMaster`, §4.5).
  - `RestartReason` enum + `needsRestartReasons` in `TweaksRepository`.
  - `RestartBanner` component (§5.1).
  - `TweaksEntryRow` primitive (§2.2).
  - New navigation routes (placeholders for unmade screens).
  - `TweaksScreen` hub composable with restart banner + search field + section blocks (§2).
  - 11 entry rows wired to existing or placeholder destinations.
- **Wave 2** — Quick-win sub-screens (parallel-safe; each just relocates existing controls)
  - `TweaksAppearanceScreen` (§3.1) — easy, mostly relocation.
  - `TweaksLanguageScreen` (§3.2) — searchable list pattern.
  - `TweaksStorageScreen` (§3.8) — narrower than V1's Library cleanup; just downloaded APKs.
  - `TweaksAppInfoScreen` (§3.11) — meta links + version card.
  - `LicensesScreen` (new, §3.11 / §5.1) — requires `gradle-license-plugin` config.
- **Wave 3** — Connection redesign (biggest piece)
  - `TweaksConnectionScreen` (§3.3 + §4).
  - `ModePillSegment` promotion (§5.1).
  - `UseMainSegment` (§5.1).
  - `PasteProxyUrlSheet` (§5.1).
  - VM action additions (§4.5).
  - Schema migration unit tests.
- **Wave 4** — Privacy + Updates + Sources + Translation + Install (parallel-safe)
  - `TweaksPrivacyScreen` (§3.9) — telemetry opt-out is highest-priority new control.
  - `TweaksUpdatesScreen` (§3.7) — drops 3h interval.
  - `TweaksSourcesScreen` (§3.4) + `CustomForgesSheet` (§5.1).
  - `TweaksTranslationScreen` (§3.5) — provider radio redesign.
  - `TweaksInstallScreen` (§3.6) + Android-only badge handling for desktop.
- **Wave 5** — Polish & cleanup
  - Send feedback hub row wires to `FeedbackBottomSheet` directly (§3.12).
  - String rename diff (English; CSV handoff for translators §9).
  - Delete old `sections/Appearance.kt`, `sections/Language.kt`, `sections/Network.kt`, `sections/Translation.kt`, `sections/Installation.kt`, `sections/Others.kt`, `sections/About.kt`, `sections/SettingsSection.kt`, `components/CustomForgesDialog.kt`.
  - Border-contrast audit gate (Nord/Cream/Forest/Plum × Light/Dark/AMOLED screenshots).
- **Wave 6** — Desktop additive
  - `MenuBar` in `DesktopApp.kt` (§8).
  - macOS `Desktop.setAboutHandler` wiring.

End of spec.
