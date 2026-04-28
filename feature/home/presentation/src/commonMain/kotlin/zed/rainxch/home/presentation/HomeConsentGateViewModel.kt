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

    // Nullable so a fresh subscription doesn't briefly look like
    // "user hasn't answered" while the persisted value is still being
    // hydrated. HomeRoot only shows the sheet on an explicit NotYetAsked.
    val consent: StateFlow<ProductTelemetryConsent?> =
        tweaksRepository.getProductTelemetryConsent().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = null,
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
