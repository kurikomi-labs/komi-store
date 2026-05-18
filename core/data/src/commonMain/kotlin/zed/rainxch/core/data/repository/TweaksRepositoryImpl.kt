package zed.rainxch.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import eu.anifantakis.lib.ksafe.KSafe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import zed.rainxch.core.data.secure.MigrationEntry
import zed.rainxch.core.data.secure.migrateDataStoreToKSafe
import zed.rainxch.core.domain.model.AnnouncementCategory
import zed.rainxch.core.domain.model.AppLanguages
import zed.rainxch.core.domain.model.AppTheme
import zed.rainxch.core.domain.model.DiscoveryPlatform
import zed.rainxch.core.domain.model.FontTheme
import zed.rainxch.core.domain.model.InstallerType
import zed.rainxch.core.domain.model.TranslationProvider
import zed.rainxch.core.domain.repository.TweaksRepository

class TweaksRepositoryImpl(
    private val ksafe: KSafe,
    private val legacyDataStore: DataStore<Preferences>,
) : TweaksRepository {

    private val migrationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        migrationScope.launch {
            migrateDataStoreToKSafe(
                legacy = legacyDataStore,
                ksafe = ksafe,
                markerKey = MIGRATION_MARKER,
                entries = legacyEntries(),
            )
        }
    }

    override fun getThemeColor(): Flow<AppTheme> =
        ksafe.getFlow(K_THEME, "").map { AppTheme.fromName(it.ifEmpty { null }) }

    override suspend fun setThemeColor(theme: AppTheme) {
        ksafe.put(K_THEME, theme.name)
    }

    override fun getIsDarkTheme(): Flow<Boolean?> =
        ksafe.getFlow<Boolean?>(K_IS_DARK, null)

    override suspend fun setDarkTheme(isDarkTheme: Boolean?) {
        if (isDarkTheme == null) ksafe.delete(K_IS_DARK) else ksafe.put(K_IS_DARK, isDarkTheme)
    }

    override fun getAmoledTheme(): Flow<Boolean> = ksafe.getFlow(K_AMOLED, false)
    override suspend fun setAmoledTheme(enabled: Boolean) { ksafe.put(K_AMOLED, enabled) }

    override fun getFontTheme(): Flow<FontTheme> =
        ksafe.getFlow(K_FONT, "").map { FontTheme.fromName(it.ifEmpty { null }) }

    override suspend fun setFontTheme(fontTheme: FontTheme) {
        ksafe.put(K_FONT, fontTheme.name)
    }

    override fun getAutoDetectClipboardLinks(): Flow<Boolean> = ksafe.getFlow(K_AUTO_DETECT_CLIPBOARD, false)
    override suspend fun setAutoDetectClipboardLinks(enabled: Boolean) { ksafe.put(K_AUTO_DETECT_CLIPBOARD, enabled) }

    override fun getInstallerType(): Flow<InstallerType> =
        ksafe.getFlow(K_INSTALLER_TYPE, "").map { InstallerType.fromName(it.ifEmpty { null }) }

    override suspend fun setInstallerType(type: InstallerType) {
        ksafe.put(K_INSTALLER_TYPE, type.name)
    }

    override fun getInstallerAttribution(): Flow<zed.rainxch.core.domain.model.InstallerAttribution> =
        ksafe.getFlow(K_INSTALLER_ATTRIBUTION, "").map { decodeInstallerAttribution(it.ifEmpty { null }) }

    override suspend fun setInstallerAttribution(
        attribution: zed.rainxch.core.domain.model.InstallerAttribution,
    ) {
        ksafe.put(K_INSTALLER_ATTRIBUTION, encodeInstallerAttribution(attribution))
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

    override fun getAutoUpdateEnabled(): Flow<Boolean> = ksafe.getFlow(K_AUTO_UPDATE, false)
    override suspend fun setAutoUpdateEnabled(enabled: Boolean) { ksafe.put(K_AUTO_UPDATE, enabled) }

    override fun getUpdateCheckEnabled(): Flow<Boolean> = ksafe.getFlow(K_UPDATE_CHECK_ENABLED, true)
    override suspend fun setUpdateCheckEnabled(enabled: Boolean) { ksafe.put(K_UPDATE_CHECK_ENABLED, enabled) }

    override fun getUpdateCheckInterval(): Flow<Long> = ksafe.getFlow(K_UPDATE_CHECK_INTERVAL, DEFAULT_UPDATE_CHECK_INTERVAL_HOURS)
    override suspend fun setUpdateCheckInterval(hours: Long) { ksafe.put(K_UPDATE_CHECK_INTERVAL, hours) }

    override fun getIncludePreReleases(): Flow<Boolean> = ksafe.getFlow(K_INCLUDE_PRE_RELEASES, false)
    override suspend fun setIncludePreReleases(enabled: Boolean) { ksafe.put(K_INCLUDE_PRE_RELEASES, enabled) }

    override fun getHideSeenEnabled(): Flow<Boolean> = ksafe.getFlow(K_HIDE_SEEN_ENABLED, false)
    override suspend fun setHideSeenEnabled(enabled: Boolean) { ksafe.put(K_HIDE_SEEN_ENABLED, enabled) }

    override fun getDiscoveryPlatforms(): Flow<Set<DiscoveryPlatform>> =
        ksafe.getFlow<List<String>>(K_DISCOVERY_PLATFORMS, emptyList()).map { stored ->
            stored.mapNotNull { name ->
                DiscoveryPlatform.entries.find { it.name == name && it != DiscoveryPlatform.All }
            }.toSet()
        }

    override suspend fun setDiscoveryPlatforms(platforms: Set<DiscoveryPlatform>) {
        ksafe.put(
            K_DISCOVERY_PLATFORMS,
            platforms.filter { it != DiscoveryPlatform.All }.map { it.name },
        )
    }

    override fun getScrollbarEnabled(): Flow<Boolean> = ksafe.getFlow(K_SCROLLBAR_ENABLED, false)
    override suspend fun setScrollbarEnabled(enabled: Boolean) { ksafe.put(K_SCROLLBAR_ENABLED, enabled) }

    override fun getTelemetryEnabled(): Flow<Boolean> = ksafe.getFlow(K_TELEMETRY_ENABLED, false)
    override suspend fun setTelemetryEnabled(enabled: Boolean) { ksafe.put(K_TELEMETRY_ENABLED, enabled) }

    override fun getTranslationProvider(): Flow<TranslationProvider> =
        ksafe.getFlow(K_TRANSLATION_PROVIDER, "").map { TranslationProvider.fromName(it.ifEmpty { null }) }

    override suspend fun setTranslationProvider(provider: TranslationProvider) {
        ksafe.put(K_TRANSLATION_PROVIDER, provider.name)
    }

    override fun getYoudaoAppKey(): Flow<String> = ksafe.getFlow(K_YOUDAO_APP_KEY, "")
    override suspend fun setYoudaoAppKey(appKey: String) {
        val trimmed = appKey.trim()
        if (trimmed.isEmpty()) ksafe.delete(K_YOUDAO_APP_KEY) else ksafe.put(K_YOUDAO_APP_KEY, trimmed)
    }

    override fun getYoudaoAppSecret(): Flow<String> = ksafe.getFlow(K_YOUDAO_APP_SECRET, "")
    override suspend fun setYoudaoAppSecret(appSecret: String) {
        val trimmed = appSecret.trim()
        if (trimmed.isEmpty()) ksafe.delete(K_YOUDAO_APP_SECRET) else ksafe.put(K_YOUDAO_APP_SECRET, trimmed)
    }

    override fun getAppLanguage(): Flow<String?> =
        ksafe.getFlow<String?>(K_APP_LANGUAGE, null).map { raw ->
            raw?.trim()?.takeIf { it.isNotEmpty() && AppLanguages.containsTag(it) }
        }

    override suspend fun setAppLanguage(tag: String?) {
        val normalized = tag?.trim().orEmpty()
        if (normalized.isEmpty() || !AppLanguages.containsTag(normalized)) {
            ksafe.delete(K_APP_LANGUAGE)
        } else {
            ksafe.put(K_APP_LANGUAGE, normalized)
        }
    }

    override fun getExternalImportEnabled(): Flow<Boolean> = ksafe.getFlow(K_EXTERNAL_IMPORT_ENABLED, true)
    override suspend fun setExternalImportEnabled(enabled: Boolean) { ksafe.put(K_EXTERNAL_IMPORT_ENABLED, enabled) }

    override fun getExternalMatchSearchEnabled(): Flow<Boolean> = ksafe.getFlow(K_EXTERNAL_MATCH_SEARCH_ENABLED, true)
    override suspend fun setExternalMatchSearchEnabled(enabled: Boolean) { ksafe.put(K_EXTERNAL_MATCH_SEARCH_ENABLED, enabled) }

    override fun getExternalImportBannerDismissedAtCount(): Flow<Int> = ksafe.getFlow(K_EXTERNAL_IMPORT_BANNER_DISMISSED_AT, 0)
    override suspend fun setExternalImportBannerDismissedAtCount(count: Int) { ksafe.put(K_EXTERNAL_IMPORT_BANNER_DISMISSED_AT, count) }

    override fun getKaoBannerDismissed(): Flow<Boolean> = ksafe.getFlow(K_KAO_BANNER_DISMISSED, false)
    override suspend fun setKaoBannerDismissed(dismissed: Boolean) { ksafe.put(K_KAO_BANNER_DISMISSED, dismissed) }

    override fun getApkInspectCoachmarkShown(): Flow<Boolean> = ksafe.getFlow(K_APK_INSPECT_COACHMARK_SHOWN, false)
    override suspend fun setApkInspectCoachmarkShown(shown: Boolean) { ksafe.put(K_APK_INSPECT_COACHMARK_SHOWN, shown) }

    override fun getChannelChipCoachmarkShown(): Flow<Boolean> = ksafe.getFlow(K_CHANNEL_CHIP_COACHMARK_SHOWN, false)
    override suspend fun setChannelChipCoachmarkShown(shown: Boolean) { ksafe.put(K_CHANNEL_CHIP_COACHMARK_SHOWN, shown) }

    override fun getShowAllPlatforms(): Flow<Boolean> = ksafe.getFlow(K_SHOW_ALL_PLATFORMS, false)
    override suspend fun setShowAllPlatforms(enabled: Boolean) { ksafe.put(K_SHOW_ALL_PLATFORMS, enabled) }

    override fun getBatteryOptimizationPromptDismissed(): Flow<Boolean> = ksafe.getFlow(K_BATTERY_OPT_PROMPT_DISMISSED, false)
    override suspend fun setBatteryOptimizationPromptDismissed(dismissed: Boolean) { ksafe.put(K_BATTERY_OPT_PROMPT_DISMISSED, dismissed) }

    override fun getLastSeenWhatsNewVersionCode(): Flow<Int?> =
        ksafe.getFlow<Int?>(K_LAST_SEEN_WHATS_NEW_VERSION_CODE, null)

    override suspend fun setLastSeenWhatsNewVersionCode(versionCode: Int) {
        val current = ksafe.get<Int?>(K_LAST_SEEN_WHATS_NEW_VERSION_CODE, null) ?: Int.MIN_VALUE
        if (versionCode > current) {
            ksafe.put(K_LAST_SEEN_WHATS_NEW_VERSION_CODE, versionCode)
        }
    }

    override fun getAnnouncementsDismissedIds(): Flow<Set<String>> =
        ksafe.getFlow<List<String>>(K_ANNOUNCEMENTS_DISMISSED_IDS, emptyList()).map { it.toSet() }

    override suspend fun addAnnouncementDismissedId(id: String) {
        val current = ksafe.get<List<String>>(K_ANNOUNCEMENTS_DISMISSED_IDS, emptyList()).toSet()
        ksafe.put(K_ANNOUNCEMENTS_DISMISSED_IDS, (current + id).toList())
    }

    override fun getAnnouncementsAcknowledgedIds(): Flow<Set<String>> =
        ksafe.getFlow<List<String>>(K_ANNOUNCEMENTS_ACKNOWLEDGED_IDS, emptyList()).map { it.toSet() }

    override suspend fun addAnnouncementAcknowledgedId(id: String) {
        val current = ksafe.get<List<String>>(K_ANNOUNCEMENTS_ACKNOWLEDGED_IDS, emptyList()).toSet()
        ksafe.put(K_ANNOUNCEMENTS_ACKNOWLEDGED_IDS, (current + id).toList())
    }

    override fun getAnnouncementsMutedCategories(): Flow<Set<AnnouncementCategory>> =
        ksafe.getFlow<List<String>>(K_ANNOUNCEMENTS_MUTED_CATEGORIES, emptyList()).map { stored ->
            stored.mapNotNull { name ->
                runCatching { AnnouncementCategory.valueOf(name) }.getOrNull()
            }.toSet()
        }

    override suspend fun setAnnouncementCategoryMuted(category: AnnouncementCategory, muted: Boolean) {
        val current = ksafe.get<List<String>>(K_ANNOUNCEMENTS_MUTED_CATEGORIES, emptyList()).toSet()
        val updated = if (muted) current + category.name else current - category.name
        ksafe.put(K_ANNOUNCEMENTS_MUTED_CATEGORIES, updated.toList())
    }

    override fun getAnnouncementsLastFetchedAt(): Flow<Long> = ksafe.getFlow(K_ANNOUNCEMENTS_LAST_FETCHED_AT, 0L)
    override suspend fun setAnnouncementsLastFetchedAt(epochMillis: Long) { ksafe.put(K_ANNOUNCEMENTS_LAST_FETCHED_AT, epochMillis) }

    override fun getAppsSortRule(): Flow<String?> = ksafe.getFlow<String?>(K_APPS_SORT_RULE, null)
    override suspend fun setAppsSortRule(name: String) { ksafe.put(K_APPS_SORT_RULE, name) }

    override fun getStarredSortRule(): Flow<String?> = ksafe.getFlow<String?>(K_STARRED_SORT_RULE, null)
    override suspend fun setStarredSortRule(name: String) { ksafe.put(K_STARRED_SORT_RULE, name) }

    override fun getFavouritesSortRule(): Flow<String?> = ksafe.getFlow<String?>(K_FAVOURITES_SORT_RULE, null)
    override suspend fun setFavouritesSortRule(name: String) { ksafe.put(K_FAVOURITES_SORT_RULE, name) }

    private fun legacyEntries(): List<MigrationEntry> = listOf(
        MigrationEntry(stringPreferencesKey("app_theme"), K_THEME),
        MigrationEntry(booleanPreferencesKey("amoled_theme"), K_AMOLED),
        MigrationEntry(booleanPreferencesKey("is_dark_theme"), K_IS_DARK),
        MigrationEntry(stringPreferencesKey("font_theme"), K_FONT),
        MigrationEntry(stringSetPreferencesKey("discovery_platforms"), K_DISCOVERY_PLATFORMS),
        MigrationEntry(booleanPreferencesKey("auto_detect_clipboard_links"), K_AUTO_DETECT_CLIPBOARD),
        MigrationEntry(stringPreferencesKey("installer_type"), K_INSTALLER_TYPE),
        MigrationEntry(stringPreferencesKey("installer_attribution"), K_INSTALLER_ATTRIBUTION),
        MigrationEntry(booleanPreferencesKey("auto_update_enabled"), K_AUTO_UPDATE),
        MigrationEntry(booleanPreferencesKey("update_check_enabled"), K_UPDATE_CHECK_ENABLED),
        MigrationEntry(longPreferencesKey("update_check_interval_hours"), K_UPDATE_CHECK_INTERVAL),
        MigrationEntry(booleanPreferencesKey("include_pre_releases"), K_INCLUDE_PRE_RELEASES),
        MigrationEntry(booleanPreferencesKey("hide_seen_enabled"), K_HIDE_SEEN_ENABLED),
        MigrationEntry(booleanPreferencesKey("scrollbar_enabled"), K_SCROLLBAR_ENABLED),
        MigrationEntry(booleanPreferencesKey("telemetry_enabled"), K_TELEMETRY_ENABLED),
        MigrationEntry(stringPreferencesKey("translation_provider"), K_TRANSLATION_PROVIDER),
        MigrationEntry(stringPreferencesKey("youdao_app_key"), K_YOUDAO_APP_KEY),
        MigrationEntry(stringPreferencesKey("youdao_app_secret"), K_YOUDAO_APP_SECRET),
        MigrationEntry(stringPreferencesKey("app_language"), K_APP_LANGUAGE),
        MigrationEntry(booleanPreferencesKey("external_import_enabled"), K_EXTERNAL_IMPORT_ENABLED),
        MigrationEntry(booleanPreferencesKey("external_match_search_enabled"), K_EXTERNAL_MATCH_SEARCH_ENABLED),
        MigrationEntry(intPreferencesKey("external_import_banner_dismissed_at"), K_EXTERNAL_IMPORT_BANNER_DISMISSED_AT),
        MigrationEntry(booleanPreferencesKey("kao_banner_dismissed"), K_KAO_BANNER_DISMISSED),
        MigrationEntry(booleanPreferencesKey("apk_inspect_coachmark_shown"), K_APK_INSPECT_COACHMARK_SHOWN),
        MigrationEntry(booleanPreferencesKey("channel_chip_coachmark_shown"), K_CHANNEL_CHIP_COACHMARK_SHOWN),
        MigrationEntry(booleanPreferencesKey("show_all_platforms"), K_SHOW_ALL_PLATFORMS),
        MigrationEntry(booleanPreferencesKey("battery_opt_prompt_dismissed"), K_BATTERY_OPT_PROMPT_DISMISSED),
        MigrationEntry(intPreferencesKey("last_seen_whats_new_version_code"), K_LAST_SEEN_WHATS_NEW_VERSION_CODE),
        MigrationEntry(stringSetPreferencesKey("announcements_dismissed_ids"), K_ANNOUNCEMENTS_DISMISSED_IDS),
        MigrationEntry(stringSetPreferencesKey("announcements_acknowledged_ids"), K_ANNOUNCEMENTS_ACKNOWLEDGED_IDS),
        MigrationEntry(stringSetPreferencesKey("announcements_muted_categories"), K_ANNOUNCEMENTS_MUTED_CATEGORIES),
        MigrationEntry(longPreferencesKey("announcements_last_fetched_at"), K_ANNOUNCEMENTS_LAST_FETCHED_AT),
        MigrationEntry(stringPreferencesKey("apps_sort_rule"), K_APPS_SORT_RULE),
        MigrationEntry(stringPreferencesKey("starred_sort_rule"), K_STARRED_SORT_RULE),
        MigrationEntry(stringPreferencesKey("favourites_sort_rule"), K_FAVOURITES_SORT_RULE),
    )

    companion object {
        private const val DEFAULT_UPDATE_CHECK_INTERVAL_HOURS = 6L
        private const val MIGRATION_MARKER = "__migrated_from_datastore_v1__"

        private const val K_THEME = "app_theme"
        private const val K_AMOLED = "amoled_theme"
        private const val K_IS_DARK = "is_dark_theme"
        private const val K_FONT = "font_theme"
        private const val K_DISCOVERY_PLATFORMS = "discovery_platforms"
        private const val K_AUTO_DETECT_CLIPBOARD = "auto_detect_clipboard_links"
        private const val K_INSTALLER_TYPE = "installer_type"
        private const val K_INSTALLER_ATTRIBUTION = "installer_attribution"
        private const val K_AUTO_UPDATE = "auto_update_enabled"
        private const val K_UPDATE_CHECK_ENABLED = "update_check_enabled"
        private const val K_UPDATE_CHECK_INTERVAL = "update_check_interval_hours"
        private const val K_INCLUDE_PRE_RELEASES = "include_pre_releases"
        private const val K_HIDE_SEEN_ENABLED = "hide_seen_enabled"
        private const val K_SCROLLBAR_ENABLED = "scrollbar_enabled"
        private const val K_TELEMETRY_ENABLED = "telemetry_enabled"
        private const val K_TRANSLATION_PROVIDER = "translation_provider"
        private const val K_YOUDAO_APP_KEY = "youdao_app_key"
        private const val K_YOUDAO_APP_SECRET = "youdao_app_secret"
        private const val K_APP_LANGUAGE = "app_language"
        private const val K_EXTERNAL_IMPORT_ENABLED = "external_import_enabled"
        private const val K_EXTERNAL_MATCH_SEARCH_ENABLED = "external_match_search_enabled"
        private const val K_EXTERNAL_IMPORT_BANNER_DISMISSED_AT = "external_import_banner_dismissed_at"
        private const val K_KAO_BANNER_DISMISSED = "kao_banner_dismissed"
        private const val K_APK_INSPECT_COACHMARK_SHOWN = "apk_inspect_coachmark_shown"
        private const val K_CHANNEL_CHIP_COACHMARK_SHOWN = "channel_chip_coachmark_shown"
        private const val K_SHOW_ALL_PLATFORMS = "show_all_platforms"
        private const val K_BATTERY_OPT_PROMPT_DISMISSED = "battery_opt_prompt_dismissed"
        private const val K_LAST_SEEN_WHATS_NEW_VERSION_CODE = "last_seen_whats_new_version_code"
        private const val K_ANNOUNCEMENTS_DISMISSED_IDS = "announcements_dismissed_ids"
        private const val K_ANNOUNCEMENTS_ACKNOWLEDGED_IDS = "announcements_acknowledged_ids"
        private const val K_ANNOUNCEMENTS_MUTED_CATEGORIES = "announcements_muted_categories"
        private const val K_ANNOUNCEMENTS_LAST_FETCHED_AT = "announcements_last_fetched_at"
        private const val K_APPS_SORT_RULE = "apps_sort_rule"
        private const val K_STARRED_SORT_RULE = "starred_sort_rule"
        private const val K_FAVOURITES_SORT_RULE = "favourites_sort_rule"
    }
}
