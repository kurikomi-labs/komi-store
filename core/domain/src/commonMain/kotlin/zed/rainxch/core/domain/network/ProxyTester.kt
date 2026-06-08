package zed.rainxch.core.domain.network

import zed.rainxch.core.domain.model.settings.ProxyConfig

interface ProxyTester {
    suspend fun test(config: ProxyConfig): ProxyTestOutcome

    suspend fun test(config: ProxyConfig, url: String): ProxyTestOutcome = test(config)
}

sealed interface ProxyTestOutcome {

    data class Success(
        val latencyMs: Long,
    ) : ProxyTestOutcome

    sealed interface Failure : ProxyTestOutcome {

        data object DnsFailure : Failure

        data object ProxyUnreachable : Failure

        data object Timeout : Failure

        data object ProxyAuthRequired : Failure

        data class UnexpectedResponse(
            val statusCode: Int,
        ) : Failure

        data class Unknown(
            val message: String?,
        ) : Failure
    }
}
