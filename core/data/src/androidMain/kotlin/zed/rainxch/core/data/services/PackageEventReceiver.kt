package zed.rainxch.core.data.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import zed.rainxch.core.data.local.db.dao.ExternalLinkDao
import zed.rainxch.core.domain.repository.ExternalImportRepository
import zed.rainxch.core.domain.repository.InstalledAppsRepository
import zed.rainxch.core.domain.system.ExternalLinkState
import zed.rainxch.core.domain.system.PackageMonitor
import zed.rainxch.core.domain.util.VersionVerdict
import zed.rainxch.core.domain.util.resolveExternalInstallVerdict

/**
 * Listens for package install/replace/remove broadcasts to update tracked app state.
 *
 * Registered both statically (manifest — works when process is dead, e.g. after
 * Shizuku silent install) and dynamically (GithubStoreApp — immediate in-process delivery).
 *
 * Uses [KoinComponent] for the no-arg constructor path (manifest-registered).
 * The constructor with explicit dependencies is used for dynamic registration.
 */
class PackageEventReceiver() :
    BroadcastReceiver(),
    KoinComponent {
    private val installedAppsRepositoryKoin: InstalledAppsRepository by inject()
    private val packageMonitorKoin: PackageMonitor by inject()
    private val appScopeKoin: CoroutineScope by inject()
    private val externalImportRepositoryKoin: ExternalImportRepository by inject()
    private val externalLinkDaoKoin: ExternalLinkDao by inject()

    // Explicitly provided dependencies (dynamic registration path)
    private var explicitRepository: InstalledAppsRepository? = null
    private var explicitMonitor: PackageMonitor? = null
    private var explicitExternalImport: ExternalImportRepository? = null
    private var explicitExternalLinkDao: ExternalLinkDao? = null
    private var explicitAppScope: CoroutineScope? = null

    // Local fallback scope for the manifest-registered path when
    // `onReceive` fires but Koin somehow couldn't resolve the shared
    // app scope (extremely unlikely — the Application installs Koin
    // synchronously in onCreate). The async backstop below prefers
    // the Koin scope via `getBackstopScope`.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    constructor(
        installedAppsRepository: InstalledAppsRepository,
        packageMonitor: PackageMonitor,
        externalImportRepository: ExternalImportRepository,
        externalLinkDao: ExternalLinkDao,
        appScope: CoroutineScope,
    ) : this() {
        this.explicitRepository = installedAppsRepository
        this.explicitMonitor = packageMonitor
        this.explicitExternalImport = externalImportRepository
        this.explicitExternalLinkDao = externalLinkDao
        this.explicitAppScope = appScope
    }

    private fun getRepository(): InstalledAppsRepository = explicitRepository ?: installedAppsRepositoryKoin

    private fun getMonitor(): PackageMonitor = explicitMonitor ?: packageMonitorKoin

    private fun getExternalImport(): ExternalImportRepository =
        explicitExternalImport ?: externalImportRepositoryKoin

    private fun getExternalLinkDao(): ExternalLinkDao =
        explicitExternalLinkDao ?: externalLinkDaoKoin

    private fun getBackstopScope(): CoroutineScope =
        // Koin's app-scoped CoroutineScope outlives a manifest-registered
        // receiver whose local `scope` would die with the instance. Fall
        // back to the local scope only if Koin isn't initialized yet
        // (shouldn't happen post-Application.onCreate, but defensive).
        explicitAppScope ?: runCatching { appScopeKoin }.getOrElse { scope }

    override fun onReceive(
        context: Context?,
        intent: Intent?,
    ) {
        val packageName = intent?.data?.schemeSpecificPart ?: return

        Logger.d { "PackageEventReceiver: ${intent.action} for $packageName" }

        try {
            when (intent.action) {
                Intent.ACTION_PACKAGE_ADDED,
                Intent.ACTION_PACKAGE_REPLACED,
                -> {
                    scope.launch { onPackageInstalled(packageName) }
                }

                Intent.ACTION_PACKAGE_FULLY_REMOVED -> {
                    scope.launch { onPackageRemoved(packageName) }
                }
            }
        } catch (e: Exception) {
            Logger.e { "PackageEventReceiver: Failed to handle ${intent.action}: ${e.message}" }
        }
    }

    private suspend fun onPackageInstalled(packageName: String) {
        try {
            val repo = getRepository()
            val monitor = getMonitor()
            val app = repo.getAppByPackage(packageName)

            // First-time installs (app == null) skip the tracked-app branches
            // but MUST still hit the backstop delta-scan launch below — that's
            // how a freshly-installed GitHub app surfaces as a wizard candidate
            // when the user installs it after the initial scan.
            if (app != null) {
                if (app.isPendingInstall) {
                    val systemInfo = monitor.getInstalledPackageInfo(packageName)
                    if (systemInfo != null) {
                        val expectedVersionCode = app.latestVersionCode ?: 0L
                        val wasActuallyUpdated =
                            expectedVersionCode > 0L &&
                                systemInfo.versionCode >= expectedVersionCode

                        if (wasActuallyUpdated) {
                            repo.updateAppVersion(
                                packageName = packageName,
                                newTag = app.latestVersion ?: systemInfo.versionName,
                                newAssetName = app.latestAssetName ?: "",
                                newAssetUrl = app.latestAssetUrl ?: "",
                                newVersionName = systemInfo.versionName,
                                newVersionCode = systemInfo.versionCode,
                                signingFingerprint = app.signingFingerprint,
                            )
                            repo.updatePendingStatus(packageName, false)
                            Logger.i { "Update confirmed via broadcast: $packageName (v${systemInfo.versionName})" }
                        } else {
                            repo.updateApp(
                                app.copy(
                                    isPendingInstall = false,
                                    installedVersionName = systemInfo.versionName,
                                    installedVersionCode = systemInfo.versionCode,
                                    isUpdateAvailable =
                                        (
                                            app.latestVersionCode
                                                ?: 0L
                                        ) > systemInfo.versionCode,
                                ),
                            )
                            Logger.i {
                                "Package replaced but not updated to target: $packageName " +
                                    "(system: v${systemInfo.versionName}/${systemInfo.versionCode}, " +
                                    "target: v${app.latestVersionName}/${app.latestVersionCode})"
                            }
                        }
                    } else {
                        repo.updatePendingStatus(packageName, false)
                        Logger.i { "Resolved pending install via broadcast (no system info): $packageName" }
                    }
                } else {
                    handleExternalInstall(packageName, app, repo, monitor)
                }
            }
        } catch (e: Exception) {
            Logger.e { "PackageEventReceiver error for $packageName: ${e.message}" }
        }

        // Fire a delta scan for previously-untracked installs so the
        // import banner can pick up the new candidate. Guarded so we
        // don't churn on apps the user already linked or asked us to
        // ignore. Runs on the app scope — independent of the install
        // path above. Always fires regardless of whether `app` was found.
        getBackstopScope().launch {
            runCatching {
                val rescan = shouldRescan(packageName)
                if (rescan) {
                    getExternalImport().runDeltaScan(setOf(packageName))
                }
            }.onFailure {
                Logger.w(it) { "Delta scan failed for $packageName" }
            }
        }
    }

    // Skip re-scanning when (a) we already track the app in
    // `installed_apps` (the user installed it through the store, or
    // we already auto-linked it and materialized the row), or (b) the
    // package is already MATCHED / NEVER_ASK in `external_links`.
    // PENDING_REVIEW and SKIPPED are intentionally rescanned —
    // metadata may have changed (label, fingerprint, installer) and
    // the user hasn't given a permanent answer yet.
    private suspend fun shouldRescan(packageName: String): Boolean {
        val tracked = runCatching { getRepository().getAppByPackage(packageName) }
            .getOrNull()
        if (tracked != null) return false
        val link = runCatching { getExternalLinkDao().get(packageName) }.getOrNull()
        val state = link?.state ?: return true
        return state != ExternalLinkState.MATCHED.name &&
            state != ExternalLinkState.NEVER_ASK.name
    }

    /**
     * Path taken when the broadcast fires for a tracked app that the
     * user did NOT install from inside the store (sideload, browser
     * download, Play Store update, F-Droid update of a shared
     * package, etc.). The pending-install branch above handles the
     * in-app install case.
     *
     * Strategy (GitHub-Store#378):
     *
     *  1. Refresh every version field from PackageManager — this is
     *     the strictest source of truth for what is actually on
     *     device right now.
     *  2. Apply [resolveExternalInstallVerdict] for an immediate
     *     decision about `isUpdateAvailable`. The resolver uses a
     *     priority ladder (versionCode → versionName vs
     *     latestVersionName → versionName vs release tag) and only
     *     returns [VersionVerdict.UNKNOWN] when none of those
     *     produce a reliable answer.
     *  3. Dispatch an async `checkForUpdates(packageName)` on the
     *     app-scoped coroutine scope. That call re-fetches the
     *     latest release list from GitHub and applies
     *     [zed.rainxch.core.domain.util.VersionMath] with the freshly
     *     updated `installedVersion`, so even an incorrect optimistic
     *     verdict is corrected within the RTT of a single GitHub
     *     API hit.
     *
     * The async backstop runs on the Koin-provided app scope so it
     * survives the receiver instance being torn down after
     * `onReceive` returns — critical for the manifest-registered
     * path.
     */
    private suspend fun handleExternalInstall(
        packageName: String,
        app: zed.rainxch.core.domain.model.InstalledApp,
        repo: InstalledAppsRepository,
        monitor: PackageMonitor,
    ) {
        val systemInfo = monitor.getInstalledPackageInfo(packageName) ?: return
        val versionChanged =
            systemInfo.versionCode != app.installedVersionCode ||
                systemInfo.versionName != app.installedVersionName
        if (!versionChanged) {
            Logger.d {
                "Broadcast touch with no version change: $packageName (v${systemInfo.versionName})"
            }
            return
        }

        val verdict =
            resolveExternalInstallVerdict(
                app = app,
                newVersionName = systemInfo.versionName,
                newVersionCode = systemInfo.versionCode,
            )

        val newIsUpdateAvailable =
            when (verdict) {
                VersionVerdict.UP_TO_DATE -> false
                VersionVerdict.UPDATE_AVAILABLE -> true
                // Preserve the current flag for UNKNOWN — the async
                // checkForUpdates below is about to overwrite it with
                // an authoritative answer anyway.
                VersionVerdict.UNKNOWN -> app.isUpdateAvailable
            }

        // Targeted column-only write: avoids clobbering sibling fields
        // (download orchestrator metadata, variant pin, favourite
        // toggle, checkForUpdates results…) that may have landed
        // between `onPackageInstalled`'s initial `getAppByPackage` and
        // this write. See `InstalledAppsRepository.updateInstalledVersion`.
        repo.updateInstalledVersion(
            packageName = packageName,
            installedVersion = systemInfo.versionName,
            installedVersionName = systemInfo.versionName,
            installedVersionCode = systemInfo.versionCode,
            isUpdateAvailable = newIsUpdateAvailable,
        )

        Logger.i {
            "External version change via broadcast: $packageName " +
                "DB v${app.installedVersionName}(${app.installedVersionCode}) → " +
                "System v${systemInfo.versionName}(${systemInfo.versionCode}), " +
                "verdict=$verdict, updateAvailable=$newIsUpdateAvailable"
        }

        // Authoritative re-validation against fresh GitHub release data.
        // Runs on the app scope so it outlives this broadcast.
        getBackstopScope().launch {
            try {
                repo.checkForUpdates(packageName)
                Logger.d {
                    "External-install re-validation completed for $packageName"
                }
            } catch (e: Exception) {
                Logger.w {
                    "External-install re-validation failed for $packageName: ${e.message}"
                }
            }
        }
    }

    private suspend fun onPackageRemoved(packageName: String) {
        try {
            getRepository().deleteInstalledApp(packageName)
            runCatching { getExternalImport().unlink(packageName) }
                .onFailure { initialError ->
                    Logger.w(initialError) { "External link cleanup failed for $packageName; scheduling retry" }
                    // A failed unlink leaves a stale MATCHED/NEVER_ASK row that
                    // makes `shouldRescan` return false on a future reinstall —
                    // i.e., the user reinstalls a previously-tracked app and we
                    // silently fail to re-link it. Retry once on the app scope
                    // after a short backoff. If the retry also fails, the next
                    // periodic worker sweep gets a chance via `runPeriodicExternalDeltaScan`.
                    getBackstopScope().launch {
                        kotlinx.coroutines.delay(UNLINK_RETRY_DELAY_MS)
                        runCatching { getExternalImport().unlink(packageName) }
                            .onSuccess {
                                Logger.i { "External link cleanup retry succeeded for $packageName" }
                                // Recovery delta scan: a fast reinstall during the
                                // retry backoff window would have hit shouldRescan
                                // while the row was still MATCHED → no rescan
                                // queued. Now that the row is gone, evaluate
                                // shouldRescan again and fire if the package is
                                // currently installed.
                                runCatching {
                                    if (shouldRescan(packageName)) {
                                        getExternalImport().runDeltaScan(setOf(packageName))
                                    }
                                }.onFailure { e ->
                                    Logger.w(e) { "Post-retry delta scan failed for $packageName" }
                                }
                            }
                            .onFailure { retryError ->
                                Logger.w(retryError) {
                                    "External link cleanup final failure for $packageName; " +
                                        "row may persist until next periodic scan"
                                }
                            }
                    }
                }
            Logger.i { "Removed uninstalled app via broadcast: $packageName" }
        } catch (e: Exception) {
            Logger.e { "PackageEventReceiver remove error for $packageName: ${e.message}" }
        }
    }

    companion object {
        private const val UNLINK_RETRY_DELAY_MS: Long = 1_000

        fun createIntentFilter(): IntentFilter =
            IntentFilter().apply {
                addAction(Intent.ACTION_PACKAGE_ADDED)
                addAction(Intent.ACTION_PACKAGE_REPLACED)
                addAction(Intent.ACTION_PACKAGE_FULLY_REMOVED)
                addDataScheme("package")
            }
    }
}
