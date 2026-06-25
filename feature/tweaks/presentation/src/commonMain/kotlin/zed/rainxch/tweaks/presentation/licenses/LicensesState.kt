package zed.rainxch.tweaks.presentation.licenses

import kotlinx.collections.immutable.ImmutableList

data class LicensesState(
    val libraries: ImmutableList<LibraryEntry>? = null,
    val isLoading: Boolean = true,
    val loadError: Boolean = false,
)
