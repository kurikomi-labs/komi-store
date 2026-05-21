package zed.rainxch.githubstore

import java.awt.AWTEvent
import java.awt.EventQueue
import java.awt.Toolkit
import java.util.concurrent.atomic.AtomicBoolean

object A11yCrashGuard {
    private val warnedEdt = AtomicBoolean(false)
    private val warnedUncaught = AtomicBoolean(false)

    fun install() {
        val osName = System.getProperty("os.name")?.lowercase().orEmpty()
        if (!osName.contains("mac")) return

        Toolkit.getDefaultToolkit().systemEventQueue.push(FilteringEventQueue())

        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            if (isComposeA11yNpe(throwable)) {
                if (warnedUncaught.compareAndSet(false, true)) {
                    System.err.println(
                        "[A11yCrashGuard] Suppressed Compose a11y NPE via uncaught-exception path " +
                            "(known issue, see GitHub-Store#330 / #640). Further occurrences silenced.",
                    )
                }
                return@setDefaultUncaughtExceptionHandler
            }

            previous?.uncaughtException(thread, throwable)
                ?: throwable.printStackTrace(System.err)
        }
    }

    private fun isComposeA11yNpe(throwable: Throwable): Boolean {
        if (throwable !is NullPointerException) return false
        var current: Throwable? = throwable
        while (current != null) {
            if (current.stackTrace.any { frame ->
                    frame.className.startsWith("androidx.compose.ui.platform.a11y") ||

                        frame.className.startsWith("sun.lwawt.macosx.CAccessible\$AXChangeNotifier")
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
            } catch (npe: NullPointerException) {
                if (isComposeA11yNpe(npe)) {
                    if (warnedEdt.compareAndSet(false, true)) {
                        System.err.println(
                            "[A11yCrashGuard] Suppressed Compose a11y NPE on macOS " +
                                "(known issue, see GitHub-Store#330 / #640). Further occurrences silenced.",
                        )
                    }
                    return
                }
                throw npe
            }
        }
    }
}
