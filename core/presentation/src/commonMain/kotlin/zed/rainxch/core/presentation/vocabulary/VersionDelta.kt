package zed.rainxch.core.presentation.vocabulary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import zed.rainxch.core.presentation.theme.LocalStatusColors

enum class VersionDeltaKind { PATCH, MINOR, MAJOR }

@Composable
fun VersionDelta(
    delta: VersionDeltaKind,
    modifier: Modifier = Modifier,
) {
    val status = LocalStatusColors.current
    when (delta) {
        VersionDeltaKind.PATCH -> Box(
            modifier = modifier
                .size(6.dp)
                .background(status.freshnessFresh, CircleShape),
        )
        VersionDeltaKind.MINOR -> Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(2.5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(2) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(status.freshnessWarm, CircleShape),
                )
            }
        }
        VersionDeltaKind.MAJOR -> Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(width = 9.dp, height = 9.dp)
                    .background(status.freshnessHot, RoundedCornerShape(1.5.dp)),
            )
            Box(
                modifier = Modifier
                    .size(width = 2.dp, height = 9.dp)
                    .background(status.freshnessHot),
            )
        }
    }
}
