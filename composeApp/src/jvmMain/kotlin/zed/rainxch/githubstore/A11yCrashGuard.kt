package zed.rainxch.githubstore

import zed.rainxch.core.domain.system.DesktopOs
import java.awt.AWTEvent
import java.awt.EventQueue
import java.awt.Toolkit
import java.util.concurrent.atomic.AtomicBoolean

object A11yCrashGuard {
    private const val COMPOSE_ACCESSIBILITY_ENABLE = "compose.accessibility.enable"

    private val warnedEdt = AtomicBoolean(false)
    private val warnedUncaught = AtomicBoolean(false)

    fun install() {
        if (!DesktopOs.isMac) return

        disableComposeAccessibilityBridgeByDefault()

        Toolkit.getDefaultToolkit().systemEventQueue.push(FilteringEventQueue())

        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            if (isComposeA11yCrash(throwable)) {
                if (warnedUncaught.compareAndSet(false, true)) {
                    System.err.println(
                        "[A11yCrashGuard] Suppressed Compose a11y crash via uncaught-exception path " +
                            "(known issue, see GitHub-Store#330 / #639 / #640 / #684). Further occurrences silenced.",
                    )
                }
                return@setDefaultUncaughtExceptionHandler
            }

            previous?.uncaughtException(thread, throwable)
                ?: throwable.printStackTrace(System.err)
        }
    }

    private fun disableComposeAccessibilityBridgeByDefault() {
        if (System.getProperty(COMPOSE_ACCESSIBILITY_ENABLE) != null) return

        // Compose MP 1.10.x can still crash on macOS when the AWT accessibility bridge
        // queries detached Compose components. Keep it off unless a user explicitly opts in.
        System.setProperty(COMPOSE_ACCESSIBILITY_ENABLE, "false")
        System.err.println(
            "[A11yCrashGuard] Disabled Compose accessibility bridge on macOS " +
                "(known issue, see GitHub-Store#330 / #639 / #640).",
        )
    }

    private fun isComposeA11yCrash(throwable: Throwable): Boolean {
        var current: Throwable? = throwable
        while (current != null) {
            if (current.stackTrace.any { frame ->
                    frame.className.startsWith("androidx.compose.ui.platform.a11y") ||
                        frame.className.startsWith("sun.lwawt.macosx.CAccessib")
                }
            ) {
                return true
            }
            current = current.cause
        }
        return false
    }

    private class FilteringEventQueue : EventQueue() {
        override fun dispatchEvent(event: AWTEvent) {
            try {
                super.dispatchEvent(event)
            } catch (ex: RuntimeException) {
                if (isComposeA11yCrash(ex)) {
                    if (warnedEdt.compareAndSet(false, true)) {
                        System.err.println(
                            "[A11yCrashGuard] Suppressed Compose a11y crash on macOS " +
                                "(known issue, see GitHub-Store#330 / #639 / #640 / #684). Further occurrences silenced.",
                        )
                    }
                    return
                }
                throw ex
            }
        }
    }
}
