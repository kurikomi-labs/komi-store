package zed.rainxch.core.domain.model

data class InstalledApp(
    val packageName: String,
    val repoId: Long,
    val repoName: String,
    val repoOwner: String,
    val repoOwnerAvatarUrl: String,
    val repoDescription: String?,
    val primaryLanguage: String?,
    val repoUrl: String,
    val installedVersion: String,
    val installedAssetName: String?,
    val installedAssetUrl: String?,
    val latestVersion: String?,
    val latestAssetName: String?,
    val latestAssetUrl: String?,
    val latestAssetSize: Long?,
    val appName: String,
    val installSource: InstallSource,
    val installedAt: Long,
    val lastCheckedAt: Long,
    val lastUpdatedAt: Long,
    val isUpdateAvailable: Boolean,
    val signingFingerprint: String?,
    val updateCheckEnabled: Boolean = true,
    val releaseNotes: String? = "",
    val systemArchitecture: String,
    val fileExtension: String,
    val isPendingInstall: Boolean = false,
    val installedVersionName: String? = null,
    val installedVersionCode: Long = 0L,
    val latestVersionName: String? = null,
    val latestVersionCode: Long? = null,
    val latestReleasePublishedAt: String? = null,
    val includePreReleases: Boolean = false,
    /**
     * Optional regex applied to asset names. When set, only assets whose
     * names match the pattern are considered installable for this app —
     * the building block for tracking one app inside a monorepo that ships
     * multiple apps (e.g. `ente-auth.*` against `ente-io/ente`).
     */
    val assetFilterRegex: String? = null,
    /**
     * When true, the update check walks back through past releases looking
     * for one whose assets match [assetFilterRegex]. Required for monorepos
     * where the latest release belongs to a sibling app.
     */
    val fallbackToOlderReleases: Boolean = false,
    /**
     * Stable identifier for the asset variant (e.g. `arm64-v8a`,
     * `universal`) that the user has chosen to track. Derived from the
     * picked asset filename's tail (everything after the version) so it
     * survives version bumps. `null` means "auto-pick by architecture".
     */
    val preferredAssetVariant: String? = null,
    /**
     * Set when the update checker can't find an asset matching
     * [preferredAssetVariant] in a fresh release — typically because the
     * maintainer renamed or restructured the artefacts. The UI shows a
     * "variant changed" prompt; the flag is cleared once the user picks
     * a new variant.
     */
    val preferredVariantStale: Boolean = false,
    /**
     * Token-set fingerprint of the picked asset, serialized via
     * `AssetVariant.serializeTokens` (sorted, joined by `|`). Primary
     * identity layer for the resolver — handles arch-before-version,
     * OS-version interlopers, and counters between version and arch.
     *
     * `null` for older rows pinned before this column existed and for
     * filenames where the token vocabulary recognises nothing.
     */
    val preferredAssetTokens: String? = null,
    /**
     * Glob-pattern fingerprint of the picked asset (e.g.
     * `app-*-arm64-v8a.apk`). Secondary identity layer used when the
     * token vocabulary doesn't recognise anything in the filename —
     * the most common case being custom flavor names.
     */
    val assetGlobPattern: String? = null,
    /**
     * Zero-based index of the picked asset in the original release's
     * installable-asset list. Last-resort same-position fallback when
     * none of the fingerprint layers match in a fresh release.
     */
    val pickedAssetIndex: Int? = null,
    /**
     * Total installable assets in the release the user picked from.
     * Pairs with [pickedAssetIndex] for the same-position fallback.
     */
    val pickedAssetSiblingCount: Int? = null,
    /**
     * Absolute path to a downloaded asset that's waiting for the user
     * to confirm install. Non-null means: the orchestrator finished
     * the download in `InstallWhileForeground` mode but the foreground
     * screen had gone away by then, so the bytes are parked and the
     * apps list shows a "ready to install" row.
     *
     * Cleared by the orchestrator after a successful install or when
     * the user dismisses the row.
     */
    val pendingInstallFilePath: String? = null,
    /**
     * Release tag of the version represented by [pendingInstallFilePath].
     * Used by Details to detect "the parked file matches the
     * currently-selected release" and skip re-downloading on install.
     */
    val pendingInstallVersion: String? = null,
    /**
     * Original (unscoped) asset filename of the parked file. Pairs
     * with [pendingInstallVersion] for the Details-screen match.
     */
    val pendingInstallAssetName: String? = null,
    /**
     * Release tag the user explicitly skipped via the apps row "Skip
     * this version" action. While non-null, the periodic update check
     * suppresses the badge for [packageName] when the matched release
     * tag equals this value. Auto-clears the moment a strictly newer
     * release lands so the user gets re-notified next cycle.
     */
    val skippedReleaseTag: String? = null,
    val sourceHost: String? = null,
)

/**
 * True when the app actually exists on device. A row with
 * [InstalledApp.isPendingInstall] set means the bytes are parked on disk
 * but the system install has not completed (or failed) — callers that
 * surface an "Installed" badge must treat that case as *not* installed,
 * otherwise the Details screen (which checks `isPendingInstall`) and the
 * Home/Search cards drift out of sync after a failed install.
 */
fun InstalledApp?.isReallyInstalled(): Boolean = this != null && !this.isPendingInstall

/**
 * True when a genuine update is pending install — mirrors the check
 * [zed.rainxch.details.presentation.components.SmartInstallButton] does
 * so non-Details surfaces render the same state machine.
 */
fun InstalledApp?.hasActualUpdate(): Boolean =
    this != null && this.isUpdateAvailable && !this.isPendingInstall
