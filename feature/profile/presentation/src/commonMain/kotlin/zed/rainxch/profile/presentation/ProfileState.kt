package zed.rainxch.profile.presentation

import zed.rainxch.core.domain.model.UserProfile

data class ProfileState(
    val userProfile: UserProfile? = null,
    val isLogoutDialogVisible: Boolean = false,
    val isUserLoggedIn: Boolean = false,
)
