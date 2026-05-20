package zed.rainxch.core.domain.repository

import kotlinx.coroutines.flow.Flow
import zed.rainxch.core.domain.model.AnnouncementCategory
import zed.rainxch.core.domain.model.AppTheme
import zed.rainxch.core.domain.model.ContentWidth
import zed.rainxch.core.domain.model.DiscoveryPlatform
import zed.rainxch.core.domain.model.FontTheme
import zed.rainxch.core.domain.model.InstallerType
import zed.rainxch.core.domain.model.TranslationProvider

interface TweaksRepository {
    fun getThemeColor(): Flow<AppTheme>

    suspend fun setThemeColor(theme: AppTheme)

    fun getIsDarkTheme(): Flow<Boolean?>

    suspend fun setDarkTheme(isDarkTheme: Boolean?)

    fun getAmoledTheme(): Flow<Boolean>

    suspend fun setAmoledTheme(enabled: Boolean)

    fun getFontTheme(): Flow<FontTheme>

    suspend fun setFontTheme(fontTheme: FontTheme)

    fun getAutoDetectClipboardLinks(): Flow<Boolean>

    suspend fun setAutoDetectClipboardLinks(enabled: Boolean)

    fun getInstallerType(): Flow<InstallerType>

    suspend fun setInstallerType(type: InstallerType)

    fun getInstallerAttribution(): Flow<zed.rainxch.core.domain.model.InstallerAttribution>

    suspend fun setInstallerAttribution(attribution: zed.rainxch.core.domain.model.InstallerAttribution)

    fun getAutoUpdateEnabled(): Flow<Boolean>

    suspend fun setAutoUpdateEnabled(enabled: Boolean)

    fun getUpdateCheckEnabled(): Flow<Boolean>

    suspend fun setUpdateCheckEnabled(enabled: Boolean)

    fun getUpdateCheckInterval(): Flow<Long>

    suspend fun setUpdateCheckInterval(hours: Long)

    fun getIncludePreReleases(): Flow<Boolean>

    suspend fun setIncludePreReleases(enabled: Boolean)

    fun getHideSeenEnabled(): Flow<Boolean>

    suspend fun setHideSeenEnabled(enabled: Boolean)

    fun getDiscoveryPlatforms(): Flow<Set<DiscoveryPlatform>>

    suspend fun setDiscoveryPlatforms(platforms: Set<DiscoveryPlatform>)

    fun getScrollbarEnabled(): Flow<Boolean>

    suspend fun setScrollbarEnabled(enabled: Boolean)

    fun getContentWidth(): Flow<ContentWidth>

    suspend fun setContentWidth(width: ContentWidth)

    fun getTelemetryEnabled(): Flow<Boolean>

    suspend fun setTelemetryEnabled(enabled: Boolean)

    fun getTranslationProvider(): Flow<TranslationProvider>

    suspend fun setTranslationProvider(provider: TranslationProvider)

    fun getYoudaoAppKey(): Flow<String>

    suspend fun setYoudaoAppKey(appKey: String)

    fun getYoudaoAppSecret(): Flow<String>

    suspend fun setYoudaoAppSecret(appSecret: String)

    fun getLibreTranslateBaseUrl(): Flow<String>

    suspend fun setLibreTranslateBaseUrl(url: String)

    fun getLibreTranslateApiKey(): Flow<String>

    suspend fun setLibreTranslateApiKey(apiKey: String)

    fun getDeeplAuthKey(): Flow<String>

    suspend fun setDeeplAuthKey(authKey: String)

    fun getMicrosoftTranslatorKey(): Flow<String>

    suspend fun setMicrosoftTranslatorKey(key: String)

    fun getMicrosoftTranslatorRegion(): Flow<String>

    suspend fun setMicrosoftTranslatorRegion(region: String)

    /**
     * Selected UI language as a BCP 47 tag (e.g. `zh-CN`). Emits
     * `null` when the user hasn't picked one — which means "follow
     * whatever the JVM/Android locale is" at app start. `null` is
     * distinct from `""`: the former is the unset state, the latter
     * would be a malformed user choice we don't support.
     */
    fun getAppLanguage(): Flow<String?>

    suspend fun setAppLanguage(tag: String?)

    /**
     * When `true`, Details kicks off a translation of the README and
     * release notes immediately on load, using
     * [getAutoTranslateTargetLang] as the target. Default `false`.
     */
    fun getAutoTranslateEnabled(): Flow<Boolean>

    suspend fun setAutoTranslateEnabled(enabled: Boolean)

    /**
     * BCP 47 tag of the language Details auto-translates into when
     * [getAutoTranslateEnabled] is `true`. `null` falls back to the
     * UI language ([getAppLanguage]) at translate time.
     */
    fun getAutoTranslateTargetLang(): Flow<String?>

    suspend fun setAutoTranslateTargetLang(tag: String?)

    fun getExternalImportEnabled(): Flow<Boolean>

    suspend fun setExternalImportEnabled(enabled: Boolean)

    fun getExternalMatchSearchEnabled(): Flow<Boolean>

    suspend fun setExternalMatchSearchEnabled(enabled: Boolean)

    fun getExternalImportBannerDismissedAtCount(): Flow<Int>

    suspend fun setExternalImportBannerDismissedAtCount(count: Int)

    /**
     * Permanent dismissal flag for the Keep Android Open campaign banner on
     * Apps. False until user taps the close button; flips to true forever
     * once dismissed.
     */
    fun getKaoBannerDismissed(): Flow<Boolean>

    suspend fun setKaoBannerDismissed(dismissed: Boolean)

    /**
     * One-shot flag for the APK Inspect coachmark next to the install
     * button on the details screen. `false` until the user has seen the
     * coachmark at least once; flips permanently to `true` thereafter.
     */
    fun getApkInspectCoachmarkShown(): Flow<Boolean>

    suspend fun setApkInspectCoachmarkShown(shown: Boolean)

    /**
     * One-shot flag for the release-channel coachmark on the Details
     * screen. Survey signal — users don't realise the per-app channel
     * chip toggles betas. `false` until shown at least once; permanent
     * `true` after dismissal.
     */
    fun getChannelChipCoachmarkShown(): Flow<Boolean>

    suspend fun setChannelChipCoachmarkShown(shown: Boolean)

    /**
     * When true, the release-assets picker on Details shows installers
     * for every OS (grouped by platform section). When false (default),
     * only assets installable on the current platform are listed.
     */
    fun getShowAllPlatforms(): Flow<Boolean>

    suspend fun setShowAllPlatforms(enabled: Boolean)

    /**
     * One-shot watermark for the battery-optimization prompt on
     * aggressive-OEM ROMs (Oppo / OnePlus / Realme / Xiaomi / vivo /
     * Honor). `false` until the user has either granted the exemption
     * or explicitly dismissed the prompt; flips to `true` afterwards
     * and is never re-shown.
     */
    fun getBatteryOptimizationPromptDismissed(): Flow<Boolean>

    suspend fun setBatteryOptimizationPromptDismissed(dismissed: Boolean)

    fun getLastSeenWhatsNewVersionCode(): Flow<Int?>

    suspend fun setLastSeenWhatsNewVersionCode(versionCode: Int)

    fun getAnnouncementsDismissedIds(): Flow<Set<String>>

    suspend fun addAnnouncementDismissedId(id: String)

    fun getAnnouncementsAcknowledgedIds(): Flow<Set<String>>

    suspend fun addAnnouncementAcknowledgedId(id: String)

    fun getAnnouncementsMutedCategories(): Flow<Set<AnnouncementCategory>>

    suspend fun setAnnouncementCategoryMuted(category: AnnouncementCategory, muted: Boolean)

    fun getAnnouncementsLastFetchedAt(): Flow<Long>

    suspend fun setAnnouncementsLastFetchedAt(epochMillis: Long)

    fun getAppsSortRule(): Flow<String?>

    suspend fun setAppsSortRule(name: String)

    fun getStarredSortRule(): Flow<String?>

    suspend fun setStarredSortRule(name: String)

    fun getFavouritesSortRule(): Flow<String?>

    suspend fun setFavouritesSortRule(name: String)

    fun getCustomForgeHosts(): Flow<Set<String>>

    suspend fun addCustomForgeHost(host: String)

    suspend fun removeCustomForgeHost(host: String)
}
