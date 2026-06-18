package zed.rainxch.core.presentation.personality.manga

import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import zed.rainxch.core.presentation.personality.model.PersonalityShadow

internal val MangaShadow =
    PersonalityShadow(
        card = DpOffset(6.dp, 6.dp),
        button = DpOffset(4.dp, 4.dp),
        modal = DpOffset(14.dp, 14.dp),
        blur = 0.dp,
        pressTranslate = 4.dp,
    )
