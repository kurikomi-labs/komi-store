package zed.rainxch.tweaks.presentation.components.sections

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.core.domain.model.ProxyScope
import zed.rainxch.githubstore.core.presentation.res.*
import zed.rainxch.tweaks.presentation.TweaksAction
import zed.rainxch.tweaks.presentation.TweaksState
import zed.rainxch.tweaks.presentation.components.SectionHeader
import zed.rainxch.tweaks.presentation.model.ProxyScopeFormState
import zed.rainxch.tweaks.presentation.model.ProxyType

fun LazyListScope.networkSection(
    state: TweaksState,
    onAction: (TweaksAction) -> Unit,
) {
    item {
        SectionHeader(text = stringResource(Res.string.section_network))
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(Res.string.proxy_scope_intro),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
        Spacer(Modifier.height(4.dp))
    }

    item {
        OutlinedCard(
            onClick = { onAction(TweaksAction.OnMirrorPickerClick) },
            colors =
                CardDefaults.outlinedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                ),
            shape = RoundedCornerShape(32.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.NetworkCheck,
                    contentDescription = null,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(Res.string.mirror_tweaks_entry_label),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                )
            }
        }
    }
    item {
        Spacer(Modifier.height(12.dp))
        OutlinedCard(
            onClick = { onAction(TweaksAction.OnOpenCustomForgesDialog) },
            colors =
                CardDefaults.outlinedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                ),
            shape = RoundedCornerShape(32.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Dns,
                    contentDescription = null,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(Res.string.custom_forges_entry_label),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    val count = state.customForgeHosts.size
                    if (count > 0) {
                        Text(
                            text = stringResource(Res.string.custom_forges_count_label, count),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Text(
                            text = stringResource(Res.string.custom_forges_entry_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                )
            }
        }
    }
    item {
        Spacer(Modifier.height(16.dp))
    }

    // One card per scope. Ordering mirrors the user's mental model:
    // browsing → downloading → translation (least-common last).
    ProxyScope.entries.forEach { scope ->
        item {
            ProxyScopeCard(
                scope = scope,
                form = state.formFor(scope),
                onAction = onAction,
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}

private fun scopeTitleRes(scope: ProxyScope): StringResource =
    when (scope) {
        ProxyScope.DISCOVERY -> Res.string.proxy_scope_discovery_title
        ProxyScope.DOWNLOAD -> Res.string.proxy_scope_download_title
        ProxyScope.TRANSLATION -> Res.string.proxy_scope_translation_title
    }

private fun scopeDescriptionRes(scope: ProxyScope): StringResource =
    when (scope) {
        ProxyScope.DISCOVERY -> Res.string.proxy_scope_discovery_description
        ProxyScope.DOWNLOAD -> Res.string.proxy_scope_download_description
        ProxyScope.TRANSLATION -> Res.string.proxy_scope_translation_description
    }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ProxyScopeCard(
    scope: ProxyScope,
    form: ProxyScopeFormState,
    onAction: (TweaksAction) -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
        shape = RoundedCornerShape(32.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(scopeTitleRes(scope)),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(scopeDescriptionRes(scope)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )

            Spacer(Modifier.height(12.dp))

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(ProxyType.entries) { type ->
                    FilterChip(
                        selected = form.type == type,
                        onClick = { onAction(TweaksAction.OnProxyTypeSelected(scope, type)) },
                        label = {
                            Text(
                                text =
                                    when (type) {
                                        ProxyType.NONE -> stringResource(Res.string.proxy_none)
                                        ProxyType.SYSTEM -> stringResource(Res.string.proxy_system)
                                        ProxyType.HTTP -> stringResource(Res.string.proxy_http)
                                        ProxyType.SOCKS -> stringResource(Res.string.proxy_socks)
                                    },
                                fontWeight = if (form.type == type) FontWeight.Bold else FontWeight.Normal,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                    )
                }
            }

            AnimatedVisibility(
                visible = form.type == ProxyType.NONE || form.type == ProxyType.SYSTEM,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text =
                            when (form.type) {
                                ProxyType.SYSTEM -> stringResource(Res.string.proxy_system_description)
                                else -> stringResource(Res.string.proxy_none_description)
                            },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    ProxyTestButton(
                        isInProgress = form.isTestInProgress,
                        enabled = !form.isTestInProgress,
                        onClick = { onAction(TweaksAction.OnProxyTest(scope)) },
                    )
                }
            }

            AnimatedVisibility(
                visible = form.type == ProxyType.HTTP || form.type == ProxyType.SOCKS,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                ProxyDetailsFields(
                    scope = scope,
                    form = form,
                    onAction = onAction,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ProxyDetailsFields(
    scope: ProxyScope,
    form: ProxyScopeFormState,
    onAction: (TweaksAction) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val portValue = form.port
    val isPortInvalid =
        portValue.isNotEmpty() &&
            (portValue.toIntOrNull()?.let { it !in 1..65535 } ?: true)
    val hostValue = form.host
    // Real-time host validation mirrors the port pattern: only flag as
    // invalid once the user has typed something, so the empty initial
    // state doesn't shout an error at them.
    val isHostInvalid = hostValue.isNotEmpty() && !isLikelyValidProxyHost(hostValue)
    val isFormValid =
        hostValue.isNotEmpty() &&
            !isHostInvalid &&
            portValue.isNotEmpty() &&
            portValue.toIntOrNull()?.let { it in 1..65535 } == true

    Column(
        modifier = Modifier.padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = form.host,
                onValueChange = { onAction(TweaksAction.OnProxyHostChanged(scope, it)) },
                label = { Text(stringResource(Res.string.proxy_host)) },
                placeholder = { Text("127.0.0.1") },
                singleLine = true,
                isError = isHostInvalid,
                supportingText =
                    if (isHostInvalid) {
                        { Text(stringResource(Res.string.proxy_host_invalid)) }
                    } else {
                        null
                    },
                modifier = Modifier.weight(2f),
                shape = RoundedCornerShape(12.dp),
            )

            OutlinedTextField(
                value = form.port,
                onValueChange = { onAction(TweaksAction.OnProxyPortChanged(scope, it)) },
                label = { Text(stringResource(Res.string.proxy_port)) },
                placeholder = { Text("1080") },
                singleLine = true,
                isError = isPortInvalid,
                supportingText =
                    if (isPortInvalid) {
                        { Text(stringResource(Res.string.proxy_port_error)) }
                    } else {
                        null
                    },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
            )
        }

        OutlinedTextField(
            value = form.username,
            onValueChange = { onAction(TweaksAction.OnProxyUsernameChanged(scope, it)) },
            label = { Text(stringResource(Res.string.proxy_username)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
        )

        OutlinedTextField(
            value = form.password,
            onValueChange = { onAction(TweaksAction.OnProxyPasswordChanged(scope, it)) },
            label = { Text(stringResource(Res.string.proxy_password)) },
            singleLine = true,
            visualTransformation =
                if (form.isPasswordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
            trailingIcon = {
                IconButton(
                    onClick = { onAction(TweaksAction.OnProxyPasswordVisibilityToggle(scope)) },
                ) {
                    Icon(
                        imageVector =
                            if (form.isPasswordVisible) {
                                Icons.Default.VisibilityOff
                            } else {
                                Icons.Default.Visibility
                            },
                        contentDescription =
                            if (form.isPasswordVisible) {
                                stringResource(Res.string.proxy_hide_password)
                            } else {
                                stringResource(Res.string.proxy_show_password)
                            },
                        modifier = Modifier.size(20.dp),
                    )
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
        )

        Row(
            modifier = Modifier.align(Alignment.End),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProxyTestButton(
                isInProgress = form.isTestInProgress,
                // Keep enabled regardless of form validity so the user
                // never taps a disabled button by accident on the first
                // press (the previous `isFormValid` gate raced with
                // the IME-composition-commit step on some Android
                // keyboards and silently swallowed taps). VM still
                // validates and surfaces a clear error event.
                enabled = !form.isTestInProgress,
                onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    onAction(TweaksAction.OnProxyTest(scope))
                },
            )

            FilledTonalButton(
                onClick = {
                    // Hide IME first so any pending composition commits
                    // synchronously before the VM reads form state.
                    // `clearFocus()` alone isn't enough on Gboard /
                    // SwiftKey, which keep characters in a composition
                    // buffer until focus actually transfers — that's
                    // what made the user have to tap Save twice.
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    onAction(TweaksAction.OnProxySave(scope))
                },
                enabled = !form.isTestInProgress,
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.size(8.dp))
                Text(stringResource(Res.string.proxy_save))
            }
        }
    }
}

@Composable
private fun ProxyTestButton(
    isInProgress: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
    ) {
        if (isInProgress) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.size(8.dp))
            Text(stringResource(Res.string.proxy_test_in_progress))
        } else {
            Icon(
                imageVector = Icons.Default.NetworkCheck,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.size(8.dp))
            Text(stringResource(Res.string.proxy_test))
        }
    }
}

/**
 * UI-side mirror of `TweaksViewModel.isValidProxyHost`. Drives the
 * real-time `isError` highlight on the host TextField so the user
 * sees a problem before they tap Save — same UX as the port field.
 * Authoritative validation still happens server-side in the VM; this
 * is the optimistic, free-of-cost preview.
 */
private fun isLikelyValidProxyHost(raw: String): Boolean {
    val host = raw.trim()
    if (host.isBlank()) return false
    if (host.length > 253) return false
    if (host.any { it.isWhitespace() }) return false
    if (host.contains("://") || host.contains("/") ||
        host.contains("?") || host.contains("#")
    ) {
        return false
    }
    if (IPV4_PATTERN.matches(host)) return true
    val ipv6Candidate = host.trim('[', ']')
    if (ipv6Candidate.contains(":") && IPV6_PATTERN.matches(ipv6Candidate)) return true
    return HOSTNAME_PATTERN.matches(host)
}

private val IPV4_PATTERN =
    Regex(
        "^(25[0-5]|2[0-4]\\d|[01]?\\d?\\d)" +
            "(\\.(25[0-5]|2[0-4]\\d|[01]?\\d?\\d)){3}$",
    )

private val IPV6_PATTERN = Regex("^[0-9A-Fa-f:]+$")

private val HOSTNAME_PATTERN =
    Regex(
        "^(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)" +
            "(?:\\.(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?))*$",
    )
