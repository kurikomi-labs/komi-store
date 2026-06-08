package zed.rainxch.core.presentation.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import zed.rainxch.core.presentation.theme.tokens.Tokens
import zed.rainxch.core.presentation.theme.tokens.Tokens.Status
import zed.rainxch.core.presentation.theme.tokens.Tokens.Status.IssueState
import zed.rainxch.core.presentation.theme.tokens.Tokens.Status.Severity
import zed.rainxch.core.presentation.theme.tokens.Tokens.Thresholds

val LocalPalette = staticCompositionLocalOf { Tokens.Nord.light }

val LocalStatusColors = staticCompositionLocalOf { defaultStatusColors }

val LocalThresholds = staticCompositionLocalOf { defaultThresholds }

val LocalMotion = staticCompositionLocalOf { defaultMotion }

val LocalSpacing = staticCompositionLocalOf { defaultSpacing }

data class StatusColors(
    val freshnessHot: Color,
    val freshnessFresh: Color,
    val freshnessWarm: Color,
    val freshnessCool: Color,
    val freshnessDormant: Color,
    val waxIntact: Color,
    val waxCracked: Color,
    val waxOpen: Color,
    val permLow: Color,
    val permModerate: Color,
    val permHigh: Color,
    val trendRising: Color,
    val trendFlat: Color,
    val trendFalling: Color,
    val starActive: Color,
    val issueOpen: Color,
    val issueClosed: Color,
    val pullOpen: Color,
    val pullMerged: Color,
    val pullClosed: Color,
    val severityCritical: Color,
    val severityHigh: Color,
    val severityMedium: Color,
    val severityLow: Color,
    val severityUnknown: Color,
)

data class ThresholdSet(
    val freshness: List<Thresholds.FreshnessBucket>,
    val maintenance: List<Thresholds.MaintenanceBucket>,
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
    freshnessHot = Status.Freshness.hot,
    freshnessFresh = Status.Freshness.fresh,
    freshnessWarm = Status.Freshness.warm,
    freshnessCool = Status.Freshness.cool,
    freshnessDormant = Status.Freshness.dormant,
    waxIntact = Status.Wax.intact,
    waxCracked = Status.Wax.cracked,
    waxOpen = Status.Wax.open,
    permLow = Status.Perm.low,
    permModerate = Status.Perm.moderate,
    permHigh = Status.Perm.high,
    trendRising = Status.Trend.rising,
    trendFlat = Status.Trend.flat,
    trendFalling = Status.Trend.falling,
    starActive = Status.Star.activeLight,
    issueOpen = IssueState.openLight,
    issueClosed = IssueState.closedLight,
    pullOpen = IssueState.openLight,
    pullMerged = IssueState.closedLight,
    pullClosed = IssueState.prClosedLight,
    severityCritical = Severity.criticalLight,
    severityHigh = Severity.highLight,
    severityMedium = Severity.mediumLight,
    severityLow = Severity.lowLight,
    severityUnknown = Severity.unknownLight,
)

internal val darkStatusColors = defaultStatusColors.copy(
    starActive = Status.Star.activeDark,
    issueOpen = IssueState.openDark,
    issueClosed = IssueState.closedDark,
    pullOpen = IssueState.openDark,
    pullMerged = IssueState.closedDark,
    pullClosed = IssueState.prClosedDark,
    severityCritical = Severity.criticalDark,
    severityHigh = Severity.highDark,
    severityMedium = Severity.mediumDark,
    severityLow = Severity.lowDark,
    severityUnknown = Severity.unknownDark,
)

internal val defaultThresholds = ThresholdSet(
    freshness = Thresholds.freshness,
    maintenance = Thresholds.maintenance,
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
