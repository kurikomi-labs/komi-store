package zed.rainxch.githubstore

import java.awt.AWTEvent
import java.awt.EventQueue
import java.awt.Toolkit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Workaround for a Compose Multiplatform 1.10.x NPE on macOS where the native
 * AX bridge (`sun.lwawt.macosx.CAccessible$AXChangeNotifier`) queries a
 * Compose semantic node that has already been removed by Compose's own
 * accessibility sync loop. The stack trace fingerprint is:
 *
 *     androidx.compose.ui.platform.a11y.SemanticsOwnerAccessibility.accessibleParentOf
 *        -> sun.lwawt.macosx.CAccessible$AXChangeNotifier.propertyChange
 *
 * The NPE surfaces via two propagation paths and both are guarded here:
 *
 * 1. **EDT path** — NPE escapes `DispatchedTask.run` and propagates through
 *    the AWT event queue dispatch chain. [FilteringEventQueue] swallows it so
 *    the EDT keeps draining events.
 *
 * 2. **Coroutine-failure path** — `BaseContinuationImpl.resumeWith` catches the
 *    NPE and routes the coroutine failure through `handleCoroutineException` to
 *    the default uncaught-exception handler, bypassing [FilteringEventQueue].
 *    The handler wrapper installed in [install] intercepts this path before it
 *    reaches [CrashReporter], preventing a spurious crash dump.
 *
 * Trade-off: macOS VoiceOver may miss updates on those removed nodes for the
 * remainder of the session. Remove once the upstream fix lands (track against
 * Compose MP 1.11+).
 *
 * See [GitHub-Store#330](https://github.com/OpenHub-Store/GitHub-Store/issues/330)
 * and [GitHub-Store#640](https://github.com/OpenHub-Store/GitHub-Store/issues/640).
 */
object A11yCrashGuard {
    // Separate flags per path so each path logs its first suppression independently.
    private val warnedEdt = AtomicBoolean(false)
    private val warnedUncaught = AtomicBoolean(false)

    // Must be called after CrashReporter.install() so the uncaught-exception handler
    // chain is: A11yCrashGuard (filter) -> CrashReporter (log + dump) -> JVM default.
    fun install() {
        val osName = System.getProperty("os.name")?.lowercase().orEmpty()
        if (!osName.contains("mac")) return

        // Path 1: NPE propagates out of the coroutine dispatcher and through the
        // AWT EventQueue dispatch chain.
        Toolkit.getDefaultToolkit().systemEventQueue.push(FilteringEventQueue())

        // Path 2: NPE is intercepted by BaseContinuationImpl.resumeWith and
        // forwarded to the default uncaught-exception handler via coroutine
        // failure handling. Wrap the handler that CrashReporter already installed
        // so all non-a11y exceptions still reach it.
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
            // Forward to CrashReporter (or JVM default if previous is null, which
            // would only happen if install() is called before CrashReporter.install()).
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
                        // Specific AX bridge inner class present in all known traces for this bug.
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
