package zed.rainxch.tweaks.presentation.licenses

import kotlinx.serialization.Serializable

@Serializable
data class LibraryEntry(
    val name: String,
    val license: String,
    val url: String,
)
