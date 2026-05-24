package zed.rainxch.core.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import zed.rainxch.core.presentation.theme.tokens.Radii

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExpressiveCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    check(onLongClick == null || onClick != null) {
        "ExpressiveCard: onLongClick requires onClick"
    }
    val baseModifier = modifier.fillMaxWidth().clip(Radii.row)
    val clickModifier = when {
        onClick != null && onLongClick != null -> baseModifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick,
        )
        onClick != null -> baseModifier.combinedClickable(onClick = onClick)
        else -> baseModifier
    }
    Surface(
        modifier = clickModifier,
        shape = Radii.row,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline,
        ),
        content = content,
    )
}
