package zed.rainxch.core.presentation.vocabulary

import androidx.compose.ui.graphics.Color
import zed.rainxch.core.presentation.theme.tokens.Tokens

enum class FreshnessState { HOT, FRESH, WARM, COOL, DORMANT }

data class Freshness(val state: FreshnessState, val color: Color, val ringFraction: Float)

fun freshnessOf(daysSinceRelease: Int): Freshness {
    val b = Tokens.Thresholds.freshness.first {
        it.maxDaysInclusive == null || daysSinceRelease <= it.maxDaysInclusive
    }
    val state = when (b.state) {
        "hot" -> FreshnessState.HOT
        "fresh" -> FreshnessState.FRESH
        "warm" -> FreshnessState.WARM
        "cool" -> FreshnessState.COOL
        else -> FreshnessState.DORMANT
    }
    return Freshness(state, b.color, b.ringFraction)
}
