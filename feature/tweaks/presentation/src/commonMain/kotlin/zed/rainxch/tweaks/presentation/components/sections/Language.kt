package zed.rainxch.tweaks.presentation.components.sections

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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.core.domain.model.AppLanguages
import zed.rainxch.githubstore.core.presentation.res.*
import zed.rainxch.tweaks.presentation.TweaksAction
import zed.rainxch.tweaks.presentation.TweaksState
import zed.rainxch.tweaks.presentation.components.SectionHeader

fun LazyListScope.languageSection(
    state: TweaksState,
    onAction: (TweaksAction) -> Unit,
) {
    item {
        SectionHeader(text = stringResource(Res.string.section_language))
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(Res.string.language_intro),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
        Spacer(Modifier.height(8.dp))

        LanguagePickerCard(
            state = state,
            onAction = onAction,
        )
    }
}

@Composable
private fun LanguagePickerCard(
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
                text = stringResource(Res.string.language_picker_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(Res.string.language_picker_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )

            Spacer(Modifier.height(12.dp))

            LanguageDropdown(
                selectedTag = state.selectedAppLanguage,
                onLanguageSelected = { tag ->
                    onAction(TweaksAction.OnAppLanguageSelected(tag))
                },
            )
        }
    }
}

@Composable
internal fun LanguageDropdown(
    selectedTag: String?,
    onLanguageSelected: (String?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    val currentLabel =
        when (val match = AppLanguages.findByTag(selectedTag)) {
            null -> stringResource(Res.string.language_follow_system)
            else -> match.displayName
        }

    Box(modifier = Modifier.fillMaxWidth()) {
        // Anchor row — tappable area that shows the current value and
        // toggles the menu. Uses a `surface`-tinted background so it
        // reads as a pickable control against the parent card's
        // `surfaceContainer`; the plain clickable row otherwise
        // blends into the card.
        Row(
            modifier =
                Modifier
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
            // Default menu container is `surfaceContainer`, the same
            // tone the parent `ElevatedCard` uses — the menu would
            // visually dissolve into the card. Step up to
            // `surfaceContainerHigh` so it reads as a distinct popup
            // layer with the correct elevation contrast.
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            // Follow-system first — it's the default and users
            // escaping a wrong-language lock-in look for this first.
            DropdownMenuItem(
                text = { DropdownItemText(stringResource(Res.string.language_follow_system)) },
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

            AppLanguages.ALL.forEach { language ->
                DropdownMenuItem(
                    text = {
                        // Native-script label so a user stuck in the
                        // wrong language can still recognise their
                        // own and escape.
                        DropdownItemText(language.displayName)
                    },
                    onClick = {
                        onLanguageSelected(language.tag)
                        expanded = false
                    },
                    trailingIcon = {
                        if (selectedTag == language.tag) {
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
private fun DropdownItemText(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}
