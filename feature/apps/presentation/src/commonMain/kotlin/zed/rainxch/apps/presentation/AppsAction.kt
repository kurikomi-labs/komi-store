package zed.rainxch.apps.presentation

import zed.rainxch.apps.presentation.model.InstalledAppUi
import zed.rainxch.apps.presentation.model.AppSortRule
import zed.rainxch.apps.presentation.model.DeviceAppUi
import zed.rainxch.apps.presentation.model.GithubAssetUi


sealed interface AppsAction {
    data object OnNavigateBackClick : AppsAction

    data class OnSearchChange(
        val query: String,
    ) : AppsAction

    data class OnSortRuleSelected(
        val sortRule: AppSortRule,
    ) : AppsAction

    data class OnOpenApp(
        val app: InstalledAppUi,
    ) : AppsAction

    data class OnUpdateApp(
        val app: InstalledAppUi,
    ) : AppsAction

    data class OnCancelUpdate(
        val packageName: String,
    ) : AppsAction

    data object OnUpdateAll : AppsAction

    data object OnCancelUpdateAll : AppsAction

    data object OnCheckAllForUpdates : AppsAction

    data object OnRefresh : AppsAction

    // Fired by AppsRoot on ON_RESUME. Routes through the existing
    // cooldown-gated refresh path (`autoCheckForUpdatesIfNeeded`) so
    // re-entering the Apps screen catches external installs that
    // `PackageEventReceiver` may have missed — e.g. when GHS was
    // background-killed by an aggressive OEM ROM and never received
    // the PACKAGE_REPLACED broadcast. The 30-minute cooldown keeps
    // rapid resumes from burning GitHub rate limits.
    data object OnLifecycleResume : AppsAction

    data object OnToggleUpToDateSection : AppsAction

    data class OnNavigateToRepo(
        val repoId: Long,
        val sourceHost: String? = null,
        val owner: String? = null,
        val repo: String? = null,
    ) : AppsAction

    data class OnUninstallApp(
        val app: InstalledAppUi,
    ) : AppsAction

    // Uninstall confirmation
    data class OnUninstallConfirmed(val app: InstalledAppUi) : AppsAction
    data object OnDismissUninstallDialog : AppsAction

    // Link app to repo
    data object OnAddByLinkClick : AppsAction
    data object OnDismissLinkSheet : AppsAction
    data class OnDeviceAppSearchChange(val query: String) : AppsAction
    data class OnDeviceAppSelected(val app: DeviceAppUi) : AppsAction
    data class OnLinkSuggestionSelected(val owner: String, val repo: String) : AppsAction
    data object OnLinkEnterUrlManually : AppsAction
    data object OnRetryLinkSearch : AppsAction
    data class OnRepoUrlChanged(val url: String) : AppsAction
    data object OnValidateAndLinkRepo : AppsAction
    data object OnBackToAppPicker : AppsAction
    data object OnBackToSmartMatch : AppsAction
    data class OnLinkAssetSelected(val asset: GithubAssetUi) : AppsAction
    data object OnBackToEnterUrl : AppsAction

    /** Asset filter input on the link-sheet PickAsset step. */
    data class OnLinkAssetFilterChanged(val filter: String) : AppsAction

    /** Toggle for "fall back to older releases" on the link-sheet PickAsset step. */
    data class OnLinkFallbackToggled(val enabled: Boolean) : AppsAction

    // Per-app pre-release toggle
    data class OnTogglePreReleases(val packageName: String, val enabled: Boolean) : AppsAction

    // Per-app update-check toggle (issue #536: ignore updates for individual apps)
    data class OnToggleUpdateCheck(val packageName: String, val enabled: Boolean) : AppsAction

    // Per-app skip-this-version (issue #542: dismiss false-positive update prompts).
    // [tag] is the upstream release tag the user is choosing to skip — the
    // ViewModel persists it on the row and the periodic check suppresses the
    // badge until a strictly newer release lands.
    data class OnSkipReleaseTag(val packageName: String, val tag: String) : AppsAction

    // Clears the per-app skip; the next update check will surface the badge
    // again if a release matching the user's filters is still ahead of the
    // installed version.
    data class OnUnskipReleaseTag(val packageName: String) : AppsAction

    // Per-app advanced settings sheet (monorepo)
    data class OnOpenAdvancedSettings(val app: InstalledAppUi) : AppsAction
    data object OnDismissAdvancedSettings : AppsAction
    data class OnAdvancedFilterChanged(val filter: String) : AppsAction
    data class OnAdvancedFallbackToggled(val enabled: Boolean) : AppsAction
    data object OnAdvancedSaveFilter : AppsAction
    data object OnAdvancedClearFilter : AppsAction
    data object OnAdvancedRefreshPreview : AppsAction

    // Variant picker dialog (preferred APK variant)
    data class OnOpenVariantPicker(
        val app: InstalledAppUi,
        val resumeUpdateAfterPick: Boolean = false,
    ) : AppsAction
    data object OnDismissVariantPicker : AppsAction
    data class OnVariantSelected(val variant: String?) : AppsAction
    data object OnResetVariantToAuto : AppsAction

    // Export/Import
    data object OnExportApps : AppsAction
    data object OnExportObtainium : AppsAction
    data object OnImportApps : AppsAction
    data object OnDismissImportSummary : AppsAction

    data object OnDismissKaoBanner : AppsAction
    data object OnKaoLearnMore : AppsAction

    /**
     * User tapped the "Install" affordance on a row whose download
     * was previously deferred (the user navigated away from Details
     * mid-download). The orchestrator parked the file; this action
     * picks it up and runs the installer.
     */
    data class OnInstallPendingApp(
        val app: InstalledAppUi,
    ) : AppsAction

    /**
     * User tapped the Discard affordance on a pending row — opens the
     * confirmation dialog without doing anything destructive yet. The
     * actual cleanup runs only after [OnConfirmDiscardPendingInstall].
     */
    data class OnDiscardPendingInstall(
        val app: InstalledAppUi,
    ) : AppsAction

    /**
     * User confirmed the Discard prompt. Cancels the orchestrator
     * entry, deletes the parked APK file, and removes the DB row so
     * the app stops appearing in the list.
     */
    data class OnConfirmDiscardPendingInstall(
        val app: InstalledAppUi,
    ) : AppsAction

    /** User dismissed the Discard confirmation without confirming. */
    data object OnDismissDiscardPendingDialog : AppsAction

    // External import banner (E1)
    data object OnImportProposalReview : AppsAction

    data object OnImportProposalDismiss : AppsAction

    // Manual rescan trigger from the apps screen overflow. Resets the banner
    // dismiss watermark and routes the user into the import wizard, which
    // runs a fresh scan + match resolution on entry.
    data object OnRescanForGithubApps : AppsAction

    data object OnAddFromStarredClick : AppsAction
}
