package zed.rainxch.githubstore.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import zed.rainxch.core.data.local.db.dao.ExternalLinkDao
import zed.rainxch.core.data.services.DownloadNotificationObserver
import zed.rainxch.core.data.services.PackageEventReceiver
import zed.rainxch.core.data.services.UpdateScheduler
import zed.rainxch.core.domain.model.InstallSource
import zed.rainxch.core.domain.model.InstalledApp
import zed.rainxch.core.domain.repository.ExternalImportRepository
import zed.rainxch.core.domain.repository.InstalledAppsRepository
import zed.rainxch.core.domain.repository.TweaksRepository
import zed.rainxch.core.domain.system.PackageMonitor
import zed.rainxch.githubstore.app.di.initKoin

class GithubStoreApp : Application() {
    private var packageEventReceiver: PackageEventReceiver? = null
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        initKoin {
            androidContext(this@GithubStoreApp)
        }

        createNotificationChannels()
        registerPackageEventReceiver()
        startDownloadNotificationObserver()
        scheduleBackgroundUpdateChecks()
        registerSelfAsInstalledApp()
        scheduleInitialExternalScan()
        scheduleSigningSeedSync()
    }

    private fun scheduleInitialExternalScan() {
        appScope.launch {
            runCatching {
                get<ExternalImportRepository>().scheduleInitialScanIfNeeded()
            }.onFailure {
                Logger.w(it) { "Initial external scan scheduling failed" }
            }
        }
    }

    private fun scheduleSigningSeedSync() {
        appScope.launch {
            runCatching {
                get<ExternalImportRepository>().syncSigningFingerprintSeed()
            }.onFailure {
                Logger.w(it) { "Signing seed sync failed" }
            }
        }
    }

    private fun startDownloadNotificationObserver() {
        get<DownloadNotificationObserver>().start(get<CoroutineScope>())
    }

    private fun createNotificationChannels() {
        val notificationManager = getSystemService(NotificationManager::class.java)

        val updatesChannel =
            NotificationChannel(
                UPDATES_CHANNEL_ID,
                "App Updates",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Notifications when app updates are available"
            }
        notificationManager.createNotificationChannel(updatesChannel)

        val serviceChannel =
            NotificationChannel(
                UPDATE_SERVICE_CHANNEL_ID,
                "Update Service",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Background update check and auto-update progress"
                setShowBadge(false)
            }
        notificationManager.createNotificationChannel(serviceChannel)

        val downloadsChannel =
            NotificationChannel(
                DOWNLOADS_CHANNEL_ID,
                "Downloads",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Live progress for in-flight downloads"
                setShowBadge(false)
            }
        notificationManager.createNotificationChannel(downloadsChannel)
    }

    private fun registerPackageEventReceiver() {
        val receiver =
            PackageEventReceiver(
                installedAppsRepository = get<InstalledAppsRepository>(),
                packageMonitor = get<PackageMonitor>(),
                externalImportRepository = get<ExternalImportRepository>(),
                externalLinkDao = get<ExternalLinkDao>(),
                appScope = get<CoroutineScope>(),
            )
        val filter = PackageEventReceiver.createIntentFilter()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(receiver, filter)
        }

        packageEventReceiver = receiver
    }

    private fun scheduleBackgroundUpdateChecks() {
        appScope.launch {
            try {
                val intervalHours = get<TweaksRepository>().getUpdateCheckInterval().first()
                UpdateScheduler.schedule(
                    context = this@GithubStoreApp,
                    intervalHours = intervalHours,
                )
            } catch (e: Exception) {
                Logger.e(e) { "Failed to schedule background update checks" }
            }
        }
    }

    private fun registerSelfAsInstalledApp() {
        appScope.launch {
            try {
                val repo = get<InstalledAppsRepository>()
                val selfPackageName = packageName
                val existing = repo.getAppByPackage(selfPackageName)

                if (existing != null) {
                    // After a self-update the old process is killed before
                    // ACTION_PACKAGE_REPLACED can be delivered to our own
                    // receiver, so isPendingInstall stays true. Resolve it
                    // here at the earliest startup opportunity.
                    if (existing.isPendingInstall) {
                        resolveSelfPendingInstall(existing, repo)
                    }
                    return@launch
                }

                val packageMonitor = get<PackageMonitor>()
                val systemInfo = packageMonitor.getInstalledPackageInfo(selfPackageName)
                if (systemInfo == null) {
                    Logger.w { "GithubStoreApp: Skip self-registration, package info missing for $selfPackageName" }
                    return@launch
                }

                val now = System.currentTimeMillis()
                val versionName = systemInfo.versionName
                val versionCode = systemInfo.versionCode

                val selfApp =
                    InstalledApp(
                        packageName = selfPackageName,
                        repoId = SELF_REPO_ID,
                        repoName = SELF_REPO_NAME,
                        repoOwner = SELF_REPO_OWNER,
                        repoOwnerAvatarUrl = SELF_AVATAR_URL,
                        repoDescription = "A cross-platform app store for GitHub releases",
                        primaryLanguage = "Kotlin",
                        repoUrl = "https://github.com/$SELF_REPO_OWNER/$SELF_REPO_NAME",
                        installedVersion = versionName,
                        installedAssetName = null,
                        installedAssetUrl = null,
                        latestVersion = null,
                        latestAssetName = null,
                        latestAssetUrl = null,
                        latestAssetSize = null,
                        appName = "GitHub Store",
                        installSource = InstallSource.THIS_APP,
                        installedAt = now,
                        lastCheckedAt = 0L,
                        lastUpdatedAt = now,
                        isUpdateAvailable = false,
                        updateCheckEnabled = true,
                        releaseNotes = null,
                        systemArchitecture = "",
                        fileExtension = "apk",
                        isPendingInstall = false,
                        installedVersionName = versionName,
                        installedVersionCode = versionCode,
                        signingFingerprint = SELF_SHA256_FINGERPRINT,
                    )

                repo.saveInstalledApp(selfApp)
                Logger.i("GitHub Store App: App added")
            } catch (e: Exception) {
                Logger.e(e) { "GitHub Store App: Failed to register self as installed app" }
            }
        }
    }

    /**
     * Resolves a stale `isPendingInstall` flag for the app's own
     * database row. Called on every cold start when the row exists
     * and still has the flag set — the typical scenario after a
     * successful self-update where the broadcast path missed.
     */
    private suspend fun resolveSelfPendingInstall(
        existing: InstalledApp,
        repo: InstalledAppsRepository,
    ) {
        try {
            val packageMonitor = get<PackageMonitor>()
            val systemInfo = packageMonitor.getInstalledPackageInfo(packageName)
            if (systemInfo != null) {
                val latestVersionCode = existing.latestVersionCode ?: 0L
                repo.updateApp(
                    existing.copy(
                        isPendingInstall = false,
                        installedVersionName = systemInfo.versionName,
                        installedVersionCode = systemInfo.versionCode,
                        isUpdateAvailable = latestVersionCode > systemInfo.versionCode,
                    ),
                )
                Logger.i { "Resolved self-update pending install: ${systemInfo.versionName} (code=${systemInfo.versionCode})" }
            } else {
                repo.updatePendingStatus(packageName, false)
                Logger.i { "Resolved self-update pending install (no system info)" }
            }
        } catch (e: Exception) {
            Logger.e(e) { "Failed to resolve self-update pending install" }
        }
    }

    companion object {
        private const val SELF_REPO_ID = 1101281251L
        private const val SELF_SHA256_FINGERPRINT =
            @Suppress("ktlint:standard:max-line-length")
            "B7:F2:8E:19:8E:48:C1:93:B0:38:C6:5D:92:DD:F7:BC:07:7B:0D:B5:9E:BC:9B:25:0A:6D:AC:48:C1:18:03:CA"
        private const val SELF_REPO_OWNER = "OpenHub-Store"
        private const val SELF_REPO_NAME = "GitHub-Store"
        private const val SELF_AVATAR_URL =
            @Suppress("ktlint:standard:max-line-length")
            "https://raw.githubusercontent.com/OpenHub-Store/GitHub-Store/refs/heads/main/media-resources/app_icon.png"
        const val UPDATES_CHANNEL_ID = "app_updates"
        const val UPDATE_SERVICE_CHANNEL_ID = "update_service"
        const val DOWNLOADS_CHANNEL_ID = "app_downloads"
    }
}
