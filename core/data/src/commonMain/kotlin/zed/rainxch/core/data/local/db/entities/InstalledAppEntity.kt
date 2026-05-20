package zed.rainxch.core.data.local.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import zed.rainxch.core.domain.model.InstallSource

@Entity(tableName = "installed_apps")
data class InstalledAppEntity(
    @PrimaryKey val packageName: String,
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
    val signingFingerprint: String?,
    val installedAt: Long,
    val lastCheckedAt: Long,
    val lastUpdatedAt: Long,
    val isUpdateAvailable: Boolean,
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
     * Per-app regex applied to asset (file) names. When non-null, only assets
     * whose name matches the pattern are considered installable for this app.
     * Used to track a single app inside a monorepo that ships multiple apps
     * (e.g. `ente-auth.*` against `ente-io/ente`).
     */
    val assetFilterRegex: String? = null,
    /**
     * When true, the update checker walks backward through past releases until
     * it finds one whose assets match [assetFilterRegex]. Required for
     * monorepos where the latest release is for a *different* app.
     *
     * `@ColumnInfo(defaultValue = "0")` matches `MIGRATION_9_10`'s
     * `DEFAULT 0` clause so Room's schema validator doesn't flag the
     * column on freshly-built databases.
     */
    @ColumnInfo(defaultValue = "0")
    val fallbackToOlderReleases: Boolean = false,
    /**
     * Stable identifier for the asset variant the user wants to track —
     * for example `arm64-v8a`, `universal`, or `x86_64`. Derived from the
     * picked asset filename by stripping the version segment, so it
     * survives release-to-release version bumps.
     *
     * `null` means "use the platform installer's automatic picker"
     * (today's behaviour). When non-null, [checkForUpdates] resolves the
     * matching asset on every check; if no asset in the new release
     * matches the variant, [preferredVariantStale] is flipped to true.
     */
    val preferredAssetVariant: String? = null,
    /**
     * Set to true by the update checker when the persisted
     * [preferredAssetVariant] cannot be found in the latest release's
     * assets — typically because the maintainer renamed or restructured
     * the artefacts. The UI surfaces this with a "variant changed —
     * pick again" prompt and clears it once the user picks a new variant.
     *
     * `@ColumnInfo(defaultValue = "0")` matches `MIGRATION_10_11`'s
     * `DEFAULT 0` clause so Room's schema validator doesn't flag a
     * mismatch between the migrated table and the freshly-created one.
     */
    @ColumnInfo(defaultValue = "0")
    val preferredVariantStale: Boolean = false,
    /**
     * Token-set fingerprint of the picked asset, serialized as
     * `token1|token2|…` (alphabetically sorted so equal sets always
     * serialize to identical strings — letting the resolver compare
     * with plain string equality instead of parsing).
     *
     * The token vocabulary is closed (see `AssetVariant.kt`); only
     * recognised arch / flavor tokens are stored. `null` means
     * "no token fingerprint available — fall back to glob or tail".
     */
    val preferredAssetTokens: String? = null,
    /**
     * Glob-pattern fingerprint of the picked asset (e.g.
     * `app-*-arm64-v8a.apk`). Used as a secondary identity layer when
     * the token vocabulary doesn't recognise anything in the filename
     * — the most common case being custom flavor names.
     *
     * `null` means the picked filename had no version-shaped substring
     * to wildcard, so the glob would just equal the filename and
     * provides no rescue value.
     */
    val assetGlobPattern: String? = null,
    /**
     * Zero-based index of the picked asset in the original release's
     * installable-asset list. Used by the same-position fallback —
     * when none of the fingerprint layers match in a fresh release
     * but the new release has exactly the same number of installable
     * assets, the asset at this index is preferred.
     *
     * Weak signal, only consulted as a last resort. `null` for older
     * rows pinned before this column existed.
     */
    val pickedAssetIndex: Int? = null,
    /**
     * Total installable assets in the release the user picked from.
     * Pairs with [pickedAssetIndex] for same-position fallback.
     */
    val pickedAssetSiblingCount: Int? = null,
    /**
     * Absolute path to a downloaded but not-yet-installed APK / asset.
     *
     * Set by `DefaultDownloadOrchestrator` when an
     * `InstallPolicy.InstallWhileForeground` download finishes after
     * the foreground screen has been destroyed (the user navigated
     * away mid-download). The apps list shows a "Ready to install"
     * row with a one-tap install action when this is non-null.
     *
     * Cleared by the orchestrator on successful install or by the
     * user dismissing the row.
     *
     * Survives process restarts: the file lives on disk and the
     * column points back at it. Both are cleaned up if the file is
     * missing on next read.
     */
    val pendingInstallFilePath: String? = null,
    /**
     * Release tag of the version represented by [pendingInstallFilePath].
     * Used by the Details screen to detect "the parked file matches
     * the currently-selected release" — when both [pendingInstallVersion]
     * and [pendingInstallAssetName] match the user's selection, the
     * install button skips the download phase and dispatches the
     * existing install dialog flow on the parked file directly.
     */
    val pendingInstallVersion: String? = null,
    /**
     * Asset filename (the original, not the scoped form) of the
     * parked file. Pairs with [pendingInstallVersion] for
     * Details-screen "ready to install" detection.
     */
    val pendingInstallAssetName: String? = null,
    /**
     * Tag of an upstream release the user has chosen to ignore. Set when
     * the user taps "Skip this version" on the apps row. While non-null
     * and equal to the matched release tag, [checkForUpdates] suppresses
     * the update badge for [packageName]. The flag auto-clears the
     * moment a strictly newer release lands so the user gets re-notified
     * the next cycle without having to unskip manually.
     *
     * `@ColumnInfo(defaultValue = "NULL")` matches `MIGRATION_15_16` so
     * Room's schema validator doesn't flag a mismatch between the
     * migrated table and the freshly-created one.
     */
    @ColumnInfo(defaultValue = "NULL")
    val skippedReleaseTag: String? = null,
    @ColumnInfo(defaultValue = "NULL")
    val sourceHost: String? = null,
)
