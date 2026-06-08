package zed.rainxch.core.domain.helpers

interface BrowserHelper {
    fun openUrl(
        url: String,
        onFailure: (error: String) -> Unit = { },
    )
}
