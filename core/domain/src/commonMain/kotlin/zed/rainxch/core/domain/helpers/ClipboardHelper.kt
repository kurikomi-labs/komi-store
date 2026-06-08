package zed.rainxch.core.domain.helpers

interface ClipboardHelper {
    fun copy(
        label: String,
        text: String,
    )

    fun getText(): String?
}
