package zed.rainxch.tweaks.presentation

import zed.rainxch.core.domain.model.AppTheme
import zed.rainxch.core.domain.model.DhizukuAvailability
import zed.rainxch.core.domain.model.FontTheme
import zed.rainxch.core.domain.model.InstallerAttribution
import zed.rainxch.core.domain.model.InstallerType
import zed.rainxch.core.domain.model.ProxyScope
import zed.rainxch.core.domain.model.RootAvailability
import zed.rainxch.core.domain.model.ShizukuAvailability
import zed.rainxch.core.domain.model.TranslationProvider
import zed.rainxch.tweaks.presentation.model.ProxyScopeFormState

data class TweaksState(
    val selectedThemeColor: AppTheme = AppTheme.OCEAN,
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
    val isTelemetryEnabled: Boolean = false,
    val translationProvider: TranslationProvider = TranslationProvider.Default,
    /**
     * Transient UI-only selection used when the user picks a provider
     * that needs more configuration before it can be activated (e.g.
     * Youdao with missing credentials). Rendered as the "selected
     * chip" when non-null; persisted [translationProvider] is the
     * source of truth for what the app actually uses for translation.
     * Cleared once the pending selection is either committed
     * (credentials saved) or abandoned (another provider picked).
     */
    val draftTranslationProvider: TranslationProvider? = null,
    val youdaoAppKey: String = "",
    val youdaoAppSecret: String = "",
    val isYoudaoAppSecretVisible: Boolean = false,
    /**
     * User-selected UI language as a BCP 47 tag, or `null` to follow
     * the system locale. Mirrors the preference observed by
     * `MainViewModel` — surfaced here so the Tweaks picker can show
     * which chip is selected.
     */
    val selectedAppLanguage: String? = null,
    val autoTranslateEnabled: Boolean = false,
    val autoTranslateTargetLang: String? = null,
    val isFeedbackSheetVisible: Boolean = false,
    /**
     * True only on aggressive-OEM Android devices (Oppo, OnePlus, Realme,
     * Xiaomi, vivo, Honor) that have NOT yet whitelisted the app from
     * battery optimization AND the user has not dismissed the prompt.
     * Drives the "Allow background updates" card in the Updates section.
     */
    val showBatteryOptimizationCard: Boolean = false,
    val customForgeHosts: Set<String> = emptySet(),
    val showCustomForgesDialog: Boolean = false,
    val customForgeDraft: String = "",
    val customForgeError: String? = null,
) {
    /** Effective provider to render as "selected" in the UI — draft
     *  overrides persisted when a pending selection is in flight. */
    val displayedTranslationProvider: TranslationProvider
        get() = draftTranslationProvider ?: translationProvider

    /** Convenience accessor — returns a fresh default if the map is
     *  missing an entry for [scope]. The constructor seeds all scopes,
     *  but `copy(proxyForms = …)` call sites could in theory produce an
     *  incomplete map; the safe default keeps the UI from crashing in
     *  that case. */
    fun formFor(scope: ProxyScope): ProxyScopeFormState =
        proxyForms[scope] ?: ProxyScopeFormState()
}
