package zed.rainxch.githubstore.desktop

import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.ptr.IntByReference
import com.sun.jna.win32.StdCallLibrary
import java.awt.Window

private interface DwmApi : StdCallLibrary {
    fun DwmSetWindowAttribute(
        hwnd: WinDef.HWND,
        attribute: Int,
        value: WinDef.BOOLByReference,
        size: Int,
    ): Int

    fun DwmSetWindowAttribute(
        hwnd: WinDef.HWND,
        attribute: Int,
        value: IntByReference,
        size: Int,
    ): Int

    companion object {
        val INSTANCE: DwmApi? =
            runCatching {
                Native.load("dwmapi", DwmApi::class.java)
            }.getOrNull()
    }
}

private const val DWMWA_USE_IMMERSIVE_DARK_MODE = 20
private const val DWMWA_USE_IMMERSIVE_DARK_MODE_PRE_20H1 = 19
private const val DWMWA_BORDER_COLOR = 34
private const val DWMWA_CAPTION_COLOR = 35
private const val DWMWA_COLOR_DEFAULT = -1
private const val DARK_TITLE_BAR_COLORREF = 0x001E1E1E
private const val S_OK = 0

private val osName: String
    get() = System.getProperty("os.name").orEmpty().lowercase()

private val isWindows: Boolean
    get() = osName.startsWith("windows")

private val isMac: Boolean
    get() = osName.contains("mac")

fun installMacosSystemAppearance() {
    if (!isMac) return
    if (System.getProperty("apple.awt.application.appearance") == null) {
        System.setProperty("apple.awt.application.appearance", "system")
    }
}

fun applyMacosWindowAppearance(
    window: Window,
    isDark: Boolean,
) {
    if (!isMac) return
    val frame = window as? javax.swing.JFrame ?: return
    val key = "apple.awt.windowAppearance"
    val value = if (isDark) "NSAppearanceNameDarkAqua" else "NSAppearanceNameAqua"
    runCatching { frame.rootPane.putClientProperty(key, value) }
}

fun applyWindowsImmersiveDarkMode(
    window: Window,
    isDark: Boolean,
) {
    if (!isWindows) return
    val dwm = DwmApi.INSTANCE ?: return
    val hwnd =
        runCatching {
            WinDef.HWND(Pointer(Native.getWindowID(window)))
        }.getOrNull() ?: return
    val flag = WinDef.BOOLByReference(WinDef.BOOL(isDark))
    val hr =
        runCatching {
            dwm.DwmSetWindowAttribute(hwnd, DWMWA_USE_IMMERSIVE_DARK_MODE, flag, 4)
        }.getOrElse { -1 }

    if (hr != S_OK) {
        runCatching {
            dwm.DwmSetWindowAttribute(hwnd, DWMWA_USE_IMMERSIVE_DARK_MODE_PRE_20H1, flag, 4)
        }
    }

    val colorRef = IntByReference(if (isDark) DARK_TITLE_BAR_COLORREF else DWMWA_COLOR_DEFAULT)
    runCatching { dwm.DwmSetWindowAttribute(hwnd, DWMWA_CAPTION_COLOR, colorRef, 4) }
    runCatching { dwm.DwmSetWindowAttribute(hwnd, DWMWA_BORDER_COLOR, colorRef, 4) }
}
