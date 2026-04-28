package zed.rainxch.apps.presentation.import.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.apps.presentation.components.InstalledAppIcon
import zed.rainxch.apps.presentation.import.model.CandidateUi
import zed.rainxch.apps.presentation.import.model.RepoSuggestionUi
import zed.rainxch.apps.presentation.import.util.LocalReducedMotion
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.external_import_card_action_less
import zed.rainxch.githubstore.core.presentation.res.external_import_card_action_link
import zed.rainxch.githubstore.core.presentation.res.external_import_card_action_more
import zed.rainxch.githubstore.core.presentation.res.external_import_card_action_skip
import zed.rainxch.githubstore.core.presentation.res.external_import_card_collapse_label
import zed.rainxch.githubstore.core.presentation.res.external_import_card_expand_label
import zed.rainxch.githubstore.core.presentation.res.external_import_card_installer_chip
import zed.rainxch.githubstore.core.presentation.res.external_import_card_preselect_known
import zed.rainxch.githubstore.core.presentation.res.external_import_card_preselect_unknown

@Composable
fun CandidateCard(
    candidate: CandidateUi,
    expanded: Boolean,
    searchQuery: String,
    searchResults: ImmutableList<RepoSuggestionUi>,
    isSearching: Boolean,
    searchError: String?,
    onToggleExpanded: () -> Unit,
    onPick: (RepoSuggestionUi) -> Unit,
    onSkip: () -> Unit,
    onLink: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSearchSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val expandLabel = stringResource(Res.string.external_import_card_expand_label)
    val collapseLabel = stringResource(Res.string.external_import_card_collapse_label)
    val reducedMotion = LocalReducedMotion.current

    Surface(
        tonalElevation = 1.dp,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(
                    onClickLabel = if (expanded) collapseLabel else expandLabel,
                    role = Role.Button,
                ) { onToggleExpanded() },
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            CandidateHeader(candidate = candidate)

            PreselectedRow(suggestion = candidate.preselectedSuggestion)

            // Collapsed footer: primary Link CTA (or hint) + expand affordance.
            // The whole card is clickable to expand, but a dedicated control
            // gives the disclosure a clear screen-reader role and a tap target
            // that doesn't fight the underlying CTA buttons in expanded mode.
            if (!expanded) {
                CollapsedActions(
                    canLink = candidate.preselectedSuggestion != null,
                    onLink = onLink,
                    onExpand = onToggleExpanded,
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter =
                    if (reducedMotion) fadeIn() else fadeIn() + expandVertically(),
                exit =
                    if (reducedMotion) fadeOut() else fadeOut() + shrinkVertically(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    if (candidate.suggestions.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            candidate.suggestions.take(3).forEach { suggestion ->
                                RepoCandidateRow(
                                    suggestion = suggestion,
                                    onPick = onPick,
                                )
                            }
                        }
                    }

                    RepoSearchOverride(
                        query = searchQuery,
                        results = searchResults,
                        isSearching = isSearching,
                        searchError = searchError,
                        onQueryChange = onSearchQueryChange,
                        onSubmit = onSearchSubmit,
                        onPick = onPick,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedButton(
                            onClick = onSkip,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(stringResource(Res.string.external_import_card_action_skip))
                        }
                        TextButton(onClick = onToggleExpanded) {
                            Text(stringResource(Res.string.external_import_card_action_less))
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = null,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CollapsedActions(
    canLink: Boolean,
    onLink: () -> Unit,
    onExpand: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (canLink) {
            Button(
                onClick = onLink,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(Res.string.external_import_card_action_link))
            }
        }
        TextButton(
            onClick = onExpand,
            modifier = if (canLink) Modifier else Modifier.weight(1f),
        ) {
            Text(stringResource(Res.string.external_import_card_action_more))
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
            )
        }
    }
}

@Composable
private fun CandidateHeader(candidate: CandidateUi) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        InstalledAppIcon(
            packageName = candidate.packageName,
            appName = candidate.appLabel,
            modifier =
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp)),
        )

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = candidate.appLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = candidate.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            InstallerChip(installerLabel = candidate.installerLabel)
        }
    }
}

@Composable
private fun InstallerChip(installerLabel: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(8.dp),
    ) {
        Text(
            text = stringResource(Res.string.external_import_card_installer_chip, installerLabel),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun PreselectedRow(suggestion: RepoSuggestionUi?) {
    val containerColor =
        if (suggestion != null) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        }
    val contentColor =
        if (suggestion != null) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (suggestion == null) {
            Text(
                text = stringResource(Res.string.external_import_card_preselect_unknown),
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            )
        } else {
            val percent = (suggestion.confidence * 100).roundToInt().coerceIn(0, 100)
            Text(
                text =
                    stringResource(
                        Res.string.external_import_card_preselect_known,
                        suggestion.ownerSlashRepo,
                        percent,
                    ),
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            )
        }
    }
}

