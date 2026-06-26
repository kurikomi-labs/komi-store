package zed.rainxch.core.presentation.components.inputs

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import zed.rainxch.core.presentation.locals.LocalPersonality
import zed.rainxch.core.presentation.personality.ClassicPersonality
import zed.rainxch.core.presentation.personality.MangaPersonality
import zed.rainxch.core.presentation.personality.manga.decoration.inkFocusRing

@Composable
fun KomiRadioButton(
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    when (val personality = LocalPersonality.current) {
        is MangaPersonality -> {
            val colors = personality.colors
            val alpha = if (enabled) 1f else 0.45f
            val shape = RoundedCornerShape(personality.shape.cornerSmall)
            val innerScale by animateFloatAsState(if (selected) 1f else 0f, label = "komiRadioInner")
            val interaction = remember { MutableInteractionSource() }
            val focused by interaction.collectIsFocusedAsState()
            val hovered by interaction.collectIsHoveredAsState()
            val selectModifier =
                if (onClick != null) {
                    Modifier.selectable(
                        selected = selected,
                        interactionSource = interaction,
                        indication = null,
                        enabled = enabled,
                        role = Role.RadioButton,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                }
            Box(
                modifier =
                    modifier
                        .then(selectModifier)
                        .size(22.dp)
                        .inkFocusRing(focused = { focused || hovered }, color = colors.primary)
                        .background(color = colors.surface.copy(alpha = alpha), shape = shape)
                        .border(width = 2.5.dp, color = colors.outline.copy(alpha = alpha), shape = shape),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier =
                        Modifier
                            .scale(innerScale)
                            .size(11.dp)
                            .background(
                                color = colors.primary.copy(alpha = alpha),
                                shape = RoundedCornerShape(personality.shape.cornerSmall),
                            ),
                )
            }
        }

        is ClassicPersonality -> {
            val colors = personality.colors
            RadioButton(
                selected = selected,
                onClick = onClick,
                modifier = modifier,
                enabled = enabled,
                colors =
                    RadioButtonDefaults.colors(
                        selectedColor = colors.primary,
                        unselectedColor = colors.outline,
                    ),
            )
        }
    }
}
