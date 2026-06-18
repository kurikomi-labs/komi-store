package zed.rainxch.githubstore

import zed.rainxch.core.domain.system.DesktopOs
import java.io.File
import java.io.FileOutputStream
import java.io.PrintStream
import java.io.PrintWriter
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object CrashReporter {
    private const val MAX_SESSION_LOG_BYTES = 5L * 1024 * 1024

    private val logDir: File by lazy { resolveLogDir().also { it.mkdirs() } }

    fun install() {
        val teed =
            runCatching {
                val file = File(logDir, "session.log")
                rotateIfLarge(file)
                PrintStream(FileOutputStream(file, true), true, Charsets.UTF_8)
                    .also { stream ->
                        System.setOut(TeePrintStream(System.out, stream))
                        System.setErr(TeePrintStream(System.err, stream))
                    }
            }.getOrNull()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching { writeCrashDump(thread, throwable) }
            runCatching { throwable.printStackTrace(System.err) }
        }

        if (teed != null) {
            println("=== Komi Store session ${Instant.now()} ===")
            println(
                "OS=${DesktopOs.rawName} ${DesktopOs.version} " +
                    "(${System.getProperty("os.arch")})",
            )
            println(
                "Java=${System.getProperty("java.version")} (${System.getProperty("java.vendor")})",
            )
            println("LogDir=${logDir.absolutePath}")
        }
    }

    private fun writeCrashDump(
        thread: Thread,
        throwable: Throwable,
    ) {
        val file = File(logDir, "crash-${timestamp()}.log")
        PrintWriter(file, Charsets.UTF_8).use { writer ->
            writer.println("=== Komi Store crash ===")
            writer.println("Time: ${Instant.now()}")
            writer.println("Thread: ${thread.name}")
            writer.println(
                "OS: ${DesktopOs.rawName} ${DesktopOs.version} " +
                    "(${System.getProperty("os.arch")})",
            )
            writer.println(
                "Java: ${System.getProperty("java.version")} (${System.getProperty("java.vendor")})",
            )
            writer.println()
            throwable.printStackTrace(writer)
        }
    }

    private fun rotateIfLarge(file: File) {
        if (!file.exists() || file.length() <= MAX_SESSION_LOG_BYTES) return
        val rotated = File(file.parentFile, "session.1.log")
        if (rotated.exists()) rotated.delete()
        file.renameTo(rotated)
    }

    private fun resolveLogDir(): File {
        val home = File(System.getProperty("user.home"))
        return when {
            DesktopOs.isMac -> {
                File(home, "Library/Logs/GitHub-Store")
            }

            DesktopOs.isWindows -> {
                val localAppData = System.getenv("LOCALAPPDATA")?.let(::File) ?: home
                File(localAppData, "GitHub-Store/logs")
            }

            else -> {
                val stateHome =
                    System.getenv("XDG_STATE_HOME")?.let(::File)
                        ?: File(home, ".local/state")
                File(stateHome, "GitHub-Store/logs")
            }
        }
    }

    private fun timestamp(): String =
        DateTimeFormatter
            .ofPattern("yyyyMMdd-HHmmss-SSS")
            .withZone(ZoneId.systemDefault())
            .format(Instant.now())
}

private class TeePrintStream(
    private val primary: PrintStream,
    private val secondary: PrintStream,
) : PrintStream(primary) {
    override fun write(b: Int) {
        primary.write(b)
        runCatching { secondary.write(b) }
    }

    override fun write(
        buf: ByteArray,
        off: Int,
        len: Int,
    ) {
        primary.write(buf, off, len)
        runCatching { secondary.write(buf, off, len) }
    }

    override fun flush() {
        primary.flush()
        runCatching { secondary.flush() }
    }
}
