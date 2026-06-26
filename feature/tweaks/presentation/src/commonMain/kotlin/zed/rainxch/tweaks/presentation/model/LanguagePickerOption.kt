package zed.rainxch.tweaks.presentation.model

import androidx.compose.runtime.Immutable

@Immutable
data class LanguagePickerOption(
    val id: String,
    val title: String,
    val tag: String?,
)
