package zed.rainxch.tweaks.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import zed.rainxch.core.domain.isAndroid
import zed.rainxch.core.domain.isDesktop
import zed.rainxch.core.domain.model.settings.AppLanguages
import zed.rainxch.core.presentation.components.overlays.KomiSheet
import zed.rainxch.core.presentation.components.overlays.KomiSheetPlacement
import zed.rainxch.core.presentation.components.overlays.rememberKomiToastState
import zed.rainxch.core.presentation.components.scaffold.KomiScaffold
import zed.rainxch.core.presentation.components.text.KomiText
import zed.rainxch.core.presentation.components.text.KomiTextRole
import zed.rainxch.core.presentation.locals.LocalPersonality
import zed.rainxch.core.presentation.personality.utils.PersonalityPreview
import zed.rainxch.core.presentation.utils.ObserveAsEvents
import zed.rainxch.core.presentation.utils.constrainedContentWidth
import zed.rainxch.githubstore.core.presentation.res.*
import zed.rainxch.tweaks.presentation.components.ClearDownloadsDialog
import zed.rainxch.tweaks.presentation.components.desktop.TweaksDesktopContent
import zed.rainxch.tweaks.presentation.components.sections.appSection
import zed.rainxch.tweaks.presentation.components.sections.connectivitySection
import zed.rainxch.tweaks.presentation.components.sections.installsSection
import zed.rainxch.tweaks.presentation.components.sections.languageSectionContent
import zed.rainxch.tweaks.presentation.components.sections.lookAndFeelSection
import zed.rainxch.tweaks.presentation.components.sections.privacySection
import zed.rainxch.tweaks.presentation.components.shell.TweaksMangaHeader
import zed.rainxch.tweaks.presentation.feedback.components.FeedbackBottomSheet
import zed.rainxch.tweaks.presentation.feedback.model.FeedbackChannel

@Composable
fun TweaksRoot(
    onNavigateBack: () -> Unit,
    onNavigateToHostTokens: () -> Unit,
    onNavigateToMirrorPicker: () -> Unit,
    onNavigateToSkippedUpdates: () -> Unit,
    onNavigateToHiddenRepositories: () -> Unit,
    viewModel: TweaksViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val toastState = rememberKomiToastState()
    val coroutineScope = rememberCoroutineScope()
    var feedbackSheetOpen by rememberSaveable { mutableStateOf(false) }
    var languageSheetOpen by rememberSaveable { mutableStateOf(false) }
    val onAction: (TweaksAction) -> Unit = remember(viewModel) { { viewModel.onAction(it) } }

    LaunchedEffect(Unit) {
        TweaksDeepLinkBus.openFeedbackRequests.collect {
            feedbackSheetOpen = true
            TweaksDeepLinkBus.consume()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer =
            androidx.lifecycle.LifecycleEventObserver { _, event ->
                if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                    viewModel.onAction(TweaksAction.OnRefreshCacheSize)
                    viewModel.onAction(TweaksAction.OnReevaluateBatteryOptimizationCard)
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            TweaksEvent.OnAppLanguageChangeRequiresRestart -> {
                coroutineScope.launch {
                    toastState.warning(
                        message = getString(Res.string.language_restart_required),
                        actionLabel = getString(Res.string.language_restart_action),
                        onAction = { restartAppAfterLanguageChange() },
                    )
                }
            }

            TweaksEvent.OnCacheCleared -> {
                coroutineScope.launch { toastState.info(getString(Res.string.downloads_cleared)) }
            }

            is TweaksEvent.OnCacheClearError -> {
                coroutineScope.launch { toastState.danger(event.message) }
            }

            else -> Unit
        }
    }

    val currentLanguageLabel =
        state.selectedAppLanguage
            ?.let { tag -> AppLanguages.ALL.firstOrNull { it.tag == tag }?.displayName }
            ?: stringResource(Res.string.language_follow_system)
    val onOpenLanguage = { languageSheetOpen = true }
    val onOpenFeedback = { feedbackSheetOpen = true }

    if (isDesktop()) {
        KomiScaffold(toastState = toastState, grid = false, screentone = false) { innerPadding ->
            TweaksDesktopContent(
                state = state,
                onAction = onAction,
                personality = state.selectedPersonality,
                accent = state.selectedAccent,
                onPersonalitySelected = { onAction(TweaksAction.OnPersonalitySelected(it)) },
                onAccentSelected = { onAction(TweaksAction.OnAccentSelected(it)) },
                currentLanguageLabel = currentLanguageLabel,
                onOpenLanguage = onOpenLanguage,
                onOpenFeedback = onOpenFeedback,
                onNavigateToMirrorPicker = onNavigateToMirrorPicker,
                onNavigateToHiddenRepositories = onNavigateToHiddenRepositories,
                onNavigateToHostTokens = onNavigateToHostTokens,
                modifier = Modifier.fillMaxSize().padding(innerPadding),
            )
        }
    } else {
        KomiScaffold(
            toastState = toastState,
            grid = true,
            screentone = true,
            topBar = {
                TweaksMangaHeader(
                    title = stringResource(Res.string.tweaks_title),
                    jp = "設定 · SETTINGS",
                    onNavigateBack = onNavigateBack,
                )
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    modifier =
                        Modifier
                            .constrainedContentWidth()
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp)
                            .padding(top = 4.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    lookAndFeelSection(
                        state = state,
                        onAction = onAction,
                        personality = state.selectedPersonality,
                        accent = state.selectedAccent,
                        onPersonalitySelected = { onAction(TweaksAction.OnPersonalitySelected(it)) },
                        onAccentSelected = { onAction(TweaksAction.OnAccentSelected(it)) },
                        currentLanguageLabel = currentLanguageLabel,
                        onOpenLanguage = onOpenLanguage,
                    )
                    connectivitySection(
                        state = state,
                        onAction = onAction,
                        onNavigateToMirrorPicker = onNavigateToMirrorPicker,
                    )
                    if (isAndroid()) {
                        installsSection(
                            state = state,
                            onAction = onAction,
                            onNavigateToSkippedUpdates = onNavigateToSkippedUpdates,
                        )
                    }
                    privacySection(
                        state = state,
                        onAction = onAction,
                        onNavigateToHiddenRepositories = onNavigateToHiddenRepositories,
                        onNavigateToHostTokens = onNavigateToHostTokens,
                    )
                    appSection(onOpenFeedback = onOpenFeedback)

                    KomiText(
                        text = "― 設定 · komi store ―",
                        role = KomiTextRole.Stamp,
                        color = LocalPersonality.current.colors.onSurfaceVariant,
                        fontSize = 11.sp,
                        uppercase = false,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    )
                }
            }
        }
    }

    if (state.isClearDownloadsDialogVisible) {
        ClearDownloadsDialog(
            cacheSize = state.cacheSize,
            onDismissRequest = { onAction(TweaksAction.OnClearDownloadsDismiss) },
            onConfirm = { onAction(TweaksAction.OnClearDownloadsConfirm) },
        )
    }

    if (languageSheetOpen) {
        KomiSheet(
            onDismiss = { languageSheetOpen = false },
            placement = KomiSheetPlacement.Bottom,
            title = stringResource(Res.string.select_language),
        ) {
            languageSectionContent(state = state, onAction = onAction)
        }
    }

    if (feedbackSheetOpen) {
        FeedbackBottomSheet(
            onDismiss = { feedbackSheetOpen = false },
            onSent = { channel ->
                feedbackSheetOpen = false
                coroutineScope.launch {
                    val msg =
                        when (channel) {
                            FeedbackChannel.EMAIL -> getString(Res.string.feedback_send_success_email)
                            FeedbackChannel.GITHUB -> getString(Res.string.feedback_send_success_github)
                        }
                    toastState.success(msg)
                }
            },
            onError = { error ->
                coroutineScope.launch {
                    toastState.danger(getString(Res.string.feedback_send_error, error))
                }
            },
        )
    }
}

@Preview
@Composable
private fun TweaksRootPreview() {
    PersonalityPreview {
        Spacer(Modifier.height(0.dp))
    }
}
