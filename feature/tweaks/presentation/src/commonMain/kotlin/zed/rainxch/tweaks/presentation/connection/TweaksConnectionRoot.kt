package zed.rainxch.tweaks.presentation.connection

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import zed.rainxch.core.domain.model.settings.ProxyScope
import zed.rainxch.core.presentation.components.buttons.GhsButton
import zed.rainxch.core.presentation.components.buttons.GhsButtonSize
import zed.rainxch.core.presentation.components.buttons.GhsButtonVariant
import zed.rainxch.core.presentation.components.inputs.GhsPasswordVisibilityIcon
import zed.rainxch.core.presentation.components.inputs.GhsTextField
import zed.rainxch.core.presentation.components.inputs.passwordVisualTransformation
import zed.rainxch.core.presentation.theme.tokens.Radii
import zed.rainxch.core.presentation.utils.ObserveAsEvents
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.proxy_password
import zed.rainxch.githubstore.core.presentation.res.proxy_port
import zed.rainxch.githubstore.core.presentation.res.proxy_save
import zed.rainxch.githubstore.core.presentation.res.proxy_saved
import zed.rainxch.githubstore.core.presentation.res.proxy_host
import zed.rainxch.githubstore.core.presentation.res.proxy_test_success
import zed.rainxch.githubstore.core.presentation.res.proxy_username
import zed.rainxch.githubstore.core.presentation.res.tweaks_connection_applies_to_all
import zed.rainxch.githubstore.core.presentation.res.tweaks_connection_custom
import zed.rainxch.githubstore.core.presentation.res.tweaks_connection_intro_body
import zed.rainxch.githubstore.core.presentation.res.tweaks_connection_intro_title
import zed.rainxch.githubstore.core.presentation.res.tweaks_connection_main_section
import zed.rainxch.githubstore.core.presentation.res.tweaks_connection_mode_http
import zed.rainxch.githubstore.core.presentation.res.tweaks_connection_mode_http_caption
import zed.rainxch.githubstore.core.presentation.res.tweaks_connection_mode_no_proxy
import zed.rainxch.githubstore.core.presentation.res.tweaks_connection_mode_socks
import zed.rainxch.githubstore.core.presentation.res.tweaks_connection_mode_socks_caption
import zed.rainxch.githubstore.core.presentation.res.tweaks_connection_mode_system
import zed.rainxch.githubstore.core.presentation.res.tweaks_connection_overrides_body
import zed.rainxch.githubstore.core.presentation.res.tweaks_connection_overrides_section
import zed.rainxch.githubstore.core.presentation.res.tweaks_connection_paste_url
import zed.rainxch.githubstore.core.presentation.res.tweaks_connection_scope_discovery_body
import zed.rainxch.githubstore.core.presentation.res.tweaks_connection_scope_discovery_title
import zed.rainxch.githubstore.core.presentation.res.tweaks_connection_scope_download_body
import zed.rainxch.githubstore.core.presentation.res.tweaks_connection_scope_download_title
import zed.rainxch.githubstore.core.presentation.res.tweaks_connection_scope_translation_body
import zed.rainxch.githubstore.core.presentation.res.tweaks_connection_scope_translation_title
import zed.rainxch.githubstore.core.presentation.res.tweaks_connection_test
import zed.rainxch.githubstore.core.presentation.res.tweaks_connection_use_main
import zed.rainxch.githubstore.core.presentation.res.tweaks_entry_connection
import zed.rainxch.tweaks.presentation.TweaksAction
import zed.rainxch.tweaks.presentation.TweaksEvent
import zed.rainxch.tweaks.presentation.TweaksState
import zed.rainxch.tweaks.presentation.TweaksViewModel
import zed.rainxch.tweaks.presentation.components.TweaksSubScreenScaffold
import zed.rainxch.tweaks.presentation.model.ProxyScopeFormState
import zed.rainxch.tweaks.presentation.model.ProxyType

@Composable
fun TweaksConnectionRoot(
    onNavigateBack: () -> Unit,
    viewModel: TweaksViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var pasteSheetOpen by rememberSaveable { mutableStateOf(false) }

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            TweaksEvent.OnProxySaved -> coroutineScope.launch {
                snackbarState.showSnackbar(getString(Res.string.proxy_saved))
            }
            is TweaksEvent.OnProxySaveError -> coroutineScope.launch {
                snackbarState.showSnackbar(event.message)
            }
            is TweaksEvent.OnProxyTestSuccess -> coroutineScope.launch {
                snackbarState.showSnackbar(
                    getString(Res.string.proxy_test_success, event.latencyMs),
                )
            }
            is TweaksEvent.OnProxyTestError -> coroutineScope.launch {
                snackbarState.showSnackbar(event.message)
            }
            is TweaksEvent.OnMasterProxyTestResult -> coroutineScope.launch {
                val parts = listOfNotNull(
                    "Search ${event.searchMs?.let { "✓ ${it}ms" } ?: "✗"}",
                    "Downloads ${event.downloadMs?.let { "✓ ${it}ms" } ?: "✗"}",
                    "Translation ${event.translationMs?.let { "✓ ${it}ms" } ?: "✗"}",
                )
                snackbarState.showSnackbar(parts.joinToString(" · "))
            }
            else -> Unit
        }
    }

    TweaksSubScreenScaffold(
        title = stringResource(Res.string.tweaks_entry_connection),
        onNavigateBack = onNavigateBack,
        snackbarState = snackbarState,
        restartReasons = state.needsRestartReasons,
        onRestartNow = { viewModel.onAction(TweaksAction.OnRestartNowClick) },
        onRestartLater = { viewModel.onAction(TweaksAction.OnRestartLaterClick) },
        showRestartBanner = state.restartBannerVisible,
    ) {
        item(key = "intro") {
            IntroCard()
            Spacer(Modifier.height(16.dp))
        }

        item(key = "main_card") {
            MainConnectionCard(
                form = state.masterProxyForm,
                onAction = { viewModel.onAction(it) },
                onPasteUrl = { pasteSheetOpen = true },
            )
            Spacer(Modifier.height(16.dp))
        }

        item(key = "overrides_card") {
            OverridesCard(
                state = state,
                onAction = { viewModel.onAction(it) },
            )
        }
    }

    if (pasteSheetOpen) {
        PasteProxyUrlSheet(
            onDismiss = { pasteSheetOpen = false },
            onParsed = { parsed ->
                viewModel.onAction(
                    TweaksAction.OnMasterProxyPasteUrl(
                        type = parsed.type,
                        host = parsed.host,
                        port = parsed.port,
                        username = parsed.username,
                        password = parsed.password,
                    ),
                )
                pasteSheetOpen = false
            },
        )
    }
}

@Composable
private fun IntroCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = Radii.row,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(Res.string.tweaks_connection_intro_title),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(Res.string.tweaks_connection_intro_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MainConnectionCard(
    form: ProxyScopeFormState,
    onAction: (TweaksAction) -> Unit,
    onPasteUrl: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = Radii.row,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(Res.string.tweaks_connection_main_section),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(12.dp))

            ModePillSegment(
                selected = form.type,
                onSelected = { onAction(TweaksAction.OnMasterProxyTypeSelected(it)) },
            )

            AnimatedVisibility(visible = form.type == ProxyType.HTTP || form.type == ProxyType.SOCKS) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    ProxyFormFields(
                        form = form,
                        onHostChange = { onAction(TweaksAction.OnMasterProxyHostChanged(it)) },
                        onPortChange = { onAction(TweaksAction.OnMasterProxyPortChanged(it)) },
                        onUserChange = { onAction(TweaksAction.OnMasterProxyUsernameChanged(it)) },
                        onPassChange = { onAction(TweaksAction.OnMasterProxyPasswordChanged(it)) },
                        onPassVisibility = { onAction(TweaksAction.OnMasterProxyPasswordVisibilityToggle) },
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(Res.string.tweaks_connection_applies_to_all),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    GhsButton(
                        onClick = onPasteUrl,
                        label = stringResource(Res.string.tweaks_connection_paste_url),
                        variant = GhsButtonVariant.Text,
                        size = GhsButtonSize.Sm,
                        leadingIcon = Icons.Outlined.ContentPaste,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        GhsButton(
                            onClick = { onAction(TweaksAction.OnMasterProxyTest) },
                            label = stringResource(Res.string.tweaks_connection_test),
                            variant = GhsButtonVariant.Outline,
                            leadingIcon = Icons.Default.NetworkCheck,
                            enabled = !form.isTestInProgress,
                            loading = form.isTestInProgress,
                            modifier = Modifier.weight(1f),
                        )
                        GhsButton(
                            onClick = { onAction(TweaksAction.OnMasterProxySave) },
                            label = stringResource(Res.string.proxy_save),
                            variant = GhsButtonVariant.Primary,
                            leadingIcon = Icons.Default.Save,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

        }
    }
}

@Composable
private fun ProxyFormFields(
    form: ProxyScopeFormState,
    onHostChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onUserChange: (String) -> Unit,
    onPassChange: (String) -> Unit,
    onPassVisibility: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        GhsTextField(
            value = form.host,
            onValueChange = onHostChange,
            modifier = Modifier.weight(2f),
            label = stringResource(Res.string.proxy_host),
        )
        GhsTextField(
            value = form.port,
            onValueChange = onPortChange,
            modifier = Modifier.weight(1f),
            label = stringResource(Res.string.proxy_port),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
    }
    Spacer(Modifier.height(8.dp))
    GhsTextField(
        value = form.username,
        onValueChange = onUserChange,
        modifier = Modifier.fillMaxWidth(),
        label = stringResource(Res.string.proxy_username),
    )
    Spacer(Modifier.height(8.dp))
    GhsTextField(
        value = form.password,
        onValueChange = onPassChange,
        modifier = Modifier.fillMaxWidth(),
        label = stringResource(Res.string.proxy_password),
        visualTransformation = passwordVisualTransformation(form.isPasswordVisible),
        trailingIcon = {
            GhsPasswordVisibilityIcon(
                visible = form.isPasswordVisible,
                onToggle = onPassVisibility,
            )
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ModePillSegment(
    selected: ProxyType,
    onSelected: (ProxyType) -> Unit,
) {
    val items = listOf(
        ProxyType.NONE to (stringResource(Res.string.tweaks_connection_mode_no_proxy) to null),
        ProxyType.SYSTEM to (stringResource(Res.string.tweaks_connection_mode_system) to null),
        ProxyType.HTTP to (
            stringResource(Res.string.tweaks_connection_mode_http) to
                stringResource(Res.string.tweaks_connection_mode_http_caption)
            ),
        ProxyType.SOCKS to (
            stringResource(Res.string.tweaks_connection_mode_socks) to
                stringResource(Res.string.tweaks_connection_mode_socks_caption)
            ),
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items.forEach { (type, labels) ->
                val isSelected = type == selected
                val container = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLow
                }
                val content = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(container)
                        .clickable { onSelected(type) }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = labels.first,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = content,
                        maxLines = 1,
                    )
                }
            }
        }
        val caption = items.firstOrNull { it.first == selected }?.second?.second
        if (!caption.isNullOrBlank()) {
            Text(
                text = caption,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}

@Composable
private fun OverridesCard(
    state: TweaksState,
    onAction: (TweaksAction) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = Radii.row,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(Res.string.tweaks_connection_overrides_section),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(Res.string.tweaks_connection_overrides_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))

            ProxyScope.entries.forEachIndexed { idx, scope ->
                if (idx > 0) Spacer(Modifier.height(8.dp))
                ScopeOverrideRow(
                    scope = scope,
                    useMain = state.useMain(scope),
                    scopeForm = state.formFor(scope),
                    onToggle = { useMain ->
                        onAction(TweaksAction.OnScopeUseMainToggled(scope, useMain))
                    },
                    onAction = onAction,
                )
            }
        }
    }
}

@Composable
private fun ScopeOverrideRow(
    scope: ProxyScope,
    useMain: Boolean,
    scopeForm: ProxyScopeFormState,
    onToggle: (Boolean) -> Unit,
    onAction: (TweaksAction) -> Unit,
) {
    val (title, subtitle) = when (scope) {
        ProxyScope.DISCOVERY -> stringResource(Res.string.tweaks_connection_scope_discovery_title) to
            stringResource(Res.string.tweaks_connection_scope_discovery_body)
        ProxyScope.DOWNLOAD -> stringResource(Res.string.tweaks_connection_scope_download_title) to
            stringResource(Res.string.tweaks_connection_scope_download_body)
        ProxyScope.TRANSLATION -> stringResource(Res.string.tweaks_connection_scope_translation_title) to
            stringResource(Res.string.tweaks_connection_scope_translation_body)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = Radii.chip,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                UseMainSegment(
                    useMain = useMain,
                    onSelected = onToggle,
                )
            }

            AnimatedVisibility(visible = !useMain) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    ModePillSegment(
                        selected = scopeForm.type,
                        onSelected = { onAction(TweaksAction.OnProxyTypeSelected(scope, it)) },
                    )

                    AnimatedVisibility(
                        visible = scopeForm.type == ProxyType.HTTP ||
                            scopeForm.type == ProxyType.SOCKS,
                    ) {
                        Column {
                            Spacer(Modifier.height(12.dp))
                            ProxyFormFields(
                                form = scopeForm,
                                onHostChange = {
                                    onAction(TweaksAction.OnProxyHostChanged(scope, it))
                                },
                                onPortChange = {
                                    onAction(TweaksAction.OnProxyPortChanged(scope, it))
                                },
                                onUserChange = {
                                    onAction(TweaksAction.OnProxyUsernameChanged(scope, it))
                                },
                                onPassChange = {
                                    onAction(TweaksAction.OnProxyPasswordChanged(scope, it))
                                },
                                onPassVisibility = {
                                    onAction(TweaksAction.OnProxyPasswordVisibilityToggle(scope))
                                },
                            )
                            Spacer(Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                GhsButton(
                                    onClick = { onAction(TweaksAction.OnProxyTest(scope)) },
                                    label = stringResource(Res.string.tweaks_connection_test),
                                    variant = GhsButtonVariant.Outline,
                                    leadingIcon = Icons.Default.NetworkCheck,
                                    enabled = !scopeForm.isTestInProgress,
                                    loading = scopeForm.isTestInProgress,
                                    modifier = Modifier.weight(1f),
                                )
                                GhsButton(
                                    onClick = { onAction(TweaksAction.OnProxySave(scope)) },
                                    label = stringResource(Res.string.proxy_save),
                                    variant = GhsButtonVariant.Tonal,
                                    leadingIcon = Icons.Default.Save,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }

                }
            }
        }
    }
}

@Composable
private fun UseMainSegment(
    useMain: Boolean,
    onSelected: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        SegmentChip(
            label = stringResource(Res.string.tweaks_connection_use_main),
            selected = useMain,
            onClick = { onSelected(true) },
        )
        SegmentChip(
            label = stringResource(Res.string.tweaks_connection_custom),
            selected = !useMain,
            onClick = { onSelected(false) },
        )
    }
}

@Composable
private fun SegmentChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val container = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        Color.Transparent
    }
    val content = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(container)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
            ),
            color = content,
        )
    }
}
