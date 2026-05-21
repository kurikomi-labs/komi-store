package zed.rainxch.githubstore

import zed.rainxch.core.domain.model.AppTheme
import zed.rainxch.core.domain.model.ContentWidth
import zed.rainxch.core.domain.model.FontTheme
import zed.rainxch.core.domain.model.RateLimitInfo

data class MainState(
    val isLoggedIn: Boolean = false,
    val rateLimitInfo: RateLimitInfo? = null,
    val showRateLimitDialog: Boolean = false,
    val showSessionExpiredDialog: Boolean = false,
    val currentColorTheme: AppTheme = AppTheme.NORD,
    val isAmoledTheme: Boolean = false,
    val isDarkTheme: Boolean? = null,
    val currentFontTheme: FontTheme = FontTheme.CUSTOM,
    val isScrollbarEnabled: Boolean = false,
    val contentWidth: ContentWidth = ContentWidth.COMPACT,
    /** First-launch onboarding state. `null` while the flow is still loading. */
    val onboardingComplete: Boolean? = null,
)
