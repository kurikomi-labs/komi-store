package zed.rainxch.core.presentation.components.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import zed.rainxch.core.presentation.theme.tokens.Radii

@Composable
fun VitalSignsGrid(
    released: VitalTile,
    maintained: VitalTile,
    stars: VitalTile,
    permissions: VitalTile,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            VitalTileBox(released, Modifier.weight(1f))
            VitalTileBox(maintained, Modifier.weight(1f))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            VitalTileBox(stars, Modifier.weight(1f))
            VitalTileBox(permissions, Modifier.weight(1f))
        }
    }
}

data class VitalTile(
    val label: String,
    val value: String,
    val valueColor: Color? = null,
    val glyph: @Composable () -> Unit = {},
)

@Composable
private fun VitalTileBox(tile: VitalTile, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .clip(Radii.cardSm)
            .background(cs.surfaceContainer)
            .border(width = 1.dp, color = cs.outline, shape = Radii.cardSm)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(modifier = Modifier, contentAlignment = Alignment.Center) {
            tile.glyph()
        }
        Text(
            text = tile.value,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
            ),
            color = tile.valueColor ?: cs.onSurface,
        )
        Text(
            text = tile.label.uppercase(),
            color = cs.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 9.5.sp,
                letterSpacing = 0.04.em(),
            ),
        )
    }
}

private fun Double.em() = androidx.compose.ui.unit.TextUnit(this.toFloat(), androidx.compose.ui.unit.TextUnitType.Em)
