package zed.rainxch.core.presentation.personality.classic

import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import zed.rainxch.core.presentation.personality.model.PersonalityShadow

internal val ClassicShadow =
    PersonalityShadow(
        card = DpOffset(0.dp, 1.dp),
        button = DpOffset(0.dp, 1.dp),
        modal = DpOffset(0.dp, 6.dp),
        blur = 10.dp,
        pressTranslate = 0.dp,
    )
