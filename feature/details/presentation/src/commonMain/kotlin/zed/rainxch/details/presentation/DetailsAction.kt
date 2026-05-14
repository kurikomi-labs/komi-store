package zed.rainxch.details.presentation

import org.jetbrains.compose.resources.StringResource
import zed.rainxch.core.domain.model.DiscoveryPlatform
import zed.rainxch.core.domain.model.GithubAsset
import zed.rainxch.core.domain.model.GithubRelease
import zed.rainxch.details.domain.model.ReleaseCategory
import zed.rainxch.details.presentation.model.TranslationTarget

sealed interface DetailsAction {
    data class OnPlatformChipClick(
        val platform: DiscoveryPlatform,
    ) : DetailsAction

    data object Retry : DetailsAction

    data object RetryReleases : DetailsAction

    data object Refresh : DetailsAction

    data object InstallPrimary : DetailsAction

    data object OnDismissDowngradeWarning : DetailsAction

    data object OnDismissSigningKeyWarning : DetailsAction

    data object OnOverrideSigningKeyWarning : DetailsAction

    data object UninstallApp : DetailsAction
    data object OnRequestUninstall : DetailsAction
    data object OnDismissUninstallConfirmation : DetailsAction
    data object OnConfirmUninstall : DetailsAction

    data object OnUnlinkExternalApp : DetailsAction
    data object OnDismissUnlinkConfirmation : DetailsAction
    data object OnConfirmUnlinkExternalApp : DetailsAction

    data class DownloadAsset(
        val downloadUrl: String,
        val assetName: String,
        val sizeBytes: Long,
    ) : DetailsAction

    data object CancelCurrentDownload : DetailsAction

    data object OpenRepoInBrowser : DetailsAction

    data object OpenAuthorInBrowser : DetailsAction

    data class OpenDeveloperProfile(
        val username: String,
    ) : DetailsAction

    data object OpenInObtainium : DetailsAction

    data object OpenInAppManager : DetailsAction

    data object InstallWithExternalApp : DetailsAction

    data object OpenWithExternalInstaller : DetailsAction

    data object DismissExternalInstallerPrompt : DetailsAction

    data object OnToggleInstallDropdown : DetailsAction

    data object OnNavigateBackClick : DetailsAction

    data object OnToggleFavorite : DetailsAction

    data object OnShareClick : DetailsAction

    data object UpdateApp : DetailsAction

    data object OpenApp : DetailsAction

    data class OnMessage(
        val messageText: StringResource,
    ) : DetailsAction

    data class SelectReleaseCategory(
        val category: ReleaseCategory,
    ) : DetailsAction

    data class SelectRelease(
        val release: GithubRelease,
    ) : DetailsAction

    data object ToggleVersionPicker : DetailsAction

    data object ToggleAboutExpanded : DetailsAction

    data object ToggleWhatsNewExpanded : DetailsAction

    data class TranslateAbout(
        val targetLanguageCode: String,
    ) : DetailsAction

    data class TranslateWhatsNew(
        val targetLanguageCode: String,
    ) : DetailsAction

    data object ToggleAboutTranslation : DetailsAction

    data object ToggleWhatsNewTranslation : DetailsAction

    data class ShowLanguagePicker(
        val target: TranslationTarget,
    ) : DetailsAction

    data object DismissLanguagePicker : DetailsAction

    // show release asset picker
    data class SelectDownloadAsset(
        val release: GithubAsset,
    ) : DetailsAction

    data object ToggleReleaseAssetsPicker : DetailsAction

    /**
     * Clears the user's preferred variant pin for the currently-tracked
     * app. Falls back to the platform auto-picker on subsequent updates.
     * Triggered by the "Unpin variant" affordance in the asset picker
     * sheet.
     */
    data object UnpinPreferredVariant : DetailsAction

    /**
     * Flips the per-app `includePreReleases` flag. Exposed as the
     * inline channel toggle on Details so users can opt in/out of
     * beta updates without digging into the apps advanced settings
     * sheet (GitHub-Store release UX #2).
     */
    data object ToggleIncludeBetas : DetailsAction

    /**
     * Switches the currently-tracked app from a pre-release to the
     * latest stable release. Selects the stable release and
     * initiates the install flow on it (GitHub-Store release UX #3).
     */
    data object SwitchToStable : DetailsAction

    /**
     * Opens the APK Inspect bottom sheet. The ViewModel resolves the
     * APK source automatically — installed package wins over parked
     * file when both exist (installed manifest is the authoritative
     * source on what's actually on the device).
     */
    data object OnInspectApk : DetailsAction

    /** Closes the APK Inspect bottom sheet. */
    data object OnDismissApkInspect : DetailsAction

    /**
     * Acknowledges the inspect-button discoverability coachmark — fired
     * either when the coachmark is tapped/dismissed or when the user
     * opens the inspect sheet for the first time. Persists so the
     * coachmark only ever shows once.
     */
    data object OnAcknowledgeApkInspectCoachmark : DetailsAction

    /**
     * Acknowledges the release-channel chip coachmark. Fired on
     * tap/dismiss or when the user toggles the channel chip itself.
     * Persists so the coachmark only ever shows once.
     */
    data object OnAcknowledgeChannelChipCoachmark : DetailsAction

    /**
     * Flip the "Show all platforms" picker setting (persisted globally).
     * Carries the explicit target value so rapid back-and-forth toggles
     * don't race against a stale read of the in-memory state.
     */
    data class OnToggleShowAllPlatforms(val enabled: Boolean) : DetailsAction

    /**
     * Download a non-current-platform asset for transfer to another
     * device. Routes to the user's browser so the file lands in their
     * normal Downloads folder.
     */
    data class OnDownloadForTransfer(
        val assetUrl: String,
    ) : DetailsAction
}
