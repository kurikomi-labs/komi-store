package zed.rainxch.core.presentation.status

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// Domain-semantic status colors (GitHub conventions). Shared-neutral, but light/dark
// resolved from the active personality's mode by PersonalityThemeProvider.
@Immutable
data class StatusColors(
    val issueOpen: Color,
    val issueClosed: Color,
    val pullOpen: Color,
    val pullMerged: Color,
    val pullClosed: Color,
    val severityCritical: Color,
    val severityHigh: Color,
    val severityMedium: Color,
    val severityLow: Color,
    val severityUnknown: Color,
    val statusReady: Color,
    val statusWarning: Color,
    val statusError: Color,
    val protectionSignature: Color,
    val protectionPrivileged: Color,
)
