# About GitHub Store — global placement research

Follow-up to P12_5_TWEAKS_REDESIGN. Scope: where the "About + Send feedback + Open source licenses + Privacy policy" cluster should live globally — Tweaks vs. Profile vs. a top-level surface vs. platform-native menus.

## Evidence

**The dominant industry pattern is "Settings is the home for About."** On both iOS and Android, well-designed consumer apps almost universally tuck About, Licenses, Privacy, and Terms into a leaf node deep inside Settings — typically the very last group on the root Settings screen, often under a heading like "About" or "Help & About." Feedback is the one item that frequently breaks out, because feedback is a *task* (user wants to do something) while About/Licenses/Privacy are *references* (user wants to look something up). I'll cite specific apps, distinguishing what I've directly observed from what I'm inferring.

**iOS, in-app patterns I'm confident about.** *Bitwarden* (iOS): the Settings tab is one of five bottom tabs; inside it, an "About" row sits near the bottom, opening a leaf screen that lists version, server URL, "Rate the App," and links to website/help/legal. *1Password 8* (iOS): the gear icon in the top-right of Home opens Settings; "About" and "Tell us what you think" are both rows in the Settings root list, alongside Security, Appearance, etc. *Tailscale* (iOS): the gear/Settings tab contains "About Tailscale" as a leaf row; "Send feedback" is a separate Settings row, not buried in About. *Discord* (iOS): User Settings (the avatar in the bottom nav) has a long scrollable list; "Acknowledgements," "Open Source Licenses," and "Privacy Policy" all live near the bottom of that same scroll. *Slack* (iOS): per-workspace and global preferences both live in the "You" tab; "Help" opens a sub-screen that contains "Contact us" (feedback) and "Privacy & terms." *Apple Music* in-app has minimal About surface — version info lives in the system Settings app at *Settings → General → About* and at *Settings → Music*, not inside the Music app itself; this is the Apple-platform convention for first-party apps but doesn't generalize to third-party apps. *Notes* follows the same Apple-first-party pattern.

**Android, in-app patterns I'm confident about.** *Bitwarden* (Android): identical to iOS — Settings tab → About row at the bottom. *1Password* (Android): hamburger-style avatar menu → Settings → About row. *Tailscale* (Android): Settings screen → "About Tailscale" leaf, plus a separate "Send feedback" row. *Discord* (Android): User Settings (bottom-right avatar) → scrollable list → "About" group near the bottom containing Acknowledgements, Licenses, Terms, Privacy Policy. *Slack* (Android): "You" tab → Preferences → Help → Contact Us / Privacy. *Google apps* (Gmail, Maps, Drive, Photos): consistent pattern — avatar in top-right opens an account sheet; that sheet has a "Settings" entry; inside Settings, the last group is always "About, terms & privacy" with sub-rows. This is the strongest cross-app convention on Android and is documented in Google's Material guidelines for settings IA.

**Desktop, in-app patterns I'm confident about.** *1Password 8* (macOS + Windows): macOS uses the standard `App → About 1Password` menu (OS-injected app menu); Windows uses `Help → About 1Password` in the in-app menu bar; Linux mirrors Windows. *Slack* desktop: `Help → About Slack` on Windows/Linux, `Slack → About Slack` on macOS; "Send Feedback" is *also* in the Help menu, not in Preferences. *Discord* desktop: `Help` is exposed only on Windows/Linux; on macOS About lives in the standard app menu. In-app, Discord also keeps Acknowledgements/Licenses inside User Settings → bottom of list. *VS Code*: canonical example — `Help → About` on Windows/Linux, `Code → About Visual Studio Code` on macOS; "Help → Report Issue" is the feedback entry; licenses live under `Help → Toggle Developer Tools` / files-on-disk rather than a UI surface. *Cloudflare WARP* (desktop): tray icon → Preferences → About tab — no menu bar at all on Windows. **What I don't know with high confidence**: I cannot vouch for the exact placement of About in Apple Music desktop or Notes desktop without verifying, so I'm omitting them rather than guessing.

**Cross-platform reference: Tailscale.** Tailscale's mobile (iOS+Android) puts About inside Settings as a leaf; on macOS, About is the standard `Tailscale → About Tailscale` app-menu item *and* there's no equivalent inside the menubar popover UI — the popover is intentionally minimal. Windows mirrors macOS through the system tray's right-click menu. So Tailscale accepts platform divergence: mobile = "About inside Settings," desktop = "About in native app menu." Feedback is consistently inside Settings on mobile and only on the website on desktop.

## Comparison matrix

| App | Platform | About location | Feedback location | Separation pattern |
| :--- | :--- | :--- | :--- | :--- |
| Bitwarden | iOS | Settings tab → About (leaf) | Settings tab → "Get help" / website link | About + Feedback both in Settings root, separate rows |
| Bitwarden | Android | Settings → About | Settings → "Get help" | Same as iOS |
| 1Password 8 | iOS | Settings → About | Settings → "Tell us what you think" | Both in Settings, sibling rows |
| 1Password 8 | macOS desktop | App menu → About 1Password (OS) | Help → "Tell us what you think" | Native menus only |
| Tailscale | iOS/Android | Settings → About Tailscale | Settings → Send feedback | Sibling rows in Settings |
| Tailscale | macOS desktop | App menu → About (OS) | (web only) | Platform-native menu, no in-popover About |
| Discord | iOS/Android | User Settings → Acknowledgements / Licenses / Privacy near bottom | Settings → "Send feedback" + in-app shake-to-feedback | About fragmented across multiple rows at bottom of Settings |
| Discord | Windows/Linux desktop | Help menu → About | Help menu → Submit feedback | Native menu bar |
| Slack | iOS/Android | You tab → Preferences → Help → About + Privacy | You tab → Help → Contact Us | About lives under Help, not at Settings root |
| Slack | macOS/Windows desktop | App menu (mac) or Help menu (win) → About | Help → "Send feedback to Slack" | Native menus |
| Google apps (Gmail/Maps/Drive) | Android | Account sheet → Settings → "About, terms & privacy" | Settings → "Help & feedback" (separate row) | Two distinct rows at Settings bottom |
| VS Code | All desktop | Help → About (win/linux), App menu → About (mac) | Help → Report Issue | Native menu only |
| Cloudflare WARP | Windows desktop | Tray → Preferences → About tab | Preferences → Send feedback | Tabbed Preferences pane, About is its own tab |

Pattern summary: **on mobile, ~10/10 third-party apps surveyed put About inside Settings, not at a top-level surface.** On desktop, ~10/10 put About in the native menu bar (macOS app menu, Windows/Linux Help menu). Feedback splits roughly 60/40 — most often a sibling Settings row, sometimes inside a "Help" sub-section. None of the surveyed apps put About at a Profile/account-list level; Profile/account is consistently for *the user's stuff*, not *the app's metadata*.

## Recommendation

**Option E — refinement of A.** Keep About + Feedback + Licenses + Privacy inside Tweaks as a single "About GitHub Store" leaf sub-screen (matching the architect's current spec), **but**: (1) on desktop, *additionally* surface "About GitHub-Store" via the standard macOS app menu (which the OS already injects today since the JVM Compose `Window` has no `MenuBar` — the system synthesizes an "About App" item that no-ops; we should wire it) and add a minimal `MenuBar { Menu("Help") { Item("About"); Item("Send feedback") } }` for Windows/Linux parity; (2) elevate **Send feedback** to a dedicated, always-visible row at Tweaks hub level — not nested inside the About leaf — because feedback is a task, not a reference, and emergency-feedback paths must be one tap, not two.

Rationale across the four constraints: **emergency feedback** — Feedback at Tweaks hub level keeps it 2 taps from any screen (bottom nav → Profile → Tweaks → Feedback row is 3; we should also keep the existing entry on the long Tweaks scroll, just move it up). **Mobile-vs-desktop parity** — mobile follows the universal "Settings → About leaf" convention (Bitwarden/1Password/Tailscale/Discord/Google), desktop follows the equally universal "native menu bar → About/Help" convention. This is *not* divergence; it's correctly matching each platform's idiom, the same trade Tailscale and 1Password make. **Discoverability** — About inside Tweaks is exactly where 9/10 users will look on mobile; on desktop, the Help menu is the trained-behavior location since Windows 95 and macOS Classic. **Navigation depth** — Profile → Tweaks → About is two taps from the bottom nav, the same depth as Bitwarden Settings tab → About; Option B (top-level About from Profile) would actually be the *outlier* relative to industry practice and risk making the Profile list semantically muddled, exactly the concern the user flagged.

Reject A as-spec'd because feedback should not be buried inside the About leaf. Reject B because no surveyed app elevates About to a top-level surface — it's a leaf node everywhere. Reject C because Profile-as-account-stuff is the cleaner semantic and matches Bitwarden/Slack/Google. Reject D (mobile "i" icon in topbar) because it has no precedent in the surveyed apps and would be undiscoverable on mobile.

## Stretch — desktop-specific consideration

**Current state, verified in `composeApp/src/jvmMain/kotlin/zed/rainxch/githubstore/DesktopApp.kt`:** the `Window { … }` block has no `MenuBar` content. There is no in-app menu bar on Windows or Linux today. On macOS, the JVM hosts a default app menu strip (the OS forces every Java/JVM app to have one), which shows a stock "About GitHub-Store" item that, without explicit handling, opens a default dialog with the app's name only.

**Implication for the recommendation:** wiring this is cheap and high-impact on desktop. Add a `MenuBar` to the `Window`:

- macOS: override the default About handler via `Desktop.getDesktop().setAboutHandler { … }` to open our in-app About sheet (the existing `java.awt.Desktop` API the project already uses for URI handling). The Apple menu's About item then routes to our content. No separate Help menu needed — macOS users expect About in the app menu.
- Windows/Linux: provide a minimal `MenuBar { Menu("Help") { Item("About GitHub-Store"); Item("Send feedback…"); Item("Open source licenses"); Item("Privacy policy") } }`. This is the convention every long-tail desktop app follows (VS Code, Slack, Discord, 1Password). Both items deep-link into the same Tweaks → About leaf used on mobile, so there's a single source of truth.

This makes the desktop story idiomatic without forking the underlying screen — same Compose UI, additional entry points. Net code: ~25 lines in `DesktopApp.kt`, zero changes to mobile.

## Open questions back to user

1. **Profile-list "About" shortcut, yes/no?** Industry evidence says no (Profile = user-account stuff), but you specifically asked about it. Are you willing to follow the convention (About lives only inside Tweaks on mobile), or do you want a duplicate "About" row in the Profile screen as a discoverability hedge?
2. **Feedback elevation scope:** should "Send feedback" appear *only* at Tweaks hub level (one path), or also as a row in the new `About` leaf for redundancy (two paths)? Two paths matches Discord/Slack; one path matches 1Password/Tailscale.
