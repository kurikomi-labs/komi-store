package zed.rainxch.apps.presentation.import

import zed.rainxch.apps.presentation.import.model.RepoSuggestionUi

sealed interface ExternalImportAction {
    data object OnStart : ExternalImportAction

    data object OnRequestPermission : ExternalImportAction

    data class OnPermissionGranted(val sdkInt: Int?) : ExternalImportAction

    data class OnPermissionDenied(val sdkInt: Int?) : ExternalImportAction

    data class OnSkipCard(val packageName: String) : ExternalImportAction

    data class OnSkipForever(val packageName: String) : ExternalImportAction

    data object OnSkipRemaining : ExternalImportAction

    data class OnPickSuggestion(
        val packageName: String,
        val suggestion: RepoSuggestionUi,
    ) : ExternalImportAction

    data class OnLinkCard(val packageName: String) : ExternalImportAction

    data class OnToggleCardExpanded(val packageName: String) : ExternalImportAction

    data class OnSearchOverrideChanged(
        val packageName: String,
        val query: String,
    ) : ExternalImportAction

    data class OnSearchOverrideSubmit(val packageName: String) : ExternalImportAction

    data object OnUndoLast : ExternalImportAction

    data object OnExit : ExternalImportAction

    data object OnDismissCompletionToast : ExternalImportAction

    data object OnAutoSummaryContinue : ExternalImportAction

    data object OnAutoSummaryUndoAll : ExternalImportAction

    data object OnAddManually : ExternalImportAction

    /**
     * Cancels in-flight scan / auto-import work and drops to the review
     * wizard with whatever candidates have already been resolved (or an
     * empty Done state if nothing made it through yet). Exposed via the
     * Skip button that surfaces after the progress screen has been up
     * past the skip threshold (~5 s).
     */
    data object OnSkipLongScan : ExternalImportAction
}
