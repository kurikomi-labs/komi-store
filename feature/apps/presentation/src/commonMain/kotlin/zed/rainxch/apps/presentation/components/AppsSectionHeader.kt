package zed.rainxch.apps.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.core.presentation.vocabulary.Squiggle
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.apps_section_collapse
import zed.rainxch.githubstore.core.presentation.res.apps_section_count_suffix
import zed.rainxch.githubstore.core.presentation.res.apps_section_expand
import zed.rainxch.githubstore.core.presentation.res.apps_section_state_collapsed
import zed.rainxch.githubstore.core.presentation.res.apps_section_state_expanded

@Composable
fun AppsSectionHeader(
    title: String,
    count: Int,
    isExpanded: Boolean,
    collapsible: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val expandLabel = stringResource(Res.string.apps_section_expand)
    val collapseLabel = stringResource(Res.string.apps_section_collapse)
    val expandedStateLabel = stringResource(Res.string.apps_section_state_expanded)
    val collapsedStateLabel = stringResource(Res.string.apps_section_state_collapsed)
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 0f else -90f,
        animationSpec = tween(durationMillis = 180),
        label = "section-chevron",
    )

    val rowSemantic = if (isExpanded) collapseLabel else expandLabel

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 20.dp, bottom = 6.dp)
            .let { base ->
                if (collapsible) {
                    base
                        .clickable(onClick = onToggle)
                        .semantics(mergeDescendants = true) {
                            role = Role.Button
                            heading()
                            contentDescription = "$title, $count, $rowSemantic"
                            stateDescription = if (isExpanded) expandedStateLabel else collapsedStateLabel
                        }
                } else {
                    base.semantics(mergeDescendants = true) {
                        heading()
                        contentDescription = "$title, $count"
                    }
                }
            },
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 22.sp,
                ),
                color = MaterialTheme.colorScheme.onBackground,
            )

            Text(
                text = stringResource(Res.string.apps_section_count_suffix, count),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )

            if (collapsible) {
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(22.dp)
                        .rotate(rotation),
                )
            }
        }

        Squiggle()
    }
}
