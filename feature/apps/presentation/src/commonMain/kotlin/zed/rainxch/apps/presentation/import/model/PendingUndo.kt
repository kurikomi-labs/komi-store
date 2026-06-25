package zed.rainxch.apps.presentation.import.model

import zed.rainxch.core.domain.system.ExternalDecisionSnapshot

data class PendingUndo(
    val card: CandidateUi,
    val snapshot: ExternalDecisionSnapshot?,
    val hadInstalledAppRowBefore: Boolean,
    val kind: Kind,
) {
    val packageName: String get() = card.packageName
    val appLabel: String get() = card.appLabel

    enum class Kind { Skip, Link }
}
