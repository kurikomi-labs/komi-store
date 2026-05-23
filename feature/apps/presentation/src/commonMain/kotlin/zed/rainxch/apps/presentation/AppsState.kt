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
import zed.rainxch.core.domain.system.RepoMatchSuggestion

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

    val isUpToDateSectionExpanded: Boolean = true,
    val isUpdatesSectionExpanded: Boolean = true,

    val showLinkSheet: Boolean = false,
    val linkStep: LinkStep = LinkStep.PickApp,
    val deviceApps: ImmutableList<DeviceAppUi> = persistentListOf(),
    val deviceAppSearchQuery: String = "",
    val selectedDeviceApp: DeviceAppUi? = null,
    val repoUrl: String = "",
    val linkSearchLoading: Boolean = false,
    val linkSuggestions: ImmutableList<RepoMatchSuggestion> = persistentListOf(),
    val linkSearchError: String? = null,
    val isValidatingRepo: Boolean = false,
    val repoValidationError: String? = null,
    val linkValidationStatus: String? = null,
    val linkInstallableAssets: ImmutableList<GithubAssetUi> = persistentListOf(),
    val linkSelectedAsset: GithubAssetUi? = null,
    val linkDownloadProgress: Int? = null,
    val fetchedRepoInfo: GithubRepoInfoUi? = null,

    val linkAssetFilter: String = "",

    val linkAssetFilterError: String? = null,

    val linkFallbackToOlder: Boolean = false,

    val advancedSettingsApp: InstalledAppUi? = null,
    val advancedFilterDraft: String = "",
    val advancedFallbackDraft: Boolean = false,
    val advancedFilterError: String? = null,
    val advancedPreviewLoading: Boolean = false,
    val advancedPreviewMatched: ImmutableList<GithubAssetUi> = persistentListOf(),
    val advancedPreviewTag: String? = null,
    val advancedPreviewMessage: String? = null,
    val advancedSavingFilter: Boolean = false,

    val variantPickerApp: InstalledAppUi? = null,
    val variantPickerLoading: Boolean = false,
    val variantPickerOptions: ImmutableList<GithubAssetUi> = persistentListOf(),
    val variantPickerCurrentVariant: String? = null,
    val variantPickerError: String? = null,

    val variantPickerResumeUpdateAfterPick: Boolean = false,

    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val importSummary: zed.rainxch.apps.domain.model.ImportResult? = null,

    val appPendingUninstall: InstalledAppUi? = null,

    val appPendingDiscard: InstalledAppUi? = null,

    val pendingExternalImportCount: Int = 0,
    val showImportProposalBanner: Boolean = false,
    val isExternalImportInFlight: Boolean = false,

    val showKaoBanner: Boolean = false,
    val linkSourceHost: String? = null,

    val twoPaneSelectedPackage: String? = null,
) {
    val filteredDeviceApps: ImmutableList<DeviceAppUi>
        get() {
            val searched =
                if (deviceAppSearchQuery.isBlank()) {
                    deviceApps
                } else {
                    deviceApps.filter {
                        it.appName.contains(deviceAppSearchQuery, ignoreCase = true) ||
                            it.packageName.contains(deviceAppSearchQuery, ignoreCase = true)
                    }
                }
            return searched
                .sortedWith(
                    compareBy<DeviceAppUi> { it.installerCategory.sortPriority }
                        .thenBy { it.appName.lowercase() }
                        .thenBy { it.packageName },
                ).toImmutableList()
        }

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
    SmartMatch,
    EnterUrl,
    PickAsset,
}
