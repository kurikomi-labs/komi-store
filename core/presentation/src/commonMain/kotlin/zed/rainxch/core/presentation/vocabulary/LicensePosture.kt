package zed.rainxch.core.presentation.vocabulary

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import zed.rainxch.core.presentation.theme.jetbrainsMono
import zed.rainxch.core.presentation.theme.tokens.Tokens

@Composable
fun LicensePosture(
    spdx: String?,
    modifier: Modifier = Modifier,
    sizeDp: Int = 14,
) {
    val heavy = spdx != null && spdx in Tokens.Licenses.copyleft
    val ink = MaterialTheme.colorScheme.onSurface
    val bg = MaterialTheme.colorScheme.background
    val mono = jetbrainsMono
    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .background(
                color = if (heavy) ink else Color.Transparent,
                shape = RoundedCornerShape(3.dp),
            )
            .border(
                width = 1.4.dp,
                color = ink,
                shape = RoundedCornerShape(3.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (heavy) "©" else "·",
            color = if (heavy) bg else ink,
            fontFamily = mono,
            fontWeight = FontWeight.Bold,
            fontSize = (sizeDp * 0.55f).sp,
        )
    }
}
