package zed.rainxch.core.data.local

import java.io.File
import zed.rainxch.core.domain.system.DesktopOs

object DesktopAppDataPaths {
    private const val APP_DIR_NAME = "GitHub-Store"

    fun appDataDir(): File {
        val home = File(System.getProperty("user.home"))
        val dir = when {
            DesktopOs.isMac ->
                File(home, "Library/Application Support/$APP_DIR_NAME")

            DesktopOs.isWindows -> {
                val appData = System.getenv("APPDATA")?.let(::File)
                    ?: System.getenv("LOCALAPPDATA")?.let(::File)
                    ?: File(home, "AppData/Roaming")
                File(appData, APP_DIR_NAME)
            }

            else -> {
                val dataHome = System.getenv("XDG_DATA_HOME")?.let(::File)
                    ?: File(home, ".local/share")
                File(dataHome, APP_DIR_NAME)
            }
        }
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun migrateFromTmpIfNeeded(filename: String): Boolean {
        val newFile = File(appDataDir(), filename)
        if (newFile.exists()) return false
        val legacyFile = File(System.getProperty("java.io.tmpdir"), filename)
        if (!legacyFile.exists()) return false
        return try {
            legacyFile.copyTo(newFile, overwrite = false)
            true
        } catch (_: Exception) {
            false
        }
    }
}
