package zed.rainxch.core.presentation.personality.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily

@Immutable
data class PersonalityType(
    val display: TextStyle,
    val title: TextStyle,
    val stamp: TextStyle,
    val body: TextStyle,
    val label: TextStyle,
    val mono: TextStyle,
    val uppercaseHeadings: Boolean,
) {
    fun withFamilies(
        display: FontFamily,
        body: FontFamily,
        mono: FontFamily,
    ): PersonalityType =
        copy(
            display = this.display.copy(fontFamily = display),
            title = this.title.copy(fontFamily = display),
            stamp = this.stamp.copy(fontFamily = display),
            body = this.body.copy(fontFamily = body),
            label = this.label.copy(fontFamily = body),
            mono = this.mono.copy(fontFamily = mono),
        )
}
