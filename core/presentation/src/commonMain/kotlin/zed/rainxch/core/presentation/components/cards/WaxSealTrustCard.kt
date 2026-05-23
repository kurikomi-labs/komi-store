package zed.rainxch.core.presentation.components.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import zed.rainxch.core.presentation.theme.jetbrainsMono
import zed.rainxch.core.presentation.theme.tokens.Radii
import zed.rainxch.core.presentation.vocabulary.WaxSeal
import zed.rainxch.core.presentation.vocabulary.WaxSealState

@Composable
fun WaxSealTrustCard(
    state: WaxSealState,
    fingerprintDetail: String,
    modifier: Modifier = Modifier,
    stateLabel: String = defaultLabel(state),
) {
    val cs = MaterialTheme.colorScheme
    val (bg, border) = when (state) {
        WaxSealState.INTACT -> cs.tertiaryContainer to cs.tertiary
        WaxSealState.CRACKED -> cs.errorContainer to cs.error
        WaxSealState.OPEN -> cs.surface to cs.outline
    }
    Row(
        modifier = modifier
            .clip(Radii.card)
            .background(bg)
            .border(width = 1.dp, color = border, shape = Radii.card)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WaxSeal(state = state, sizeDp = 38)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stateLabel,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp,
                ),
                color = cs.onSurface,
            )
            Text(
                text = fingerprintDetail,
                fontFamily = jetbrainsMono,
                color = cs.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            )
        }
    }
}

private fun defaultLabel(state: WaxSealState): String = when (state) {
    WaxSealState.INTACT -> "Sealed"
    WaxSealState.CRACKED -> "Broken seal"
    WaxSealState.OPEN -> "Unsigned"
}
