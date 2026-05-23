package zed.rainxch.tweaks.presentation.connection

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import zed.rainxch.core.domain.model.ProxyScope
import zed.rainxch.core.presentation.theme.shapes.WonkySquircleShape
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
                text = "How the app reaches the internet",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Pick a connection mode below. Most people leave this on No proxy.",
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
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = Radii.row,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Main connection",
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
                        text = "Applies to all traffic unless overridden below.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = { onAction(TweaksAction.OnMasterProxyTest) },
                            shape = Radii.chip,
                            modifier = Modifier.weight(1f),
                            enabled = !form.isTestInProgress,
                        ) {
                            if (form.isTestInProgress) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.NetworkCheck,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            Spacer(Modifier.size(6.dp))
                            Text(text = "Test main connection")
                        }
                        FilledTonalButton(
                            onClick = { onAction(TweaksAction.OnMasterProxySave) },
                            shape = WonkySquircleShape.CtaPrimary,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.size(6.dp))
                            Text(text = stringResource(Res.string.proxy_save))
                        }
                    }
                }
            }

            AnimatedVisibility(visible = form.type == ProxyType.NONE || form.type == ProxyType.SYSTEM) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    FilledTonalButton(
                        onClick = { onAction(TweaksAction.OnMasterProxySave) },
                        shape = WonkySquircleShape.CtaPrimary,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.size(6.dp))
                        Text(text = stringResource(Res.string.proxy_save))
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
    val colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.outline,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = form.host,
            onValueChange = onHostChange,
            modifier = Modifier.weight(2f),
            label = { Text(text = stringResource(Res.string.proxy_host)) },
            singleLine = true,
            shape = Radii.chip,
            colors = colors,
        )
        OutlinedTextField(
            value = form.port,
            onValueChange = onPortChange,
            modifier = Modifier.weight(1f),
            label = { Text(text = stringResource(Res.string.proxy_port)) },
            singleLine = true,
            shape = Radii.chip,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = colors,
        )
    }
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = form.username,
        onValueChange = onUserChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(text = stringResource(Res.string.proxy_username)) },
        singleLine = true,
        shape = Radii.chip,
        colors = colors,
    )
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = form.password,
        onValueChange = onPassChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(text = stringResource(Res.string.proxy_password)) },
        singleLine = true,
        visualTransformation = if (form.isPasswordVisible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        trailingIcon = {
            IconButton(onClick = onPassVisibility) {
                Icon(
                    imageVector = if (form.isPasswordVisible) {
                        Icons.Filled.VisibilityOff
                    } else {
                        Icons.Filled.Visibility
                    },
                    contentDescription = null,
                )
            }
        },
        shape = Radii.chip,
        colors = colors,
    )
}

@Composable
private fun ModePillSegment(
    selected: ProxyType,
    onSelected: (ProxyType) -> Unit,
) {
    val items = listOf(
        ProxyType.NONE to ("No proxy" to null),
        ProxyType.SYSTEM to ("System" to null),
        ProxyType.HTTP to ("HTTP/HTTPS" to "Most corporate proxies."),
        ProxyType.SOCKS to ("SOCKS5" to "Tor, SSH tunnels."),
    )

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items.forEach { (type, labels) ->
                val isSelected = type == selected
                val container = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    Color.Transparent
                }
                val content = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(container)
                        .clickable { onSelected(type) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = labels.first,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = content,
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
                modifier = Modifier.padding(start = 8.dp),
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
                text = "Per-scope overrides",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Each scope uses the main connection by default. Choose 'Custom' to keep its own settings.",
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
) {
    val (title, subtitle) = when (scope) {
        ProxyScope.DISCOVERY -> "Search & metadata" to "GitHub API, search, repo details."
        ProxyScope.DOWNLOAD -> "Downloads" to "APK and asset downloads."
        ProxyScope.TRANSLATION -> "Translation" to "DeepL, Microsoft, LibreTranslate calls."
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
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = scopeStatusLabel(scopeForm),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun scopeStatusLabel(form: ProxyScopeFormState): String =
    when (form.type) {
        ProxyType.NONE -> "Custom: no proxy"
        ProxyType.SYSTEM -> "Custom: system"
        ProxyType.HTTP -> "Custom: HTTP ${form.host.ifBlank { "—" }}:${form.port.ifBlank { "—" }}"
        ProxyType.SOCKS -> "Custom: SOCKS5 ${form.host.ifBlank { "—" }}:${form.port.ifBlank { "—" }}"
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
            label = "Use main",
            selected = useMain,
            onClick = { onSelected(true) },
        )
        SegmentChip(
            label = "Custom",
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
