package zed.rainxch.tweaks.presentation.components.sections

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.core.domain.model.settings.AppLanguages
import zed.rainxch.core.presentation.locals.LocalPersonality
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.language_follow_system
import zed.rainxch.githubstore.core.presentation.res.tweaks_language_intro_body
import zed.rainxch.tweaks.presentation.TweaksAction
import zed.rainxch.tweaks.presentation.TweaksState
import zed.rainxch.tweaks.presentation.components.TweaksSearchField

@Composable
fun languageSectionContent(
    state: TweaksState,
    onAction: (TweaksAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }

    val languages = AppLanguages.ALL
    val filtered by remember(query) {
        derivedStateOf {
            if (query.isBlank()) languages
            else languages.filter {
                it.displayName.contains(query, ignoreCase = true) ||
                    it.tag.contains(query, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(Res.string.tweaks_language_intro_body),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
        )
        Spacer(Modifier.height(8.dp))

        TweaksSearchField(
            query = query,
            onQueryChange = { query = it },
            onClear = { query = "" },
        )
        Spacer(Modifier.height(12.dp))

        if (query.isBlank()) {
            LanguageRow(
                title = stringResource(Res.string.language_follow_system),
                subtitleTag = null,
                leadingIcon = true,
                selected = state.selectedAppLanguage == null,
                onClick = { onAction(TweaksAction.OnAppLanguageSelected(null)) },
            )
            Spacer(Modifier.height(8.dp))
        }

        filtered.forEach { language ->
            LanguageRow(
                title = language.displayName,
                subtitleTag = language.tag,
                leadingIcon = false,
                selected = state.selectedAppLanguage == language.tag,
                onClick = {
                    onAction(TweaksAction.OnAppLanguageSelected(language.tag))
                },
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun LanguageRow(
    title: String,
    subtitleTag: String?,
    leadingIcon: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(LocalPersonality.current.shape.corner))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(LocalPersonality.current.shape.corner),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (leadingIcon) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(LocalPersonality.current.shape.cornerSmall))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PhoneAndroid,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!subtitleTag.isNullOrBlank()) {
                    Text(
                        text = subtitleTag,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}
