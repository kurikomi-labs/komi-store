package zed.rainxch.repopages.presentation.security

import zed.rainxch.repopages.domain.model.SecurityOverview

data class SecurityUiState(
    val isLoading: Boolean = false,
    val overview: SecurityOverview? = null,
    val errorMessage: String? = null,
)
