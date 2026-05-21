# UX-AUDIT — GitHub Store, design-system refresh

Branch: `feat/design-system-refresh`. Audited against `Downloads/handoff 4/` (DESIGN.md, MIGRATION.md, patterns.md, tokens.json). Code citations are file:line on this branch's working tree. No implementation here; only what is, what changes, and the risks.

---

## 1. Current state inventory

Every primary screen + the chrome that wraps them.

### 1.1 Chrome (composeApp)

| Element | Route / file | Current behavior |
|---|---|---|
| Nav graph | `composeApp/.../app/navigation/GithubStoreGraph.kt` | 22 sealed routes (lines 6–84). `DetailsScreen` carries `sourceHost` for Forgejo/Codeberg (line 32). |
| Bottom navigation | `app/navigation/BottomNavigation.kt:60–264` + `BottomNavigationUtils.kt:20–60` | "Liquid glass" pill rail, 5 entries: Home, Search, Apps, Profile, Tweaks (lines 22–51). Apps is Android-only — filtered out on desktop (lines 54–59). Custom shape: `CircleShape` outer + animated rounded indicator (`BottomNavigation.kt:181, 193`). Two badges supported: AppsScreen (update available), ProfileScreen (unread announcements) — `BottomNavigation.kt:242–244`. |
| Top app bars | Per-screen `TopAppBar` from M3 | No shared chrome layer; each screen owns its own bar. |
| App nav host | `app/navigation/AppNavigation.kt:87–…` | `startDestination = HomeScreen` (line 89). `onNavigateToSettings` from Home actually navigates to **Profile** (line 98), not Tweaks. |

### 1.2 Feature screens

| # | Route | Composable | Description (1–2 sentences) | Data it surfaces (prose vs primitive today) |
|---|---|---|---|---|
| 1 | `HomeScreen` | `feature/home/presentation/HomeRoot.kt:100` → `HomeScreen` line 165 | Staggered grid of `RepositoryCard`s (`HomeRoot.kt:427`). Filter chips for category (Trending / Hot Release / Most Popular) + topic chips + platform popup; collapsible header on scroll (`HomeRoot.kt:216–234, 279–314`). | **Prose:** owner login, repo name, full description (2-line clamp), `Released 2 days ago`, language, fork badge. **Primitives:** star/fork/download counts as `InfoChip` rows, `PlatformChip` icons, `🔥` emoji prepended when `hasWeekNotPassed` (`core/presentation/.../RepositoryCard.kt:306–308`). |
| 2 | `SearchScreen` | `feature/search/presentation/SearchRoot.kt:107` | Search field + filter chips (platform / language / sort / source: GitHub / Codeberg / Custom) + paginated grid of same `RepositoryCard`. Clipboard banner detects pasted GitHub URLs. Bottom sheets for sort + language. | Same prose-heavy `RepositoryCard` payload as Home. Source chip is text. |
| 3 | `AuthenticationScreen` | `feature/auth/presentation/AuthenticationRoot.kt:…` | Three paths in one screen: web OAuth (PKCE handoff), device flow with copy-code card, PAT paste. State machine across `AuthPath.{Backend, Direct}` + PAT fallback sheet. | Code reveal text, polling spinner (CircularWavyProgressIndicator), error chips. No identity glyph at top. |
| 4 | `DetailsScreen` | `feature/details/presentation/DetailsRoot.kt` (1200+ LOC) — composes `sections/Header.kt`, `Stats.kt`, `WhatsNew.kt`, `ReleaseChannel.kt`, `ReportIssue.kt`, `Logs.kt`, `About.kt` + components/`AppHeader`, `SmartInstallButton`, `ReleaseAssetsPicker`, `VersionPicker`, `LanguagePicker`, `ApkInspectSheet`, `LinkedRepoBanner`. | `LazyColumn` of sections. Hero (`sections/Header.kt`): avatar + repo name + owner row + meta stats. `SmartInstallButton` (548 LOC) drives state across idle/download/install/done. Release notes via `multiplatform-markdown-renderer`. Translation via `TranslationControls` + `LanguagePicker`. APK Inspect sheet for Android. | **Prose:** `Released N days ago`, "Verified build" copy on signed releases (`SmartInstallButton.kt:625–632`). Star count + download count as numeric chips. Owner + ✓ self-owned badge. **No** wax-seal glyph; "Verified" is `Icons.Filled.VerifiedUser` + a text string. APK Inspect already surfaces permission groups + min/target SDK as numerals. |
| 5 | `ProfileScreen` | `feature/profile/presentation/ProfileRoot.kt:43, 177` | LazyColumn of two sections: `profile(...)` (`components/sections/ProfileSection.kt`) + `logout(...)`. Profile section embeds the **Sponsor card** (`Options.kt:114, 251–311`). Settings entry routes to TweaksScreen. **No** settings inline — they live in Tweaks. | Username, avatar circle, bio, repos count, "Favourites / Starred / Recently viewed" rows, "What's new" + "Announcements" rows, sponsor CTA, version footer. Prose-heavy. |
| 6 | `TweaksScreen` | `feature/tweaks/presentation/TweaksRoot.kt:46, 138 (TweaksScreen)` | LazyColumn of grouped settings: Account, Appearance, Installation, Language, Network, Translation, Others, About. Sub-screens for Hidden / Skipped / Host tokens / Mirror picker. Feedback sheet. Coachmark flags persisted (`apk_inspect_coachmark_shown`, `channel_chip_coachmark_shown` — `TweaksRepositoryImpl.kt:301–305`). | Toggle rows with M3 switches, dropdown pickers, value rows with chevrons. JetBrains Mono + Inter typography mismatch with handoff Fraunces requirement. |
| 7 | `DeveloperProfileScreen` | `feature/dev-profile/presentation/DeveloperProfileRoot.kt` | Username-keyed user profile: bio, stats row (`components/StatsRow.kt`), filter+sort (`FilterSortControls.kt`), grid of `DeveloperRepoItem`. | Avatar circle, followers/following/repos numerics, repo rows with star count + language. |
| 8 | `FavouritesScreen` | `feature/favourites/presentation/FavouritesRoot.kt:60, 94` | List of locally-saved favourites. Sort rules. | `FavouriteRepositoryItem` rows — avatar + name + meta. |
| 9 | `StarredReposScreen` | `feature/starred/presentation/StarredReposRoot.kt` | User's GitHub stars (live `/user/starred`). | List of repo rows. |
| 10 | `RecentlyViewedScreen` | `feature/recently-viewed/presentation/RecentlyViewedRoot.kt` | Locally-tracked viewed repos with timestamps. | `RecentlyViewedItem` rows. |
| 11 | `AppsScreen` | `feature/apps/presentation/AppsRoot.kt` (~1500 LOC) | **Android-only.** Lists installed apps; surfaces update state; multi-flavor variant picker; per-app advanced sheet; export/import (Obtainium JSON, manual link); starred picker. | `CompactAppRow` rows with `InstalledAppIcon`, version-state badges, `StatusDotCluster` for variants, source chips. Update banner for available updates. |
| 12 | `SponsorScreen` | `feature/profile/presentation/SponsorScreen.kt:56` | Donation CTAs: GitHub Sponsors (`:97-100`), Buy Me a Coffee (`:105-109`), "Other ways" (star repo / report bugs / share — `:259-298`). | Hero icon (VolunteerActivism), 2 elevated cards, support copy. |
| 13 | `ExternalImportScreen` | `feature/apps/presentation/import/ExternalImportRoot.kt` | Obtainium JSON import wizard with bucketed proposals. | Section banner + candidate rows. |
| 14 | `MirrorPickerScreen` | `feature/tweaks/presentation/mirror/MirrorPickerRoot.kt` | Pick GitHub mirror endpoint; latency probe. | Row per mirror + ms result + radio. |
| 15 | `StarredPickerScreen` | `feature/apps/presentation/starred/StarredPickerRoot.kt` | Scan signed-in user's stars for APK-shipping repos. | Rows with checkbox + asset hints. |
| 16 | `SkippedUpdatesScreen` | `feature/tweaks/presentation/skipped/SkippedUpdatesRoot.kt` | Per-app skipped-release-tag manager. | Rows + unskip button. |
| 17 | `HiddenRepositoriesScreen` | `feature/tweaks/presentation/hidden/HiddenRepositoriesRoot.kt` | Restore hidden repos from Home/Search long-press hide. | Rows + unhide / unhide-all. |
| 18 | `WhatsNewHistoryScreen` | `core/presentation/.../whatsnew/WhatsNewHistoryScreen.kt` | Versioned what's-new release notes (markdown). | Section headings + markdown blocks. |
| 19 | `AnnouncementsScreen` | `core/presentation/.../announcements/AnnouncementsRoot.kt` | Backend-served announcements (rate-limit notices, etc.). | List of announcement rows. |
| 20 | `HostTokensScreen` | `feature/tweaks/presentation/hosttokens/HostTokensRoot.kt` | Per-host PAT CRUD (KSafe-encrypted). | Row per host + token (masked), add dialog. |

---

## 2. Gap analysis — old → new (per screen)

Applies DESIGN.md §12 migration tables and §4 vocabulary. Every entry is concrete.

### 2.1 RepositoryCard (used by Home + Search + most lists)
- `Icons.Outlined.StarOutline` + numeric chip (`RepositoryCard.kt:245–248`) → **StarTier** (1–5 filled stars, log buckets at 1k/10k/50k/100k per tokens.json `thresholds.stars`) + count in JetBrains Mono. Reason: scannable tier replaces 5-digit text.
- `Icons.AutoMirrored.Outlined.CallSplit` fork count chip (`:250–253`) → **drop**. Reason: DESIGN.md §12.2 ("Forks/issues numbers — mostly drop; heartbeat does the 'alive' job"). Heartbeat is the live signal.
- `Icons.Outlined.Download` download chip (`:255–260`) → **DownloadWeight** dot (radius = log10(downloads)). Reason: §4.1.
- `Icons.Outlined.Code` language chip (`:262–267`) → **drop or fold into TopicGlyph**. Reason: language is rarely the decision input on an app store row.
- `"🔥 " + formatReleasedAt(...)` (`:306–311`) → **FreshnessRing around the avatar** + flame day-count pill on hero/lead cards. Reason: §1.4 forbids emoji as data; the FreshnessRing's fractional fill *is* the answer (§4.1).
- 40dp circle avatar (`:172–175`) → **avatar inside FreshnessRing**, ring color from `tokens.json.thresholds.freshness` keyed off `releaseRecency` days. Reason: §12.1.
- `PlatformChip` text labels (`:296–297`) → **PlatformGlyph** mono silhouettes (Android robot, Windows quad, macOS apple, Linux penguin). Reason: §12.1, §4.1. Filled = supported, dashed outline = unsupported.
- `IconButton(... ContentDescription = open_in_browser)` + share button (`:335–365`) → move into long-press `RepositoryActionsBottomSheet` (which already exists). Reason: §7.4 list-row template doesn't carry browser icons; cards stay quiet.
- Bottom CTA "View details" `GithubStoreButton` (`:329–333`) → **remove**; whole-card tap already navigates. Reason: §7.3 compact-card template has no inline CTA.
- Card shape (`ExpressiveCard` — `core/presentation/components/ExpressiveCard.kt`) — currently a generic M3 `Card` wrapper → **asymmetric squircle** `radD(16,12)`. Lead cards (top of Hot list) → **wonky squircle** (constraint: see §4). Reason: §5.1, §5.2.

### 2.2 HomeScreen layout
- `LazyVerticalStaggeredGrid(StaggeredGridCells.Adaptive(350.dp))` (`HomeRoot.kt:427–440`) → **vertical list with fixed section order** (Lead release → Hot releases → Trending → Most popular → From your stars). Reason: DESIGN.md §10.1 sets section order; staggered grid hides the rank semantic the new design relies on.
- Top tab `HomeFilterChips` for HomeCategory (TRENDING / HOT_RELEASE / MOST_POPULAR) — `components/HomeFilterChips.kt`, `LiquidGlassCategoryChips` — → **time-window filter** (Today/Week/Month/All) per §9.3 / §10.1. Reason: category becomes section, not filter. Filter is temporal.
- Topic chips row (`HomeRoot.kt:317–391`) → **keep**, but restyle: M3 `FilterChip` + `RoundedCornerShape(12.dp)` → asymmetric chip radD(11,8) (§7.2). TopicCategory icons stay; ensure they map to TopicGlyph vocabulary (§4.2) — drop any not in `tokens.json.topicGlyphs.supported`.
- Platform popup (`PlatformsPopup` — separate Composable in HomeRoot) → keep; restyle with wonky squircle dropdown + outline border (§16.7).
- Section headers — none today (sections are flat behind the category chip) → add **section header with Squiggle** (§7.5).

### 2.3 DetailsScreen
- Header `sections/Header.kt` with left-aligned avatar + repo name + chip row → on mobile, **centered hero** (`Avatar in FreshnessRing 92px → Fraunces italic name → owner login row → StarTier → topic glyphs`) per §9.4. Desktop stays left-aligned per §8.3.
- `SmartInstallButton` (Material `Button` with text + spinner — `components/SmartInstallButton.kt`) → **wonky-squircle primary CTA** with `boxShadow: 0 4px 12px -4px primary99` (DESIGN.md §7.1). Sub-meta below button ("permissions: low · arm64 · ▮▮▮▮") becomes a row of PermDot + PlatformGlyph + SignalBars.
- "Verified build" string + `Icons.Filled.VerifiedUser` (`SmartInstallButton.kt:625-632`) → **WaxSeal card** (§7.8). Mobile: top-of-section card; desktop: rotated stamp in install-panel corner. Three states: Sealed (successT), Broken (dangerT, **only place red is used aggressively**), Open (surface).
- `Stats.kt` 2-up / 4-up stats chips → **vital signs 2×2 grid** (`RELEASED · MAINTAINED · STARS · PERMISSIONS`) per §7.7. Each tile: glyph 22px + value Fraunces italic 13px + uppercase label 9.5px.
- APK Inspect sheet (`ApkInspectSheet.kt`, ~mobile-only) → keep as `APK Inspect` full-screen sheet (§9.6). Replace permissions wall-of-text with **PermDot heat + grouped chips** color-coded inline (Dangerous red / Sensitive amber / Normal green). Min/Target/Compile SDK become big italic numerals in 3 tiles.
- `LanguagePicker` translate UI → **dropdown menu** (§16.7) — copy the existing `TranslationControls.kt` pattern.
- "What's new" tabs across the top → **inner-screen takeover** with back arrow (§12.2). Version rail on the left, notes on the right.
- `Icons.Filled.Favorite/FavoriteBorder` heart button → keep; restyle as icon-only 36×36 button.
- `LinkedRepoBanner` → keep; restyle as accent-tinted banner per §7.6.

### 2.4 SearchScreen
- M3 `TextField` (`SearchRoot.kt` — search field) → **wonky-squircle search input** with the `tokens.json.shape.wonkySquircle.search` radii (`24px 18px 26px 20px / 18px 24px 20px 26px`).
- Source chip row (GitHub / Codeberg / Custom) — `SearchSourceUi` — → keep; restyle as filter chips (§7.2). Add `+ Add filter` dashed-border affordance for custom forges.
- `LanguageFilterBottomSheet` / `SortByBottomSheet` → **bottom sheet** (§16.1) — top-corners-only wonky squircle + drag handle.
- Recent-queries quick-chips (`SearchHistorySection.kt`) → keep; chip styling per §7.2.

### 2.5 ProfileScreen
- M3 `TopAppBar` with title only (`ProfileRoot.kt:231–242`) → top bar with **Cookie-shape user mark** on the right (§9.2).
- `profile(...)` section list (avatar + bio + nav rows) → **hero card with gradient** for identity (`tintP → surface` 135°, patterns.md "Hero card with gradient"). User avatar inside Cookie shape (§4.3) — *not* FreshnessRing (the cookie is the user-identity moment).
- `SponsorCard` (`components/sections/Options.kt:251`) — see §6 below. MIGRATION.md flags this as **must not be re-introduced** without product approval; current code still ships it. **Decision required**.
- Logout section → confirm dialog (§16.2) with Cookie-shape glyph above title.

### 2.6 TweaksScreen
- LazyColumn of grouped settings → keep structure; apply **List screen with sections** pattern (patterns.md). Section labels become uppercase 0.04em tracking.
- M3 switches → primary fill when on, surface2 when off (§"Form / Settings group").
- Value rows ("Theme: Ocean Blue", "Font: System") → caption + `›` chevron.
- Coachmark popups (APK Inspect pulse, ReleaseChannel chip popup) → keep; restyle popup container per §16.7.

### 2.7 AppsScreen (Library equivalent)
- TopAppBar + filter row + sort dropdown → **Library** layout (§9.5): top-bar title "Library" + meta line "N apps · M updates available" → update banner with VersionStack glyph → tab row [Installed | Updates · 1 | Pending] → rows.
- `CompactAppRow` (`components/CompactAppRow.kt`) → **list row** template (§7.4): `[avatar in FreshnessRing] [name + heartbeat] [version tag in Mono] [Open / Update button (wonky squircle)]`.
- `StatusDotCluster` for multi-flavor variant indicator (`components/StatusDotCluster.kt`) → stays — already a primitive-style cluster; just align colors with `tokens.json.status.freshness`.
- Update banner inside the screen → accent-tinted banner with **VersionStack glyph + accent button** (§12.1, §"Update banner").
- "Update all" extended FAB → maybe drop; the per-row Update button + update-banner CTA already covers it. Open question.
- Variant picker dialog (`VariantPickerDialog.kt`) → confirm dialog (§16.2) with VersionStack glyph at top.

### 2.8 AuthenticationScreen
- Web-OAuth + Device-flow + PAT-paste split → **full-screen sheet** layout (§16.5) with **CookieShape · GitHub** identity mark at top.
- Device-code reveal box (`CardDefaults.elevatedCardColors`) → mono JetBrains Mono 28–32px in primary-bordered wonky-squircle code box.
- Polling spinner (`CircularWavyProgressIndicator`) → **Heartbeat glyph** + "waiting…" caption (§16.5; reuses the maintenance-state vocabulary).
- "Use Personal Access Token" fallback → outlined button at bottom of the sheet.
- PAT bottom sheet (`ModalBottomSheet`) → bottom sheet (§16.1) with drag handle + top-corners-only wonky squircle.

### 2.9 SponsorScreen
- `Icons.Filled.VolunteerActivism` hero (`:141`) + 2 elevated cards → **decision required** before redesign. MIGRATION.md explicitly bans donate UI without product approval. If kept, restyle hero with Cookie + Squiggle; cards become squircle compact cards.

### 2.10 Favourites / Starred / Recently-viewed
- Item composables (`FavouriteRepositoryItem`, default starred row, `RecentlyViewedItem`) → unify into the **list row** template (§7.4). All three should reuse the same row composable; today they're three slightly-different layouts.

### 2.11 DeveloperProfileScreen
- `ProfileInfoCard` (avatar + bio) → identity hero card (gradient `tintP → surface`) with avatar inside a soft outline circle (not Cookie — Cookie is reserved for the signed-in user). `StatsRow` (followers/following/repos) → 3-tile vital-signs row (Fraunces italic numerals).
- `DeveloperRepoItem` rows → list-row template (§7.4) with StarTier instead of numeric stars.

### 2.12 HostTokensScreen
- Per-host PAT rows (mask + edit + delete) → list rows; PAT value in JetBrains Mono `••••••••••••` (mono is the "technical fact" voice per §3.3). Add dialog → bottom sheet.

### 2.13 MirrorPickerScreen
- Radio + ms latency rows → list row with **SignalBars** primitive replacing the numeric ms label. Numeric ms can stay in mono as the secondary line.

### 2.14 WhatsNewHistoryScreen
- Versioned markdown sections → **inner-screen** style (§8.4): version rail + selected version notes. Existing markdown renderer is reusable.

### 2.15 ExternalImportScreen / StarredPickerScreen
- Pre-import summary buckets → section list with squiggle headers (`patterns.md` "List screen with sections"). Candidate rows: avatar + name + matched-host chip + checkbox. Bottom CTA: outline Cancel + primary wonky "Import N".

---

## 3. Information-architecture deltas (what to keep, change, drop)

MIGRATION.md prescribes IA changes (bottom-nav 4→3, Settings into Profile, Library single-screen). Reality at GHS:

| Handoff prescription | GHS actual today | Recommendation |
|---|---|---|
| Bottom nav 4 → 3 tabs (Home, Library, Profile) + detached Search FAB | **5 tabs** today (Home, Search, Apps, Profile, Tweaks — `BottomNavigationUtils.kt:22–51`), Apps Android-only | Move to **4** on Android (Home, Search, Library, Profile) and **3** on desktop drawer (Home, Library, Profile — Library replaces "Apps"; Search becomes top-bar / drawer search per §8.1). **Reject** the "Search becomes FAB only" recommendation — Search-as-tab is a power-user fixture in GHS and the third-most-used surface. Keep Search visible. |
| Settings moves into Profile | **Already split:** Tweaks is the settings home (`feature/tweaks/`), Profile is identity-only (`feature/profile/CLAUDE.md`). Tweaks is currently a 5th bottom-nav tab. | **Drop Tweaks from bottom nav**; surface "Settings" as a row in Profile (already wired — `HomeRoot.onNavigateToSettings` lands on Profile, then Profile→Tweaks is one tap). The route stays. |
| Library single screen replacing "Installed / Updates / Stars" tabs | **GHS today:** Apps screen has tabs/sort but no Starred tab; Starred is a separate route reached from Profile (`StarredReposScreen`). | **Don't merge Starred into Library.** Starred is GitHub-account-bound; Apps is local-install-bound. They share row visuals but not data semantics. Library = Apps (rename) with Installed/Updates sections per §9.5 ; Stars stays under Profile. |
| Profile's "Support" replaced by "Connect" + "Business inquiries"; no donations | GHS still ships `SponsorScreen` with GitHub Sponsors + Buy Me a Coffee links | **Product call required.** MIGRATION.md is opinionated; rainxchzed is the maintainer and may want sponsorship kept. Audit flags this — see §6 risks. |
| "Translate the app" CTA banned | GHS ships **README translation** (not app translation) via `TranslationRepository` — Google / Youdao / LibreTranslate / DeepL / Microsoft. App localization is 13-locale built-in via Compose resources. | **Not applicable** — handoff is talking about *contributing translations*, not in-app content translation. README translation stays. App-locale picker in Tweaks stays. |
| Multi-script font fallback for CJK/Devanagari/Arabic/Hebrew | GHS bundles Inter (Latin+Cyrillic) and JetBrains Mono only. No Noto fallback set. 8 of 13 shipped locales use non-Latin scripts (ar, bn, hi, ja, ko, ru partially, tr partially, zh-CN). | **Action required** — see §4. Add Noto Sans family fallback via Compose `FontFamily` resolver. |
| Pre-release toggle: per-app overrides global | GHS already implements this (`InstallationManagerImpl` seeds new install from `include_pre_releases` pref; existing rows keep per-app value — `feature/tweaks/CLAUDE.md`). | **No change.** Already aligned. |
| Connect card on Profile | Not present | Add only if Sponsor card is removed (replacement narrative). |

### 3.1 Bottom-nav delta (concrete)

`BottomNavigationUtils.items()` currently builds:
```
0 Home, 1 Search, 2 Apps (Android), 3 Profile, 4 Tweaks
```
Proposed:
```
Android:   Home, Search, Library, Profile     (Tweaks reachable via Profile)
Desktop:   Drawer with Home, Library, Profile (Search via drawer header + ⌘K; Tweaks via Profile)
```
That's **4 on Android** (handoff says 3 — we push back; see above) and **3 on desktop** (drawer matches §8.1 exactly).

---

## 4. Cross-platform / Compose constraints

The handoff is web-native (CSS). Several conventions don't translate cleanly to Compose Multiplatform. Each constraint listed once with every place it bites.

### 4.1 Wonky squircle (asymmetric elliptical radii)
- **CSS does:** `border-radius: 20px 14px 22px 16px / 16px 22px 14px 20px` — four corners, each with independent horizontal + vertical radii (8 numbers total).
- **Compose can't:** `RoundedCornerShape(topStart, topEnd, bottomEnd, bottomStart)` takes 4 numbers, applied as circular radius per corner. There is **no built-in elliptical radius**.
- **Mitigation:** Build a custom `Shape` (Path-based) — `AsymmetricSquircleShape(topStart: Size, topEnd: Size, bottomEnd: Size, bottomStart: Size)` that emits a `Path` with `arcTo(rect, …, sweepAngle, …)` per corner using ellipse bounds. Live in `core/presentation/shape/` (new package).
- **Where it matters:**
  - All primary CTAs — `SmartInstallButton.kt` (Details), the "Update" / "Open" CTAs in `AppsRoot` (Apps), `GithubStoreButton` (`core/presentation/components/GithubStoreButton.kt`) used in Home cards and Search.
  - Lead release card on Home (top of Hot list).
  - Search input — `feature/search/.../SearchRoot.kt` `TextField`.
  - Device-code reveal box on Authentication.
  - Bottom sheet top corners — `ReleaseAssetsPicker`, `LanguageFilterBottomSheet`, `SortByBottomSheet`, `LinkAppBottomSheet`, `ImportSummarySheet`, `AdvancedAppSettingsBottomSheet`, `ApkInspectSheet`, `RepositoryActionsBottomSheet`. ModalBottomSheet's `shape` parameter accepts a custom Shape — feed the asymmetric squircle there.
  - Toast / Snackbar background (§16.3) — `core/presentation/.../components/` (no shared snackbar today).
  - Confirm dialog containers — `LogoutDialog`, `ClearDownloadsDialog`, `VariantPickerDialog`, the unlink-external-app dialog (`DetailsRoot.kt`), the discard-pending-install dialog.

### 4.2 Cookie shape (9-petal organic)
- **CSS does:** SVG path import.
- **Compose:** `Shape` from `Path` — feed `tokens.json.shape.cookie.path` into `Path.parseSvgPath` (KMP support varies; use `Path` API + manual cubic-Bezier commands). Live in `core/presentation/shape/CookieShape.kt` (new).
- **Where:** brand mark at top-left of HomeScreen top-bar, user mark at top-right, active bottom-nav tab background (Android), authentication identity mark, ProfileScreen avatar tile. Four touchpoints (§5.3).

### 4.3 Squiggle underline
- **CSS does:** inline SVG `<path>` 36–42px × 5px.
- **Compose:** small `Canvas` with `drawPath` of the `tokens.json.shape.squiggle.path`. Component `SectionHeading(text, modifier)` in `core/presentation/components/`.
- **Where:** every section header — Home Hot/Trending/Popular, Tweaks section labels, Library Installed/Updates sections, Detail About/Permissions sections, Diagnostics card (Tweaks → Feedback).

### 4.4 Drop shadow on primary CTAs
- **CSS does:** `box-shadow: 0 4px 12px -4px primary99`.
- **Compose:** `Modifier.shadow(elevation, shape, ambientColor, spotColor)`. Custom color shadows only on **API 28+** Android — min SDK 26 → graceful fallback on 26/27 to neutral shadow. Use `androidx.compose.ui.draw.shadow` with `ambientColor` + `spotColor` and accept that on API 26/27 it falls back.

### 4.5 Fonts (most critical mismatch)
- **Handoff wants:** Fraunces (italic 600), Inter Tight, JetBrains Mono.
- **GHS has:** Inter (NOT Inter Tight — DESIGN.md §3.4 explicitly forbids), JetBrains Mono, **no Fraunces**. See `core/presentation/.../theme/Type.kt:21–30, 14–19` and `composeResources/font/` listing.
- **Worse:** `Type.kt:43–51` uses **JetBrains Mono for displayLarge through titleSmall** — i.e. the entire heading stack is mono. DESIGN.md §3.3 explicitly says mono is "for technical artifacts (versions, hashes, package names) so it carries weight when it appears."
- **Mitigation:** bundle Fraunces (variable + italic) + Inter Tight in `core/presentation/.../composeResources/font/`. Add Noto Sans CJK/Devanagari/Arabic fallbacks (MIGRATION.md "Multi-script type stack"). Rewrite `Type.kt` to map display/headline/title → Fraunces italic; body/label → Inter Tight; introduce a new typed `monoFontFamily` for tokens explicitly tagged as mono (versions, SHAs, package names).
- **FontTheme enum** (`core/domain/.../FontTheme.kt:6–8`) currently `SYSTEM | CUSTOM("JetBrains Mono + Inter")`. The display name lies after the refactor; pre-refactor user pref persistence keeps working but the label is stale.

### 4.6 Heartbeat animation
- **CSS does:** CSS keyframes.
- **Compose:** `rememberInfiniteTransition` + `animateFloat`. Periods from `tokens.json.thresholds.maintenance`. Skip animation when `Settings → Accessibility → Reduce motion` is on (Android: `Settings.Global.ANIMATOR_DURATION_SCALE == 0`; desktop: respect OS pref where available).

### 4.7 macOS-style traffic-light window chrome (desktop §8)
- **CSS does:** mock traffic lights with three colored circles.
- **Compose Desktop:** `Window` on macOS already provides native traffic lights when `undecorated = false`. Don't re-draw them — let macOS render its own. On Windows/Linux, draw chrome ourselves at 36px tall with title centered.

### 4.8 Color shadows behind FreshnessRing accent
- The per-app accent (`accent: { c, lt, dt }` per repo) is **not currently stored** anywhere. `core/data/dto/BackendRepoResponse.kt` has no accent field. **Action:** add to backend response (open question per DESIGN.md §14 OQ #2) or derive client-side (color-thief on avatar). For now, leave the accent slot unfilled and let primitives use palette `primary` as fallback.

---

## 5. Honesty audit (data we invent vs derive)

DESIGN.md §11 forbids invented data. Walk of current code:

| Location | Today | Verdict | Action |
|---|---|---|---|
| `RepositoryCard.kt:306–308` | `"🔥 " + formatReleasedAt(updatedAt)` when `hasWeekNotPassed(updatedAt)`. The 🔥 is conditional on a real signal (≤ 7 days). | **Derivable** — but emoji forbidden by §1.4. | Replace emoji with FreshnessRing fraction (the ring already encodes "hot"). |
| `feature/home/data/.../HomeRepositoryImpl.kt:571–604` | Local affinity-`score` computed by adding bonuses (topics, language, description keywords). Drives sort. | **Borderline.** Not displayed as a percentage; only used to order. Per DESIGN.md §14 OQ #1, this is a "local sort proxy" while backend `trendingScore` is null. | **Keep**, but never surface as text. Position (`#1`, `#2`) is the only honest display. |
| `core/data/dto/BackendRepoResponse.kt:27` | `trendingScore: Double? = null` from backend. Used only for sort key in `CachedRepositoriesDataSourceImpl.kt:207`. | **Honest** — used as opaque sort key, not displayed. | No change. Don't ever render this as a percentage. |
| `SmartInstallButton.kt:625–632` | "Verified build" string + `Icons.Filled.VerifiedUser` whenever `AttestationStatus.VERIFIED`. | **Derivable** — backed by real attestation verifier (`DetailsViewModel.kt:2315`). | Migrate copy to **WaxSeal** glyph + "Sealed" Fraunces label + sha256 fingerprint in mono. The verification result is real; the *representation* needs to align with vocabulary. |
| `Stats.kt` (Details) numeric chips (stars, forks, downloads) | Real numbers from API. | **Honest.** | Replace stars with StarTier+count, drop forks (§12.2), keep downloads as DownloadWeight dot + count. |
| `formatCount(...)` (used by `RepositoryCard`) | Pretty-print thousands. | **Honest.** | Keep; render in JetBrains Mono (technical fact per §3.3). |
| Search "trending" — no current invented signal | n/a | — | — |
| "Featured" curation — **no occurrence in codebase** (verified `grep -rn "[Ff]eatured" --include='*.kt'` returns zero hits). | n/a | — | The new Lead-release slot (§10.1) is honestly "top of the filtered Hot list" — match that. |
| "+X%" trending percentages — **no occurrence in codebase** (verified). | n/a | — | Keep that way. |

**Net:** the only active honesty violation today is the 🔥 emoji at RepositoryCard.kt:307. The local affinity score is a sort proxy and acceptable. The "Verified build" string is honest but uses the wrong vocabulary (icon-with-label instead of WaxSeal).

---

## 6. Risks + breaking changes for users

| Risk | Impact | Mitigation |
|---|---|---|
| **Bottom-nav reduction from 5 → 4 (Android)** | Tweaks tab disappearing breaks muscle memory; users tap empty space. | One-shot coachmark on Profile→Settings row first time Profile is opened post-upgrade. Add `K_SETTINGS_RELOCATION_COACHMARK_SHOWN` to `TweaksRepositoryImpl.kt:477+` (companion object) and a `getSettingsRelocationCoachmarkShown()`/`setSettingsRelocationCoachmarkShown(Boolean)` pair to `TweaksRepository.kt`. Trigger from `ProfileRoot` first composition. |
| **Apps tab renamed to Library** | Users searching for "Apps" in onboarding/help. | Keep the route name `AppsScreen` (don't churn the nav graph). Only rename the visible label `Res.string.bottom_nav_apps_title`. Add a what's-new bullet in the upcoming-release JSON. |
| **Bottom-nav 5 → 4 → 3 path on desktop** | Desktop already filters out Apps (`BottomNavigationUtils.kt:54–59`); going to drawer is a bigger redesign than dropping a tab. | Ship drawer behind a behind-feature-flag toggle in Tweaks → Appearance → "Use drawer (experimental)" until validated. Reuse the existing `convention.cmp.application` build config; no new module. |
| **Typography change (mono headings → Fraunces italic)** | Users who chose `FontTheme.CUSTOM` ("JetBrains Mono + Inter") expect mono everywhere. Visual whiplash. | Add a new `FontTheme.STORE` variant (Fraunces + Inter Tight + JetBrains Mono) as the *new default*. Keep `CUSTOM` as a legacy choice for users who don't want the change. Migration: do **not** auto-flip persisted `CUSTOM` to `STORE`; let users opt in. |
| **Palette change (Ocean/Purple/Forest/Slate/Amber → Nord/Cream/Forest/Plum)** | `AppTheme` enum (`core/domain/.../AppTheme.kt:3–10`) loses two values, adds two. Persisted `AppTheme.fromName` returns OCEAN default — users with PURPLE/SLATE/AMBER land on OCEAN→Nord silently. | Map old→new in `AppTheme.fromName` migration: `OCEAN→NORD`, `PURPLE→PLUM`, `FOREST→FOREST`, `SLATE→NORD`, `AMBER→CREAM`. Surface a one-shot snackbar "Your theme was updated to Nord — pick a different one in Tweaks → Appearance" if the old name was not NORD. Persist a `theme_migrated_v1` flag in `TweaksRepository`. |
| **Sponsor card removal** | Maintainer revenue (rainxchzed). | **Decision required by product / maintainer.** Audit recommends *keeping* SponsorScreen at the project's discretion regardless of MIGRATION.md ban — but restyle, don't remove, unless owner says so. |
| **Sheet shape change (rounded → wonky asymmetric)** | Visual change only, no functional regression. | None needed. |
| **Heartbeat motion on Library rows** | Users with reduce-motion will get static dot per DESIGN.md §6.1; tokens.json `thresholds.maintenance` already has a `dormant` (`heartbeat_period_s: null`) fallback. | Honor system reduce-motion: snap to "dormant" period (no animation). |
| **AppsRoot complexity (~1500 LOC)** | Refactor risk: regressing variant picker, advanced sheet, import flow. | Migrate visuals only; keep all behavior + state machinery. New `CompactAppRow` is a re-skin of the existing — same params. |
| **Markdown rendering in inner-screen takeover** | Existing chunked progressive rendering + theme-aware images (`details/CLAUDE.md` "Markdown perf") is fragile. | Reuse existing markdown stack 1:1. Only the surrounding chrome changes (back arrow + tabs + version rail). |

---

## 7. Open questions for product (beyond DESIGN.md §14)

| # | Question | Conflicts with |
|---|---|---|
| 1 | **Per-host PATs (Codeberg / Forgejo / custom forges).** Handoff assumes a single GitHub identity. How do non-GitHub forges represent "the signed-in user" in the user-mark Cookie shape on top bars? Display the GitHub user even when looking at a Codeberg repo? | DESIGN.md §9.2 + §16.5 (CookieShape · GitHub identity mark) assumes one identity. |
| 2 | **Foreign-source repos (`sourceHost != null`) on Home / Search.** Codeberg / Forgejo repos are interleaved with GitHub repos in lists. Do they show the same FreshnessRing / StarTier / DownloadWeight (signal vocabulary is host-agnostic) — but with a tiny host-glyph (Forgejo flame icon, Codeberg blue G) somewhere on the card? | DESIGN.md doesn't address multi-source rows. `RepoIdCodec` packs host into `repoId`. |
| 3 | **Multi-flavor installed apps (#638).** A single repo can ship multiple APK variants (generic + Play). `StatusDotCluster` already shows this. New design has no concept of "this row represents N installed packages." How does the row's Update CTA disambiguate? | DESIGN.md §9.5 Library rows assume 1 repo = 1 install. |
| 4 | **Web-OAuth path with handoff custom-scheme.** The handoff §16.5 OAuth full-screen sheet shows a device-flow code box. Our primary path is web OAuth (no code) — the user gets bounced to a browser and back. The sheet should reflect that: "Open browser → Sign in → Return here" steps, not "Open github.com/login/device → enter code". | §16.5 numbered steps assume device flow as primary. |
| 5 | **KSafe-encrypted prefs and the Diagnostics card (§16.4).** The "what we'll attach" JSON pretty-prints prefs. Some prefs are KSafe-encrypted (per-host PATs, translation API keys). They must NEVER appear in diagnostics — even masked. Confirm the diagnostics block lists only non-secret fields, and that the Tweaks feedback sheet (`FeedbackBottomSheet`) is already aligned with this. | §16.4 doesn't enumerate which fields are secret. |
| 6 | **Shizuku / Dhizuku / Root installer state on Library rows.** When a row is installed via Shizuku, do we surface that as a glyph next to the version, or only inside the per-app advanced sheet? | DESIGN.md is GitHub-Releases-only — Android installer plurality isn't addressed. |
| 7 | **Skipped releases (per-app `skippedReleaseTag`, E542).** When a user has skipped v2.7.5 of immich, do we show VersionStack (v2.7.0 → v2.7.5) as "you skipped this" with a different color, or suppress entirely until a newer release? Current code suppresses CTA until strictly-newer release. | DESIGN.md §12.4 says VersionStack badge replaces M3 numeric "1"; doesn't address skip state. |
| 8 | **Ignore-updates** (silence the badge per app). Same family as skip — should the row still show a "Update available" VersionStack with a "silenced" dashed-outline modifier? | Not addressed. |
| 9 | **Trust-fingerprint storage** (DESIGN.md §14 OQ #4) — where do we persist the previously-installed signing fingerprint? Compose existing `AttestationVerifier` (`DetailsViewModel.kt:2315`) — is the "previously-installed fingerprint" available, or do we need a new Room column on `InstalledApp`? | OQ #4 already flagged; needs concrete answer for the broken-wax-seal flow. |
| 10 | **Translation provider credentials in the OAuth sheet treatment.** TweaksRoot's Translation section has LibreTranslate + DeepL + Microsoft credential forms with show/hide toggles. These don't map to any handoff section. Keep current pattern; flag for review. | n/a |
| 11 | **Locale picker remains in Tweaks** (13 locales) — handoff doesn't address app-level locale switching. Keep as-is. | DESIGN.md doesn't mention. |
| 12 | **Sponsor / "Connect" call.** Final answer needed before §2.9 work begins. | MIGRATION.md "Risky areas". |
| 13 | **What-to-do with "Tweaks" as a brand word.** Rename to "Settings" matching handoff vocabulary, or keep "Tweaks" (existing affordance, localized in 13 languages)? | DESIGN.md §8.1 uses "Settings" in the drawer; existing string `bottom_nav_profile_tweaks` is "Tweaks". |
| 14 | **Announcements & WhatsNew** sit inside Profile today. Handoff doesn't address them. Keep, restyle. | n/a |

---

## 8. Recommended sequencing (KMP/CMP-tailored, replaces MIGRATION.md order)

MIGRATION.md is web-bottom-up. Tuned for our layered modules:

| # | Phase | Concrete paths | Why |
|---|---|---|---|
| 1 | **Foundation — design tokens module** | New: `core/presentation/src/commonMain/.../tokens/` with `Palette.kt` (4 palettes × light/dark from tokens.json), `Status.kt` (freshness/wax/perm/trend hex), `Shapes.kt`, `Typography.kt`, `Motion.kt`. Rewrite: `core/presentation/.../theme/Color.kt`, `Theme.kt`, `Type.kt`. | Everything below consumes tokens. No screen work yet. |
| 2 | **Fonts** | Bundle Fraunces variable+italic + Inter Tight + Noto Sans CJK/Devanagari/Arabic into `core/presentation/.../composeResources/font/`. Rewrite `Type.kt` mapping (Fraunces for display/headline/title; Inter Tight for body/label; mono for explicit-mono slots). Migrate `FontTheme` enum: add `STORE` value, leave `CUSTOM` for legacy. | Phase-1 typography unblocks every label + heading change downstream. |
| 3 | **Shape primitives (Compose-specific)** | New: `core/presentation/src/commonMain/.../shape/AsymmetricSquircleShape.kt`, `WonkySquircleShape.kt`, `CookieShape.kt`, `SquiggleUnderline.kt` (Canvas composable). | Required before any card/CTA/sheet refactor. |
| 4 | **Silent Vocabulary primitives** | New: `core/presentation/src/commonMain/.../primitives/FreshnessRing.kt`, `Heartbeat.kt`, `StarTier.kt`, `WaxSeal.kt`, `VersionDelta.kt`, `VersionStack.kt`, `PermDot.kt`, `PlatformGlyph.kt`, `TopicGlyph.kt`, `SignalBars.kt`, `DownloadWeight.kt`, `LicensePosture.kt`. All take their data via typed inputs (days since release, star count, etc.) — no string parsing. | Bottom-up: the 12 primitives compose every screen below. Drop-in replacements for existing chips. |
| 5 | **Shared chrome — TopBar, BottomNav, SectionHeader, Banner, Toast** | Rewrite: `composeApp/.../app/navigation/BottomNavigation.kt` (Cookie-shape active tab, Fraunces italic label, VersionStack badge). Rewrite: `BottomNavigationUtils.kt` (drop Tweaks from items, add Library label). New: `core/presentation/.../components/SectionHeader.kt`, `Banner.kt`, `StoreToast.kt`. | Cross-cutting chrome lands before any screen. Bottom-nav reduction lives here. |
| 6 | **RepositoryCard rewrite** | Rewrite: `core/presentation/.../components/RepositoryCard.kt` + `ExpressiveCard.kt`. Now uses primitives + asymmetric squircle. | Single component change ripples to Home + Search + Favourites + Starred + Recently-viewed + DeveloperProfile. Biggest visual win per hour. |
| 7 | **HomeScreen** | Rewrite section order (Lead → Hot → Trending → Popular → Stars). Replace `HomeCategory` chips with time-window filter (Today/Week/Month/All) inside the state. Add lead-card with wonky squircle + radial bloom. Edit: `feature/home/presentation/HomeRoot.kt`, `HomeState.kt` (add `timeWindow`), `HomeViewModel.kt`. Keep `HomeRepository` interface intact. | High-traffic; sets the tone. The IA change (category becomes section, not filter) is the headline. |
| 8 | **AppsScreen → Library** | Edit: `feature/apps/presentation/AppsRoot.kt` (new layout per §9.5), `CompactAppRow.kt` (new primitives). Rename label `bottom_nav_apps_title` → `bottom_nav_library_title` (touch every 13 locales). Route name stays `AppsScreen` to avoid churn. | Establishes the list-row template (§7.4) used everywhere else. |
| 9 | **ProfileScreen** | Edit: `feature/profile/presentation/ProfileRoot.kt`, `components/sections/ProfileSection.kt`, `Options.kt`. Cookie-shape user mark in top bar. Identity hero card with gradient. Settings row → Tweaks (already wired). **Decide** sponsor card. | Required before bottom-nav reduction. |
| 10 | **Bottom-nav reduction** | Edit: `BottomNavigationUtils.kt:20–60` — drop Tweaks entry. Add settings-relocation coachmark in `TweaksRepository` + trigger from `ProfileRoot`. | Do AFTER Profile lands. Don't strand users. |
| 11 | **DetailsScreen** | Edit: `feature/details/presentation/DetailsRoot.kt`, `sections/Header.kt`, `Stats.kt`, `components/SmartInstallButton.kt`. WaxSeal card. Vital signs 2×2. Wonky-squircle CTA. Permission heat in `ApkInspectSheet.kt`. About / What's new as inner-screen takeover. | Longest screen; touches the most components. After bottom-up primitives stabilize. |
| 12 | **SearchScreen** | Edit: `feature/search/presentation/SearchRoot.kt` — wonky-squircle search input, restyled chips, restyled sheets. | Reuses primitives; quick. |
| 13 | **Tweaks** | Edit: `feature/tweaks/presentation/TweaksRoot.kt`, `components/sections/*.kt`. Section labels with Squiggle. Form rows with tinted icon shell + chevron. Feedback diagnostics card per §16.4. | Low priority — users hit Tweaks rarely. |
| 14 | **AuthenticationScreen** | Edit: `feature/auth/presentation/AuthenticationRoot.kt`. Full-screen sheet layout. Cookie · GitHub identity mark. Heartbeat polling indicator. Code-box wonky squircle. Adapt step copy for web OAuth path (Q #4 above). | Done late; rarely-seen post-signup. |
| 15 | **Net-new screens** | DeveloperProfile re-skin, Favourites/Starred/Recently-viewed unify to list-row template, HostTokens / Skipped / Hidden / Mirror / WhatsNew / Announcements re-skin. | All reuse phase-4–6 primitives. Mostly parallelizable. |
| 16 | **Sponsor decision** | Either remove `SponsorScreen.kt` + nav entry, or restyle. Coordinate with maintainer. | Last — depends on product call (Q #12). |
| 17 | **Polish** | Heartbeat reduce-motion guard. Theme-migration mapping (Q under §6 "Palette change"). Localization audit (Fraunces glyph coverage for ar / bn / hi). | Catch-up; ship behind the last bullet. |

### 8.1 Per-phase exit criteria
- After Phase 4: a primitives gallery preview composable renders all 12 silent vocab items + 2 expressive shapes in a dev-only screen behind a debug flag.
- After Phase 6: Home + Search + Favourites + Starred render with new RepositoryCard and no other screen yet touched. Visual regression expected, no functional regression.
- After Phase 11: every primary user flow (discover → install → manage → settings) reads in the new vocabulary; remaining work is net-new screens + polish.

### 8.2 Parallelization hints
- Phases 1–4 are strictly sequential (foundation).
- Phases 5–6 can fork once Phase 4 is on `main`.
- Phases 7–13 can fork once Phase 6 lands.
- Phase 14 (Auth) and Phase 15 (net-new) parallelize freely.
- One screen per session per MIGRATION.md principle 1 — still holds.

---

## 9. Out-of-scope notes (flagged, not actioned)

- `HomeRepositoryImpl.kt:571–604` local affinity score: stay as a sort-only proxy, never displayed. If backend supplies real `trendingScore` in a future API revision, this code can be deleted (open question DESIGN.md §14 OQ #1).
- `core/domain/.../util/EmojiShortcodes.kt` (290+ short-code → emoji map) is for **user content** (release notes, READMEs) only. Don't touch.
- `WhatsNewLoaderImpl.kt` per-version markdown is content, not chrome. Re-skin the renderer wrapper, not the markdown.
- `core/data/network/BackendApiClient.kt` calls out to `api.github-store.org` — unchanged by design refresh.
- Per-host PATs (`HostTokenRepository`, `HostTokensScreen`) — visual re-skin only; AES-256-GCM storage stays.

---

End of audit. Path: `/Users/rainxchzed/Documents/development/kmp/GitHub-Store/.design/UX-AUDIT.md`.
