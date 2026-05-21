package zed.rainxch.core.presentation.theme.tokens

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * Asymmetric corner-radius scale from tokens.json. Pairs are (primary, secondary)
 * applied diagonally — Compose uses `RoundedCornerShape(topStart, topEnd, bottomEnd, bottomStart)`.
 * The handoff's `radD(primary, secondary)` translates to topStart=primary, topEnd=secondary,
 * bottomEnd=primary, bottomStart=secondary.
 *
 * For "wonky" CTAs / lead cards / search input / sheets / dialogs / toasts, use
 * [zed.rainxch.core.presentation.theme.shapes.WonkySquircleShape] in core/components.
 * Compose's `RoundedCornerShape` cannot express the elliptical (x/y differ) corners
 * required for wonkiness — that lands in P5.
 */
object Radii {
    val chip = shape(11, 8)
    val row = shape(13, 10)
    val cardSm = shape(15, 11)
    val card = shape(18, 14)
    val cardLg = shape(20, 15)
    val hero = shape(24, 18)
    val heroLg = shape(28, 22)

    /** Build an asymmetric squircle with diagonally-paired (primary, secondary) radii. */
    fun shape(primary: Int, secondary: Int) = RoundedCornerShape(
        topStart = primary.dp,
        topEnd = secondary.dp,
        bottomEnd = primary.dp,
        bottomStart = secondary.dp,
    )
}
