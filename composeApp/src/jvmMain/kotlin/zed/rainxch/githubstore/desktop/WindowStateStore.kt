package zed.rainxch.githubstore.desktop

import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.awt.GraphicsEnvironment
import java.awt.Rectangle
import java.awt.Toolkit
import java.io.File

@Serializable
private data class PersistedWindowState(
    val x: Float? = null,
    val y: Float? = null,
    val width: Float,
    val height: Float,
    val placement: String,
)

object WindowStateStore {
    private const val FILE_NAME = "window-state.json"
    private const val MIN_WIDTH = 480f
    private const val MIN_HEIGHT = 600f
    private const val DEFAULT_WIDTH = 1280f
    private const val DEFAULT_HEIGHT = 840f
    private const val VISIBLE_TITLEBAR_X_INSET = 40
    private const val VISIBLE_TITLEBAR_Y_INSET = 20

    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    private val configFile: File by lazy {
        val home = File(System.getProperty("user.home"))
        val osName = System.getProperty("os.name").orEmpty().lowercase()
        val dir =
            when {
                "mac" in osName -> {
                    File(home, "Library/Application Support/GitHub-Store")
                }

                "win" in osName -> {
                    val appData = System.getenv("APPDATA")?.let(::File) ?: File(home, "AppData/Roaming")
                    File(appData, "GitHub-Store")
                }

                else -> {
                    val xdg =
                        System
                            .getenv("XDG_CONFIG_HOME")
                            ?.takeIf { it.isNotBlank() }
                            ?.let(::File)
                            ?: File(home, ".config")
                    File(xdg, "GitHub-Store")
                }
            }
        File(dir, FILE_NAME)
    }

    fun load(): WindowState {
        val saved =
            runCatching {
                configFile
                    .takeIf { it.isFile }
                    ?.readText()
                    ?.let { json.decodeFromString<PersistedWindowState>(it) }
            }.getOrNull()
        val width = (saved?.width ?: DEFAULT_WIDTH).coerceAtLeast(MIN_WIDTH)
        val height = (saved?.height ?: DEFAULT_HEIGHT).coerceAtLeast(MIN_HEIGHT)
        val placement = parsePlacement(saved?.placement)
        val savedX = saved?.x
        val savedY = saved?.y
        val position =
            if (
                savedX != null &&
                savedY != null &&
                isTitleBarVisible(savedX, savedY, width, height)
            ) {
                WindowPosition(savedX.dp, savedY.dp)
            } else {
                WindowPosition.Aligned(Alignment.Center)
            }
        return WindowState(
            placement = placement,
            position = position,
            size = DpSize(width.dp, height.dp),
        )
    }

    fun save(state: WindowState) {
        if (!state.size.isSpecified) return
        val width = state.size.width.value
        val height = state.size.height.value
        if (width.isNaN() || height.isNaN()) return
        runCatching {
            val pos = state.position
            val (x, y) =
                if (pos is WindowPosition.Absolute) {
                    pos.x.value to pos.y.value
                } else {
                    null to null
                }
            val persisted =
                PersistedWindowState(
                    x = x,
                    y = y,
                    width = width,
                    height = height,
                    placement = state.placement.name,
                )
            val parent = configFile.parentFile
            if (parent != null && !parent.isDirectory) parent.mkdirs()
            configFile.writeText(json.encodeToString(persisted))
        }
    }

    private fun parsePlacement(name: String?): WindowPlacement =
        when (name) {
            WindowPlacement.Maximized.name -> WindowPlacement.Maximized
            WindowPlacement.Fullscreen.name -> WindowPlacement.Fullscreen
            else -> WindowPlacement.Floating
        }

    private fun isTitleBarVisible(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
    ): Boolean {
        if (width <= 0f || height <= 0f) return false
        val rect = Rectangle(x.toInt(), y.toInt(), width.toInt(), height.toInt())
        val ge =
            runCatching {
                GraphicsEnvironment.getLocalGraphicsEnvironment()
            }.getOrNull() ?: return false
        val toolkit = runCatching { Toolkit.getDefaultToolkit() }.getOrNull()
        return ge.screenDevices.any { device ->
            device.configurations.any { cfg ->
                val usable =
                    runCatching {
                        val insets = toolkit?.getScreenInsets(cfg)
                        if (insets != null) {
                            Rectangle(
                                cfg.bounds.x + insets.left,
                                cfg.bounds.y + insets.top,
                                cfg.bounds.width - insets.left - insets.right,
                                cfg.bounds.height - insets.top - insets.bottom,
                            )
                        } else {
                            cfg.bounds
                        }
                    }.getOrDefault(cfg.bounds)
                usable.intersects(rect) &&
                    usable.contains(
                        rect.x + VISIBLE_TITLEBAR_X_INSET,
                        rect.y + VISIBLE_TITLEBAR_Y_INSET,
                    )
            }
        }
    }
}
