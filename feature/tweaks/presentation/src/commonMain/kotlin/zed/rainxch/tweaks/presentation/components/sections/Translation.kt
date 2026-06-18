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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.core.domain.model.settings.SupportedTranslationLanguages
import zed.rainxch.core.domain.model.settings.TranslationProvider
import zed.rainxch.core.presentation.components.buttons.KomiButton
import zed.rainxch.core.presentation.components.buttons.KomiButtonSize
import zed.rainxch.core.presentation.components.buttons.KomiButtonVariant
import zed.rainxch.core.presentation.components.chips.KomiChip
import zed.rainxch.core.presentation.components.chips.KomiChipKind
import zed.rainxch.core.presentation.components.inputs.KomiTextField
import zed.rainxch.githubstore.core.presentation.res.*
import zed.rainxch.tweaks.presentation.TweaksAction
import zed.rainxch.tweaks.presentation.TweaksState

@Composable
fun translationSectionContent(
    state: TweaksState,
    onAction: (TweaksAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
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
                    KomiChip(
                        label = providerLabel(provider),
                        kind = KomiChipKind.Filter,
                        selected = state.displayedTranslationProvider == provider,
                        onClick = { onAction(TweaksAction.OnTranslationProviderSelected(provider)) },
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

            AnimatedVisibility(
                visible = state.displayedTranslationProvider == TranslationProvider.MICROSOFT,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                MicrosoftCredentialsForm(
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
        TranslationProvider.MICROSOFT -> stringResource(Res.string.translation_provider_microsoft)
    }

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

        KomiTextField(
            value = state.youdaoAppKey,
            onValueChange = { onAction(TweaksAction.OnYoudaoAppKeyChanged(it)) },
            label = stringResource(Res.string.translation_youdao_app_key),
            modifier = Modifier.fillMaxWidth(),
        )

        KomiTextField(
            value = state.youdaoAppSecret,
            onValueChange = { onAction(TweaksAction.OnYoudaoAppSecretChanged(it)) },
            label = stringResource(Res.string.translation_youdao_app_secret),
            password = true,
            keyboardType = KeyboardType.Password,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.align(Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            KomiButton(
                onClick = { onAction(TweaksAction.OnYoudaoCredentialsSave) },
                label = stringResource(Res.string.translation_youdao_save),
                variant = KomiButtonVariant.Tonal,
                enabled = canSave,
                leadingIcon = Icons.Default.Save,
            )
        }
    }
}

@Composable
private fun LibreTranslateCredentialsForm(
    state: TweaksState,
    onAction: (TweaksAction) -> Unit,
) {

    val canSave = true

    Column(
        modifier = Modifier.padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(Res.string.translation_libre_help),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        KomiTextField(
            value = state.libreTranslateBaseUrl,
            onValueChange = { onAction(TweaksAction.OnLibreTranslateBaseUrlChanged(it)) },
            label = stringResource(Res.string.translation_libre_base_url),
            placeholder = "https://translate.disroot.org",
            keyboardType = KeyboardType.Uri,
            modifier = Modifier.fillMaxWidth(),
        )

        KomiTextField(
            value = state.libreTranslateApiKey,
            onValueChange = { onAction(TweaksAction.OnLibreTranslateApiKeyChanged(it)) },
            label = stringResource(Res.string.translation_libre_api_key),
            password = true,
            keyboardType = KeyboardType.Password,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.align(Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            KomiButton(
                onClick = { onAction(TweaksAction.OnLibreTranslateCredentialsSave) },
                label = stringResource(Res.string.translation_libre_save),
                variant = KomiButtonVariant.Tonal,
                enabled = canSave,
                leadingIcon = Icons.Default.Save,
            )
        }
    }
}

@Composable
private fun DeeplCredentialsForm(
    state: TweaksState,
    onAction: (TweaksAction) -> Unit,
) {
    val canSave = state.deeplAuthKey.isNotBlank()
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

    Column(
        modifier = Modifier.padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(Res.string.translation_deepl_help),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        KomiButton(
            onClick = { runCatching { uriHandler.openUri("https://www.deepl.com/pro-api") } },
            label = stringResource(Res.string.translation_deepl_get_free_key),
            variant = KomiButtonVariant.Text,
            size = KomiButtonSize.Sm,
            modifier = Modifier.align(Alignment.Start),
        )

        KomiTextField(
            value = state.deeplAuthKey,
            onValueChange = { onAction(TweaksAction.OnDeeplAuthKeyChanged(it)) },
            label = stringResource(Res.string.translation_deepl_auth_key),
            password = true,
            keyboardType = KeyboardType.Password,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.align(Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            KomiButton(
                onClick = { onAction(TweaksAction.OnDeeplCredentialsSave) },
                label = stringResource(Res.string.translation_deepl_save),
                variant = KomiButtonVariant.Tonal,
                enabled = canSave,
                leadingIcon = Icons.Default.Save,
            )
        }
    }
}

@Composable
private fun MicrosoftCredentialsForm(
    state: TweaksState,
    onAction: (TweaksAction) -> Unit,
) {
    val canSave = state.microsoftTranslatorKey.isNotBlank()
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

    Column(
        modifier = Modifier.padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(Res.string.translation_microsoft_help),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        KomiButton(
            onClick = { runCatching { uriHandler.openUri("https://portal.azure.com/#create/Microsoft.CognitiveServicesTextTranslation") } },
            label = stringResource(Res.string.translation_microsoft_get_free_key),
            variant = KomiButtonVariant.Text,
            size = KomiButtonSize.Sm,
            modifier = Modifier.align(Alignment.Start),
        )

        KomiTextField(
            value = state.microsoftTranslatorKey,
            onValueChange = { onAction(TweaksAction.OnMicrosoftTranslatorKeyChanged(it)) },
            label = stringResource(Res.string.translation_microsoft_key),
            password = true,
            keyboardType = KeyboardType.Password,
            modifier = Modifier.fillMaxWidth(),
        )

        KomiTextField(
            value = state.microsoftTranslatorRegion,
            onValueChange = { onAction(TweaksAction.OnMicrosoftTranslatorRegionChanged(it)) },
            label = stringResource(Res.string.translation_microsoft_region),
            placeholder = "global",
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.align(Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            KomiButton(
                onClick = { onAction(TweaksAction.OnMicrosoftTranslatorCredentialsSave) },
                label = stringResource(Res.string.translation_microsoft_save),
                variant = KomiButtonVariant.Tonal,
                enabled = canSave,
                leadingIcon = Icons.Default.Save,
            )
        }
    }
}
