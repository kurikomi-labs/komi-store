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

    data class OnNavigateToRepo(
        val repoId: Long,
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
    data class OnRepoUrlChanged(val url: String) : AppsAction
    data object OnValidateAndLinkRepo : AppsAction
    data object OnBackToAppPicker : AppsAction
    data class OnLinkAssetSelected(val asset: GithubAssetUi) : AppsAction
    data object OnBackToEnterUrl : AppsAction

    /** Asset filter input on the link-sheet PickAsset step. */
    data class OnLinkAssetFilterChanged(val filter: String) : AppsAction

    /** Toggle for "fall back to older releases" on the link-sheet PickAsset step. */
    data class OnLinkFallbackToggled(val enabled: Boolean) : AppsAction

    // Per-app pre-release toggle
    data class OnTogglePreReleases(val packageName: String, val enabled: Boolean) : AppsAction

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
    data object OnImportApps : AppsAction

    /**
     * User tapped the "Install" affordance on a row whose download
     * was previously deferred (the user navigated away from Details
     * mid-download). The orchestrator parked the file; this action
     * picks it up and runs the installer.
     */
    data class OnInstallPendingApp(
        val app: InstalledAppUi,
    ) : AppsAction

    // External import banner (E1)
    data object OnImportProposalReview : AppsAction

    data object OnImportProposalDismiss : AppsAction

    // Manual rescan trigger from the apps screen overflow. Resets the banner
    // dismiss watermark and routes the user into the import wizard, which
    // runs a fresh scan + match resolution on entry.
    data object OnRescanForGithubApps : AppsAction
}
