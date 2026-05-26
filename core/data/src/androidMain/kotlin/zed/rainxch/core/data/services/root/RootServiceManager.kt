package zed.rainxch.core.data.services.root

import co.touchlab.kermit.Logger
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import zed.rainxch.core.data.services.root.model.RootStatus
import java.io.File

class RootServiceManager(
    private val scope: CoroutineScope,
) {
    private val _status = MutableStateFlow(RootStatus.NOT_AVAILABLE)
    val status: StateFlow<RootStatus> = _status.asStateFlow()

    @Volatile
    private var configured = false

    fun initialize() {
        configureDefaultShell()
        scope.launch(Dispatchers.IO) { refreshStatusBlocking() }
    }

    fun refreshStatus() {
        configureDefaultShell()
        scope.launch(Dispatchers.IO) { refreshStatusBlocking() }
    }

    fun requestPermission() {
        configureDefaultShell()
        scope.launch(Dispatchers.IO) {
            Logger.d(TAG) { "requestPermission() — forcing main shell creation" }
            try {
                val shell = Shell.getShell()
                Logger.d(TAG) { "requestPermission() — shell rootStatus=${shell.status}" }
            } catch (e: Exception) {
                Logger.w(TAG) { "requestPermission() — getShell failed: ${e.javaClass.simpleName}: ${e.message}" }
            }
            refreshStatusBlocking()
        }
    }

    suspend fun installPackage(
        apkFile: File,
        installerPackageName: String?,
    ): Int? = withContext(Dispatchers.IO) {
        configureDefaultShell()
        if (!isRootGranted()) {
            Logger.w(TAG) { "installPackage() — root not granted, aborting" }
            return@withContext null
        }
        val safeInstaller = installerPackageName?.takeIf { it.isNotBlank() }
        if (safeInstaller != null && !PACKAGE_NAME_PATTERN.matches(safeInstaller)) {
            Logger.w(TAG) {
                "installPackage() — rejecting non-conformant installerPackageName='$safeInstaller'"
            }
            return@withContext STATUS_FAILURE
        }

        val tmpPath = "/data/local/tmp/ghs_${System.currentTimeMillis()}_${(0..Int.MAX_VALUE).random()}.apk"
        val srcPath = shellQuote(apkFile.absolutePath)
        val tmpPathQuoted = shellQuote(tmpPath)

        val copyRes = Shell.cmd("cp $srcPath $tmpPathQuoted && chmod 644 $tmpPathQuoted").exec()
        if (!copyRes.isSuccess) {
            Logger.e(TAG) {
                "installPackage() — staging copy failed: exit=${copyRes.code} out='${copyRes.out.joinToString("\n")}' err='${copyRes.err.joinToString("\n")}'"
            }
            return@withContext STATUS_FAILURE
        }

        try {
            val command = buildString {
                append("pm install ")
                if (safeInstaller != null) append("-i ").append(safeInstaller).append(' ')
                append("-r ")
                append(tmpPathQuoted)
            }
            Logger.d(TAG) { "installPackage() — executing: $command" }
            val result = Shell.cmd(command).exec()
            val stdout = result.out.joinToString("\n").trim()
            val stderr = result.err.joinToString("\n").trim()
            Logger.d(TAG) { "installPackage() — exit=${result.code} stdout='$stdout' stderr='$stderr'" }
            if (result.isSuccess && stdout.contains("Success")) {
                STATUS_SUCCESS
            } else {
                Logger.w(TAG) { "installPackage() — pm reported failure: stdout='$stdout' stderr='$stderr'" }
                STATUS_FAILURE
            }
        } finally {
            Shell.cmd("rm -f $tmpPathQuoted").submit()
        }
    }

    suspend fun uninstallPackage(packageName: String): Int? = withContext(Dispatchers.IO) {
        configureDefaultShell()
        if (!isRootGranted()) {
            Logger.w(TAG) { "uninstallPackage() — root not granted, aborting" }
            return@withContext null
        }
        if (!PACKAGE_NAME_PATTERN.matches(packageName)) {
            Logger.w(TAG) {
                "uninstallPackage() — rejecting non-conformant packageName='$packageName'"
            }
            return@withContext STATUS_FAILURE
        }
        val result = Shell.cmd("pm uninstall $packageName").exec()
        val stdout = result.out.joinToString("\n").trim()
        Logger.d(TAG) { "uninstallPackage($packageName) — exit=${result.code} stdout='$stdout'" }
        if (result.isSuccess && stdout.contains("Success")) STATUS_SUCCESS else STATUS_FAILURE
    }

    private fun refreshStatusBlocking() {
        val computed = computeStatus()
        if (_status.value != computed) {
            Logger.d(TAG) { "refreshStatus() — $computed (was ${_status.value})" }
        }
        _status.value = computed
    }

    private fun computeStatus(): RootStatus {
        val granted = Shell.isAppGrantedRoot()
        return when (granted) {
            true -> RootStatus.READY
            false -> RootStatus.PERMISSION_NEEDED
            null -> RootStatus.NOT_AVAILABLE
        }
    }

    private fun isRootGranted(): Boolean = Shell.isAppGrantedRoot() == true

    private fun configureDefaultShell() {
        if (configured) return
        synchronized(this) {
            if (configured) return
            try {
                Shell.setDefaultBuilder(
                    Shell.Builder.create()
                        .setFlags(Shell.FLAG_MOUNT_MASTER)
                        .setTimeout(SHELL_TIMEOUT_SECONDS),
                )
            } catch (e: IllegalStateException) {
                Logger.d(TAG) { "configureDefaultShell() — main shell already constructed, skipping setDefaultBuilder" }
            }
            configured = true
        }
    }

    private fun shellQuote(value: String): String = "'" + value.replace("'", "'\\''") + "'"

    companion object {
        private const val TAG = "RootServiceManager"
        private const val STATUS_SUCCESS = 0
        private const val STATUS_FAILURE = -1
        private const val SHELL_TIMEOUT_SECONDS = 10L

        private val PACKAGE_NAME_PATTERN =
            Regex("""^[A-Za-z][A-Za-z0-9_]*(\.[A-Za-z][A-Za-z0-9_]*)+$""")
    }
}
