package zed.rainxch.tweaks.presentation.translation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import zed.rainxch.core.presentation.utils.ObserveAsEvents
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.translation_deepl_saved
import zed.rainxch.githubstore.core.presentation.res.translation_libre_saved
import zed.rainxch.githubstore.core.presentation.res.translation_microsoft_saved
import zed.rainxch.githubstore.core.presentation.res.translation_provider_saved
import zed.rainxch.githubstore.core.presentation.res.translation_youdao_saved
import zed.rainxch.githubstore.core.presentation.res.tweaks_entry_translation
import zed.rainxch.tweaks.presentation.TweaksAction
import zed.rainxch.tweaks.presentation.TweaksEvent
import zed.rainxch.tweaks.presentation.TweaksViewModel
import zed.rainxch.tweaks.presentation.components.TweaksSubScreenScaffold
import zed.rainxch.tweaks.presentation.components.sections.translationSection

@Composable
fun TweaksTranslationRoot(
    onNavigateBack: () -> Unit,
    viewModel: TweaksViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            TweaksEvent.OnTranslationProviderSaved -> coroutineScope.launch {
                snackbarState.showSnackbar(getString(Res.string.translation_provider_saved))
            }
            TweaksEvent.OnYoudaoCredentialsSaved -> coroutineScope.launch {
                snackbarState.showSnackbar(getString(Res.string.translation_youdao_saved))
            }
            TweaksEvent.OnLibreTranslateCredentialsSaved -> coroutineScope.launch {
                snackbarState.showSnackbar(getString(Res.string.translation_libre_saved))
            }
            TweaksEvent.OnDeeplCredentialsSaved -> coroutineScope.launch {
                snackbarState.showSnackbar(getString(Res.string.translation_deepl_saved))
            }
            TweaksEvent.OnMicrosoftTranslatorCredentialsSaved -> coroutineScope.launch {
                snackbarState.showSnackbar(getString(Res.string.translation_microsoft_saved))
            }
            else -> Unit
        }
    }

    TweaksSubScreenScaffold(
        title = stringResource(Res.string.tweaks_entry_translation),
        onNavigateBack = onNavigateBack,
        snackbarState = snackbarState,
        restartReasons = state.needsRestartReasons,
        onRestartNow = { viewModel.onAction(TweaksAction.OnRestartNowClick) },
        onRestartLater = { viewModel.onAction(TweaksAction.OnRestartLaterClick) },
        showRestartBanner = state.restartBannerVisible,
    ) {
        translationSection(
            state = state,
            onAction = { viewModel.onAction(it) },
        )
    }
}
