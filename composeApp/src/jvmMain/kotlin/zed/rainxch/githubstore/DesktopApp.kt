package zed.rainxch.githubstore

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.core.context.GlobalContext
import zed.rainxch.core.data.services.LocalizationManager
import zed.rainxch.core.domain.repository.TweaksRepository
import zed.rainxch.core.domain.telemetry.ProductTelemetry
import zed.rainxch.core.domain.telemetry.ProductTelemetryEvents
import zed.rainxch.core.domain.telemetry.ProductTelemetryProps
import zed.rainxch.githubstore.app.ColdStart
import zed.rainxch.githubstore.app.categorizeCrash
import zed.rainxch.githubstore.app.desktop.KeyboardNavigation
import zed.rainxch.githubstore.app.desktop.KeyboardNavigationEvent
import zed.rainxch.githubstore.app.di.initKoin
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.app_icon
import zed.rainxch.githubstore.core.presentation.res.app_name
import java.awt.Desktop
import kotlin.system.exitProcess

private const val LANGUAGE_PREF_READ_TIMEOUT_MS = 2000L

fun main(args: Array<String>) {
    ColdStart.markStart()

    // Install first so anything that blows up during Koin init or
    // resource loading leaves a diagnosable trail on disk (see
    // `CrashReporter.resolveLogDir` for the per-OS path).
    CrashReporter.install()

    // Guard the AWT EventDispatchThread against a known Compose MP 1.10.x NPE
    // raised by the macOS accessibility bridge (see `A11yCrashGuard`).
    A11yCrashGuard.install()

    // Reduce JVM DNS cache TTL so network changes (VPN on/off) are picked up quickly.
    // Default JVM caches positive lookups for 30s and negative lookups forever,
    // which breaks connectivity when a VPN changes DNS/routing mid-session.
    java.security.Security.setProperty("networkaddress.cache.ttl", "30")
    java.security.Security.setProperty("networkaddress.cache.negative.ttl", "5")

    initKoin()

    installCrashTelemetryHandler()
    installTelemetryShutdownHook()

    // Apply persisted UI language before any Compose code runs — same
    // reasoning as on Android (see `MainActivity.onCreate`). Desktop
    // Compose has no runtime `recreate()` equivalent, so mid-session
    // language swaps surface as a "restart required" snackbar from the
    // Tweaks screen; this block just covers the cold-start path so
    // users see their chosen language immediately on next launch.
    runBlocking {
        val koin = GlobalContext.get()
        val tweaksRepo = koin.get<TweaksRepository>()
        val localization = koin.get<LocalizationManager>()
        val tag =
            try {
                withTimeoutOrNull(LANGUAGE_PREF_READ_TIMEOUT_MS) {
                    tweaksRepo.getAppLanguage().first()
                }
            } catch (_: Exception) {
                null
            }
        localization.setActiveLanguageTag(tag)
    }

    // Fire app_launched once per process. No-op when consent is not Granted.
    // The impl reads BuildKonfig.VERSION_NAME internally for the appVersion
    // field on every fire(); we just supply the platform-specific version
    // bucket via the props map. (BuildKonfig is internal to core/data so we
    // can't read it from composeApp directly.)
    GlobalContext.get().get<ProductTelemetry>().fire(
        name = ProductTelemetryEvents.APP_LAUNCHED,
        props = mapOf(ProductTelemetryProps.PLATFORM to desktopPlatformSlug()),
    )

    val deepLinkArg = args.firstOrNull()

    if (deepLinkArg != null && DesktopDeepLink.tryForwardToRunningInstance(deepLinkArg)) {
        exitProcess(0)
    }

    DesktopDeepLink.registerUriSchemeIfNeeded()

    application {
        var deepLinkUri by mutableStateOf(deepLinkArg)

        LaunchedEffect(Unit) {
            DesktopDeepLink.startInstanceListener { uri ->
                deepLinkUri = uri
            }
        }

        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().let { desktop ->
                if (desktop.isSupported(Desktop.Action.APP_OPEN_URI)) {
                    desktop.setOpenURIHandler { event ->
                        deepLinkUri = event.uri.toString()
                    }
                }
            }
        }

        Window(
            onCloseRequest = {
                val telemetry = GlobalContext.get().get<ProductTelemetry>()
                ColdStart.elapsedSeconds()?.let { seconds ->
                    telemetry.fire(
                        name = ProductTelemetryEvents.SESSION_DURATION,
                        props = mapOf(ProductTelemetryProps.SECONDS to seconds.toString()),
                    )
                }
                exitApplication()
            },
            title = stringResource(Res.string.app_name),
            icon = painterResource(Res.drawable.app_icon),
            onKeyEvent = { keyEvent ->
                if (keyEvent.key == Key.F && keyEvent.type == KeyEventType.KeyDown) {
                    if (keyEvent.isCtrlPressed || keyEvent.isMetaPressed) {
                        KeyboardNavigation.onKeyClicked(KeyboardNavigationEvent.OnCtrlFClick)
                        true
                    } else {
                        false
                    }
                } else {
                    false
                }
            },
        ) {
            App(deepLinkUri = deepLinkUri)
        }
    }
}

private fun installTelemetryShutdownHook() {
    Runtime.getRuntime().addShutdownHook(
        Thread {
            runCatching {
                runBlocking {
                    withTimeoutOrNull(2_000) {
                        GlobalContext.get().get<ProductTelemetry>().flush()
                    }
                }
            }
        },
    )
}

private fun installCrashTelemetryHandler() {
    val previous = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        runCatching {
            val telemetry = GlobalContext.get().get<ProductTelemetry>()
            telemetry.fire(
                name = ProductTelemetryEvents.CRASH,
                props =
                    mapOf(
                        ProductTelemetryProps.CATEGORY to categorizeCrash(throwable),
                        ProductTelemetryProps.PLATFORM to desktopPlatformSlug(),
                    ),
            )
            runBlocking { withTimeoutOrNull(500) { telemetry.flush() } }
        }
        previous?.uncaughtException(thread, throwable)
    }
}

private fun desktopPlatformSlug(): String {
    val os = System.getProperty("os.name").orEmpty().lowercase()
    return when {
        os.contains("mac") -> "macos"
        os.contains("win") -> "windows"
        os.contains("nix") || os.contains("nux") -> "linux"
        else -> "desktop"
    }
}
