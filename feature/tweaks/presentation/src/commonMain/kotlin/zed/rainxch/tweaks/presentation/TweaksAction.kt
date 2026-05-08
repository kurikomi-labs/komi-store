package zed.rainxch.tweaks.presentation

import zed.rainxch.core.domain.model.AppTheme
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

    data class OnTelemetryToggled(
        val enabled: Boolean,
    ) : TweaksAction

    data object OnResetAnalyticsId : TweaksAction

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

    /**
     * User picked a UI language. `tag == null` means "follow system
     * locale" — cleared persisted preference.
     */
    data class OnAppLanguageSelected(
        val tag: String?,
    ) : TweaksAction

    data object OnOpenBatteryOptimizationSettings : TweaksAction

    data object OnDismissBatteryOptimizationCard : TweaksAction

    /**
     * Re-evaluates whether the battery-optimization card should still
     * show. Fired on Tweaks resume so the card disappears immediately
     * after the user grants the exemption from the system Settings
     * screen.
     */
    data object OnReevaluateBatteryOptimizationCard : TweaksAction
}
