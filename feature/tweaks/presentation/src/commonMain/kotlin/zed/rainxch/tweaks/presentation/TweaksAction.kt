package zed.rainxch.tweaks.presentation

import zed.rainxch.core.domain.model.AppTheme
import zed.rainxch.core.domain.model.ContentWidth
import zed.rainxch.core.domain.model.FontTheme
import zed.rainxch.core.domain.model.InstallerType
import zed.rainxch.core.domain.model.ProxyScope
import zed.rainxch.core.domain.model.TranslationProvider
import zed.rainxch.tweaks.presentation.model.ProxyType

sealed interface TweaksAction {
    data object OnNavigateBackClick : TweaksAction

    data class OnThemeColorSelected(
        val themeColor: AppTheme,
    ) : TweaksAction

    data class OnAmoledThemeToggled(
        val enabled: Boolean,
    ) : TweaksAction

    data class OnDarkThemeChange(
        val isDarkTheme: Boolean?,
    ) : TweaksAction

    data class OnFontThemeSelected(
        val fontTheme: FontTheme,
    ) : TweaksAction

    data class OnScrollbarToggled(
        val enabled: Boolean,
    ) : TweaksAction

    data class OnContentWidthSelected(
        val width: ContentWidth,
    ) : TweaksAction

    data class OnProxyTypeSelected(
        val scope: ProxyScope,
        val type: ProxyType,
    ) : TweaksAction

    data class OnProxyHostChanged(
        val scope: ProxyScope,
        val host: String,
    ) : TweaksAction

    data class OnProxyPortChanged(
        val scope: ProxyScope,
        val port: String,
    ) : TweaksAction

    data class OnProxyUsernameChanged(
        val scope: ProxyScope,
        val username: String,
    ) : TweaksAction

    data class OnProxyPasswordChanged(
        val scope: ProxyScope,
        val password: String,
    ) : TweaksAction

    data class OnProxyPasswordVisibilityToggle(
        val scope: ProxyScope,
    ) : TweaksAction

    data class OnProxySave(
        val scope: ProxyScope,
    ) : TweaksAction

    data class OnProxyTest(
        val scope: ProxyScope,
    ) : TweaksAction

    data class OnInstallerTypeSelected(
        val type: InstallerType,
    ) : TweaksAction

    data object OnRequestShizukuPermission : TweaksAction

    data object OnRequestDhizukuPermission : TweaksAction

    data object OnRequestRootPermission : TweaksAction

    data object OnInstallerAttributionSystemDefault : TweaksAction

    data class OnInstallerAttributionPresetSelected(
        val key: zed.rainxch.core.domain.model.PresetKey,
    ) : TweaksAction

    data object OnInstallerAttributionCustomToggleExpanded : TweaksAction

    data class OnInstallerAttributionCustomChanged(val value: String) : TweaksAction

    data object OnInstallerAttributionCustomSave : TweaksAction

    data class OnAutoUpdateToggled(
        val enabled: Boolean,
    ) : TweaksAction

    data class OnUpdateCheckIntervalChanged(
        val hours: Long,
    ) : TweaksAction

    data class OnUpdateCheckEnabledToggled(
        val enabled: Boolean,
    ) : TweaksAction

    data class OnIncludePreReleasesToggled(
        val enabled: Boolean,
    ) : TweaksAction

    data class OnAutoDetectClipboardToggled(
        val enabled: Boolean,
    ) : TweaksAction

    data class OnHideSeenToggled(
        val enabled: Boolean,
    ) : TweaksAction

    data object OnClearSeenRepos : TweaksAction

    data object OnRefreshCacheSize : TweaksAction

    data object OnClearCacheClick : TweaksAction

    data object OnClearDownloadsConfirm : TweaksAction

    data object OnClearDownloadsDismiss : TweaksAction

    data object OnFeedbackClick : TweaksAction

    data object OnFeedbackDismiss : TweaksAction

    data object OnMirrorPickerClick : TweaksAction

    data object OnSkippedUpdatesClick : TweaksAction

    data object OnHiddenRepositoriesClick : TweaksAction

    data class OnTranslationProviderSelected(
        val provider: TranslationProvider,
    ) : TweaksAction

    data class OnYoudaoAppKeyChanged(
        val appKey: String,
    ) : TweaksAction

    data class OnYoudaoAppSecretChanged(
        val appSecret: String,
    ) : TweaksAction

    data object OnYoudaoAppSecretVisibilityToggle : TweaksAction

    data object OnYoudaoCredentialsSave : TweaksAction

    data class OnLibreTranslateBaseUrlChanged(
        val url: String,
    ) : TweaksAction

    data class OnLibreTranslateApiKeyChanged(
        val apiKey: String,
    ) : TweaksAction

    data object OnLibreTranslateApiKeyVisibilityToggle : TweaksAction

    data object OnLibreTranslateCredentialsSave : TweaksAction

    data class OnDeeplAuthKeyChanged(
        val authKey: String,
    ) : TweaksAction

    data object OnDeeplAuthKeyVisibilityToggle : TweaksAction

    data object OnDeeplCredentialsSave : TweaksAction

    data class OnMicrosoftTranslatorKeyChanged(
        val key: String,
    ) : TweaksAction

    data class OnMicrosoftTranslatorRegionChanged(
        val region: String,
    ) : TweaksAction

    data object OnMicrosoftTranslatorKeyVisibilityToggle : TweaksAction

    data object OnMicrosoftTranslatorCredentialsSave : TweaksAction

    data class OnAutoTranslateEnabledToggle(
        val enabled: Boolean,
    ) : TweaksAction

    data class OnAutoTranslateTargetSelected(
        val tag: String?,
    ) : TweaksAction

    data class OnAppLanguageSelected(
        val tag: String?,
    ) : TweaksAction

    data object OnOpenBatteryOptimizationSettings : TweaksAction

    data object OnDismissBatteryOptimizationCard : TweaksAction

    data object OnReevaluateBatteryOptimizationCard : TweaksAction

    data object OnOpenCustomForgesDialog : TweaksAction
    data object OnDismissCustomForgesDialog : TweaksAction
    data class OnCustomForgeDraftChanged(val draft: String) : TweaksAction
    data object OnAddCustomForge : TweaksAction
    data class OnRemoveCustomForge(val host: String) : TweaksAction

    data object OnRestartNowClick : TweaksAction
    data object OnRestartLaterClick : TweaksAction

    data class OnMasterProxyTypeSelected(val type: ProxyType) : TweaksAction
    data class OnMasterProxyHostChanged(val host: String) : TweaksAction
    data class OnMasterProxyPortChanged(val port: String) : TweaksAction
    data class OnMasterProxyUsernameChanged(val username: String) : TweaksAction
    data class OnMasterProxyPasswordChanged(val password: String) : TweaksAction
    data object OnMasterProxyPasswordVisibilityToggle : TweaksAction
    data object OnMasterProxySave : TweaksAction
    data object OnMasterProxyTest : TweaksAction

    data class OnScopeUseMainToggled(val scope: ProxyScope, val useMain: Boolean) : TweaksAction

    data class OnTelemetryToggled(val enabled: Boolean) : TweaksAction
    data object OnTelemetryExpandToggle : TweaksAction
    data object OnClearSeenHistoryRequest : TweaksAction
    data object OnClearSeenHistoryDismiss : TweaksAction
    data object OnClearSeenHistoryConfirm : TweaksAction
}
