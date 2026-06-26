package zed.rainxch.githubstore

import zed.rainxch.core.domain.system.DesktopOs
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket

object DesktopDeepLink {
    private const val SINGLE_INSTANCE_PORT = 47632
    private const val SCHEME = "githubstore"
    private const val DESKTOP_FILE_NAME = "github-store-deeplink"

    fun registerUriSchemeIfNeeded() {
        when {
            isWindows() -> registerWindows()
            isLinux() -> registerLinux()
        }
    }

    private fun registerWindows() {
        val exePath =
            resolveExePath() ?: run {
                println("DeepLink: skipped Windows scheme registration (exe path unresolved)")
                return
            }

        if (windowsRegistrationIsValid(exePath)) return

        val iconValue = "\"$exePath\",1"
        val commandValue = "\"$exePath\" \"%1\""

        val regContent =
            buildString {
                append("Windows Registry Editor Version 5.00\r\n\r\n")
                append("[HKEY_CURRENT_USER\\SOFTWARE\\Classes\\$SCHEME]\r\n")
                append("@=\"URL:Komi Store Protocol\"\r\n")
                append("\"URL Protocol\"=\"\"\r\n\r\n")
                append("[HKEY_CURRENT_USER\\SOFTWARE\\Classes\\$SCHEME\\DefaultIcon]\r\n")
                append("@=\"${regEscape(iconValue)}\"\r\n\r\n")
                append("[HKEY_CURRENT_USER\\SOFTWARE\\Classes\\$SCHEME\\shell\\open\\command]\r\n")
                append("@=\"${regEscape(commandValue)}\"\r\n")
            }

        val regFile =
            try {
                File.createTempFile("komi-scheme", ".reg")
            } catch (e: Exception) {
                println("DeepLink: Windows scheme registration failed (temp file): ${e.message}")
                return
            }

        try {
            regFile.writeBytes(("\uFEFF$regContent").toByteArray(Charsets.UTF_16LE))
            val result = runCommand("reg", "import", regFile.absolutePath)
            if (result == null) {
                println("DeepLink: Windows scheme registration failed (reg import returned no output)")
            }
        } catch (e: Exception) {
            println("DeepLink: Windows scheme registration failed: ${e.message}")
        } finally {
            runCatching { regFile.delete() }
        }
    }

    private fun windowsRegistrationIsValid(exePath: String): Boolean {
        val protocol = runCommand("reg", "query", "HKCU\\SOFTWARE\\Classes\\$SCHEME", "/v", "URL Protocol")
        if (protocol == null || !protocol.contains("URL Protocol")) return false
        val command = runCommand("reg", "query", "HKCU\\SOFTWARE\\Classes\\$SCHEME\\shell\\open\\command", "/ve")
        return command != null && command.contains(exePath, ignoreCase = true)
    }

    private fun regEscape(value: String): String = value.replace("\\", "\\\\").replace("\"", "\\\"")

    private fun registerLinux() {
        val appsDir = File(System.getProperty("user.home"), ".local/share/applications")
        val desktopFile = File(appsDir, "$DESKTOP_FILE_NAME.desktop")

        if (desktopFile.exists()) return

        val exePath = resolveExePath() ?: return

        appsDir.mkdirs()

        desktopFile.writeText(
            """
            [Desktop Entry]
            Type=Application
            Name=Komi Store
            Exec="$exePath" %u
            Terminal=false
            MimeType=x-scheme-handler/$SCHEME;
            NoDisplay=true
            """.trimIndent(),
        )

        runCommand("xdg-mime", "default", "$DESKTOP_FILE_NAME.desktop", "x-scheme-handler/$SCHEME")
    }

    fun tryForwardToRunningInstance(uri: String): Boolean =
        try {
            Socket("127.0.0.1", SINGLE_INSTANCE_PORT).use { socket ->
                PrintWriter(socket.getOutputStream(), true).println(uri)
            }
            true
        } catch (_: Exception) {
            false
        }

    fun startInstanceListener(onUri: (String) -> Unit) {
        val thread =
            Thread({
                try {
                    val server = ServerSocket(SINGLE_INSTANCE_PORT, 50, InetAddress.getLoopbackAddress())
                    while (true) {
                        val client = server.accept()
                        try {
                            val reader = BufferedReader(InputStreamReader(client.getInputStream()))
                            val uri = reader.readLine()
                            if (!uri.isNullOrBlank()) {
                                onUri(uri.trim())
                            }
                        } catch (_: Exception) {
                        } finally {
                            client.close()
                        }
                    }
                } catch (_: Exception) {
                }
            }, "DeepLinkListener")
        thread.isDaemon = true
        thread.start()
    }

    private fun isWindows(): Boolean = DesktopOs.isWindows

    private fun isLinux(): Boolean = DesktopOs.isLinux

    private fun resolveExePath(): String? {
        System
            .getProperty("jpackage.app-path")
            ?.takeIf { it.isNotBlank() }
            ?.let { return it }
        return try {
            ProcessHandle
                .current()
                .info()
                .command()
                .orElse(null)
                ?.takeIf {
                    it.isNotBlank() &&
                        !it.endsWith("java.exe", ignoreCase = true) &&
                        !it.endsWith("javaw.exe", ignoreCase = true)
                }
        } catch (_: Exception) {
            null
        }
    }

    private fun runCommand(vararg cmd: String): String? =
        try {
            val process =
                ProcessBuilder(*cmd)
                    .redirectErrorStream(true)
                    .start()
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            output
        } catch (_: Exception) {
            null
        }
}
