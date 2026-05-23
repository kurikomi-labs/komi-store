package zed.rainxch.details.presentation.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import zed.rainxch.core.presentation.theme.shapes.WonkySquircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.apk_inspect_button_label
import zed.rainxch.githubstore.core.presentation.res.apk_inspect_coachmark_body
import zed.rainxch.githubstore.core.presentation.res.apk_inspect_coachmark_dismiss
import zed.rainxch.githubstore.core.presentation.res.apk_inspect_coachmark_title

@Composable
fun InspectApkButton(
    showCoachmark: Boolean,
    onClick: () -> Unit,
    onCoachmarkDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pulse by rememberPulse(active = showCoachmark)
    val tilt by rememberTilt(active = showCoachmark)

    Box(modifier = modifier) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(52.dp)
                .scale(pulse)
                .graphicsLayer { rotationZ = tilt },
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            ),
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = stringResource(Res.string.apk_inspect_button_label),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
        }

        if (showCoachmark) {
            Coachmark(onDismiss = onCoachmarkDismiss)
        }
    }
}

@Composable
private fun rememberPulse(active: Boolean) =
    rememberInfiniteTransition(label = "inspect-pulse")
        .animateFloat(
            initialValue = if (active) 1f else 1f,
            targetValue = if (active) 1.08f else 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 900),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "inspect-pulse-scale",
        )

@Composable
private fun rememberTilt(active: Boolean) =
    rememberInfiniteTransition(label = "inspect-tilt")
        .animateFloat(
            initialValue = if (active) -6f else 0f,
            targetValue = if (active) 6f else 0f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1300),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "inspect-tilt-deg",
        )

@Composable
private fun Coachmark(onDismiss: () -> Unit) {
    Popup(
        alignment = Alignment.TopEnd,

        offset = androidx.compose.ui.unit.IntOffset(x = 0, y = -260),
        properties = PopupProperties(
            focusable = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
        ),
        onDismissRequest = onDismiss,
    ) {
        Column(horizontalAlignment = Alignment.End) {
            Surface(
                shape = WonkySquircleShape.Toast,
                color = MaterialTheme.colorScheme.primary,
                shadowElevation = 6.dp,
                modifier = Modifier.width(260.dp),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = stringResource(Res.string.apk_inspect_coachmark_title),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                            ),
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                    Text(
                        text = stringResource(Res.string.apk_inspect_coachmark_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                    )
                    Row(
                        modifier = Modifier.padding(top = 2.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        // TODO(ghs-button): needs onPrimary text color in coachmark popup
                        TextButton(onClick = onDismiss) {
                            Text(
                                text = stringResource(Res.string.apk_inspect_coachmark_dismiss),
                                color = MaterialTheme.colorScheme.onPrimary,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                ),
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .padding(end = 24.dp)
                    .size(width = 16.dp, height = 8.dp)
                    .arrowDown(MaterialTheme.colorScheme.primary),
            )
        }
    }
}

private fun Modifier.arrowDown(color: androidx.compose.ui.graphics.Color): Modifier =
    this.fillMaxSize().background(
        color = color,
        shape = TriangleDownShape,
    )

private val TriangleDownShape = androidx.compose.foundation.shape.GenericShape { size, _ ->
    moveTo(0f, 0f)
    lineTo(size.width, 0f)
    lineTo(size.width / 2f, size.height)
    close()
}
