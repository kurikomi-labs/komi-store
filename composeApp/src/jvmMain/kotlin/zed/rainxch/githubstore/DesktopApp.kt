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
            onCloseRequest = ::exitApplication,
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

private fun selectLinuxRenderBackendIfRequested() {
    val osName = System.getProperty("os.name", "").lowercase()
    if (!osName.contains("linux")) return
    if (System.getProperty("skiko.renderApi") != null) return
    val fromEnv = System.getenv("SKIKO_RENDER_API")?.trim().orEmpty()
    if (fromEnv.isEmpty()) return
    System.setProperty("skiko.renderApi", fromEnv)
}
