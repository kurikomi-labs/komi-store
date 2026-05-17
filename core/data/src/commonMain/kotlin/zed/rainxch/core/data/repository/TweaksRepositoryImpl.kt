package zed.rainxch.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import zed.rainxch.core.domain.model.AnnouncementCategory
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

    override fun getInstallerAttribution(): Flow<zed.rainxch.core.domain.model.InstallerAttribution> =
        preferences.data.map { prefs ->
            val raw = prefs[INSTALLER_ATTRIBUTION_KEY]
            decodeInstallerAttribution(raw)
        }

    override suspend fun setInstallerAttribution(
        attribution: zed.rainxch.core.domain.model.InstallerAttribution,
    ) {
        preferences.edit { prefs ->
            prefs[INSTALLER_ATTRIBUTION_KEY] = encodeInstallerAttribution(attribution)
        }
    }

    private fun decodeInstallerAttribution(
        raw: String?,
    ): zed.rainxch.core.domain.model.InstallerAttribution {
        if (raw.isNullOrBlank()) return zed.rainxch.core.domain.model.InstallerAttribution.SystemDefault
        val parts = raw.split(":", limit = 2)
        return when (parts[0]) {
            "preset" -> {
                val key = parts.getOrNull(1)?.let {
                    zed.rainxch.core.domain.model.PresetKey.fromName(it)
                }
                if (key != null) {
                    zed.rainxch.core.domain.model.InstallerAttribution.Preset(key)
                } else {
                    zed.rainxch.core.domain.model.InstallerAttribution.SystemDefault
                }
            }
            "custom" -> {
                val name = parts.getOrNull(1).orEmpty()
                if (name.isNotBlank()) {
                    zed.rainxch.core.domain.model.InstallerAttribution.Custom(name)
                } else {
                    zed.rainxch.core.domain.model.InstallerAttribution.SystemDefault
                }
            }
            else -> zed.rainxch.core.domain.model.InstallerAttribution.SystemDefault
        }
    }

    private fun encodeInstallerAttribution(
        attribution: zed.rainxch.core.domain.model.InstallerAttribution,
    ): String = when (attribution) {
        zed.rainxch.core.domain.model.InstallerAttribution.SystemDefault -> ""
        is zed.rainxch.core.domain.model.InstallerAttribution.Preset -> "preset:${attribution.key.name}"
        is zed.rainxch.core.domain.model.InstallerAttribution.Custom -> "custom:${attribution.packageName.trim()}"
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

    override fun getUpdateCheckEnabled(): Flow<Boolean> =
        preferences.data.map { prefs ->
            prefs[UPDATE_CHECK_ENABLED_KEY] ?: true
        }

    override suspend fun setUpdateCheckEnabled(enabled: Boolean) {
        preferences.edit { prefs ->
            prefs[UPDATE_CHECK_ENABLED_KEY] = enabled
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

    override fun getHideSeenEnabled(): Flow<Boolean> =
        preferences.data.map { prefs ->
            prefs[HIDE_SEEN_ENABLED_KEY] ?: false
        }

    override suspend fun setHideSeenEnabled(enabled: Boolean) {
        preferences.edit { prefs ->
            prefs[HIDE_SEEN_ENABLED_KEY] = enabled
        }
    }

    override fun getDiscoveryPlatforms(): Flow<Set<DiscoveryPlatform>> =
        preferences.data.map { prefs ->
            val stored = prefs[DISCOVERY_PLATFORMS_KEY]
            if (stored != null) {
                stored
                    .mapNotNull { name ->
                        DiscoveryPlatform.entries.find { it.name == name && it != DiscoveryPlatform.All }
                    }.toSet()
            } else {
                // Legacy single-platform key migration: map old `All` to empty set.
                val legacy = prefs[DISCOVERY_PLATFORM_KEY]?.let { DiscoveryPlatform.fromName(it) }
                if (legacy != null && legacy != DiscoveryPlatform.All) setOf(legacy) else emptySet()
            }
        }

    override suspend fun setDiscoveryPlatforms(platforms: Set<DiscoveryPlatform>) {
        preferences.edit { prefs ->
            val sanitized =
                platforms
                    .filter { it != DiscoveryPlatform.All }
                    .map { it.name }
                    .toSet()
            prefs[DISCOVERY_PLATFORMS_KEY] = sanitized
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
            prefs[EXTERNAL_MATCH_SEARCH_ENABLED_KEY] ?: true
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

    override fun getKaoBannerDismissed(): Flow<Boolean> =
        preferences.data.map { prefs ->
            prefs[KAO_BANNER_DISMISSED_KEY] ?: false
        }

    override suspend fun setKaoBannerDismissed(dismissed: Boolean) {
        preferences.edit { prefs ->
            prefs[KAO_BANNER_DISMISSED_KEY] = dismissed
        }
    }

    override fun getApkInspectCoachmarkShown(): Flow<Boolean> =
        preferences.data.map { prefs ->
            prefs[APK_INSPECT_COACHMARK_SHOWN_KEY] ?: false
        }

    override suspend fun setApkInspectCoachmarkShown(shown: Boolean) {
        preferences.edit { prefs ->
            prefs[APK_INSPECT_COACHMARK_SHOWN_KEY] = shown
        }
    }

    override fun getChannelChipCoachmarkShown(): Flow<Boolean> =
        preferences.data.map { prefs ->
            prefs[CHANNEL_CHIP_COACHMARK_SHOWN_KEY] ?: false
        }

    override suspend fun setChannelChipCoachmarkShown(shown: Boolean) {
        preferences.edit { prefs ->
            prefs[CHANNEL_CHIP_COACHMARK_SHOWN_KEY] = shown
        }
    }

    override fun getShowAllPlatforms(): Flow<Boolean> =
        preferences.data.map { prefs ->
            prefs[SHOW_ALL_PLATFORMS_KEY] ?: false
        }

    override suspend fun setShowAllPlatforms(enabled: Boolean) {
        preferences.edit { prefs ->
            prefs[SHOW_ALL_PLATFORMS_KEY] = enabled
        }
    }

    override fun getBatteryOptimizationPromptDismissed(): Flow<Boolean> =
        preferences.data.map { prefs ->
            prefs[BATTERY_OPT_PROMPT_DISMISSED_KEY] ?: false
        }

    override suspend fun setBatteryOptimizationPromptDismissed(dismissed: Boolean) {
        preferences.edit { prefs ->
            prefs[BATTERY_OPT_PROMPT_DISMISSED_KEY] = dismissed
        }
    }

    override fun getLastSeenWhatsNewVersionCode(): Flow<Int?> =
        preferences.data.map { prefs ->
            prefs[LAST_SEEN_WHATS_NEW_VERSION_CODE_KEY]
        }

    override suspend fun setLastSeenWhatsNewVersionCode(versionCode: Int) {
        preferences.edit { prefs ->
            val current = prefs[LAST_SEEN_WHATS_NEW_VERSION_CODE_KEY] ?: Int.MIN_VALUE
            if (versionCode > current) {
                prefs[LAST_SEEN_WHATS_NEW_VERSION_CODE_KEY] = versionCode
            }
        }
    }

    override fun getAnnouncementsDismissedIds(): Flow<Set<String>> =
        preferences.data.map { prefs ->
            prefs[ANNOUNCEMENTS_DISMISSED_IDS_KEY] ?: emptySet()
        }

    override suspend fun addAnnouncementDismissedId(id: String) {
        preferences.edit { prefs ->
            val current = prefs[ANNOUNCEMENTS_DISMISSED_IDS_KEY] ?: emptySet()
            prefs[ANNOUNCEMENTS_DISMISSED_IDS_KEY] = current + id
        }
    }

    override fun getAnnouncementsAcknowledgedIds(): Flow<Set<String>> =
        preferences.data.map { prefs ->
            prefs[ANNOUNCEMENTS_ACKNOWLEDGED_IDS_KEY] ?: emptySet()
        }

    override suspend fun addAnnouncementAcknowledgedId(id: String) {
        preferences.edit { prefs ->
            val current = prefs[ANNOUNCEMENTS_ACKNOWLEDGED_IDS_KEY] ?: emptySet()
            prefs[ANNOUNCEMENTS_ACKNOWLEDGED_IDS_KEY] = current + id
        }
    }

    override fun getAnnouncementsMutedCategories(): Flow<Set<AnnouncementCategory>> =
        preferences.data.map { prefs ->
            prefs[ANNOUNCEMENTS_MUTED_CATEGORIES_KEY]
                ?.mapNotNull { name ->
                    runCatching { AnnouncementCategory.valueOf(name) }.getOrNull()
                }
                ?.toSet()
                ?: emptySet()
        }

    override suspend fun setAnnouncementCategoryMuted(category: AnnouncementCategory, muted: Boolean) {
        preferences.edit { prefs ->
            val current = prefs[ANNOUNCEMENTS_MUTED_CATEGORIES_KEY] ?: emptySet()
            prefs[ANNOUNCEMENTS_MUTED_CATEGORIES_KEY] = if (muted) current + category.name else current - category.name
        }
    }

    override fun getAnnouncementsLastFetchedAt(): Flow<Long> =
        preferences.data.map { prefs ->
            prefs[ANNOUNCEMENTS_LAST_FETCHED_AT_KEY] ?: 0L
        }

    override suspend fun setAnnouncementsLastFetchedAt(epochMillis: Long) {
        preferences.edit { prefs ->
            prefs[ANNOUNCEMENTS_LAST_FETCHED_AT_KEY] = epochMillis
        }
    }

    override fun getAppsSortRule(): Flow<String?> =
        preferences.data.map { prefs -> prefs[APPS_SORT_RULE_KEY] }

    override suspend fun setAppsSortRule(name: String) {
        preferences.edit { prefs -> prefs[APPS_SORT_RULE_KEY] = name }
    }

    override fun getStarredSortRule(): Flow<String?> =
        preferences.data.map { prefs -> prefs[STARRED_SORT_RULE_KEY] }

    override suspend fun setStarredSortRule(name: String) {
        preferences.edit { prefs -> prefs[STARRED_SORT_RULE_KEY] = name }
    }

    override fun getFavouritesSortRule(): Flow<String?> =
        preferences.data.map { prefs -> prefs[FAVOURITES_SORT_RULE_KEY] }

    override suspend fun setFavouritesSortRule(name: String) {
        preferences.edit { prefs -> prefs[FAVOURITES_SORT_RULE_KEY] = name }
    }

    companion object {
        private const val DEFAULT_UPDATE_CHECK_INTERVAL_HOURS = 6L

        private val THEME_KEY = stringPreferencesKey("app_theme")
        private val AMOLED_KEY = booleanPreferencesKey("amoled_theme")
        private val IS_DARK_THEME_KEY = booleanPreferencesKey("is_dark_theme")
        private val FONT_KEY = stringPreferencesKey("font_theme")
        private val DISCOVERY_PLATFORM_KEY = stringPreferencesKey("discovery_platform")
        private val DISCOVERY_PLATFORMS_KEY = stringSetPreferencesKey("discovery_platforms")
        private val AUTO_DETECT_CLIPBOARD_KEY = booleanPreferencesKey("auto_detect_clipboard_links")
        private val INSTALLER_TYPE_KEY = stringPreferencesKey("installer_type")
        private val INSTALLER_ATTRIBUTION_KEY = stringPreferencesKey("installer_attribution")
        private val AUTO_UPDATE_KEY = booleanPreferencesKey("auto_update_enabled")
        private val UPDATE_CHECK_ENABLED_KEY = booleanPreferencesKey("update_check_enabled")
        private val UPDATE_CHECK_INTERVAL_KEY = longPreferencesKey("update_check_interval_hours")
        private val INCLUDE_PRE_RELEASES_KEY = booleanPreferencesKey("include_pre_releases")
        private val HIDE_SEEN_ENABLED_KEY = booleanPreferencesKey("hide_seen_enabled")
        private val SCROLLBAR_ENABLED_KEY = booleanPreferencesKey("scrollbar_enabled")
        private val TELEMETRY_ENABLED_KEY = booleanPreferencesKey("telemetry_enabled")
        private val TRANSLATION_PROVIDER_KEY = stringPreferencesKey("translation_provider")
        private val YOUDAO_APP_KEY = stringPreferencesKey("youdao_app_key")
        private val YOUDAO_APP_SECRET = stringPreferencesKey("youdao_app_secret")
        private val APP_LANGUAGE_KEY = stringPreferencesKey("app_language")
        private val EXTERNAL_IMPORT_ENABLED_KEY = booleanPreferencesKey("external_import_enabled")
        private val EXTERNAL_MATCH_SEARCH_ENABLED_KEY = booleanPreferencesKey("external_match_search_enabled")
        private val EXTERNAL_IMPORT_BANNER_DISMISSED_AT_KEY = intPreferencesKey("external_import_banner_dismissed_at")
        private val KAO_BANNER_DISMISSED_KEY = booleanPreferencesKey("kao_banner_dismissed")
        private val APK_INSPECT_COACHMARK_SHOWN_KEY = booleanPreferencesKey("apk_inspect_coachmark_shown")
        private val CHANNEL_CHIP_COACHMARK_SHOWN_KEY = booleanPreferencesKey("channel_chip_coachmark_shown")
        private val SHOW_ALL_PLATFORMS_KEY = booleanPreferencesKey("show_all_platforms")
        private val BATTERY_OPT_PROMPT_DISMISSED_KEY = booleanPreferencesKey("battery_opt_prompt_dismissed")
        private val LAST_SEEN_WHATS_NEW_VERSION_CODE_KEY = intPreferencesKey("last_seen_whats_new_version_code")
        private val ANNOUNCEMENTS_DISMISSED_IDS_KEY = stringSetPreferencesKey("announcements_dismissed_ids")
        private val ANNOUNCEMENTS_ACKNOWLEDGED_IDS_KEY = stringSetPreferencesKey("announcements_acknowledged_ids")
        private val ANNOUNCEMENTS_MUTED_CATEGORIES_KEY = stringSetPreferencesKey("announcements_muted_categories")
        private val ANNOUNCEMENTS_LAST_FETCHED_AT_KEY = longPreferencesKey("announcements_last_fetched_at")
        private val APPS_SORT_RULE_KEY = stringPreferencesKey("apps_sort_rule")
        private val STARRED_SORT_RULE_KEY = stringPreferencesKey("starred_sort_rule")
        private val FAVOURITES_SORT_RULE_KEY = stringPreferencesKey("favourites_sort_rule")
    }
}
