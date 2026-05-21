package zed.rainxch.core.presentation.theme

import androidx.compose.runtime.staticCompositionLocalOf
import zed.rainxch.core.presentation.theme.tokens.Tokens

val LocalPalette = staticCompositionLocalOf { Tokens.Nord.light }

val LocalStatusColors = staticCompositionLocalOf<StatusColors> { defaultStatusColors }

val LocalThresholds = staticCompositionLocalOf<ThresholdSet> { defaultThresholds }

val LocalMotion = staticCompositionLocalOf<MotionTokens> { defaultMotion }

val LocalSpacing = staticCompositionLocalOf<SpacingTokens> { defaultSpacing }

data class StatusColors(
    val freshnessHot: androidx.compose.ui.graphics.Color,
    val freshnessFresh: androidx.compose.ui.graphics.Color,
    val freshnessWarm: androidx.compose.ui.graphics.Color,
    val freshnessCool: androidx.compose.ui.graphics.Color,
    val freshnessDormant: androidx.compose.ui.graphics.Color,
    val waxIntact: androidx.compose.ui.graphics.Color,
    val waxCracked: androidx.compose.ui.graphics.Color,
    val waxOpen: androidx.compose.ui.graphics.Color,
    val permLow: androidx.compose.ui.graphics.Color,
    val permModerate: androidx.compose.ui.graphics.Color,
    val permHigh: androidx.compose.ui.graphics.Color,
    val trendRising: androidx.compose.ui.graphics.Color,
    val trendFlat: androidx.compose.ui.graphics.Color,
    val trendFalling: androidx.compose.ui.graphics.Color,
)

data class ThresholdSet(
    val freshness: List<Tokens.Thresholds.FreshnessBucket>,
    val stars: List<Tokens.Thresholds.StarTier>,
    val maintenance: List<Tokens.Thresholds.MaintenanceBucket>,
)

data class MotionTokens(
    val tapHighlightMs: Int,
    val paletteCrossfadeMs: Int,
    val sheetSlideMs: Int,
    val scrimFadeMs: Int,
    val toastSlideMs: Int,
    val toastFadeMs: Int,
    val heartbeatScaleFrom: Float,
    val heartbeatScaleTo: Float,
    val heartbeatHaloFromScale: Float,
    val heartbeatHaloToScale: Float,
    val heartbeatHaloFromAlpha: Float,
    val heartbeatHaloToAlpha: Float,
)

data class SpacingTokens(
    val xs: Int,
    val sm: Int,
    val md: Int,
    val lg: Int,
    val xl: Int,
    val xxl: Int,
)

internal val defaultStatusColors = StatusColors(
    freshnessHot = Tokens.Status.Freshness.hot,
    freshnessFresh = Tokens.Status.Freshness.fresh,
    freshnessWarm = Tokens.Status.Freshness.warm,
    freshnessCool = Tokens.Status.Freshness.cool,
    freshnessDormant = Tokens.Status.Freshness.dormant,
    waxIntact = Tokens.Status.Wax.intact,
    waxCracked = Tokens.Status.Wax.cracked,
    waxOpen = Tokens.Status.Wax.open,
    permLow = Tokens.Status.Perm.low,
    permModerate = Tokens.Status.Perm.moderate,
    permHigh = Tokens.Status.Perm.high,
    trendRising = Tokens.Status.Trend.rising,
    trendFlat = Tokens.Status.Trend.flat,
    trendFalling = Tokens.Status.Trend.falling,
)

internal val defaultThresholds = ThresholdSet(
    freshness = Tokens.Thresholds.freshness,
    stars = Tokens.Thresholds.stars,
    maintenance = Tokens.Thresholds.maintenance,
)

internal val defaultMotion = MotionTokens(
    tapHighlightMs = Tokens.Motion.tapHighlightMs,
    paletteCrossfadeMs = Tokens.Motion.paletteCrossfadeMs,
    sheetSlideMs = Tokens.Motion.sheetSlideMs,
    scrimFadeMs = Tokens.Motion.scrimFadeMs,
    toastSlideMs = Tokens.Motion.toastSlideMs,
    toastFadeMs = Tokens.Motion.toastFadeMs,
    heartbeatScaleFrom = Tokens.Motion.heartbeatScaleFrom,
    heartbeatScaleTo = Tokens.Motion.heartbeatScaleTo,
    heartbeatHaloFromScale = Tokens.Motion.heartbeatHaloFromScale,
    heartbeatHaloToScale = Tokens.Motion.heartbeatHaloToScale,
    heartbeatHaloFromAlpha = Tokens.Motion.heartbeatHaloFromAlpha,
    heartbeatHaloToAlpha = Tokens.Motion.heartbeatHaloToAlpha,
)

internal val defaultSpacing = SpacingTokens(
    xs = Tokens.Spacing.xs,
    sm = Tokens.Spacing.sm,
    md = Tokens.Spacing.md,
    lg = Tokens.Spacing.lg,
    xl = Tokens.Spacing.xl,
    xxl = Tokens.Spacing.xxl,
)
