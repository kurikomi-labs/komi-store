package zed.rainxch.core.presentation.components.buttons

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import zed.rainxch.core.presentation.theme.shapes.WonkySquircleShape

enum class GhsButtonVariant {
    Primary,
    Tonal,
    Outline,
    Text,
    Destructive,
}

enum class GhsButtonSize {
    Sm,
    Md,
    Lg,
}

@Composable
fun GhsButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: GhsButtonVariant = GhsButtonVariant.Primary,
    size: GhsButtonSize = GhsButtonSize.Md,
    enabled: Boolean = true,
    loading: Boolean = false,
    containerColorOverride: Color? = null,
    contentColorOverride: Color? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val isDark = cs.background.luminance() < 0.5f
    val container: Color = containerColorOverride ?: when (variant) {
        GhsButtonVariant.Primary -> if (isDark) cs.primaryContainer else cs.primary
        GhsButtonVariant.Tonal -> cs.secondaryContainer
        GhsButtonVariant.Outline -> Color.Transparent
        GhsButtonVariant.Text -> Color.Transparent
        GhsButtonVariant.Destructive -> cs.errorContainer
    }
    val contentColor: Color = contentColorOverride ?: when (variant) {
        GhsButtonVariant.Primary -> if (isDark) cs.onPrimaryContainer else cs.onPrimary
        GhsButtonVariant.Tonal -> cs.onSecondaryContainer
        GhsButtonVariant.Outline -> cs.onSurface
        GhsButtonVariant.Text -> cs.primary
        GhsButtonVariant.Destructive -> cs.onErrorContainer
    }
    val borderColor: Color? = when (variant) {
        GhsButtonVariant.Outline -> cs.outline
        else -> null
    }
    val shape: Shape = when (variant) {
        GhsButtonVariant.Primary -> WonkySquircleShape.CtaPrimary
        GhsButtonVariant.Destructive -> WonkySquircleShape.CtaPrimary
        GhsButtonVariant.Tonal -> RoundedCornerShape(50)
        GhsButtonVariant.Outline -> RoundedCornerShape(50)
        GhsButtonVariant.Text -> RoundedCornerShape(50)
    }
    val minHeight: Dp = when (size) {
        GhsButtonSize.Sm -> 36.dp
        GhsButtonSize.Md -> 44.dp
        GhsButtonSize.Lg -> 52.dp
    }
    val hPadding: Dp = when (size) {
        GhsButtonSize.Sm -> 12.dp
        GhsButtonSize.Md -> 18.dp
        GhsButtonSize.Lg -> 22.dp
    }
    val vPadding: Dp = when (size) {
        GhsButtonSize.Sm -> 6.dp
        GhsButtonSize.Md -> 10.dp
        GhsButtonSize.Lg -> 14.dp
    }
    val fontSize = when (size) {
        GhsButtonSize.Sm -> 13.sp
        GhsButtonSize.Md -> 14.sp
        GhsButtonSize.Lg -> 16.sp
    }

    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled && !loading) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh,
        ),
        label = "ghs-button-press",
    )
    val alpha = if (enabled && !loading) 1f else 0.38f

    var rowModifier = modifier
        .scale(scale)
        .clip(shape)
        .background(container.copy(alpha = container.alpha * alpha))
    if (borderColor != null) {
        rowModifier = rowModifier.border(
            width = 1.dp,
            color = borderColor.copy(alpha = alpha),
            shape = shape,
        )
    }
    rowModifier = rowModifier
        .clickable(
            enabled = enabled && !loading,
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick,
        )
        .heightIn(min = minHeight)
        .padding(horizontal = hPadding, vertical = vPadding)

    Row(
        modifier = rowModifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor.copy(alpha = alpha)) {
            ProvideTextStyle(
                value = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = fontSize,
                ),
            ) {
                content()
            }
        }
    }
}

@Composable
fun GhsButton(
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    variant: GhsButtonVariant = GhsButtonVariant.Primary,
    size: GhsButtonSize = GhsButtonSize.Md,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    containerColorOverride: Color? = null,
    contentColorOverride: Color? = null,
) {
    val iconSize = when (size) {
        GhsButtonSize.Sm -> 16.dp
        GhsButtonSize.Md -> 18.dp
        GhsButtonSize.Lg -> 20.dp
    }
    GhsButton(
        onClick = onClick,
        modifier = modifier,
        variant = variant,
        size = size,
        enabled = enabled,
        loading = loading,
        containerColorOverride = containerColorOverride,
        contentColorOverride = contentColorOverride,
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(iconSize),
                strokeWidth = 2.dp,
                color = LocalContentColor.current,
            )
        } else if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                modifier = Modifier.size(iconSize),
            )
        }
        Text(
            text = label,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
        if (trailingIcon != null && !loading) {
            Icon(
                imageVector = trailingIcon,
                contentDescription = null,
                modifier = Modifier.size(iconSize),
            )
        }
    }
}
