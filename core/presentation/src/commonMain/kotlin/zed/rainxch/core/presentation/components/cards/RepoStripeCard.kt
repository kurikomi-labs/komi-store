package zed.rainxch.core.presentation.components.cards

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.lerp
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import zed.rainxch.core.presentation.theme.shapes.CornerRadii
import zed.rainxch.core.presentation.theme.shapes.WonkySquircleShape

private val RepoStripeShape = WonkySquircleShape(
    topStart = CornerRadii(28.dp, 22.dp),
    topEnd = CornerRadii(22.dp, 28.dp),
    bottomEnd = CornerRadii(28.dp, 22.dp),
    bottomStart = CornerRadii(22.dp, 28.dp),
)

private fun Color.normalizedForStripe(isDark: Boolean): Color {
    val r = (red * 255f).toInt().coerceIn(0, 255)
    val g = (green * 255f).toInt().coerceIn(0, 255)
    val b = (blue * 255f).toInt().coerceIn(0, 255)
    val lum = (r + g + b) / 3
    val target = if (isDark) 170 else 140
    val minLum = if (isDark) 90 else 70
    if (lum >= minLum) return this
    val factor = target.toFloat() / lum.coerceAtLeast(1).toFloat()
    return Color(
        red = (r * factor).toInt().coerceIn(0, 255),
        green = (g * factor).toInt().coerceIn(0, 255),
        blue = (b * factor).toInt().coerceIn(0, 255),
        alpha = 255,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RepoStripeCard(
    accent: Color,
    ownerLogin: String,
    name: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    stripeTrailing: (@Composable () -> Unit)? = null,
    avatar: @Composable () -> Unit,
    chips: @Composable () -> Unit,
    languagePill: (@Composable () -> Unit)? = null,
    cta: @Composable () -> Unit,
) {
    val borderColor = MaterialTheme.colorScheme.outline
    val isDark = isSystemInDarkTheme()
    val surface = MaterialTheme.colorScheme.surface
    val normalizedTarget = remember(accent, isDark) { accent.normalizedForStripe(isDark) }
    val tintTargetFraction = if (isDark) 0.10f else 0.06f
    val animatedAccent by animateColorAsState(
        targetValue = normalizedTarget,
        animationSpec = tween(durationMillis = 1800, easing = LinearOutSlowInEasing),
        label = "repo-stripe-accent",
    )
    val animatedSurface by animateColorAsState(
        targetValue = lerp(surface, normalizedTarget, tintTargetFraction),
        animationSpec = tween(durationMillis = 1800, easing = LinearOutSlowInEasing),
        label = "repo-stripe-surface",
    )
    val stripeBase = if (isDark) animatedAccent.copy(alpha = 0.18f) else animatedAccent.copy(alpha = 0.12f)
    val stripeLineThick = if (isDark) animatedAccent.copy(alpha = 0.45f) else animatedAccent.copy(alpha = 0.55f)
    val stripeLineThin = if (isDark) animatedAccent.copy(alpha = 0.22f) else animatedAccent.copy(alpha = 0.30f)
    val avatarBg = if (isDark) animatedAccent.copy(alpha = 0.18f) else animatedAccent.copy(alpha = 0.14f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RepoStripeShape)
            .background(animatedSurface)
            .border(width = 1.5.dp, color = borderColor, shape = RepoStripeShape)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clipToBounds()
                    .drawBehind {
                        drawRect(color = stripeBase)
                        val thick = 9.dp.toPx()
                        val thin = 2.5.dp.toPx()
                        val gapAfterThick = 10.dp.toPx()
                        val gapBetweenThin = 6.dp.toPx()
                        val cycle = thick + gapAfterThick + thin + gapBetweenThin + thin + gapAfterThick
                        var x = -size.height
                        while (x < size.width + size.height) {
                            val baseY = size.height
                            val endX = x + size.height
                            drawLine(
                                color = stripeLineThick,
                                start = Offset(x, baseY),
                                end = Offset(endX, 0f),
                                strokeWidth = thick,
                                cap = StrokeCap.Round,
                            )
                            var xt = x + thick + gapAfterThick
                            drawLine(
                                color = stripeLineThin,
                                start = Offset(xt, baseY),
                                end = Offset(xt + size.height, 0f),
                                strokeWidth = thin,
                                cap = StrokeCap.Round,
                            )
                            xt += thin + gapBetweenThin
                            drawLine(
                                color = stripeLineThin,
                                start = Offset(xt, baseY),
                                end = Offset(xt + size.height, 0f),
                                strokeWidth = thin,
                                cap = StrokeCap.Round,
                            )
                            x += cycle
                        }
                    },
            ) {
                if (stripeTrailing != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 14.dp),
                    ) {
                        stripeTrailing()
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 96.dp, end = 14.dp, top = 12.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = ownerLogin,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 12.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = name,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 24.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        thickness = 1.dp,
                    )
                    Spacer(Modifier.height(10.dp))
                    chips()
                    if (languagePill != null) {
                        Spacer(Modifier.height(6.dp))
                        languagePill()
                    }
                }
                cta()
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 14.dp, y = 26.dp)
                .size(72.dp)
                .clip(CircleShape)
                .background(avatarBg),
            contentAlignment = Alignment.Center,
        ) {
            avatar()
        }
    }
}
