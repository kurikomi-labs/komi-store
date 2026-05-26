package zed.rainxch.githubstore

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.core.context.GlobalContext
import zed.rainxch.core.data.services.LocalizationManager
import zed.rainxch.core.domain.repository.TweaksRepository
import zed.rainxch.githubstore.app.desktop.KeyboardNavigation
import zed.rainxch.githubstore.app.desktop.KeyboardNavigationEvent
import zed.rainxch.githubstore.app.di.initKoin
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.app_icon
import zed.rainxch.githubstore.core.presentation.res.app_name
import zed.rainxch.githubstore.core.presentation.res.menubar_help_about
import zed.rainxch.githubstore.core.presentation.res.menubar_help_feedback
import zed.rainxch.githubstore.core.presentation.res.menubar_help_licenses
import zed.rainxch.githubstore.core.presentation.res.menubar_help_menu
import zed.rainxch.githubstore.core.presentation.res.menubar_help_privacy
import zed.rainxch.githubstore.desktop.WindowStateStore
import zed.rainxch.githubstore.desktop.applyMacosWindowAppearance
import zed.rainxch.githubstore.desktop.applyWindowsImmersiveDarkMode
import zed.rainxch.githubstore.desktop.installMacosSystemAppearance
import java.awt.Desktop
import java.net.URI
import kotlin.system.exitProcess

private const val PRIVACY_POLICY_URL = "https://github-store.org/privacy"

private const val LANGUAGE_PREF_READ_TIMEOUT_MS = 2000L

fun main(args: Array<String>) {
    installMacosSystemAppearance()
    CrashReporter.install()

    A11yCrashGuard.install()

    selectLinuxRenderBackendIfRequested()

    java.security.Security.setProperty("networkaddress.cache.ttl", "30")
    java.security.Security.setProperty("networkaddress.cache.negative.ttl", "5")

    initKoin()

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

    val deepLinkArg = args.firstOrNull()

    if (deepLinkArg != null && DesktopDeepLink.tryForwardToRunningInstance(deepLinkArg)) {
        exitProcess(0)
    }

    DesktopDeepLink.registerUriSchemeIfNeeded()

    application {
        var deepLinkUri by mutableStateOf(deepLinkArg)
        val windowState = remember { WindowStateStore.load() }

        DisposableEffect(windowState) {
            val hook = Thread { WindowStateStore.save(windowState) }
            runCatching { Runtime.getRuntime().addShutdownHook(hook) }
            onDispose {
                runCatching { Runtime.getRuntime().removeShutdownHook(hook) }
            }
        }

        @OptIn(FlowPreview::class)
        LaunchedEffect(windowState) {
            snapshotFlow {
                Triple(windowState.placement, windowState.position, windowState.size)
            }.distinctUntilChanged()
                .debounce(500)
                .collect { WindowStateStore.save(windowState) }
        }

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
                if (desktop.isSupported(Desktop.Action.APP_ABOUT)) {
                    runCatching {
                        desktop.setAboutHandler {
                            deepLinkUri = "githubstore://tweaks/app-info"
                        }
                    }
                }
            }
        }

        Window(
            onCloseRequest = {
                WindowStateStore.save(windowState)
                exitApplication()
            },
            state = windowState,
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
            val isOsDark = isSystemInDarkTheme()
            LaunchedEffect(window, isOsDark) {
                applyWindowsImmersiveDarkMode(window, isOsDark)
                applyMacosWindowAppearance(window, isOsDark)
            }
            MenuBar {
                Menu(text = stringResource(Res.string.menubar_help_menu)) {
                    Item(
                        text = stringResource(Res.string.menubar_help_about),
                        onClick = { deepLinkUri = "githubstore://tweaks/app-info" },
                    )
                    Item(
                        text = stringResource(Res.string.menubar_help_feedback),
                        onClick = { deepLinkUri = "githubstore://tweaks" },
                    )
                    Item(
                        text = stringResource(Res.string.menubar_help_licenses),
                        onClick = { deepLinkUri = "githubstore://tweaks/licenses" },
                    )
                    Item(
                        text = stringResource(Res.string.menubar_help_privacy),
                        onClick = {
                            runCatching {
                                if (Desktop.isDesktopSupported() &&
                                    Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)
                                ) {
                                    Desktop.getDesktop().browse(URI(PRIVACY_POLICY_URL))
                                }
                            }
                        },
                    )
                }
            }
            App(deepLinkUri = deepLinkUri)
        }
    }
}

private fun selectLinuxRenderBackendIfRequested() {
    val osName = System.getProperty("os.name", "").lowercase()
    if (!osName.contains("linux")) return
    if (System.getProperty("skiko.renderApi") != null) return
    val fromEnv = System.getenv("SKIKO_RENDER_API")?.trim().orEmpty()
    if (fromEnv.isEmpty()) return
    System.setProperty("skiko.renderApi", fromEnv)
}
