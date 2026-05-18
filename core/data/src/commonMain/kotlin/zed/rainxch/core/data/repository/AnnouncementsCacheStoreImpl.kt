package zed.rainxch.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import co.touchlab.kermit.Logger
import eu.anifantakis.lib.ksafe.KSafe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import zed.rainxch.core.domain.repository.AnnouncementsCacheStore

class AnnouncementsCacheStoreImpl(
    private val ksafe: KSafe,
    private val legacyDataStore: DataStore<Preferences>,
) : AnnouncementsCacheStore {
    private val logger = Logger.withTag("AnnouncementsCacheStore")
    private val migrationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile private var migrated: Boolean = false

    init {
        migrationScope.launch { migrateIfNeeded() }
    }

    override fun getCachedPayload(): Flow<String?> =
        ksafe.getFlow<String?>(K_CACHED_PAYLOAD, null).map { it?.takeIf { v -> v.isNotEmpty() } }

    override suspend fun setCachedPayload(payload: String?) {
        if (payload == null) {
            ksafe.delete(K_CACHED_PAYLOAD)
        } else {
            ksafe.put(K_CACHED_PAYLOAD, payload)
        }
    }

    private suspend fun migrateIfNeeded() {
        if (migrated) return
        val existing = runCatching { ksafe.get<String?>(K_CACHED_PAYLOAD, null) }.getOrNull()
        if (existing != null) {
            migrated = true
            return
        }
        val legacy = runCatching {
            legacyDataStore.data.first()[stringPreferencesKey("announcements_cached_payload")]
        }.getOrNull()
        if (!legacy.isNullOrEmpty()) {
            runCatching { ksafe.put(K_CACHED_PAYLOAD, legacy) }
            runCatching {
                legacyDataStore.edit {
                    it.remove(stringPreferencesKey("announcements_cached_payload"))
                }
            }.onFailure { logger.w("legacy clear failed: ${it.message}") }
        }
        migrated = true
    }

    private companion object {
        const val K_CACHED_PAYLOAD = "announcements_cached_payload"
    }
}
