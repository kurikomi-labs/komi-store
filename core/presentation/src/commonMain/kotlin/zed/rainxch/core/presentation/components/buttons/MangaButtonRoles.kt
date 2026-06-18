package zed.rainxch.core.presentation.components.buttons

import androidx.compose.ui.graphics.Color
import zed.rainxch.core.presentation.personality.model.PersonalityColors

internal fun mangaButtonContainer(
    variant: KomiButtonVariant,
    colors: PersonalityColors,
): Color =
    when (variant) {
        KomiButtonVariant.Primary -> colors.primary
        KomiButtonVariant.Destructive -> colors.error
        KomiButtonVariant.Tonal -> colors.surfaceVariant
        KomiButtonVariant.Outline, KomiButtonVariant.Text -> Color.Transparent
    }

internal fun mangaButtonContent(
    variant: KomiButtonVariant,
    colors: PersonalityColors,
): Color =
    when (variant) {
        KomiButtonVariant.Primary -> colors.onPrimary
        KomiButtonVariant.Destructive -> colors.onError
        KomiButtonVariant.Tonal, KomiButtonVariant.Outline, KomiButtonVariant.Text -> colors.onSurface
    }
