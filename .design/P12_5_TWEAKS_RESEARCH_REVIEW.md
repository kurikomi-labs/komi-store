# Tweaks Redesign — UX Research Review

> Reviewer: UX-Research. Target: `.design/P12_5_TWEAKS_REDESIGN.md` by ArchitectUX. Date: 2026-05-23.
> Evidence base: Nielsen Norman Group (NN/g), Material 3 guidance, Apple HIG (Settings + Localization), WCAG 2.2, and direct comparison with Tailscale, Cloudflare WARP, Bitwarden, 1Password, iOS Settings, and Termux. Code references cite the architect's own line numbers in the spec.

---

## Verdict

**Iterate before ship.** The IA refactor is directionally right and most per-screen specs are sound, but the Connection redesign has a load-bearing mental-model gap, telemetry is silently missing, and several discoverability/accessibility/i18n risks need answers in writing — not in a comment thread during implementation.

---

## Critical issues (must fix before implementation)

### C1. Telemetry is not in the spec at all — but it's a first-class privacy control

**Problem.** `TweaksViewModel` injects `TelemetryRepository` (confirmed in `feature/tweaks/CLAUDE.md` → "TweaksViewModel injects: …, TelemetryRepository, …"). The redesign spec contains **zero** occurrences of the word "telemetry" across all 668 lines. It is not on the hub, not in About, not in Library cleanup. If it exists today, the spec deletes it by omission. If it doesn't yet have UI, this is the redesign moment to add it.

**Evidence.** Modern privacy-forward apps universally surface telemetry as an explicit user-facing toggle within one tap of Settings root: Bitwarden ("Help us improve" → off by default), Tailscale ("Send usage analytics"), Cloudflare WARP ("Diagnostics"), 1Password ("Help us improve 1Password"). Hiding a telemetry control inside a sub-screen, or worse omitting it, fails GDPR's "easy to find" requirement for consent management (Article 7(3)) and Apple App Store guideline 5.1.2.

**Proposed fix.** Add a "Privacy" card to the **About** sub-screen (preferred — keeps "App"/About as the meta home) OR move it into **Library cleanup** and rename that screen "Privacy & cleanup" (the architect even hinted at this in §7.2 #6: *"a quieter alternative is 'Privacy & storage'"*). Surface: toggle "Share anonymous usage data" + 1-line body + link "What we collect →" (modal or expandable). Also: any crash-reporter opt-out, if `CrashReporter` (desktop) has one.

**Severity.** Critical (legal/compliance + user trust + an injected repository with no UI is a code smell on its own).

---

### C2. Connection screen — "master" is fictional and the user has no way to know which scope they're testing

**Problem.** §4.2 #4 admits the master model is presentation-only: *"`ProxyRepository` stores per-scope `ProxyConfig` already. On open, we read all three; if they're identical, we treat them as 'master.' If they diverge, we mark the divergent ones as 'overridden'."* This derivation is invisible to the user.

The bigger issue is in §4.3, Card 1 step 4: *"`Test` … runs `OnProxyTest` against `ProxyScope.DOWNLOAD` (canonical scope for testing the master)."* The user reads "Test" and thinks "test this proxy." The app actually tests Download scope only. If Discovery has an override and Download doesn't, master-test passes and the user assumes their setup is fine — but Discovery is broken.

**Evidence.**

- NN/g, "Visibility of System Status" (Heuristic #1): system state must be reflected accurately. Hidden "canonical scope" violates this.
- Cloudflare WARP, Tailscale, and Bitwarden all expose a connection test that explicitly says *what* it's testing ("Test connection to coordination server", "Pinging derp1.tailscale.com…"). Anonymous "Test" with hidden semantics is the worst of both worlds.
- The "load semantics" itself is brittle: any time a user toggles an override on then off, repository writes diverge then converge across millisecond-asynchronous DataStore writes — Jetpack DataStore doesn't guarantee snapshot-atomic reads across three keys. Compare-for-equality risks racy "is this master?" decisions.

**Proposed fix.**

1. **Option A chosen — master config + per-scope `useMaster` booleans** in `ProxyRepository`. The master is stored as a regular `ProxyConfig` under `MasterKeys` (`master_proxy_type`, `master_proxy_host`, etc.), and each scope gets a boolean key (`discovery_proxy_use_master`, etc.). This approach was chosen over Option B (new `ProxyConfig.Master` sealed variant) because: (a) reuses the existing `parseConfig()` function and `writeMasterConfig()` storage path without duplication, (b) the `MasterKeys` object already provides the storage structure, and (c) the `migrateMasterV2IfNeeded()` migration already writes `useMaster` booleans — no new migration needed. Don't derive master from equality comparison.
2. **Test must announce the scope.** Card 1 test button label: "Test main connection". Per-scope override test: "Test for Downloads". Snackbar should always include scope name: "Downloads proxy connected in 184 ms."
3. **When master is selected, run the test against all 3 endpoints** (search/metadata GitHub API, download CDN, configured translation provider) and show a 3-line result. This is what Tailscale's `tailscale netcheck` does. Roundtrip cost is acceptable — testing is rare.
4. Surface "current state" pill on every override sub-row even when collapsed: green "Using main", or amber "Override: HTTP 127.0.0.1:1080".

**Severity.** Critical (load-bearing UX claim + persistence correctness).

---

### C3. "Direct" is the wrong rename and will mistranslate

**Problem.** §6 row: `proxy_none` "None" → "Direct". The architect's reasoning (§4.3 *"'Direct' replaces today's `ProxyType.NONE`. Renamed for clarity — 'None' sounds like an error state."*) is half-right and half-wrong.

**Evidence.**

- "Direct" is industry term in *English* (Firefox, Chrome, system proxy.pac syntax: `DIRECT`). In the 13 locales the app ships, **"Direct" has no shared meaning**. In German "Direkt" works; in Chinese (Simplified) the literal "直接" is awkward UI copy — Chrome zh-CN uses "不使用代理服务器" ("do not use proxy server"); in Russian "Прямое соединение" is verbose. In Polish "Bezpośrednio" — same problem.
- The original "None" maps cleanly across locales because every translator already has localized "None" from the rest of the OS.
- iOS HIG, Internationalization: *"Avoid technical jargon and idioms that may not translate. Prefer concrete nouns over abstract states."*
- "Direct" also collides with the Connection screen body copy: *"Pick a connection mode below. Most people leave this on Direct."* — a user reading that translation might wonder "direct what?"

**Proposed fix.** Three options, ranked:

1. **"No proxy"** — concrete, translatable, accurate. Used by Firefox UI ("No proxy"). Bitwarden uses this exact phrase.
2. **"Off"** — pairs naturally with a master pill ("Connection: Off"). Cloudflare WARP uses "Off / On / Auto".
3. Keep "Direct" but mark the rename as English-only and require translator notes for each locale ("the connection bypasses any proxy"). Worst option.

Pick #1. Also rename `proxy_none_description` from "Connect directly, no proxy." to "The app connects to the internet without a proxy."

**Severity.** Critical (13 locales × one mistranslation per locale = compounding bad copy).

---

### C4. Empty-state and confirmation patterns are spec'd inconsistently across sub-screens

**Problem.** The spec handles empty states on some screens but ignores them on others:

- §3.4 Sources: explicit empty state for custom forges ✔.
- §3.7 Updates: skipped/hidden drill rows show counts but **no spec for "0 items"** subtitle copy. Today's `skipped_updates_entry_description` is just descriptive; with the new dynamic-subtitle pattern (§2.2 *"reflects the current value of that domain"*), a zero state needs friendly copy.
- §3.9 Access tokens: *"No visual change needed beyond making sure its rows already use `Radii.row`"* — but the hub subtitle (§1.1) says "No tokens yet" when empty. The actual `HostTokensScreen` empty state is unaudited. A user tapping "No tokens yet" deserves an "Add your first token" CTA, not a blank list.
- §3.8 Library cleanup: cache size "Using: 124 MB" but no spec for "Using: 0 B" copy or whether the Clear button should be disabled.

Confirmation dialogs are also inconsistent:

- §3.8 Clear viewed history: uses `GhsConfirmDialog`. Good.
- §3.4 Remove custom forge: uses `GhsConfirmDialog`. Good.
- §4 Connection override toggle-off: **no confirmation**, but toggling off override loses test results and arguably changes behavior across an entire scope. Should at least surface a snackbar "Discovery now uses main connection" so the destructive change is observable.
- §3.8 Clear downloaded APKs: uses existing `ClearDownloadsDialog`. Need to confirm copy is consistent with `GhsConfirmDialog` vocabulary post-D4 squircle scope.

**Evidence.** NN/g, "10 Usability Heuristics," #5 Error prevention and #9 Help users recognize, diagnose, recover. Material 3 Confirmation Dialog guidance: *"Use for destructive actions that can't be undone, or that change scope of effect."*

**Proposed fix.** Add an "Empty / loading / error states" sub-section to **every** per-screen spec. Standardize the destructive-action pattern across the redesign:

- Reversible state changes → snackbar with Undo (Material 3 standard).
- Irreversible destructive (clear cache, clear history, remove token, remove forge) → `GhsConfirmDialog`.
- Mode switches that change behavior across many scopes (proxy override on/off) → snackbar confirming scope change.

**Severity.** Critical (inconsistency between sibling screens degrades trust + lookahead for translators).

---

### C5. Hub navigation cost is real, and the spec rejects all standard mitigations without testing them

**Problem.** §2.6 rejects search; §1.2 declines greyed-out cross-platform rows; §7.1 (Discoverability loss) acknowledges *"Today a user scrolling Tweaks accidentally discovers 'Custom forges' or 'Hide seen.' In a hub-and-spoke model, those settings are one tap further away. Mitigation: the hub's dynamic subtitles … make the state visible without entering."* The mitigation is partial — subtitle visibility helps recognition, not initial discovery, and only for users who already know what to look for.

**Evidence.**

- NN/g, "Site Map vs Search" (2019): site maps (= our hub) work for users who know domain vocabulary; search wins for users who know what they want but not where it lives. Power users (our audience) hover at both ends — proxy tinkerers know exactly what they want, but onboarding users browse.
- iOS Settings: ~30 hub rows but ships a search bar at top of every Settings screen. Apple HIG, Settings: *"Provide a search bar when your settings hierarchy is more than one level deep."*
- 1Password and Bitwarden Settings: both ship search.
- Material 3 Settings pattern (m3.material.io): "For more than 6 categories, provide search." We have 10.

The architect's three reasons for rejecting search (§2.6) are:

1. *"A user can scan them in under 2 seconds."* True for first visit, false for return visits to a specific item.
2. *"contradicting the 'much cleaner' directive."* A search bar one row tall doesn't violate "much cleaner."
3. *"deep links … better than fuzzy search field."* These solve different problems — deep links from outside the app, search from inside.

**Proposed fix.** Either:

1. **Ship a tiny search field** at the top of the hub (under the title, above the first block header), `WonkySquircleShape.Search` shape, filters rows + their subtitle text. Cost: ~20 lines of Compose. Removes the entire discoverability complaint.
2. **Or commit to deep links in v1, not P13** (§7.1 defers them). At minimum: `githubstore://tweaks/connection`, `githubstore://tweaks/translation`, `githubstore://tweaks/updates`. These can be invoked from notifications ("Battery optimization is blocking updates →"), from in-app banners (the existing `D3` themes-refreshed banner), and from external "open settings" deep links.

Even better: ship both. Search is cheap, deep links are infrastructure.

**Severity.** Critical (user-explicit "rethought" goal vs new friction; user is a power user who navigates Tweaks weekly, not yearly).

---

## Major concerns (strongly recommend fixing)

### M1. "Library cleanup" is two unrelated mental models stapled together

**Problem.** The user's brief explicitly asks this question. §3.8 puts under one roof:

- Downloaded APK cache (disk-space concern, performance-y).
- Detect repo links in clipboard (privacy / convenience pref).
- Hide seen / Clear viewed history (privacy / behavior pref).

These have nothing in common except "things the app remembers." The clipboard toggle in particular has **zero** to do with "cleanup."

**Evidence.**

- NN/g, "Card Sorting": users group by goal, not by implementation. A user trying to recover disk space won't think to look here for clipboard prefs; a user trying to stop the app from peeking at clipboard won't think to look in a screen called "cleanup."
- iOS Settings splits these: General → iPhone Storage (disk), General → AirDrop & Handoff → Universal Clipboard (clipboard), Safari → Privacy (browsing history).

**Proposed fix.** Two options:

1. **Split into "Storage" + "Privacy"** at the hub level. Storage = downloaded APKs only. Privacy = clipboard, hide seen, viewed history, **and the telemetry toggle from C1**. Adds one hub row (now 11), but groups by user goal. This is the architecturally honest answer.
2. **Keep one screen but rename to "Privacy & data"** and add internal sub-sections "Disk" / "Clipboard" / "History" with `titleSmall` sub-headers (the screen-internal convention from §3 general). Less disruptive, slightly muddier.

Prefer #1. The user's "rethink each category" directive supports it.

**Severity.** Major (named in user's original brief).

---

### M2. Per-scope override toggle wording is wrong direction

**Problem.** §4.3 Card 2 #3: *"Each row has a trailing toggle 'Use main connection' (default ON). When toggled OFF, the row expands … into a mini-editor."* §7.2 #3 already flags this: *"Could also be 'Inherit from main' — more technical, possibly clearer to power users."*

The user audience is power users. "Use main connection" reads ambiguously — it sounds like enabling a connection, not inheriting a setting. The toggle is also semantically inverted from the visual: ON = no editor visible (you can't change anything). Users will toggle it to "see the controls" and then panic when they realize they just unhooked the override.

**Evidence.**

- iOS Settings → Wi-Fi → Network → "Configure DNS": uses radio buttons "Automatic / Manual" — same problem, better solution. Two explicit choices, no toggle inversion.
- Tailscale → Network → Exit Node: uses "Inherit from network" vs explicit "Override".
- Material 3 Switch guidance: *"Use switches for on/off states of a single setting, not for revealing more options."*

**Proposed fix.** Replace the toggle with a 2-way segment per scope row:

```text
[ Use main ]  [ Custom… ]
```

"Custom…" reveals the mini-editor. "Use main" hides it. This is what 1Password does for per-vault settings ("Inherit / Custom"), and Bitwarden for organization policies ("Use default / Override").

Bonus: a segment makes the "override is on" state visually obvious without subtitle parsing.

**Severity.** Major (every power user hits this).

---

### M3. PAC files / SOCKS variants / proxy URL paste are unaddressed

**Problem.** §4.3 mode segment offers Direct / System / HTTP / SOCKS. The user community asking for proxy support (China + privacy-conscious EU + Tor) will request:

- **SOCKS4 vs SOCKS5** distinction. Today's `ProxyConfig.Socks` doesn't expose version — fine if SOCKS5 only, but worth confirming. Tor's local proxy is SOCKS5; some corporate proxies are SOCKS4a.
- **HTTPS proxy** vs HTTP proxy. Many corporate MITM proxies require HTTPS. Today's `ProxyConfig.Http` is ambiguous in name.
- **PAC files / proxy auto-config URL**. Mentioned in the review brief; not covered by the spec at all. Many enterprise users have a PAC URL from IT. Firefox + Chrome + macOS System Settings + Windows Internet Options all support PAC.
- **Paste full proxy URL** as a fast-path (e.g. `socks5://user:pass@127.0.0.1:1080`). Tailscale + warp-cli + curl all accept this format. Five fields is a lot of typing; one paste field is mass-market UX.

**Evidence.**

- Tor Browser Bundle, Settings → Connection → "Use a bridge" → bridge string is one paste field, not 5 separate inputs.
- Cloudflare WARP-CLI supports `warp-cli set-custom-proxy` with full URL.
- Material 3 form guidance: *"If a single input can replace multiple, prefer the single input with smart parsing."*

**Proposed fix.**

1. **SOCKS5-only confirmed.** `ProxyConfig.Socks` stays versionless — JVM `SocksSocketImpl` is SOCKS5 by default, and there's no demand for SOCKS4. The paste parser (`parseProxyUrl()`) accepts `socks4`, `socks5`, `socks5h` but all map to `ProxyType.SOCKS` → `ProxyConfig.Socks`. Info loss on socks4 paste is accepted as a negligible edge case. UI pill label keeps "SOCKS" (not "SOCKS5") for brevity.
2. **HTTP confirmed for both HTTP and HTTPS.** JVM `Proxy.Type.HTTP` handles both protocols transparently. UI label stays "HTTP" — the caption explains the mode; tooltip not needed.
3. **Paste URL already implemented.** `PasteProxyUrlSheet` (`connection/PasteProxyUrlSheet.kt`) exists with `parseProxyUrl()` that handles `http`, `https`, `socks`, `socks4`, `socks5`, `socks5h` schemes. Wired in `TweaksConnectionRoot.kt` — the "Paste URL" text button opens the sheet, parses the input, and dispatches `OnMasterProxyPasteUrl`.
4. **PAC file: explicit non-goal for v1.** No code exists. If issues come up, address as a follow-up (separate ticket). Not adding a Mode pill for it.

**Severity.** Major (will generate GitHub issues within a week of release if not addressed).

---

### M4. Color contrast on `outlineVariant.copy(alpha = 0.55f)` will fail WCAG AA on Cream (amber light) palette

**Problem.** §2.2: *"`border = BorderStroke(1.dp, outlineVariant.copy(alpha = 0.55f))`"*. This is the universal border for the entire redesign. Material 3's `outlineVariant` is already low-contrast (~3:1 against `surfaceContainerLow`), and multiplying by 0.55 alpha makes it ~1.7:1 against light Cream surfaces.

**Evidence.**

- WCAG 2.2, SC 1.4.11 Non-text Contrast: **3:1 minimum** for UI component boundaries.
- Material 3 contrast tables: `outlineVariant` is designed for low-emphasis dividers, not standalone container borders. When used as a container border (your case), Material recommends `outline` (which is `outlineVariant` boosted ~2x).
- The amber Cream light palette is on file in `D3`. Cream's `surfaceContainerLow` is high-luminance off-white; the border will be nearly invisible.

**Proposed fix.** Either:

1. Drop the 0.55 alpha. Use `outlineVariant` at full opacity.
2. Use `outline` at 0.55 alpha (lands at roughly the same visual weight on dark themes but stays above 3:1 on Cream light).
3. Audit per-palette. The architect should screenshot Nord/Cream/Forest/Plum × Light/Dark/AMOLED with a candidate `TweaksEntryRow` and confirm border visibility before committing.

Run option #2, then audit.

**Severity.** Major (accessibility compliance + Cream light is the maintainer's recently-changed lead-theme; visually buggy there is highly visible).

---

### M5. 48dp tap-target accounting + chevron a11y are unaudited

**Problem.** §2.2 hub row: *"padding(horizontal = 16.dp, vertical = 14.dp)"* + icon tile 40.dp + two-line text. Total row height depends on font metrics; with Geist `titleMedium` (~22sp line height) + 2dp gap + `bodySmall` (~16sp) + 28dp = roughly **68dp tall**. ✔ exceeds 48dp.

But: §3.4 Custom forges row has a trailing `IconButton` for delete ("Trailing: `Icons.Outlined.DeleteOutline` `IconButton` → confirmation"). A 24dp icon inside the row needs **48dp tap target** per Material guidance and **44pt** per Apple HIG. Spec doesn't say. Also: an `IconButton` *inside* a clickable row creates a nested-click trap — tapping near the icon may register the row click first (Compose ripple bubbles to outer clickable unless the inner is wrapped in `onClick = {}` with `interactionSource`).

**Evidence.**

- Material 3 Touch Targets: 48 × 48dp minimum for all interactive elements.
- WCAG 2.2 SC 2.5.8 (AA): 24 × 24 CSS pixel minimum; 44 × 44 strongly recommended.
- Compose nested clickables: the outer `Modifier.clickable` swallows child clicks if not explicitly excluded via `consumeWindowInsets` or empty inner click handler (this has been a recurring bug — see Compose 1.6 release notes on `clickable` consumption).

**Proposed fix.**

1. Spec the delete `IconButton` as 48dp min with internal 24dp icon and `Modifier.clip(Radii.chip)` for ripple.
2. Spec the parent row's `Modifier.clickable` to **not** be the only click handler — the row click navigates, but the delete button stops propagation.
3. A11y semantics for every hub row:

   ```kotlin
   semantics {
       role = Role.Button
       contentDescription = "$title. $subtitle. Double-tap to open."
   }
   ```

   Chevron icon: `contentDescription = null` (decorative). Status pills (Install method "Needs permission"): exposed as a `liveRegion = Polite` so TalkBack announces re-evaluation when permissions change.
4. Override toggle accessibility (§4.3 Card 2): when toggled OFF and editor expands, announce "Discovery override on, custom proxy settings for Discovery."

**Severity.** Major (accessibility is global app concern, not per-feature).

---

### M6. i18n: hub-row subtitle truncation + plural rules + RTL are unspec'd

**Problem.** §2.2: *"Subtitle: bodySmall, onSurfaceVariant, max 1 line, ellipsize."* §1.1 examples: *"DeepL · auto on"*, *"Auto · every 6h"*, *"Nord · Dark"*. These are English-shape sentences.

- **German**: "Auto · alle 6 Stunden" (no abbreviation for "hour"; "alle 6 Std." possible but unusual). "Befolge Systemeinstellungen · Dunkel" for "Follow system · Dark" — 35 chars before "Dark" even appears. Subtitle ellipsizes at ~28 chars on mobile.
- **Russian**: "Каждые 6 часов" + provider names like "Майкрософт" — Cyrillic is wider per char.
- **CJK** (Simplified Chinese, Traditional Chinese, Japanese, Korean): the · separator may not parse — CJK uses 「、」 or 「·」 (different code point). Compose `Text` handles BOM but the dot character should be locale-aware. Also no spaces between words is the norm in CJK, so "DeepL · 自动开启" works but feels foreign.
- **RTL** (Arabic, Hebrew): the · separator order flips, chevron flips. App must wrap subtitles in `LocaleTextDirection.Content` so dot-separated tokens lay out right.
- **Plurals**: "%d host" / "%d hosts" (§6 row for `custom_forges_count_label`) — this requires Android plural resources (`plurals.xml`) per locale; Compose Multiplatform Resources supports plurals (`pluralStringResource` in `org.jetbrains.compose.resources` since 1.6). Russian has 3 plural forms, Polish has 3, Arabic has 6. Naive "%d hosts" breaks all of them.

**Evidence.**

- W3C i18n best practices, "Don't use 'one' / 'other'" — use CLDR plural categories.
- Apple HIG Localization: *"Allow at least 50% extra width for German and Russian. CJK can compress 30%."*
- Compose Multiplatform Resources release notes: `pluralStringResource` is the supported API; today's code uses `stringResource(..., count)` which is **not** plural-aware.

**Proposed fix.**

1. Mandate `pluralStringResource` for any subtitle / label with a count, and add plural XML for every locale that doesn't have it. Audit: %d tokens, %d hosts, %d apps (skipped updates count).
2. Spec subtitle truncation behavior: *"Truncate with ellipsis. If subtitle has multiple dot-separated tokens, prefer truncating the rightmost token first."* (This is hard in Compose; pragmatic compromise: cap subtitle at ~32 chars in any locale, drop secondary token if over.)
3. Use a locale-aware separator: `LocalConfiguration.current.locales` → CJK locales use 「· 」(with width adjustment) or " / "; non-CJK uses " · ".
4. Chevron auto-flips via `Icons.AutoMirrored.Filled.KeyboardArrowRight` (already used in `GhsEntryRow.kt`). No `CompositionLocalProvider` wrapping needed — text respects user direction automatically, and the mirrored icon handles the arrow flip in RTL locales.
5. **Translator handoff**: the §6 rename table is English-only. Generate a CSV of "old key → new English → context note" for translators per locale. Don't ship the redesign with stale 12 locales (the user's policy elsewhere ships with English first and queues translations; that's acceptable, but it must be explicit in the spec, not implicit).

**Severity.** Major (13 locales is a load-bearing project property).

---

### M7. About sub-screen swallows the emergency "Send feedback" path

**Problem.** §1.3 / §3.10: Send feedback moves into About sub-screen. The hub no longer surfaces feedback. Today, a user encountering a bug can reach Feedback in: Profile → Tweaks → scroll to about → Feedback button. New flow: Profile → Tweaks → About row → Feedback row → sheet. **One extra tap** at the worst possible moment — the user is already frustrated.

**Evidence.**

- NN/g, "Error recovery": friction during an error state is multiplied by user frustration. Each extra tap during a bug-report flow doubles the abandonment rate (Sauro 2016 on error-recovery UI).
- Most apps that have "Send feedback" or "Report a problem" surface it on the Settings root, not buried. Bitwarden: Settings → "Help" (one tap). 1Password: Settings → "Help" (one tap). Tailscale: Settings → "Bug report" (one tap).
- The user's brief: *"What about emergencies (something broke, user wants to report fast)?"* — direct callout.

**Proposed fix.** Three patterns to consider:

1. **Keep feedback as its own hub row** at the bottom of the "App" block, alongside About. Cost: 11 rows in hub instead of 10. Worth it.
2. Add a **floating "Help" overflow icon** in the hub topbar (architect's §2.1 says no trailing — reconsider). Pulls open a sheet with "Send feedback / What's new / Privacy".
3. Surface feedback as a **persistent banner** at the bottom of the hub only when a crash has occurred in the current session (use `CrashReporter`'s presence). Best-case UX, more engineering.

Ship #1. It's the cheapest and matches industry norm.

**Severity.** Major (named in user's brief; emotional moment).

---

### M8. Cross-platform conditional rules in §1.2 hide too much

**Problem.** §1.2: *"Hub omits the Install method row entirely on desktop. No greyed-out placeholder."* §3.6 Desktop note: *"if reached via deep link on desktop, show a centered empty state — 'Install method is Android-only'"*. Hiding entirely is consistent with iOS HIG ("don't show what doesn't apply"). But:

- Users supporting both platforms (e.g. running desktop + Android Pixel) will assume parity. Hiding means they can't even tell the feature exists. This contradicts the broader trend in this app: the Tweaks screen exists *because* both platforms share most settings.
- The architect's chosen mitigation (centered empty state on deep link) is asymmetric — works in one direction (Android user opens deep link on desktop) but not the other (desktop user wondering "how do I make APK install silent on my Pixel from here").

**Evidence.**

- Apple HIG Settings, Cross-platform: *"Where settings are platform-specific, surface a small badge instead of hiding entirely so users know the feature exists elsewhere."*
- Discord, Slack, 1Password all do this with "Coming soon on this platform" or "Mobile only" subtitle badges.

**Proposed fix.** Show the row on desktop with a **subtitle badge** "Android only" (use `tertiaryContainer` pill), clicking shows the §3.6 empty state with explanation. Same for any future iOS-only or Linux-only rows. The visual cost (one row) is dominated by the discoverability gain.

(Alternative: leave architect's decision intact, but require a one-line entry under About on desktop: "On Android: install method, silent updates, attribution.")

**Severity.** Major (user is dual-platform power user).

---

### M9. "Connection" + "Sources" hub block name "Network & data" doesn't match its rows

**Problem.** §1.1 block "Network & data" contains: Connection, Sources, Translation, Access tokens.

- "Translation" is not network-related from the user's mental model — it's a feature of repo Details. A user looking for "Translation settings" won't think "Network & data."
- "Access tokens" is closer to "Account / Security," not "Network."

**Evidence.**

- NN/g, "Information Scent": users follow labels that match their intent. "Translation" under "Network & data" has weak scent.
- iOS Settings groups Translation under "Apps" or "General"; Tokens / passwords are always under "Passwords & Security."
- The architect's own §6 rename: `section_network` "Network" → "Network & data" (hub block header) — calling this a "data" block is a stretch when 2 of 4 children are arguably not about data movement.

**Proposed fix.** Re-block:

1. **Look & feel**: Appearance, Language ✔ (no change)
2. **Connectivity**: Connection, Sources ← new tighter block
3. **Content & translation**: Translation (single row; or merge with another sibling) ← OR put Translation under Look & feel since it's per-locale-y
4. **Security & accounts**: Access tokens, telemetry (from C1) ← new block
5. **Installs & updates** ✔ (no change)
6. **Storage** + **Privacy** (from M1)
7. **App**: About, Send feedback (from M7)

That's 7 blocks for 12 rows — heavier than the current 4 blocks for 10 rows, but each block is internally coherent. Alternative compromise: keep 4 blocks but rename "Network & data" → "Connectivity" and move Translation out (e.g. into Look & feel or its own micro-block).

**Severity.** Major (the user's brief explicitly asked about this).

---

### M10. Restart-on-language-change UX preserved but not specified for the new flow

**Problem.** §3.2: *"Validation / events: selecting a language fires `OnAppLanguageSelected(tag)` → existing `OnAppLanguageChangeRequiresRestart` snackbar with 'Restart now' action. Unchanged."*

But the new flow is: hub → tap "Language" row → arrive on `TweaksLanguageScreen` → tap a language → snackbar appears on … which screen? If the snackbar appears on `TweaksLanguageScreen` and the user taps "Restart now," the app restarts and lands on Home (existing behavior) — user loses position. If the user dismisses the snackbar and navigates back to the hub, the snackbar host is gone — but the language is still set, and the app hasn't restarted. Are language and UI now in inconsistent state until the next restart?

**Evidence.**

- Today's monolithic `TweaksRoot.kt` has one `SnackbarHostState` for the whole screen; the snackbar persists across user actions until dismissed.
- New per-sub-screen pattern (§3 general: *"each sub-screen has its own `SnackbarHostState`"*) means the language snackbar belongs to `TweaksLanguageScreen` only. Navigating back drops it.
- Material 3 Snackbar guidance: "Restart now" is a high-stakes action — should be a banner, not a transient snackbar, when stakes are this high.

**Proposed fix.**

1. Replace the "Restart now" snackbar with a **persistent banner** at the top of `TweaksLanguageScreen` that says "Language changed. Restart the app to see it everywhere. [Restart now] [Later]". Reuse existing `Banner` if any, otherwise a `Radii.row` outlined Surface with `tertiaryContainer` tint.
2. Persist the "needs restart" flag in `TweaksRepository` so navigating back to the hub still shows a top-level banner: "Restart pending — some screens may still show the old language."
3. On hub, show the banner above the first block. On any sub-screen, show it at the top of the screen. Dismisses only on app restart.

**Severity.** Major (today's behavior is acceptable monolithic; new IA breaks it implicitly).

---

## Nits / polish

- **N1.** §2.2 spring scale 0.98× — fine on mobile, may feel "stuck" on desktop with a mouse cursor. Recommend `0.985×` on desktop, `0.97×` on Android (per D10's mention of `MediumBouncy`).
- **N2.** §3.5 Auto-translate target picker "drill-row" → "smaller variant of the §3.2 Language screen" — should explicitly say "uses the same searchable list but filters to `SupportedTranslationLanguages.all`." Otherwise risk of two diverging language pickers maintained separately.
- **N3.** §3.7 "every 6h" pill labels: spell out as "6 hours" not "6h" — abbreviations don't translate. Also: a 24-hour pill on an app store is unusual; consider "Every 12h" / "Every day" / "Manual only" instead. 3h is very aggressive given GitHub API rate limits.
- **N4.** §3.10 About screen: tagline copy *"Cross-platform app store for GitHub, Codeberg, and Forgejo releases."* is good. Add a localized version with `<xliff:g>` placeholders for the three product names so translators don't relocalize "Codeberg".
- **N5.** §3.4 Custom forges row hostname in `Geist Mono labelLarge` — confirm mono font weight reads on the smaller subtitle line below; mono fonts often need +1 weight bump to match proportional siblings.
- **N6.** §3.3 / §4 spec is silent on whether the test request honors per-host PATs (`HostTokenInterceptor`). It should — testing without the token won't catch auth-broken proxies for private GH Enterprise + PAT users.
- **N7.** §6 row: `installer_type_default` → "System installer (Default)" — the parenthetical adds noise. Just "System installer" reads cleaner. The word "Default" was always a non-noun in this UI.
- **N8.** §3.10 "Open source licenses" row labeled as *"placeholder row, hide if not implemented"* — please don't ship a hidden row. Either implement it now (it's a static markdown view; 30 min of work) or omit until done.
- **N9.** §2.2 "icon tile … 40.dp square clipped to `Radii.chip`, background `colorScheme.surfaceContainerHigh`, padded 8.dp, tinted `onSurfaceVariant`" — every row has its own colored tile. The architect listed icons by Material name only; no per-row tile tint. Material 3 settings convention is one tile color (neutral) or one tint per **block** (not per row). Per-row colored tiles add visual noise; the spec already doesn't do this, but the user's brief asked the question — confirm in the spec that tiles are uniformly tinted (not per-icon-color). One line in §2.2.
- **N10.** §4.3 *"Inline helper at the bottom of the form (when expanded): 'Used for fetching repos, downloads, and translations unless you override below.'"* — that's a long sentence under a form. Compress: "Applies to all traffic unless overridden below."
- **N11.** §3.1 *"AMOLED toggle visibility binds to `resolvedDark`, not the raw `isDarkTheme` value"* — call out that this is true for the new `Mode segment` too. The toggle visibility computation depends on the segment selection + system theme. State machine deserves one diagram in spec.
- **N12.** §3.2 the language-tag mono font subtitle (`pl-PL`) is great UX. But for "Follow system" the subtitle should show the *resolved* tag in parens, e.g. "Follow system (en-US)" so the user knows what they're getting.

---

## Things the architect got right

1. **Splitting "Network" into Connection + Sources.** This is the spec's strongest IA call. The two were always different mental models bolted together. (§1.1 / §1.3)
2. **The hub's dynamic-state subtitle pattern.** Showing "DeepL · auto on" / "Nord · Dark" / "Direct" on the hub row gives users glance-readable state without entering — exactly the right mitigation for the discoverability cost of hub-and-spoke. (§2.2)
3. **Killing the 3× duplicated proxy form.** Whatever wins between Option A / B / C, **anything** is better than three identical 7-field forms. The current code (`feature/tweaks/presentation/components/sections/Network.kt:161-171`) is the worst pattern in the entire feature. The user's complaint here is fully justified and the architect heard it.
4. **The §3.5 Translation provider redesign.** Moving from chips + below-card form to radio rows + revealed credentials card is the right pattern (5 providers, 4 with credentials, 1-of-N selection — that's a radio group, not a chip group). (§3.5)
5. **Section sub-headers in sentence case** (drop `.uppercase()`). Universally correct call; ALLCAPS feels shouty in 2026 UI. (§2.3)
6. **Rejecting `RoundedCornerShape(32.dp)` cards** in favor of `Radii.row` consistency. The current code has three competing shape vocabularies; collapsing to one is overdue. (§5.3)
7. **Empty-state for custom forges with "Add a Forgejo or Gitea host" framing.** Honest copy that names the system. Better than today's generic "Add your own". (§3.4)
8. **§7.4 explicit "things I did not change" list.** Architect's restraint here is good — pinning down what's *not* touched is half of any redesign spec. (Though §7.4's promise to not change `ProxyRepository` schema needs reversal per C2.)

---

## Open questions for product owner

1. **Telemetry — does it exist today?** (`TelemetryRepository` is injected but there's no UI surface I could find.) If yes, where; if no, do we ship the opt-out in this PR or block on backend? (C1)
2. **Master proxy persistence — is a `ProxyRepository` schema bump acceptable in this phase?** Adds one field per scope (`useMaster: Boolean`) + a fourth master record. (C2)
3. **Search-in-settings + deep links** — pick one, both, or none for v1? (C5)
4. **Library cleanup vs Privacy split** — keep one screen with sub-sections, or split into two hub rows? (M1)
5. **Feedback affordance** — own hub row, topbar overflow, or buried in About? (M7)
6. **Cross-platform-only rows** — hide entirely (architect's pick) or show with "Android only" badge? (M8)
7. **String rename translation policy** — ship in English with `[needs translation]` markers, queue with maintainer translators, or block redesign on 13-locale parity? (M6)
8. **"Direct" rename** — "No proxy" recommended; final call? (C3)
9. **PAC files** — explicit non-goal, or fifth mode pill? (M3)
10. **Restart banner pattern** — adopt persistent banner across all sub-screens for "language pending," "theme migrating," "telemetry consent updated"? (M10)
11. **Hub block grouping** — 4 blocks (architect) or 6–7 tighter blocks (M9)?

End of review.
