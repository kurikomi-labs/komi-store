package zed.rainxch.apps.presentation.import.model

enum class ImportPhase {
    Idle,
    RequestingPermission,
    Scanning,
    AutoImporting,
    AutoImportSummary,
    AwaitingReview,
    Done,
}
