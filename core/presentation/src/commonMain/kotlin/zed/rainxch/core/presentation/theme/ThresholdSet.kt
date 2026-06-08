package zed.rainxch.core.presentation.theme

import zed.rainxch.core.presentation.theme.tokens.Tokens.Thresholds

data class ThresholdSet(
    val freshness: List<Thresholds.FreshnessBucket>,
    val maintenance: List<Thresholds.MaintenanceBucket>,
)
