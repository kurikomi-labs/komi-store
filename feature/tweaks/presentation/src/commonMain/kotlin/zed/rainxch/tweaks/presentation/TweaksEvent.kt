package zed.rainxch.tweaks.presentation

sealed interface TweaksEvent {
    data object OnProxySaved : TweaksEvent

    data class OnProxySaveError(
        val message: String,
    ) : TweaksEvent

    data class OnProxyTestSuccess(
        val latencyMs: Long,
    ) : TweaksEvent

    data class OnProxyTestError(
        val message: String,
    ) : TweaksEvent

    data object OnCacheCleared : TweaksEvent

    data class OnCacheClearError(
        val message: String,
    ) : TweaksEvent

    data object OnSeenHistoryCleared : TweaksEvent

    data object OnAnalyticsIdReset : TweaksEvent

    data object OnTranslationProviderSaved : TweaksEvent

    data object OnYoudaoCredentialsSaved : TweaksEvent

    data object OnLibreTranslateCredentialsSaved : TweaksEvent

    data object OnDeeplCredentialsSaved : TweaksEvent

    /**
     * Fired on platforms where changing the UI language cannot be
     * applied in-place (currently Desktop — no `Activity.recreate()`
     * equivalent). The UI prompts the user to restart so the new
     * locale takes effect on the next cold start. On Android this
     * event is never emitted; `MainActivity` handles runtime changes
     * via `recreate()` directly.
     */
    data object OnAppLanguageChangeRequiresRestart : TweaksEvent
}
