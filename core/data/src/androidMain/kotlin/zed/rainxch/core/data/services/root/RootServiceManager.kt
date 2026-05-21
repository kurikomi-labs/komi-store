package zed.rainxch.core.data.services.root

import android.os.ParcelFileDescriptor
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import zed.rainxch.core.data.services.root.model.RootStatus
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

class RootServiceManager(
    private val scope: CoroutineScope,
) {
    private val _status = MutableStateFlow(RootStatus.NOT_AVAILABLE)
    val status: StateFlow<RootStatus> = _status.asStateFlow()

    @Volatile
    private var cachedSuPath: String? = null

    fun initialize() {

        scope.launch(Dispatchers.IO) {
            refreshStatusBlocking()
        }
    }

    fun refreshStatus() {
        scope.launch(Dispatchers.IO) { refreshStatusBlocking() }
    }

    fun requestPermission() {
        scope.launch(Dispatchers.IO) {
            val su = cachedSuPath ?: locateSuBinary()?.path ?: run {
                Logger.d(TAG) { "requestPermission() — no su binary on device, skipping" }
                refreshStatusBlocking()
                return@launch
            }
            try {
                Logger.d(TAG) { "requestPermission() — invoking '$su -c true' to surface root prompt" }
                val proc = Runtime.getRuntime().exec(arrayOf(su, "-c", "true"))
                proc.waitFor(PROMPT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                if (proc.isAlive) {
                    Logger.w(TAG) { "requestPermission() — prompt invocation still running after ${PROMPT_TIMEOUT_SECONDS}s, destroying" }
                    proc.destroyForcibly()
                }
            } catch (e: Exception) {
                Logger.w(TAG) { "requestPermission() failed: ${e.javaClass.simpleName}: ${e.message}" }
            }
            refreshStatusBlocking()
        }
    }

    suspend fun installPackage(
        apkFile: File,
        installerPackageName: String?,
    ): Int? =
        withContext(Dispatchers.IO) {
            val su = cachedSuPath ?: locateSuBinary()?.path ?: run {
                Logger.w(TAG) { "installPackage() — no su binary available" }
                return@withContext null
            }

            val safeInstaller = installerPackageName?.takeIf { it.isNotBlank() }
            if (safeInstaller != null && !PACKAGE_NAME_PATTERN.matches(safeInstaller)) {
                Logger.w(TAG) {
                    "installPackage() — rejecting non-conformant installerPackageName='$safeInstaller'"
                }
                return@withContext STATUS_FAILURE
            }
            val pm = "/system/bin/pm"

            val command = buildString {
                append(pm).append(" install ")
                if (safeInstaller != null) append("-i ").append(safeInstaller).append(' ')
                append("-S ").append(apkFile.length()).append(' ')
                append('-')
            }
            Logger.d(TAG) { "installPackage() — executing via $su: $command" }
            val proc = try {
                Runtime.getRuntime().exec(arrayOf(su, "-c", command))
            } catch (e: Exception) {
                Logger.e(TAG) { "installPackage() — su exec failed: ${e.message}" }
                return@withContext null
            }

            val stdoutBuf = StringBuilder()
            val stderrBuf = StringBuilder()
            val stdoutThread = Thread {
                try {
                    BufferedReader(InputStreamReader(proc.inputStream)).use { reader ->
                        reader.forEachLine { stdoutBuf.append(it).append('\n') }
                    }
                } catch (_: Exception) {
                }
            }
            val stderrThread = Thread {
                try {
                    BufferedReader(InputStreamReader(proc.errorStream)).use { reader ->
                        reader.forEachLine { stderrBuf.append(it).append('\n') }
                    }
                } catch (_: Exception) {
                }
            }
            stdoutThread.start()
            stderrThread.start()

            val pipeError = StringBuilder()
            val pipeThread = Thread {
                try {
                    apkFile.inputStream().use { input ->
                        proc.outputStream.use { output ->
                            input.copyTo(output)
                        }
                    }
                } catch (e: Exception) {
                    pipeError.append(e.javaClass.simpleName).append(": ").append(e.message)
                    Logger.e(TAG) { "installPackage() — stdin pipe failed: $pipeError" }
                }
            }
            pipeThread.start()
            pipeThread.join(INSTALL_TIMEOUT_SECONDS * 1000L)
            if (pipeThread.isAlive) {
                Logger.e(TAG) { "installPackage() — pipe thread still alive after ${INSTALL_TIMEOUT_SECONDS}s, destroying process" }
                pipeThread.interrupt()
                proc.destroyForcibly()
                stdoutThread.join(READER_DRAIN_TIMEOUT_MS)
                stderrThread.join(READER_DRAIN_TIMEOUT_MS)
                return@withContext STATUS_FAILURE
            }

            val finished = proc.waitFor(INSTALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            if (!finished) {
                Logger.e(TAG) { "installPackage() — pm process timed out, destroying" }
                proc.destroyForcibly()
                stdoutThread.join(READER_DRAIN_TIMEOUT_MS)
                stderrThread.join(READER_DRAIN_TIMEOUT_MS)
                return@withContext STATUS_FAILURE
            }

            stdoutThread.join(READER_DRAIN_TIMEOUT_MS)
            stderrThread.join(READER_DRAIN_TIMEOUT_MS)

            val stdout = stdoutBuf.toString().trim()
            val stderr = stderrBuf.toString().trim()
            val exit = proc.exitValue()
            Logger.d(TAG) { "installPackage() — exit=$exit stdout='$stdout' stderr='$stderr'" }
            if (exit == 0 && stdout.contains("Success")) {
                STATUS_SUCCESS
            } else {
                Logger.w(TAG) { "installPackage() — pm reported failure: stdout='$stdout' stderr='$stderr'" }
                STATUS_FAILURE
            }
        }

    suspend fun uninstallPackage(packageName: String): Int? =
        withContext(Dispatchers.IO) {
            val su = cachedSuPath ?: locateSuBinary()?.path ?: return@withContext null
            if (!PACKAGE_NAME_PATTERN.matches(packageName)) {
                Logger.w(TAG) {
                    "uninstallPackage() — rejecting non-conformant packageName='$packageName'"
                }
                return@withContext STATUS_FAILURE
            }
            val command = "/system/bin/pm uninstall $packageName"
            try {
                val proc = Runtime.getRuntime().exec(arrayOf(su, "-c", command))
                val stdoutBuf = StringBuilder()
                val stdoutThread = Thread {
                    try {
                        BufferedReader(InputStreamReader(proc.inputStream)).use { reader ->
                            reader.forEachLine { stdoutBuf.append(it).append('\n') }
                        }
                    } catch (_: Exception) {
                    }
                }
                stdoutThread.start()
                val finished = proc.waitFor(UNINSTALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                if (!finished) {
                    proc.destroyForcibly()
                    stdoutThread.join(READER_DRAIN_TIMEOUT_MS)
                    return@withContext STATUS_FAILURE
                }
                stdoutThread.join(READER_DRAIN_TIMEOUT_MS)
                val stdout = stdoutBuf.toString().trim()
                val exit = proc.exitValue()
                Logger.d(TAG) { "uninstallPackage($packageName) — exit=$exit stdout='$stdout'" }
                if (exit == 0 && stdout.contains("Success")) STATUS_SUCCESS else STATUS_FAILURE
            } catch (e: Exception) {
                Logger.e(TAG) { "uninstallPackage($packageName) — su exec failed: ${e.message}" }
                STATUS_FAILURE
            }
        }

    private fun refreshStatusBlocking() {
        val computed = computeStatus()
        if (_status.value != computed) {
            Logger.d(TAG) { "refreshStatus() — $computed (was ${_status.value})" }
        }
        _status.value = computed
    }

    private fun computeStatus(): RootStatus {
        val probe = locateSuBinary()
        if (probe == null) {
            cachedSuPath = null
            return RootStatus.NOT_AVAILABLE
        }
        cachedSuPath = probe.path
        return when (probe.kind) {
            ProbeResultKind.UID_ZERO -> RootStatus.READY
            ProbeResultKind.NOT_ZERO -> RootStatus.PERMISSION_NEEDED
        }
    }

    private fun locateSuBinary(): SuProbe? {

        var firstNotZero: SuProbe? = null
        for (path in SU_PATHS) {
            val result = probeSu(path) ?: continue
            if (result == ProbeResultKind.UID_ZERO) {
                return SuProbe(path = path, kind = result)
            }
            if (firstNotZero == null) {
                firstNotZero = SuProbe(path = path, kind = result)
            }
        }
        return firstNotZero
    }

    private fun probeSu(path: String): ProbeResultKind? =
        try {
            val proc = Runtime.getRuntime().exec(arrayOf(path, "-c", "id"))
            val finished = proc.waitFor(PROBE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            if (!finished) {
                Logger.d(TAG) { "probeSu($path) timed out, treating as PERMISSION_NEEDED" }
                proc.destroyForcibly()
                ProbeResultKind.NOT_ZERO
            } else {
                val output =
                    BufferedReader(InputStreamReader(proc.inputStream)).use {
                        it.readText().trim()
                    }
                if (proc.exitValue() == 0 && output.contains("uid=0")) {
                    ProbeResultKind.UID_ZERO
                } else {
                    ProbeResultKind.NOT_ZERO
                }
            }
        } catch (_: java.io.IOException) {

            null
        } catch (e: Exception) {
            Logger.w(TAG) { "probeSu($path) threw: ${e.javaClass.simpleName}: ${e.message}" }
            null
        }

    private data class SuProbe(
        val path: String,
        val kind: ProbeResultKind,
    )

    private enum class ProbeResultKind {
        UID_ZERO,
        NOT_ZERO,
    }

    companion object {
        private const val TAG = "RootServiceManager"
        private const val STATUS_SUCCESS = 0
        private const val STATUS_FAILURE = -1
        private const val INSTALL_TIMEOUT_SECONDS = 120L
        private const val UNINSTALL_TIMEOUT_SECONDS = 30L
        private const val PROBE_TIMEOUT_SECONDS = 5L
        private const val PROMPT_TIMEOUT_SECONDS = 60L
        private const val READER_DRAIN_TIMEOUT_MS = 1_000L

        private val PACKAGE_NAME_PATTERN =
            Regex("""^[A-Za-z][A-Za-z0-9_]*(\.[A-Za-z][A-Za-z0-9_]*)+$""")

        private val SU_PATHS =
            listOf(
                "/system/bin/su",
                "/system/xbin/su",
                "/sbin/su",
                "/su/bin/su",
                "/magisk/.core/bin/su",
                "/data/adb/magisk/su",
                "/data/adb/ksu/bin/su",
                "/data/adb/ap/su",
            )
    }
}
