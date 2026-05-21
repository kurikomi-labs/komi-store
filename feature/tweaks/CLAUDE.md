# Tweaks Feature

Single home for every app-level setting. Update prefs, installer choice (Default / Shizuku / Dhizuku / Root), telemetry, translation, mirror, feedback, hidden + skipped list managers. Absorbs settings half of former `feature/profile/`.

## Structure

```
feature/tweaks/presentation/
├── TweaksViewModel / State / Action / Event / Root, RestartApp
├── components/
│   ├── sections/  Account, Appearance, Installation, Language, Network, Others, Translation, SettingsSection
│   └── ToggleSettingCard, ClearDownloadsDialog, SectionText, CustomForgesDialog
├── feedback/      # FeedbackViewModel + sheet
├── hidden/        # HiddenRepositoriesRoot — Tweaks → Updates → Hidden repos
├── skipped/       # SkippedUpdatesRoot — Tweaks → Updates → Skipped updates
├── hosttokens/    # HostTokensRoot/ViewModel — Tweaks → Access Tokens (per-host PATs)
├── mirror/        # MirrorPickerRoot — Tweaks → Network → Mirror picker
└── model/         # ProxyScopeFormState, ProxyType
```

## Sub-screen VMs (registered in `composeApp/.../app/di/ViewModelsModule.kt`)

`SkippedUpdatesViewModel` (unskip via `InstalledAppsRepository.setSkippedReleaseTag`), `HiddenRepositoriesViewModel` (unhide / unhide-all via `HiddenReposRepository`), `HostTokensViewModel` (CRUD on `HostTokenRepository`), `FeedbackViewModel`, `AutoSuggestMirrorViewModel`, `MirrorPickerViewModel`.

## Navigation

`TweaksScreen`, `SkippedUpdatesScreen`, `HiddenRepositoriesScreen`, `HostTokensScreen`, `MirrorPickerScreen`.

## Notes

- TweaksViewModel injects: `TweaksRepository`, `ThemesRepository`, `ProxyRepository`, `InstalledAppsRepository`, `ProfileRepository`, `InstallerStatusProvider`, `TelemetryRepository`, `Platform`, `BatteryOptimizationManager` (Android), `GitHubStoreLogger`.
- One-shot coachmark flags in `TweaksRepository` (`apk_inspect_coachmark_shown`, `channel_chip_coachmark_shown`). Once persisted true, never re-shown.
- `include_pre_releases` pref read by `InstallationManagerImpl` to seed new install's `InstalledApp.includePreReleases`. Existing rows keep per-app value.
- `show_all_platforms` pref drives cross-platform asset section in Details.
- Mirror picker gated on user locale (suggested in throttled regions).
- **Appearance → Content width (desktop):** `ContentWidthCard` picks `ContentWidth` (`COMPACT` / `WIDE` / `EXTRA_WIDE`); written via `OnContentWidthSelected` action; consumed by `LocalContentWidth` in Details.
- **Network → Custom forges:** `CustomForgesDialog` (opened via `OnOpenCustomForgesDialog`) edits `state.customForgeHosts` (persisted via `TweaksRepository`). Hosts surface in `RepositoryUrlParser` + power `SearchSourceUi.Custom` chips + register Ktor clients in `ForgejoClientRegistry`.
- **Access Tokens (`HostTokensScreen`):** sub-screen lists / adds / edits / deletes per-host PATs in `HostTokenRepository` (AES-256-GCM via KSafe). Token surfaces via `HostTokenInterceptor` on matched host (`api.github.com → github.com` mapped through `HostNames.apiHostToTokenHost`).
- **Translation section:** `AutoTranslateCard` toggles `OnAutoTranslateEnabledToggle` + picks target via `OnAutoTranslateTargetSelected`. Per-provider credential forms — `LibreTranslateCredentialsForm` (base URL + API key, defaults to `translate.disroot.org` mirror when blank), `MicrosoftCredentialsForm` (key + region; Azure portal deep-link); DeepL key field auto-routes `:fx` suffix to free endpoint. All credentials persisted via `TweaksRepository` (KSafe-encrypted), visibility toggles per field.
- `RestartApp.kt` applies locale change (persist tag, restart MainActivity / DesktopApp).
