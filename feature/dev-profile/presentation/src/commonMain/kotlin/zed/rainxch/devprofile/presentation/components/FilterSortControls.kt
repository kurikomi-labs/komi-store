package zed.rainxch.devprofile.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.core.presentation.components.inputs.GhsTextField
import zed.rainxch.core.presentation.components.overlays.GhsDropdownMenu
import zed.rainxch.core.presentation.components.overlays.GhsDropdownMenuItem
import zed.rainxch.core.presentation.theme.tokens.Radii
import zed.rainxch.devprofile.domain.model.RepoFilterType
import zed.rainxch.devprofile.domain.model.RepoSortType
import zed.rainxch.devprofile.presentation.DeveloperProfileAction
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.clear_search
import zed.rainxch.githubstore.core.presentation.res.filter_favorites
import zed.rainxch.githubstore.core.presentation.res.filter_installed
import zed.rainxch.githubstore.core.presentation.res.filter_with_installable
import zed.rainxch.githubstore.core.presentation.res.filter_with_releases
import zed.rainxch.githubstore.core.presentation.res.repositories
import zed.rainxch.githubstore.core.presentation.res.repository_singular
import zed.rainxch.githubstore.core.presentation.res.search_repositories
import zed.rainxch.githubstore.core.presentation.res.showing_x_of_y_repositories
import zed.rainxch.githubstore.core.presentation.res.sort
import zed.rainxch.githubstore.core.presentation.res.sort_most_stars
import zed.rainxch.githubstore.core.presentation.res.sort_name
import zed.rainxch.githubstore.core.presentation.res.sort_recently_updated

@Composable
fun FilterSortControls(
    currentFilter: RepoFilterType,
    currentSort: RepoSortType,
    searchQuery: String,
    repoCount: Int,
    totalCount: Int,
    onAction: (DeveloperProfileAction) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        GhsTextField(
            value = searchQuery,
            onValueChange = { onAction(DeveloperProfileAction.OnSearchQueryChange(it)) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = stringResource(Res.string.search_repositories),
            leadingIcon = Icons.Default.Search,
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { onAction(DeveloperProfileAction.OnSearchQueryChange("")) }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(Res.string.clear_search),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            },
            singleLine = true,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RepoFilterType.entries.forEach { filter ->
                    FilterPill(
                        label = filter.displayName(),
                        isSelected = currentFilter == filter,
                        onClick = { onAction(DeveloperProfileAction.OnFilterChange(filter)) },
                    )
                }
            }
            SortButton(
                currentSort = currentSort,
                onSortChange = { onAction(DeveloperProfileAction.OnSortChange(it)) },
            )
        }

        Text(
            text = if (repoCount == totalCount) {
                "$repoCount ${stringResource(
                    if (repoCount == 1) Res.string.repository_singular else Res.string.repositories,
                )}"
            } else {
                stringResource(Res.string.showing_x_of_y_repositories, repoCount, totalCount)
            },
            maxLines = 1,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FilterPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val container by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = tween(durationMillis = 180),
        label = "filter_container",
    )
    val content by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        animationSpec = tween(durationMillis = 180),
        label = "filter_content",
    )
    Box(
        modifier = Modifier
            .clip(Radii.chip)
            .background(container)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
            ),
            color = content,
            maxLines = 1,
        )
    }
}

@Composable
private fun SortButton(
    currentSort: RepoSortType,
    onSortChange: (RepoSortType) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(Radii.chip)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .clickable { expanded = true },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Sort,
                contentDescription = stringResource(Res.string.sort),
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        GhsDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            RepoSortType.entries.forEach { sort ->
                GhsDropdownMenuItem(
                    text = sort.displayName(),
                    onClick = {
                        onSortChange(sort)
                        expanded = false
                    },
                    trailingIcon = if (currentSort == sort) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    } else {
                        null
                    },
                )
            }
        }
    }
}

@Composable
private fun RepoFilterType.displayName(): String = when (this) {
    RepoFilterType.WITH_RELEASES -> stringResource(Res.string.filter_with_releases)
    RepoFilterType.WITH_INSTALLABLE -> stringResource(Res.string.filter_with_installable)
    RepoFilterType.INSTALLED -> stringResource(Res.string.filter_installed)
    RepoFilterType.FAVORITES -> stringResource(Res.string.filter_favorites)
}

@Composable
private fun RepoSortType.displayName(): String = when (this) {
    RepoSortType.UPDATED -> stringResource(Res.string.sort_recently_updated)
    RepoSortType.STARS -> stringResource(Res.string.sort_most_stars)
    RepoSortType.NAME -> stringResource(Res.string.sort_name)
}
