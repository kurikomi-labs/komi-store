package zed.rainxch.details.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.details.presentation.model.SupportedLanguages
import zed.rainxch.details.presentation.model.TranslationState
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.translation_card_cancel
import zed.rainxch.githubstore.core.presentation.res.translation_card_change_language
import zed.rainxch.githubstore.core.presentation.res.translation_card_detected_source
import zed.rainxch.githubstore.core.presentation.res.translation_card_retry
import zed.rainxch.githubstore.core.presentation.res.translation_card_show_original
import zed.rainxch.githubstore.core.presentation.res.translation_card_show_translation
import zed.rainxch.githubstore.core.presentation.res.translation_card_subtitle_idle
import zed.rainxch.githubstore.core.presentation.res.translation_card_subtitle_original
import zed.rainxch.githubstore.core.presentation.res.translation_card_subtitle_translated
import zed.rainxch.githubstore.core.presentation.res.translation_card_subtitle_translating
import zed.rainxch.githubstore.core.presentation.res.translation_card_target_label
import zed.rainxch.githubstore.core.presentation.res.translation_card_title
import zed.rainxch.githubstore.core.presentation.res.translation_card_translate_to

@Composable
fun TranslationCard(
    state: TranslationState,
    deviceLanguageCode: String,
    onPickLanguage: () -> Unit,
    onTranslate: (String) -> Unit,
    onToggle: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val effectiveTargetCode = state.targetLanguageCode ?: deviceLanguageCode
    val effectiveTargetName = state.targetLanguageDisplayName
        ?: SupportedLanguages.all.firstOrNull { it.code == effectiveTargetCode }?.displayName
        ?: effectiveTargetCode

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Header(state = state, displayName = effectiveTargetName)

            Spacer(Modifier.height(14.dp))

            TargetLanguageRow(
                displayName = effectiveTargetName,
                onPick = onPickLanguage,
            )

            Spacer(Modifier.height(12.dp))

            ActionRow(
                state = state,
                effectiveTargetCode = effectiveTargetCode,
                effectiveTargetName = effectiveTargetName,
                onTranslate = onTranslate,
                onToggle = onToggle,
                onCancel = onCancel,
            )

            if (state.error != null) {
                Spacer(Modifier.height(10.dp))
                ErrorRow(
                    message = state.error,
                    onRetry = { onTranslate(effectiveTargetCode) },
                )
            }

            if (state.translatedText != null && state.detectedSourceLanguage != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(
                        Res.string.translation_card_detected_source,
                        SupportedLanguages.all.firstOrNull { it.code == state.detectedSourceLanguage }?.displayName
                            ?: state.detectedSourceLanguage,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun Header(state: TranslationState, displayName: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Translate,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(Res.string.translation_card_title),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            val subtitle = when {
                state.isTranslating -> stringResource(Res.string.translation_card_subtitle_translating)
                state.translatedText != null && state.isShowingTranslation ->
                    stringResource(Res.string.translation_card_subtitle_translated, displayName)
                state.translatedText != null && !state.isShowingTranslation ->
                    stringResource(Res.string.translation_card_subtitle_original)
                else -> stringResource(Res.string.translation_card_subtitle_idle)
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun TargetLanguageRow(
    displayName: String,
    onPick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(Res.string.translation_card_target_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onPick),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Language,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = stringResource(Res.string.translation_card_change_language),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun ActionRow(
    state: TranslationState,
    effectiveTargetCode: String,
    effectiveTargetName: String,
    onTranslate: (String) -> Unit,
    onToggle: () -> Unit,
    onCancel: () -> Unit,
) {
    when {
        state.isTranslating -> {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 16.dp),
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                )
                Spacer(Modifier.width(10.dp))
                Text(stringResource(Res.string.translation_card_cancel), fontWeight = FontWeight.SemiBold)
            }
        }

        state.translatedText != null -> {
            OutlinedButton(
                onClick = onToggle,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 16.dp),
            ) {
                Text(
                    text = if (state.isShowingTranslation) {
                        stringResource(Res.string.translation_card_show_original)
                    } else {
                        stringResource(Res.string.translation_card_show_translation)
                    },
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        else -> {
            Button(
                onClick = { onTranslate(effectiveTargetCode) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Translate,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(
                        Res.string.translation_card_translate_to,
                        effectiveTargetName,
                    ),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ErrorRow(
    message: String,
    onRetry: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error)
                    .clickable(onClick = onRetry)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(Res.string.translation_card_retry),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onError,
                )
            }
        }
    }
}
