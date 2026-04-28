package zed.rainxch.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import zed.rainxch.core.domain.model.AppLanguages
import zed.rainxch.core.domain.model.AppTheme
import zed.rainxch.core.domain.model.DiscoveryPlatform
import zed.rainxch.core.domain.model.FontTheme
import zed.rainxch.core.domain.model.InstallerType
import zed.rainxch.core.domain.model.TranslationProvider
import zed.rainxch.core.domain.repository.TweaksRepository

class TweaksRepositoryImpl(
    private val preferences: DataStore<Preferences>,
) : TweaksRepository {
    override fun getThemeColor(): Flow<AppTheme> =
        preferences.data.map { prefs ->
            val themeName = prefs[THEME_KEY]
            AppTheme.fromName(themeName)
        }

    override suspend fun setThemeColor(theme: AppTheme) {
        preferences.edit { prefs ->
            prefs[THEME_KEY] = theme.name
        }
    }

    override fun getIsDarkTheme(): Flow<Boolean?> =
        preferences.data.map { prefs ->
            prefs[IS_DARK_THEME_KEY]
        }

    override suspend fun setDarkTheme(isDarkTheme: Boolean?) {
        preferences.edit { prefs ->
            if (isDarkTheme == null) {
                prefs.remove(IS_DARK_THEME_KEY)
            } else {
                prefs[IS_DARK_THEME_KEY] = isDarkTheme
            }
        }
    }

    override fun getAmoledTheme(): Flow<Boolean> =
        preferences.data.map { prefs ->
            prefs[AMOLED_KEY] ?: false
        }

    override suspend fun setAmoledTheme(enabled: Boolean) {
        preferences.edit { prefs ->
            prefs[AMOLED_KEY] = enabled
        }
    }

    override fun getFontTheme(): Flow<FontTheme> =
        preferences.data.map { prefs ->
            val fontName = prefs[FONT_KEY]
            FontTheme.fromName(fontName)
        }

    override suspend fun setFontTheme(fontTheme: FontTheme) {
        preferences.edit { prefs ->
            prefs[FONT_KEY] = fontTheme.name
        }
    }

    override fun getAutoDetectClipboardLinks(): Flow<Boolean> =
        preferences.data.map { prefs ->
            prefs[AUTO_DETECT_CLIPBOARD_KEY] ?: false
        }

    override suspend fun setAutoDetectClipboardLinks(enabled: Boolean) {
        preferences.edit { prefs ->
            prefs[AUTO_DETECT_CLIPBOARD_KEY] = enabled
        }
    }

    override fun getInstallerType(): Flow<InstallerType> =
        preferences.data.map { prefs ->
            val name = prefs[INSTALLER_TYPE_KEY]
            InstallerType.fromName(name)
        }

    override suspend fun setInstallerType(type: InstallerType) {
        preferences.edit { prefs ->
            prefs[INSTALLER_TYPE_KEY] = type.name
        }
    }

    override fun getAutoUpdateEnabled(): Flow<Boolean> =
        preferences.data.map { prefs ->
            prefs[AUTO_UPDATE_KEY] ?: false
        }

    override suspend fun setAutoUpdateEnabled(enabled: Boolean) {
        preferences.edit { prefs ->
            prefs[AUTO_UPDATE_KEY] = enabled
        }
    }

    override fun getUpdateCheckInterval(): Flow<Long> =
        preferences.data.map { prefs ->
            prefs[UPDATE_CHECK_INTERVAL_KEY] ?: DEFAULT_UPDATE_CHECK_INTERVAL_HOURS
        }

    override suspend fun setUpdateCheckInterval(hours: Long) {
        preferences.edit { prefs ->
            prefs[UPDATE_CHECK_INTERVAL_KEY] = hours
        }
    }

    override fun getIncludePreReleases(): Flow<Boolean> =
        preferences.data.map { prefs ->
            prefs[INCLUDE_PRE_RELEASES_KEY] ?: false
        }

    override suspend fun setIncludePreReleases(enabled: Boolean) {
        preferences.edit { prefs ->
            prefs[INCLUDE_PRE_RELEASES_KEY] = enabled
        }
    }

    override fun getLiquidGlassEnabled(): Flow<Boolean> =
        preferences.data.map { prefs ->
            prefs[LIQUID_GLASS_ENABLED_KEY] ?: true
        }

    override suspend fun setLiquidGlassEnabled(enabled: Boolean) {
        preferences.edit { prefs ->
            prefs[LIQUID_GLASS_ENABLED_KEY] = enabled
        }
    }

    override fun getHideSeenEnabled(): Flow<Boolean> =
        preferences.data.map { prefs ->
            prefs[HIDE_SEEN_ENABLED_KEY] ?: false
        }

    override suspend fun setHideSeenEnabled(enabled: Boolean) {
        preferences.edit { prefs ->
            prefs[HIDE_SEEN_ENABLED_KEY] = enabled
        }
    }

    override fun getDiscoveryPlatform(): Flow<DiscoveryPlatform> =
        preferences.data.map { prefs ->
            val platform = prefs[DISCOVERY_PLATFORM_KEY]
            DiscoveryPlatform.fromName(platform)
        }

    override suspend fun setDiscoveryPlatform(platform: DiscoveryPlatform) {
        preferences.edit { prefs ->
            prefs[DISCOVERY_PLATFORM_KEY] = platform.name
        }
    }

    override fun getScrollbarEnabled(): Flow<Boolean> =
        preferences.data.map { prefs ->
            prefs[SCROLLBAR_ENABLED_KEY] ?: false
        }

    override suspend fun setScrollbarEnabled(enabled: Boolean) {
        preferences.edit { prefs ->
            prefs[SCROLLBAR_ENABLED_KEY] = enabled
        }
    }

    override fun getTelemetryEnabled(): Flow<Boolean> =
        preferences.data.map { prefs ->
            prefs[TELEMETRY_ENABLED_KEY] ?: false
        }

    override suspend fun setTelemetryEnabled(enabled: Boolean) {
        preferences.edit { prefs ->
            prefs[TELEMETRY_ENABLED_KEY] = enabled
        }
    }

    override fun getProductTelemetryConsent(): Flow<zed.rainxch.core.domain.telemetry.ProductTelemetryConsent> =
        preferences.data.map { prefs ->
            when (prefs[PRODUCT_TELEMETRY_CONSENT_KEY]) {
                "Granted" -> zed.rainxch.core.domain.telemetry.ProductTelemetryConsent.Granted
                "Denied" -> zed.rainxch.core.domain.telemetry.ProductTelemetryConsent.Denied
                else -> zed.rainxch.core.domain.telemetry.ProductTelemetryConsent.NotYetAsked
            }
        }

    override suspend fun setProductTelemetryConsent(consent: zed.rainxch.core.domain.telemetry.ProductTelemetryConsent) {
        preferences.edit { prefs ->
            prefs[PRODUCT_TELEMETRY_CONSENT_KEY] = consent.name
        }
    }

    override fun getTranslationProvider(): Flow<TranslationProvider> =
        preferences.data.map { prefs ->
            TranslationProvider.fromName(prefs[TRANSLATION_PROVIDER_KEY])
        }

    override suspend fun setTranslationProvider(provider: TranslationProvider) {
        preferences.edit { prefs ->
            prefs[TRANSLATION_PROVIDER_KEY] = provider.name
        }
    }

    override fun getYoudaoAppKey(): Flow<String> =
        preferences.data.map { prefs -> prefs[YOUDAO_APP_KEY] ?: "" }

    override suspend fun setYoudaoAppKey(appKey: String) {
        preferences.edit { prefs ->
            val trimmed = appKey.trim()
            if (trimmed.isEmpty()) prefs.remove(YOUDAO_APP_KEY) else prefs[YOUDAO_APP_KEY] = trimmed
        }
    }

    override fun getYoudaoAppSecret(): Flow<String> =
        preferences.data.map { prefs -> prefs[YOUDAO_APP_SECRET] ?: "" }

    override suspend fun setYoudaoAppSecret(appSecret: String) {
        preferences.edit { prefs ->
            val trimmed = appSecret.trim()
            if (trimmed.isEmpty()) prefs.remove(YOUDAO_APP_SECRET) else prefs[YOUDAO_APP_SECRET] = trimmed
        }
    }

    override fun getAppLanguage(): Flow<String?> =
        preferences.data.map { prefs ->
            prefs[APP_LANGUAGE_KEY]
                ?.trim()
                ?.takeIf { it.isNotEmpty() && AppLanguages.containsTag(it) }
        }

    override suspend fun setAppLanguage(tag: String?) {
        preferences.edit { prefs ->
            val normalized = tag?.trim().orEmpty()
            if (normalized.isEmpty() || !AppLanguages.containsTag(normalized)) {
                prefs.remove(APP_LANGUAGE_KEY)
            } else {
                prefs[APP_LANGUAGE_KEY] = normalized
            }
        }
    }

    override fun getExternalImportEnabled(): Flow<Boolean> =
        preferences.data.map { prefs ->
            prefs[EXTERNAL_IMPORT_ENABLED_KEY] ?: true
        }

    override suspend fun setExternalImportEnabled(enabled: Boolean) {
        preferences.edit { prefs ->
            prefs[EXTERNAL_IMPORT_ENABLED_KEY] = enabled
        }
    }

    override fun getExternalMatchSearchEnabled(): Flow<Boolean> =
        preferences.data.map { prefs ->
            prefs[EXTERNAL_MATCH_SEARCH_ENABLED_KEY] ?: false
        }

    override suspend fun setExternalMatchSearchEnabled(enabled: Boolean) {
        preferences.edit { prefs ->
            prefs[EXTERNAL_MATCH_SEARCH_ENABLED_KEY] = enabled
        }
    }

    override fun getExternalImportBannerDismissedAtCount(): Flow<Int> =
        preferences.data.map { prefs ->
            prefs[EXTERNAL_IMPORT_BANNER_DISMISSED_AT_KEY] ?: 0
        }

    override suspend fun setExternalImportBannerDismissedAtCount(count: Int) {
        preferences.edit { prefs ->
            prefs[EXTERNAL_IMPORT_BANNER_DISMISSED_AT_KEY] = count
        }
    }

    companion object {
        private const val DEFAULT_UPDATE_CHECK_INTERVAL_HOURS = 6L

        private val THEME_KEY = stringPreferencesKey("app_theme")
        private val AMOLED_KEY = booleanPreferencesKey("amoled_theme")
        private val IS_DARK_THEME_KEY = booleanPreferencesKey("is_dark_theme")
        private val FONT_KEY = stringPreferencesKey("font_theme")
        private val DISCOVERY_PLATFORM_KEY = stringPreferencesKey("discovery_platform")
        private val AUTO_DETECT_CLIPBOARD_KEY = booleanPreferencesKey("auto_detect_clipboard_links")
        private val INSTALLER_TYPE_KEY = stringPreferencesKey("installer_type")
        private val AUTO_UPDATE_KEY = booleanPreferencesKey("auto_update_enabled")
        private val UPDATE_CHECK_INTERVAL_KEY = longPreferencesKey("update_check_interval_hours")
        private val INCLUDE_PRE_RELEASES_KEY = booleanPreferencesKey("include_pre_releases")
        private val LIQUID_GLASS_ENABLED_KEY = booleanPreferencesKey("liquid_glass_enabled")
        private val HIDE_SEEN_ENABLED_KEY = booleanPreferencesKey("hide_seen_enabled")
        private val SCROLLBAR_ENABLED_KEY = booleanPreferencesKey("scrollbar_enabled")
        private val TELEMETRY_ENABLED_KEY = booleanPreferencesKey("telemetry_enabled")
        private val PRODUCT_TELEMETRY_CONSENT_KEY = stringPreferencesKey("product_telemetry_consent")
        private val TRANSLATION_PROVIDER_KEY = stringPreferencesKey("translation_provider")
        private val YOUDAO_APP_KEY = stringPreferencesKey("youdao_app_key")
        private val YOUDAO_APP_SECRET = stringPreferencesKey("youdao_app_secret")
        private val APP_LANGUAGE_KEY = stringPreferencesKey("app_language")
        private val EXTERNAL_IMPORT_ENABLED_KEY = booleanPreferencesKey("external_import_enabled")
        private val EXTERNAL_MATCH_SEARCH_ENABLED_KEY = booleanPreferencesKey("external_match_search_enabled")
        private val EXTERNAL_IMPORT_BANNER_DISMISSED_AT_KEY = intPreferencesKey("external_import_banner_dismissed_at")
    }
}
