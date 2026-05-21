package zed.rainxch.core.presentation.theme.tokens

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

object Radii {
    val chip = shape(11, 8)
    val row = shape(13, 10)
    val cardSm = shape(15, 11)
    val card = shape(18, 14)
    val cardLg = shape(20, 15)
    val hero = shape(24, 18)
    val heroLg = shape(28, 22)

    fun shape(primary: Int, secondary: Int) = RoundedCornerShape(
        topStart = primary.dp,
        topEnd = secondary.dp,
        bottomEnd = primary.dp,
        bottomStart = secondary.dp,
    )
}
