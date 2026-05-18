package zed.rainxch.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import eu.anifantakis.lib.ksafe.KSafe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import zed.rainxch.core.domain.repository.DeviceIdentityRepository

@OptIn(ExperimentalUuidApi::class)
class DeviceIdentityRepositoryImpl(
    private val ksafe: KSafe,
    private val legacyDataStore: DataStore<Preferences>,
) : DeviceIdentityRepository {

    private val deviceIdMutex = Mutex()

    @Volatile private var migrated: Boolean = false

    override suspend fun getDeviceId(): String =
        deviceIdMutex.withLock {
            migrateIfNeeded()
            // Let read failures surface — a transient Keystore / DataStore
            // hiccup must not silently mint a new ID and orphan telemetry.
            val existing = ksafe.get(DEVICE_ID_KEY, "")
            if (existing.isNotBlank()) return existing

            val generated = Uuid.random().toString()
            ksafe.put(DEVICE_ID_KEY, generated)
            generated
        }

    override suspend fun resetDeviceId(): String =
        deviceIdMutex.withLock {
            // Migrate first so any pre-existing legacy entry is removed even
            // if the user resets before the first getDeviceId() call.
            migrateIfNeeded()
            val next = Uuid.random().toString()
            ksafe.put(DEVICE_ID_KEY, next)
            // Reset semantics: any legacy copy must die too.
            runCatching {
                legacyDataStore.edit { it.remove(stringPreferencesKey("anonymous_device_id")) }
            }
            next
        }

    private suspend fun migrateIfNeeded() {
        if (migrated) return
        val existing = runCatching { ksafe.get(DEVICE_ID_KEY, "") }.getOrDefault("")
        if (existing.isNotBlank()) {
            migrated = true
            return
        }
        val legacy = runCatching {
            legacyDataStore.data.first()[stringPreferencesKey("anonymous_device_id")]
        }.getOrNull()
        if (legacy.isNullOrBlank()) {
            // Nothing to migrate; first-run install or already cleaned.
            migrated = true
            return
        }
        val putResult = runCatching { ksafe.put(DEVICE_ID_KEY, legacy) }
        if (putResult.isFailure) {
            // Keep legacy intact for next attempt; do not flip marker.
            return
        }
        val deleteResult = runCatching {
            legacyDataStore.edit { it.remove(stringPreferencesKey("anonymous_device_id")) }
        }
        if (deleteResult.isFailure) {
            // Plaintext copy still on disk — retry cleanup next launch.
            return
        }
        migrated = true
    }

    private companion object {
        const val DEVICE_ID_KEY = "anonymous_device_id"
    }
}
