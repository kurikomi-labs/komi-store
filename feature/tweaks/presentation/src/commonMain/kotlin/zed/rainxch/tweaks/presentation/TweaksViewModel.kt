package zed.rainxch.tweaks.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import zed.rainxch.core.domain.getPlatform
import zed.rainxch.core.domain.model.Platform
import zed.rainxch.core.domain.model.ProxyConfig
import zed.rainxch.core.domain.model.ProxyScope
import zed.rainxch.core.domain.model.TranslationProvider
import zed.rainxch.core.domain.network.ProxyTestOutcome
import zed.rainxch.core.domain.network.ProxyTester
import zed.rainxch.core.domain.repository.DeviceIdentityRepository
import zed.rainxch.core.domain.repository.ProxyRepository
import zed.rainxch.core.domain.repository.SeenReposRepository
import zed.rainxch.core.domain.repository.TelemetryRepository
import zed.rainxch.core.domain.repository.TweaksRepository
import zed.rainxch.core.domain.system.InstallerStatusProvider
import zed.rainxch.core.domain.system.UpdateScheduleManager
import zed.rainxch.core.domain.utils.BrowserHelper
import zed.rainxch.tweaks.presentation.model.ProxyScopeFormState
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.failed_to_save_proxy_settings
import zed.rainxch.githubstore.core.presentation.res.invalid_proxy_port
import zed.rainxch.githubstore.core.presentation.res.proxy_host_required
import zed.rainxch.githubstore.core.presentation.res.proxy_test_error_auth_required
import zed.rainxch.githubstore.core.presentation.res.proxy_test_error_dns
import zed.rainxch.githubstore.core.presentation.res.proxy_test_error_status
import zed.rainxch.githubstore.core.presentation.res.proxy_test_error_timeout
import zed.rainxch.githubstore.core.presentation.res.proxy_test_error_unknown
import zed.rainxch.githubstore.core.presentation.res.proxy_test_error_unreachable
import zed.rainxch.profile.domain.repository.ProfileRepository
import zed.rainxch.tweaks.presentation.model.ProxyType

class TweaksViewModel(
    private val browserHelper: BrowserHelper,
    private val tweaksRepository: TweaksRepository,
    private val profileRepository: ProfileRepository,
    private val installerStatusProvider: InstallerStatusProvider,
    private val proxyRepository: ProxyRepository,
    private val proxyTester: ProxyTester,
    private val updateScheduleManager: UpdateScheduleManager,
    private val seenReposRepository: SeenReposRepository,
    private val deviceIdentityRepository: DeviceIdentityRepository,
    private val telemetryRepository: TelemetryRepository,
) : ViewModel() {
    private var hasLoadedInitialData = false
    private var cacheSizeJob: Job? = null

    private val _state = MutableStateFlow(TweaksState())
    val state =
        _state
            .onStart {
                if (!hasLoadedInitialData) {
                    loadCurrentTheme()
                    loadVersionName()
                    loadProxyConfig()
                    loadInstallerPreference()
                    loadAutoUpdatePreference()
                    loadUpdateCheckInterval()
                    loadIncludePreReleases()
                    loadLiquidGlassEnabled()
                    loadHideSeenEnabled()
                    loadScrollbarEnabled()
                    loadTelemetryEnabled()
                    loadProductTelemetryConsent()
                    loadTranslationSettings()
                    loadAppLanguage()

                    observeShizukuStatus()

                    hasLoadedInitialData = true
                }
                refreshCacheSize()
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = TweaksState(),
            )

    private val _events = Channel<TweaksEvent>()
    val events = _events.receiveAsFlow()

    private fun refreshCacheSize() {
        if (cacheSizeJob?.isActive == true) return
        cacheSizeJob =
            viewModelScope.launch {
                profileRepository.observeCacheSize().collect { sizeBytes ->
                    _state.update {
                        it.copy(cacheSize = formatCacheSize(sizeBytes))
                    }
                }
            }
    }

    private fun formatCacheSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        var size = bytes.toDouble()
        var unitIndex = 0
        while (size >= 1024 && unitIndex < units.lastIndex) {
            size /= 1024
            unitIndex++
        }
        return if (size == size.toLong().toDouble()) {
            "${size.toLong()} ${units[unitIndex]}"
        } else {
            "${"%.1f".format(size)} ${units[unitIndex]}"
        }
    }

    private fun loadVersionName() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    versionName = profileRepository.getVersionName(),
                )
            }
        }
    }

    private fun loadCurrentTheme() {
        viewModelScope.launch {
            tweaksRepository.getThemeColor().collect { theme ->
                _state.update {
                    it.copy(selectedThemeColor = theme)
                }
            }
        }

        viewModelScope.launch {
            tweaksRepository.getAmoledTheme().collect { isAmoled ->
                _state.update {
                    it.copy(isAmoledThemeEnabled = isAmoled)
                }
            }
        }

        viewModelScope.launch {
            tweaksRepository.getIsDarkTheme().collect { isDarkTheme ->
                _state.update {
                    it.copy(isDarkTheme = isDarkTheme)
                }
            }
        }

        viewModelScope.launch {
            tweaksRepository.getFontTheme().collect { fontTheme ->
                _state.update {
                    it.copy(selectedFontTheme = fontTheme)
                }
            }
        }

        viewModelScope.launch {
            tweaksRepository.getAutoDetectClipboardLinks().collect { enabled ->
                _state.update {
                    it.copy(autoDetectClipboardLinks = enabled)
                }
            }
        }
    }

    private fun loadProxyConfig() {
        // Start one collector per scope. Each updates its slot in the
        // [TweaksState.proxyForms] map — scopes are independent so the
        // flows intentionally don't share state.
        //
        // If the user has an in-progress edit on a scope (isDraftDirty)
        // we skip hydration for that scope until they commit (save) or
        // reset (switch type via OnProxyTypeSelected for None/System).
        // DataStore emits on *any* preference change — without this
        // guard, toggling any unrelated setting while the user is mid-
        // typing in the host field would snap the form back to persisted
        // values.
        ProxyScope.entries.forEach { scope ->
            viewModelScope.launch {
                proxyRepository.getProxyConfig(scope).collect { config ->
                    _state.update { state ->
                        val existing = state.formFor(scope)
                        if (existing.isDraftDirty) return@update state
                        val populated =
                            existing.copy(
                                type = ProxyType.fromConfig(config),
                                host =
                                    when (config) {
                                        is ProxyConfig.Http -> config.host
                                        is ProxyConfig.Socks -> config.host
                                        else -> existing.host
                                    },
                                port =
                                    when (config) {
                                        is ProxyConfig.Http -> config.port.toString()
                                        is ProxyConfig.Socks -> config.port.toString()
                                        else -> existing.port
                                    },
                                username =
                                    when (config) {
                                        is ProxyConfig.Http -> config.username.orEmpty()
                                        is ProxyConfig.Socks -> config.username.orEmpty()
                                        else -> existing.username
                                    },
                                password =
                                    when (config) {
                                        is ProxyConfig.Http -> config.password.orEmpty()
                                        is ProxyConfig.Socks -> config.password.orEmpty()
                                        else -> existing.password
                                    },
                            )
                        state.copy(
                            proxyForms = state.proxyForms + (scope to populated),
                        )
                    }
                }
            }
        }
    }

    /** User-triggered form edit — marks the scope dirty so the
     *  preferences flow won't clobber the edit on an unrelated emit. */
    private fun mutateForm(
        scope: ProxyScope,
        block: (ProxyScopeFormState) -> ProxyScopeFormState,
    ) {
        _state.update { state ->
            val updated = block(state.formFor(scope)).copy(isDraftDirty = true)
            state.copy(
                proxyForms = state.proxyForms + (scope to updated),
            )
        }
    }

    /** Transient UI-state mutation (password visibility, test-in-
     *  progress) — does *not* mark the scope dirty, so toggling the eye
     *  icon or running a test doesn't block preference hydration. Only
     *  use for flags that don't represent a real config change the user
     *  expects to save. */
    private fun mutateFormUi(
        scope: ProxyScope,
        block: (ProxyScopeFormState) -> ProxyScopeFormState,
    ) {
        _state.update { state ->
            state.copy(
                proxyForms = state.proxyForms + (scope to block(state.formFor(scope))),
            )
        }
    }

    /** Clears the dirty flag — call after a successful save or an
     *  explicit reset so the next preferences emission can re-hydrate
     *  the form. */
    private fun clearDirty(scope: ProxyScope) {
        _state.update { state ->
            val form = state.formFor(scope)
            if (!form.isDraftDirty) return@update state
            state.copy(
                proxyForms = state.proxyForms + (scope to form.copy(isDraftDirty = false)),
            )
        }
    }

    private fun loadInstallerPreference() {
        viewModelScope.launch {
            tweaksRepository.getInstallerType().collect { type ->
                _state.update {
                    it.copy(installerType = type)
                }
            }
        }
    }

    private fun observeShizukuStatus() {
        viewModelScope.launch {
            installerStatusProvider.shizukuAvailability.collect { availability ->
                _state.update {
                    it.copy(shizukuAvailability = availability)
                }
            }
        }
    }

    private fun loadAutoUpdatePreference() {
        viewModelScope.launch {
            tweaksRepository.getAutoUpdateEnabled().collect { enabled ->
                _state.update {
                    it.copy(autoUpdateEnabled = enabled)
                }
            }
        }
    }

    private fun loadUpdateCheckInterval() {
        viewModelScope.launch {
            tweaksRepository.getUpdateCheckInterval().collect { hours ->
                _state.update {
                    it.copy(updateCheckIntervalHours = hours)
                }
            }
        }
    }

    private fun loadLiquidGlassEnabled() {
        viewModelScope.launch {
            tweaksRepository.getLiquidGlassEnabled().collect { enabled ->
                _state.update {
                    it.copy(isLiquidGlassEnabled = enabled)
                }
            }
        }
    }

    private fun loadHideSeenEnabled() {
        viewModelScope.launch {
            tweaksRepository.getHideSeenEnabled().collect { enabled ->
                _state.update {
                    it.copy(isHideSeenEnabled = enabled)
                }
            }
        }
    }

    private fun loadScrollbarEnabled() {
        viewModelScope.launch {
            tweaksRepository.getScrollbarEnabled().collect { enabled ->
                _state.update {
                    it.copy(isScrollbarEnabled = enabled)
                }
            }
        }
    }

    private fun loadTelemetryEnabled() {
        viewModelScope.launch {
            tweaksRepository.getTelemetryEnabled().collect { enabled ->
                _state.update {
                    it.copy(isTelemetryEnabled = enabled)
                }
            }
        }
    }

    private fun loadProductTelemetryConsent() {
        viewModelScope.launch {
            tweaksRepository.getProductTelemetryConsent().collect { consent ->
                _state.update {
                    it.copy(productTelemetryConsent = consent)
                }
            }
        }
    }

    private fun loadTranslationSettings() {
        viewModelScope.launch {
            tweaksRepository.getTranslationProvider().collect { provider ->
                _state.update { it.copy(translationProvider = provider) }
            }
        }
        viewModelScope.launch {
            tweaksRepository.getYoudaoAppKey().collect { appKey ->
                _state.update { it.copy(youdaoAppKey = appKey) }
            }
        }
        viewModelScope.launch {
            tweaksRepository.getYoudaoAppSecret().collect { appSecret ->
                _state.update { it.copy(youdaoAppSecret = appSecret) }
            }
        }
    }

    private fun loadAppLanguage() {
        viewModelScope.launch {
            tweaksRepository.getAppLanguage().collect { tag ->
                _state.update { it.copy(selectedAppLanguage = tag) }
            }
        }
    }

    private fun loadIncludePreReleases() {
        viewModelScope.launch {
            tweaksRepository.getIncludePreReleases().collect { enabled ->
                _state.update {
                    it.copy(includePreReleases = enabled)
                }
            }
        }
    }

    fun onAction(action: TweaksAction) {
        when (action) {
            TweaksAction.OnNavigateBackClick -> {
                // Handled in composable
            }

            is TweaksAction.OnThemeColorSelected -> {
                viewModelScope.launch {
                    tweaksRepository.setThemeColor(action.themeColor)
                }
            }

            is TweaksAction.OnAmoledThemeToggled -> {
                viewModelScope.launch {
                    tweaksRepository.setAmoledTheme(action.enabled)
                }
            }

            is TweaksAction.OnDarkThemeChange -> {
                viewModelScope.launch {
                    tweaksRepository.setDarkTheme(action.isDarkTheme)
                }
            }

            is TweaksAction.OnFontThemeSelected -> {
                viewModelScope.launch {
                    tweaksRepository.setFontTheme(action.fontTheme)
                }
            }

            is TweaksAction.OnLiquidGlassEnabledChange -> {
                viewModelScope.launch {
                    tweaksRepository.setLiquidGlassEnabled(action.enabled)
                }
            }

            is TweaksAction.OnScrollbarToggled -> {
                viewModelScope.launch {
                    tweaksRepository.setScrollbarEnabled(action.enabled)
                }
            }

            is TweaksAction.OnProxyTypeSelected -> {
                mutateForm(action.scope) { it.copy(type = action.type) }
                // NONE / SYSTEM have no form fields — persist immediately
                // since there's nothing left for the user to fill in. For
                // HTTP / SOCKS, wait for an explicit Save so validation
                // can run against a completed form.
                if (action.type == ProxyType.NONE || action.type == ProxyType.SYSTEM) {
                    val config =
                        if (action.type == ProxyType.NONE) {
                            ProxyConfig.None
                        } else {
                            ProxyConfig.System
                        }
                    viewModelScope.launch {
                        runCatching {
                            proxyRepository.setProxyConfig(action.scope, config)
                        }.onSuccess {
                            // Committed — allow preferences-flow hydration
                            // to resume for this scope.
                            clearDirty(action.scope)
                            _events.send(TweaksEvent.OnProxySaved)
                        }.onFailure { error ->
                            _events.send(
                                TweaksEvent.OnProxySaveError(
                                    error.message ?: getString(Res.string.failed_to_save_proxy_settings),
                                ),
                            )
                        }
                    }
                }
            }

            is TweaksAction.OnProxyHostChanged -> {
                mutateForm(action.scope) { it.copy(host = action.host) }
            }

            is TweaksAction.OnProxyPortChanged -> {
                mutateForm(action.scope) { it.copy(port = action.port) }
            }

            is TweaksAction.OnProxyUsernameChanged -> {
                mutateForm(action.scope) { it.copy(username = action.username) }
            }

            is TweaksAction.OnProxyPasswordChanged -> {
                mutateForm(action.scope) { it.copy(password = action.password) }
            }

            is TweaksAction.OnProxyPasswordVisibilityToggle -> {
                mutateFormUi(action.scope) {
                    it.copy(isPasswordVisible = !it.isPasswordVisible)
                }
            }

            is TweaksAction.OnProxySave -> {
                val form = _state.value.formFor(action.scope)
                // Only HTTP/SOCKS need host+port — validate for those
                // only. NONE/SYSTEM carry no form fields and would
                // otherwise be rejected with "host required" for no
                // reason if something ever triggered Save for them
                // (today the UI doesn't, but defense in depth).
                val config: ProxyConfig =
                    when (form.type) {
                        ProxyType.NONE -> ProxyConfig.None
                        ProxyType.SYSTEM -> ProxyConfig.System
                        ProxyType.HTTP, ProxyType.SOCKS -> {
                            val port =
                                form.port
                                    .toIntOrNull()
                                    ?.takeIf { it in 1..65535 }
                                    ?: run {
                                        viewModelScope.launch {
                                            _events.send(
                                                TweaksEvent.OnProxySaveError(
                                                    getString(Res.string.invalid_proxy_port),
                                                ),
                                            )
                                        }
                                        return
                                    }
                            val host =
                                form.host.trim().takeIf { it.isNotBlank() }
                                    ?: run {
                                        viewModelScope.launch {
                                            _events.send(
                                                TweaksEvent.OnProxySaveError(
                                                    getString(Res.string.proxy_host_required),
                                                ),
                                            )
                                        }
                                        return
                                    }
                            val username = form.username.takeIf { it.isNotBlank() }
                            val password = form.password.takeIf { it.isNotBlank() }
                            if (form.type == ProxyType.HTTP) {
                                ProxyConfig.Http(host, port, username, password)
                            } else {
                                ProxyConfig.Socks(host, port, username, password)
                            }
                        }
                    }

                viewModelScope.launch {
                    runCatching {
                        proxyRepository.setProxyConfig(action.scope, config)
                    }.onSuccess {
                        clearDirty(action.scope)
                        _events.send(TweaksEvent.OnProxySaved)
                    }.onFailure { error ->
                        _events.send(
                            TweaksEvent.OnProxySaveError(
                                error.message ?: getString(Res.string.failed_to_save_proxy_settings),
                            ),
                        )
                    }
                }
            }

            is TweaksAction.OnProxyTest -> {
                val form = _state.value.formFor(action.scope)
                if (form.isTestInProgress) return
                val config = buildProxyConfigForTest(action.scope) ?: return
                mutateFormUi(action.scope) { it.copy(isTestInProgress = true) }
                viewModelScope.launch {
                    val outcome: ProxyTestOutcome =
                        try {
                            proxyTester.test(config)
                        } catch (e: CancellationException) {
                            // Preserve structured concurrency — never swallow.
                            throw e
                        } catch (e: Exception) {
                            ProxyTestOutcome.Failure.Unknown(e.message)
                        } finally {
                            mutateFormUi(action.scope) { it.copy(isTestInProgress = false) }
                        }
                    _events.send(outcome.toEvent())
                }
            }

            is TweaksAction.OnInstallerTypeSelected -> {
                viewModelScope.launch {
                    tweaksRepository.setInstallerType(action.type)
                }
            }

            TweaksAction.OnRequestShizukuPermission -> {
                installerStatusProvider.requestShizukuPermission()
            }

            is TweaksAction.OnAutoUpdateToggled -> {
                viewModelScope.launch {
                    tweaksRepository.setAutoUpdateEnabled(action.enabled)
                }
            }

            is TweaksAction.OnUpdateCheckIntervalChanged -> {
                viewModelScope.launch {
                    tweaksRepository.setUpdateCheckInterval(action.hours)
                    updateScheduleManager.reschedule(action.hours)
                }
            }

            is TweaksAction.OnIncludePreReleasesToggled -> {
                viewModelScope.launch {
                    tweaksRepository.setIncludePreReleases(action.enabled)
                }
            }

            is TweaksAction.OnAutoDetectClipboardToggled -> {
                viewModelScope.launch {
                    tweaksRepository.setAutoDetectClipboardLinks(action.enabled)
                }
            }

            is TweaksAction.OnHideSeenToggled -> {
                viewModelScope.launch {
                    tweaksRepository.setHideSeenEnabled(action.enabled)
                }
            }

            TweaksAction.OnClearSeenRepos -> {
                viewModelScope.launch {
                    seenReposRepository.clearAll()
                    _events.send(TweaksEvent.OnSeenHistoryCleared)
                }
            }

            TweaksAction.OnRefreshCacheSize -> {
                refreshCacheSize()
            }

            TweaksAction.OnClearCacheClick -> {
                _state.update { it.copy(isClearDownloadsDialogVisible = true) }
            }

            TweaksAction.OnClearDownloadsConfirm -> {
                _state.update { it.copy(isClearDownloadsDialogVisible = false) }
                viewModelScope.launch {
                    runCatching {
                        profileRepository.clearCache()
                    }.onSuccess {
                        cacheSizeJob?.cancel()
                        cacheSizeJob = null
                        refreshCacheSize()
                        _events.send(TweaksEvent.OnCacheCleared)
                    }.onFailure { error ->
                        _events.send(
                            TweaksEvent.OnCacheClearError(
                                error.message ?: "Failed to clear downloads",
                            ),
                        )
                    }
                }
            }

            TweaksAction.OnClearDownloadsDismiss -> {
                _state.update { it.copy(isClearDownloadsDialogVisible = false) }
            }

            TweaksAction.OnHelpClick -> {
                browserHelper.openUrl(
                    url = "https://github.com/OpenHub-Store/GitHub-Store/issues",
                )
            }

            is TweaksAction.OnTelemetryToggled -> {
                viewModelScope.launch {
                    tweaksRepository.setTelemetryEnabled(action.enabled)
                }
            }

            is TweaksAction.OnProductTelemetryToggled -> {
                viewModelScope.launch {
                    tweaksRepository.setProductTelemetryConsent(
                        if (action.enabled) zed.rainxch.core.domain.telemetry.ProductTelemetryConsent.Granted
                        else zed.rainxch.core.domain.telemetry.ProductTelemetryConsent.Denied,
                    )
                }
            }

            TweaksAction.OnResetAnalyticsId -> {
                viewModelScope.launch {
                    // Clear the telemetry buffer *before* resetting the ID.
                    // Order matters: any buffered event still carries the
                    // old device ID in its EventRequest payload, so draining
                    // them after the reset would leak the old ID to the
                    // backend attached to "fresh start" identity semantics.
                    telemetryRepository.clearPending()
                    deviceIdentityRepository.resetDeviceId()
                    _events.send(TweaksEvent.OnAnalyticsIdReset)
                }
            }

            is TweaksAction.OnTranslationProviderSelected -> {
                when (action.provider) {
                    TranslationProvider.GOOGLE -> {
                        // No credentials required — persist immediately
                        // and clear any pending draft selection.
                        _state.update { it.copy(draftTranslationProvider = null) }
                        viewModelScope.launch {
                            tweaksRepository.setTranslationProvider(action.provider)
                            _events.send(TweaksEvent.OnTranslationProviderSaved)
                        }
                    }
                    TranslationProvider.YOUDAO -> {
                        val current = _state.value
                        val hasCreds =
                            current.youdaoAppKey.isNotBlank() &&
                                current.youdaoAppSecret.isNotBlank()
                        if (hasCreds) {
                            _state.update { it.copy(draftTranslationProvider = null) }
                            viewModelScope.launch {
                                tweaksRepository.setTranslationProvider(action.provider)
                                _events.send(TweaksEvent.OnTranslationProviderSaved)
                            }
                        } else {
                            // No credentials yet — expose the selection as
                            // a draft so the UI expands the credentials
                            // form, but don't commit to storage. If we
                            // persisted here the next translation attempt
                            // would fail with "not configured" and any
                            // other repository that observes the flow
                            // would snap back on the next re-emission.
                            // Committed later from [OnYoudaoCredentialsSave].
                            _state.update {
                                it.copy(draftTranslationProvider = TranslationProvider.YOUDAO)
                            }
                        }
                    }
                }
            }

            is TweaksAction.OnYoudaoAppKeyChanged -> {
                _state.update { it.copy(youdaoAppKey = action.appKey) }
            }

            is TweaksAction.OnYoudaoAppSecretChanged -> {
                _state.update { it.copy(youdaoAppSecret = action.appSecret) }
            }

            TweaksAction.OnYoudaoAppSecretVisibilityToggle -> {
                _state.update {
                    it.copy(isYoudaoAppSecretVisible = !it.isYoudaoAppSecretVisible)
                }
            }

            TweaksAction.OnYoudaoCredentialsSave -> {
                val current = _state.value
                viewModelScope.launch {
                    tweaksRepository.setYoudaoAppKey(current.youdaoAppKey)
                    tweaksRepository.setYoudaoAppSecret(current.youdaoAppSecret)
                    // Auto-switch to YOUDAO when the user explicitly saves
                    // credentials — saves them an extra tap and matches
                    // the implicit intent ("I just configured this, use
                    // it"). Also covers the "draft" case where the chip
                    // was picked but not yet persisted because creds
                    // were missing.
                    val shouldActivate =
                        current.youdaoAppKey.isNotBlank() &&
                            current.youdaoAppSecret.isNotBlank() &&
                            (
                                current.translationProvider != TranslationProvider.YOUDAO ||
                                    current.draftTranslationProvider == TranslationProvider.YOUDAO
                            )
                    if (shouldActivate) {
                        tweaksRepository.setTranslationProvider(TranslationProvider.YOUDAO)
                    }
                    // Drop any draft — either we committed it above or
                    // the user emptied fields and cancelled implicitly.
                    _state.update { it.copy(draftTranslationProvider = null) }
                    _events.send(TweaksEvent.OnYoudaoCredentialsSaved)
                }
            }

            is TweaksAction.OnAppLanguageSelected -> {
                if (action.tag == _state.value.selectedAppLanguage) return
                viewModelScope.launch {
                    tweaksRepository.setAppLanguage(action.tag)
                    if (getPlatform() != Platform.ANDROID) {
                        _events.send(TweaksEvent.OnAppLanguageChangeRequiresRestart)
                    }
                }
            }
        }
    }

    /**
     * Builds the [ProxyConfig] to test from the current form state for [scope].
     * For [ProxyType.HTTP] / [ProxyType.SOCKS] this requires a valid host and
     * port — if either is missing the user is told via an error event and
     * `null` is returned, mirroring the validation in [TweaksAction.OnProxySave].
     */
    private fun buildProxyConfigForTest(scope: ProxyScope): ProxyConfig? {
        val form = _state.value.formFor(scope)
        return when (form.type) {
            ProxyType.NONE -> ProxyConfig.None
            ProxyType.SYSTEM -> ProxyConfig.System
            ProxyType.HTTP, ProxyType.SOCKS -> {
                val port =
                    form.port
                        .toIntOrNull()
                        ?.takeIf { it in 1..65535 }
                        ?: run {
                            viewModelScope.launch {
                                _events.send(
                                    TweaksEvent.OnProxyTestError(
                                        getString(Res.string.invalid_proxy_port),
                                    ),
                                )
                            }
                            return null
                        }
                val host =
                    form.host.trim().takeIf { it.isNotBlank() }
                        ?: run {
                            viewModelScope.launch {
                                _events.send(
                                    TweaksEvent.OnProxyTestError(
                                        getString(Res.string.proxy_host_required),
                                    ),
                                )
                            }
                            return null
                        }
                val username = form.username.takeIf { it.isNotBlank() }
                val password = form.password.takeIf { it.isNotBlank() }
                if (form.type == ProxyType.HTTP) {
                    ProxyConfig.Http(host, port, username, password)
                } else {
                    ProxyConfig.Socks(host, port, username, password)
                }
            }
        }
    }

    private suspend fun ProxyTestOutcome.toEvent(): TweaksEvent =
        when (this) {
            is ProxyTestOutcome.Success ->
                TweaksEvent.OnProxyTestSuccess(latencyMs = latencyMs)

            ProxyTestOutcome.Failure.DnsFailure ->
                TweaksEvent.OnProxyTestError(getString(Res.string.proxy_test_error_dns))

            ProxyTestOutcome.Failure.ProxyUnreachable ->
                TweaksEvent.OnProxyTestError(getString(Res.string.proxy_test_error_unreachable))

            ProxyTestOutcome.Failure.Timeout ->
                TweaksEvent.OnProxyTestError(getString(Res.string.proxy_test_error_timeout))

            ProxyTestOutcome.Failure.ProxyAuthRequired ->
                TweaksEvent.OnProxyTestError(getString(Res.string.proxy_test_error_auth_required))

            is ProxyTestOutcome.Failure.UnexpectedResponse ->
                TweaksEvent.OnProxyTestError(
                    getString(Res.string.proxy_test_error_status, statusCode),
                )

            is ProxyTestOutcome.Failure.Unknown ->
                // Raw exception messages are platform-specific, untranslated,
                // and may leak internal detail — always show the localized
                // fallback to the user. The original `message` is intentionally
                // dropped here; surface it via logging if diagnostics are needed.
                TweaksEvent.OnProxyTestError(getString(Res.string.proxy_test_error_unknown))
        }
}
