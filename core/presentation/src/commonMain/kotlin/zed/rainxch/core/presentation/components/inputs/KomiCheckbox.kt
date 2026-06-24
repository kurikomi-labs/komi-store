package zed.rainxch.core.presentation.components.inputs

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import zed.rainxch.core.presentation.components.icon.KomiIcon
import zed.rainxch.core.presentation.locals.LocalPersonality
import zed.rainxch.core.presentation.personality.ClassicPersonality
import zed.rainxch.core.presentation.personality.MangaPersonality
import zed.rainxch.core.presentation.personality.manga.decoration.inkFocusRing

@Composable
fun KomiCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    when (val personality = LocalPersonality.current) {
        is MangaPersonality -> {
            val colors = personality.colors
            val alpha = if (enabled) 1f else 0.45f
            val shape = RoundedCornerShape(personality.shape.cornerSmall)
            val fill by animateColorAsState(
                if (checked) colors.primary else colors.surface,
                label = "komiCheckboxFill",
            )
            val interaction = remember { MutableInteractionSource() }
            val focused by interaction.collectIsFocusedAsState()
            val hovered by interaction.collectIsHoveredAsState()
            val toggleModifier =
                if (onCheckedChange != null) {
                    Modifier.toggleable(
                        value = checked,
                        interactionSource = interaction,
                        indication = null,
                        enabled = enabled,
                        role = Role.Checkbox,
                        onValueChange = onCheckedChange,
                    )
                } else {
                    Modifier
                }
            Box(
                modifier =
                    modifier
                        .then(toggleModifier)
                        .size(22.dp)
                        .inkFocusRing(focused = { focused || hovered }, color = colors.primary)
                        .background(color = fill.copy(alpha = fill.alpha * alpha), shape = shape)
                        .border(width = 2.5.dp, color = colors.outline.copy(alpha = alpha), shape = shape),
                contentAlignment = Alignment.Center,
            ) {
                if (checked) {
                    KomiIcon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = colors.onPrimary.copy(alpha = alpha),
                    )
                }
            }
        }

        is ClassicPersonality -> {
            val colors = personality.colors
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = modifier,
                enabled = enabled,
                colors =
                    CheckboxDefaults.colors(
                        checkedColor = colors.primary,
                        uncheckedColor = colors.outline,
                        checkmarkColor = colors.onPrimary,
                    ),
            )
        }
    }
}
