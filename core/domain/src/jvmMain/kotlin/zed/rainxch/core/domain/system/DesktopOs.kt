package zed.rainxch.core.domain.system

object DesktopOs {
    val rawName: String = System.getProperty("os.name").orEmpty()
    val version: String = System.getProperty("os.version").orEmpty()

    private val normalized: String = rawName.lowercase()

    val isWindows: Boolean = normalized.contains("win")
    val isMac: Boolean = normalized.contains("mac") || normalized.contains("darwin")
    val isLinux: Boolean = normalized.contains("linux")
}
