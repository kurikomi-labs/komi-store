package zed.rainxch.core.presentation.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

private val EXPRESSIVE_CARD_SHAPE = RoundedCornerShape(32.dp)

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
    when {
        onClick != null && onLongClick != null -> {

            ElevatedCard(
                modifier =
                    modifier
                        .fillMaxWidth()
                        .clip(EXPRESSIVE_CARD_SHAPE)
                        .combinedClickable(
                            onClick = onClick,
                            onLongClick = onLongClick,
                        ),
                colors =
                    CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                shape = EXPRESSIVE_CARD_SHAPE,
                content = { content() },
            )
        }

        onClick != null -> {
            ElevatedCard(
                modifier = modifier.fillMaxWidth(),
                colors =
                    CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                onClick = onClick,
                shape = EXPRESSIVE_CARD_SHAPE,
                content = { content() },
            )
        }

        else -> {
            ElevatedCard(
                modifier = modifier.fillMaxWidth(),
                colors =
                    CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                shape = EXPRESSIVE_CARD_SHAPE,
                content = { content() },
            )
        }
    }
}
