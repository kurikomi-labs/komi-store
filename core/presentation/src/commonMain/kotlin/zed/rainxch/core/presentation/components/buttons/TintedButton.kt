package zed.rainxch.core.presentation.components.buttons

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import zed.rainxch.core.presentation.theme.tokens.Radii

/**
 * Tinted secondary CTA — `primaryContainer` background, `primary` text. Asymmetric
 * (non-wonky) squircle. Used for Get / Read more / secondary install-panel actions
 * (DESIGN.md §7.1).
 */
@Composable
fun TintedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    backgroundColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.primary,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = modifier
            .clip(Radii.cardSm)
            .background(if (enabled) backgroundColor else backgroundColor.copy(alpha = 0.38f))
            .clickable(enabled = enabled, onClick = onClick)
            .heightIn(min = 44.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            ProvideTextStyle(
                value = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            ) {
                content()
            }
        }
    }
}
