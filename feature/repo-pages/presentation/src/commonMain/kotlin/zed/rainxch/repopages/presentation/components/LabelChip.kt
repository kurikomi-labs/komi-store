package zed.rainxch.repopages.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import zed.rainxch.repopages.domain.model.IssueLabel
import zed.rainxch.repopages.presentation.utils.parseLabelColor

@Composable
internal fun LabelChip(label: IssueLabel) {
    val color = parseLabelColor(label.color)
    Box(
        modifier = Modifier
            .background(color = color.copy(alpha = 0.18f), shape = RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(
            text = label.name,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
