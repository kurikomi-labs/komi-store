package zed.rainxch.details.domain.system

interface AttestationVerifier {

    suspend fun verify(
        owner: String,
        repoName: String,
        filePath: String,
    ): VerificationResult
}

sealed interface VerificationResult {
    data object Verified : VerificationResult

    data object Unverified : VerificationResult

    data class Error(val reason: String) : VerificationResult
}
