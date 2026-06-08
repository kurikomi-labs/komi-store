package zed.rainxch.auth.data.crypto

data class PkceTriplet(
    val state: String,
    val codeVerifier: String,
    val codeChallenge: String,
)
