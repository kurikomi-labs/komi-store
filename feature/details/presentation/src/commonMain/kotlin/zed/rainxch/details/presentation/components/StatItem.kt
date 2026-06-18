package zed.rainxch.details.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import zed.rainxch.core.presentation.locals.LocalPersonality
import zed.rainxch.core.presentation.utils.formatCount

@Composable
fun StatItem(
    label: String,
    stat: Int,
    modifier: Modifier = Modifier,
) {
    StatItem(label = label, stat = stat.toLong(), modifier = modifier)
}

@Composable
fun StatItem(
    label: String,
    stat: Long,
    modifier: Modifier = Modifier,
) {
    val rowShape = RoundedCornerShape(LocalPersonality.current.shape.corner)
    Column(
        modifier = modifier
            .clip(rowShape)
            .border(width = 1.dp, color = MaterialTheme.colorScheme.outline, shape = rowShape)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            softWrap = false,
        )
        Text(
            text = formatCount(stat),
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Black,
                fontSize = 22.sp,
                letterSpacing = (-0.3).sp,
            ),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
fun TextStatItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    val rowShape = RoundedCornerShape(LocalPersonality.current.shape.corner)
    Column(
        modifier = modifier
            .clip(rowShape)
            .border(width = 1.dp, color = MaterialTheme.colorScheme.outline, shape = rowShape)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            softWrap = false,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                letterSpacing = (-0.3).sp,
            ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            softWrap = false,
        )
    }
}
