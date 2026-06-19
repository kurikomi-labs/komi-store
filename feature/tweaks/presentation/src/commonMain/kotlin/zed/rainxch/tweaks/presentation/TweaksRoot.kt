package zed.rainxch.tweaks.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Feedback
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import zed.rainxch.core.presentation.components.overlays.rememberKomiToastState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import zed.rainxch.core.domain.isAndroid
import zed.rainxch.core.presentation.components.bars.KomiTopBar
import zed.rainxch.core.presentation.components.bars.KomiTopBarSize
import zed.rainxch.core.presentation.components.buttons.KomiButtonVariant
import zed.rainxch.core.presentation.components.buttons.KomiIconButton
import zed.rainxch.core.presentation.components.scaffold.KomiScaffold
import zed.rainxch.core.presentation.components.surfaces.KomiSurface
import zed.rainxch.core.presentation.components.text.KomiText
import zed.rainxch.core.presentation.components.text.KomiTextRole
import zed.rainxch.core.presentation.personality.utils.PersonalityPreview
import zed.rainxch.core.presentation.utils.ObserveAsEvents
import zed.rainxch.core.presentation.utils.constrainedContentWidth
import zed.rainxch.githubstore.core.presentation.res.*
import zed.rainxch.tweaks.presentation.components.ClearDownloadsDialog
import zed.rainxch.tweaks.presentation.components.sections.appearanceSectionContent
import zed.rainxch.tweaks.presentation.components.sections.connectionSectionContent
import zed.rainxch.tweaks.presentation.components.sections.installSectionContent
import zed.rainxch.tweaks.presentation.components.sections.languageSectionContent
import zed.rainxch.tweaks.presentation.components.sections.privacySectionContent
import zed.rainxch.tweaks.presentation.components.sections.SourcesSectionContent
import zed.rainxch.tweaks.presentation.components.sections.storageSectionContent
import zed.rainxch.tweaks.presentation.components.sections.translationSectionContent
import zed.rainxch.tweaks.presentation.components.sections.updatesSectionContent
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
                        onAction = {
                            restartAppAfterLanguageChange()
                        }
                    )
                }
            }

            TweaksEvent.OnCacheCleared -> {
                coroutineScope.launch {
                    toastState.info(getString(Res.string.downloads_cleared))
                }
            }

            is TweaksEvent.OnCacheClearError -> {
                coroutineScope.launch { toastState.danger(event.message) }
            }

            else -> Unit
        }
    }

    KomiScaffold(
        toastState = toastState,
        topBar = {
            KomiTopBar(
                title = stringResource(Res.string.tweaks_title),
                size = KomiTopBarSize.Compact,
                leading = {
                    KomiIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.back_cd),
                        onClick = onNavigateBack,
                        variant = KomiButtonVariant.Text,
                    )
                },
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
                        .padding(top = 8.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SectionHeader(stringResource(Res.string.section_look_and_feel))
                appearanceSectionContent(state = state, onAction = onAction)
                languageSectionContent(state = state, onAction = onAction)

                SectionHeader(stringResource(Res.string.section_connectivity))
                connectionSectionContent(state = state, onAction = onAction)
                SourcesSectionContent(
                    state = state,
                    onAction = onAction,
                    onNavigateToMirrorPicker = onNavigateToMirrorPicker,
                )
                translationSectionContent(state = state, onAction = onAction)

                if (isAndroid()) {
                    SectionHeader(stringResource(Res.string.section_installs_and_updates))
                    installSectionContent(state = state, onAction = onAction)
                    updatesSectionContent(
                        state = state,
                        onAction = onAction,
                        onNavigateToSkippedUpdates = onNavigateToSkippedUpdates,
                    )
                }

                SectionHeader(stringResource(Res.string.section_privacy_and_data))
                storageSectionContent(state = state, onAction = onAction)
                privacySectionContent(
                    state = state,
                    onAction = onAction,
                    onNavigateToHiddenRepositories = onNavigateToHiddenRepositories,
                )
                TweaksEntryRow(
                    title = stringResource(Res.string.tweaks_entry_access_tokens),
                    subtitle = stringResource(Res.string.tweaks_entry_subtitle_tap),
                    icon = Icons.Outlined.VpnKey,
                    onClick = onNavigateToHostTokens,
                    accentColor = MaterialTheme.colorScheme.primary,
                )

                SectionHeader(stringResource(Res.string.section_app_block))
                TweaksEntryRow(
                    title = stringResource(Res.string.tweaks_entry_feedback),
                    subtitle = stringResource(Res.string.feedback_hub_subtitle),
                    icon = Icons.Outlined.Feedback,
                    onClick = { feedbackSheetOpen = true },
                    accentColor = MaterialTheme.colorScheme.primary,
                )
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

@Composable
private fun SectionHeader(title: String) {
    KomiText(text = title, role = KomiTextRole.Title)
}

@Composable
private fun TweaksEntryRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    accentColor: Color,
) {
    KomiSurface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(22.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                KomiText(text = title, role = KomiTextRole.Label)
                KomiText(
                    text = subtitle,
                    role = KomiTextRole.Body,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Preview
@Composable
private fun TweaksRootPreview() {
    PersonalityPreview {
        Spacer(Modifier.height(0.dp))
    }
}
