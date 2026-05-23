package zed.rainxch.tweaks.presentation.appearance

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.tweaks_entry_appearance
import zed.rainxch.tweaks.presentation.TweaksAction
import zed.rainxch.tweaks.presentation.TweaksViewModel
import zed.rainxch.tweaks.presentation.components.TweaksSubScreenScaffold
import zed.rainxch.tweaks.presentation.components.sections.appearanceSection

@Composable
fun TweaksAppearanceRoot(
    onNavigateBack: () -> Unit,
    viewModel: TweaksViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarState = remember { SnackbarHostState() }

    TweaksSubScreenScaffold(
        title = stringResource(Res.string.tweaks_entry_appearance),
        onNavigateBack = onNavigateBack,
        snackbarState = snackbarState,
        restartReasons = state.needsRestartReasons,
        onRestartNow = { viewModel.onAction(TweaksAction.OnRestartNowClick) },
        onRestartLater = { viewModel.onAction(TweaksAction.OnRestartLaterClick) },
        showRestartBanner = state.restartBannerVisible,
    ) {
        appearanceSection(
            state = state,
            onAction = { viewModel.onAction(it) },
        )
    }
}
