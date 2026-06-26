package zed.rainxch.apps.presentation.import.model

import kotlin.math.roundToInt

data class RepoSuggestionUi(
    val owner: String,
    val repo: String,
    val confidence: Double,
    val source: SuggestionSource,
    val stars: Int? = null,
    val description: String? = null,
    val sourceHost: String? = null,
    val confidencePercent: Int = (confidence * 100).roundToInt().coerceIn(0, 100),
    val chipTone: SuggestionChipTone =
        if (source != SuggestionSource.MANUAL && confidence >= CHIP_PRIMARY_MIN) {
            SuggestionChipTone.Primary
        } else {
            SuggestionChipTone.Muted
        },
)

private const val CHIP_PRIMARY_MIN = 0.5
