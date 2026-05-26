package zed.rainxch.githubstore.desktop

import com.sun.jna.Native
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.win32.StdCallLibrary
import java.awt.Window

private interface DwmApi : StdCallLibrary {
    fun DwmSetWindowAttribute(
        hwnd: WinDef.HWND,
        attribute: Int,
        value: WinDef.BOOLByReference,
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

private val isWindows: Boolean
    get() =
        System
            .getProperty("os.name")
            .orEmpty()
            .lowercase()
            .startsWith("windows")

fun applyWindowsImmersiveDarkMode(
    window: Window,
    isDark: Boolean,
) {
    if (!isWindows) return
    val dwm = DwmApi.INSTANCE ?: return
    val hwnd =
        runCatching {
            WinDef.HWND(com.sun.jna.Pointer(Native.getWindowID(window)))
        }.getOrNull() ?: return
    val flag = WinDef.BOOLByReference(WinDef.BOOL(isDark))
    runCatching { dwm.DwmSetWindowAttribute(hwnd, DWMWA_USE_IMMERSIVE_DARK_MODE, flag, 4) }
        .onFailure {
            runCatching {
                dwm.DwmSetWindowAttribute(hwnd, DWMWA_USE_IMMERSIVE_DARK_MODE_PRE_20H1, flag, 4)
            }
        }
}
