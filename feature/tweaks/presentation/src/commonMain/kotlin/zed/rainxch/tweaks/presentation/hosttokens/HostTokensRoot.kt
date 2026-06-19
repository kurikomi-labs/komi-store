package zed.rainxch.tweaks.presentation.hosttokens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import zed.rainxch.core.presentation.components.overlays.rememberKomiToastState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import zed.rainxch.core.domain.model.account.ForgeKind
import zed.rainxch.core.domain.model.account.HostToken
import zed.rainxch.core.presentation.components.bars.KomiTopBar
import zed.rainxch.core.presentation.components.bars.KomiTopBarSize
import zed.rainxch.core.presentation.components.buttons.KomiButton
import zed.rainxch.core.presentation.components.buttons.KomiButtonVariant
import zed.rainxch.core.presentation.components.buttons.KomiButtonSize
import zed.rainxch.core.presentation.components.buttons.KomiIconButton
import zed.rainxch.core.presentation.components.inputs.KomiTextField
import zed.rainxch.core.presentation.components.scaffold.KomiScaffold
import zed.rainxch.core.presentation.locals.LocalPersonality
import zed.rainxch.core.presentation.utils.ObserveAsEvents
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.host_tokens_action_add
import zed.rainxch.githubstore.core.presentation.res.host_tokens_action_back
import zed.rainxch.githubstore.core.presentation.res.host_tokens_action_cancel
import zed.rainxch.githubstore.core.presentation.res.host_tokens_action_delete
import zed.rainxch.githubstore.core.presentation.res.host_tokens_action_save
import zed.rainxch.githubstore.core.presentation.res.host_tokens_compose_add_title
import zed.rainxch.githubstore.core.presentation.res.host_tokens_compose_detected
import zed.rainxch.githubstore.core.presentation.res.host_tokens_compose_field_forge_address
import zed.rainxch.githubstore.core.presentation.res.host_tokens_compose_replace_hint
import zed.rainxch.githubstore.core.presentation.res.host_tokens_compose_replace_title
import zed.rainxch.githubstore.core.presentation.res.host_tokens_compose_will_connect
import zed.rainxch.githubstore.core.presentation.res.host_tokens_empty_subtitle
import zed.rainxch.githubstore.core.presentation.res.host_tokens_empty_title
import zed.rainxch.githubstore.core.presentation.res.host_tokens_field_display_name
import zed.rainxch.githubstore.core.presentation.res.host_tokens_field_token
import zed.rainxch.githubstore.core.presentation.res.host_tokens_oauth_coexistence
import zed.rainxch.githubstore.core.presentation.res.host_tokens_picker_open_page
import zed.rainxch.githubstore.core.presentation.res.host_tokens_picker_other
import zed.rainxch.githubstore.core.presentation.res.host_tokens_picker_other_subtitle
import zed.rainxch.githubstore.core.presentation.res.host_tokens_picker_paste
import zed.rainxch.githubstore.core.presentation.res.host_tokens_picker_title
import zed.rainxch.githubstore.core.presentation.res.host_tokens_row_edit_label
import zed.rainxch.githubstore.core.presentation.res.host_tokens_row_invalid_short
import zed.rainxch.githubstore.core.presentation.res.host_tokens_row_menu
import zed.rainxch.githubstore.core.presentation.res.host_tokens_row_open_token_page
import zed.rainxch.githubstore.core.presentation.res.host_tokens_row_replace_token
import zed.rainxch.githubstore.core.presentation.res.host_tokens_row_valid_rate
import zed.rainxch.githubstore.core.presentation.res.host_tokens_row_valid_short
import zed.rainxch.githubstore.core.presentation.res.host_tokens_row_validate
import zed.rainxch.githubstore.core.presentation.res.host_tokens_title
import zed.rainxch.githubstore.core.presentation.res.host_tokens_undo_action
import zed.rainxch.githubstore.core.presentation.res.host_tokens_undo_snackbar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostTokensRoot(
    onNavigateBack: () -> Unit,
    viewModel: HostTokensViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val toastState = rememberKomiToastState()
    val coroutineScope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is HostTokensEvent.Message ->
                coroutineScope.launch { toastState.show(event.text) }
            is HostTokensEvent.OpenUrl ->
                runCatching { uriHandler.openUri(event.url) }
            is HostTokensEvent.TokenDeletedWithUndo ->
                coroutineScope.launch {
                    toastState.warning(
                        message = getString(Res.string.host_tokens_undo_snackbar, event.deleted.host),
                        actionLabel = getString(Res.string.host_tokens_undo_action),
                        onAction = {
                            viewModel.onAction(HostTokensAction.OnUndoDelete)
                        }
                    )
                }
        }
    }

    KomiScaffold(
        topBar = {
            KomiTopBar(
                title = stringResource(Res.string.host_tokens_title),
                size = KomiTopBarSize.Compact,
                leading = {
                    KomiIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.host_tokens_action_back),
                        onClick = onNavigateBack,
                        variant = KomiButtonVariant.Text,
                    )
                },
            )
        },
        toastState = toastState,
        floatingActionButton = {
            if (state.tokens.isNotEmpty()) {
                FloatingActionButton(onClick = { viewModel.onAction(HostTokensAction.OnAddClicked) }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(Res.string.host_tokens_action_add))
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            if (state.isOAuthSignedInToGithub) {
                OAuthCoexistenceNote()
                Spacer(Modifier.height(12.dp))
            }
            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator() }
                }
                state.tokens.isEmpty() -> {
                    EmptyStatePicker(
                        presetForges = visiblePresetForges(state),
                        onPickPreset = { kind -> viewModel.onAction(HostTokensAction.OnPickPresetForge(kind)) },
                        onPickOther = { viewModel.onAction(HostTokensAction.OnPickOtherForge) },
                        onOpenTokenCreationPage = { kind ->
                            viewModel.onAction(HostTokensAction.OnOpenTokenCreationPage(kind))
                        },
                    )
                }
                else -> {
                    val listState = rememberLazyListState()
                    LazyColumn(
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(state.tokens, key = { it.host }) { token ->
                            TokenRow(
                                token = token,
                                isValidating = token.host in state.validatingHosts,
                                validation = state.validationByHost[token.host],
                                onValidate = { viewModel.onAction(HostTokensAction.OnValidate(token.host)) },
                                onDelete = { viewModel.onAction(HostTokensAction.OnDelete(token.host)) },
                                onReplace = { viewModel.onAction(HostTokensAction.OnReplaceToken(token)) },
                                onEditLabel = { viewModel.onAction(HostTokensAction.OnEditLabel(token)) },
                                onOpenManagePage = { kind ->
                                    viewModel.onAction(HostTokensAction.OnOpenTokenCreationPage(kind))
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    when (val mode = state.draftMode) {
        DraftMode.Closed -> Unit
        DraftMode.Picker -> {
            PickerDialog(
                presetForges = visiblePresetForges(state),
                onPickPreset = { kind -> viewModel.onAction(HostTokensAction.OnPickPresetForge(kind)) },
                onPickOther = { viewModel.onAction(HostTokensAction.OnPickOtherForge) },
                onOpenTokenCreationPage = { kind ->
                    viewModel.onAction(HostTokensAction.OnOpenTokenCreationPage(kind))
                },
                onDismiss = { viewModel.onAction(HostTokensAction.OnAddDismiss) },
            )
        }
        is DraftMode.Compose -> {
            AddTokenDialog(
                state = state,
                replacingExisting = mode.replacingExisting,
                onAction = viewModel::onAction,
            )
        }
    }
}

private fun visiblePresetForges(state: HostTokensState): List<ForgeKind> {

    val storedHosts = state.tokens.map { it.host }.toSet()
    return ForgeKind.entries.filter { kind ->
        when {
            kind.tokenHost in storedHosts -> true
            kind == ForgeKind.GITHUB && state.isOAuthSignedInToGithub -> false
            else -> true
        }
    }
}

@Composable
private fun OAuthCoexistenceNote() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(LocalPersonality.current.shape.corner),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = stringResource(Res.string.host_tokens_oauth_coexistence),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EmptyStatePicker(
    presetForges: List<ForgeKind>,
    onPickPreset: (ForgeKind) -> Unit,
    onPickOther: () -> Unit,
    onOpenTokenCreationPage: (ForgeKind) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(Res.string.host_tokens_empty_title),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(Res.string.host_tokens_empty_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(Res.string.host_tokens_picker_title),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
        }
        items(presetForges, key = { it.tokenHost }) { kind ->
            PresetForgeCard(
                kind = kind,
                onPick = { onPickPreset(kind) },
                onOpenTokenCreationPage = { onOpenTokenCreationPage(kind) },
            )
        }
        item {
            OtherForgeCard(onPick = onPickOther)
        }
    }
}

@Composable
private fun PresetForgeCard(
    kind: ForgeKind,
    onPick: () -> Unit,
    onOpenTokenCreationPage: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(LocalPersonality.current.shape.corner),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    Icons.Default.VpnKey,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = kind.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = kind.tokenHost,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                KomiButton(
                    onClick = onOpenTokenCreationPage,
                    label = stringResource(Res.string.host_tokens_picker_open_page),
                    variant = KomiButtonVariant.Text,
                    size = KomiButtonSize.Sm,
                    leadingIcon = Icons.Default.OpenInBrowser,
                )
                KomiButton(
                    onClick = onPick,
                    label = stringResource(Res.string.host_tokens_picker_paste),
                    variant = KomiButtonVariant.Text,
                    size = KomiButtonSize.Sm,
                )
            }
        }
    }
}

@Composable
private fun OtherForgeCard(onPick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(LocalPersonality.current.shape.corner))
            .clickable(onClick = onPick),
        shape = RoundedCornerShape(LocalPersonality.current.shape.corner),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                Icons.Default.VpnKey,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.host_tokens_picker_other),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(Res.string.host_tokens_picker_other_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PickerDialog(
    presetForges: List<ForgeKind>,
    onPickPreset: (ForgeKind) -> Unit,
    onPickOther: () -> Unit,
    onOpenTokenCreationPage: (ForgeKind) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.host_tokens_picker_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                presetForges.forEach { kind ->
                    PresetForgeCard(
                        kind = kind,
                        onPick = { onPickPreset(kind) },
                        onOpenTokenCreationPage = { onOpenTokenCreationPage(kind) },
                    )
                }
                OtherForgeCard(onPick = onPickOther)
            }
        },
        confirmButton = {
            KomiButton(
                onClick = onDismiss,
                label = stringResource(Res.string.host_tokens_action_cancel),
                variant = KomiButtonVariant.Text,
                size = KomiButtonSize.Sm,
            )
        },
    )
}

@Composable
private fun TokenRow(
    token: HostToken,
    isValidating: Boolean,
    validation: ValidationLine?,
    onValidate: () -> Unit,
    onDelete: () -> Unit,
    onReplace: () -> Unit,
    onEditLabel: () -> Unit,
    onOpenManagePage: (ForgeKind) -> Unit,
) {
    val forge = ForgeKind.fromHost(token.host)
    var menuOpen by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(LocalPersonality.current.shape.corner),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 12.dp, end = 8.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                Icons.Default.VpnKey,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = forge?.displayName ?: token.host,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                val secondary = when {
                    validation?.isSuccess == true -> {
                        val login = validation.login ?: token.displayName ?: token.host
                        if (validation.rateLimitRemaining != null) {
                            stringResource(Res.string.host_tokens_row_valid_short, login) +
                                " · " + stringResource(
                                    Res.string.host_tokens_row_valid_rate,
                                    validation.rateLimitRemaining,
                                )
                        } else {
                            stringResource(Res.string.host_tokens_row_valid_short, login)
                        }
                    }
                    validation != null -> stringResource(
                        Res.string.host_tokens_row_invalid_short,
                        validation.errorMessage.orEmpty(),
                    )
                    else -> token.displayName ?: token.host
                }
                Text(
                    text = secondary,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (validation != null && !validation.isSuccess) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            if (isValidating) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
            } else {
                IconButton(onClick = onValidate) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = stringResource(Res.string.host_tokens_row_validate),
                    )
                }
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = stringResource(Res.string.host_tokens_row_menu),
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.host_tokens_row_edit_label)) },
                        onClick = { menuOpen = false; onEditLabel() },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.host_tokens_row_replace_token)) },
                        onClick = { menuOpen = false; onReplace() },
                    )
                    if (forge != null) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(
                                        Res.string.host_tokens_row_open_token_page,
                                        forge.displayName,
                                    ),
                                )
                            },
                            onClick = { menuOpen = false; onOpenManagePage(forge) },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.host_tokens_action_delete)) },
                        onClick = { menuOpen = false; onDelete() },
                    )
                }
            }
        }
    }
}

@Composable
private fun AddTokenDialog(
    state: HostTokensState,
    replacingExisting: HostToken?,
    onAction: (HostTokensAction) -> Unit,
) {
    val title = if (replacingExisting != null) {
        stringResource(Res.string.host_tokens_compose_replace_title, replacingExisting.host)
    } else {
        stringResource(Res.string.host_tokens_compose_add_title)
    }
    AlertDialog(
        onDismissRequest = { onAction(HostTokensAction.OnAddDismiss) },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.draftForge == null) {
                    val hostSupporting = state.draftHostError?.let { stringResource(it) }
                        ?: state.draftHostNormalized
                            .takeIf { it.isNotBlank() && it != state.draftHost.trim() }
                            ?.let { normalized ->
                                stringResource(
                                    Res.string.host_tokens_compose_will_connect,
                                    normalized,
                                )
                            }
                    KomiTextField(
                        value = state.draftHost,
                        onValueChange = { onAction(HostTokensAction.OnDraftHostChanged(it)) },
                        label = stringResource(Res.string.host_tokens_compose_field_forge_address),
                        error = if (state.draftHostError != null) hostSupporting else null,
                        helper = if (state.draftHostError == null) hostSupporting else null,
                    )
                }
                val tokenSupporting = state.draftTokenError?.let { stringResource(it) }
                    ?: state.draftDetectedTokenKind?.let { kind ->
                        stringResource(Res.string.host_tokens_compose_detected, kind)
                    }
                    ?: replacingExisting?.let {
                        stringResource(Res.string.host_tokens_compose_replace_hint)
                    }
                KomiTextField(
                    value = state.draftToken,
                    onValueChange = { onAction(HostTokensAction.OnDraftTokenChanged(it)) },
                    label = stringResource(Res.string.host_tokens_field_token),
                    error = if (state.draftTokenError != null) tokenSupporting else null,
                    helper = if (state.draftTokenError == null) tokenSupporting else null,
                    password = true,
                )
                KomiTextField(
                    value = state.draftDisplayName,
                    onValueChange = { onAction(HostTokensAction.OnDraftDisplayNameChanged(it)) },
                    label = stringResource(Res.string.host_tokens_field_display_name),
                )
            }
        },
        confirmButton = {
            KomiButton(
                onClick = { onAction(HostTokensAction.OnAddConfirm) },
                label = stringResource(Res.string.host_tokens_action_save),
                variant = KomiButtonVariant.Text,
                size = KomiButtonSize.Sm,
            )
        },
        dismissButton = {
            KomiButton(
                onClick = { onAction(HostTokensAction.OnAddDismiss) },
                label = stringResource(Res.string.host_tokens_action_cancel),
                variant = KomiButtonVariant.Text,
                size = KomiButtonSize.Sm,
            )
        },
    )
}
