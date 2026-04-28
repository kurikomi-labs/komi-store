package zed.rainxch.apps.presentation

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import zed.rainxch.apps.domain.model.GithubRepoInfo
import zed.rainxch.apps.presentation.model.AppItem
import zed.rainxch.apps.presentation.model.AppSortRule
import zed.rainxch.apps.presentation.model.DeviceAppUi
import zed.rainxch.apps.presentation.model.GithubAssetUi
import zed.rainxch.apps.presentation.model.GithubRepoInfoUi
import zed.rainxch.apps.presentation.model.InstalledAppUi
import zed.rainxch.apps.presentation.model.UpdateAllProgress
import zed.rainxch.core.domain.model.DeviceApp
import zed.rainxch.core.domain.model.GithubAsset

data class AppsState(
    val apps: ImmutableList<AppItem> = persistentListOf(),
    val filteredApps: ImmutableList<AppItem> = persistentListOf(),
    val searchQuery: String = "",
    val sortRule: AppSortRule = AppSortRule.UpdatesFirst,
    val isLoading: Boolean = false,
    val isUpdatingAll: Boolean = false,
    val updateAllProgress: UpdateAllProgress? = null,
    val updateAllButtonEnabled: Boolean = true,
    val isCheckingForUpdates: Boolean = false,
    val lastCheckedTimestamp: Long? = null,
    val isRefreshing: Boolean = false,
    val isLiquidGlassEnabled: Boolean = true,
    // Link app to repo
    val showLinkSheet: Boolean = false,
    val linkStep: LinkStep = LinkStep.PickApp,
    val deviceApps: ImmutableList<DeviceAppUi> = persistentListOf(),
    val deviceAppSearchQuery: String = "",
    val selectedDeviceApp: DeviceAppUi? = null,
    val repoUrl: String = "",
    val isValidatingRepo: Boolean = false,
    val repoValidationError: String? = null,
    val linkValidationStatus: String? = null,
    val linkInstallableAssets: ImmutableList<GithubAssetUi> = persistentListOf(),
    val linkSelectedAsset: GithubAssetUi? = null,
    val linkDownloadProgress: Int? = null,
    val fetchedRepoInfo: GithubRepoInfoUi? = null,
    /** Filter input on the PickAsset step. Live-narrows [linkInstallableAssets]. */
    val linkAssetFilter: String = "",
    /** Validation message for [linkAssetFilter] (invalid regex syntax). */
    val linkAssetFilterError: String? = null,
    /** Whether linking should also enable fallback-to-older-releases. */
    val linkFallbackToOlder: Boolean = false,
    // Per-app advanced settings (monorepo support)
    val advancedSettingsApp: InstalledAppUi? = null,
    val advancedFilterDraft: String = "",
    val advancedFallbackDraft: Boolean = false,
    val advancedFilterError: String? = null,
    val advancedPreviewLoading: Boolean = false,
    val advancedPreviewMatched: ImmutableList<GithubAssetUi> = persistentListOf(),
    val advancedPreviewTag: String? = null,
    val advancedPreviewMessage: String? = null,
    val advancedSavingFilter: Boolean = false,
    // Variant picker dialog (shown when preferredVariantStale, when the
    // user explicitly opens it from advanced settings, or when they tap
    // Update on a stale-variant app)
    val variantPickerApp: InstalledAppUi? = null,
    val variantPickerLoading: Boolean = false,
    val variantPickerOptions: ImmutableList<GithubAssetUi> = persistentListOf(),
    val variantPickerCurrentVariant: String? = null,
    val variantPickerError: String? = null,
    /**
     * Set when the picker is being shown specifically because the user
     * tapped Update on a stale-variant app — after they pick we should
     * automatically resume the update flow.
     */
    val variantPickerResumeUpdateAfterPick: Boolean = false,
    // Export/Import
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    // Uninstall confirmation
    val appPendingUninstall: InstalledAppUi? = null,
    // External import banner (E1)
    val pendingExternalImportCount: Int = 0,
    val showImportProposalBanner: Boolean = false,
    val isExternalImportInFlight: Boolean = false,
) {
    val filteredDeviceApps: ImmutableList<DeviceAppUi>
        get() =
            if (deviceAppSearchQuery.isBlank()) {
                deviceApps.toImmutableList()
            } else {
                deviceApps
                    .filter {
                        it.appName.contains(deviceAppSearchQuery, ignoreCase = true) ||
                            it.packageName.contains(deviceAppSearchQuery, ignoreCase = true)
                    }.toImmutableList()
            }

    /**
     * Live-filtered view of [linkInstallableAssets] for the link sheet's
     * PickAsset step. When the filter is invalid we keep showing the full
     * list so the user can still pick something — the error is surfaced via
     * [linkAssetFilterError].
     */
    val filteredLinkAssets: ImmutableList<GithubAssetUi>
        get() {
            val raw = linkAssetFilter.trim()
            if (raw.isEmpty()) return linkInstallableAssets
            val regex =
                runCatching { Regex(raw, RegexOption.IGNORE_CASE) }.getOrNull()
                    ?: return linkInstallableAssets
            return linkInstallableAssets
                .filter { regex.containsMatchIn(it.name) }
                .toImmutableList()
        }
}

enum class LinkStep {
    PickApp,
    EnterUrl,
    PickAsset,
}
