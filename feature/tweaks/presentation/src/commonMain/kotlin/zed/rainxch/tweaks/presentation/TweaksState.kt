package zed.rainxch.tweaks.presentation

import zed.rainxch.core.domain.model.AppTheme
import zed.rainxch.core.domain.model.ContentWidth
import zed.rainxch.core.domain.model.DhizukuAvailability
import zed.rainxch.core.domain.model.DiscoveryPlatform
import zed.rainxch.core.domain.model.FontTheme
import zed.rainxch.core.domain.model.InstallerAttribution
import zed.rainxch.core.domain.model.InstallerType
import zed.rainxch.core.domain.model.ProxyScope
import zed.rainxch.core.domain.model.RestartReason
import zed.rainxch.core.domain.model.RootAvailability
import zed.rainxch.core.domain.model.ShizukuAvailability
import zed.rainxch.core.domain.model.TranslationProvider
import zed.rainxch.tweaks.presentation.model.ProxyScopeFormState

data class TweaksState(
    val selectedThemeColor: AppTheme = AppTheme.NORD,
    val selectedFontTheme: FontTheme = FontTheme.CUSTOM,
    val isAmoledThemeEnabled: Boolean = false,
    val isDarkTheme: Boolean? = null,
    val versionName: String = "",
    val proxyForms: Map<ProxyScope, ProxyScopeFormState> =
        ProxyScope.entries.associateWith { ProxyScopeFormState() },
    val autoDetectClipboardLinks: Boolean = true,
    val cacheSize: String = "",
    val isClearDownloadsDialogVisible: Boolean = false,
    val installerType: InstallerType = InstallerType.DEFAULT,
    val installerAttribution: InstallerAttribution = InstallerAttribution.SystemDefault,
    val installerAttributionCustomDraft: String = "",
    val installerAttributionCustomExpanded: Boolean = false,
    val installerAttributionCustomError: String? = null,
    val shizukuAvailability: ShizukuAvailability = ShizukuAvailability.UNAVAILABLE,
    val dhizukuAvailability: DhizukuAvailability = DhizukuAvailability.UNAVAILABLE,
    val rootAvailability: RootAvailability = RootAvailability.UNAVAILABLE,
    val autoUpdateEnabled: Boolean = false,
    val updateCheckEnabled: Boolean = true,
    val updateCheckIntervalHours: Long = 6L,
    val includePreReleases: Boolean = false,
    val isHideSeenEnabled: Boolean = false,
    val isScrollbarEnabled: Boolean = false,
    val contentWidth: ContentWidth = ContentWidth.COMPACT,
    val translationProvider: TranslationProvider = TranslationProvider.Default,

    val draftTranslationProvider: TranslationProvider? = null,
    val youdaoAppKey: String = "",
    val youdaoAppSecret: String = "",
    val isYoudaoAppSecretVisible: Boolean = false,
    val libreTranslateBaseUrl: String = "",
    val libreTranslateApiKey: String = "",
    val isLibreTranslateApiKeyVisible: Boolean = false,
    val deeplAuthKey: String = "",
    val isDeeplAuthKeyVisible: Boolean = false,
    val microsoftTranslatorKey: String = "",
    val microsoftTranslatorRegion: String = "",
    val isMicrosoftTranslatorKeyVisible: Boolean = false,

    val selectedAppLanguage: String? = null,
    val autoTranslateEnabled: Boolean = false,
    val autoTranslateTargetLang: String? = null,
    val isFeedbackSheetVisible: Boolean = false,

    val showBatteryOptimizationCard: Boolean = false,
    val customForgeHosts: Set<String> = emptySet(),
    val showCustomForgesDialog: Boolean = false,
    val customForgeDraft: String = "",
    val customForgeError: String? = null,
    val needsRestartReasons: Set<RestartReason> = emptySet(),
    val restartBannerSessionDismissed: Boolean = false,
    val masterProxyForm: ProxyScopeFormState = ProxyScopeFormState(),
    val useMasterByScope: Map<ProxyScope, Boolean> =
        ProxyScope.entries.associateWith { false },
    val isClearSeenHistoryDialogVisible: Boolean = false,
    val selectedDiscoveryPlatforms: Set<DiscoveryPlatform> = emptySet(),
) {

    val restartBannerVisible: Boolean
        get() = needsRestartReasons.isNotEmpty() && !restartBannerSessionDismissed

    fun useMain(scope: ProxyScope): Boolean = useMasterByScope[scope] ?: false


    val displayedTranslationProvider: TranslationProvider
        get() = draftTranslationProvider ?: translationProvider

    fun formFor(scope: ProxyScope): ProxyScopeFormState =
        proxyForms[scope] ?: ProxyScopeFormState()
}
