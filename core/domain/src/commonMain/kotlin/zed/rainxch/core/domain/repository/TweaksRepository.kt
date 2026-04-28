package zed.rainxch.core.domain.repository

import kotlinx.coroutines.flow.Flow
import zed.rainxch.core.domain.model.AppTheme
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

    fun getAutoUpdateEnabled(): Flow<Boolean>

    suspend fun setAutoUpdateEnabled(enabled: Boolean)

    fun getUpdateCheckInterval(): Flow<Long>

    suspend fun setUpdateCheckInterval(hours: Long)

    fun getIncludePreReleases(): Flow<Boolean>

    suspend fun setIncludePreReleases(enabled: Boolean)

    fun getLiquidGlassEnabled(): Flow<Boolean>

    suspend fun setLiquidGlassEnabled(enabled: Boolean)

    fun getHideSeenEnabled(): Flow<Boolean>

    suspend fun setHideSeenEnabled(enabled: Boolean)

    fun getDiscoveryPlatform(): Flow<DiscoveryPlatform>

    suspend fun setDiscoveryPlatform(platform: DiscoveryPlatform)

    fun getScrollbarEnabled(): Flow<Boolean>

    suspend fun setScrollbarEnabled(enabled: Boolean)

    fun getTelemetryEnabled(): Flow<Boolean>

    suspend fun setTelemetryEnabled(enabled: Boolean)

    // E6 product-metric telemetry consent. Three-state because the first-launch
    // sheet needs to distinguish "user actively said no" from "we haven't asked
    // yet." Independent of getTelemetryEnabled — that's the legacy ranking-
    // signals pipeline (different data, different backend table).
    fun getProductTelemetryConsent(): Flow<zed.rainxch.core.domain.telemetry.ProductTelemetryConsent>

    suspend fun setProductTelemetryConsent(consent: zed.rainxch.core.domain.telemetry.ProductTelemetryConsent)

    fun getTranslationProvider(): Flow<TranslationProvider>

    suspend fun setTranslationProvider(provider: TranslationProvider)

    fun getYoudaoAppKey(): Flow<String>

    suspend fun setYoudaoAppKey(appKey: String)

    fun getYoudaoAppSecret(): Flow<String>

    suspend fun setYoudaoAppSecret(appSecret: String)

    /**
     * Selected UI language as a BCP 47 tag (e.g. `zh-CN`). Emits
     * `null` when the user hasn't picked one — which means "follow
     * whatever the JVM/Android locale is" at app start. `null` is
     * distinct from `""`: the former is the unset state, the latter
     * would be a malformed user choice we don't support.
     */
    fun getAppLanguage(): Flow<String?>

    suspend fun setAppLanguage(tag: String?)

    fun getExternalImportEnabled(): Flow<Boolean>

    suspend fun setExternalImportEnabled(enabled: Boolean)

    fun getExternalMatchSearchEnabled(): Flow<Boolean>

    suspend fun setExternalMatchSearchEnabled(enabled: Boolean)

    fun getExternalImportBannerDismissedAtCount(): Flow<Int>

    suspend fun setExternalImportBannerDismissedAtCount(count: Int)
}
