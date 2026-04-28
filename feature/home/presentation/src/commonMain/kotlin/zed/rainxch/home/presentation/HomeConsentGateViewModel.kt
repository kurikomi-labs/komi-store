package zed.rainxch.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import zed.rainxch.core.domain.repository.TweaksRepository
import zed.rainxch.core.domain.telemetry.ProductTelemetryConsent

class HomeConsentGateViewModel(
    private val tweaksRepository: TweaksRepository,
) : ViewModel() {

    val consent: StateFlow<ProductTelemetryConsent> =
        tweaksRepository.getProductTelemetryConsent().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = ProductTelemetryConsent.NotYetAsked,
        )

    fun grant() {
        viewModelScope.launch {
            tweaksRepository.setProductTelemetryConsent(ProductTelemetryConsent.Granted)
        }
    }

    fun deny() {
        viewModelScope.launch {
            tweaksRepository.setProductTelemetryConsent(ProductTelemetryConsent.Denied)
        }
    }
}
