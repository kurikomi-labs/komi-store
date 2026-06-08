package zed.rainxch.core.domain.use_cases

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import zed.rainxch.core.domain.logging.GitHubStoreLogger
import zed.rainxch.core.domain.model.installation.InstalledApp
import zed.rainxch.core.domain.model.system.Platform
import zed.rainxch.core.domain.repository.InstalledAppsRepository
import zed.rainxch.core.domain.system.PackageMonitor
import zed.rainxch.core.domain.utils.executeInTransaction

class SyncInstalledAppsUseCase(
    private val packageMonitor: PackageMonitor,
    private val installedAppsRepository: InstalledAppsRepository,
    private val platform: Platform,
    private val logger: GitHubStoreLogger,
) {
    companion object {
        private const val PENDING_TIMEOUT_MS = 24 * 60 * 60 * 1000L
    }

    suspend operator fun invoke(): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val installedPackageNames = packageMonitor.getAllInstalledPackageNames()
                val appsInDb = installedAppsRepository.getAllInstalledApps().first()
                val now = System.currentTimeMillis()

                val toDelete = mutableListOf<String>()
                val toMigrate = mutableListOf<Pair<String, MigrationResult>>()
                val toResolvePending = mutableListOf<InstalledApp>()
                val toDeleteStalePending = mutableListOf<String>()
                val toSyncVersions = mutableListOf<InstalledApp>()

                val toClearStaleParkedFile = mutableListOf<InstalledApp>()

                appsInDb.forEach { app ->
                    val isOnSystem = installedPackageNames.contains(app.packageName)
                    when {
                        app.isPendingInstall -> {
                            if (isOnSystem) {
                                toResolvePending.add(app)
                            } else if (now - app.installedAt > PENDING_TIMEOUT_MS) {
                                toDeleteStalePending.add(app.packageName)
                            }
                        }

                        !isOnSystem -> {
                            toDelete.add(app.packageName)
                        }

                        app.installedVersionName == null -> {
                            val migrationResult = determineMigrationData(app)
                            toMigrate.add(app.packageName to migrationResult)
                        }

                        isOnSystem && platform == Platform.ANDROID -> {
                            toSyncVersions.add(app)
                        }
                    }
                    if (isOnSystem && !app.isPendingInstall && app.pendingInstallFilePath != null) {
                        toClearStaleParkedFile.add(app)
                    }
                }

                executeInTransaction {
                    toDelete.forEach { packageName ->
                        try {
                            installedAppsRepository.deleteInstalledApp(packageName)
                            logger.info("Removed uninstalled app: $packageName")
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            logger.error("Failed to delete $packageName: ${e.message}")
                        }
                    }

                    toDeleteStalePending.forEach { packageName ->
                        try {
                            installedAppsRepository.deleteInstalledApp(packageName)
                            logger.info("Removed stale pending install (>24h): $packageName")
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            logger.error("Failed to delete stale pending $packageName: ${e.message}")
                        }
                    }

                    toResolvePending.forEach { app ->
                        try {
                            val systemInfo = packageMonitor.getInstalledPackageInfo(app.packageName)
                            if (systemInfo != null) {
                                val latestVersionCode = app.latestVersionCode ?: 0L

                                val resolvedTag = app.latestVersion ?: systemInfo.versionName
                                installedAppsRepository.updateApp(
                                    app.copy(
                                        isPendingInstall = false,
                                        installedVersion = resolvedTag,
                                        installedVersionName = systemInfo.versionName,
                                        installedVersionCode = systemInfo.versionCode,
                                        isUpdateAvailable = latestVersionCode > systemInfo.versionCode,
                                    ),
                                )
                                logger.info(
                                    "Resolved pending install: ${app.packageName} (v${systemInfo.versionName}, code=${systemInfo.versionCode}, tag=$resolvedTag)",
                                )
                            } else {
                                installedAppsRepository.updatePendingStatus(app.packageName, false)
                                logger.info("Resolved pending install (no system info): ${app.packageName}")
                            }

                            installedAppsRepository.setPendingInstallFilePath(
                                packageName = app.packageName,
                                path = null,
                            )
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            logger.error("Failed to resolve pending ${app.packageName}: ${e.message}")
                        }
                    }

                    toClearStaleParkedFile.forEach { app ->
                        try {
                            installedAppsRepository.setPendingInstallFilePath(
                                packageName = app.packageName,
                                path = null,
                            )
                            logger.info(
                                "Cleared stale parked-file metadata for already-installed ${app.packageName}",
                            )
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            logger.error(
                                "Failed to clear stale parked-file for ${app.packageName}: ${e.message}",
                            )
                        }
                    }

                    toMigrate.forEach { (packageName, migrationResult) ->
                        try {
                            val app = appsInDb.find { it.packageName == packageName } ?: return@forEach

                            installedAppsRepository.updateApp(
                                app.copy(
                                    installedVersionName = migrationResult.versionName,
                                    installedVersionCode = migrationResult.versionCode,
                                    latestVersionName = migrationResult.versionName,
                                    latestVersionCode = migrationResult.versionCode,
                                ),
                            )

                            logger.info(
                                "Migrated $packageName: ${migrationResult.source} " +
                                    "(versionName=${migrationResult.versionName}, code=${migrationResult.versionCode})",
                            )
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            logger.error("Failed to migrate $packageName: ${e.message}")
                        }
                    }

                    toSyncVersions.forEach { app ->
                        try {
                            val systemInfo = packageMonitor.getInstalledPackageInfo(app.packageName)
                            if (systemInfo != null && systemInfo.versionCode != app.installedVersionCode) {
                                val wasDowngrade = systemInfo.versionCode < app.installedVersionCode
                                val latestVersionCode = app.latestVersionCode ?: 0L
                                val isUpdateAvailable = latestVersionCode > systemInfo.versionCode

                                installedAppsRepository.updateApp(
                                    app.copy(
                                        installedVersionName = systemInfo.versionName,
                                        installedVersionCode = systemInfo.versionCode,
                                        installedVersion = systemInfo.versionName,
                                        isUpdateAvailable = isUpdateAvailable,
                                    ),
                                )

                                val action = if (wasDowngrade) "downgrade" else "external update"
                                logger.info(
                                    "Detected $action for ${app.packageName}: " +
                                        "DB v${app.installedVersionName}(${app.installedVersionCode}) → " +
                                        "System v${systemInfo.versionName}(${systemInfo.versionCode}), " +
                                        "updateAvailable=$isUpdateAvailable",
                                )
                            }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            logger.error("Failed to sync version for ${app.packageName}: ${e.message}")
                        }
                    }
                }

                logger.info(
                    "Sync completed: ${toDelete.size} deleted, ${toDeleteStalePending.size} stale pending removed, " +
                        "${toResolvePending.size} pending resolved, ${toMigrate.size} migrated, " +
                        "${toSyncVersions.size} version-checked, " +
                        "${toClearStaleParkedFile.size} stale parked files cleared",
                )

                Result.success(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.error("Sync failed: ${e.message}")
                Result.failure(e)
            }
        }

    private suspend fun determineMigrationData(app: InstalledApp): MigrationResult =
        if (platform == Platform.ANDROID) {
            val systemInfo = packageMonitor.getInstalledPackageInfo(app.packageName)
            if (systemInfo != null) {
                MigrationResult(
                    versionName = systemInfo.versionName,
                    versionCode = systemInfo.versionCode,
                    source = "system package manager",
                )
            } else {
                MigrationResult(
                    versionName = app.installedVersion,
                    versionCode = 0L,
                    source = "fallback to release tag",
                )
            }
        } else {
            MigrationResult(
                versionName = app.installedVersion,
                versionCode = 0L,
                source = "desktop fallback to release tag",
            )
        }

    private data class MigrationResult(
        val versionName: String,
        val versionCode: Long,
        val source: String,
    )
}
