package zed.rainxch.tweaks.presentation.mirror

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import zed.rainxch.core.presentation.components.overlays.rememberKomiToastState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import zed.rainxch.core.domain.model.mirror.MirrorConfig
import zed.rainxch.core.domain.model.mirror.MirrorPreference
import zed.rainxch.core.domain.model.mirror.MirrorType
import zed.rainxch.core.presentation.components.bars.KomiTopBar
import zed.rainxch.core.presentation.components.bars.KomiTopBarSize
import zed.rainxch.core.presentation.components.buttons.KomiButton
import zed.rainxch.core.presentation.components.buttons.KomiButtonVariant
import zed.rainxch.core.presentation.components.buttons.KomiIconButton
import zed.rainxch.core.presentation.components.scaffold.KomiScaffold
import zed.rainxch.core.presentation.utils.ObserveAsEvents
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.mirror_custom_label
import zed.rainxch.githubstore.core.presentation.res.navigate_back
import zed.rainxch.githubstore.core.presentation.res.mirror_picker_description
import zed.rainxch.githubstore.core.presentation.res.mirror_picker_title
import zed.rainxch.githubstore.core.presentation.res.mirror_removed_toast
import zed.rainxch.githubstore.core.presentation.res.mirror_section_community
import zed.rainxch.githubstore.core.presentation.res.mirror_section_official
import zed.rainxch.githubstore.core.presentation.res.mirror_test_button
import zed.rainxch.githubstore.core.presentation.res.mirror_test_dns_fail
import zed.rainxch.githubstore.core.presentation.res.mirror_test_http_error
import zed.rainxch.githubstore.core.presentation.res.mirror_test_other
import zed.rainxch.githubstore.core.presentation.res.mirror_test_success
import zed.rainxch.githubstore.core.presentation.res.mirror_test_timeout
import zed.rainxch.tweaks.presentation.mirror.components.CustomMirrorDialog
import zed.rainxch.tweaks.presentation.mirror.components.DeployYourOwnHint
import zed.rainxch.tweaks.presentation.mirror.components.MirrorRow

@Composable
fun MirrorPickerRoot(
    onNavigateBack: () -> Unit,
    viewModel: MirrorPickerViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val toastState = rememberKomiToastState()
    val coroutineScope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is MirrorPickerEvent.MirrorRemovedNotice ->
                coroutineScope.launch {
                    toastState.warning(
                        getString(
                            Res.string.mirror_removed_toast,
                            event.displayName
                        )
                    )
                }

            is MirrorPickerEvent.OpenUrl -> uriHandler.openUri(event.url)
        }
    }

    KomiScaffold(
        topBar = {
            KomiTopBar(
                title = stringResource(Res.string.mirror_picker_title),
                size = KomiTopBarSize.Compact,
                leading = {
                    KomiIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.navigate_back),
                        onClick = onNavigateBack,
                        variant = KomiButtonVariant.Text,
                    )
                },
            )
        },
        toastState = toastState,
    ) { padding ->
        val listState = rememberLazyListState()
        LazyColumn(
            state = listState,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(Res.string.mirror_picker_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
            }

            item {
                Text(
                    text = stringResource(Res.string.mirror_section_official),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(
                items = state.mirrors.filter { it.type == MirrorType.OFFICIAL },
                key = { it.id },
            ) { mirror ->
                MirrorRow(
                    mirror = mirror,
                    selected = isMirrorSelected(mirror, state.preference),
                    onClick = { viewModel.onAction(MirrorPickerAction.OnSelectMirror(mirror)) },
                )
            }

            item {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(Res.string.mirror_section_community),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(
                items = state.mirrors.filter { it.type == MirrorType.COMMUNITY },
                key = { it.id },
            ) { mirror ->
                MirrorRow(
                    mirror = mirror,
                    selected = isMirrorSelected(mirror, state.preference),
                    onClick = { viewModel.onAction(MirrorPickerAction.OnSelectMirror(mirror)) },
                )
            }

            item {
                Spacer(Modifier.height(8.dp))
                CustomMirrorRow(
                    selected = state.preference is MirrorPreference.Custom,
                    onClick = { viewModel.onAction(MirrorPickerAction.OnCustomMirrorClicked) },
                )
            }

            item {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))
            }

            item {
                KomiButton(
                    onClick = { viewModel.onAction(MirrorPickerAction.OnTestConnection) },
                    label = stringResource(Res.string.mirror_test_button),
                    variant = KomiButtonVariant.Primary,
                    enabled = !state.isTesting,
                    loading = state.isTesting,
                    modifier = Modifier.fillMaxWidth(),
                )
                state.testResult?.let { result ->
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = formatTestResult(result),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            item {
                Spacer(Modifier.height(16.dp))
                DeployYourOwnHint(
                    onClick = { viewModel.onAction(MirrorPickerAction.OnDeployYourOwnClicked) },
                )
            }

            item {
                Spacer(Modifier.height(32.dp))
            }
        }
    }

    if (state.isCustomDialogVisible) {
        CustomMirrorDialog(
            draft = state.customDraft,
            error = state.customDraftError,
            onDraftChange = { viewModel.onAction(MirrorPickerAction.OnCustomDraftChanged(it)) },
            onConfirm = { viewModel.onAction(MirrorPickerAction.OnCustomMirrorConfirm) },
            onDismiss = { viewModel.onAction(MirrorPickerAction.OnCustomMirrorDismiss) },
        )
    }
}

@Composable
private fun CustomMirrorRow(
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .selectable(
                    selected = selected,
                    onClick = onClick,
                    role = Role.RadioButton,
                ).padding(vertical = 8.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(
            text = stringResource(Res.string.mirror_custom_label),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun formatTestResult(result: TestResult): String =
    when (result) {
        is TestResult.Success -> stringResource(Res.string.mirror_test_success, result.latencyMs)
        is TestResult.HttpError -> stringResource(Res.string.mirror_test_http_error, result.code)
        TestResult.Timeout -> stringResource(Res.string.mirror_test_timeout)
        TestResult.DnsFailure -> stringResource(Res.string.mirror_test_dns_fail)
        is TestResult.Other -> stringResource(Res.string.mirror_test_other, result.message)
    }

private fun isMirrorSelected(
    mirror: MirrorConfig,
    pref: MirrorPreference,
): Boolean =
    when (pref) {
        MirrorPreference.Direct -> mirror.id == "direct"
        is MirrorPreference.Selected -> mirror.id == pref.id
        is MirrorPreference.Custom -> false
    }
