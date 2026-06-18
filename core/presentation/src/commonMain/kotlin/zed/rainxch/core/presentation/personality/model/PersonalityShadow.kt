package zed.rainxch.core.presentation.personality.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset

@Immutable
data class PersonalityShadow(
    val card: DpOffset,
    val button: DpOffset,
    val modal: DpOffset,
    val blur: Dp,
    val pressTranslate: Dp,
)
