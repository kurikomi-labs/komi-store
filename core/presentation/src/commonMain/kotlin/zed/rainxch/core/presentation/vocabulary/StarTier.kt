package zed.rainxch.core.presentation.vocabulary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StarTier(
    stars: Int,
    modifier: Modifier = Modifier,
    size: Int = 11,
    activeColor: Color = Color(0xFFC49652),
    inactiveColor: Color = MaterialTheme.colorScheme.outline,
) {
    val tier = starTierOf(stars)
    val style = TextStyle(fontSize = size.sp)
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (i in 1..5) {
            Text(
                text = "★",
                color = if (i <= tier) activeColor else inactiveColor,
                style = style,
            )
        }
    }
}

fun starTierOf(stars: Int): Int = when {
    stars >= 100_000 -> 5
    stars >= 50_000 -> 4
    stars >= 10_000 -> 3
    stars >= 1_000 -> 2
    else -> 1
}
