package zed.rainxch.auth.domain.repository

data class WebAuthRegistration(
    val state: String,
    val authUrl: String,
)
