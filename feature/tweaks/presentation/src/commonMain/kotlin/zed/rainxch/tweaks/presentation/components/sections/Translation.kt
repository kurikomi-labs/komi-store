package zed.rainxch.tweaks.presentation.components.sections

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.core.domain.model.SupportedTranslationLanguages
import zed.rainxch.core.domain.model.TranslationProvider
import zed.rainxch.githubstore.core.presentation.res.*
import zed.rainxch.tweaks.presentation.TweaksAction
import zed.rainxch.tweaks.presentation.TweaksState
import zed.rainxch.tweaks.presentation.components.SectionHeader

fun LazyListScope.translationSection(
    state: TweaksState,
    onAction: (TweaksAction) -> Unit,
) {
    item {
        SectionHeader(text = stringResource(Res.string.section_translation))
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(Res.string.translation_intro),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
        Spacer(Modifier.height(8.dp))

        TranslationProviderCard(
            state = state,
            onAction = onAction,
        )

        Spacer(Modifier.height(8.dp))

        AutoTranslateCard(
            enabled = state.autoTranslateEnabled,
            targetLanguageTag = state.autoTranslateTargetLang,
            appLanguageTag = state.selectedAppLanguage,
            onToggle = { onAction(TweaksAction.OnAutoTranslateEnabledToggle(it)) },
            onTargetSelected = { tag ->
                onAction(TweaksAction.OnAutoTranslateTargetSelected(tag))
            },
        )
    }
}

@Composable
private fun AutoTranslateCard(
    enabled: Boolean,
    targetLanguageTag: String?,
    appLanguageTag: String?,
    onToggle: (Boolean) -> Unit,
    onTargetSelected: (String?) -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        shape = RoundedCornerShape(32.dp),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(Res.string.translation_auto_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(Res.string.translation_auto_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = onToggle,
                )
            }
            if (enabled) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.translation_auto_target_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    TranslationTargetDropdown(
                        selectedTag = targetLanguageTag,
                        onLanguageSelected = onTargetSelected,
                    )
                    if (targetLanguageTag == null && !appLanguageTag.isNullOrBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = stringResource(
                                Res.string.translation_auto_target_followup,
                                appLanguageTag,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TranslationProviderCard(
    state: TweaksState,
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
                text = stringResource(Res.string.translation_provider_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(Res.string.translation_provider_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )

            Spacer(Modifier.height(12.dp))

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(TranslationProvider.entries) { provider ->
                    FilterChip(
                        selected = state.displayedTranslationProvider == provider,
                        onClick = { onAction(TweaksAction.OnTranslationProviderSelected(provider)) },
                        label = {
                            Text(
                                text = providerLabel(provider),
                                fontWeight =
                                    if (state.displayedTranslationProvider == provider) {
                                        FontWeight.Bold
                                    } else {
                                        FontWeight.Normal
                                    },
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
                visible = state.displayedTranslationProvider == TranslationProvider.YOUDAO,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                YoudaoCredentialsForm(
                    state = state,
                    onAction = onAction,
                )
            }

            AnimatedVisibility(
                visible = state.displayedTranslationProvider == TranslationProvider.LIBRE_TRANSLATE,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                LibreTranslateCredentialsForm(
                    state = state,
                    onAction = onAction,
                )
            }

            AnimatedVisibility(
                visible = state.displayedTranslationProvider == TranslationProvider.DEEPL,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                DeeplCredentialsForm(
                    state = state,
                    onAction = onAction,
                )
            }
        }
    }
}

@Composable
private fun providerLabel(provider: TranslationProvider): String =
    when (provider) {
        TranslationProvider.GOOGLE -> stringResource(Res.string.translation_provider_google)
        TranslationProvider.YOUDAO -> stringResource(Res.string.translation_provider_youdao)
        TranslationProvider.LIBRE_TRANSLATE -> stringResource(Res.string.translation_provider_libre)
        TranslationProvider.DEEPL -> stringResource(Res.string.translation_provider_deepl)
    }

/**
 * Dropdown for picking the auto-translate target language.
 *
 * Distinct from [LanguageDropdown]: that one is bound to `AppLanguages` —
 * the 13 locales the app ships UI translations for. Translation targets
 * are a wider set ([SupportedTranslationLanguages.all] — 33 entries
 * spanning everything the translation service can produce, including
 * German, Dutch, Portuguese, Ukrainian, Vietnamese, etc. that the app
 * itself isn't translated into).
 */
@Composable
private fun TranslationTargetDropdown(
    selectedTag: String?,
    onLanguageSelected: (String?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val currentLabel = when (val match = SupportedTranslationLanguages.findByCode(selectedTag)) {
        null -> stringResource(Res.string.language_follow_system)
        else -> match.displayName
    }
    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .clickable { expanded = true }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = currentLabel,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Spacer(Modifier.size(8.dp))
            Icon(
                imageVector = Icons.Default.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.language_follow_system)) },
                onClick = {
                    onLanguageSelected(null)
                    expanded = false
                },
                trailingIcon = {
                    if (selectedTag == null) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                },
            )
            SupportedTranslationLanguages.all.forEach { lang ->
                DropdownMenuItem(
                    text = { Text(lang.displayName) },
                    onClick = {
                        onLanguageSelected(lang.code)
                        expanded = false
                    },
                    trailingIcon = {
                        if (selectedTag.equals(lang.code, ignoreCase = true)) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun YoudaoCredentialsForm(
    state: TweaksState,
    onAction: (TweaksAction) -> Unit,
) {
    val canSave =
        state.youdaoAppKey.isNotBlank() && state.youdaoAppSecret.isNotBlank()

    Column(
        modifier = Modifier.padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(Res.string.translation_youdao_help),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            value = state.youdaoAppKey,
            onValueChange = { onAction(TweaksAction.OnYoudaoAppKeyChanged(it)) },
            label = { Text(stringResource(Res.string.translation_youdao_app_key)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
        )

        OutlinedTextField(
            value = state.youdaoAppSecret,
            onValueChange = { onAction(TweaksAction.OnYoudaoAppSecretChanged(it)) },
            label = { Text(stringResource(Res.string.translation_youdao_app_secret)) },
            singleLine = true,
            visualTransformation =
                if (state.isYoudaoAppSecretVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
            trailingIcon = {
                IconButton(
                    onClick = { onAction(TweaksAction.OnYoudaoAppSecretVisibilityToggle) },
                ) {
                    Icon(
                        imageVector =
                            if (state.isYoudaoAppSecretVisible) {
                                Icons.Default.VisibilityOff
                            } else {
                                Icons.Default.Visibility
                            },
                        contentDescription =
                            if (state.isYoudaoAppSecretVisible) {
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
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledTonalButton(
                onClick = { onAction(TweaksAction.OnYoudaoCredentialsSave) },
                enabled = canSave,
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.size(8.dp))
                Text(stringResource(Res.string.translation_youdao_save))
            }
        }
    }
}

@Composable
private fun LibreTranslateCredentialsForm(
    state: TweaksState,
    onAction: (TweaksAction) -> Unit,
) {
    val canSave = state.libreTranslateBaseUrl.isNotBlank()

    Column(
        modifier = Modifier.padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(Res.string.translation_libre_help),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            value = state.libreTranslateBaseUrl,
            onValueChange = { onAction(TweaksAction.OnLibreTranslateBaseUrlChanged(it)) },
            label = { Text(stringResource(Res.string.translation_libre_base_url)) },
            placeholder = { Text("https://translate.example.com") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
        )

        OutlinedTextField(
            value = state.libreTranslateApiKey,
            onValueChange = { onAction(TweaksAction.OnLibreTranslateApiKeyChanged(it)) },
            label = { Text(stringResource(Res.string.translation_libre_api_key)) },
            singleLine = true,
            visualTransformation =
                if (state.isLibreTranslateApiKeyVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
            trailingIcon = {
                IconButton(
                    onClick = { onAction(TweaksAction.OnLibreTranslateApiKeyVisibilityToggle) },
                ) {
                    Icon(
                        imageVector =
                            if (state.isLibreTranslateApiKeyVisible) {
                                Icons.Default.VisibilityOff
                            } else {
                                Icons.Default.Visibility
                            },
                        contentDescription =
                            if (state.isLibreTranslateApiKeyVisible) {
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
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledTonalButton(
                onClick = { onAction(TweaksAction.OnLibreTranslateCredentialsSave) },
                enabled = canSave,
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.size(8.dp))
                Text(stringResource(Res.string.translation_libre_save))
            }
        }
    }
}

@Composable
private fun DeeplCredentialsForm(
    state: TweaksState,
    onAction: (TweaksAction) -> Unit,
) {
    val canSave = state.deeplAuthKey.isNotBlank()

    Column(
        modifier = Modifier.padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(Res.string.translation_deepl_help),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            value = state.deeplAuthKey,
            onValueChange = { onAction(TweaksAction.OnDeeplAuthKeyChanged(it)) },
            label = { Text(stringResource(Res.string.translation_deepl_auth_key)) },
            singleLine = true,
            visualTransformation =
                if (state.isDeeplAuthKeyVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
            trailingIcon = {
                IconButton(
                    onClick = { onAction(TweaksAction.OnDeeplAuthKeyVisibilityToggle) },
                ) {
                    Icon(
                        imageVector =
                            if (state.isDeeplAuthKeyVisible) {
                                Icons.Default.VisibilityOff
                            } else {
                                Icons.Default.Visibility
                            },
                        contentDescription =
                            if (state.isDeeplAuthKeyVisible) {
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
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledTonalButton(
                onClick = { onAction(TweaksAction.OnDeeplCredentialsSave) },
                enabled = canSave,
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.size(8.dp))
                Text(stringResource(Res.string.translation_deepl_save))
            }
        }
    }
}
