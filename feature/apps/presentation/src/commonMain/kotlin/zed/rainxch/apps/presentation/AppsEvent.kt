package zed.rainxch.apps.presentation

import zed.rainxch.apps.domain.model.ImportResult

sealed interface AppsEvent {
    data class ShowError(
        val message: String,
    ) : AppsEvent

    data class ShowSuccess(
        val message: String,
    ) : AppsEvent

    data class NavigateToRepo(
        val repoId: Long,
        val sourceHost: String? = null,
        // Forgejo / Codeberg repos use a synthetic 64-bit repoId
        // (RepoIdCodec) that the backend / GitHub APIs don't recognise.
        // When sourceHost is set we have to look the repo up by
        // owner+name instead. Carry them through nav so the VM doesn't
        // fall back to GitHub `/repositories/{id}` and 404.
        val owner: String? = null,
        val repo: String? = null,
    ) : AppsEvent

    data class AppLinkedSuccessfully(
        val appName: String,
    ) : AppsEvent

    data class ImportComplete(
        val result: ImportResult,
    ) : AppsEvent

    data object NavigateToExternalImport : AppsEvent
}
