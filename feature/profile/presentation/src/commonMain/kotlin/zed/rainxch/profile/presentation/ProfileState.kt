package zed.rainxch.profile.presentation

import zed.rainxch.core.domain.model.account.UserProfile

data class ProfileState(
    val userProfile: UserProfile? = null,
    val isLogoutDialogVisible: Boolean = false,
    val isUserLoggedIn: Boolean = false,
)
